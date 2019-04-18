package ca.ulaval.ima.mp.models.gateway.voice;

import android.util.Log;

import java.net.DatagramPacket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;

import ca.ulaval.ima.mp.lib.TweetNaclFast;

public class AudioPacket {
    public static final int RTP_HEADER_BYTE_LENGTH = 12;
    public static final byte RTP_VERSION_PAD_EXTEND = (byte) 0x80;
    public static final byte RTP_PAYLOAD_TYPE = (byte) 0x78;
    public static final short RTP_DISCORD_EXTENSION = (short) 0xBEDE;

    public static final int PT_INDEX = 1;
    public static final int SEQ_INDEX = 2;
    public static final int TIMESTAMP_INDEX = 4;
    public static final int SSRC_INDEX = 8;

    private final byte type;
    private final char seq;
    private final int timestamp;
    private final int ssrc;
    private final byte[] encodedAudio;
    private final byte[] rawPacket;

    public AudioPacket(byte[] rawPacket)
    {
        this.rawPacket = rawPacket;

        ByteBuffer buffer = ByteBuffer.wrap(rawPacket);
        this.seq = buffer.getChar(SEQ_INDEX);
        this.timestamp = buffer.getInt(TIMESTAMP_INDEX);
        this.ssrc = buffer.getInt(SSRC_INDEX);
        this.type = buffer.get(PT_INDEX);

        final byte profile = buffer.get(0);
        final byte[] data = buffer.array();
        final boolean hasExtension = (profile & 0x10) != 0;
        final byte cc = (byte) (profile & 0x0f);
        final int csrcLength = cc * 4;
        final short extension = hasExtension ? getShort(data, RTP_HEADER_BYTE_LENGTH + csrcLength) : 0;

        int offset = RTP_HEADER_BYTE_LENGTH + csrcLength;
        if (hasExtension && extension == RTP_DISCORD_EXTENSION)
            offset = getPayloadOffset(data, csrcLength);

        this.encodedAudio = new byte[data.length - offset];
        System.arraycopy(data, offset, this.encodedAudio, 0, this.encodedAudio.length);
    }

    public AudioPacket(ByteBuffer buffer, char seq, int timestamp, int ssrc, byte[] encodedAudio)
    {
        this.seq = seq;
        this.ssrc = ssrc;
        this.timestamp = timestamp;
        this.encodedAudio = encodedAudio;
        this.type = RTP_PAYLOAD_TYPE;
        this.rawPacket = generateRawPacket(buffer, seq, timestamp, ssrc, encodedAudio);
    }

    public AudioPacket(char seq, int timestamp, int ssrc, byte[] encodedAudio)
    {
        this(null, seq, timestamp, ssrc, encodedAudio);
    }

    public byte[] getHeader()
    {
        return Arrays.copyOf(rawPacket, RTP_HEADER_BYTE_LENGTH);
    }

    private short getShort(byte[] arr, int offset)
    {
        return (short) ((arr[offset] & 0xff) << 8 | arr[offset + 1] & 0xff);
    }

    public char getSequence()
    {
        return seq;
    }

    public int getSSRC()
    {
        return ssrc;
    }

    public int getTimestamp()
    {
        return timestamp;
    }

    public byte[] getEncodedAudio()
    {
        return encodedAudio;
    }

    public byte[] getEncodedAudio(int nonceLength)
    {
        if (nonceLength == 0)
            return encodedAudio;
        return Arrays.copyOf(encodedAudio, encodedAudio.length - nonceLength);
    }

    public byte[] getNoncePadded()
    {
        byte[] nonce = new byte[TweetNaclFast.SecretBox.nonceLength];
        //The first 12 bytes are the rawPacket are the RTP Discord Nonce.
        System.arraycopy(rawPacket, 0, nonce, 0, RTP_HEADER_BYTE_LENGTH);
        return nonce;
    }

    private int getPayloadOffset(byte[] data, int csrcLength)
    {
        final short headerLength = getShort(data, RTP_HEADER_BYTE_LENGTH + 2 + csrcLength);
        int i = RTP_HEADER_BYTE_LENGTH
                + 4
                + csrcLength
                + headerLength * 4;

        while (data[i] == 0)
            i++;
        return i;
    }

    private static byte[] generateRawPacket(ByteBuffer buffer, char seq, int timestamp, int ssrc, byte[] data)
    {
        if (buffer == null)
            buffer = ByteBuffer.allocate(RTP_HEADER_BYTE_LENGTH + data.length);
        populateBuffer(seq, timestamp, ssrc, data, buffer);
        return buffer.array();
    }

    public ByteBuffer asEncryptedPacket(ByteBuffer buffer, byte[] secretKey, byte[] nonce, int nlen)
    {
        byte[] extendedNonce = nonce;
        if (nonce == null)
            extendedNonce = getNoncePadded();

        //Create our SecretBox encoder with the secretKey provided by Discord.
        TweetNaclFast.SecretBox boxer = new TweetNaclFast.SecretBox(secretKey);
        byte[] encryptedAudio = boxer.box(encodedAudio, extendedNonce);
        ((Buffer) buffer).clear();
        int capacity = RTP_HEADER_BYTE_LENGTH + encryptedAudio.length + nlen;
        if (capacity > buffer.remaining())
            buffer = ByteBuffer.allocate(capacity);
        populateBuffer(seq, timestamp, ssrc, encryptedAudio, buffer);
        if (nonce != null)
            buffer.put(nonce, 0, nlen);

        return buffer;
    }

    public static AudioPacket decryptAudioPacket(byte[] buffer, byte[] secretKey)
    {
        TweetNaclFast.SecretBox boxer = new TweetNaclFast.SecretBox(secretKey);
        AudioPacket encryptedPacket = new AudioPacket(buffer);
        if (encryptedPacket.type != RTP_PAYLOAD_TYPE) {
            Log.d("AUDIO PACKET", "Wrong RTP_PAYLOAD_TYPE");
            return null;
        }

        byte[] extendedNonce = encryptedPacket.getNoncePadded();

        final byte[] decryptedAudio = boxer.open(encryptedPacket.rawPacket, extendedNonce);
        if (decryptedAudio == null)
        {
            Log.d("AUDIO PACKET","Failed to decrypt audio packet");
            return null;
        }
        final byte[] decryptedRawPacket = new byte[RTP_HEADER_BYTE_LENGTH + decryptedAudio.length];

        System.arraycopy(encryptedPacket.rawPacket, 0, decryptedRawPacket, 0, RTP_HEADER_BYTE_LENGTH);
        System.arraycopy(decryptedAudio, 0, decryptedRawPacket, RTP_HEADER_BYTE_LENGTH, decryptedAudio.length);

        return new AudioPacket(decryptedRawPacket);
    }

    private static void populateBuffer(char seq, int timestamp, int ssrc, byte[] data, ByteBuffer buffer)
    {
        buffer.put(RTP_VERSION_PAD_EXTEND);
        buffer.put(RTP_PAYLOAD_TYPE);
        buffer.putChar(seq);
        buffer.putInt(timestamp);
        buffer.putInt(ssrc);
        buffer.put(data);
    }
}

package ca.ulaval.ima.mp.gateway.voice;

import android.support.annotation.Nullable;

import java.nio.ByteBuffer;

import ca.ulaval.ima.mp.gateway.crypto.TweetNaclFast;
import io.netty.buffer.ByteBuf;

final class AudioPacket {
    private static final int RTP_HEADER_LENGTH = 12;
    private static final int EXTENDED_RTP_HEADER_LENGTH = 24;

    private final int ssrc;
    private final TweetNaclFast.SecretBox boxer;

    private char seq = 0;

    AudioPacket(int ssrc, TweetNaclFast.SecretBox boxer) {
        this.ssrc = ssrc;
        this.boxer = boxer;
    }

    byte[] nextSend(byte[] audio) {
        byte[] header = getRtpHeader(seq++);
        byte[] encrypted = boxer.box(audio, getNonce(header));

        return getAudioPacket(header, encrypted);
    }

    @Nullable
    byte[] nextReceive(ByteBuf packet) {
        byte[] header = new byte[RTP_HEADER_LENGTH];
        packet.getBytes(0, header);

        int audioOffset = RTP_HEADER_LENGTH + (4 * (byte) (header[0] & 0x0F));

        byte[] encrypted = new byte[packet.readableBytes() - audioOffset];
        packet.getBytes(audioOffset, encrypted);

        byte[] decrypted = boxer.open(encrypted, getNonce(header));
        if (decrypted == null) {
            return null;
        }

        byte[] newPacket = new byte[RTP_HEADER_LENGTH + decrypted.length];
        System.arraycopy(header, 0, newPacket, 0, RTP_HEADER_LENGTH);
        System.arraycopy(decrypted, 0, newPacket, audioOffset, decrypted.length);
        return newPacket;
    }

    private byte[] getNonce(byte[] rtpHeader) {
        byte[] nonce = new byte[EXTENDED_RTP_HEADER_LENGTH];
        System.arraycopy(rtpHeader, 0, nonce, 0, RTP_HEADER_LENGTH);
        return nonce;
    }

    private byte[] getRtpHeader(char seq) {
        return ByteBuffer.allocate(RTP_HEADER_LENGTH)
                .put((byte) 0x80)
                .put((byte) 0x78)
                .putChar(seq)
                .putInt(seq * Opus.CONFIG.FRAME_SIZE)
                .putInt(ssrc)
                .array();
    }

    private static byte[] getAudioPacket(byte[] rtpHeader, byte[] encryptedAudio) {
        byte[] packet = new byte[rtpHeader.length + encryptedAudio.length];
        System.arraycopy(rtpHeader, 0, packet, 0, rtpHeader.length);
        System.arraycopy(encryptedAudio, 0, packet, rtpHeader.length, encryptedAudio.length);
        return packet;
    }

}
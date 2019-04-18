package ca.ulaval.ima.mp.models.gateway.voice;

import java.nio.IntBuffer;

public class Decoder
{
    protected int ssrc;
    protected char lastSeq;
    protected int lastTimestamp;

    protected Decoder(int ssrc)
    {
        this.ssrc = ssrc;
        this.lastSeq = (char) -1;
        this.lastTimestamp = -1;

        IntBuffer error = IntBuffer.allocate(1);
        /*opusDecoder = Opus.INSTANCE.opus_decoder_create(UDPListener.OPUS_SAMPLE_RATE, UDPListener.OPUS_CHANNEL_COUNT, error);
        if (error.get() != Opus.OPUS_OK && opusDecoder == null)
            throw new IllegalStateException("Received error code from opus_decoder_create(...): " + error.get());*/
    }

    protected boolean isInOrder(char newSeq)
    {
        return lastSeq == (char) -1 || newSeq > lastSeq || lastSeq - newSeq > 10;
    }

    protected boolean wasPacketLost(char newSeq)
    {
        return newSeq > lastSeq + 1;
    }

    /*protected short[] decodeFromOpus(AudioPacket decryptedPacket)
    {
        int result;
        ShortBuffer decoded = ShortBuffer.allocate(4096);
        if (decryptedPacket == null)    //Flag for packet-loss
        {
            result = Opus.INSTANCE.opus_decode(opusDecoder, null, 0, decoded, UDPListener.OPUS_FRAME_SIZE, 0);
            lastSeq = (char) -1;
            lastTimestamp = -1;
        }
        else
        {
            this.lastSeq = decryptedPacket.getSequence();
            this.lastTimestamp = decryptedPacket.getTimestamp();

            byte[] encodedAudio = decryptedPacket.getEncodedAudio();

            result = Opus.INSTANCE.opus_decode(opusDecoder, encodedAudio, encodedAudio.length, decoded,
                    UDPListener.OPUS_FRAME_SIZE, 0);
        }

        //If we get a result that is less than 0, then there was an error. Return null as a signifier.
        if (result < 0)
        {
            handleDecodeError(result);
            return null;
        }

        short[] audio = new short[result * 2];
        decoded.get(audio);
        return audio;
    }*/

}
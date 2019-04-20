package ca.ulaval.ima.mp.models.gateway.voice;

import java.nio.ByteBuffer;

/**
 * Used to receive audio.
 * <p>
 * The receiver uses a shared buffer. Keep this in mind when implementing.
 *
 * @see #receive()
 *
 */
public abstract class IAudioReceiveHandler {

    public static final int DEFAULT_BUFFER_SIZE = 2048;
    public static final IAudioReceiveHandler NO_OP = new IAudioReceiveHandler(ByteBuffer.allocate(0)) {
        @Override
        public void receive(char sequence, int timestamp, int ssrc, byte[] audio) {
        }
    };

    private final ByteBuffer buffer;

    public IAudioReceiveHandler() {
        this(ByteBuffer.allocate(DEFAULT_BUFFER_SIZE));
    }

    public IAudioReceiveHandler(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * Called when audio is received. After reading, the implementor is expected to clear the buffer.
     */
    public void receive() {
        getBuffer().get();
        getBuffer().get(); // skip first two bytes
        char sequence = getBuffer().getChar();
        int timestamp = getBuffer().getInt();
        int ssrc = getBuffer().getInt();
        byte[] audio = new byte[getBuffer().remaining()];
        getBuffer().get(audio);

        receive(sequence, timestamp, ssrc, audio);

        getBuffer().clear();
    }

    /**
     * Called when audio is received, automatically extracting useful information.
     * @param sequence The sequence of the packet.
     * @param timestamp The timestamp of the packet.
     * @param ssrc The ssrc of the audio source.
     * @param audio The <a href="https://en.wikipedia.org/wiki/Opus_(audio_format)">Opus</a>-encoded audio.
     */
    public abstract void receive(char sequence, int timestamp, int ssrc, byte[] audio);
}
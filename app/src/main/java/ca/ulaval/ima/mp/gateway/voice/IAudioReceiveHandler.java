package ca.ulaval.ima.mp.gateway.voice;

import java.nio.ByteBuffer;

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

    public abstract void receive(char sequence, int timestamp, int ssrc, byte[] audio);
}
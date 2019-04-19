package ca.ulaval.ima.mp.models.gateway.voice;

import java.nio.ByteBuffer;

public abstract class IAudioSendHandler {

    private static final int DEFAULT_BUFFER_SIZE = AudioPacket.OPUS.FRAME_SIZE;
    public static final IAudioSendHandler NO_OP = new IAudioSendHandler(ByteBuffer.allocate(0)) {
        @Override
        public boolean provide() {
            return false;
        }
    };

    private final ByteBuffer buffer;

    IAudioSendHandler() {
        this(ByteBuffer.allocate(DEFAULT_BUFFER_SIZE));
    }

    IAudioSendHandler(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    ByteBuffer getBuffer() {
        return buffer;
    }

    public abstract boolean provide();
}
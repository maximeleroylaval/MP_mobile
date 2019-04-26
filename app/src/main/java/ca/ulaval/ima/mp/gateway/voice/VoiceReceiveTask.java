package ca.ulaval.ima.mp.gateway.voice;

import io.netty.buffer.ByteBuf;

class VoiceReceiveTask {

    private final IAudioReceiveHandler receiver;
    private final AudioPacket transformer;

    VoiceReceiveTask(IAudioReceiveHandler receiver, AudioPacket transformer) {
        this.receiver = receiver;
        this.transformer = transformer;
    }

    public void run(ByteBuf packet) {
        byte[] buffer = transformer.nextReceive(packet);
        if (buffer != null && buffer.length > 0) {
            receiver.getBuffer().put(buffer);
            receiver.getBuffer().flip();
            receiver.receive();
        }
    }
}
package ca.ulaval.ima.mp.models.gateway.voice;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class VoiceSendTask {

    private final VoiceListener client;
    private final IAudioSendHandler provider;
    private final AudioPacket transformer;

    private boolean speaking = false;

    VoiceSendTask(VoiceListener client, IAudioSendHandler provider, AudioPacket transformer) {
        this.client = client;
        this.provider = provider;
        this.transformer = transformer;
    }

    void run() {
        if (provider.provide()) {
            if (!speaking) {
                changeSpeaking(true);
            }

            byte[] b = new byte[provider.getBuffer().limit()];
            provider.getBuffer().get(b);
            provider.getBuffer().clear();
            ByteBuf packet = Unpooled.wrappedBuffer(transformer.nextSend(b));

            client.voiceSocket.send(packet);
        } else if (speaking) {
            changeSpeaking(false);
        }
    }

    private void changeSpeaking(boolean speaking) {
        VoiceListener.speak(speaking);
        this.speaking = speaking;
    }
}

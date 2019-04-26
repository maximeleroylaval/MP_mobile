package ca.ulaval.ima.mp.gateway.voice;

import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import ca.ulaval.ima.mp.activity.MainActivity;

public class AudioSendHandler extends IAudioSendHandler {
    private Opus opusPlayer = new Opus();

    boolean playOpusFile(File opusFile) {
        try {
            opusPlayer.setup(FileUtils.readFileToByteArray(opusFile));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    void stopPlaying() {
        opusPlayer.setup(new byte[0]);
    }

    @Override
    public boolean provide() {
        byte[] opusChunk = opusPlayer.getPacket();
        this.getBuffer().put(opusChunk);
        if (this.getBuffer().position() > 0) {
            log("OPUS CHUNK LEN :" +  String.valueOf(opusChunk.length));
            log("BUFFER LIMIT :" + String.valueOf(this.getBuffer().limit()));
            this.getBuffer().rewind();
            this.getBuffer().flip();
            this.getBuffer().limit(opusChunk.length);
            return true;
        }
        return false;
    }

    private void log(String txt) {
        if (MainActivity.debug)
            Log.d("[AUDIO]", txt);
    }
}

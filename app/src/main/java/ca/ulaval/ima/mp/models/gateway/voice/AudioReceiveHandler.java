package ca.ulaval.ima.mp.models.gateway.voice;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioReceiveHandler extends IAudioReceiveHandler {
    File file = new File(AudioSendHandler.sdcard, "channel.opus");
    FileOutputStream outStream;

    public AudioReceiveHandler() {
        try {
            outStream = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receive(char sequence, int timestamp, int ssrc, byte[] audio) {
        try {
            outStream.write(audio);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package ca.ulaval.ima.mp.models.gateway.voice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ca.ulaval.ima.mp.FileManager;

public class AudioReceiveHandler extends IAudioReceiveHandler {
    private File file = new File(FileManager.sdcard, "channel.opus");
    private FileOutputStream outStream;

    AudioReceiveHandler() {
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

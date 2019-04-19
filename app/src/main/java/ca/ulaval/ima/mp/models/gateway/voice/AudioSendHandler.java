package ca.ulaval.ima.mp.models.gateway.voice;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ca.ulaval.ima.mp.MainActivity;
import top.oply.opuslib.OpusTool;

public class AudioSendHandler extends IAudioSendHandler {
    private File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    private final File wavFile = new File(sdcard,"sao_a_tender_feeling.wav");
    private final File opusFile = new File(sdcard,"sao_a_tender_feeling.opus");
    private final File outFile = new File(sdcard, "48KHZSTEREO.opus");
    private byte[] encryptedData;

    private int currentOffset = 0;

    private boolean convertFile = false;

    private AudioThread mAudioThread = new AudioThread();
    private boolean mIsStarted;

    AudioSendHandler() {
        this.start();
    }

    private void start() {
        mIsStarted = true;
        mAudioThread = new AudioThread();
        mAudioThread.start();
    }

    private void stop() {
        mAudioThread.interrupt();
        try {
            mAudioThread.join();
        } catch (InterruptedException e) {
            log("Interrupted waiting for audio thread to finish");
        }
        mIsStarted = false;
    }

    // Input must be wav and output opus
    private void convertFileToOpus(File inputFile, File outFile) {
        OpusTool oTool = new OpusTool();
        oTool.encode(inputFile.getAbsolutePath(), outFile.getAbsolutePath(), null);
        //OpusService.play(SDK.mainContext, outFile.getAbsolutePath());
    }

    private void saveChunk(byte[] data) {
        File file = new File(sdcard, "chunk.opus");
        try {
            FileOutputStream stream = new FileOutputStream(file, false);
            stream.write(data);
            stream.close();
        } catch (FileNotFoundException e) {
            log(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] readChunk(File opusFile) {
        if (currentOffset > encryptedData.length) {
            log("Reached end of song");
            return new byte[0];
        }
        byte[] chunkData = new byte[AudioPacket.OPUS.FRAME_SIZE];
        for (int i = 0; i < AudioPacket.OPUS.FRAME_SIZE && i + currentOffset < encryptedData.length; i++) {
            chunkData[i] = encryptedData[currentOffset + i];
        }
        //saveChunk(chunkData);
        currentOffset += AudioPacket.OPUS.FRAME_SIZE;
        return chunkData;
    }

    private byte[] getOpusChunk() {
        if (convertFile) {
            log(wavFile.getAbsolutePath());
            log(opusFile.getAbsolutePath());
            convertFileToOpus(wavFile, opusFile);
            convertFile = false;
        }
        return readChunk(opusFile);
    }

    @Override
    public boolean provide() {
        byte[] opusChunk = this.mAudioThread.getEncBuf();//this.getOpusChunk();
        log("OPUS CHUNK LEN :" +  String.valueOf(opusChunk.length));
        log("BUFFER LIMIT :" + String.valueOf(this.getBuffer().limit()));
        this.getBuffer().put(opusChunk);
        if (this.getBuffer().position() > 0) {
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

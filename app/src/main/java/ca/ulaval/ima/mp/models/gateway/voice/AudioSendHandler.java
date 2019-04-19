package ca.ulaval.ima.mp.models.gateway.voice;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import ca.ulaval.ima.mp.MainActivity;
import ca.ulaval.ima.mp.SDK;
import top.oply.opuslib.OpusService;
import top.oply.opuslib.OpusTool;

public class AudioSendHandler extends IAudioSendHandler {
    private File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    private final File wavFile = new File(sdcard,"sao_a_tender_feeling.wav");
    private final File opusFile = new File(sdcard,"sao_a_tender_feeling.opus");
    private byte[] encryptedData;

    private int currentOffset = 0;

    private boolean convertFile = false;

    public AudioSendHandler() {
        try {
            this.encryptedData = FileUtils.readFileToByteArray(opusFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Input must be wav and output opus
    private void convertFileToOpus(File inputFile, File outFile) {
        OpusTool oTool = new OpusTool();
        oTool.encode(inputFile.getAbsolutePath(), outFile.getAbsolutePath(), null);
        OpusService.play(SDK.mainContext, outFile.getAbsolutePath());
    }

    private byte[] readChunk(File opusFile) {
        if (currentOffset > encryptedData.length) {
            log("Reached end of song");
            return new byte[0];
        }
        byte[] chunkData = new byte[AudioPacket.OPUS.FRAME_SIZE];
        System.arraycopy(encryptedData, currentOffset, chunkData, 0, AudioPacket.OPUS.FRAME_SIZE);
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
        byte[] opusChunk = this.getOpusChunk();
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

package ca.ulaval.ima.mp.models.gateway.voice;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ca.ulaval.ima.mp.MainActivity;
import top.oply.opuslib.OpusTool;

public class AudioSendHandler extends IAudioSendHandler {
    public static File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    private final File wavFile = new File(sdcard,"sao_a_tender_feeling.wav");
    private final File opusFile = new File(sdcard,"sao_a_tender_feeling.opus");
    private final File outFile = new File(sdcard, "audd_final.opus");
    private byte[] rawData;
    private byte[] encryptedData;
    private int position = 0;

    private int currentOffset = 0;
    private int nbPages = 0;
    private int nbPacket = 0;

    private boolean convertFile = false;
    private boolean playFile = true;

    private AudioThread mAudioThread;
    private boolean mIsStarted;

    AudioSendHandler()
    {
        //this.start();
        try {
            rawData = FileUtils.readFileToByteArray(wavFile);
            encryptedData = FileUtils.readFileToByteArray(opusFile);
        } catch (IOException e) {
            log(e.getMessage());
        }
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

    private static long byteAsULong(byte b) {
        return ((long)b) & 0x00000000000000FFL;
    }

    private static long getUInt32(byte[] bytes) {
        return byteAsULong(bytes[0]) | (byteAsULong(bytes[1]) << 8) | (byteAsULong(bytes[2]) << 16) | (byteAsULong(bytes[3]) << 24);
    }

    private long mBitStream = 0;
    private long currentBitsream;
    private String mHead = null;
    private List<Integer> segments = new ArrayList<>();
    private int segmentCount = 0;
    private int currentSegmentCount = 0;
    private List<Byte> frame;

    private boolean readHeader(byte[] buffer) {
        if(buffer.length - position <= 26) {
            return true;
        }

        byte[] name = new byte[4];
        int y = 0;
        for (int i = position; i < position + 4; i++) {
            name[y] = buffer[i];
            y++;
        }

        if(!new String(name).equals("OggS")) {
            log("Invalid ogg magic string");
            return false;
        }

        log("Magic byte : " + new String(name));

        int type = buffer[position + 5];

        log("Type : " + type);

        if(type == 1) {
            log("OGG continued page not supported");
            return false;
        }

        byte[] bitstream = new byte[4];
        y = 0;
        for (int i = position + 14; i < position + 14 + 4; i++) {
            bitstream[y] = buffer[i];
            y++;
        }

        currentBitsream = getUInt32(bitstream);

        log("Bitstream : " + currentBitsream);

        position += 26;

        segmentCount = buffer[position];
        if(buffer.length - position - 1 < segmentCount) {
            log("Error segment size");
            return false;
        }

        log("Segment Count : " + segmentCount);
        segments = new ArrayList<>();
        y = 0;
        byte myByte = 0;
        int size = 0;
        int total = 0;
        int i = 0;
        while (i < segmentCount) {
            position++;
            myByte = buffer[position];
            int uByte = myByte & (0xFF);
            if (uByte < 255) {
                segments.add(size + uByte);
                y++;
                size = 0;
            } else {
                size += uByte;
            }
            total += uByte;
            i++;
        }

        position++;

        if(buffer.length - position < total) {
            log("Error position");
            return false;
        }
        return true;
    }

    private boolean getOpusChunkFile(byte[] buffer) {
        log("Position : " + position);
        position += segments.get(currentSegmentCount);
        log("Position after : " + position);

        int size = segments.get(currentSegmentCount);
        byte[] innerSegment = new byte[size];
        int y = 0;
        for (int i = position - size; i < position && i < buffer.length; i++) {
            innerSegment[y] = buffer[i];
            y++;
        }

        byte[] myTag = new byte[8];
        y = 0;
        for (int i = 0; i < 8; i++) {
            myTag[i] = innerSegment[i];
        }
        String myStrTag = new String(myTag);

        log("Segment tag : " + myStrTag);

        if(mHead != null) {
            if(myStrTag.equals("OpusTags")) {
                //log("Inner segment opus tag : " + new String(innerSegment));
            } else if(mBitStream == currentBitsream) {
                for (int i = 0; i < innerSegment.length; i++) {
                    // send data
                    frame.add(innerSegment[i]);
                }
                log("Packet number :" + nbPacket);
                nbPacket++;
                currentSegmentCount++;
                return true;
            }
        } else if(myStrTag.equals("OpusHead")) {
            log("PACKET " + nbPacket);
            mBitStream = currentBitsream;
            mHead = new String(innerSegment);
        } else {
            log("Invalid codec: " + myStrTag);
        }
        log("Packet number :" + nbPacket);
        nbPacket++;
        currentSegmentCount++;
        return false;
    }

    private byte[] getChunk(List<Byte> frame) {
        byte[] chunk = new byte[frame.size()];
        for (int i = 0; i < chunk.length; i++) {
            chunk[i] = frame.get(i);
        }
        return chunk;
    }

    private byte[] getPacketChunk(byte[] buffer, int size) {
        frame = new ArrayList<>();
        int i = 0;
        for (; i < size; i++) {
            log("I inner bcl : " + i);
            if (position < buffer.length) {
                if (currentSegmentCount < segments.size()) {
                    log("Current segment count : " +  currentSegmentCount);
                    if (!this.getOpusChunkFile(buffer)) {
                        i--;
                    }
                } else {
                    segmentCount = 0;
                    currentSegmentCount = 0;
                    if (this.readHeader(buffer)) {
                        i--;
                        log("SUCCESS");
                        nbPages++;
                    } else {
                        break;
                    }
                }
            } else {
                break;
            }
        }
        log("I:" + i);
        log("Size:" + size);
        if (i == size)
            return this.getChunk(frame);
        return new byte[0];
    }

    @Override
    public boolean provide() {
        byte[] opusChunk = this.getPacketChunk(encryptedData, 1);
        log("Page number parsed : " + nbPages);
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

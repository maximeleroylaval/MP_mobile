package ca.ulaval.ima.mp.models.gateway.voice;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ca.ulaval.ima.mp.MainActivity;
import top.oply.opuslib.OpusTool;

public class Opus {
    static class CONFIG {
        static final int CHANNELS = 2;
        static final int FRAME_TIME = 20; // ms
        static final int SAMPLE_RATE = 48_000; // Hz
        static final int FRAME_SIZE = SAMPLE_RATE / (1000 / FRAME_TIME); // 960 per second
    }

    private long mBitStream = 0;
    private long currentBitstream;
    private String mHead;
    private List<Integer> segments;
    private int segmentCount = 0;
    private int currentSegmentCount = 0;
    private List<Byte> frame;
    private byte[] encryptedData;
    private int position = 0;
    private int nbPacket = 0;
    private int nbPages = 0;

    Opus() {
        setup(new byte[0]);
    }

    void setup(byte[] opusData) {
        encryptedData = opusData;
        position = 0;
        nbPages = 0;
        nbPacket = 0;
        mBitStream = 0;
        currentBitstream = 0;
        segmentCount = 0;
        currentSegmentCount = 0;
        mHead = null;
        segments = new ArrayList<>();
    }

    public static int getChannelCfg() {
        return CONFIG.CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
    }

    // Input must be wav and output opus
    private void convertFileToOpus(File inputFile, File outFile) {
        OpusTool oTool = new OpusTool();
        oTool.encode(inputFile.getAbsolutePath(), outFile.getAbsolutePath(), null);
    }

    private static long byteAsULong(byte b) {
        return ((long)b) & 0x00000000000000FFL;
    }

    private static long getUInt32(byte[] bytes) {
        return byteAsULong(bytes[0]) | (byteAsULong(bytes[1]) << 8) | (byteAsULong(bytes[2]) << 16) | (byteAsULong(bytes[3]) << 24);
    }

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
            log("Invalid ogg magic string : " + new String(name));
            return false;
        }

        log("Magic byte : " + new String(name));

        int type = buffer[position + 5];

        log("Type : " + type);

        if(type == 1) {
            log("OGG continued page not supported");
            return false;
        }

        byte[] bitStream = new byte[4];
        y = 0;
        for (int i = position + 14; i < position + 14 + 4; i++) {
            bitStream[y] = buffer[i];
            y++;
        }

        currentBitstream = getUInt32(bitStream);

        log("Bitstream : " + currentBitstream);

        position += 26;

        segmentCount = buffer[position] & (0xFF);
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
            log("uByte : " + uByte);
            total += uByte;
            i++;
        }

        log("Number of segments : " + segments.size());

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
        for (int i = 0; i < 8 && i < innerSegment.length; i++) {
            myTag[i] = innerSegment[i];
        }
        String myStrTag = new String(myTag);

        log("Segment tag : " + myStrTag);

        if(mHead != null) {
            if(myStrTag.equals("OpusTags")) {
                log("Inner segment opus tag : " + new String(innerSegment));
            } else if(mBitStream == currentBitstream) {
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
            mBitStream = currentBitstream;
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
        log("Page number : " + nbPages);
        if (i == size)
            return this.getChunk(frame);
        return new byte[0];
    }

    public static void requestRecordPermission(Activity activity, Context context) {
        int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    public interface Callback {
        void onSuccess(File file);
        void onFailure(String message);
    }

    byte[] getPacket() {
        return this.getPacketChunk(encryptedData, 1);
    }

    private void log(String txt) {
        if (MainActivity.debug)
            Log.d("[OP]", txt);
    }
}

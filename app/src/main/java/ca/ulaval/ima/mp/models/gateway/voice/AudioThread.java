package ca.ulaval.ima.mp.models.gateway.voice;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import com.score.rahasak.utils.OpusDecoder;
import com.score.rahasak.utils.OpusEncoder;

import java.util.Arrays;

class AudioThread extends Thread {

    private byte[] encBuf = new byte[AudioPacket.OPUS.FRAME_SIZE];

    public byte[] getEncBuf() {
        return encBuf;
    }

    public int getChannelCfg() {
        return AudioPacket.OPUS.CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_FRONT;
    }

    @Override
    public void run() {
        int minBufSize = AudioRecord.getMinBufferSize(AudioPacket.OPUS.SAMPLE_RATE,
                getChannelCfg(),
                AudioFormat.ENCODING_PCM_16BIT);

        if (minBufSize < 0) {
            log("Invalid buffer size : " + minBufSize);
            return;
        }

        // initialize audio recorder
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                AudioPacket.OPUS.SAMPLE_RATE,
                getChannelCfg(),
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize);

        // init opus encoder
        OpusEncoder encoder = new OpusEncoder();
        encoder.init(AudioPacket.OPUS.SAMPLE_RATE, AudioPacket.OPUS.CHANNELS, OpusEncoder.OPUS_APPLICATION_VOIP);

        // init audio track
        AudioTrack track = new AudioTrack(AudioManager.STREAM_SYSTEM,
                AudioPacket.OPUS.SAMPLE_RATE,
                getChannelCfg(),
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize,
                AudioTrack.MODE_STREAM);

        // init opus decoder
        OpusDecoder decoder = new OpusDecoder();
        decoder.init(AudioPacket.OPUS.SAMPLE_RATE, AudioPacket.OPUS.CHANNELS);

        // start
        recorder.startRecording();
        track.play();

        byte[] inBuf = new byte[AudioPacket.OPUS.FRAME_SIZE * AudioPacket.OPUS.CHANNELS * 2];
        short[] outBuf = new short[AudioPacket.OPUS.FRAME_SIZE * AudioPacket.OPUS.CHANNELS];

        try {
            while (!Thread.interrupted()) {
                // Encoder must be fed entire frames.
                int to_read = inBuf.length;
                int offset = 0;
                while (to_read > 0) {
                    int read = recorder.read(inBuf, offset, to_read);
                    if (read < 0) {
                        throw new RuntimeException("recorder.read() returned error " + read);
                    }
                    to_read -= read;
                    offset += read;
                }

                int encoded = encoder.encode(inBuf, AudioPacket.OPUS.FRAME_SIZE, encBuf);

                log("Encoded " + inBuf.length + " bytes of audio into " + encoded + " bytes");

                byte[] encBuf2 = Arrays.copyOf(encBuf, encoded);

                int decoded = decoder.decode(encBuf2, outBuf, AudioPacket.OPUS.FRAME_SIZE);

                log("Decoded back " + decoded * AudioPacket.OPUS.CHANNELS * 2 + " bytes");

                track.write(outBuf, 0, decoded * AudioPacket.OPUS.CHANNELS);
            }
        } finally {
            recorder.stop();
            recorder.release();
            track.stop();
            track.release();
        }
    }

    private void log(String txt) {
        Log.d("AUDIO THREAD", txt);
    }
}

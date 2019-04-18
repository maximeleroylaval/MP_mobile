package ca.ulaval.ima.mp.models.gateway.voice;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public interface AudioSendHandler
{
    int bufSize = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
    AudioTrack INPUT_FORMAT = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT, bufSize, AudioTrack.MODE_STREAM);

    byte[] provide20MsAudio();
}
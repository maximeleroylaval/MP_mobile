package ca.ulaval.ima.mp.gateway.voice;

import ca.ulaval.ima.mp.sdk.models.User;

public class UserAudio
{
    protected User user;
    protected short[] audioData;

    public UserAudio(User user, short[] audioData)
    {
        this.user = user;
        this.audioData = audioData;
    }

    public User getUser()
    {
        return user;
    }

    public byte[] getAudioData(double volume)
    {
        short s;
        int byteIndex = 0;
        byte[] audio = new byte[audioData.length * 2];
        for (int i = 0; i < audioData.length; i++)
        {
            s = audioData[i];
            if (volume != 1.0)
                s = (short) (s * volume);

            byte leftByte = (byte) ((0x000000FF) & (s >> 8));
            byte rightByte =  (byte) (0x000000FF & s);
            audio[byteIndex] = leftByte;
            audio[byteIndex + 1] = rightByte;
            byteIndex += 2;
        }
        return audio;
    }
}
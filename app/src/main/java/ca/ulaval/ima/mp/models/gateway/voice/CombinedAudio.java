package ca.ulaval.ima.mp.models.gateway.voice;

import java.util.Collections;
import java.util.List;

import ca.ulaval.ima.mp.models.User;

public class CombinedAudio
{
    protected List<User> users;
    protected short[] audioData;

    public CombinedAudio(List<User> users, short[] audioData)
    {
        this.users = Collections.unmodifiableList(users);
        this.audioData = audioData;
    }

    public List<User> getUsers()
    {
        return users;
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

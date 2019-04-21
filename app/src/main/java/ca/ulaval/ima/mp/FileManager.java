package ca.ulaval.ima.mp;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;

import ca.ulaval.ima.mp.models.gateway.voice.Opus;
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;

public class FileManager extends FilePickerActivity {
    public static File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    public static void convertToOpus(Context context, final File inputFile, final Opus.Callback callback) {
        final FFmpeg ffmpeg = FFmpeg.getInstance(context);
        if (FFmpeg.getInstance(context).isSupported()) {
            Log.d("[FFMPEG]","Loaded");
            final File outputFile = new File(FileManager.sdcard, inputFile.getName() + ".opus");
            String[] cmd = new String[6];
            cmd[0] = "-y";
            cmd[1] = "-i";
            cmd[2] = inputFile.getAbsolutePath();
            cmd[3] = "-c:a";
            cmd[4] = "libopus";
            cmd[3] = "-frame_size";
            cmd[4] = "20";
            cmd[5] = outputFile.getAbsolutePath();
            Log.d("[FFMPEG]","Trying to convert " + inputFile.getAbsolutePath() + " to " + outputFile.getAbsolutePath());
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    Log.d("[FFMPEG]", "Starting conversion");
                }

                @Override
                public void onProgress(String message) {
                    Log.d("[FFMPEG] Progress : ", message);
                }

                @Override
                public void onFailure(String message) {
                    callback.onFailure(message);
                    Log.d("[FFMPEG]", "Failed conversion");
                }

                @Override
                public void onSuccess(String message) {
                    callback.onSuccess(outputFile);
                    Log.d("[FFMPEG]","File conversion success");
                }

                @Override
                public void onFinish() {
                    Log.d("[FFMPEG]","File conversion finished");
                }
            });
        } else {
            Log.d("[FFMPEG]","Not supported on your device");
        }
    }

    private void log(String txt) {
        if (MainActivity.debug)
            Log.e("[FileManager]", txt);
    }
}

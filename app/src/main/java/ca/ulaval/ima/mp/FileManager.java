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
    public static class CODE {
        public static int PLAY_FILE = 1;
        public static int IMPORT_FILE = 2;
    }

    public interface Callback {
        void onSuccess(File file);
        void onProgress(String message);
        void onFailure(String message);
    }

    public static File sdcard = Environment.getExternalStorageDirectory();
    public static File importedDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    public static void convertToOpus(Context context, final File inputFile, final Callback callback) {
        final FFmpeg ffmpeg = FFmpeg.getInstance(context);
        if (FFmpeg.getInstance(context).isSupported()) {
            String msg = "Setting up file conversion system";
            logConverter(msg);
            callback.onProgress(msg);

            final File outputFile = new File(FileManager.importedDir, getFileName(inputFile) + ".opus");
            String[] cmd = new String[10];
            cmd[0] = "-y";
            cmd[1] = "-i";
            cmd[2] = inputFile.getAbsolutePath();
            cmd[3] = "-c:a";
            cmd[4] = "libopus";
            cmd[5] = "-frame_size";
            cmd[6] = String.valueOf(Opus.CONFIG.FRAME_TIME);
            cmd[7] = "-ar";
            cmd[8] = String.valueOf(Opus.CONFIG.SAMPLE_RATE);
            cmd[9] = outputFile.getAbsolutePath();

            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    String msg = "Starting file conversion of " + inputFile.getAbsolutePath() + " to " + outputFile.getAbsolutePath();
                    logConverter(msg);
                    callback.onProgress(msg);
                }

                @Override
                public void onProgress(String message) {
                    logConverter("Progress : " + message);
                    callback.onProgress(message);
                }

                @Override
                public void onFailure(String message) {
                    logConverter(message);
                    callback.onFailure(message);
                }

                @Override
                public void onSuccess(String message) {
                    logConverter("File conversion success for " + outputFile.getAbsolutePath());
                    callback.onSuccess(outputFile);
                }

                @Override
                public void onFinish() {
                    logConverter("File conversion finished");
                }
            });
        } else {
            String msg = "Could not convert requested file " + inputFile.getAbsolutePath() + " : your device is not supported";
            logConverter(msg);
            callback.onFailure(msg);
        }
    }


    public static String getFileExtension(String fileName) {
        int pos = fileName.lastIndexOf(".");
        if (pos > 0)
            return fileName.substring(pos + 1);
        return "";
    }

    public static String getFileExtension(File file) {
        String fileName = file.getName();
        return getFileExtension(fileName);
    }

    public static String getFileName(String fileName) {
        int pos = fileName.lastIndexOf(".");
        if (pos > 0)
            return fileName.substring(0, pos);
        return "";
    }

    public static String getFileName(File file) {
        String fileName = file.getName();
        return getFileName(fileName);
    }

    private static void logConverter(String txt) {
        if (MainActivity.debug)
            Log.d("[FFMPEG]", txt);
    }

    private void log(String txt) {
        if (MainActivity.debug)
            Log.d("[FileManager]", txt);
    }
}

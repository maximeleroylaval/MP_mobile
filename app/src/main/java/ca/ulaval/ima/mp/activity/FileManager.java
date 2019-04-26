package ca.ulaval.ima.mp.activity;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;

import ca.ulaval.ima.mp.gateway.voice.Opus;
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;

public class FileManager extends FilePickerActivity {
    public static class CODE {
        public static int PLAY_FILE = 1;
        public static int IMPORT_FILE = 2;
    }

    public interface ConvertCallback {
        void onSuccess(File file);
        void onProgress(String message, float percent);
        void onFailure(String message);
    }

    public interface InformationCallback {
        void onSuccess(String message);
        void onProgress(String message);
        void onFailure(String message);
    }

    public static File sdcard = Environment.getExternalStorageDirectory();
    public static File importedDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    public static int getDuration(String timestampStr) {
        String[] tokens = timestampStr.split(":");
        int pos = tokens[2].indexOf(".");
        if (pos > 0) {
            tokens[2] = tokens[2].substring(0, pos);
        }
        int hours = Integer.parseInt(tokens[0]);
        int minutes = Integer.parseInt(tokens[1]);
        int seconds = Integer.parseInt(tokens[2]);
        return 3600 * hours + 60 * minutes + seconds;
    }

    public static void convertToOpus(Context context, final File inputFile, final ConvertCallback callback) {
        final FFmpeg ffmpeg = FFmpeg.getInstance(context);
        if (FFmpeg.getInstance(context).isSupported()) {
            String msg = "Setting up file conversion system";
            logConverter(msg);
            callback.onProgress(msg, 0);

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

                private Integer duration = null;
                private String timeFormat = "HH:mm:ss.SS";

                @Override
                public void onStart() {
                    String msg = "Starting file conversion of " + inputFile.getAbsolutePath() + " to " + outputFile.getAbsolutePath();
                    logConverter(msg);
                    callback.onProgress(msg, 0);
                }

                @Override
                public void onProgress(String message) {
                    float percent = 0;
                    logConverter("Progress : " + message);
                    String durationField = "Duration: ";
                    int posDuration = message.indexOf(durationField);
                    if (posDuration > 0) {
                        posDuration = posDuration + durationField.length();
                        String strDuration = message.substring(posDuration, posDuration + timeFormat.length());
                        duration = getDuration(strDuration);
                    }
                    String timeField = "time=";
                    int posTime = message.indexOf(timeField);
                    if (posTime > 0) {
                        posTime = posTime + timeField.length();
                        String strTime = message.substring(posTime, posTime + timeFormat.length());
                        int time = getDuration(strTime);
                        if (duration != null && duration != 0) {
                            percent = (((float)time / (float)duration) * 100.0f);
                        }
                    }
                    callback.onProgress(message, percent);
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

    public static void getSongInfo(Context context, final File inputFile, final InformationCallback callback) {
        final FFmpeg ffmpeg = FFmpeg.getInstance(context);
        if (FFmpeg.getInstance(context).isSupported()) {
            String msg = "Setting up file information system";
            logConverter(msg);
            callback.onProgress(msg);

            String[] cmd = new String[2];
            cmd[0] = "-i";
            cmd[1] = inputFile.getAbsolutePath();

            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    String msg = "Starting to get file information of " + inputFile.getAbsolutePath();
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
                    int posBegin = message.indexOf("Input #0");
                    int posEnd = message.indexOf("At least one output file must be specified");
                    if (posBegin > 0 && posEnd > 0) {
                        message = message.substring(posBegin, posEnd);
                    }
                    callback.onSuccess(message);
                }

                @Override
                public void onSuccess(String message) {
                    logConverter("Getting file information success for " + inputFile);
                    callback.onSuccess(message);
                }

                @Override
                public void onFinish() {
                    logConverter("File information retrieve finished");
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

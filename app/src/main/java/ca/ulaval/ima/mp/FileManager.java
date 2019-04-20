package ca.ulaval.ima.mp;

import android.os.Environment;
import android.util.Log;

import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;

public class FileManager extends FilePickerActivity {
    public static File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    private void log(String txt) {
        if (MainActivity.debug)
            Log.e("[FileManager]", txt);
    }
}

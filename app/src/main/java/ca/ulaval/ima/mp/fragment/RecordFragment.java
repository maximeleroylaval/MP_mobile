package ca.ulaval.ima.mp.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ca.ulaval.ima.mp.R;
import ca.ulaval.ima.mp.activity.FileManager;
import ca.ulaval.ima.mp.sdk.SDK;

public class RecordFragment extends Fragment {

    private boolean isRecording = false;
    private MediaRecorder mediaRecorder = null;
    private RecordFragment.Listener mListener = null;
    private File file = null;


    public RecordFragment() {
        // Required empty public constructor
    }

    public static RecordFragment newInstance(RecordFragment.Listener l) {
        RecordFragment f = new RecordFragment();
        f.setListener(l);
        return f;
    }

    public void setListener(RecordFragment.Listener mListener) {
        this.mListener = mListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {

        final Button b = view.findViewById(R.id.action_record_button);
        final EditText filename = view.findViewById(R.id.edit_record_name);
        b.setText(getString(R.string.start_record));
        b.setBackgroundResource(R.color.info);
        mediaRecorder = new MediaRecorder();
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) SDK.mainContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                if (filename.getText().toString().equals("")) {
                    new AlertDialog.Builder(view.getContext())
                            .setTitle("Le nom de ne peux pas être vide")
                            .setMessage("Le nom de fichier doit être spécifié, merci de préciser le nom")
                            .setNeutralButton(R.string.details, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else if (!isRecording) {
                    // Record
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    file = new File(FileManager.importedDir.getAbsolutePath() + "/" + filename.getText().toString() + ".mp3");
                    try {
                        FileOutputStream ff = new FileOutputStream(file);
                        mediaRecorder.setOutputFile(ff.getFD());
                        isRecording = true;
                        mediaRecorder.prepare();
                        b.setText(getString(R.string.stop_record));
                        b.setBackgroundResource(R.color.danger);
                        mediaRecorder.start();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    isRecording = false;
                    mediaRecorder.stop();
                    mediaRecorder.reset();
                    b.setText(getString(R.string.start_record));
                    b.setBackgroundResource(R.color.info);
                    if (mListener != null) {
                        mListener.onRecordEnd(file);
                    }
                }
            }
        });

        super.onViewCreated(view, savedInstanceState);
    }


    public static boolean requirePermissions(AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onPause() {
        isRecording = false;
        super.onPause();
    }

    @Override
    public void onDetach() {
        isRecording = false;
        super.onDetach();
    }

    public interface Listener {
        void onRecordEnd(File output);
    }
}

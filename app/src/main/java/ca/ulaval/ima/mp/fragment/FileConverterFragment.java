package ca.ulaval.ima.mp.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import ca.ulaval.ima.mp.R;
import ca.ulaval.ima.mp.activity.FileManager;

public class FileConverterFragment extends Fragment {
    private OnFileConverterFragmentInteractionListener mListener;

    private File inputFile = null;

    public static FileConverterFragment newInstance(String filePath) {
        final FileConverterFragment fragment = new FileConverterFragment();
        if (filePath != null && !filePath.equals("") && !FileManager.getFileExtension(filePath).equals("opus")) {
            fragment.inputFile = new File(filePath);
        }
        return fragment;
    }

    protected void handleConversion(final View view) {
        if (inputFile == null) {
            mListener.onFileConversionFailure("Supplied file is invalid");
            return;
        }
        TextView textView = view.findViewById(R.id.file_input);
        textView.setText(inputFile.getAbsolutePath());
        FileManager.convertToOpus(getContext(), inputFile, new FileManager.ConvertCallback() {
            @Override
            public void onSuccess(File file) {
                ProgressBar progress = view.findViewById(R.id.progress);
                progress.setProgress(100);
                mListener.onFileConversionSuccess(file);
            }

            @Override
            public void onProgress(String message, float percent) {
                TextView textView = view.findViewById(R.id.details);
                textView.append(message + "\n");
                ProgressBar progressBar = view.findViewById(R.id.progress);
                progressBar.setProgress((int)percent);
            }

            @Override
            public void onFailure(String message) {
                TextView textView = view.findViewById(R.id.details);
                textView.setText(message);
                mListener.onFileConversionFailure(message);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_file_convert, container, false);
        this.handleConversion(view);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFileConverterFragmentInteractionListener) {
            mListener = (OnFileConverterFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFileConverterFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFileConverterFragmentInteractionListener {
        void onFileConversionSuccess(File file);
        void onFileConversionFailure(String message);
    }
}

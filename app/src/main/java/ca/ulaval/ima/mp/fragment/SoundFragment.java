package ca.ulaval.ima.mp.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import ca.ulaval.ima.mp.R;
import ca.ulaval.ima.mp.activity.FileManager;
import ca.ulaval.ima.mp.gateway.Gateway;
import ca.ulaval.ima.mp.sdk.SDK;
import ca.ulaval.ima.mp.sdk.models.Channel;

public class SoundFragment extends Fragment {
    private OnSoundFragmentInteractionListener mListener;

    private Channel voiceChannel = null;
    private File inputFile = null;

    public static SoundFragment newInstance(Channel voiceChannel) {
        final SoundFragment fragment = new SoundFragment();
        fragment.voiceChannel = voiceChannel;
        return fragment;
    }

    public static SoundFragment newInstance(File file) {
        final SoundFragment fragment = new SoundFragment();
        fragment.inputFile = file;
        fragment.playFile(file);
        return fragment;
    }

    public void runOnUI(Runnable runner) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runner);
        } else {
            Log.d("[SoundFragment]", "Could not run on ui");
        }
    }

    public void playFile(File file) {
        if (Gateway.voice != null) {
            if (!Gateway.voice.playFile(file))
                SDK.displayMessage("Play error", "Could not send voice data to selected channel", null);
        } else
            SDK.displayMessage("Channel error", "Please select a voice channel",
                    null);
    }

    public void onImportFile() {
        mListener.onImportFile();
    }

    public void onPlayFile() {
        mListener.onPlayFile();
    }

    public void onDisconnect() {
        Gateway.voice.stopPlaying();
        Gateway.server.leaveVoiceChannel();
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroy() {
        onDisconnect();
        super.onDestroy();
    }

    public void onStopFile() {
        Gateway.voice.stopPlaying();
    }

    public void setupView(LayoutInflater inflater, View view) {
        if (inputFile != null) {
            final TextView infoView = view.findViewById(R.id.details);
            FileManager.getSongInfo(getContext(), inputFile, new FileManager.InformationCallback() {
                @Override
                public void onSuccess(String message) {
                    infoView.setText(message);
                }

                @Override
                public void onProgress(String message) {
                    infoView.setText(message);
                }

                @Override
                public void onFailure(String message) {
                    infoView.setText(message);
                }
            });
        }
        Button playButton = view.findViewById(R.id.action_play_file);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayFile();
            }
        });
        Button stopButton = view.findViewById(R.id.action_stop_file);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStopFile();
            }
        });
        Button importButton = view.findViewById(R.id.action_import_file);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onImportFile();
            }
        });
        Button disconnectButton = view.findViewById(R.id.action_voice_disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDisconnect();
            }
        });
    }

    public void setup() {
        if (inputFile != null) {
            this.playFile(inputFile);
        }
        if (voiceChannel != null) {
            Gateway.server.joinVoiceChannel(voiceChannel);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sound, container, false);
        this.setupView(inflater, view);
        this.setup();
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSoundFragmentInteractionListener) {
            mListener = (OnSoundFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSoundFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnSoundFragmentInteractionListener {
        void onPlayFile();
        void onImportFile();
    }
}

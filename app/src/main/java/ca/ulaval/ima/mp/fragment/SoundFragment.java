package ca.ulaval.ima.mp.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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

    private View rootView = null;

    public static SoundFragment newInstance(Channel voiceChannel) {
        final SoundFragment fragment = new SoundFragment();
        fragment.voiceChannel = voiceChannel;
        return fragment;
    }

    public void playFile(File file) {
        if (Gateway.voice != null) {
            if (!Gateway.voice.playFile(file))
                SDK.displayMessage("Play error", "Could not send voice data to selected channel", null);
        } else
            SDK.displayMessage("Channel error", "Please select a voice channel",
                    null);
    }

    public void setInfo(TextView infoView, String message) {
        infoView.setText(message);
    }

    public void onImportFile() {
        mListener.onImportFile();
    }

    public void onPlayFile() {
        mListener.onPlayFile();
    }

    public void onDisconnect(boolean fromDestroy) { mListener.onVoiceDisconnect(fromDestroy); }

    public void onStopFile() {
        Gateway.voice.stopPlaying();
    }

    public void onSearch() {mListener.onSearchFile();}

    public void setupView() {
        ImageButton playButton = rootView.findViewById(R.id.action_play_file);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayFile();
            }
        });
        ImageButton stopButton = rootView.findViewById(R.id.action_stop_file);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStopFile();
            }
        });
        ImageButton importButton = rootView.findViewById(R.id.action_import_file);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onImportFile();
            }
        });
        ImageButton searchButton = rootView.findViewById(R.id.action_search_file);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearch();
            }
        });
        Button disconnectButton = rootView.findViewById(R.id.action_voice_disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDisconnect(false);
            }
        });
        ImageButton recordButton = rootView.findViewById(R.id.action_record);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onRecord();
            }
        });
    }

    public void setFileInfo(File file) {
        final TextView infoView = rootView.findViewById(R.id.details);
        try {
            FileManager.getSongInfo(getContext(), file, new FileManager.InformationCallback() {
                @Override
                public void onSuccess(String message) {
                    setInfo(infoView, message);
                }

                @Override
                public void onProgress(String message) {
                    setInfo(infoView, message);
                }

                @Override
                public void onFailure(String message) {
                    setInfo(infoView, message);
                }
            });
        } catch (Exception e) {
            setInfo(infoView, e.getMessage());
            Log.e("getSongInfo fail", e.getMessage());
            e.printStackTrace();
        }
    }

    public void setActiveChannel(Channel channel) {
        voiceChannel = channel;
        if (getActivity() != null && channel != null && channel.name != null) {
            getActivity().setTitle("@" + channel.name);
        }
        if (voiceChannel != null) {
            Gateway.server.joinVoiceChannel(voiceChannel);
        }
    }

    public void tryToPlayFile(File file) {
        if ((!Gateway.voice.isPlaying() || (inputFile != null && !inputFile.getAbsolutePath().equals(file.getAbsolutePath()))))
            this.playFile(file);
    }

    public void setActiveFile(File file) {
        this.setFile(file, true);
    }

    public void setFile(File file, boolean tryToPlay) {
        if (file != null) {
            this.setFileInfo(file);
            if (tryToPlay)
                this.tryToPlayFile(file);
        }
        inputFile = file;
    }

    @Override
    public void onResume() {
        setFile(inputFile, false);
        super.onResume();
    }

    @Override
    public void onDestroy() {
        onDisconnect(true);
        inputFile = null;
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_sound, container, false);
        this.setupView();
        this.setActiveChannel(voiceChannel);
        this.setFile(inputFile, false);
        return rootView;
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
        void onSearchFile();
        void onRecord();
        void onVoiceDisconnect(boolean fromDestroy);
    }
}

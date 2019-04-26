package ca.ulaval.ima.mp.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ca.ulaval.ima.mp.sdk.JSONHelper;
import ca.ulaval.ima.mp.R;
import ca.ulaval.ima.mp.sdk.models.Channel;
import ca.ulaval.ima.mp.sdk.SDK;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChannelFragment extends Fragment {
    private OnChannelFragmentInteractionListener mListener;

    private List<Channel> channels = new ArrayList<>();

    public static ChannelFragment newInstance() {
        final ChannelFragment fragment = new ChannelFragment();
        return fragment;
    }

    protected void displayChannels(LayoutInflater inflater, View view) {
        boolean firstTextChannel = true;
        LinearLayout linearLayout = view.findViewById(R.id.root);
        for (final Channel channel : channels) {
            if (channel.type == Channel.TYPES.CATEGORY) {
                View categoryView = inflater.inflate(R.layout.content_channel_category, null);
                TextView categoryTextView = categoryView.findViewById(R.id.title);
                categoryTextView.setText(channel.name);
                linearLayout.addView(categoryView);
                for (final Channel innerChannel : channels) {
                    if (channel.id.equals(innerChannel.parentId)) {
                        if (innerChannel.type == Channel.TYPES.GUILD_TEXT) {
                            if (firstTextChannel) {
                                mListener.onFirstTextChannelLoaded(innerChannel);
                                firstTextChannel = false;
                            }
                            View channelView = inflater.inflate(R.layout.content_channel_text, null);
                            TextView textView = channelView.findViewById(R.id.title);
                            textView.setText(innerChannel.name);
                            channelView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mListener.onTextChannelClicked(innerChannel);
                                 }
                            });
                            linearLayout.addView(channelView);
                        } else if (innerChannel.type == Channel.TYPES.GUILD_VOICE) {
                            View channelView = inflater.inflate(R.layout.content_channel_voice, null);
                            TextView textView = channelView.findViewById(R.id.title);
                            textView.setText(innerChannel.name);
                            channelView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mListener.onVoiceChannelClicked(innerChannel);
                                }
                            });
                            linearLayout.addView(channelView);
                        }
                    }
                }
            }
        }
    }

    public void runOnUI(Runnable runner) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runner);
        } else {
            Log.d("[ChannelFragment]", "Could not run on ui");
        }
    }

    public void loadChannels(final LayoutInflater inflater, final View view) {
        SDK.getChannels(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        SDK.displayMessage("Channel error", "Failed to retrieve channels", null);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                channels = Channel.sort(JSONHelper.asArray(Channel.class, response));
                runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        displayChannels(inflater, view);
                    }
                });
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_channel, container, false);
        this.loadChannels(inflater, view);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnChannelFragmentInteractionListener) {
            mListener = (OnChannelFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnChannelFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnChannelFragmentInteractionListener {
        void onFirstTextChannelLoaded(Channel channel);
        void onTextChannelClicked(Channel channel);
        void onVoiceChannelClicked(Channel channel);
    }
}

package ca.ulaval.ima.mp.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ca.ulaval.ima.mp.R;
import ca.ulaval.ima.mp.adapter.MessageAdapter;
import ca.ulaval.ima.mp.gateway.Gateway;
import ca.ulaval.ima.mp.gateway.server.IMessageHandler;
import ca.ulaval.ima.mp.sdk.SDK;
import ca.ulaval.ima.mp.sdk.models.Channel;
import ca.ulaval.ima.mp.sdk.models.Message;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MessageFragment extends Fragment implements IMessageHandler {
    private Channel activeChannel = null;
    private List<Message> messages = new ArrayList<>();

    private View rootView = null;

    private MessageAdapter mAdapter;

    public static MessageFragment newInstance(Channel textChannel) {
        final MessageFragment fragment = new MessageFragment();
        fragment.activeChannel = textChannel;
        return fragment;
    }

    public void runOnUI(Runnable runner) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runner);
        } else {
            Log.d("[MessageFragment]", "Could not run on ui");
        }
    }

    public void sendMessage(View view) {
        if (activeChannel == null) {
            SDK.displayMessage("Channel", "Please select a text channel before sending messages", null);
            return;
        }
        final EditText messageView = view.findViewById(R.id.edit_message);
        String message = messageView.getText().toString();
        SDK.postMessage(message, activeChannel, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        SDK.displayMessage("MESSAGE", "Failed to send your message", null);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        messageView.setText("");
                    }
                });
            }
        });
    }

    protected void loadMessages(Channel channel) {
        if (channel == null) {
            runOnUI(new Runnable() {
                @Override
                public void run() {
                    SDK.displayMessage("Channel", "Please select a text channel before loading messages", null);
                }
            });
            return;
        }
        SDK.getMessages(channel, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        SDK.displayMessage("Message error", "Failed to retrieve messages", null);
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                messages.clear();
                try {
                    JSONArray rawMessages = new JSONArray(response.body().string());
                    for (int i = rawMessages.length() - 1; i > 0; i--) {
                        JSONObject obj = rawMessages.getJSONObject(i);
                        messages.add(new Message(obj));
                    }
                    runOnUI(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setActiveChannel(Channel channel) {
        this.loadMessages(channel);
        if (getActivity() != null && channel != null && channel.name != null) {
            getActivity().setTitle("#" + channel.name);
        }
        activeChannel = channel;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_message, container, false);
        Button button = rootView.findViewById(R.id.send_message);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(rootView);
            }
        });

        RecyclerView recyclerView = rootView.findViewById(R.id.message_list);
        if (recyclerView != null) {
            mAdapter = new MessageAdapter(this.messages);
            recyclerView.setAdapter(mAdapter);
        }
        this.setActiveChannel(this.activeChannel);
        if (Gateway.server != null)
            Gateway.server.setMessageHandler(this);
        return rootView;
    }

    @Override
    public void onMessageReceived(Message message) {
        messages.add(message);
        runOnUI(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemInserted(messages.size() - 1);
                RecyclerView recyclerView = rootView.findViewById(R.id.message_list);
                if (rootView != null) {
                    RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
                    if (manager != null)
                        manager.scrollToPosition(messages.size() - 1);
                }
            }
        });
    }
}

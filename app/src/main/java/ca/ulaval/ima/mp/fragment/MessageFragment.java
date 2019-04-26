package ca.ulaval.ima.mp.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ca.ulaval.ima.mp.sdk.JSONHelper;
import ca.ulaval.ima.mp.R;
import ca.ulaval.ima.mp.sdk.models.Channel;
import ca.ulaval.ima.mp.sdk.models.Message;
import ca.ulaval.ima.mp.sdk.SDK;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MessageFragment extends Fragment {
    private Channel activeChannel = null;
    private List<Message> messages = new ArrayList<>();

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

    protected void loadMessages(final View view) {
        if (activeChannel == null) {
            SDK.displayMessage("Channel", "Please select a text channel before loading messages", null);
            return;
        }
        SDK.getMessages(activeChannel, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        SDK.displayMessage("Message error", "Failed to retreive messages", null);
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                messages = JSONHelper.asArray(Message.class, response);
                for (Message message : messages) {
                    // Display messages
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_message, container, false);
        this.loadMessages(view);
        Button button = view.findViewById(R.id.send_message);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(view);
            }
        });
        return view;
    }
}

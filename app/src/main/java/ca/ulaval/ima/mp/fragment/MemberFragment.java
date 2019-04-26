package ca.ulaval.ima.mp.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ca.ulaval.ima.mp.R;
import ca.ulaval.ima.mp.sdk.JSONHelper;
import ca.ulaval.ima.mp.sdk.SDK;
import ca.ulaval.ima.mp.sdk.models.Guild;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MemberFragment extends Fragment {
    private List<Guild.Member> members = new ArrayList<>();

    public static MemberFragment newInstance() {
        final MemberFragment fragment = new MemberFragment();
        return fragment;
    }

    public void runOnUI(Runnable runner) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runner);
        } else {
            Log.d("[MemberFragment]", "Could not run on ui");
        }
    }

    protected void displayMembers(LayoutInflater inflater, View view) {
        LinearLayout linearLayout = view.findViewById(R.id.root);
        for (Guild.Member member : members) {
            String name = member.nick != null ? member.nick : member.user.username;
            View memberView = inflater.inflate(R.layout.content_member, null);
            TextView textView = memberView.findViewById(R.id.username);
            textView.setText(name);
            Uri avatarURI = member.user.getAvatarURI();
            if (avatarURI != null) {
                ImageView imageView = memberView.findViewById(R.id.avatar);
                Picasso.get().load(avatarURI).into(imageView);
            }
            linearLayout.addView(memberView);
        }
    }

    protected void loadMembers(final LayoutInflater inflater, final View view) {
        SDK.getGuildMembers(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        SDK.displayMessage("Guild Members", "Could not retrieve guild members", null);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                members = JSONHelper.asArray(Guild.Member.class, response);
                runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        displayMembers(inflater, view);
                    }
                });
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_member, container, false);
        this.loadMembers(inflater, view);
        return view;
    }
}

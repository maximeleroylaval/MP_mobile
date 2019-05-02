package ca.ulaval.ima.mp.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import ca.ulaval.ima.mp.R;
import ca.ulaval.ima.mp.sdk.models.Message;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private final List<Message> mValues;

    public MessageAdapter(List<Message> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.content_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (position < mValues.size()) {
            holder.mItem = mValues.get(position);
            holder.mUserView.setText(mValues.get(position).author.username);
            holder.mContentView.setText(mValues.get(position).content);
            Picasso.get().load(mValues.get(position).author.getAvatarURI()).into(holder.mAvatarView);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mAvatarView;
        public final TextView mUserView;
        public final TextView mContentView;
        public Message mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mAvatarView = view.findViewById(R.id.avatar);
            mUserView = view.findViewById(R.id.user);
            mContentView = view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mUserView.getText() + "'" + mContentView.getText() + "'";
        }
    }
}
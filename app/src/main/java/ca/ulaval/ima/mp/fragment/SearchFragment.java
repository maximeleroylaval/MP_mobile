package ca.ulaval.ima.mp.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import ca.ulaval.ima.mp.R;


public class SearchFragment extends Fragment {

    private SearchFragment.Listener listener = null;

    public static SearchFragment newInstance(SearchFragment.Listener l) {
        SearchFragment f = new SearchFragment();
        f.setListener(l);
        return f;
    }


    public void setListener(SearchFragment.Listener l){
        this.listener = l;
    }

    public void search(String search){
        if (listener != null) {
            listener.onSearch(search);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        Button searchButton = view.findViewById(R.id.action_search_button);
        final EditText searchValue = view.findViewById(R.id.edit_search);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search(searchValue.getText().toString());
            }
        });

        super.onViewCreated(view, savedInstanceState);
    }

    public interface Listener {
        void onSearch(String search);
    }
}

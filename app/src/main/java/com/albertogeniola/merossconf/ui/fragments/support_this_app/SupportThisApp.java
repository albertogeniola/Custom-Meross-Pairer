package com.albertogeniola.merossconf.ui.fragments.support_this_app;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.albertogeniola.merossconf.R;

public class SupportThisApp extends Fragment {
        public SupportThisApp() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_support_this_app, container, false);
        TextView gitHubSponsorshipTextView = view.findViewById(R.id.gitHubSponsorshipTextView);
        gitHubSponsorshipTextView.setMovementMethod(LinkMovementMethod.getInstance());

        TextView buyMeCoffeTextView = view.findViewById(R.id.buyMeCoffeTextView);
        buyMeCoffeTextView.setMovementMethod(LinkMovementMethod.getInstance());
        return view;
    }
}
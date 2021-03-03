package com.albertogeniola.merossconf.ui.fragments.pair;

import android.app.TaskStackBuilder;
import android.app.job.JobInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.albertogeniola.merossconf.R;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cdflynn.android.library.checkview.CheckView;


public class PairDoneFragment extends Fragment {
    private CheckView mCheckView;
    private Timer mTimer;

    public PairDoneFragment() {
        mTimer = new Timer();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pair_done, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCheckView = view.findViewById(R.id.pairDoneCheckView);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mCheckView.check();
            }
        }, 1500);
    }
}

package com.albertogeniola.merossconf.ui.fragments.pair;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.R;

import java.util.Timer;
import java.util.TimerTask;

import cdflynn.android.library.checkview.CheckView;


public class PairCompletedFragment extends Fragment {
    private CheckView mCheckView;
    private Timer mTimer;
    private Handler mHandler;
    private Button pairDoneButton;

    public PairCompletedFragment() {
        mHandler = new Handler();
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
        pairDoneButton = view.findViewById(R.id.pairDoneButton);
        pairDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController ctrl = NavHostFragment.findNavController(PairCompletedFragment.this);
                ctrl.popBackStack(R.id.ScanDeviceFragment, false);
                //ctrl.navigate(R.id.PairDone);
            }
        });

        mCheckView = view.findViewById(R.id.pairDoneCheckView);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCheckView.check();
                    }
                });
            }
        }, 1000);
    }
}

package com.albertogeniola.merossconf;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.ProgressBar;

public class PairActivity extends AppCompatActivity implements ProgressableActivity {
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.progressBar = findViewById(R.id.progressBar);
    }

    @Override
    public void setProgressIndeterminate() {
        this.progressBar.setIndeterminate(true);
        this.progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void setProgressDone() {
        this.progressBar.setIndeterminate(true);
        this.progressBar.setVisibility(View.INVISIBLE);
    }
}

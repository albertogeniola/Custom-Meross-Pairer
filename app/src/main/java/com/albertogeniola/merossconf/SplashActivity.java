package com.albertogeniola.merossconf;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.albertogeniola.merossconf.model.HttpClientManager;
import com.albertogeniola.merosslib.MerossHttpClient;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.google.android.material.button.MaterialButton;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SplashActivity extends AppCompatActivity {
    private MaterialButton agreeButton;
    private MaterialButton quitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        agreeButton = findViewById(R.id.acceptButton);
        agreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });
        quitButton = findViewById(R.id.quitButton);
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        hideSystemUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        init();
    }

    private void init() {
        // Configure logging
        if (BuildConfig.DEBUG) {
            ConsoleHandler handler = new ConsoleHandler();
            handler.setLevel(Level.ALL);
            Logger logger = Logger.getLogger("com.albertogeniola.merosslib");
            logger.setLevel(Level.ALL);
            logger.addHandler(handler);
        }

        // Load credentials
        HttpClientManager instance = HttpClientManager.getInstance();
        ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(this);
        if (creds != null)
            instance.loadFromCredentials(creds);
    }

    private void hideSystemUI() {

        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        android.app.ActionBar bar = getActionBar();
        if (bar!=null)
            bar.setDisplayHomeAsUpEnabled(false);

        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}

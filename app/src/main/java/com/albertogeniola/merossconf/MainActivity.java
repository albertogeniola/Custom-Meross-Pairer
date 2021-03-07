package com.albertogeniola.merossconf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.albertogeniola.merossconf.ui.MainActivityViewModel;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.google.android.material.navigation.NavigationView;

import static android.location.LocationManager.PROVIDERS_CHANGED_ACTION;
import static android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private BroadcastReceiver mReceiver;
    private TextView wifiTextView;
    private TextView locationTextView;
    private LinearLayout wifiLocationStatusLayout;

    private MenuItem mPairMenuItem, mDeviceMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final NavigationView navigationView = findViewById(R.id.nav_view);
        mPairMenuItem = navigationView.getMenu().findItem(R.id.pair_activity);
        mDeviceMenuItem = navigationView.getMenu().findItem(R.id.devices_fragment);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        final NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                navController.getGraph())
                .setDrawerLayout(drawer)
                .build();
        final TextView loggedUserTextView = navigationView.getHeaderView(0).findViewById(R.id.navigation_header_logged_email_textview);
        final TextView httpEndpointTextView = navigationView.getHeaderView(0).findViewById(R.id.navigation_header_http_endpoint_textview);

        final MainActivityViewModel mainActivityViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        mainActivityViewModel.getCredentials().observe(this, new Observer<ApiCredentials>() {
            @Override
            public void onChanged(ApiCredentials apiCredentials) {
                loggedUserTextView.setText(apiCredentials==null?"User: not logged":apiCredentials.getUserEmail());
                httpEndpointTextView.setText(apiCredentials==null?"Server: not logged":apiCredentials.getApiServer());
            }
        });

        wifiTextView = findViewById(R.id.wifiOffTextView);
        locationTextView = findViewById(R.id.locationOffTextView);
        wifiLocationStatusLayout = findViewById(R.id.wifiLocationStatusLayout);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Boolean wifiEnabled = null;
                Boolean locationEnabled = null;
                if(intent.getAction().equals(NETWORK_STATE_CHANGED_ACTION) || intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    wifiEnabled = AndroidUtils.isWifiEnabbled(MainActivity.this);
                } else if (intent.getAction().equals(PROVIDERS_CHANGED_ACTION)) {
                    locationEnabled = AndroidUtils.isLocationEnabled(MainActivity.this);
                }
                updateWifiLocationStatusBar(wifiEnabled, locationEnabled);
            }
        };

        // Load the data into the view
        mainActivityViewModel.setCredentials(AndroidPreferencesManager.loadHttpCredentials(this));
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(MainActivity.this);
                NavigationUI.onNavDestinationSelected(menuItem, navController);
                return true;
            }
        });
    }

    private void updateWifiLocationStatusBar(@Nullable Boolean wifiEnabled, @Nullable Boolean locationEnabled) {
        if (wifiEnabled != null)
            wifiTextView.setVisibility(wifiEnabled ? View.GONE : View.VISIBLE);
        if (locationEnabled!=null)
            locationTextView.setVisibility(locationEnabled ? View.GONE : View.VISIBLE);
        wifiLocationStatusLayout.setVisibility(wifiTextView.getVisibility()==View.VISIBLE || locationTextView.getVisibility()==View.VISIBLE ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(PROVIDERS_CHANGED_ACTION);
        this.registerReceiver(this.mReceiver, filter);

        // Update wifi status bar
        boolean wifiEnabled = AndroidUtils.isWifiEnabbled(MainActivity.this);
        boolean locationEnabled = AndroidUtils.isLocationEnabled(MainActivity.this);
        updateWifiLocationStatusBar(wifiEnabled, locationEnabled);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(this.mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(MainActivity.this);

        if (creds == null) {
            mPairMenuItem.setEnabled(false);
            mPairMenuItem.setTitle("Pair (Login required)");
            mDeviceMenuItem.setEnabled(false);
            mDeviceMenuItem.setTitle("Devices (Login required)");
        } else {
            mPairMenuItem.setEnabled(true);
            mPairMenuItem.setTitle("Pair");
            mDeviceMenuItem.setEnabled(true);
            mDeviceMenuItem.setTitle("Devices");
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }
}

package edu.umbc.cs.iot.clients.android.ui.activities;

/**
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 */

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import edu.umbc.cs.iot.clients.android.R;
import edu.umbc.cs.iot.clients.android.UMBCIoTApplication;
import edu.umbc.cs.iot.clients.android.ui.fragments.PrefsFragment;
import edu.umbc.cs.iot.clients.android.ui.fragments.TextQueryFragment;
import edu.umbc.cs.iot.clients.android.ui.fragments.VoiceQueryFragment;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        TextQueryFragment.OnTextQueryFragmentInteractionListener,
        VoiceQueryFragment.OnVoiceQueryFragmentInteractionListener {

    private SharedPreferences sharedPreferences;
    private boolean beaconDisabled;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private GoogleApiClient mGoogleApiClient;
    private String beaconData;
    private MessageListener mMessageListener;

    /**
     * Delay the process of stopping beacon discovery for 100 seconds so that we don't keep on hanging around forever for beacons
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 100000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(UMBCIoTApplication.getSharedPreference(), Context.MODE_PRIVATE);
        beaconDisabled = sharedPreferences.getBoolean(UMBCIoTApplication.getPrefBeaconDisabledTag(), false);

        if(!isBluetoothAvailable()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, UMBCIoTApplication.PERMISSIONS_REQUEST_BLUETOOTH);
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Get a GoogleApiClient object for the using the NearbyMessagesApi
        setGoogleApiClient();
        setListeners();
//        launchFragment(new VoiceQueryFragment());
    }

    @Override
    public void onStart() {
        super.onStart();
        //Initiate connection to Play Services
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient.isConnected()) {
            unsubscribe();
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private void setGoogleApiClient() {
        //Construct a connection to Play Services
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Nearby.MESSAGES_API, new MessagesOptions.Builder()
                            .setPermissions(NearbyPermissions.BLE)
                            .build())
                    .addConnectionCallbacks(this)
                    .enableAutoManage(this, this)
                    .build();
        }
        else
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Nearby.MESSAGES_API)
                    .addConnectionCallbacks(this)
                    .enableAutoManage(this, this)
                    .build();
    }

    private void setListeners() {
        mMessageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                String foundMessage = new String(message.getContent());
                // Do something with the message here.
                Log.i(UMBCIoTApplication.getDebugTag(), "Message string: " + new String(message.getContent()));
                Log.i(UMBCIoTApplication.getDebugTag(), "Message namespace type: " + message.getNamespace() +
                        "/" + message.getType());
                if(message.getNamespace().equals(UMBCIoTApplication.getProjectId())
                        && message.getType().equals("string")
                        && foundMessage.startsWith("MAC")) {
                    beaconData = foundMessage.substring(4);
                    Toast.makeText(getApplicationContext(),"Found: "+beaconData,Toast.LENGTH_SHORT).show();
                    // Only when the beaconData has been found we shall move on to loading the UI
                    launchFragment(new VoiceQueryFragment());
//                    launchFragment(new TextQueryFragment());
                }
            }

            @Override
            public void onLost(Message message) {
                String messageAsString = new String(message.getContent());
                Log.d(UMBCIoTApplication.getDebugTag(), "Lost beacon message: " + messageAsString);
                Toast.makeText(getApplicationContext(), "Lost contact with beacon! I will wait for "+AUTO_HIDE_DELAY_MILLIS+" milliseconds...", Toast.LENGTH_SHORT).show();
                PauseBeaconNotFoundActivity aPauseBeaconNotFoundActivity = new PauseBeaconNotFoundActivity();
                aPauseBeaconNotFoundActivity.execute();
            }
        };
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_clear: {
                /**
                 * TODO This is a bug. The fragment being loaded is a new fragment every time. This might cause memory leakges.
                 * Fix it @prajit
                 */
                launchFragment(new VoiceQueryFragment());
                return true;
            }
            default: {
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            }
        }
    }

//    public void verifyBeaconDataAndLaunch(Fragment fragment) {
//        if(isBeaconDisabled())
//            launchFragment(fragment);
//    }

    private void launchFragment(Fragment fragment) {
        if (beaconData != null) {
            Bundle bundle = new Bundle();
            bundle.putString(UMBCIoTApplication.getBeaconTag(), beaconData);
            fragment.setArguments(bundle);
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.container, fragment)
                    .commit();
        } else
            launchAlternateMainActivity();
    }

    private void launchFragment(Fragment fragment, boolean isPrefFragment) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, fragment)
                .commit();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment = null;
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.search_voice_btn)
            fragment = new VoiceQueryFragment();
        else if (id == R.id.search_text_btn)
            fragment = new TextQueryFragment();
        else if (id == R.id.app_settings_btn)
            fragment = new PrefsFragment();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        // update the main content by replacing fragments
        if (fragment != null) {
            if (fragment instanceof PrefsFragment)
                launchFragment(fragment, true);
            else
                launchFragment(fragment);
//                verifyBeaconDataAndLaunch(fragment);
        } else {
            Log.e(UMBCIoTApplication.getDebugTag(), "Error");
        }

        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        subscribe();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.e(UMBCIoTApplication.getDebugTag(), "GoogleApiClient disconnected with cause: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(this, UMBCIoTApplication.REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                Log.e(UMBCIoTApplication.getDebugTag(), "GoogleApiClient connection failed due to"+e.getMessage());
                Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(UMBCIoTApplication.getDebugTag(), "GoogleApiClient connection failed");
            Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This is called in response to a button tap in the system permissions dialog.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case UMBCIoTApplication.REQUEST_RESOLVE_ERROR: {
                if (resultCode == RESULT_OK) {
                    // Permission granted or error resolved successfully then we proceed
                    // with publish and subscribe..
                    subscribe();
                } else {
                    Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed. Unable to resolve.", Toast.LENGTH_SHORT).show();
                }
            }
            case UMBCIoTApplication.REQUEST_PERMISSION: {
                if (resultCode != RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "We need location permission to get scan results!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            case UMBCIoTApplication.PERMISSIONS_REQUEST_BLUETOOTH: {
                if (resultCode != RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "We need bluetooth enabled to use the nearby messages api!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    // Subscribe to receive messages.
    private void subscribe() {
        Log.i(UMBCIoTApplication.getDebugTag(), "Subscribing.");
        Toast.makeText(getApplicationContext(),"Subscribing!",Toast.LENGTH_SHORT).show();
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .build();
        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options);
    }

    private void unsubscribe() {
        Log.i(UMBCIoTApplication.getDebugTag(), "Unsubscribing.");
        Toast.makeText(getApplicationContext(),"Unsubscribing!",Toast.LENGTH_SHORT).show();
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
    }

    /**
     * Check for Bluetooth.
     * @return True if Bluetooth is available and enabled.
     */
    public boolean isBluetoothAvailable() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth.\nApp won't work without it!", Toast.LENGTH_SHORT).show();
            finish();
        }
        return mBluetoothAdapter.isEnabled();
    }

    private void launchAlternateMainActivity() {
        Intent alternateActivityLaunchIntent = new Intent(getApplicationContext(), AlternateMainActivity.class);
        startActivity(alternateActivityLaunchIntent);
    }

    @Override
    public void onTextQueryFragmentInteraction(Uri uri) {

    }

    @Override
    public void onVoiceQueryFragmentInteraction(Uri uri) {

    }

    public boolean isBeaconDisabled() {
        return beaconDisabled;
    }

    class PauseBeaconNotFoundActivity extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Log.d(UMBCIoTApplication.getDebugTag(), "came into doInBackground"+System.currentTimeMillis());
                Thread.sleep(AUTO_HIDE_DELAY_MILLIS);
                Log.d(UMBCIoTApplication.getDebugTag(), "sleep complete"+System.currentTimeMillis());
                launchAlternateMainActivity();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Log.d(UMBCIoTApplication.getDebugTag(), "Came to onPostCreate");
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
    }

    private final Handler mHideHandler = new Handler();

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        Log.d(UMBCIoTApplication.getDebugTag(), "Came to delayedHide");
        if (beaconData == null) {
            Log.d(UMBCIoTApplication.getDebugTag(), "Came inside delayedHide");
            /**
             * TODO Callback to display beacon not found activity has been removed for the time being in order to ensure demo works.
             * Fix it @prajit
             */
//            mHideHandler.removeCallbacks(mHideRunnable);
//            mHideHandler.postDelayed(mHideRunnable, delayMillis);
        }
    }

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(UMBCIoTApplication.getDebugTag(), "Could not connect to any beacon for "+AUTO_HIDE_DELAY_MILLIS+" milliseconds, going offline...");
            Toast.makeText(getApplicationContext(), "Could not connect to any beacon for "+AUTO_HIDE_DELAY_MILLIS+" milliseconds, going offline...", Toast.LENGTH_SHORT).show();
            launchAlternateMainActivity();
        }
    };
}
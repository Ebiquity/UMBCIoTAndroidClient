package edu.umbc.cs.iot.clients.android.ui.activities;

/**
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 */

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
        TextQueryFragment.OnFragmentInteractionListener,
        VoiceQueryFragment.OnFragmentInteractionListener {

    private NavigationView navigationView;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private GoogleApiClient mGoogleApiClient;
    private String beaconData;
    private MessageListener mMessageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Get a GoogleApiClient object for the using the NearbyMessagesApi
        setGoogleApiClient();
        setListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        //Initiate connection to Play Services
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!isBluetoothAvailable()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, UMBCIoTApplication.PERMISSIONS_REQUEST_BLUETOOTH);
        }
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
                        && foundMessage.startsWith("ITE")) {
                    beaconData = foundMessage;
                    Toast.makeText(getApplicationContext(),"Found: "+beaconData,Toast.LENGTH_LONG).show();
                    // Only when the beaconData has been found we shall move on to loading the UI
                    launchFragment(new VoiceQueryFragment());
                }
            }

            @Override
            public void onLost(Message message) {
                String messageAsString = new String(message.getContent());
                Log.d(UMBCIoTApplication.getDebugTag(), "Lost beacon message: " + messageAsString);
                Toast.makeText(getApplicationContext(), "Lost contact with beacon! I will wait for 100 seconds...", Toast.LENGTH_LONG).show();
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
                if (beaconData != null)
                    launchFragment(new VoiceQueryFragment());
                else
                    launchAlternateMainActivity();
                return true;
            }
            default: {
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            }
        }
    }

    private void launchFragment(Fragment fragment) {
        Bundle bundle = new Bundle();
        bundle.putString(UMBCIoTApplication.getBeaconTag(), beaconData);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, fragment)
                .commit();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment = null;
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.search_voice_btn) {
            fragment = new VoiceQueryFragment();
        } else if (id == R.id.search_text_btn) {
            fragment = new TextQueryFragment();
        } else if (id == R.id.app_settings_btn) {
            fragment = new PrefsFragment();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        // update the main content by replacing fragments
        if (fragment != null) {
            launchFragment(fragment);
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
                Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed due to"+e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e(UMBCIoTApplication.getDebugTag(), "GoogleApiClient connection failed");
            Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed. Unable to resolve.", Toast.LENGTH_LONG).show();
                }
            }
            case UMBCIoTApplication.REQUEST_PERMISSION: {
                if (resultCode != RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "We need location permission to get scan results!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            case UMBCIoTApplication.PERMISSIONS_REQUEST_BLUETOOTH: {
                if (resultCode != RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "We need bluetooth enabled to use the nearby messages api!", Toast.LENGTH_LONG).show();
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
        Toast.makeText(getApplicationContext(),"Subscribing!",Toast.LENGTH_LONG).show();
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .build();
        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options);
    }

    private void unsubscribe() {
        Log.i(UMBCIoTApplication.getDebugTag(), "Unsubscribing.");
        Toast.makeText(getApplicationContext(),"Unsubscribing!",Toast.LENGTH_LONG).show();
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
    }

    /**
     * Check for Bluetooth.
     * @return True if Bluetooth is available and enabled.
     */
    public boolean isBluetoothAvailable() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth.\nApp won't work without it!", Toast.LENGTH_LONG).show();
            finish();
        }
        return mBluetoothAdapter.isEnabled();
    }

    private void launchAlternateMainActivity() {
        Intent alternateActivityLaunchIntent = new Intent(getApplicationContext(), AlternateMainActivity.class);
        startActivity(alternateActivityLaunchIntent);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    class PauseBeaconNotFoundActivity extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Log.d(UMBCIoTApplication.getDebugTag(), "came into doInBackground"+System.currentTimeMillis());
                Thread.sleep(100000);
                Log.d(UMBCIoTApplication.getDebugTag(), "sleep complete"+System.currentTimeMillis());
                launchAlternateMainActivity();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
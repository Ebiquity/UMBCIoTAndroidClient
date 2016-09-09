package edu.umbc.cs.iot.clients.android.ui.activities;

/**
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 * @purpose: The purpose for this code is to setup the Physical Web connection and to obtain the info from the server using the beacon upon successfully connecting to it.
 */

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.util.Calendar;

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

    /**
     * Delay the process of stopping beacon discovery for 100 seconds so that we don't keep on hanging around forever for beacons
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 100000;
    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
//            Log.d(UMBCIoTApplication.getDebugTag(), "Could not connect to any beacon for "+AUTO_HIDE_DELAY_MILLIS+" milliseconds, going offline...");
//            Toast.makeText(getApplicationContext(), "Could not connect to any beacon for "+AUTO_HIDE_DELAY_MILLIS+" milliseconds, going offline...", Toast.LENGTH_SHORT).show();
            launchAlternateMainActivity();
        }
    };
    private SharedPreferences sharedPreferences;
    private boolean beaconDisabled;
    private String userId;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private GoogleApiClient mGoogleApiClient;
    private String beaconData;
    private MessageListener mMessageListener;
    private View headerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(UMBCIoTApplication.getSharedPreference(), Context.MODE_PRIVATE);
        beaconDisabled = sharedPreferences.getBoolean(UMBCIoTApplication.getPrefBeaconDisabledKey(), false);
        userId = sharedPreferences.getString(UMBCIoTApplication.getPrefUserIdKey(), getResources().getString(R.string.pref_user_id_default_value));
//        Toast.makeText(this,userId,Toast.LENGTH_LONG).show();

        if(sharedPreferences.getString(UMBCIoTApplication.getJsonSessionIdKey(),"").isEmpty()) {
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(UMBCIoTApplication.getJsonSessionIdKey(), UMBCIoTApplication.generateRandomSessionId());
            editor.commit();
        }

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

        /**
         * We wanted to show different banner at different times during the day. The following sub-section of the method takes care of that.
         * http://stackoverflow.com/questions/33560219/in-android-how-to-set-navigation-drawer-header-image-and-name-programmatically-i
         * As mentioned in the bug 190226, Since version 23.1.0 getting header layout view with: navigationView.findViewById(R.id.navigation_header_text) no longer works.
         * A workaround is to inflate the headerview programatically and find view by ID from the inflated header view.
         * mNavHeaderMain = (LinearLayout) findViewById(R.id.drawer_view);
         */
        headerView = navigationView.inflateHeaderView(R.layout.nav_header_main);
        headerView.findViewById(R.id.drawer_view);
        Calendar cal = Calendar.getInstance();
        int hourofday = cal.get(Calendar.HOUR_OF_DAY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (hourofday <= 12 && hourofday > 6)
                headerView.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.csee_morning, getTheme()));
            else if (hourofday <= 18 && hourofday > 12)
                headerView.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.csee_afternoon, getTheme()));
            else
                headerView.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.csee_evening, getTheme()));
        } else {
            if (hourofday <= 12 && hourofday > 6)
                headerView.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.csee_morning));
            else if (hourofday <= 18 && hourofday > 12)
                headerView.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.csee_afternoon));
            else
                headerView.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.csee_evening));
        }

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
    public void onStop() {
        if (mGoogleApiClient.isConnected()) {
            unsubscribe();
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private void setGoogleApiClient() {
        //Construct a connection to Play Services
//        Log.d(UMBCIoTApplication.getDebugTag(),"Came into setGoogleApiClient");
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
//                Log.d(UMBCIoTApplication.getDebugTag(),"Came into onFound");
                String foundMessage = new String(message.getContent());
                // Do something with the message here.
//                Log.d(UMBCIoTApplication.getDebugTag(), "Message string: " + new String(message.getContent()));
//                Log.d(UMBCIoTApplication.getDebugTag(), "Message namespace type: " + message.getNamespace() + "/" + message.getType());
                if(message.getNamespace().equals(UMBCIoTApplication.getProjectId())
                        && message.getType().equals("string")
                        && foundMessage.startsWith("MAC")) {
                    beaconData = foundMessage.substring(4);
//                    Toast.makeText(getApplicationContext(),"Found: "+beaconData,Toast.LENGTH_SHORT).show();
                    // Only when the beaconData has been found we shall move on to loading the UI
                    launchFragment(new VoiceQueryFragment(), false);
//                    launchFragment(new TextQueryFragment());
                }
            }

            @Override
            public void onLost(Message message) {
                String messageAsString = new String(message.getContent());
//                Log.d(UMBCIoTApplication.getDebugTag(), "Lost beacon message: " + messageAsString);
//                Toast.makeText(getApplicationContext(), "Lost contact with beacon! I will wait for "+AUTO_HIDE_DELAY_MILLIS+" milliseconds...", Toast.LENGTH_SHORT).show();
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
                launchFragment(new VoiceQueryFragment(), false);
                return true;
            }
            default: {
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            }
        }
    }

    private void launchFragment(Fragment fragment, boolean isPrefFragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if(!isPrefFragment) {
            if (beaconData == null) {
                launchAlternateMainActivity();
                return;
            }
            else {
                Bundle bundle = new Bundle(); //Launch one of the QueryFragments
                bundle.putString(UMBCIoTApplication.getJsonBeaconKey(), beaconData);
                fragment.setArguments(bundle);
            }
        }
        /**
         * From: http://stackoverflow.com/a/18940937/1816861
         * Replace whatever is in the fragment_container view with this fragment,
         * and add the transaction to the back stack if needed
         */
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(null);
        // Commit the transaction
        transaction.commit();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment = new VoiceQueryFragment();
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.search_voice_btn)
            fragment = new VoiceQueryFragment();
        else if (id == R.id.search_text_btn)
            fragment = new TextQueryFragment();
        else if (id == R.id.app_settings_btn)
            fragment = new PrefsFragment();
        else if (id == R.id.help_btn) {
            launchHelpFeedbackActivity();
            return true;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        if (fragment instanceof PrefsFragment)
            launchFragment(fragment, true);
        else
            if(!isBeaconDisabled())
                launchFragment(fragment, false);
        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        subscribe();
    }

    @Override
    public void onConnectionSuspended(int cause) {
//        Log.e(UMBCIoTApplication.getDebugTag(), "GoogleApiClient disconnected with cause: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(this, UMBCIoTApplication.REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
//                Log.e(UMBCIoTApplication.getDebugTag(), "GoogleApiClient connection failed due to"+e.getMessage());
//                Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
//        else {
//            Log.e(UMBCIoTApplication.getDebugTag(), "GoogleApiClient connection failed");
//            Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed", Toast.LENGTH_SHORT).show();
//        }
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
                }
//                else {
//                    Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed. Unable to resolve.", Toast.LENGTH_SHORT).show();
//                }
            }
            case UMBCIoTApplication.REQUEST_PERMISSION: {
                if (resultCode != RESULT_OK) {
//                    Toast.makeText(getApplicationContext(), "We need location permission to get scan results!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            case UMBCIoTApplication.PERMISSIONS_REQUEST_BLUETOOTH: {
                if (resultCode != RESULT_OK) {
//                    Toast.makeText(getApplicationContext(), "We need bluetooth enabled to use the nearby messages api!", Toast.LENGTH_SHORT).show();
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
//        Toast.makeText(getApplicationContext(),"Subscribing!",Toast.LENGTH_SHORT).show();
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .build();
        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options);
//        Log.d(UMBCIoTApplication.getDebugTag(), "Finished subscribing method tasks.");
    }

    private void unsubscribe() {
//        Toast.makeText(getApplicationContext(),"Unsubscribing!",Toast.LENGTH_SHORT).show();
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
//        Log.d(UMBCIoTApplication.getDebugTag(), "Finished unsubscribing method tasks.");
    }

    /**
     * Check for Bluetooth.
     * @return True if Bluetooth is available and enabled.
     */
    public boolean isBluetoothAvailable() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
//            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth.\nApp won't work without it!", Toast.LENGTH_SHORT).show();
            finish();
        }
        return mBluetoothAdapter.isEnabled();
    }

    private void launchAlternateMainActivity() {
        Intent alternateActivityLaunchIntent = new Intent(getApplicationContext(), AlternateMainActivity.class);
        startActivity(alternateActivityLaunchIntent);
    }

    private void launchHelpFeedbackActivity() {
        Intent helpFeedbackActivityLaunchIntent = new Intent(getApplicationContext(), HelpFeedbackActivity.class);
        startActivity(helpFeedbackActivityLaunchIntent);
    }

    @Override
    public void onTextQueryFragmentInteraction(Uri uri) {
//        Log.d(UMBCIoTApplication.getDebugTag(), uri.toString());
    }

    @Override
    public void onVoiceQueryFragmentInteraction(Uri uri) {
//        Log.d(UMBCIoTApplication.getDebugTag(), uri.toString());
    }

    public boolean isBeaconDisabled() {
        return beaconDisabled;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

//        Log.d(UMBCIoTApplication.getDebugTag(), "Came to onPostCreate");
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
//        delayedHide(AUTO_HIDE_DELAY_MILLIS);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
//    private void delayedHide(int delayMillis) {
//        Log.d(UMBCIoTApplication.getDebugTag(), "Came to delayedHide");
//        if (beaconData == null) {
//            Log.d(UMBCIoTApplication.getDebugTag(), "Came inside delayedHide");
            /**
             * TODO Callback to display beacon not found activity has been removed for the time being in order to ensure demo works.
             * Fix it @prajit
             */
//            mHideHandler.removeCallbacks(mHideRunnable);
//            mHideHandler.postDelayed(mHideRunnable, delayMillis);
//        }
//    }

            class PauseBeaconNotFoundActivity extends AsyncTask<Void, Void, Void> {
                protected Void doInBackground(Void... params) {
                    try {
//                Log.d(UMBCIoTApplication.getDebugTag(), "came into doInBackground"+System.currentTimeMillis());
                        Thread.sleep(AUTO_HIDE_DELAY_MILLIS);
//                Log.d(UMBCIoTApplication.getDebugTag(), "sleep complete"+System.currentTimeMillis());
                        launchAlternateMainActivity();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
        }
            }
}
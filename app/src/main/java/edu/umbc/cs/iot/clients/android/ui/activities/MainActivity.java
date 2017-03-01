package edu.umbc.cs.iot.clients.android.ui.activities;

/*
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 * @purpose: The purpose for this code is to setup the Physical Web connection and to obtain the info from the server using the beacon upon successfully connecting to it.
 */

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.BleSignal;
import com.google.android.gms.nearby.messages.Distance;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.util.Calendar;

import edu.umbc.cs.iot.clients.android.BuildConfig;
import edu.umbc.cs.iot.clients.android.R;
import edu.umbc.cs.iot.clients.android.UMBCIoTApplication;
import edu.umbc.cs.iot.clients.android.ui.fragments.EmptyFragment;
import edu.umbc.cs.iot.clients.android.ui.fragments.PrefsFragment;
import edu.umbc.cs.iot.clients.android.ui.fragments.TextQueryFragment;
import edu.umbc.cs.iot.clients.android.ui.fragments.VoiceQueryFragment;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        TextQueryFragment.OnTextQueryFragmentInteractionListener,
        VoiceQueryFragment.OnVoiceQueryFragmentInteractionListener,
        EmptyFragment.OnFragmentInteractionListener {

    private static final int TTL_IN_SECONDS = 3 * 60; // Three minutes.

    /**
     * Sets the time in seconds for a published message or a subscription to live. Set to three
     * minutes in this sample.
     */
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();

    /**
     * Delay the process of stopping beacon discovery for 1 seconds so that we don't keep on hanging around forever for beacons
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 1000;
    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(UMBCIoTApplication.getDebugTag(), "Could not connect to any beacon for " + AUTO_HIDE_DELAY_MILLIS + " milliseconds, going offline...");
//            Toast.makeText(getApplicationContext(), "Could not connect to any beacon for "+AUTO_HIDE_DELAY_MILLIS+" milliseconds, going offline...", Toast.LENGTH_SHORT).show();
            launchAlternateMainActivity();
        }
    };
    private SharedPreferences sharedPreferences;

    private boolean beaconDisabled;
    private String userId;
    private String beaconData;

    private View headerView;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    //    private SwitchCompat mSubscribeSwitch;
    private FrameLayout mContainer;

    private GoogleApiClient mGoogleApiClient;
    private MessageListener mMessageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        sharedPreferences = getSharedPreferences(UMBCIoTApplication.getSharedPreference(), Context.MODE_PRIVATE);

//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
//                if (!sharedPreferences.getBoolean(UMBCIoTApplication.getPrefAlreadyAsked(), false)) {
//                    SharedPreferences.Editor editor = sharedPreferences.edit();
//                    editor.putBoolean(UMBCIoTApplication.getPrefAlreadyAsked(), true);
//                    editor.commit();
//                    Toast.makeText(this, "You denied " + Manifest.permission.ACCESS_FINE_LOCATION + " permission. This might disrupt some functionality!", Toast.LENGTH_SHORT).show();
//                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, UMBCIoTApplication.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
//                } else
//                    createGoogleApiClient();
//            } else {
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, UMBCIoTApplication.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
//            }
//        } else
//            createGoogleApiClient();

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

        mContainer = (FrameLayout) findViewById(R.id.container_main);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        setListeners();

//        mSubscribeSwitch = (SwitchCompat) findViewById(R.id.subscribe_switch);
//        mSubscribeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                // If GoogleApiClient is connected, perform sub actions in response to user action.
//                // If it isn't connected, do nothing, and perform sub actions when it connects (see
//                // onConnected()).
//                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
//                    if (isChecked) {
//                        subscribe();
//                    } else {
//                        unsubscribe();
//                    }
//                }
//            }
//        });

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        /*
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

        launchFragment(new EmptyFragment(), false);
        if (!havePermissions()) {
            Log.i(UMBCIoTApplication.getDebugTag(), "Requesting permissions needed for this app.");
            requestPermissions();
        }
    }

//    private void checkAndSetSubscribedChecked() {
//        if (sharedPreferences.getBoolean(UMBCIoTApplication.getPrefSubscribed(), false))
//            mSubscribeSwitch.setChecked(true);
//        else
//            mSubscribeSwitch.setChecked(false);
//    }

    @Override
    protected void onResume() {
        super.onResume();

//        checkAndSetSubscribedChecked();

        // As part of the permissions workflow, check permissions in case the user has gone to
        // Settings and enabled location there. If permissions are adequate, kick off a subscription
        // process by building GoogleApiClient.
        if (havePermissions()) {
            buildGoogleApiClient();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != UMBCIoTApplication.PERMISSIONS_REQUEST_ALL_REQUIRED) {
            return;
        }
        boolean denied = false;
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                // There are states to watch when a user denies permission when presented with
                // the Nearby permission dialog: 1) When the user pressed "Deny", but does not
                // check the "Never ask again" option. In this case, we display a Snackbar which
                // lets the user kick off the permissions flow again. 2) When the user pressed
                // "Deny" and also checked the "Never ask again" option. In this case, the
                // permission dialog will no longer be presented to the user. The user may still
                // want to authorize location and use the app, and we present a Snackbar that
                // directs them to go to Settings where they can grant the location permission.
                if (shouldShowRequestPermissionRationale(permission)) {
                    Log.i(UMBCIoTApplication.getDebugTag(), "Permission denied without 'NEVER ASK AGAIN': " + permission);
                    showRequestPermissionsSnackbar();
                } else {
                    Log.i(UMBCIoTApplication.getDebugTag(), "Permission denied with 'NEVER ASK AGAIN': " + permission);
                    showLinkToSettingsSnackbar();
                }
                denied = true;
            }
        }
        if (denied)
            return;
        Log.i(UMBCIoTApplication.getDebugTag(), "Permission granted, building GoogleApiClient");
        buildGoogleApiClient();
    }

    /**
     * Displays {@link Snackbar} instructing user to visit Settings to grant permissions required by
     * this application.
     */
    private void showLinkToSettingsSnackbar() {
        if (mContainer == null) {
            return;
        }
        Snackbar.make(mContainer,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Build intent that displays the App settings screen.
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package",
                                BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }).show();
    }

    /**
     * Displays {@link Snackbar} with button for the user to re-initiate the permission workflow.
     */
    private void showRequestPermissionsSnackbar() {
        if (mContainer == null) {
            return;
        }
        Snackbar.make(mContainer, R.string.permission_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Request permission.
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.RECORD_AUDIO},
                                UMBCIoTApplication.PERMISSIONS_REQUEST_ALL_REQUIRED);
                    }
                }).show();
    }

    private synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Nearby.MESSAGES_API, new MessagesOptions.Builder()
                            .setPermissions(NearbyPermissions.BLE)
                            .setPermissions(NearbyPermissions.MICROPHONE)
                            .setPermissions(NearbyPermissions.BLUETOOTH)
                            .build())
                    .addConnectionCallbacks(this)
                    .enableAutoManage(this, this)
                    .build();
        }
    }

//    private void createGoogleApiClient() {
//        if (mGoogleApiClient == null) {
//            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) == PackageManager.PERMISSION_DENIED) {
//                // permission denied, boo! Disable the
//                // functionality that depends on this permission.
//                mGoogleApiClient = new GoogleApiClient.Builder(this)
//                        .addApi(Nearby.MESSAGES_API)
//                        .enableAutoManage(this, this)
//                        .addConnectionCallbacks(this)
////                        .addOnConnectionFailedListener(this)
//                        .build();
//            } else {
//                mGoogleApiClient = new GoogleApiClient.Builder(this)
//                        .addApi(Nearby.MESSAGES_API, new MessagesOptions.Builder()
//                                .setPermissions(NearbyPermissions.BLE)
//                                .build())
//                        .enableAutoManage(this, this)
//                        .addConnectionCallbacks(this)
////                        .addOnConnectionFailedListener(this)
//                        .build();
//                // permission was granted, yay! Do the
//                // contacts-related task you need to do.
//            }
//        }
//    }

    private void setListeners() {
        Log.d(UMBCIoTApplication.getDebugTag(), "Came into setListeners");
        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
                String foundMessage = new String(message.getContent());
                // Do something with the message here.
                Log.d(UMBCIoTApplication.getDebugTag(), "Message string: " + new String(message.getContent()));
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

            /**
             * Called when the Bluetooth Low Energy (BLE) signal associated with a message changes.
             * <p>
             * This is currently only called for BLE beacon messages.
             * <p>
             * For example, this is called when we see the first BLE advertisement
             * frame associated with a message; or when we see subsequent frames with
             * significantly different received signal strength indicator (RSSI)
             * readings.
             * <p>
             * For more information, see the MessageListener Javadocs.
             */
            @Override
            public void onBleSignalChanged(final Message message, final BleSignal bleSignal) {
                Log.d(UMBCIoTApplication.getDebugTag(), "Message: " + message + " has new BLE signal information: " + bleSignal);
            }

            /**
             * Called when Nearby's estimate of the distance to a message changes.
             * <p>
             * This is currently only called for BLE beacon messages.
             * <p>
             * For more information, see the MessageListener Javadocs.
             */
            @Override
            public void onDistanceChanged(final Message message, final Distance distance) {
                Log.d(UMBCIoTApplication.getDebugTag(), "Distance changed, message: " + message + ", new distance: " + distance);
            }

            @Override
            public void onLost(final Message message) {
                String messageAsString = new String(message.getContent());
                Log.d(UMBCIoTApplication.getDebugTag(), "Lost beacon message: " + messageAsString);
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
                if(beaconData != null)
                    launchFragment(new VoiceQueryFragment(), false);
                else
                    launchFragment(new EmptyFragment(), false);
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
            if (beaconData != null) {
                Bundle bundle = new Bundle(); //Launch one of the QueryFragments
                bundle.putString(UMBCIoTApplication.getJsonBeaconKey(), beaconData);
                fragment.setArguments(bundle);
                /*
                 * From: http://stackoverflow.com/a/18940937/1816861
                 * Replace whatever is in the fragment_container view with this fragment,
                 * and add the transaction to the back stack if needed
                 */
                transaction.replace(R.id.container_main, fragment);
            } else {
                transaction.replace(R.id.container_main, new EmptyFragment());
            }
        } else
            transaction.replace(R.id.container_main, fragment);
        transaction.addToBackStack(null);
        // Commit the transaction
        transaction.commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
        else if (!isBeaconDisabled())
            launchFragment(fragment, false);
        return true;
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(UMBCIoTApplication.getDebugTag(), "Disconnecting API client");
            unsubscribe();
        }
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(UMBCIoTApplication.getDebugTag(), "Connected to API client");
//        if (mSubscribeSwitch.isChecked()) {
        subscribe();
//        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
//        mSubscribeSwitch.setEnabled(false);
        logAndShowSnackbar("Exception while connecting to Google Play services: " +
                connectionResult.getErrorMessage());
    }

    @Override
    public void onConnectionSuspended(int i) {
        logAndShowSnackbar("Connection suspended. Error code: " + i);
    }

    private boolean havePermissions() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.RECORD_AUDIO}, UMBCIoTApplication.PERMISSIONS_REQUEST_ALL_REQUIRED);
    }

//    @Override
//    public void onConnectionSuspended(int cause) {
//        Log.d(UMBCIoTApplication.getDebugTag(), "GoogleApiClient disconnected with cause: " + cause);
//    }
//
//    @Override
//    public void onConnectionFailed(ConnectionResult result) {
//        if (result.hasResolution()) {
//            try {
//                result.startResolutionForResult(this, UMBCIoTApplication.REQUEST_RESOLVE_ERROR);
//            } catch (IntentSender.SendIntentException e) {
//                Log.d(UMBCIoTApplication.getDebugTag(), "GoogleApiClient connection failed due to" + e.getMessage());
////                Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        } else {
//            Log.d(UMBCIoTApplication.getDebugTag(), "GoogleApiClient connection failed");
////            Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed", Toast.LENGTH_SHORT).show();
//        }
//    }

    /**
     * Subscribes to messages from nearby devices and updates the UI if the subscription either
     * fails or TTLs.
     */
    private void subscribe() {
        Log.d(UMBCIoTApplication.getDebugTag(), "Subscribing");
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.d(UMBCIoTApplication.getDebugTag(), "No longer subscribing");
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                SharedPreferences.Editor editor = sharedPreferences.edit();
//                                editor.putBoolean(UMBCIoTApplication.getPrefSubscribed(), false);
//                                editor.commit();
//                                mSubscribeSwitch.setChecked(false);
//                            }
//                        });
                    }
                }).build();

        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(UMBCIoTApplication.getPrefSubscribed(), true);
                            editor.commit();
                            Log.d(UMBCIoTApplication.getDebugTag(), "Subscribed successfully.");
                        } else {
                            logAndShowSnackbar("Could not subscribe, status = " + status);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(UMBCIoTApplication.getPrefSubscribed(), false);
                            editor.commit();
//                            mSubscribeSwitch.setChecked(false);
                        }
                    }
                });

//        Toast.makeText(getApplicationContext(),"Subscribing!",Toast.LENGTH_SHORT).show();
//        SubscribeOptions options = new SubscribeOptions.Builder()
//                .setStrategy(Strategy.BLE_ONLY)
//                .build();
//        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options).setResultCallback(new ResultCallback<Status>() {
//            @Override
//            public void onResult(@NonNull Status status) {
//                Log.d(UMBCIoTApplication.getDebugTag(), "Subscription result : " + status.getStatus());
//            }
//        });
//
//        Log.d(UMBCIoTApplication.getDebugTag(), "Finished subscribing method tasks.");
    }

    private void unsubscribe() {
//        Toast.makeText(getApplicationContext(),"Unsubscribing!",Toast.LENGTH_SHORT).show();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(UMBCIoTApplication.getPrefSubscribed(), false);
        editor.commit();
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
        Log.d(UMBCIoTApplication.getDebugTag(), "Finished unsubscribing method tasks.");
    }

    /**
     * Logs a message and shows a {@link Snackbar} using {@code text};
     *
     * @param text The text used in the Log message and the SnackBar.
     */
    private void logAndShowSnackbar(final String text) {
        Log.d(UMBCIoTApplication.getDebugTag(), text);
        View container = findViewById(R.id.container_main);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Log.d(UMBCIoTApplication.getDebugTag(), "Came to onPostCreate");
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
//        delayedHide(AUTO_HIDE_DELAY_MILLIS);
    }

    /**
     * This is called in response to a button tap in the system permissions dialog.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case UMBCIoTApplication.REQUEST_RESOLVE_ERROR: {
                if (resultCode == RESULT_OK) {
                    Log.d(UMBCIoTApplication.getDebugTag(), "User consent granted...");
                    subscribe();
                } else {
                    Log.d(UMBCIoTApplication.getDebugTag(), "GoogleApiClient connection failed. Unable to resolve.");
//                    Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed. Unable to resolve.", Toast.LENGTH_SHORT).show();
                }
            }
            case UMBCIoTApplication.REQUEST_PERMISSION: {
                if (resultCode != RESULT_OK) {
                    Log.d(UMBCIoTApplication.getDebugTag(), "We need location permission to get scan results!");
//                    Toast.makeText(getApplicationContext(), "We need location permission to get scan results!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            case UMBCIoTApplication.PERMISSIONS_REQUEST_BLUETOOTH: {
                if (resultCode != RESULT_OK) {
                    Log.d(UMBCIoTApplication.getDebugTag(), "We need bluetooth enabled to use the nearby messages api!");
//                    Toast.makeText(getApplicationContext(), "We need bluetooth enabled to use the nearby messages api!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Check for Bluetooth.
     * @return True if Bluetooth is available and enabled.
     */
    public boolean isBluetoothAvailable() {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Log.d(UMBCIoTApplication.getDebugTag(), "Device does not support Bluetooth.\\nApp won't work without it!");
//            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth.\nApp won't work without it!", Toast.LENGTH_SHORT).show();
            finish();
        }
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    private void launchAlternateMainActivity() {
        Intent alternateActivityLaunchIntent = new Intent(getApplicationContext(), AlternateMainActivity.class);
        startActivity(alternateActivityLaunchIntent);
    }

    private void launchHelpFeedbackActivity() {
        Intent helpFeedbackActivityLaunchIntent = new Intent(getApplicationContext(), HelpFeedbackActivity.class);
        startActivity(helpFeedbackActivityLaunchIntent);
    }

    public boolean isBeaconDisabled() {
        return beaconDisabled;
    }

    /*
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        Log.d(UMBCIoTApplication.getDebugTag(), "Came to delayedHide");
        if (beaconData == null) {
            Log.d(UMBCIoTApplication.getDebugTag(), "Came inside delayedHide");
            /*
             * DONE Callback to display beacon not found activity has been removed for the time being in order to ensure demo works.
             * Fix it @prajit - done
             */
            mHideHandler.removeCallbacks(mHideRunnable);
            mHideHandler.postDelayed(mHideRunnable, delayMillis);
        }
    }

    @Override
    public void onTextQueryFragmentInteraction(Uri uri) {
        Log.d(UMBCIoTApplication.getDebugTag(), uri.toString());
    }

    @Override
    public void onVoiceQueryFragmentInteraction(Uri uri) {
        Log.d(UMBCIoTApplication.getDebugTag(), uri.toString());
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //TODO do something
    }

    private class PauseBeaconNotFoundActivity extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Log.d(UMBCIoTApplication.getDebugTag(), "came into doInBackground" + System.currentTimeMillis());
                Thread.sleep(AUTO_HIDE_DELAY_MILLIS);
                Log.d(UMBCIoTApplication.getDebugTag(), "sleep complete" + System.currentTimeMillis());
                launchAlternateMainActivity();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
package edu.umbc.cs.iot.clients.android.ui.activities;

/**
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 */

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.app.FragmentTransaction;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
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

import java.util.ArrayList;

import edu.umbc.cs.iot.clients.android.R;
import edu.umbc.cs.iot.clients.android.UMBCIoTApplication;
import edu.umbc.cs.iot.clients.android.service.EddystoneScannerService;
import edu.umbc.cs.iot.clients.android.ui.fragments.QueryFragment;
import edu.umbc.cs.iot.clients.android.util.Beacon;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        ServiceConnection,
        EddystoneScannerService.OnBeaconEventListener {
//        GoogleApiClient.ConnectionCallbacks,
//        GoogleApiClient.OnConnectionFailedListener {

    private NavigationView navigationView;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private QueryFragment aQueryFragment;
//    private GoogleApiClient mGoogleApiClient;
    private String beaconData;
    private FragmentManager fragmentManager;
//    private Message mActiveMessage;
//    private MessageListener mMessageListener;
    private AlertDialog.Builder btEnableDialog;
    private AlertDialog btEnableAlertDialog;
    private AlertDialog.Builder userBtEnableDialog;
    private AlertDialog userBtEnableAlertDialog;
//    private WaitToConnectBeacon aWaitToConnectBeaconObject;
    private static final int EXPIRE_TIMEOUT = 5000;
    private static final int EXPIRE_TASK_PERIOD = 1000;

    private EddystoneScannerService mService;
    private ArrayList<Beacon> listOfBeacons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
            Log.d(UMBCIoTApplication.getDebugTag(),"Have to request permissions");
            // Do something for Marhsmallow and above versions
            getPermissions();
        }

        if(!isBluetoothAvailable()) {
            createAlertDialogForUserActionOnBT();
            userBtEnableAlertDialog = userBtEnableDialog.create();
            userBtEnableAlertDialog.show();
        }

//        aWaitToConnectBeaconObject = new WaitToConnectBeacon();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        aQueryFragment = new QueryFragment();
        fragmentManager = getFragmentManager();

        listOfBeacons = new ArrayList<>();

        // Get a GoogleApiClient object for the using the NearbyMessagesApi
//        setGoogleApiClient();
//        setListeners();
//        aWaitToConnectBeaconObject.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkBluetoothStatus()) {
            Intent intent = new Intent(this, EddystoneScannerService.class);
            bindService(intent, this, BIND_AUTO_CREATE);

            mHandler.post(mPruneTask);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mPruneTask);

        mService.setBeaconEventListener(null);
        unbindService(this);
    }

    /* This task checks for beacons we haven't seen in awhile */
    private Handler mHandler = new Handler();
    private Runnable mPruneTask = new Runnable() {
        @Override
        public void run() {
            final ArrayList<Beacon> expiredBeacons = new ArrayList<>();
            final long now = System.currentTimeMillis();
            for (Beacon beacon : listOfBeacons) {
                long delta = now - beacon.lastDetectedTimestamp;
                if (delta >= EXPIRE_TIMEOUT) {
                    expiredBeacons.add(beacon);
                }
            }

            if (!expiredBeacons.isEmpty()) {
                Log.d(UMBCIoTApplication.getDebugTag(), "Found " + expiredBeacons.size() + " expired");
                listOfBeacons.removeAll(expiredBeacons);
            }

            mHandler.postDelayed(this, EXPIRE_TASK_PERIOD);
        }
    };

    /* Verify Bluetooth Support */
    private boolean checkBluetoothStatus() {
        BluetoothManager manager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (adapter == null || !adapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return false;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        return true;
    }

    /* Handle connection events to the discovery service */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(UMBCIoTApplication.getDebugTag(), "Connected to scanner service");
        mService = ((EddystoneScannerService.LocalBinder) service).getService();
        mService.setBeaconEventListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(UMBCIoTApplication.getDebugTag(), "Disconnected from scanner service");
        mService = null;
    }

    /* Handle callback events from the discovery service */
    @Override
    public void onBeaconIdentifier(String deviceAddress, int rssi, String instanceId) {
        final long now = System.currentTimeMillis();
        for (Beacon item : listOfBeacons) {
            if (instanceId.equals(item.id)) {
                //Already have this one, make sure device info is up to date
                item.update(deviceAddress, rssi, now);
                getBestRssiBeacon();
                defaultFragmentLoad();
                return;
            }
        }

        //New beacon, add it
        Beacon beacon =
                new Beacon(deviceAddress, rssi, instanceId, now);
        listOfBeacons.add(beacon);
    }

    private void getBestRssiBeacon() {
        int bestRssi = 0;
        String deviceAddress = new String();
        for (Beacon item : listOfBeacons) {
            if(bestRssi < item.latestRssi) {
                bestRssi = item.latestRssi;
                deviceAddress = item.deviceAddress;
            }
        }
        beaconData = deviceAddress;
    }

    @Override
    public void onBeaconTelemetry(String deviceAddress, float battery, float temperature) {
        for (Beacon item : listOfBeacons) {
            if (deviceAddress.equals(item.deviceAddress)) {
                //Found it, update voltage
                item.battery = battery;
                item.temperature = temperature;
                return;
            }
        }
    }

    private void getPermissions() {
        /**
         * Check for the following permissions
         * android.permission.INTERNET
         * android.permission.ACCESS_FINE_LOCATION
         * android.permission.BLUETOOTH
         * android.permission.BLUETOOTH_ADMIN
         */
        getInternetAccessPermission();
        getAccessFineLocationPermission();
        getBluetoothPermission();
        getBluetoothAdminPermission();
    }

    private void getInternetAccessPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.INTERNET)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(getApplicationContext(),"Internet access is required to respond to user queries",Toast.LENGTH_LONG).show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.INTERNET},
                        UMBCIoTApplication.PERMISSIONS_REQUEST_INTERNET);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    private void getAccessFineLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(getApplicationContext(),"Location access is required for using the Nearby Messages API",Toast.LENGTH_LONG).show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        UMBCIoTApplication.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    private void getBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.BLUETOOTH)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(getApplicationContext(),"Bluetooth access is required for checking bluetooth state",Toast.LENGTH_LONG).show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH},
                        UMBCIoTApplication.PERMISSIONS_REQUEST_BLUETOOTH);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    private void getBluetoothAdminPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.BLUETOOTH_ADMIN)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(getApplicationContext(),"Bluetooth admin access is required for change bluetooth state",Toast.LENGTH_LONG).show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                        UMBCIoTApplication.PERMISSIONS_REQUEST_BLUETOOTH_ADMIN);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //Initiate connection to Play Services
//        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
//        if (mGoogleApiClient.isConnected()) {
////        unpublish();
//            unsubscribe();
//            mGoogleApiClient.disconnect();
//        }
        super.onStop();
    }

//    private void setGoogleApiClient() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
//            mGoogleApiClient = new GoogleApiClient.Builder(this)
//                    .addApi(Nearby.MESSAGES_API, new MessagesOptions.Builder()
//                            .setPermissions(NearbyPermissions.BLE)
//                            .build())
//                    .addConnectionCallbacks(this)
//                    .enableAutoManage(this, this)
//                    .build();
//        }
//        else
//            mGoogleApiClient = new GoogleApiClient.Builder(this)
//                    .addApi(Nearby.MESSAGES_API)
//                    .addConnectionCallbacks(this)
//                    .enableAutoManage(this, this)
//                    .build();
        //Construct a connection to Play Services
//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .addApi(Nearby.MESSAGES_API)
//                .build();
//    }

    private void setListeners() {
//        mMessageListener = new MessageListener() {
//            @Override
//            public void onFound(Message message) {
//                String foundMessage = new String(message.getContent());
//                // Do something with the message here.
//                Log.i(UMBCIoTApplication.getDebugTag(), "Message string: " + new String(message.getContent()));
//                Log.i(UMBCIoTApplication.getDebugTag(), "Message namespace type: " + message.getNamespace() +
//                        "/" + message.getType());
//                if(message.getNamespace().equals("dark-airway-132523")
//                        && message.getType().equals("string")
//                        && foundMessage.equals("ITE332")) {
//                    beaconData = foundMessage;
//                    // Only when the beaconData has been found we shall move on to loading the UI
//                    defaultFragmentLoad();
//                }
//            }
//
//            @Override
//            public void onLost(Message message) {
//                String messageAsString = new String(message.getContent());
//                Log.d(UMBCIoTApplication.getDebugTag(), "Lost beacon message: " + messageAsString);
//                Toast.makeText(getApplicationContext(), "Lost contact with beacon!", Toast.LENGTH_LONG).show();
//                launchAlternateMainActivity();
//            }
//        };
    }

    private void defaultFragmentLoad() {
        Bundle bundle = new Bundle();
        if(!isFragmentUIActive()) {
//            Toast.makeText(getApplicationContext(),"I have: "+beaconData,Toast.LENGTH_LONG).show();
            bundle.putString(UMBCIoTApplication.getBeaconTag(), beaconData);
            aQueryFragment.setArguments(bundle);
            fragmentManager.beginTransaction().replace(R.id.container, aQueryFragment)
                    .commit();
        } else {
            final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.detach(aQueryFragment);
            fragmentTransaction.attach(aQueryFragment);
            fragmentTransaction.commit();
        }
    }

    public boolean isFragmentUIActive() {
        return aQueryFragment.isAdded() && !aQueryFragment.isDetached() && !aQueryFragment.isRemoving();
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear) {
            if(beaconData != null)
                defaultFragmentLoad();
            else
                launchAlternateMainActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

//    @Override
//    public void onConnected(@Nullable Bundle bundle) {
////        publish("Hello World");
//        subscribe();
//    }
//
//    @Override
//    public void onConnectionSuspended(int cause) {
//        Log.e(UMBCIoTApplication.getDebugTag(), "GoogleApiClient disconnected with cause: " + cause);
//    }
//
//    @Override
//    public void onConnectionFailed(ConnectionResult result) {
//        if (result.hasResolution()) {
//            try {
//                result.startResolutionForResult(this, UMBCIoTApplication.getRequestResolveError());
//            } catch (IntentSender.SendIntentException e) {
//                Log.e(UMBCIoTApplication.getDebugTag(), "GoogleApiClient connection failed due to"+e.getMessage());
//                Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed due to"+e.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        } else {
//            Log.e(UMBCIoTApplication.getDebugTag(), "GoogleApiClient connection failed");
//            Toast.makeText(getApplicationContext(), "GoogleApiClient connection failed", Toast.LENGTH_LONG).show();
//        }
//    }

    /**
     * This is called in response to a button tap in the system permissions dialog.
     */
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == UMBCIoTApplication.getRequestResolveError()) {
//            if (resultCode == RESULT_OK) {
//                // Permission granted or error resolved successfully then we proceed
//                // with publish and subscribe..
//                subscribe();
//            } else {
//                Toast.makeText(getApplicationContext(),"GoogleApiClient connection failed. Unable to resolve.",Toast.LENGTH_LONG).show();
//            }
//        } else {
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//        if (requestCode == UMBCIoTApplication.getRequestPermission()) {
//            if (resultCode != RESULT_OK) {
//                Toast.makeText(getApplicationContext(),"We need location permission to get scan results!",Toast.LENGTH_LONG).show();
//                finish();
//            }
//        }
//    }

//    // Subscribe to receive messages.
//    private void subscribe() {
//        Log.i(UMBCIoTApplication.getDebugTag(), "Subscribing.");
//        Toast.makeText(getApplicationContext(),"Subscribing!",Toast.LENGTH_LONG).show();
//        SubscribeOptions options = new SubscribeOptions.Builder()
//                .setStrategy(Strategy.BLE_ONLY)
//                .build();
//        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options);
//    }
//
//    private void unsubscribe() {
//        Log.i(UMBCIoTApplication.getDebugTag(), "Unsubscribing.");
//        Toast.makeText(getApplicationContext(),"Unsubscribing!",Toast.LENGTH_LONG).show();
//        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
//    }

    // Publishing messages.
//    private void publish(String message) {
//        Log.i(UMBCIoTApplication.getDebugTag(), "Publishing message: " + message);
//        mActiveMessage = new Message(message.getBytes());
//        Nearby.Messages.publish(mGoogleApiClient, mActiveMessage);
//    }

//    private void unpublish() {
//        Log.i(UMBCIoTApplication.getDebugTag(), "Unpublishing.");
//        if (mActiveMessage != null) {
//            Nearby.Messages.unpublish(mGoogleApiClient, mActiveMessage);
//            mActiveMessage = null;
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case UMBCIoTApplication.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                Log.d(UMBCIoTApplication.getDebugTag(),Integer.toString(grantResults.length));
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    Log.d(UMBCIoTApplication.getDebugTag(),"Location access permission was allowed");
                    return;
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(),"Sorry, app needs location access",Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            case UMBCIoTApplication.PERMISSIONS_REQUEST_BLUETOOTH: {
                Log.d(UMBCIoTApplication.getDebugTag(),"Bluetooth access permission was allowed");
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    Log.d(UMBCIoTApplication.getDebugTag(),"Bluetooth access permission was allowed");
                    return;
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(),"Sorry, app needs bluetooth access",Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            case UMBCIoTApplication.PERMISSIONS_REQUEST_BLUETOOTH_ADMIN: {
                Log.d(UMBCIoTApplication.getDebugTag(),"Bluetooth admin access permission was allowed");
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    Log.d(UMBCIoTApplication.getDebugTag(),"Bluetooth admin access permission was allowed");
                    return;
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(),"Sorry, app needs bluetooth admin access",Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            case UMBCIoTApplication.PERMISSIONS_REQUEST_INTERNET: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    Log.d(UMBCIoTApplication.getDebugTag(),"Internet permission was allowed");
                    return;
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(),"Sorry, app needs internet access",Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    private void createAlertDialogForEnablingBT() {
        btEnableDialog = new AlertDialog.Builder(this);
        btEnableDialog.setMessage("Bluetooth is not enabled. You need to enable bluetooth in order to use this app.");
        btEnableDialog.setCancelable(true);

        btEnableDialog.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
//                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//                        bluetoothAdapter.enable();
                    }
                });

        btEnableDialog.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getApplicationContext(), "You cannot use this app without Bluetooth!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void createAlertDialogForUserActionOnBT() {
        userBtEnableDialog = new AlertDialog.Builder(this);
        userBtEnableDialog.setMessage("Bluetooth is not enabled. You need to enable bluetooth in order to use this app. Please enable it and restart the app.");
        userBtEnableDialog.setCancelable(true);

        userBtEnableDialog.setPositiveButton(
                "Okay!",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
//                        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
//                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//                        bluetoothAdapter.enable();
                    }
                });

//        btEnableDialog.setNegativeButton(
//                "No",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        Toast.makeText(getApplicationContext(), "You cannot use this app without Bluetooth!", Toast.LENGTH_LONG).show();
//                        finish();
//                    }
//                });
    }

    /**
     * Check for Bluetooth.
     * @return True if Bluetooth is available.
     */
    public static boolean isBluetoothAvailable() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return (bluetoothAdapter != null && bluetoothAdapter.isEnabled());
    }

    private void enableBluetooth() {
        Log.d(UMBCIoTApplication.getDebugTag(), "came into enableBluetooth");
        if(!isBluetoothAvailable()) {
            Log.d(UMBCIoTApplication.getDebugTag(), "came into isBluetoothAvailable");
            createAlertDialogForEnablingBT();
            btEnableAlertDialog = btEnableDialog.create();
            btEnableAlertDialog.show();
        }
    }

    private void launchAlternateMainActivity() {
        Intent alternateActivityLaunchIntent = new Intent(getApplicationContext(), AlternateMainActivity.class);
        startActivity(alternateActivityLaunchIntent);
    }

//    class WaitToConnectBeacon extends AsyncTask<Void, Void, Void> {
//        protected Void doInBackground(Void... params) {
//            try {
//                Log.d(UMBCIoTApplication.getDebugTag(), "doInBackground");
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//
//        protected void onPostExecute() {
//            launchAlternateMainActivity();
//        }
//    }
}
package edu.umbc.cs.iot.clients.android.ui.activities;

/**
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 */

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.umbc.cs.iot.clients.android.R;
import edu.umbc.cs.iot.clients.android.UMBCIoTApplication;
import edu.umbc.cs.iot.clients.android.service.EddystoneScannerService;
import edu.umbc.cs.iot.clients.android.ui.fragments.QueryFragment;
import edu.umbc.cs.iot.clients.android.util.Beacon;

public class BLEAPIMainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        QueryFragment.OnFragmentInteractionListener{

    private int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
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

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Log.d(UMBCIoTApplication.getDebugTag(),"Have to request permissions");
            // Do something for Marhsmallow and above versions
            getPermissions();
        }

        if(!isBluetoothAvailable()) {
            createAlertDialogForUserActionOnBT();
            userBtEnableAlertDialog = userBtEnableDialog.create();
            userBtEnableAlertDialog.show();
        }

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

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

//        if (checkBluetoothStatus()) {
//            Intent intent = new Intent(this, EddystoneScannerService.class);
//            bindService(intent, this, BIND_AUTO_CREATE);
//
//            mHandler.post(mPruneTask);
//        }

        // Get a GoogleApiClient object for the using the NearbyMessagesApi
//        setGoogleApiClient();
//        setListeners();
//        aWaitToConnectBeaconObject.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);

                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }


    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            Log.i(UMBCIoTApplication.getDebugTag(),result.getDevice().getAddress()+result.getScanRecord().getServiceUuids().toString());
//            if(result.getScanRecord().getServiceUuids().toString().equals(UMBCIoTApplication.getEddystoneUuid())) {
                Toast.makeText(getApplicationContext(), "Discovered EddystoneUUID: " + result.getDevice().getAddress() + " " + result.getRssi() + " " + result.getDevice().fetchUuidsWithSdp() + " " + result.getScanRecord().getServiceUuids().toString(), Toast.LENGTH_LONG).show();
                beaconData = result.getDevice().getAddress();
//                    // Only when the beaconData has been found we shall move on to loading the UI
                defaultFragmentLoad();
//            }
            BluetoothDevice btDevice = result.getDevice();
            connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
//            defaultFragmentLoad();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };

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

    @Override
    public void onFragmentInteraction(Uri uri) {

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
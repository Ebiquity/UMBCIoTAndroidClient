package edu.umbc.cs.iot.clients.android.ui.activities;

/**
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 */

import android.Manifest;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import edu.umbc.cs.iot.clients.android.ui.fragments.QueryFragment;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private NavigationView navigationView;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private QueryFragment aQueryFragment;
    private GoogleApiClient mGoogleApiClient;
    private String beaconData;
    private FragmentManager fragmentManager;
//    private Message mActiveMessage;
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

        aQueryFragment = new QueryFragment();
        fragmentManager = getFragmentManager();

        // Get a GoogleApiClient object for the using the NearbyMessagesApi
        setGoogleApiClient();
        setListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        //Initiate connection to Play Services
        mGoogleApiClient.connect();

        //The location permission is required on API 23+ to obtain BLE scan results
        int result = ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (result != PackageManager.PERMISSION_GRANTED) {
            //Ask for the location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    UMBCIoTApplication.getRequestPermission());
        }
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient.isConnected()) {
//        unpublish();
            unsubscribe();
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private void setGoogleApiClient() {
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
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.MESSAGES_API)
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
                if(message.getNamespace().equals("dark-airway-132523")
                        && message.getType().equals("string")
                        && foundMessage.startsWith("beacon")) {
                    beaconData = foundMessage;
                    // Only when the beaconData has been found we shall move on to loading the UI
                    defaultFragmentLoad();
                }
            }

            @Override
            public void onLost(Message message) {
                String messageAsString = new String(message.getContent());
                Log.d(UMBCIoTApplication.getDebugTag(), "Lost sight of message: " + messageAsString);
            }
        };
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
            defaultFragmentLoad();
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
    public void onConnected(@Nullable Bundle bundle) {
//        publish("Hello World");
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
                result.startResolutionForResult(this, UMBCIoTApplication.getRequestResolveError());
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
        if (requestCode == UMBCIoTApplication.getRequestResolveError()) {
            if (resultCode == RESULT_OK) {
                // Permission granted or error resolved successfully then we proceed
                // with publish and subscribe..
                subscribe();
            } else {
                Toast.makeText(getApplicationContext(),"GoogleApiClient connection failed. Unable to resolve.",Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == UMBCIoTApplication.getRequestPermission()) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(getApplicationContext(),"We need location permission to get scan results!",Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // Subscribe to receive messages.
    private void subscribe() {
        Log.i(UMBCIoTApplication.getDebugTag(), "Subscribing.");
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .build();
        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options);
    }

    private void unsubscribe() {
        Log.i(UMBCIoTApplication.getDebugTag(), "Unsubscribing.");
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
    }

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
}
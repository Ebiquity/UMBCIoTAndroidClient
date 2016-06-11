package edu.umbc.cs.iot.clients.android;

/**
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 */

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

public class UMBCIoTApplication extends Application {
    private static final String DEBUG_TAG = "UMBCIoTDebugTag";
    private static final String PROJECT_ID = "androidclient-umbc";
    private static final String QUESTION_TAG = "question";
    private static final String BEACON_TAG = "beaconid";
    private static final String RESPONSE_TAG = "response";
    private static final String EDDYSTONE_UUID = "0000feaa-0000-1000-8000-00805f9b34fb";
    private static final String url = "http://31230528.ngrok.io/bot";
    public static final int REQUEST_RESOLVE_ERROR = 1001;
    public static final int REQUEST_PERMISSION = 42;
    public static final int PERMISSIONS_REQUEST_INTERNET = 1;
    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2;
    public static final int PERMISSIONS_REQUEST_BLUETOOTH = 3;
    public static final int PERMISSIONS_REQUEST_BLUETOOTH_ADMIN = 4;

    public static String getDebugTag() {
        return DEBUG_TAG;
    }

    public static String getQuestionTag() {
        return QUESTION_TAG;
    }

    public static String getBeaconTag() {
        return BEACON_TAG;
    }

    public static String getResponseTag() {
        return RESPONSE_TAG;
    }

    public static String getUrl() {
        return url;
    }

    public static String getEddystoneUuid() {
        return EDDYSTONE_UUID;
    }

    public static String getProjectId() {
        return PROJECT_ID;
    }
}
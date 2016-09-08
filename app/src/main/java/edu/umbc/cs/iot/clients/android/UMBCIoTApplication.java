package edu.umbc.cs.iot.clients.android;

/**
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 */

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

import java.util.UUID;

public class UMBCIoTApplication extends Application {
    private static final String DEBUG_TAG = "UMBCIoTDebugTag";
    private static final String PROJECT_ID = "androidclient-umbc";
    private static final String QUESTION_TAG = "question";
    private static final String BEACON_TAG = "beaconid";
    private static final String SESSION_ID_TAG = "sessionid";
    private static final String RESPONSE_TAG = "response";
    private static final String EDDYSTONE_UUID = "0000feaa-0000-1000-8000-00805f9b34fb";
    private static final String URL = "http://104.154.36.223/bot";
    public static final int REQUEST_RESOLVE_ERROR = 1;
    public static final int REQUEST_PERMISSION = 2;
    public static final int VOICE_QUERY_RESPONSE = 3;
    public static final int PERMISSIONS_REQUEST_BLUETOOTH = 4;
    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 5;
    public static final int PERMISSIONS_REQUEST_INTERNET = 6;
    public static final int PERMISSIONS_REQUEST_BLUETOOTH_ADMIN = 7;
    private static final String SHARED_PREFERENCE = "UMBC_IOT_APP_SHARED_PREFERENCE";
    private static final String PREF_BEACON_DISABLED_TAG = "beaconDisabledTag";
    private static final String PREF_SESSION_ID_TAG = "sessionIdTag";
    private static final String PREF_USER_ID_TAG = "userIdTag";

    public static String generateRandomSessionId() {
        return UUID.randomUUID().toString();
    }

    public static String getDebugTag() {
        return DEBUG_TAG;
    }

    public static String getProjectId() {
        return PROJECT_ID;
    }

    public static String getQuestionTag() {
        return QUESTION_TAG;
    }

    public static String getBeaconTag() {
        return BEACON_TAG;
    }

    public static String getSessionIdTag() {
        return SESSION_ID_TAG;
    }

    public static String getURL() {
        return URL;
    }

    public static int getRequestResolveError() {
        return REQUEST_RESOLVE_ERROR;
    }

    public static int getPermissionsRequestBluetooth() {
        return PERMISSIONS_REQUEST_BLUETOOTH;
    }

    public static String getSharedPreference() {
        return SHARED_PREFERENCE;
    }

    public static String getPrefBeaconDisabledTag() {
        return PREF_BEACON_DISABLED_TAG;
    }

    public static String getPrefSessionIdTag() {
        return PREF_SESSION_ID_TAG;
    }

    public static String getPrefUserIdTag() {
        return PREF_USER_ID_TAG;
    }
}
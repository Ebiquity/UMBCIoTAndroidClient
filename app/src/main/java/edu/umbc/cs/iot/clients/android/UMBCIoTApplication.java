package edu.umbc.cs.iot.clients.android;

/**
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 */

import android.app.Application;

import java.util.UUID;

public class UMBCIoTApplication extends Application {
    public static final int REQUEST_RESOLVE_ERROR = 1;
    public static final int REQUEST_PERMISSION = 2;
    public static final int VOICE_QUERY_RESPONSE = 3;
    public static final int PERMISSIONS_REQUEST_BLUETOOTH = 4;
    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 5;
    public static final int PERMISSIONS_REQUEST_INTERNET = 6;
    public static final int PERMISSIONS_REQUEST_BLUETOOTH_ADMIN = 7;
    private static final String DEBUG_TAG = "UMBCIoTDebugTag";
    private static final String PROJECT_ID = "androidclient-umbc";
    private static final String JSON_QUESTION_KEY = "question";
    private static final String JSON_BEACON_KEY = "beaconid";
    private static final String JSON_SESSION_ID_KEY = "sessionid";
    private static final String JSON_USER_ID_KEY = "userid";
    private static final String JSON_RESPONSE_KEY = "response";
    private static final String EDDYSTONE_UUID = "0000feaa-0000-1000-8000-00805f9b34fb";
    private static final String URL = "http://104.154.36.223/bot";
    private static final String SHARED_PREFERENCE = "UMBC_IOT_APP_SHARED_PREFERENCE";
    private static final String PREF_BEACON_DISABLED_KEY = "beaconDisabledKey";
    private static final String PREF_ENABLE_USER_ID_KEY = "enableUserIdKey";
    private static final String PREF_USER_ID_KEY = "userIdKey";

    public static String getDebugTag() {
        return DEBUG_TAG;
    }

    public static String getProjectId() {
        return PROJECT_ID;
    }

    public static String getJsonQuestionKey() {
        return JSON_QUESTION_KEY;
    }

    public static String getJsonBeaconKey() {
        return JSON_BEACON_KEY;
    }

    public static String getJsonSessionIdKey() {
        return JSON_SESSION_ID_KEY;
    }

    public static String getJsonUserIdKey() {
        return JSON_USER_ID_KEY;
    }

    public static String getJsonResponseKey() {
        return JSON_RESPONSE_KEY;
    }

    public static String getEddystoneUuid() {
        return EDDYSTONE_UUID;
    }

    public static String getURL() {
        return URL;
    }

    public static String getSharedPreference() {
        return SHARED_PREFERENCE;
    }

    public static String getPrefBeaconDisabledKey() {
        return PREF_BEACON_DISABLED_KEY;
    }

    public static String getPrefEnableUserIdKey() {
        return PREF_ENABLE_USER_ID_KEY;
    }

    public static String getPrefUserIdKey() {
        return PREF_USER_ID_KEY;
    }

    public static String generateRandomSessionId() {
        return UUID.randomUUID().toString();
    }
}
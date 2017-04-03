package edu.umbc.cs.iot.clients.android;

/*
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 */

import android.app.Application;

import java.util.UUID;

public class UMBCIoTApplication extends Application {
    public static final int PERMISSIONS_REQUEST_ALL_REQUIRED = 1;
    public static final int VOICE_QUERY_RESPONSE = 2;
    public static final int PERMISSIONS_REQUEST_BLUETOOTH = 3;

    private static final String DEBUG_TAG = "UMBCIoTDebugTag";
    private static final String PROJECT_ID = "androidclient-umbc";
    private static final String JSON_QUESTION_KEY = "question";
    private static final String JSON_BEACON_KEY = "beaconid";
    private static final String JSON_SESSION_ID_KEY = "sessionid";
    private static final String JSON_USER_ID_KEY = "contextid";//userid will be used later now it is going to be contextid
    private static final String JSON_RESPONSE_KEY = "response";
    private static final String URL = "http://104.154.36.223/bot/";
    private static final String FEEDBACK_URL = "http://104.154.36.223/bot/feedback/";
    private static final String SHARED_PREFERENCE = "UMBC_IOT_APP_SHARED_PREFERENCE";
    private static final String PREF_BEACON_DISABLED_KEY = "beaconDisabledKey";
    private static final String PREF_ENABLE_USER_ID_KEY = "enableUserIdKey";
    private static final String PREF_USER_ID_KEY = "userIdKey";
    private static final String PREF_SUBSCRIBED = "subscribed";

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

    public static String getFeedbackUrl() {
        return FEEDBACK_URL;
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

    public static String getPrefSubscribed() {
        return PREF_SUBSCRIBED;
    }
}
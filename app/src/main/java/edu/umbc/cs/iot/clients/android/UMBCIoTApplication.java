package edu.umbc.cs.iot.clients.android;

import android.app.Application;

public class UMBCIoTApplication extends Application {
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DEBUG_TAG = "UMBCIoTDebugTag";
    private static final String QUESTION_TAG = "question";
    private static final String BEACON_TAG = "beaconid";
    private static final String RESPONSE_TAG = "response";
    private static final String url = "https://botengine-1323.appspot.com/bot";

    public static int getRequestResolveError() {
        return REQUEST_RESOLVE_ERROR;
    }

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
}
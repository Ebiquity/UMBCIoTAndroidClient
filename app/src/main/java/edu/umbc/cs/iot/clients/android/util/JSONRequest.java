package edu.umbc.cs.iot.clients.android.util;

/*
 * Created by prajit on 7/27/2016.
 * @author Prajit Kumar Das
 */

import org.json.JSONException;
import org.json.JSONObject;

import edu.umbc.cs.iot.clients.android.UMBCIoTApplication;

public final class JSONRequest {
    private JSONObject request;

    public JSONObject getRequest() {
        return request;
    }

    public JSONRequest(String aQuery, String aBeasonId, String aSessionId, String aUserId) throws JSONException {
        request = new JSONObject();
        request.put(UMBCIoTApplication.getJsonQuestionKey(), aQuery);
        request.put(UMBCIoTApplication.getJsonBeaconKey(), aBeasonId);
//        request.put(UMBCIoTApplication.getJsonSessionIdKey(), aSessionId);
        request.put(UMBCIoTApplication.getJsonUserIdKey(), aUserId);
    }

    public JSONRequest(boolean someFeedback, String someFeedbackText, String aQuery, String aResponse, String aBeasonId, String aSessionId, String aUserId) throws JSONException {
        request = new JSONObject();
        request.put(UMBCIoTApplication.getJsonFeedbackKey(), someFeedback);
        request.put(UMBCIoTApplication.getJsonFeedbackTextKey(), someFeedbackText);
        request.put(UMBCIoTApplication.getJsonQuestionKey(), aQuery);
        request.put(UMBCIoTApplication.getJsonResponseKey(), aResponse);
        request.put(UMBCIoTApplication.getJsonBeaconKey(), aBeasonId);
//        request.put(UMBCIoTApplication.getJsonSessionIdKey(), aSessionId);
        request.put(UMBCIoTApplication.getJsonUserIdKey(), aUserId);
    }
}
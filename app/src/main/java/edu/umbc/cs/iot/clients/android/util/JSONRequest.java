package edu.umbc.cs.iot.clients.android.util;

/**
 * Created by praji on 7/27/2016.
 * @author Prajit Kumar Das
 */

import org.json.JSONException;
import org.json.JSONObject;

import edu.umbc.cs.iot.clients.android.UMBCIoTApplication;

public final class JSONRequest {
    private JSONObject request;

    public JSONRequest(String aQuery, String aBeasonId, String aSessionId, String aUserId) throws JSONException {
        // Add your data
        //Create JSONObject here
        request = new JSONObject();
        request.put(UMBCIoTApplication.getJsonQuestionKey(), aQuery);
        request.put(UMBCIoTApplication.getJsonBeaconKey(), aBeasonId);
        request.put(UMBCIoTApplication.getJsonSessionIdKey(), aSessionId);
        request.put(UMBCIoTApplication.getJsonUserIdKey(), aUserId);
//        Toast.makeText(view.getContext(),"I have: "+mBeconID,Toast.LENGTH_LONG).show();

//        JSONArray jsonArray = new JSONArray();
//        for(String applicationInfo : getCurrentlyInstalledAppsList()) {
//            jsonArray.put(applicationInfo);
//        	  jsonArray.put("Facebook");
//			  jsonArray.put("Twitter");
//        }
//        jsonParam.put("currentApps",jsonArray);

//        return request.toString();
    }

    public JSONObject getRequest() {
        return request;
    }
}
package edu.umbc.cs.iot.clients.android.ui.fragments;

/**
 * Created on June 11, 2016
 * @author: Prajit Kumar Das
 */

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import edu.umbc.cs.iot.clients.android.R;
import edu.umbc.cs.iot.clients.android.UMBCIoTApplication;
import edu.umbc.cs.iot.clients.android.util.JSONRequest;
import edu.umbc.cs.iot.clients.android.util.VolleySingleton;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VoiceQueryFragment.OnVoiceQueryFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the  factory method to
 * create an instance of this fragment.
 */
public class VoiceQueryFragment extends Fragment implements TextToSpeech.OnInitListener {
//    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//    private String mParam1;
//    private String mParam2;

    private JSONRequest jsonRequest;
    private RequestQueue queue;
    // temporary string to show the parsed response
    private String jsonResponse;
    private TextToSpeech talker;

    private TextView mVoiceFgmtDisplayTextView;
    private ScrollView mVoiceFgmtScrollViewForDisplayText;
    private View view;
    private ImageButton mSendVoiceQueryToServerBtn;

    private String mBeconIDParam;
    private Bundle bundle;
    private String mSessionId;
    private String mUserId;
    private SharedPreferences sharedPreferences;

    private OnVoiceQueryFragmentInteractionListener mListener;

    public VoiceQueryFragment() {
        super();
        // Just to be an empty Bundle. You can use this later with getArguments().set...
        setArguments(new Bundle());
        // Required empty public constructor
    }

//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment VoiceQueryFragment.
//     */
//    // TODO: Rename and change types and number of parameters
//    public static VoiceQueryFragment newInstance(String param1, String param2) {
//        VoiceQueryFragment fragment = new VoiceQueryFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
//        return fragment;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBeconIDParam = getArguments().getString(UMBCIoTApplication.getJsonBeaconKey(), "No beaconID");
            }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_voice_query, container, false);
        initViews();
        initData();
        setOnClickListeners();
        // Inflate the layout for this fragment
        return view;
    }

    private void initViews() {
        mVoiceFgmtDisplayTextView = (TextView) view.findViewById(R.id.voiceFgmtDisplayTextView);
        mVoiceFgmtScrollViewForDisplayText = (ScrollView) view.findViewById(R.id.voiceFgmtScrollViewForDisplayText);
        mSendVoiceQueryToServerBtn = (ImageButton) view.findViewById(R.id.sendVoiceQueryToServerBtn);
    }

    private void setOnClickListeners() {
        mSendVoiceQueryToServerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
                try {
                    startActivityForResult(i, UMBCIoTApplication.VOICE_QUERY_RESPONSE);
                } catch (Exception e) {
//                    Toast.makeText(v.getContext(), "Error initializing speech to text engine.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UMBCIoTApplication.VOICE_QUERY_RESPONSE) {
            if (resultCode == RESULT_OK) {
                ArrayList<String> userSpeech = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                callWebServiceWithQuery(userSpeech.get(0));
            }
        }
    }

    private void initData() {
        bundle = this.getArguments();
        mBeconIDParam = bundle.getString(UMBCIoTApplication.getJsonBeaconKey(), "No beaconID");
        sharedPreferences = getActivity().getSharedPreferences(UMBCIoTApplication.getSharedPreference(), Context.MODE_PRIVATE);
        mSessionId = sharedPreferences.getString(UMBCIoTApplication.getJsonSessionIdKey(),"No sessionID");
        mUserId = sharedPreferences.getString(UMBCIoTApplication.getPrefUserIdKey(), "No userID");
        jsonResponse = new String();
        // Get a RequestQueue
        queue = VolleySingleton.getInstance(view.getContext()).getRequestQueue();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onVoiceQueryFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnVoiceQueryFragmentInteractionListener) {
            mListener = (OnVoiceQueryFragmentInteractionListener) context;
            talker = new TextToSpeech(this.getActivity(), this);
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnVoiceQueryFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if(talker != null)
            talker.shutdown();
    }

    @Override
    public void onInit(int status) {
//        Log.d(UMBCIoTApplication.getDebugTag(), "came into onInit");
        if(status == TextToSpeech.SUCCESS) {
            talker.setLanguage(Locale.getDefault());
            talker.setPitch(1f);
            talker.setSpeechRate(1f);
        }
    }

    private void callWebServiceWithQuery(String query) {
//        Log.d(UMBCIoTApplication.getDebugTag(),"Came to callWebServiceWithQuery");
        // Create a JSONObject for the POST call to the NLP engine server
        try {
//            createJSONObject(query,mBeconIDParam);
            jsonRequest = new JSONRequest(query,mBeconIDParam,mSessionId,mUserId);
        } catch (JSONException aJSONException) {
//            Log.d("JSONException:"," Something went wrong in JSON object creation");
        }
//        Toast.makeText(view.getContext(),"Calling the webservice with url: "+UMBCIoTApplication.getUrl()+" and payload "+jsonObject.toString(),Toast.LENGTH_LONG).show();
        /**
         * Creates a new request.
         * @param method the HTTP method to use
         * @param url URL to fetch the JSON from
         * @param jsonRequest A {@link JSONObject} to post with the request. Null is allowed and
         *   indicates no parameters will be posted along with request.
         * @param listener Listener to receive the JSON response
         * @param errorListener Error listener, or null to ignore errors.
         */
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                Request.Method.POST,
                UMBCIoTApplication.getURL(),
                jsonRequest.getRequest(),
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Parsing json object response
                            // response will be a json object
                            final String status = response.getString("status");
                            final String text = response.getString("text");
//                            JSONObject phone = response.getJSONObject("phone");
//                            String home = phone.getString("home");
//                            String mobile = phone.getString("mobile");

//                            jsonResponse = "";
                            if (!mVoiceFgmtDisplayTextView.getText().equals(view.getContext().getResources().getString(R.string.default_display_text)))
                                jsonResponse += "------------------------" + "\n";
                            jsonResponse +=  "Query parameters were: "
                                    +jsonRequest.getRequest().getString(UMBCIoTApplication.getJsonQuestionKey())
                                    +" "
                                    +jsonRequest.getRequest().get(UMBCIoTApplication.getJsonBeaconKey())
                                    +"\n\n";
                            jsonResponse += "Response is:\nStatus: " + status + " Text: " + text + "\n";
//                            response += "Home: " + home + "\n\n";
//                            response += "Mobile: " + mobile + "\n\n";

//                            Toast.makeText(view.getContext(),"JSON response: "+jsonResponse,Toast.LENGTH_LONG).show();
                            mVoiceFgmtDisplayTextView.setText(jsonResponse);
                            speakThis(text);
                        } catch (JSONException e) {
                            e.printStackTrace();
//                            Toast.makeText(view.getContext(),
//                                    "Error: " + e.getMessage(),
//                                    Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String body = new String();
                        //get status code here
                        String statusCode = String.valueOf(error.networkResponse.statusCode);
//                        Log.d(UMBCIoTApplication.getDebugTag(), "Error status code was: " + statusCode);
//                        Toast.makeText(view.getContext(), "Error status code was: " + statusCode, Toast.LENGTH_LONG).show();
                        try {
                            if (!mVoiceFgmtDisplayTextView.getText().equals(view.getContext().getResources().getString(R.string.default_display_text)))
                                jsonResponse += "------------------------" + "\n";
                            jsonResponse +=  "Query parameters were: "
                                    +jsonRequest.getRequest().getString(UMBCIoTApplication.getJsonQuestionKey())
                                    +" "
                                    +jsonRequest.getRequest().get(UMBCIoTApplication.getJsonBeaconKey())
                                    +"\n\n";
                            jsonResponse += "Getting an error code: " + statusCode + " from the server\n";
//                            response += "Home: " + home + "\n\n";
//                            response += "Mobile: " + mobile + "\n\n";

//                            Toast.makeText(view.getContext(),"JSON response: "+jsonResponse,Toast.LENGTH_LONG).show();
                            mVoiceFgmtDisplayTextView.setText(jsonResponse);
                        } catch (JSONException e) {
                            e.printStackTrace();
//                            Toast.makeText(view.getContext(),
//                                    "Error: " + e.getMessage(),
//                                    Toast.LENGTH_LONG).show();
                        }
                        //get response body and parse with appropriate encoding
                        if(error.networkResponse.data!=null) {
                            try {
                                body = new String(error.networkResponse.data,"UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
//                        Log.d(UMBCIoTApplication.getDebugTag(), "In ErrorListener"+statusCode+body);
                        VolleyLog.d(UMBCIoTApplication.getDebugTag(), "I am here Error: " + error.getMessage());
                        //                Toast.makeText(view.getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Add a request (in this example, called jsObjRequest) to your RequestQueue.
        VolleySingleton.getInstance(view.getContext()).addToRequestQueue(jsObjRequest);
    }

    private void speakThis(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Random random = new Random();
            random.setSeed(System.currentTimeMillis());
            String utteranceId=this.hashCode() + Integer.toString(random.nextInt(Integer.MAX_VALUE));
            talker.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, utteranceId);
        }
//        else {
//            talker.speak(text, TextToSpeech.QUEUE_FLUSH, null);
//        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnVoiceQueryFragmentInteractionListener {
        // TODO: Update argument type and name
        void onVoiceQueryFragmentInteraction(Uri uri);
    }

//    private String createJSONObject(String query, String beacon) throws JSONException {
//        // Add your data
//        //Create JSONObject here
//        jsonObject = new JSONObject();
//        jsonObject.put(UMBCIoTApplication.getQuestionTag(), query);
//        jsonObject.put(UMBCIoTApplication.getBeaconTag(), beacon);
////        Toast.makeText(view.getContext(),"I have: "+mBeconID,Toast.LENGTH_LONG).show();
//
////        JSONArray jsonArray = new JSONArray();
////        for(String applicationInfo : getCurrentlyInstalledAppsList()) {
////            jsonArray.put(applicationInfo);
////        	  jsonArray.put("Facebook");
////			  jsonArray.put("Twitter");
////        }
////        jsonParam.put("currentApps",jsonArray);
//
//        return jsonObject.toString();
//    }
}

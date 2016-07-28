package edu.umbc.cs.iot.clients.android.ui.fragments;

/**
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 */

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import edu.umbc.cs.iot.clients.android.R;
import edu.umbc.cs.iot.clients.android.UMBCIoTApplication;
import edu.umbc.cs.iot.clients.android.util.JSONRequest;
import edu.umbc.cs.iot.clients.android.util.VolleySingleton;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TextQueryFragment.OnTextQueryFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the  factory method to
 * create an instance of this fragment.
 */
public class TextQueryFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_QUESTION = UMBCIoTApplication.getQuestionTag();
//    private static final String ARG_BEACONID = UMBCIoTApplication.getBeaconTag();

    private JSONRequest jsonRequest;
    private RequestQueue queue;
    // temporary string to show the parsed response
    private String jsonResponse;

    private TextView mTextFgmtDisplayTextView;
    private ScrollView mTextFgmtScrollViewForDisplayTextView;
    private EditText mUserQueryEditText;
    private View view;
    private ImageButton mSendTextQueryToServerImageButton;

    private String mBeconIDParam;
    private String mSessionId;
    private SharedPreferences sharedPreferences;

    private OnTextQueryFragmentInteractionListener mListener;

    public TextQueryFragment() {
        super();
        // Just to be an empty Bundle. You can use this later with getArguments().set...
        setArguments(new Bundle());
        // Required empty public constructor
    }

//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param beaconIDParam Parameter 2.
//     * @return A new instance of fragment TextQueryFragment.
//     */
//    public static TextQueryFragment newInstance(String beaconIDParam){
//        TextQueryFragment fragment = new TextQueryFragment();
//        Bundle args = new Bundle();
////        args.putString(ARG_QUESTION, quesionParam);
//        args.putString(ARG_BEACONID, beaconIDParam);
//        fragment.setArguments(args);
//        return fragment;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBeconIDParam = getArguments().getString(UMBCIoTApplication.getBeaconTag(), "No beaconID");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_text_query, container, false);
        initViews();
        initData();
        setOnClickListeners();
        // Inflate the layout for this fragment
        return view;
    }

    private void initViews() {
        mTextFgmtDisplayTextView = (TextView) view.findViewById(R.id.textFgmtDisplayTextView);
        mTextFgmtScrollViewForDisplayTextView = (ScrollView) view.findViewById(R.id.textFgmtScrollViewForDisplayText);
        mUserQueryEditText = (EditText) view.findViewById(R.id.userQueryEditText);
        mUserQueryEditText.clearFocus();
        mUserQueryEditText.setText("");
        mSendTextQueryToServerImageButton = (ImageButton) view.findViewById(R.id.sendTextQueryToServerImageButton);
    }

    private void setOnClickListeners() {
        mUserQueryEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if(mUserQueryEditText.getText().toString().isEmpty())
                        Snackbar.make(view, "Type a query first...", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    else
                        callWebServiceWithQuery(mUserQueryEditText.getText().toString());
                    mUserQueryEditText.clearFocus();
                    mUserQueryEditText.setText("");
                    hideKeyboardFrom(v.getContext(),v);
                    handled = true;
                }
                return handled;
            }
        });

        mSendTextQueryToServerImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mUserQueryEditText.getText().toString().isEmpty())
                    Snackbar.make(view, "Type a query first...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                else
                    callWebServiceWithQuery(mUserQueryEditText.getText().toString());
                mUserQueryEditText.clearFocus();
                mUserQueryEditText.setText("");
                hideKeyboardFrom(v.getContext(),v);
            }
        });

        mTextFgmtDisplayTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mTextFgmtScrollViewForDisplayTextView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void initData() {
        Bundle bundle = this.getArguments();
        mBeconIDParam = bundle.getString(UMBCIoTApplication.getBeaconTag(), "No beaconID");
        sharedPreferences = getActivity().getSharedPreferences(UMBCIoTApplication.getSharedPreference(), Context.MODE_PRIVATE);
        mSessionId = sharedPreferences.getString(UMBCIoTApplication.getPrefSessionIdTag(),"No sessionID");
        jsonResponse = new String("");
        // Get a RequestQueue
        queue = VolleySingleton.getInstance(view.getContext()).getRequestQueue();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onTextQueryFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnTextQueryFragmentInteractionListener) {
            mListener = (OnTextQueryFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnTextQueryFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        resetFragmentView();
    }

    private void resetFragmentView() {
        mTextFgmtDisplayTextView.setText(view.getContext().getResources().getString(R.string.default_display_text));
        mUserQueryEditText.setText("");
        mUserQueryEditText.clearFocus();
        mUserQueryEditText.setText("");
        hideKeyboardFrom(view.getContext(),view);
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
    public interface OnTextQueryFragmentInteractionListener {
        // TODO: Update argument type and name
        void onTextQueryFragmentInteraction(Uri uri);
    }

    private void callWebServiceWithQuery(String query) {
        Log.d(UMBCIoTApplication.getDebugTag(),"Came to callWebServiceWithQuery");
        // Create a JSONObject for the POST call to the NLP engine server
        try {
//            createJSONObject(query,mBeconIDParam);
            jsonRequest = new JSONRequest(query,mBeconIDParam,mSessionId);
        } catch (JSONException aJSONException) {
            Log.d("JSONException:"," Something went wrong in JSON object creation");
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
            UMBCIoTApplication.getUrl(),
            jsonRequest.getRequest(),
            new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
                    try {
                        // Parsing json object response
                        // response will be a json object
                        String status = response.getString("status");
                        String text = response.getString("text");
//                            JSONObject phone = response.getJSONObject("phone");
//                            String home = phone.getString("home");
//                            String mobile = phone.getString("mobile");

//                            jsonResponse = "";
                        if (!mTextFgmtDisplayTextView.getText().equals(view.getContext().getResources().getString(R.string.default_display_text)))
                            jsonResponse += "------------------------" + "\n";
                        jsonResponse +=  "Query parameters were: "
                                +jsonRequest.getRequest().getString(UMBCIoTApplication.getQuestionTag())
                                +" "
                                +jsonRequest.getRequest().get(UMBCIoTApplication.getBeaconTag())
                                +"\n\n";
                        jsonResponse += "Response is:\nStatus: " + status + " Text: " + text + "\n";
//                            response += "Home: " + home + "\n\n";
//                            response += "Mobile: " + mobile + "\n\n";

//                            Toast.makeText(view.getContext(),"JSON response: "+jsonResponse,Toast.LENGTH_LONG).show();
                        mTextFgmtDisplayTextView.setText(jsonResponse);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(view.getContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            },
            new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    String body = new String();
                    //get status code here
                    String statusCode = String.valueOf(error.networkResponse.statusCode);
                    Log.d(UMBCIoTApplication.getDebugTag(), "Error status code was: " + statusCode);
                    Toast.makeText(view.getContext(), "Error status code was: " + statusCode, Toast.LENGTH_LONG).show();
                    try {
                        if (!mTextFgmtDisplayTextView.getText().equals(view.getContext().getResources().getString(R.string.default_display_text)))
                            jsonResponse += "------------------------" + "\n";
                        jsonResponse +=  "Query parameters were: "
                                +jsonRequest.getRequest().getString(UMBCIoTApplication.getQuestionTag())
                                +" "
                                +jsonRequest.getRequest().get(UMBCIoTApplication.getBeaconTag())
                                +"\n\n";
                        jsonResponse += "Getting an error code: " + statusCode + " from the server\n";
//                            response += "Home: " + home + "\n\n";
//                            response += "Mobile: " + mobile + "\n\n";

//                            Toast.makeText(view.getContext(),"JSON response: "+jsonResponse,Toast.LENGTH_LONG).show();
                        mTextFgmtDisplayTextView.setText(jsonResponse);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(view.getContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    //get response body and parse with appropriate encoding
                    if(error.networkResponse.data!=null) {
                        try {
                            body = new String(error.networkResponse.data,"UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d(UMBCIoTApplication.getDebugTag(), "In ErrorListener"+statusCode+body);
                    VolleyLog.d(UMBCIoTApplication.getDebugTag(), "I ma here Error: " + error.getMessage());
    //                Toast.makeText(view.getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        );

        // Add a request (in this example, called jsObjRequest) to your RequestQueue.
        VolleySingleton.getInstance(view.getContext()).addToRequestQueue(jsObjRequest);
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
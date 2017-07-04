package edu.umbc.cs.iot.clients.android.ui.fragments;

/**
 * Created on May 27, 2016
 * @author: Prajit Kumar Das
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
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
    private JSONRequest feedbackJsonRequest;
    private String feedbackJsonResponse;

    private JSONRequest jsonRequest;
    private RequestQueue queue;
    // temporary string to show the parsed response
    private String jsonResponse;

    private TextView mTextFgmtDisplayTextView;
    private ScrollView mTextFgmtScrollViewForDisplayTextView;
    private EditText mUserQueryEditText;
    private View view;
    private ImageButton mSendTextQueryToServerImageButton;
    private ImageButton mThumbUpBtn;
    private ImageButton mThumbDnBtn;

    private String lastQuery;
    private String lastResponse;
    private String mBeconIDParam;
    private String mSessionId;
    private String mUserId;
    private SharedPreferences sharedPreferences;

    private OnTextQueryFragmentInteractionListener mListener;

    public TextQueryFragment() {
        super();
        // Just to be an empty Bundle. You can use this later with getArguments().set...
        setArguments(new Bundle());
        // Required empty public constructor
    }

    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

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

        mThumbUpBtn = (ImageButton) view.findViewById(R.id.textThumbsUpBtn);
        mThumbDnBtn = (ImageButton) view.findViewById(R.id.textThumbsDownBtn);

        mThumbUpBtn.setVisibility(View.GONE);
        mThumbDnBtn.setVisibility(View.GONE);
    }

    private void setOnClickListeners() {
        mUserQueryEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (mUserQueryEditText.getText().toString().isEmpty())
                        Snackbar.make(view, "Type a query first...", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    else
                        callWebServiceWithQuery(mUserQueryEditText.getText().toString());
                    mUserQueryEditText.clearFocus();
                    mUserQueryEditText.setText("");
                    hideKeyboardFrom(v.getContext(), v);
                    handled = true;
                }
                return handled;
            }
        });

        mSendTextQueryToServerImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUserQueryEditText.getText().toString().isEmpty())
                    Snackbar.make(view, "Type a query first...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                else
                    callWebServiceWithQuery(mUserQueryEditText.getText().toString());
                mUserQueryEditText.clearFocus();
                mUserQueryEditText.setText("");
                hideKeyboardFrom(v.getContext(), v);
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

        mThumbUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAlertDialog(true);
            }
        });

        mThumbDnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAlertDialog(false);
            }
        });
    }

    private void initData() {
        Bundle bundle = this.getArguments();
        mBeconIDParam = bundle.getString(UMBCIoTApplication.getJsonBeaconKey(), "No beaconID");
        sharedPreferences = getActivity().getSharedPreferences(UMBCIoTApplication.getSharedPreference(), Context.MODE_PRIVATE);
        mSessionId = sharedPreferences.getString(UMBCIoTApplication.getJsonSessionIdKey(), "No sessionID");
        mUserId = sharedPreferences.getString(UMBCIoTApplication.getPrefUserIdKey(), "No userID");
        jsonResponse = new String();
        feedbackJsonResponse = new String();
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
        hideKeyboardFrom(view.getContext(), view);
    }

    private void callWebServiceWithQuery(final String query) {
        // Create a JSONObject for the POST call to the NLP engine server
        try {
            jsonRequest = new JSONRequest(query, mBeconIDParam, mSessionId, mUserId);
        } catch (JSONException aJSONException) {
        }
        /*
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
                            String text = response.getString("text");
                            lastQuery = query;
                            lastResponse = text;
                            mThumbDnBtn.setVisibility(View.VISIBLE);
                            mThumbUpBtn.setVisibility(View.VISIBLE);
                            if (!mTextFgmtDisplayTextView.getText().equals(view.getContext().getResources().getString(R.string.default_display_text)))
                                jsonResponse += "------------------------" + "\n";
                            jsonResponse += "Query parameters were: "
                                    + jsonRequest.getRequest().getString(UMBCIoTApplication.getJsonQuestionKey())
                                    + " "
                                    + jsonRequest.getRequest().get(UMBCIoTApplication.getJsonBeaconKey())
                                    + "\n\n";
                            jsonResponse += "Response is:\nText: " + text + "\n";
                            mTextFgmtDisplayTextView.setText(jsonResponse);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String body = new String();
                        //get status code here
                        String statusCode;
                        try {
                            statusCode = String.valueOf(error.networkResponse.statusCode);
                        } catch (NullPointerException e) {
                            statusCode = "fatal error! Error code not received";
                        }
                        try {
                            if (!mTextFgmtDisplayTextView.getText().equals(view.getContext().getResources().getString(R.string.default_display_text)))
                                jsonResponse += "------------------------" + "\n";
                            jsonResponse += "Query parameters were: "
                                    + jsonRequest.getRequest().getString(UMBCIoTApplication.getJsonQuestionKey())
                                    + " "
                                    + jsonRequest.getRequest().get(UMBCIoTApplication.getJsonBeaconKey())
                                    + "\n\n";
                            jsonResponse += "Getting an error code: " + statusCode + " from the server\n";
                            mTextFgmtDisplayTextView.setText(jsonResponse);
                            mThumbDnBtn.setVisibility(View.GONE);
                            mThumbUpBtn.setVisibility(View.GONE);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //get response body and parse with appropriate encoding
                        if (error.networkResponse.data != null) {
                            try {
                                body = new String(error.networkResponse.data, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );

        // Add a request (in this example, called jsObjRequest) to your RequestQueue.
        VolleySingleton.getInstance(view.getContext()).addToRequestQueue(jsObjRequest);
    }

    private void sendUserFeedback(boolean feedback, String feedbackText) {
        try {
            feedbackJsonRequest = new JSONRequest(feedback, feedbackText, lastQuery, lastResponse, mBeconIDParam, mSessionId, mUserId);
        } catch (JSONException aJSONException) {
        }
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                Request.Method.POST,
                UMBCIoTApplication.getFeedbackUrl(),
                feedbackJsonRequest.getRequest(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            final String status = response.getString("status");
                            feedbackJsonResponse = "Status: " + status;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String statusCode;
                        try {
                            statusCode = String.valueOf(error.networkResponse.statusCode);
                        } catch (NullPointerException e) {
                            statusCode = "fatal error! Error code not received";
                        }
                        feedbackJsonResponse = "Getting an error code: " + statusCode + " from the server\n";
                    }
                }
        );

        // Once feedback is submitted remove the feedback buttons
        mThumbDnBtn.setVisibility(View.GONE);
        mThumbUpBtn.setVisibility(View.GONE);
        // Add a request (in this example, called jsObjRequest) to your RequestQueue.
        VolleySingleton.getInstance(view.getContext()).addToRequestQueue(jsObjRequest);
    }

    private void createAlertDialog(final boolean feedback) {
        final StringBuilder feedbackText = new StringBuilder();
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.feedback_dialog_title);

        // Set up the input
        final EditText input = new EditText(getActivity());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.feedback_hint);
        builder.setView(input);

        builder.setPositiveButton(R.string.submit_feedback, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                feedbackText.append(input.getText().toString());
                sendUserFeedback(feedback, feedbackText.toString());
            }
        });

        // create alert dialog
        AlertDialog alertDialog = builder.create();

        // show it
        alertDialog.show();
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
}
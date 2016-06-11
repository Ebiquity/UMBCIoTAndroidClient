package edu.umbc.cs.iot.clients.android.ui.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import edu.umbc.cs.iot.clients.android.R;

/**
 * Created by Prajit Kumar Das on 6/11/2016.
 */

public class PrefsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pref_general);
    }
}
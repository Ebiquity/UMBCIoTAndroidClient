package edu.umbc.cs.iot.clients.android.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import edu.umbc.cs.iot.clients.android.R;
import edu.umbc.cs.iot.clients.android.UMBCIoTApplication;

/**
 * Created by Prajit Kumar Das on 6/11/2016.
 */

public class PrefsFragment extends PreferenceFragment {

    private SharedPreferences sharedPreferences;
    private boolean beaconDisabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toast.makeText(getActivity(),"Feature currently disabled",Toast.LENGTH_LONG).show();

        sharedPreferences = getActivity().getSharedPreferences(UMBCIoTApplication.getSharedPreference(), Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pref_general);

        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(UMBCIoTApplication.getPrefBeaconDisabledTag())) {
                    Preference connectionPref = findPreference(key);

                    editor.putBoolean(UMBCIoTApplication.getPrefBeaconDisabledTag(), connectionPref.getShouldDisableView());
                    editor.commit();
                }
            }
        });
    }
}
package edu.umbc.cs.iot.clients.android.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import edu.umbc.cs.iot.clients.android.R;
import edu.umbc.cs.iot.clients.android.UMBCIoTApplication;

/**
 * Created by Prajit Kumar Das on 6/11/2016.
 */

public class PrefsFragment extends PreferenceFragment {

    private SharedPreferences sharedPreferences;
    private SwitchPreference mSwitchPreferenceEnableUserIdentification;
    private EditTextPreference mEditTextPreferenceUserIdentity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        sharedPreferences = getActivity().getSharedPreferences(UMBCIoTApplication.getSharedPreference(), Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        mEditTextPreferenceUserIdentity = (EditTextPreference) getPreferenceManager().findPreference(UMBCIoTApplication.getPrefUserIdKey());
        mEditTextPreferenceUserIdentity.setSummary(
                getResources().getString(R.string.pref_summary_user_identity).concat(
                        sharedPreferences.getString(
                                UMBCIoTApplication.getPrefUserIdKey(), getResources().getString(R.string.pref_user_id_default_value))));
        if (sharedPreferences.getBoolean(UMBCIoTApplication.getPrefEnableUserIdKey(), false))
            mEditTextPreferenceUserIdentity.setEnabled(true);

        mSwitchPreferenceEnableUserIdentification = (SwitchPreference) getPreferenceManager().findPreference(UMBCIoTApplication.getPrefEnableUserIdKey());
        mSwitchPreferenceEnableUserIdentification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    editor.putBoolean(UMBCIoTApplication.getPrefEnableUserIdKey(), (Boolean) newValue);
                    editor.commit();
                    mEditTextPreferenceUserIdentity.setSummary(
                            getResources().getString(R.string.pref_summary_user_identity).concat(
                                    sharedPreferences.getString(
                                            UMBCIoTApplication.getPrefUserIdKey(), getResources().getString(R.string.pref_user_id_default_value))));
                    mEditTextPreferenceUserIdentity.setEnabled(true);
                } else {
                    editor.putString(UMBCIoTApplication.getPrefUserIdKey(), getResources().getString(R.string.pref_user_id_default_value));
                    editor.putBoolean(UMBCIoTApplication.getPrefEnableUserIdKey(), (Boolean) newValue);
                    editor.commit();
                    mEditTextPreferenceUserIdentity.setSummary(
                            getResources().getString(R.string.pref_summary_user_identity).concat(
                                    sharedPreferences.getString(
                                            UMBCIoTApplication.getPrefUserIdKey(), getResources().getString(R.string.pref_user_id_default_value))));
                    mEditTextPreferenceUserIdentity.setEnabled(false);
                }
                return true;
            }
        });

        mEditTextPreferenceUserIdentity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                if(!newValue.equals("")) {
                editor.putString(UMBCIoTApplication.getPrefUserIdKey(), (String) newValue);
                editor.commit();
                mEditTextPreferenceUserIdentity.setSummary(
                        getResources().getString(R.string.pref_summary_user_identity).concat(
                                sharedPreferences.getString(
                                        UMBCIoTApplication.getPrefUserIdKey(), getResources().getString(R.string.pref_user_id_default_value))));
//                }
                return true;
            }
        });

    }
}
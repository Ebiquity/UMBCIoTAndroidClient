package edu.umbc.cs.iot.clients.android.ui.activities;

/*
 * Created on August 23, 2016
 * @author: Prajit Kumar Das
 * @purpose: This is a placeholder activity, which will be used to obtain user feedback about the app, in the future.
 */

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import edu.umbc.cs.iot.clients.android.R;

public class AboutUsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
}

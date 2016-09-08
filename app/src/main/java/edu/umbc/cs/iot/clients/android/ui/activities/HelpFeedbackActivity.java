package edu.umbc.cs.iot.clients.android.ui.activities;

/**
 * Created on August 23, 2016
 * @author: Prajit Kumar Das
 * @purpose: This is a placeholder activity, which will be used to obtain user feedback about the app, in the future.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import edu.umbc.cs.iot.clients.android.R;

public class HelpFeedbackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_feedback);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "This feature is currently unavailable", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

//    @Override
//    protected void onPause() {
//        Intent mainActivityLaunchIntent = new Intent(this.getApplicationContext(), MainActivity.class);
//        startActivity(mainActivityLaunchIntent);
//    }
}

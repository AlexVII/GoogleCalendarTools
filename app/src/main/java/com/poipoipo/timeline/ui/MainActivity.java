package com.poipoipo.timeline.ui;

import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.facebook.stetho.Stetho;
import com.poipoipo.timeline.R;
import com.poipoipo.timeline.data.Event;
import com.poipoipo.timeline.data.Label;
import com.poipoipo.timeline.data.TimestampUtil;
import com.poipoipo.timeline.database.DatabaseHelper;
import com.poipoipo.timeline.dialog.EventEditorFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, EventEditorFragment.EventEditorListener {
    private static final String TAG = "MainActivity";

    public static final int MESSAGE_DRAWER = 0, MESSAGE_CREATE = 1, MESSAGE_QUICK_CREATE = 2, MESSAGE_DIALOG_DETAIL = 3;
    private DrawerLayout drawerLayout;
    public DatabaseHelper databaseHelper;
    private TimestampUtil timestampUtil = new TimestampUtil();
    private Fragment fragment;
    FragmentManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        databaseHelper = new DatabaseHelper(this);

        /*Stetho Debug*/
        Stetho.initializeWithDefaults(this);
        Log.d(TAG, "onCreate: Stetho Running");
        fragment = new FragmentTimeline();
        manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Bundle args = new Bundle();
        switch (item.getItemId()) {
            case R.id.nav_timeline:
                fragment = new FragmentTimeline();
                break;
            case R.id.nav_dashboard:
                fragment = new FragmentDashboard();
                break;
            case R.id.nav_category:
                fragment = new FragmentLabels();
                args.putInt(FragmentLabels.LABEL_TYPE, Label.TITLE);
                fragment.setArguments(args);
                break;
            case R.id.nav_title:
                fragment = new FragmentLabels();
                args.putInt(FragmentLabels.LABEL_TYPE, Label.LOCATION);
                fragment.setArguments(args);
                break;
            case R.id.nav_location:
                fragment = new FragmentLabels();
                args.putInt(FragmentLabels.LABEL_TYPE, Label.SUBTITLE);
                fragment.setArguments(args);
                break;
//            case R.id.nav_archive:
//            case R.id.nav_settings:
//            case R.id.nav_about:
//                Toast.makeText(getApplicationContext(), R.string.not_done, Toast.LENGTH_SHORT).show();
        }
        manager.beginTransaction().add(R.id.content_frame, fragment).commit();
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public int getTodayTimestamp() {
        return timestampUtil.getTodayTimestamp();
    }

    public int getCurrentTimestamp() {
        return timestampUtil.getCurrentTimestamp();
    }

    public final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_DRAWER:
                    drawerLayout.openDrawer(GravityCompat.START);
                    break;
                case MESSAGE_CREATE:
                    break;
                case MESSAGE_QUICK_CREATE:
                    databaseHelper.insert(new Event(timestampUtil.getCurrentTimestamp()));
                    Toast.makeText(getApplicationContext(), "Event Created", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onNegativeClick(DialogFragment fragment) {
        Event event = (Event) fragment.getArguments().getSerializable("event");
        Toast.makeText(getApplicationContext(), "Negation Clicker" + event.getStart(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPositiveClick(DialogFragment fragment) {
        Event event = (Event) fragment.getArguments().getSerializable("event");
        Toast.makeText(getApplicationContext(), "Positive Click" + event.getStart(), Toast.LENGTH_SHORT).show();
    }
}
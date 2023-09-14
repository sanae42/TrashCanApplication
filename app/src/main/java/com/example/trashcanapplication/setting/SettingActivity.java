package com.example.trashcanapplication.setting;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.trashcanapplication.R;
import com.example.trashcanapplication.activityCollector.BaseActivity;

/**
 * @Title：SettingActivity.java
 * @Description: An activity related to notification setting
 * @author P Geng
 */
public class SettingActivity extends BaseActivity {

    Toolbar toolbar;

    Switch backgroundNotificationSwitch;

    //SharedPreferences
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        initView();
    }

    /***
     *Initialize Layout
     */
    private void initView(){
        //        Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //   initialize actionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        backgroundNotificationSwitch = (Switch) findViewById(R.id.background_notification);
        backgroundNotificationSwitch.setChecked(pref.getBoolean("ifNotification", true));

        backgroundNotificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean("ifNotification", isChecked);
                editor.apply();
            }
        });

    }

    /**
     * Listen to the button, which is the button on the toolbar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }
}
package com.example.trashcanapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.example.trashcanapplication.activityCollector.BaseActivity;

public class LoginActivity extends BaseActivity {

    //sp数据库 存放应用设置状态
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

//        pref.getBoolean("backgroundNotification", true);
//
//        editor.putBoolean("backgroundNotification", true);
//        editor.apply();
    }
}
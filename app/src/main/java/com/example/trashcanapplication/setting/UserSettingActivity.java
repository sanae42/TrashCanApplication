package com.example.trashcanapplication.setting;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.trashcanapplication.R;
import com.example.trashcanapplication.activityCollector.BaseActivity;

public class UserSettingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setting);
    }
}
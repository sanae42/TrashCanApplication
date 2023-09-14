package com.example.trashcanapplication.activityCollector;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * @Title：BaseActivity.java
 * @Description: All activities inherit from this activity
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }
}

package com.example.trashcanapplication.activityCollector;

import androidx.appcompat.app.AppCompatActivity;


import com.example.trashcanapplication.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * @Title：ActivityCollector.java
 * @Description: Activity collector, which temporarily stores activities through a List
 */
public class ActivityCollector {
    public static List<AppCompatActivity> activities = new ArrayList<AppCompatActivity>();

    public static void addActivity(AppCompatActivity activity) {
        activities.add(activity);
    }
    public static void removeActivity(AppCompatActivity activity) {
        activities.remove(activity);
    }
    public static void finishAll() {
        for (AppCompatActivity activity : activities) {
            if (!activity.isFinishing()) {
                activity.finish();
            }
        }
    }

    public static void backToMainActivity() {
        for (AppCompatActivity activity : activities) {
            if (!activity.isFinishing() && activity instanceof MainActivity == false) {
                activity.finish();
            }
        }
    }
}
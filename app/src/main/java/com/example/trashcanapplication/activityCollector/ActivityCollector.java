package com.example.trashcanapplication.activityCollector;

import androidx.appcompat.app.AppCompatActivity;


import com.example.trashcanapplication.MainActivity;

import java.util.ArrayList;
import java.util.List;

//活动管理器
public class ActivityCollector {
    //通过一个List来暂存活动
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
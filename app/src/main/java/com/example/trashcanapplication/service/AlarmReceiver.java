package com.example.trashcanapplication.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    //用于接收后台执行定时任务的广播 开启通知栏弹窗
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, MyService.class);
        context.startService(i);
    }
}

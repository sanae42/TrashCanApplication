package com.example.trashcanapplication.setting;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.example.trashcanapplication.MQTT.MyMqttClient;
import com.example.trashcanapplication.R;
import com.example.trashcanapplication.activityCollector.ActivityCollector;
import com.example.trashcanapplication.activityCollector.BaseActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class UserSettingActivity extends BaseActivity {

    Toolbar toolbar;

    private MyMqttClient myMQTTClient;
    private static String IP;
    private String ClientId;

    //进度条窗口
    private ProgressDialog progressDialog;

    //sp数据库 存放应用设置状态
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setting);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        //从SharedPerformance获取IP
        IP = pref.getString("ipStr","");
        ClientId = "Android/"+IP;

        //使用EventBus与线程交流
        EventBus.getDefault().register(this);

        initView();


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String s) {
        JSONObject j;
        try {
            j = new JSONObject(s);
            String sender = j.getString("sender");
            String dataType = j.getString("dataType");
            if (sender.equals("myMqttClient") && dataType.equals("editUserNameReplyData")) {
                if(j.getString("result").equals("succeeded")){
                    if(progressDialog!=null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getApplicationContext(), "修改成功", Toast.LENGTH_SHORT).show();
                    //回到主活动
//                    ActivityCollector.backToMainActivity();
                }else {
                    if(progressDialog!=null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getApplicationContext(), "修改失败", Toast.LENGTH_SHORT).show();
                }
            }
            if (sender.equals("myMqttClient") && dataType.equals("editPasswordReplyData")) {
                if(j.getString("result").equals("succeeded")){
                    if(progressDialog!=null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getApplicationContext(), "修改成功", Toast.LENGTH_SHORT).show();
                    //回到主活动
//                    ActivityCollector.backToMainActivity();
                }else {
                    if(progressDialog!=null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getApplicationContext(), "修改失败", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Log.d("登录问题", e.getMessage());
        }
    }

    private void initView(){
        //        导航条
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //   设置actionbar（即toolbar）最左侧按钮显示状态和图标
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        LinearLayout idEditLiearlayout = (LinearLayout)findViewById(R.id.id_edit);
        idEditLiearlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUsernameSettingDialog();
            }
        });
        LinearLayout passwordEditLiearlayout = (LinearLayout)findViewById(R.id.password_edit);
        passwordEditLiearlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPasswordSettingDialog();
            }
        });
        LinearLayout exitLiearlayout = (LinearLayout)findViewById(R.id.exit);
        exitLiearlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putBoolean("ifLogin", false);
                editor.apply();
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("dataType", "exit");
                    jsonObject.put("sender", "application" );
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                String s =  new String(jsonObject.toString());
                EventBus.getDefault().post(s);
                //回到主活动
                ActivityCollector.backToMainActivity();
            }
        });

    }

    private void showUsernameSettingDialog() {
        /* @setView 装入自定义View ==> R.layout.dialog_customize
         * 由于dialog_customize.xml只放置了一个EditView，因此和图8一样
         * dialog_customize.xml可自定义更复杂的View
         */
        AlertDialog.Builder customizeDialog =
                new AlertDialog.Builder(UserSettingActivity.this);
        final View dialogView = LayoutInflater.from(UserSettingActivity.this)
                .inflate(R.layout.dialog_edit_userid,null);
        customizeDialog.setTitle("edit username");
        customizeDialog.setView(dialogView);
        customizeDialog.setPositiveButton("submit",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText edit_text =
                                (EditText) dialogView.findViewById(R.id.edit_text);

                        //展示进度条
                        try{
                            progressDialog = ProgressDialog.show(UserSettingActivity.this,"加载中","正在努力加载");
                        }catch (Exception e){
                            Log.d("进度条窗口闪退", e.getMessage());
                        }
                        Timer timer = new Timer();
                        //TimerTask属于子线程，不能执行toast “Can't toast on a thread that has not called Looper.prepare()”
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                if(progressDialog.isShowing()){
                                    progressDialog.dismiss();
                                }
                                timer.cancel();
                            }
                        };
                        //6000ms执行一次
                        timer.schedule(task, 6000);

                        //发送请求
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("dataType", "EditUserNameRequest");
                            jsonObject.put("Id", ClientId );
                            jsonObject.put("UserName", pref.getString("UserName",""));
                            jsonObject.put("newUserName", edit_text.getText());
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        MyMqttClient myMQTTClient = MyMqttClient.getInstance();
                        myMQTTClient.publishMessage("MQTTServerSub",jsonObject.toString(),0);
                    }
                });
        customizeDialog.show();
    }

    private void showPasswordSettingDialog() {
        /* @setView 装入自定义View ==> R.layout.dialog_customize
         * 由于dialog_customize.xml只放置了一个EditView，因此和图8一样
         * dialog_customize.xml可自定义更复杂的View
         */
        AlertDialog.Builder customizeDialog =
                new AlertDialog.Builder(UserSettingActivity.this);
        final View dialogView = LayoutInflater.from(UserSettingActivity.this)
                .inflate(R.layout.dialog_edit_password,null);
        customizeDialog.setTitle("edit password");
        customizeDialog.setView(dialogView);
        customizeDialog.setPositiveButton("submit",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText edit_text1 =
                                (EditText) dialogView.findViewById(R.id.edit_text1);
                        EditText edit_text2 =
                                (EditText) dialogView.findViewById(R.id.edit_text2);

                        String psw1 = edit_text1.getText().toString();
                        String psw2 = edit_text2.getText().toString();
                        if(!psw1.equals(psw2)){
                            Toast.makeText(getApplicationContext(), "密码输入不一致", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //展示进度条
                        try{
                            progressDialog = ProgressDialog.show(UserSettingActivity.this,"加载中","正在努力加载");
                        }catch (Exception e){
                            Log.d("进度条窗口闪退", e.getMessage());
                        }
                        Timer timer = new Timer();
                        //TimerTask属于子线程，不能执行toast “Can't toast on a thread that has not called Looper.prepare()”
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                if(progressDialog.isShowing()){
                                    progressDialog.dismiss();
                                }
                                timer.cancel();
                            }
                        };
                        //6000ms执行一次
                        timer.schedule(task, 6000);

                        //发送请求
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("dataType", "EditPasswordRequest");
                            jsonObject.put("Id", ClientId );
                            jsonObject.put("UserName", pref.getString("UserName",""));
                            jsonObject.put("Password", edit_text1.getText());
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        MyMqttClient myMQTTClient = MyMqttClient.getInstance();
                        myMQTTClient.publishMessage("MQTTServerSub",jsonObject.toString(),0);
                    }
                });
        customizeDialog.show();
    }

    /**
     * 按键监听，此处即toolbar上按键
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
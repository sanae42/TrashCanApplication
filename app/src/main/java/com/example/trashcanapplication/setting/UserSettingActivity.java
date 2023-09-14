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

/**
 * @Title：UserSettingActivity.java
 * @Description: An activity related to user account setting
 * @author P Geng
 */
public class UserSettingActivity extends BaseActivity {

    Toolbar toolbar;

    private MyMqttClient myMQTTClient;
    private static String IP;
    private String ClientId;

    //Progress Dialog
    private ProgressDialog progressDialog;

    //SharedPreferences
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setting);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        //Obtain IP from SharedPerformance
        IP = pref.getString("ipStr","");
        ClientId = "Android/"+IP;

        //Using EventBus to communicate with threads
        EventBus.getDefault().register(this);

        initView();


    }

    /***
     *EventBus callback
     */
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
                    Toast.makeText(getApplicationContext(), "Modified succeed", Toast.LENGTH_SHORT).show();
                    //回到主活动
//                    ActivityCollector.backToMainActivity();
                }else {
                    if(progressDialog!=null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getApplicationContext(), "Modification failed", Toast.LENGTH_SHORT).show();
                }
            }
            if (sender.equals("myMqttClient") && dataType.equals("editPasswordReplyData")) {
                if(j.getString("result").equals("succeeded")){
                    if(progressDialog!=null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getApplicationContext(), "Modified succeed", Toast.LENGTH_SHORT).show();

                }else {
                    if(progressDialog!=null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getApplicationContext(), "Modification failed", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Log.d("Login Issues", e.getMessage());
        }
    }

    /***
     *Initialize Layout
     */
    private void initView(){
        //        toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //   initialize actionBar
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

    /***
     *Display the dialog for setting user name
     */
    private void showUsernameSettingDialog() {
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

                        //show progress dialog
                        try{
                            progressDialog = ProgressDialog.show(UserSettingActivity.this,"LOADING","LOADING");
                        }catch (Exception e){
                            Log.d("Progress Bar Window Flashback", e.getMessage());
                        }
                        Timer timer = new Timer();
                        //TimerTask belongs to a child thread and cannot execute toast “Can't toast on a thread that has not called Looper.prepare()”
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                if(progressDialog.isShowing()){
                                    progressDialog.dismiss();
                                }
                                timer.cancel();
                            }
                        };
                        timer.schedule(task, 6000);

                        //send request
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

    /***
     *Display the dialog for setting password
     */
    private void showPasswordSettingDialog() {
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
                            Toast.makeText(getApplicationContext(), "Inconsistent password input", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //展示进度条
                        try{
                            progressDialog = ProgressDialog.show(UserSettingActivity.this,"LOADING","LOADING");
                        }catch (Exception e){
                            Log.d("Progress Bar Window Flashback", e.getMessage());
                        }
                        Timer timer = new Timer();
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                if(progressDialog.isShowing()){
                                    progressDialog.dismiss();
                                }
                                timer.cancel();
                            }
                        };
                        timer.schedule(task, 6000);

                        //send request
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
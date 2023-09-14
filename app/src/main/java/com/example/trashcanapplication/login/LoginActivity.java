package com.example.trashcanapplication.login;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.trashcanapplication.MQTT.MyMqttClient;
import com.example.trashcanapplication.R;
import com.example.trashcanapplication.TrashCanDetailActivity;
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
 * @Description: The activity of login an account for a user.
 * @author P Geng
 */
public class LoginActivity extends BaseActivity {

    //SharedPreferences
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private MyMqttClient myMQTTClient;
    private static String IP;
    private String ClientId;

    //Progress Dialog
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //SharedPreferences
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        //Obtain IP from SharedPerformance
        IP = pref.getString("ipStr","");
        ClientId = "Android/"+IP;

        //Using EventBus to communicate with threads
        EventBus.getDefault().register(this);

        //        toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        EditText usernameEditText = (EditText) findViewById(R.id.Username);
        EditText passwordEditText = (EditText) findViewById(R.id.Password);
        Button signinButton = (Button) findViewById(R.id.Sign_in);
        signinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show progressDialog
                try{
                    progressDialog = ProgressDialog.show(LoginActivity.this,"loading","Loading data from the server");
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

                //send login request
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("dataType", "LoginRequest");
                    jsonObject.put("Id", ClientId );
                    jsonObject.put("UserName", usernameEditText.getText());
                    jsonObject.put("Password", passwordEditText.getText());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                MyMqttClient myMQTTClient = MyMqttClient.getInstance();
                myMQTTClient.publishMessage("MQTTServerSub",jsonObject.toString(),0);
            }
        });

        Button signupButton = (Button) findViewById(R.id.Sign_up);
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String s) {
        JSONObject j;
        try {
            j = new JSONObject(s);
            String sender = j.getString("sender");
            String dataType = j.getString("dataType");
            if (sender.equals("myMqttClient") && dataType.equals("loginReplyData")) {
                if(j.getString("result").equals("succeeded")){
                    if(progressDialog!=null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getApplicationContext(), "Login succeeded", Toast.LENGTH_SHORT).show();
                    //Return to main activity
                    ActivityCollector.backToMainActivity();
                }else {
                    if(progressDialog!=null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getApplicationContext(), "Login failed", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "JSON conversion error :" + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("Login Issues", e.getMessage());
        }
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
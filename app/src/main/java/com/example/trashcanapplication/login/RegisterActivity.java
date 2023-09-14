package com.example.trashcanapplication.login;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
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
 * @Description: The activity of registering an account for a user.
 * @author P Geng
 */
public class RegisterActivity extends BaseActivity {

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
            setContentView(R.layout.activity_register);

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
            EditText password1EditText = (EditText) findViewById(R.id.Password1);
            EditText password2EditText = (EditText) findViewById(R.id.Password2);
            Button signupButton = (Button) findViewById(R.id.Sign_up);
            signupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String psw1 = password1EditText.getText().toString();
                    String psw2 = password2EditText.getText().toString();
                    if(!psw1.equals(psw2)){
                        Toast.makeText(getApplicationContext(), "The two password inputs are inconsistent", Toast.LENGTH_SHORT).show();
                        Toast.makeText(getApplicationContext(), password1EditText.getText()+" "+password2EditText.getText(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //show progressDialog
                    try{
                        progressDialog = ProgressDialog.show(com.example.trashcanapplication.login.RegisterActivity.this,"加载中","正在努力加载");
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

                    //Send login request
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("dataType", "RegisterRequest");
                        jsonObject.put("Id", ClientId );
                        jsonObject.put("UserName", usernameEditText.getText());
                        jsonObject.put("Password", password1EditText.getText());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    MyMqttClient myMQTTClient = MyMqttClient.getInstance();
                    myMQTTClient.publishMessage("MQTTServerSub",jsonObject.toString(),0);
                }
            });

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
                if (sender.equals("myMqttClient") && dataType.equals("registerReplyData")) {
                    if(j.getString("result").equals("succeeded")){
                        if(progressDialog!=null){
                            progressDialog.dismiss();
                        }
                        Toast.makeText(getApplicationContext(), "register succeed", Toast.LENGTH_SHORT).show();
                        //Return to main activity
                        ActivityCollector.backToMainActivity();
                    }else {
                        if(progressDialog!=null){
                            progressDialog.dismiss();
                        }
                        Toast.makeText(getApplicationContext(), "register failed", Toast.LENGTH_SHORT).show();
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
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

public class LoginActivity extends BaseActivity {

    //sp数据库 存放应用设置状态
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private MyMqttClient myMQTTClient;
    private static String IP;
    private String ClientId;

    //进度条窗口
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //SharedPreferences
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        //从SharedPerformance获取IP
        IP = pref.getString("ipStr","");
        ClientId = "Android/"+IP;

        //使用EventBus与线程交流
        EventBus.getDefault().register(this);

        //        导航条
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //   设置actionbar（即toolbar）最左侧按钮显示状态和图标
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
                //展示进度条
                try{
                    progressDialog = ProgressDialog.show(LoginActivity.this,"loading","Loading data from the server");
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

                //发送登录请求
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
            //接收到的JSON数据为，全部垃圾桶状态数据
            // TODO:客户端每一段时间发送一次数据，会导致marker刷新，打开的infowindow会关闭；可以只在oncreate时接收一次数据，之后手动刷新；或之后不再clear marker
            if (sender.equals("myMqttClient") && dataType.equals("loginReplyData")) {
                if(j.getString("result").equals("succeeded")){
                    if(progressDialog!=null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getApplicationContext(), "Login succeeded", Toast.LENGTH_SHORT).show();
                    //回到主活动
                    ActivityCollector.backToMainActivity();
                }else {
                    if(progressDialog!=null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getApplicationContext(), "Login failed", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "JSON转换出错 :" + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("登录问题", e.getMessage());
        }
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
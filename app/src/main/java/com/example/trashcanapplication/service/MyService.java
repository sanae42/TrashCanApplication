package com.example.trashcanapplication.service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.example.trashcanapplication.MQTT.MyMqttClient;
import com.example.trashcanapplication.MainActivity;
import com.example.trashcanapplication.R;
import com.example.trashcanapplication.TrashCanBean;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyService extends Service {

    Boolean ifLogin = false;

    JSONObject jsonData = null;

    private MyMqttClient myMQTTClient;

    //sp数据库 存放应用设置状态
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public MyService() {
        myMQTTClient = MyMqttClient.getInstance();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //使用EventBus与线程交流
        //TODO：也可以使用handler
//        https://blog.csdn.net/x97666/article/details/125172129
//        https://blog.csdn.net/android410223Sun/article/details/123183448
        EventBus.getDefault().register(this);

        // SharedPreference
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        new Thread(new Runnable() {
            @Override
            //读取会议数据发送状态栏通知
            public void run() {
                try {
                    // 刷新登录状态
                    ifLogin = pref.getBoolean("ifLogin", false);
                    if(ifLogin){
//                        work();
                    }
                    Thread.sleep(5000L);
                } catch (Exception e) {
                    Log.d("线程退出", e.getMessage());
                }
            }
        }).start();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 60 * 60 * 1000;
        int fiveSecond = 5 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + fiveSecond;
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_IMMUTABLE);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        return super.onStartCommand(intent, flags, startId);
    }

    /***
     * EventBus回调
     * 如果使用事件处理函数指定了线程模型为MainThread，那么不论事件是在哪个线程中发布出来的，
     * 该事件处理函数都会在UI线程中执行。该方法可以用来更新UI，但是不能处理耗时操作。
     * https://blog.csdn.net/weixin_42602900/article/details/127785935
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String s) {
        //TODO:此处需考虑JSON损坏，转换失败的情况
        // {"sender":"myMqttClient","dataType":"allTrashCanData","payload":[{"Id":1,"Distance":1179,"Humidity":54,"Temperature":23,"Latitude":52.445978,"Longitude":-1.935167,"Depth":100,"LastEmptyTime":{"date":26,"day":3,"hours":18,"minutes":38,"month":6,"nanos":0,"seconds":9,"time":1690393089000,"timezoneOffset":-60,"year":123},"EstimatedTime":58,"Variance":0,"LocationDescription":"Near the UOB South Gate","Mode":"1"},{"Id":2,"Distance":33,"Humidity":333,"Temperature":3333,"Latitude":52.444256,"Longitude":-1.934156,"Depth":80,"LastEmptyTime":{"date":12,"day":3,"hours":1,"minutes":8,"month":6,"nanos":0,"seconds":40,"time":1689120520000,"timezoneOffset":-60,"year":123},"EstimatedTime":111,"Variance":222,"LocationDescription":"At the entrance of McDonald's","Mode":"2"}]}
        JSONObject j;
        try {
            j = new JSONObject(s);
            String sender = j.getString("sender");
            String dataType = j.getString("dataType");
            //接收到的JSON数据为，全部垃圾桶状态数据
            if (sender.equals("myMqttClient") && dataType.equals("allTrashCanData")) {
                if(!ifLogin){
                    return;
                }
                jsonData = j;
            }

        } catch (Exception e) {
            Log.d("通知log", "通知log"+e.getMessage());
        }
    }

    private void work() {
        if(jsonData == null){
            return;
        }

        try {
            String payload = jsonData.getString("payload");
            JSONArray jsonArray = new JSONArray(payload);
            List<TrashCanBean> trashCanList = new ArrayList();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = null;

                jsonObj = jsonArray.getJSONObject(i);
                //将JSONObject的数据放入trashCanBean对象
                TrashCanBean trashCanBean = new TrashCanBean();
                trashCanBean.setId(jsonObj.getInt("Id"));
                trashCanBean.setDistance(jsonObj.getInt("Distance"));
                trashCanBean.setHumidity(jsonObj.getInt("Humidity"));
                trashCanBean.setTemperature(jsonObj.getInt("Temperature"));
                trashCanBean.setLatitude(jsonObj.getDouble("Latitude"));
                trashCanBean.setLongitude(jsonObj.getDouble("Longitude"));

                trashCanBean.setDepth(jsonObj.getInt("Depth"));
                trashCanBean.setEstimatedTime(jsonObj.getInt("EstimatedTime"));
                trashCanBean.setVariance(jsonObj.getInt("Variance"));

                trashCanBean.setLocationDescription(jsonObj.getString("LocationDescription"));
                trashCanBean.setMode(jsonObj.getString("Mode"));
                //时间戳转date
                Long timeStamp = Long.parseLong(jsonObj.getJSONObject("LastEmptyTime").getString("time"));
                Date LastEmptyTime = new Date(timeStamp);
                trashCanBean.setLastEmptyTime(LastEmptyTime);

                //将trashCanBean对象插入trashCanList
                trashCanList.add(trashCanBean);
            }

            //发送火灾报警通知
            List<Integer> emergencyIdList = new ArrayList<>();
            for(TrashCanBean bean:trashCanList){
                //假定45℃为警报温度
                if(bean.getTemperature()>45){
                    emergencyIdList.add(bean.getId());
                }
            }
            if(emergencyIdList.size() == 0){

            }else {
                String title = "emergency";
                String text = "There are " + emergencyIdList.size() + " trash cans currently on fire";
                makeNotification(title, text);
            }

        } catch (Exception e) {
            Log.d("通知log", "通知log "+e.getMessage());
        }
    }

    private String createNotificationChannel(String channelID, String channelNAME, int level) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(channelID, channelNAME, level);
            manager.createNotificationChannel(channel);
            return channelID;
        } else {
            return null;
        }
    }

    private void makeNotification(String title, String text) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        String channelId = createNotificationChannel("my_channel_ID", "my_channel_NAME", NotificationManager.IMPORTANCE_HIGH);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.icon_emergency)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(100, notification.build());
    }
}
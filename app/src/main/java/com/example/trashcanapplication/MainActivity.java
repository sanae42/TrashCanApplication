package com.example.trashcanapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trashcanapplication.MQTT.MyMqttClient;
import com.example.trashcanapplication.activityCollector.BaseActivity;
import com.example.trashcanapplication.login.LoginActivity;
import com.example.trashcanapplication.setting.SettingActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BaseActivity implements OnMapReadyCallback {
    //登录状态
    private Boolean ifLogin;

    //sp数据库 存放应用设置状态
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    //是否完成授权
    Boolean isPermissionGranter;
    //谷歌地图控件
    MapView mapView;
    GoogleMap googleMap;
    //MQTT
    private MyMqttClient myMQTTClient;
    private static Integer Id = 1;
    private String ClientId = "Android/"+Id;

    JSONObject jsonData;

    //主界面主体布局
    private NestedScrollView loggedMainView;
    private RelativeLayout unloggedMainView;

    //搜索栏
    private SearchView mSearchView = null;
    //    侧边栏
    private DrawerLayout mDrawerLayout;
    //  侧边栏头部布局
    private View headview;
    RelativeLayout loggedHeaderLayout;
    RelativeLayout unloggedHeaderLayout;
    //数字输入框
    private EditText editText;
    //进度条窗口
    private ProgressDialog progressDialog;

    //垃圾桶信息列表
    private List<TrashCanBean> trashCanList = new ArrayList();
    //地图标点列表
    private List<Marker> markerList = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initiateView();

        checkGPS();
        checkPermission();
        if (isPermissionGranter) {
            if (checkGooglePlayServices()) {
                mapView.getMapAsync(this);
                mapView.onCreate(savedInstanceState);
            } else {
                Toast.makeText(MainActivity.this, "google play 服务不可用", Toast.LENGTH_SHORT).show();
            }
        }
        /* -------------------------------------------------------------------------------------- */
        myMQTTClient = MyMqttClient.getInstance();
        //初始化连接
        myMQTTClient.start();
        //订阅主题
        myMQTTClient.subTopic("MQTTServerPub");
        myMQTTClient.subTopic(ClientId);
        myMQTTClient.publishMessage("testtopic/1", "安卓客户端连接测试", 0);
        /* -------------------------------------------------------------------------------------- */
        //使用EventBus与线程交流
        //TODO：也可以使用handler
//        https://blog.csdn.net/x97666/article/details/125172129
//        https://blog.csdn.net/android410223Sun/article/details/123183448
        EventBus.getDefault().register(this);

        // SharedPreference
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        refreshViewAccordingToLoginState();


        //如果登陆，展示进度条
        if(ifLogin){
            try{
                progressDialog = ProgressDialog.show(this,"加载中","正在努力加载");
            }catch (Exception e){
                Log.d("进度条窗口闪退", e.getMessage());
            }
            Timer timer = new Timer();
            //TimerTask属于子线程，不能执行toast “Can't toast on a thread that has not called Looper.prepare()”
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if(progressDialog!=null&&progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    timer.cancel();
                }
            };
            //6000ms执行一次
            timer.schedule(task, 6000);
        }
    }

    /**
     * 初始化控件
     */
    private void initiateView(){
        //主界面主体布局
        loggedMainView = (NestedScrollView)findViewById(R.id.logged_main_view);
        unloggedMainView = (RelativeLayout)findViewById(R.id.unlogged_main_view);
        //谷歌地图视图
        mapView = findViewById(R.id.mapView);
        //搜索栏
        mSearchView = (SearchView) findViewById(R.id.searchView);
        //去除搜索栏底部横线
        if (mSearchView != null) {
            try {        //--拿到字节码
                Class<?> argClass = mSearchView.getClass();
                //--指定某个私有属性,mSearchPlate是搜索框父布局的名字
                Field ownField = argClass.getDeclaredField("mSearchPlate");
                //--暴力反射,只有暴力反射才能拿到私有属性
                ownField.setAccessible(true);
                View mView = (View) ownField.get(mSearchView);
                //--设置背景
                mView.setBackgroundColor(getColor(R.color.transparent));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //设置搜索栏监听
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (s != null) {
                    if (isNumber(s)) {
                        //搜索的是数字
                        int num = Integer.parseInt(s);
                        boolean ifFind = false;
                        for(Marker marker: markerList){
                            //从所有marker中找到所需marker，移动到对应位置，并显示对应信息窗口
                            TrashCanBean bean = (TrashCanBean)marker.getTag();
                            if(bean .getId() == num){
                                LatLng latLng = new LatLng(bean.getLatitude(), bean.getLongitude());
                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                                googleMap.animateCamera(cameraUpdate);
                                marker.showInfoWindow();

                                ifFind = true;
                                break;
                            }
                        }
                        if(!ifFind){
                            Toast.makeText(MainActivity.this, "未找到垃圾桶："+num, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        //搜索的是字符
                        //Geocoder：根据经纬度获取详细地址信息 / 根据详细地址获取经纬度信息
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        try {
                            List<Address> addressList = geocoder.getFromLocationName(s, 1);
                            if (addressList.size() > 0) {
                                LatLng latLng = new LatLng(addressList.get(0).getLatitude(), addressList.get(0).getLongitude());
                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.title("Search Position");
                                markerOptions.position(latLng);
                                googleMap.addMarker(markerOptions);
                                // 移动相机视角
                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                                googleMap.animateCamera(cameraUpdate);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        //        导航条
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        设置actionbar相同功能
        setSupportActionBar(toolbar);

        //数字输入文本框
        editText = (EditText) findViewById(R.id.trashcans_tobefilled_edittext);
        editText.setText("0");
        //只能输入数字和小数点
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                refreshPlanView();

//                Toast.makeText(MainActivity.this, editText.getText(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        //    侧边栏布局
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        //   获取actionbar实例，设置actionbar（即toolbar）最左侧按钮功能，点击唤出侧边栏
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_menu_24);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        //  设置NavigationView布局
        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if(id == R.id.nav_chat){
                    if(!ifLogin){
                        Toast.makeText(MainActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(MainActivity.this, "点击了nav_chat", Toast.LENGTH_SHORT).show();
                    }
                }
                if(id == R.id.nav_user){
                    if(!ifLogin){
                        Toast.makeText(MainActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(MainActivity.this, "点击了nav_user", Toast.LENGTH_SHORT).show();
                    }
                }
                if(id == R.id.nav_setting){
                    if(!ifLogin){
                        Toast.makeText(MainActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                    }else {
//                        Toast.makeText(MainActivity.this, "点击了nav_setting", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        intent.setClass(getApplicationContext(), SettingActivity.class);
                        startActivity(intent);
                    }
                }
                if(id == R.id.nav_more){
                    Toast.makeText(MainActivity.this, "点击了nav_more", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        headview=navView.inflateHeaderView(R.layout.nav_header);
        loggedHeaderLayout = (RelativeLayout)headview.findViewById(R.id.LoggedHeaderLayout);
        unloggedHeaderLayout = (RelativeLayout)headview.findViewById(R.id.UnloggedHeaderLayout);
        Button loginButton = (Button) headview.findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * 根据登录状态刷新布局
     */
    private void refreshViewAccordingToLoginState(){
        ifLogin = pref.getBoolean("ifLogin", false);
        if(ifLogin){
            loggedMainView.setVisibility(View.VISIBLE);
            unloggedMainView.setVisibility(View.GONE);

            loggedHeaderLayout.setVisibility(View.VISIBLE);
            unloggedHeaderLayout.setVisibility(View.GONE);
        }else {
            loggedMainView.setVisibility(View.GONE);
            unloggedMainView.setVisibility(View.VISIBLE);

            loggedHeaderLayout.setVisibility(View.GONE);
            unloggedHeaderLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 根据输入的hour刷新plan布局
     */
    public void refreshPlanView(){
        if(trashCanList==null){
            return;
        }
        double hour = 0;
        if(editText.getText()!=null && editText.getText().toString().length()>0){
            hour = Double.parseDouble(editText.getText().toString());
        }else {
            hour = 0;
        }
        List<TrashCanBean> toBeFilledList = new ArrayList<>();
        for(TrashCanBean bean : trashCanList){
            double percentage = (double)(bean.getDepth()-bean.getDistance()) / (double)bean.getDepth();
            //认为0.9为满
            double time = (double)(0.9-percentage) * bean.getEstimatedTime();
            double instability = Math.log( 1 + Math.pow(bean.getVariance(), 0.5) );
            if((double)(0.9-percentage) * bean.getEstimatedTime() - instability < hour){
                toBeFilledList.add(bean);
            }
        }
        TextView toBeFilledNumberTextView = (TextView) findViewById(R.id.trashcans_tobefilled_number_textview);
        TextView toBeFilledListTextView = (TextView) findViewById(R.id.trashcans_tobefilled_list_textview);
        if(toBeFilledList.size() > 0){
            toBeFilledNumberTextView.setText("it is expected that " + toBeFilledList.size() + " trash cans will be filled :");
            String str = "";
            List<Integer> planIdList = new ArrayList<>();
            for(int i = 0; i < toBeFilledList.size(); i++){
                if(i!=0){
                    str += ", ";
                }
                str += toBeFilledList.get(i).getId();
                planIdList.add(toBeFilledList.get(i).getId());
            }
            toBeFilledListTextView.setText(str);
            toBeFilledListTextView.setVisibility(View.VISIBLE);

            //清空地图标记列表
            markerList.clear();
            //会导致打开的infowindow关闭
            googleMap.clear();
            //刷新地图标记
            setMarkerIcon(planIdList);
        }else {
            toBeFilledNumberTextView.setText("it is expected that no trash can will be filled");
            toBeFilledListTextView.setVisibility(View.GONE);
        }

    }

    /**
     * 菜单按键监听，此处菜单即toolbar上一系列按键
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //开启侧边栏
        if(id == android.R.id.home){
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
        if(id == R.id.button1){
            Toast.makeText(MainActivity.this, "点击了button1", Toast.LENGTH_SHORT).show();
        }
        if(id == R.id.button2){
            Toast.makeText(MainActivity.this, "点击了button2", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    /**
     * 菜单创建
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    /**
     * 判断是否为数字
     */
    private boolean isNumber(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (Exception e) {
            return false;
        }
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
            // TODO:客户端每一段时间发送一次数据，会导致marker刷新，打开的infowindow会关闭；可以只在oncreate时接收一次数据，之后手动刷新；或之后不再clear marker
            if (sender.equals("myMqttClient") && dataType.equals("allTrashCanData")) {
                if(!ifLogin){
                    return;
                }
                jsonData = j;
                refreshMapRelatedViewAfterReceiveJSONData();
                refreshPlanView();
                //加载完成，收起进度条
                if(progressDialog!=null){
                    progressDialog.dismiss();
                }
            }
            if (sender.equals("myMqttClient") && dataType.equals("loginReplyData")) {
                if(j.getString("result").equals("succeeded")){
                    editor.putBoolean("ifLogin", true);
                    editor.apply();
                    refreshViewAccordingToLoginState();
                }else {

                }
            }
            if (sender.equals("myMqttClient") && dataType.equals("registerReplyData")) {
                if(j.getString("result").equals("succeeded")){
                    editor.putBoolean("ifLogin", true);
                    editor.apply();
                    refreshViewAccordingToLoginState();
                }else {

                }
            }

        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "JSON转换出错 :" + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("登录问题", e.getMessage());
        }
    }

    /**
     * 收到JSON数据后刷新主活动相关布局
     */
    private void refreshMapRelatedViewAfterReceiveJSONData(){
        String payload = null;
        //地图布局
        try {
            payload = jsonData.getString("payload");
            JSONArray jsonArray = new JSONArray(payload);

            //清空垃圾桶数据表，重新放入数据
            trashCanList.clear();
            //清空地图标记列表
            markerList.clear();
            //清除地图标记
            //TODO: marker刷新会导致打开的infowindow关闭
            googleMap.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = null;
                try {
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

//                        MarkerOptions markerOptions = new MarkerOptions();
//                        markerOptions.title("Trash Can " + trashCanBean.getId());
//                        markerOptions.position(latLng);
//                        googleMap.addMarker(markerOptions);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,  e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("地图icon问题", e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            //设置地图标记
            setMarkerIcon(null);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        //emergency布局
        ImageView emergencyImageview = findViewById(R.id.emergency_imageview);
        TextView emergencyTextview = findViewById(R.id.emergency_textview);
        List<Integer> emergencyIdList = new ArrayList<>();
        for(TrashCanBean bean:trashCanList){
            //假定45℃为警报温度
            if(bean.getTemperature()>45){
                emergencyIdList.add(bean.getId());
            }
        }
        if(emergencyIdList.size() == 0){
            emergencyImageview.setImageResource(R.drawable.icon_no_emergency);
            emergencyTextview.setText("No emergency");
        }else {
            emergencyImageview.setImageResource(R.drawable.icon_emergency);
            String str = "There are " + emergencyIdList.size() + " emergencies:\n";
            for (int i = 0; i < emergencyIdList.size(); i++){
                if(i!=0){
                    str += ",";
                }
                str += emergencyIdList.get(i).toString();
            }
            emergencyTextview.setText(str);
        }

    }

    /**
     * 判断网络是否连接
     */
//    private boolean isConnectIsNomarl() {
//        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
//        @SuppressLint("MissingPermission") NetworkInfo info = connectivityManager.getActiveNetworkInfo();
//
//        if (info != null && info.isConnected()) {
//            String name = info.getTypeName();
//            return true;
//        } else {
//            return false;
//        }
//    }


    /***
     * 检查google play 服务是否可用
     */
    private Boolean checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int result = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (result == ConnectionResult.SUCCESS) {
            return true;
        } else if (googleApiAvailability.isUserResolvableError(result)) {
            Dialog dialog = googleApiAvailability.getErrorDialog(this, result, 201, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                }
            });
            dialog.show();
        }

        return false;
    }

    /***
     * 使用Dexter进行动态权限申请
     */
    private void checkPermission() {
        //多个权限
//        Dexter.withContext(this).withPermissions(
//                Manifest.permission.INTERNET
//                ,Manifest.permission.ACCESS_NETWORK_STATE
//        ).withListener(new MultiplePermissionsListener() {
//            @Override
//            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
//                if(multiplePermissionsReport.areAllPermissionsGranted()){
//                    Toast.makeText(MainActivity.this, "全部授权完成", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
//                permissionToken.continuePermissionRequest();
//            }
//        }).check();
        //单个权限
        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                isPermissionGranter = true;
                Toast.makeText(MainActivity.this, "授权成功", Toast.LENGTH_SHORT).show();
            }

            //拒绝：跳转到应用详情
            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), "");
                intent.setData(uri);
                startActivity(intent);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }

    /***
     * 设置垃圾桶图标样式
     */
    private void setMarkerIcon(List<Integer> planIdSet){
        if(trashCanList == null || trashCanList.size() == 0){
            return;
        }

        //bitmap_normal
        Bitmap bitmap_normal = BitmapFactory.decodeResource(getResources(), R.drawable.location_normal);
        int width = bitmap_normal.getWidth();
        int height = bitmap_normal.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(0.14f,0.14f);
        bitmap_normal=Bitmap.createBitmap(bitmap_normal,0,0,width,height,matrix,true);
        //bitmap_alert
        Bitmap bitmap_alert = BitmapFactory.decodeResource(getResources(), R.drawable.location_alert);
        bitmap_alert=Bitmap.createBitmap(bitmap_alert,0,0,width,height,matrix,true);
        //bitmap_full
        Bitmap bitmap_full = BitmapFactory.decodeResource(getResources(), R.drawable.location_full);
        bitmap_full=Bitmap.createBitmap(bitmap_full,0,0,width,height,matrix,true);
        //bitmap_plan
        Bitmap bitmap_plan = BitmapFactory.decodeResource(getResources(), R.drawable.location_plan);
        bitmap_plan=Bitmap.createBitmap(bitmap_plan,0,0,width,height,matrix,true);

        for(TrashCanBean bean : trashCanList){
            double dis = bean.getDistance();
            double dep = bean.getDepth();
            LatLng latLng = new LatLng(bean.getLatitude(), bean.getLongitude());

            if(bean.getTemperature() > 45){
                Marker marker = googleMap.addMarker(
                        new MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap_alert)));
                //将数据与标记关联,使用 Marker.setTag() 通过标记来存储任意数据对象，并可使用 Marker.getTag() 检索该数据对象
                marker.setTag(bean);
                markerList.add(marker);
                continue;
            }
            if((dep-dis)/dep > 0.9){
                Marker marker = googleMap.addMarker(
                        new MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap_full)));
                //将数据与标记关联,使用 Marker.setTag() 通过标记来存储任意数据对象，并可使用 Marker.getTag() 检索该数据对象
                marker.setTag(bean);
                markerList.add(marker);
                continue;
            }
            if(planIdSet!=null && planIdSet.contains(bean.getId())){
                Marker marker = googleMap.addMarker(
                        new MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap_plan)));
                //将数据与标记关联,使用 Marker.setTag() 通过标记来存储任意数据对象，并可使用 Marker.getTag() 检索该数据对象
                marker.setTag(bean);
                markerList.add(marker);
                continue;
            }
            if(true){
                Marker marker = googleMap.addMarker(
                        new MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap_normal)));
                //将数据与标记关联,使用 Marker.setTag() 通过标记来存储任意数据对象，并可使用 Marker.getTag() 检索该数据对象
                marker.setTag(bean);
                markerList.add(marker);
                continue;
            }

        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Set a listener for marker click.
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                marker.showInfoWindow();
//                marker.getTag();
                return false;
            }
        });

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(@NonNull Marker marker) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), TrashCanDetailActivity.class);
                TrashCanBean bean = (TrashCanBean) marker.getTag();
                intent.putExtra("TrashCanId",bean.getId());
                intent.putExtra("Depth",bean.getDepth());
                intent.putExtra("Mode",bean.getMode());
//                Toast.makeText(MainActivity.this, "活动跳转传参:"+bean.getId(), Toast.LENGTH_SHORT).show();
                startActivity(intent);
            }
        });

        map.setOnInfoWindowLongClickListener(new GoogleMap.OnInfoWindowLongClickListener() {
            @Override
            public void onInfoWindowLongClick(@NonNull Marker marker) {

            }
        });

        map.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
            @Override
            public void onInfoWindowClose(@NonNull Marker marker) {

            }
        });

        //自定义信息窗口的内容和设计
        //https://developers.google.com/maps/documentation/android-sdk/infowindows?hl=zh-cn
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Nullable
            @Override
            public View getInfoContents(@NonNull Marker marker) {
                View view;
                view = View.inflate(getApplicationContext(), R.layout.info_window, null);
                TextView title = view.findViewById(R.id.info_window_title);
                TextView content = view.findViewById(R.id.info_window_content);
                TrashCanBean bean = (TrashCanBean)marker.getTag();
                title.setText("TrashCan:"+bean.getId());
                String str = "TrashLevel:"+(int)((double)(bean.getDepth()-bean.getDistance())/(double)bean.getDepth()*100);
                str += "%\n"+"Temperature:"+bean.getTemperature()+"°C";
                content.setText(str);

                Button button = view.findViewById(R.id.info_window_button);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
//                        Intent intent = new Intent();
//                        intent.setClass(getApplicationContext(), TrashCanDetailActivity.class);
//                        TrashCanBean bean = (TrashCanBean) marker.getTag();
//                        intent.putExtra("TrashCanId",bean.getId());
//                        startActivity(intent);
                    }
                });


                return view;
            }

            @Nullable
            @Override
            public View getInfoWindow(@NonNull Marker marker) {
                return null;
            }
        });

//        LatLng latLng = new LatLng(52.45092015708054, -1.930555058272646);
//        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.title("UOB");
//        markerOptions.position(latLng);
//        googleMap.addMarker(markerOptions);
//        // 移动相机视角
//        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
//        googleMap.animateCamera(cameraUpdate);
        // 缩放按钮
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        //获取自己位置
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        googleMap.setMyLocationEnabled(true);
        //显示罗盘和导航toolbar
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        // 移动相机视角到当前位置
        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE); // 位置
        Location mlocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); // 网络
        LatLng latLng = new LatLng(mlocation.getLatitude(), mlocation.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        googleMap.animateCamera(cameraUpdate);
    }

    /***
     * 检查GPS是否开启
     */
    private void checkGPS(){
        LocationManager locationManager
                = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!gps){
            //TODO：此处dialog不显示且会黑屏，可能是未进入应用界面的问题
            Toast.makeText(MainActivity.this, "GPS不可用,请开启GPS", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        //注销EventBus
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
//        此处和官方文档不同：
//  https://developers.google.com/maps/documentation/android-sdk/map?hl=zh-cn#get_a_handle_to_the_fragment_and_register_the_callback
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
package com.example.trashcanapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.trashcanapplication.MQTT.MyMqttClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    //是否完成授权
    Boolean isPermissionGranter;
    //谷歌地图控件
    MapView mapView;
    GoogleMap googleMap;
    //MQTT
    private MyMqttClient myMQTTClient;
    //搜索栏
    private SearchView mSearchView = null;
    //    侧边栏
    private DrawerLayout mDrawerLayout;
    //  侧边栏头部布局
    private View headview;
    //长按位置信息窗口显示详情
    private LinearLayout locationInfoLinearLayout;

    //垃圾桶信息列表
    private List<TrashCanBean> trashCanList = new ArrayList();

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
        //订阅/World这个主题
        myMQTTClient.subTopic("TrashCanPub");
        myMQTTClient.publishMessage("testtopic/1", "安卓客户端连接测试", 0);
        /* -------------------------------------------------------------------------------------- */
        //使用EventBus与线程交流
        //TODO：也可以使用handler
//        https://blog.csdn.net/x97666/article/details/125172129
//        https://blog.csdn.net/android410223Sun/article/details/123183448
        EventBus.getDefault().register(this);
    }

    /**
     * 初始化控件
     */
    private void initiateView(){
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
        //长按位置信息窗口显示详情
        locationInfoLinearLayout = (LinearLayout) findViewById(R.id.locationInfo);

        //        导航条
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        设置actionbar相同功能
        setSupportActionBar(toolbar);

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
                    Toast.makeText(MainActivity.this, "点击了nav_chat", Toast.LENGTH_SHORT).show();
                }
                if(id == R.id.nav_user){
                    Toast.makeText(MainActivity.this, "点击了nav_user", Toast.LENGTH_SHORT).show();
                }
                if(id == R.id.nav_setting){
                    Toast.makeText(MainActivity.this, "点击了nav_setting", Toast.LENGTH_SHORT).show();
                }
                if(id == R.id.nav_more){
                    Toast.makeText(MainActivity.this, "点击了nav_more", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        headview=navView.inflateHeaderView(R.layout.nav_header);
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
        //{"sender":"myMqttClient","dataType":"allTrashCanData","payload":[{"Id":1,"Distance":67,"Humidity":67,"Temperature":67,"Latitude":52.445978,"Longitude":-1.935167},{"Id":2,"Distance":33,"Humidity":333,"Temperature":3333,"Latitude":52.444256,"Longitude":-1.934156}]}
        JSONObject jsonData;
        try {
            jsonData = new JSONObject(s);
            String sender = jsonData.getString("sender");
            String dataType = jsonData.getString("dataType");
            //接收到的JSON数据为，全部垃圾桶状态数据
            if (sender.equals("myMqttClient") && dataType.equals("allTrashCanData")) {
                String payload = jsonData.getString("payload");
                JSONArray jsonArray = new JSONArray(payload);

                //清空垃圾桶数据表，重新放入数据
                trashCanList.clear();

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
                        //将trashCanBean对象插入trashCanList
                        trashCanList.add(trashCanBean);
                        //在地图上标出该点
                        LatLng latLng = new LatLng(trashCanBean.getLatitude(), trashCanBean.getLongitude());
                        Marker marker = googleMap.addMarker(
                                new MarkerOptions()
                                        .position(latLng)
                                        .title("Trash Can " + trashCanBean.getId())
                                        .snippet("distance:"+trashCanBean.getDistance()));
                        //将数据与标记关联,使用 Marker.setTag() 通过标记来存储任意数据对象，并可使用 Marker.getTag() 检索该数据对象
                        marker.setTag(trashCanBean);
//                        MarkerOptions markerOptions = new MarkerOptions();
//                        markerOptions.title("Trash Can " + trashCanBean.getId());
//                        markerOptions.position(latLng);
//                        googleMap.addMarker(markerOptions);
                    } catch (JSONException e) {
                        Toast.makeText(MainActivity.this, "JSON转换出错 :" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        throw new RuntimeException(e);
                    }
                }
            }

        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "JSON转换出错 :" + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Set a listener for marker click.
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                marker.showInfoWindow();
//                marker.getTag();
                return true;
            }
        });

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(@NonNull Marker marker) {

            }
        });

        map.setOnInfoWindowLongClickListener(new GoogleMap.OnInfoWindowLongClickListener() {
            @Override
            public void onInfoWindowLongClick(@NonNull Marker marker) {
                locationInfoLinearLayout.setVisibility(View.VISIBLE);
            }
        });

        map.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
            @Override
            public void onInfoWindowClose(@NonNull Marker marker) {
                locationInfoLinearLayout.setVisibility(View.GONE);
            }
        });

        LatLng latLng = new LatLng(52.45092015708054, -1.930555058272646);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title("UOB");
        markerOptions.position(latLng);
        googleMap.addMarker(markerOptions);
        // 移动相机视角
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        googleMap.animateCamera(cameraUpdate);
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
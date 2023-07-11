package com.example.trashcanapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.trashcanapplication.MQTT.MyMqttClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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

import java.io.IOException;
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
    private String ClientName = "Android";

    //搜索栏
    private SearchView mSearchView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //谷歌地图视图
        mapView = findViewById(R.id.mapView);
        //搜索栏
        //TODO:使用搜索框后，光标一直在搜索框上，mapView无法回到自己的位置
        mSearchView = (SearchView) findViewById(R.id.searchView);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if(s!=null){
                    if(isNumber(s)){
                        //搜索的是数字
                    }else {
                        //搜索的是字符
                        //Geocoder：根据经纬度获取详细地址信息 / 根据详细地址获取经纬度信息
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        try {
                            List<Address>addressList = geocoder.getFromLocationName(s,1);
                            if(addressList.size() > 0){
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

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

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
        myMQTTClient.start(ClientName);
        //订阅/World这个主题
        myMQTTClient.subTopic("TrashCanPub");
        myMQTTClient.publishMessage("testtopic/1","安卓客户端连接测试",0);
        /* -------------------------------------------------------------------------------------- */
        //使用EventBus与线程交流
        //TODO：也可以使用handler
//        https://blog.csdn.net/x97666/article/details/125172129
//        https://blog.csdn.net/android410223Sun/article/details/123183448
        EventBus.getDefault().register(this);
    }
    /**
     * 判断是否为数字
     */
    private boolean isNumber(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch(Exception e){
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
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
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
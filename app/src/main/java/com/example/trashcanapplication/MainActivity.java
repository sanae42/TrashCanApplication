package com.example.trashcanapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
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
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.NetworkInfo;
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
import com.example.trashcanapplication.about.AboutActivity;
import com.example.trashcanapplication.activityCollector.BaseActivity;
import com.example.trashcanapplication.login.LoginActivity;
import com.example.trashcanapplication.setting.SettingActivity;
import com.example.trashcanapplication.setting.UserSettingActivity;
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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Title：MainActivity.java
 * @Description: The main activity is the first activity of the application,
 * essential functions in the application are placed on the main activity interface,
 * and there are also entrances to other activities on the its interface.
 * @author P Geng
 */
public class MainActivity extends BaseActivity implements OnMapReadyCallback {
    //Login status
    private Boolean ifLogin;

    //SharedPreferences
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    //If authorization been completed
    Boolean isPermissionGranter;

    //Google Maps component
    MapView mapView;
    GoogleMap googleMap;

    //MQTT related
    private MyMqttClient myMQTTClient;
    private static String IP;
    private String ClientId;

    JSONObject jsonData;

    //Main interface layout during login and when not logged in
    private NestedScrollView loggedMainView;
    private RelativeLayout unloggedMainView;

    //Search bar
    private SearchView mSearchView = null;
    //sidebar
    private DrawerLayout mDrawerLayout;
    //Sidebar header layout
    private View headview;
    RelativeLayout loggedHeaderLayout;
    RelativeLayout unloggedHeaderLayout;
    //input box
    private EditText editText;
    //Progress Dialog
    private ProgressDialog progressDialog;

    //Trash can information list
    private List<TrashCanBean> trashCanList = new ArrayList();
    //Map marker list
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

        // SharedPreference
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        /* -------------------------------------------------------------------------------------- */

        // At the beginning of the application, set the current IP address for the IP field of SharedPreference and subscribe to this topic.
        // Afterwards, communicate with the server through this topic
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Service.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        List<LinkAddress> linkAddresses = cm.getLinkProperties(cm.getActiveNetwork()).getLinkAddresses();
        //Obtain the network IP address information of the current connection
        if(linkAddresses != null && !linkAddresses.isEmpty()){
            //Use a regular expression to determine whether the IP address meets the requirements.
            String ipRegEx = "^([1-9]|([1-9][0-9])|(1[0-9][0-9])|(2[0-4][0-9])|(25[0-5]))(\\.([0-9]|([1-9][0-9])|(1[0-9][0-9])|(2[0-4][0-9])|(25[0-5]))){3}$";
            Pattern pattern = Pattern.compile(ipRegEx);
            for (int i = 0; i < linkAddresses.size(); i++) {
                InetAddress address = linkAddresses.get(i).getAddress();
                String ipStr = address.getHostAddress();
                Matcher matcher = pattern.matcher(ipStr);
                if (matcher.matches()){
                    editor.putString("ipStr", ipStr);
                    editor.apply();
                }
            }
        }

        /* -------------------------------------------------------------------------------------- */
        //get IP address from SharedPerformance
        IP = pref.getString("ipStr","");
        ClientId = "Android/"+IP;

        myMQTTClient = MyMqttClient.getInstance();
        //start the connection to server
        myMQTTClient.start(ClientId);
        //subscribe the topic to receive messages from the server
        myMQTTClient.subTopic("MQTTServerPub");
        myMQTTClient.subTopic(ClientId);
        myMQTTClient.publishMessage("testtopic/1", "android connection test", 0);
        /* -------------------------------------------------------------------------------------- */
        //Use EventBus to communicate with threads
        EventBus.getDefault().register(this);

        refreshViewAccordingToLoginState();

        //If logged in, display progress bar
        if(ifLogin){
            try{
                progressDialog = ProgressDialog.show(this,"loading","Loading data from the server");
            }catch (Exception e){
                Log.d("The progress bar window flashes back.", e.getMessage());
            }
            Timer timer = new Timer();
            //TimerTask belongs to a child thread and cannot execute toast “Can't toast on a thread that has not called Looper.prepare()”
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if(progressDialog!=null&&progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    timer.cancel();
                }
            };
            //Execute once every 6000ms
            timer.schedule(task, 6000);
        }
    }

    /**
     * Initialize components
     */
    private void initiateView(){
        //Main interface layout
        loggedMainView = (NestedScrollView)findViewById(R.id.logged_main_view);
        unloggedMainView = (RelativeLayout)findViewById(R.id.unlogged_main_view);
        //Google Maps component
        mapView = findViewById(R.id.mapView);
        //Search bar
        mSearchView = (SearchView) findViewById(R.id.searchView);
        //Remove the horizontal line at the bottom of the search bar
        if (mSearchView != null) {
            try {
                Class<?> argClass = mSearchView.getClass();
                Field ownField = argClass.getDeclaredField("mSearchPlate");
                ownField.setAccessible(true);
                View mView = (View) ownField.get(mSearchView);
                mView.setBackgroundColor(getColor(R.color.transparent));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //Set search bar listening
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (s != null) {
                    if (isNumber(s)) {
                        //If the search is for numbers
                        int num = Integer.parseInt(s);
                        boolean ifFind = false;
                        for(Marker marker: markerList){
                            //Find the desired marker from all the markers, move to the corresponding position, and display the corresponding information window
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
                        //If the search is for characters
                        //Geocoder：Obtain detailed address information based on longitude and latitude/Obtain longitude and latitude information based on detailed address
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        try {
                            List<Address> addressList = geocoder.getFromLocationName(s, 1);
                            if (addressList.size() > 0) {
                                LatLng latLng = new LatLng(addressList.get(0).getLatitude(), addressList.get(0).getLongitude());
                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.title("Search Position");
                                markerOptions.position(latLng);
                                googleMap.addMarker(markerOptions);
                                // Mobile camera perspective
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

        //        toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Number input text box
        editText = (EditText) findViewById(R.id.trashcans_tobefilled_edittext);
        editText.setText("0");
        //Only numbers and decimal points can be entered
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

        //    Sidebar Layout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        //   Obtain the actionbar instance, set the leftmost button function of the actionbar (i.e. toolbar),
        //   and click to call up the sidebar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.baseline_menu_24);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        //  Set NavigationView Layout
        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if(id == R.id.nav_user){
                    if(!ifLogin){
                        Toast.makeText(MainActivity.this, "please login", Toast.LENGTH_SHORT).show();
                    }else {
                        Intent intent = new Intent();
                        intent.setClass(getApplicationContext(), UserSettingActivity.class);
                        startActivity(intent);
                    }
                }
                if(id == R.id.nav_setting){
                    if(!ifLogin){
                        Toast.makeText(MainActivity.this, "please login", Toast.LENGTH_SHORT).show();
                    }else {
                        Intent intent = new Intent();
                        intent.setClass(getApplicationContext(), SettingActivity.class);
                        startActivity(intent);
                    }
                }
                if(id == R.id.nav_about){
                    Intent intent = new Intent();
                    intent.setClass(getApplicationContext(), AboutActivity.class);
                    startActivity(intent);
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
     * Refresh layout based on login status
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
     * Refresh the plan layout based on the input hour
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
            //Consider 0.9 as full
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

            //Clear Map marker List
            markerList.clear();
            //Will cause the open infowindow to close！
            googleMap.clear();
            //Refresh map markers
            setMarkerIcon(planIdList);
        }else {
            toBeFilledNumberTextView.setText("it is expected that no trash can will be filled");
            toBeFilledListTextView.setVisibility(View.GONE);
        }

    }

    /**
     * Menu button monitoring, where the menu is a series of buttons on the toolbar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //Open sidebar
        if(id == android.R.id.home){
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
//        if(id == R.id.button1){
//            Toast.makeText(MainActivity.this, "click button1", Toast.LENGTH_SHORT).show();
//        }
//        if(id == R.id.button2){
//            Toast.makeText(MainActivity.this, "click button2", Toast.LENGTH_SHORT).show();
//        }
        return true;
    }

    /**
     * create menu
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    /**
     * Determine if it is a number
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
     *EventBus callback
     *If the thread model is specified as MainThread using event handling functions,
     * then regardless of which thread the event is published in
     *This event handling function will be executed in the UI thread.
     * This method can be used to update the UI, but it cannot handle time-consuming operations.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String s) {
        // {"sender":"myMqttClient","dataType":"allTrashCanData","payload":[{"Id":1,"Distance":1179,"Humidity":54,"Temperature":23,"Latitude":52.445978,"Longitude":-1.935167,"Depth":100,"LastEmptyTime":{"date":26,"day":3,"hours":18,"minutes":38,"month":6,"nanos":0,"seconds":9,"time":1690393089000,"timezoneOffset":-60,"year":123},"EstimatedTime":58,"Variance":0,"LocationDescription":"Near the UOB South Gate","Mode":"1"},{"Id":2,"Distance":33,"Humidity":333,"Temperature":3333,"Latitude":52.444256,"Longitude":-1.934156,"Depth":80,"LastEmptyTime":{"date":12,"day":3,"hours":1,"minutes":8,"month":6,"nanos":0,"seconds":40,"time":1689120520000,"timezoneOffset":-60,"year":123},"EstimatedTime":111,"Variance":222,"LocationDescription":"At the entrance of McDonald's","Mode":"2"}]}
        JSONObject j;
        try {
            j = new JSONObject(s);
            String sender = j.getString("sender");
            String dataType = j.getString("dataType");
            //The received JSON data is, all trash can status data
            ////The client sends data once a period of time, which causes the marker to refresh and the open infowindow to close;
            // It can only receive data once during oncreate and then manually refresh it; Or no longer clear marker after
            if (sender.equals("myMqttClient") && dataType.equals("allTrashCanData")) {
                if(!ifLogin){
                    return;
                }
                jsonData = j;
                refreshMapRelatedViewAfterReceiveJSONData();
                refreshPlanView();
                //Loading completed, retract progress bar
                if(progressDialog!=null){
                    progressDialog.dismiss();
                }
            }
            if (sender.equals("myMqttClient") && dataType.equals("loginReplyData")) {
                if(j.getString("result").equals("succeeded")){
                    editor.putBoolean("ifLogin", true);
                    editor.putString("UserName", j.getString("UserName"));
                    editor.apply();
                    refreshViewAccordingToLoginState();
                }else {

                }
            }
            if (sender.equals("myMqttClient") && dataType.equals("registerReplyData")) {
                if(j.getString("result").equals("succeeded")){
                    editor.putBoolean("ifLogin", true);
                    editor.putString("UserName", j.getString("UserName"));
                    editor.apply();
                    refreshViewAccordingToLoginState();
                }else {

                }
            }
            if (sender.equals("myMqttClient") && dataType.equals("editUserNameReplyData")) {
                if(j.getString("result").equals("succeeded")){
                    editor.putString("UserName", j.getString("UserName"));
                    editor.apply();
                    refreshViewAccordingToLoginState();
                }else {

                }
            }
            if (sender.equals("application") && dataType.equals("exit")) {
                refreshViewAccordingToLoginState();
            }

        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "JSON conversion error :" + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("Login Issues", e.getMessage());
        }
    }

    /**
     * Refresh the layout related to the main activity after receiving JSON data
     */
    private void refreshMapRelatedViewAfterReceiveJSONData(){
        String payload = null;

        try {
            payload = jsonData.getString("payload");
            JSONArray jsonArray = new JSONArray(payload);

            //Empty the trash can data table and reposition the data
            trashCanList.clear();
            //Clear Map Tag List
            markerList.clear();
            //Clear map markers
            //The marker refresh will cause the open infowindow to close
            googleMap.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = null;
                try {
                    jsonObj = jsonArray.getJSONObject(i);
                    //Place JSONObject data into the trashCanBean object
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
                    //Timestamp to date
                    Long timeStamp = Long.parseLong(jsonObj.getJSONObject("LastEmptyTime").getString("time"));
                    Date LastEmptyTime = new Date(timeStamp);
                    trashCanBean.setLastEmptyTime(LastEmptyTime);

                    //Insert trashCanBean object into trashCanList
                    trashCanList.add(trashCanBean);

//                        MarkerOptions markerOptions = new MarkerOptions();
//                        markerOptions.title("Trash Can " + trashCanBean.getId());
//                        markerOptions.position(latLng);
//                        googleMap.addMarker(markerOptions);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,  e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("Map icon issues", e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            //Set map markers
            setMarkerIcon(null);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        //emergency layout
        ImageView emergencyImageview = findViewById(R.id.emergency_imageview);
        TextView emergencyTextview = findViewById(R.id.emergency_textview);
        List<Integer> emergencyIdList = new ArrayList<>();
        for(TrashCanBean bean:trashCanList){
            //Assume 45 ℃ as the alarm temperature.
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

            //Fire Notification
            if(pref.getBoolean("ifNotification", false)){
                String title = "emergency";
                String text = "There are " + emergencyIdList.size() + " trash cans currently on fire";
                makeNotification(title, text);
            }
        }

    }

    /**
     * Determine if the network is connected
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
     * Check if Google Play service is available
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
     * Using Dexter for dynamic permission application
     */
    private void checkPermission() {
        //Multiple permissions
//        Dexter.withContext(this).withPermissions(
//                Manifest.permission.INTERNET
//                ,Manifest.permission.ACCESS_NETWORK_STATE
//        ).withListener(new MultiplePermissionsListener() {
//            @Override
//            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
//                if(multiplePermissionsReport.areAllPermissionsGranted()){
//                    Toast.makeText(MainActivity.this, "Authorization successful", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
//                permissionToken.continuePermissionRequest();
//            }
//        }).check();
        //Individual permission
        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                isPermissionGranter = true;
                Toast.makeText(MainActivity.this, "Authorization successful", Toast.LENGTH_SHORT).show();
            }

            //If rejected: Jump to application details
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
     * Set trash can icon style
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

            if((dep-dis)/dep > 0.9){
                Marker marker = googleMap.addMarker(
                        new MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap_full)));
                //Associate data with tags, use Marker. setTag() to store any data object through tags, and use Marker.
                // getTag() to retrieve the data object
                marker.setTag(bean);
                markerList.add(marker);
                continue;
            }
            if(bean.getTemperature() > 45){
                Marker marker = googleMap.addMarker(
                        new MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap_alert)));
                //Associate data with tags, use Marker. setTag() to store any data object through tags, and use Marker.
                // getTag() to retrieve the data object
                marker.setTag(bean);
                markerList.add(marker);
                continue;
            }
            if(planIdSet!=null && planIdSet.contains(bean.getId())){
                Marker marker = googleMap.addMarker(
                        new MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap_plan)));
                //Associate data with tags, use Marker. setTag() to store any data object through tags, and use Marker.
                // getTag() to retrieve the data object
                marker.setTag(bean);
                markerList.add(marker);
                continue;
            }
            if(true){
                Marker marker = googleMap.addMarker(
                        new MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap_normal)));
                //Associate data with tags, use Marker. setTag() to store any data object through tags, and use Marker.
                // getTag() to retrieve the data object
                marker.setTag(bean);
                markerList.add(marker);
                continue;
            }

        }
    }

    /***
     * Called when the map is ready
     */
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

        //Customize the content and design of the information window
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

        // Zoom button
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        //Get user location
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
        //Display compass and navigation toolbar
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        // Move the camera's perspective to the current position
        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location mlocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        LatLng latLng = new LatLng(mlocation.getLatitude(), mlocation.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        googleMap.animateCamera(cameraUpdate);
    }

    /***
     * Check if GPS is turned on
     */
    private void checkGPS(){
        LocationManager locationManager
                = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!gps){
            //The dialog here is not displayed and will appear black, which may be due to not entering the application interface
            Toast.makeText(MainActivity.this, "GPS not available, please turn on GPS", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    /***
     * create notification related method
     */
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

    /***
     * Create a notification
     */
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
        //unregister EventBus
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
//        This is different from the official document：
//  https://developers.google.com/maps/documentation/android-sdk/map?hl=zh-cn#get_a_handle_to_the_fragment_and_register_the_callback
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
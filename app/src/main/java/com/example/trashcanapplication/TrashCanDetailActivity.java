package com.example.trashcanapplication;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.trashcanapplication.MQTT.MyMqttClient;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.EntryXComparator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class TrashCanDetailActivity extends AppCompatActivity {

    private int TrashCanId;
    private int Depth;

    JSONObject jsonData;

    //MQTT
    private MyMqttClient myMQTTClient;
    private static Integer Id = 1;
    private String ClientId = "Android/"+Id;

    //下拉选择器
    private Spinner spinner;
    //折线图
    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash_can_detail);

        Intent intent = getIntent();
        TrashCanId = intent.getIntExtra("TrashCanId",-1);
        Depth = intent.getIntExtra("Depth",-1);

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

        spinner = findViewById(R.id.action_bar_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.planets_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                if(jsonData!=null){
                    setChartData(adapterView.getSelectedItem().toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //折线图表组件
        lineChart = findViewById(R.id.line_chart);
        //初始化图表
        initChart();

        myMQTTClient = MyMqttClient.getInstance();
        //使用EventBus与线程交流
        EventBus.getDefault().register(this);
        //向服务器发送垃圾桶数据请求
        JSONObject jsonData = new JSONObject();

        try {
            jsonData.put("dataType", "trashCanDataRequest");
            jsonData.put("Id", ClientId );
            jsonData.put("TrashCanId", TrashCanId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        MyMqttClient myMQTTClient = MyMqttClient.getInstance();
        myMQTTClient.publishMessage("MQTTServerSub",jsonData.toString(),0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String s) {
        //TODO:此处需考虑JSON损坏，转换失败的情况
        //{"sender":"myMqttClient","dataType":"allTrashCanData","payload":[{"Id":1,"Distance":67,"Humidity":67,"Temperature":67,"Latitude":52.445978,"Longitude":-1.935167},{"Id":2,"Distance":33,"Humidity":333,"Temperature":3333,"Latitude":52.444256,"Longitude":-1.934156}]}
        try {
            JSONObject j = new JSONObject(s);
            String sender = j.getString("sender");
            String dataType = j.getString("dataType");
            //接收到的JSON数据为，某个垃圾桶的历史数据
            if (sender.equals("myMqttClient") && dataType.equals("thisTrashCanData")){

                jsonData = j;
                setChartData(spinner.getSelectedItem().toString());

            }

        } catch (Exception e) {
            Toast.makeText(TrashCanDetailActivity.this, "JSON转换出错 :" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //初始化图表
    private void initChart(){
        //点击监听
        //chart.setOnChartValueSelectedListener(this);
        //绘制网格线
        lineChart.setDrawGridBackground(false);

        //描述文本
        lineChart.getDescription().setEnabled(false);

        //是否可以触摸
        lineChart.setTouchEnabled(true);

        //启用缩放和拖动
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);

        // 如果禁用，可以分别在x轴和y轴上进行缩放
        lineChart.setPinchZoom(true);

        //设置背景色
        // chart.setBackgroundColor(Color.GRAY);

        //创建自定义MarkerView（扩展MarkerView）并指定布局
        //MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);
        //mv.setChartView(chart); // For bounds control
        //chart.setMarker(mv); // Set the marker to the chart
//
//        //配置x坐标数据
//        XAxis xl = lineChart.getXAxis();
//        xl.setAvoidFirstLastClipping(true);
//        xl.setAxisMinimum(0f);

//    https://blog.csdn.net/qq_29848853/article/details/130868720
//    file:///C:/Users/admin/Downloads/android%E6%97%B6%E9%97%B4%E5%9D%90%E6%A0%87%E6%8A%98%E7%BA%BF%E5%9B%BEMPAndr%20(1).html
        //TODO：改写横坐标格式，但是这种方法有可能会有重复的横坐标
        SimpleDateFormat mFormat = new SimpleDateFormat("M月d");
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(new Date((long) value));
            }
        });
//
        //配置y坐标左边数据
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setInverted(true);
        rightAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
//
        //关闭y坐标左边数据
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setEnabled(false);

//        setData();
    }

    private void setChartData(String SpinnerSelection) {
        try {
            String payload = jsonData.getString("payload");
            JSONArray jsonArray = new JSONArray(payload);

            System.out.println("JSON数据打印："+jsonData.toString());

            ArrayList<Entry> entries = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
//                    TrashCanBean trashCanBean = new TrashCanBean();
//                    trashCanBean.setId(jsonObj.getInt("Id"));
//                    trashCanBean.setDistance(jsonObj.getInt("Distance"));
//                    trashCanBean.setHumidity(jsonObj.getInt("Humidity"));
//                    trashCanBean.setTemperature(jsonObj.getInt("Temperature"));

                Long timeStamp = Long.parseLong(jsonObj.getJSONObject("DateTime").getString("time"));
//                    timeStamp /= 1000;

                float xVal = (timeStamp);
                float yVal = 0;
                if(SpinnerSelection.equals("Trash Amount")){
                    float distance = (float) (jsonObj.getInt("Distance"));
                    float amount = Depth - distance;
                    yVal = amount;
                }else if(SpinnerSelection.equals("Temperature")){
                    yVal = (float) (jsonObj.getInt("Temperature"));
                }else if(SpinnerSelection.equals("Humidity")){
                    yVal = (float) (jsonObj.getInt("Humidity"));
                }
                entries.add(new Entry(xVal, yVal));

            }
            //通过x坐标值排序
            Collections.sort(entries, new EntryXComparator());
            LineDataSet set1 = new LineDataSet(entries, "DataSet 1");
            //使用数据集创建数据对象
            LineData data = new LineData(set1);
            lineChart.setData(data);
            //刷新绘图
            lineChart.invalidate();
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
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
package com.example.trashcanapplication;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trashcanapplication.MQTT.MyMqttClient;
import com.example.trashcanapplication.activityCollector.BaseActivity;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.github.mikephil.charting.utils.MPPointF;

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
import java.util.Timer;
import java.util.TimerTask;

public class TrashCanDetailActivity extends BaseActivity {

    private int TrashCanId;
    private int Depth;
    private String Mode;

    JSONObject jsonData;

    //MQTT
    private MyMqttClient myMQTTClient = null;
    private static Integer Id = 1;
    private String ClientId = "Android/"+Id;

    //折线图的下拉选择器
    private Spinner spinnerLinechart;
    //折线图
    private LineChart lineChart;
    //饼图
    private PieChart pieChart;
    //进度条窗口
    private ProgressDialog progressDialog;

    //垃圾桶模式的下拉选择器
    private Spinner spinnerTrashcanMode;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash_can_detail);

        Intent intent = getIntent();
        TrashCanId = intent.getIntExtra("TrashCanId",-1);
        Depth = intent.getIntExtra("Depth",-1);
        Mode = intent.getStringExtra("Mode");

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

        spinnerLinechart = findViewById(R.id.spinner_linechart);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> spinnerLinechartAdapter = ArrayAdapter.createFromResource(this,
                R.array.linechart_type_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        spinnerLinechartAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinnerLinechart.setAdapter(spinnerLinechartAdapter);
        spinnerLinechart.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                if(jsonData!=null){
                    setLineChartData(adapterView.getSelectedItem().toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //垃圾桶模式
        spinnerTrashcanMode = findViewById(R.id.spinner_trashcan_mode);
        ArrayAdapter<CharSequence> spinnerTrashcanModeAdapter = ArrayAdapter.createFromResource(this,
                R.array.trashcan_mode_array, android.R.layout.simple_spinner_item);
        spinnerTrashcanModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinnerTrashcanMode.setAdapter(spinnerTrashcanModeAdapter);
        //最初选中此垃圾桶Mode对应的模式选项
        spinnerTrashcanMode.setSelection(Integer.parseInt(Mode)-1);
        spinnerTrashcanMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(myMQTTClient!=null){
                    String msg = String.valueOf(i+1);
                    myMQTTClient.publishMessage("TrashCanSub/"+TrashCanId,msg,0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //折线图表组件
        lineChart = findViewById(R.id.line_chart);
        //初始化图表
        initLineChart();
        //饼状图表组件
        pieChart = findViewById(R.id.pie_chart);
        //初始化图表
        initPieChart();


        myMQTTClient = MyMqttClient.getInstance();
        //使用EventBus与线程交流
        EventBus.getDefault().register(this);
        //向服务器发送垃圾桶数据请求
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("dataType", "trashCanDataRequest");
            jsonObject.put("Id", ClientId );
            jsonObject.put("TrashCanId", TrashCanId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        MyMqttClient myMQTTClient = MyMqttClient.getInstance();
        myMQTTClient.publishMessage("MQTTServerSub",jsonObject.toString(),0);

        //展示进度条
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
                if(progressDialog.isShowing()){
                    progressDialog.dismiss();
                }
                timer.cancel();
            }
        };
        //6000ms执行一次
        timer.schedule(task, 6000);
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
                setLineChartData(spinnerLinechart.getSelectedItem().toString());
                //设置数据
                setPieChartData();
                //取消进度条
                progressDialog.dismiss();
            }

        } catch (Exception e) {
            Toast.makeText(TrashCanDetailActivity.this, "JSON转换出错 :" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //初始化图表
    private void initLineChart(){
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

    //初始化图表
    private void initPieChart() {
        //是否用于百分比数据
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);

        pieChart.setDragDecelerationFrictionCoef(0.95f);

        //设置中间文本的字体
        //chart.setCenterTextTypeface(tfLight);
        //chart.setCenterText(generateCenterSpannableText());

        //是否绘制中心圆形区域和颜色
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);

        //是否绘制中心边透明区域
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);

        //绘制中中心圆，和圆边的边框大小
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);

        //是否绘制中心区域文字
        pieChart.setDrawCenterText(true);

        //默认旋转角度
        pieChart.setRotationAngle(0);
        //通过触摸启用图表的旋转
        pieChart.setRotationEnabled(true);
        //触摸进行高亮的突出设置
        pieChart.setHighlightPerTapEnabled(true);

        //设置单位
        // chart.setUnit(" €");
        // chart.setDrawUnitsInChart(true);

        //添加选择侦听器
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                //选中的扇页
            }

            @Override
            public void onNothingSelected() {
                //未选中的扇页
            }
        });

        //动画
        pieChart.animateY(1400, Easing.EaseInOutQuad);
        // chart.spin(2000, 0, 360);

        //图例
        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);

        //标签样式
        pieChart.setEntryLabelColor(Color.WHITE);
        //chart.setEntryLabelTypeface(tfRegular);
        pieChart.setEntryLabelTextSize(12f);
    }

    private void setLineChartData(String SpinnerSelection) {
        try {
            String payload = jsonData.getString("payload");
            JSONArray jsonArray = new JSONArray(payload);

            System.out.println("JSON数据打印："+jsonData.toString());

            ArrayList<Entry> entries = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);

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

    private void setPieChartData(){
        //从JSONdata里分析数据
        Long fullTime = 0L;
        Long unfullTime = 0L;
        try {
            String payload = jsonData.getString("payload");
            JSONArray jsonArray = new JSONArray(payload);

            Long lastTimeStamp = Long.parseLong(jsonArray.getJSONObject(0).getJSONObject("DateTime").getString("time"));
            for (int i = 1; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);

                Long thisTimeStamp = Long.parseLong(jsonObj.getJSONObject("DateTime").getString("time"));
                int distance = jsonObj.getInt("Distance");
                if((float)(Depth - distance)/(float)Depth > 0.9){
                    fullTime += thisTimeStamp-lastTimeStamp;
//                    fullTime++;
                }else {
                    unfullTime += thisTimeStamp-lastTimeStamp;
//                    unfullTime++;
                }

                lastTimeStamp = thisTimeStamp;
            }
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        //二维数据的二级数据
        ArrayList<PieEntry> entries = new ArrayList<>();
        //new PieEntry(数值，描述，图标icon)第一个
        entries.add(new PieEntry((float)unfullTime, "unfullTime", null));
        entries.add(new PieEntry((float)fullTime, "fullTime", null));

        //二维数据的一级数据
        PieDataSet dataSet = new PieDataSet(entries, "Election Results");
        //数据配置，是否绘制图标
        dataSet.setDrawIcons(false);
        //扇页之间的空白间距
        dataSet.setSliceSpace(3f);
        //图标偏移
        dataSet.setIconsOffset(new MPPointF(0, 40));
        dataSet.setSelectionShift(5f);

        //添加颜色集合，
        ArrayList<Integer> colors = new ArrayList<>();
        //colors.add(ColorTemplate.LIBERTY_COLORS[0]);
        colors.add(Color.parseColor("#3790A2"));
        colors.add(Color.parseColor("#37F0A2"));
        dataSet.setColors(colors);
        //dataSet.setSelectionShift(0f);

        //设置图表数据
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        //data.setValueTypeface(tfLight);
        pieChart.setData(data);

        //撤消所有高光
//        pieChart.highlightValues(null);

        //刷新图表UI
        pieChart.invalidate();
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
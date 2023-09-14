package com.example.trashcanapplication;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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

/**
 * @Title：TrashCanDetailActivity.java
 * @Description: An activity that displays detailed information of a trash can
 * @author P Geng
 */
public class TrashCanDetailActivity extends BaseActivity {

    private int TrashCanId;
    private int Depth;
    private String Mode;

    JSONObject jsonData;

    //SharedPreferences
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    //MQTT
    private MyMqttClient myMQTTClient = null;
    private static String IP;
    private String ClientId;

    //Dropdown selector for line charts
    private Spinner spinnerLinechart;
    //line chart
    private LineChart lineChart;
    //pie chart
    private PieChart pieChart;
    //Progress Dialog
    private ProgressDialog progressDialog;

    //Dropdown selector for trash can mode
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

        //        toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //   Set the leftmost button display status and icon of the actionbar ( toolbar)
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

        //trash can mode
        spinnerTrashcanMode = findViewById(R.id.spinner_trashcan_mode);
        ArrayAdapter<CharSequence> spinnerTrashcanModeAdapter = ArrayAdapter.createFromResource(this,
                R.array.trashcan_mode_array, android.R.layout.simple_spinner_item);
        spinnerTrashcanModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinnerTrashcanMode.setAdapter(spinnerTrashcanModeAdapter);
        //Initially select the mode option corresponding to this trash can Mode
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

        // SharedPreference
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        lineChart = findViewById(R.id.line_chart);
        initLineChart();
        pieChart = findViewById(R.id.pie_chart);
        initPieChart();

        myMQTTClient = MyMqttClient.getInstance();
        //Using EventBus to communicate with threads
        EventBus.getDefault().register(this);
        //Send garbage can data request to the server
        JSONObject jsonObject = new JSONObject();

        //Obtain IP from SharedPerformance
        IP = pref.getString("ipStr","");
        ClientId = "Android/"+IP;

        try {
            jsonObject.put("dataType", "trashCanDataRequest");
            jsonObject.put("Id", ClientId );
            jsonObject.put("TrashCanId", TrashCanId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        MyMqttClient myMQTTClient = MyMqttClient.getInstance();
        myMQTTClient.publishMessage("MQTTServerSub",jsonObject.toString(),0);

        //Display progress bar
        try{
            progressDialog = ProgressDialog.show(this,"loading","Loading data from the server");
        }catch (Exception e){
            Log.d("Progress Bar Window Flashback", e.getMessage());
        }
        Timer timer = new Timer();
        //TimerTask belongs to a child thread and cannot execute toast “Can't toast on a thread that has not called Looper.prepare()”
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
    }

    /***
     *EventBus callback
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String s) {
        //{"sender":"myMqttClient","dataType":"allTrashCanData","payload":[{"Id":1,"Distance":67,"Humidity":67,"Temperature":67,"Latitude":52.445978,"Longitude":-1.935167},{"Id":2,"Distance":33,"Humidity":333,"Temperature":3333,"Latitude":52.444256,"Longitude":-1.934156}]}
        try {
            JSONObject j = new JSONObject(s);
            String sender = j.getString("sender");
            String dataType = j.getString("dataType");
            //The received JSON data is the historical data of a certain trash can
            if (sender.equals("myMqttClient") && dataType.equals("thisTrashCanData")){

                jsonData = j;
                setLineChartData(spinnerLinechart.getSelectedItem().toString());
                //Set data for the chart
                setPieChartData();
                //dismiss dialog
                progressDialog.dismiss();
            }

        } catch (Exception e) {
            Toast.makeText(TrashCanDetailActivity.this, "JSON conversion error :" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /***
     *Initialize LineChart
     */
    private void initLineChart(){
        //Click listener
        //chart.setOnChartValueSelectedListener(this);
        //Draw grid lines
        lineChart.setDrawGridBackground(false);

        //description text
        lineChart.getDescription().setEnabled(false);

        //if can be touched
        lineChart.setTouchEnabled(true);

        //Enable Zoom and Drag
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);

        // If disabled, scaling can be done on the x-axis and y-axis respectively
        lineChart.setPinchZoom(true);

        //Set Background Color
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
        //Rewrite date data to horizontal coordinate format, but this method may have duplicate horizontal coordinates
        SimpleDateFormat mFormat = new SimpleDateFormat("M.d");
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(new Date((long) value));
            }
        });
//
        //Configure the data on the left side of the y-coordinate
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setInverted(true);
        rightAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
//
        //Close the data on the left side of the y-coordinate
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setEnabled(false);

//        setData();
    }

    /***
     *Initialize PieChart
     */
    private void initPieChart() {
        //Is it used for percentage data
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);

        pieChart.setDragDecelerationFrictionCoef(0.95f);

        //Set the font of the middle text
        //chart.setCenterTextTypeface(tfLight);
        //chart.setCenterText(generateCenterSpannableText());

        //Draw the central circular area and color
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);

        //Draw the transparent area of the center edge
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);

        //Draw the center circle and the border size of the circle edges
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);

        //Draw center area text
        pieChart.setDrawCenterText(true);

        //Default rotation angle
        pieChart.setRotationAngle(0);
        //Enable chart rotation by touching
        pieChart.setRotationEnabled(true);
        //Touch to highlight settings
        pieChart.setHighlightPerTapEnabled(true);

        // chart.setUnit(" €");
        // chart.setDrawUnitsInChart(true);

        //Add Selection Listener
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                //
            }

            @Override
            public void onNothingSelected() {
                //
            }
        });

        //animation
        pieChart.animateY(1400, Easing.EaseInOutQuad);
        // chart.spin(2000, 0, 360);

        //legend
        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);

        //Label Style
        pieChart.setEntryLabelColor(Color.WHITE);
        //chart.setEntryLabelTypeface(tfRegular);
        pieChart.setEntryLabelTextSize(12f);
    }

    /***
     *Set data for line charts
     */
    private void setLineChartData(String SpinnerSelection) {
        try {
            String payload = jsonData.getString("payload");
            JSONArray jsonArray = new JSONArray(payload);

//            System.out.println("JSON数据打印："+jsonData.toString());

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
            //Sort by x-coordinate value
            Collections.sort(entries, new EntryXComparator());
            LineDataSet set1 = new LineDataSet(entries, "DataSet 1");
            //Creating Data Objects Using Datasets
            LineData data = new LineData(set1);
            lineChart.setData(data);
            //Refresh Drawing
            lineChart.invalidate();
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /***
     *Set data for pie charts
     */
    private void setPieChartData(){
        //Analyzing data from JSONdata
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

        ArrayList<PieEntry> entries = new ArrayList<>();

        entries.add(new PieEntry((float)unfullTime, "unfullTime", null));
        entries.add(new PieEntry((float)fullTime, "fullTime", null));

        PieDataSet dataSet = new PieDataSet(entries, "Election Results");

        dataSet.setDrawIcons(false);

        dataSet.setSliceSpace(3f);

        dataSet.setIconsOffset(new MPPointF(0, 40));
        dataSet.setSelectionShift(5f);

        //Add Color Collection，
        ArrayList<Integer> colors = new ArrayList<>();
        //colors.add(ColorTemplate.LIBERTY_COLORS[0]);
        colors.add(Color.parseColor("#3790A2"));
        colors.add(Color.parseColor("#37F0A2"));
        dataSet.setColors(colors);
        //dataSet.setSelectionShift(0f);

        //Set Chart Data
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        //data.setValueTypeface(tfLight);
        pieChart.setData(data);

        //Remove All Highlights
//        pieChart.highlightValues(null);

        //Refresh Chart UI
        pieChart.invalidate();
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
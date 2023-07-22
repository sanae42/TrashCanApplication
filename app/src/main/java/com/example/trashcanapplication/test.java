//package com.example.trashcanapplication;
//
////MPAndroidChart新版本修复了很多BUG，同时效率相比以前版本也提高不少。
////        像x轴的改进是非常的大，原来老版本中，X轴是存在在一个List的列表中，新版本则是放到Entry类里面，然后自动计算轴的值。
////        那么问题来了，Entry的构造函数是float x,float y，那怎么显示成日期格式呢？
////        原理很简单，直接把x轴放时间戳，最好是转成秒的时间戳(Date类的getTime是精确到毫秒级，直接除1000就OK了)。
////        然后利用以下代码就可以直接转换成日期格式字符串了
//        xAxis.setValueFormatter(new IAxisValueFormatter(){
//                SimpleDateFormat mFormat=new SimpleDateFormat("M月d");
//
//@Override
//
//public String getFormattedValue(float value,AxisBase axis){
//        return mFormat.format(new Date((long)value));
//
//        }
//
//        });
//
////        还可以利用xAxis.setGranularity(3600000);//以小时为单位函数将最小刻度改成小时(本例子是毫秒级)
////
////        下面是一个代码片/**
//
//// * 历史数据
////
//// */
//
//public class HistoryCPKFragment extends SFragment implements View.OnTouchListener {
//    String dateYearMonth;
//
//    LineChart chartX;
//
//    LineChart chartR;
//
//    int[] colors = {0xff70828e, 0xffff0000, 0xffF7A35C, 0xff73cc0f,
//
//            0xff0fccab, 0xff88112d, 0xff166448, 0xffff0000, 0xff00ff00,
//
//            0xff0000ff};
//
//    List yValsX;
//
//    List yValsR;
//
//    Map data;
//
//    long beginTime;
//
//    long endTime;
//
//    @Override
//
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        setContentView(R.layout.fragment_cpk_history);
//
//        data = new LinkedHashMap<>();
//
//        dateYearMonth = Tools.formatDateTime("yyyyMMdd");
//
//        setTextValue(R.id.id_text_date, dateYearMonth);
//
//        findEditTextById(R.id.id_text_date).setOnTouchListener(this);
//
//        chartX = (LineChart) findViewById(R.id.chartX);
//
//        chartR = (LineChart) findViewById(R.id.chartR);
//
//        setupChart(chartX, colors[2], "CPK-X");// 透明背景
//
//        setupChart(chartR, colors[1], "CPK-R");// 透明背景
//
//        requestData();
//
//    }
//
//    void requestData() {
//        showProgressDialog("数据请求中...");
//
//        HttpCallback callback = new HttpCallback() {
//            @Override
//
//            public void callback(HttpResult result) {
//                disMiss();
//
//                ResultItem item = new ResultItem(result);
//
//                if (item.getError_code() != 0) {
//                    MLog.makeText(item.getMessage_cn());
//
//                    sendWhatMessage(1);
//
//                    return;
//
//                }
//
//                parseResult(item);
//
//            }
//
//        };
//
//        HttpApi.requestQualifiedcpkforpc(getCache().getCurInfo().getPlantId(), dateYearMonth, callback);
//
//    }
//
//    void parseResult(ResultItem item) {
//        try {
//            data.clear();
//
//            JSONObject detail = item.getObject().getJSONObject("detail");
//
//            JSONArray RateJson = detail.getJSONArray("RateJson");
//
//            for (int i = 0; i
//
//            JSONObject child = RateJson.getJSONObject(i);
//
//            CpkItem cpkItem = new CpkItem(child);
//
//            long t = cpkItem.longTime();
//
//            if (beginTime > t || beginTime == 0)
//
//                beginTime = t;
//
//            if (endTime
//
//            endTime = t;
//
//            data.put(cpkItem.getTime(), cpkItem);
//
//        }
//
//        update();//填充数据到曲线控件
//
//        sendWhatMessage(0);
//
//    } catch(
//    JSONException e)
//
//    {
//        e.printStackTrace();
//
//    }
//
//}
//
//    private void update() {
//        List xEntries = new ArrayList();
//
//        List rEntries = new ArrayList();
//
//        for (long i = beginTime; i <= endTime; i += 86400000) {
//            String key = Tools.formatDateTime(i, "yyyy-M-d");
//
//            CpkItem item = data.get(key);
//
//            if (item != null) {
//                String[] xv = item.getXVale();
//
//                for (int j = 0; j
//
//                float v = Float.parseFloat(xv[j]);
//
//                Entry entry = new Entry(i + (j + 1) * 25200000, v);
//
//                xEntries.add(entry);
//
//            }
//
//            String[] rv = item.getRVale();
//
//            for (int j = 0; j
//
//            float v = Float.parseFloat(rv[j]);
//
//            Entry entry = new Entry(i + (j + 1) * 25200000, v);
//
//            rEntries.add(entry);
//
//        }
//
//    } else{
//        for(int j=0;j
//
//        Entry entry=new Entry(i+(j+1)*25200000,0,false);
//
//        xEntries.add(entry);
//
//        rEntries.add(entry);
//
//        }
//
//        }
//
//        }
//
//        setData(chartX,xEntries,colors[2]);
//
//        setData(chartR,rEntries,colors[1]);
//
//        }
//
//private void setData(LineChart chart,List entries,int color){
//        List dataSets=new ArrayList();
//
//        String label="";
//
//        float max=0;
//
//        float min=0;
//
//        for(Entry entry:entries){
//        if(entry.getY()>max)
//
//        max=entry.getY();
//
//        if(entry.getY()
//
//        min=entry.getY();
//
//        }
//
//        LineDataSet set=new LineDataSet(entries,label);// 曲线数据和标签
//
//        set.setDrawFilled(android.os.Build.VERSION.SDK_INT>=18);// 填充
//
//        set.setFillAlpha(30);// 填充透明度
//
//        set.setLineWidth(1f);// 曲线粗细
//
//        set.setCircleRadius(3f);// 圆半径
//
//        set.setColor(color);
//
//        set.setCircleColor(color);
//
//        set.setHighLightColor(color);
//
//        set.setValueTextColor(0xfff3f3f3);
//
//        dataSets.add(set); // 一条曲线
//
//        YAxis leftAxis=chart.getAxisLeft();
//
//        leftAxis.setAxisMaximum(max*2);
//
//        leftAxis.setAxisMinimum(0);
//
//        LineData data=new LineData(dataSets);
//
//        chart.setData(data);
//
//        }
//
//        boolean isMove=false;
//
//@Override
//
//public boolean handleMessage(Message msg){
//        if(msg.what==0){
//        chartX.animateY(1000);
//
//        chartR.animateY(1000);
//
//        if(!isMove){
//        isMove=true;
//
//        chartX.zoom(10f,0.0f,0,0);
//
//        chartR.zoom(10f,0.0f,0,0);
//
//        }
//
//        }else if(msg.what==1){
//        chartX.clear();
//
//        chartX.animateY(1000);
//
//        chartR.clear();
//
//        chartR.animateY(1000);
//
//        }
//
//        return super.handleMessage(msg);
//
//        }
//
//private void setupChart(LineChart chart,int color,String label){
//        int bkColor=0xfff3f3f3;
//
//        chart.setNoDataText("没有数据");
//
//        Description des=new Description();
//
//        des.setText("");
//
//        chart.setDescription(des);
//
//        chart.setDrawGridBackground(false);// 不需要绘制块背景
//
//        Legend l=chart.getLegend();
//
//        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
//
//        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
//
//        LegendEntry entry=new LegendEntry();
//
//        entry.label=label;
//
//        entry.formColor=color;
//
//        l.setCustom(new LegendEntry[]{entry});
//
//        l.setForm(Legend.LegendForm.CIRCLE);
//
//        l.setFormSize(6f);
//
//        l.setTextColor(bkColor);
//
//        XAxis xAxis=chart.getXAxis();
//
//        xAxis.setTextSize(12f);
//
//        xAxis.setTextColor(bkColor);
//
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//
//        xAxis.setGranularity(3600000);//以小时为单位
//
//        xAxis.setValueFormatter(new IAxisValueFormatter(){
//        SimpleDateFormat mFormat=new SimpleDateFormat("M月d");
//
//@Override
//
//public String getFormattedValue(float value,AxisBase axis){
//        return mFormat.format(new Date((long)value));
//
//        }
//
//        });
//
//        YAxis leftAxis=chart.getAxisLeft();
//
//        leftAxis.setTextColor(bkColor);
//
//        YAxis rightAxis=chart.getAxisRight();
//
//        rightAxis.setEnabled(false);
//
//        }
//
//@Override
//
//public boolean onTouch(View v,MotionEvent event){
//        if(event.getAction()==MotionEvent.ACTION_UP){
//final View tv=v;
//
//final Calendar c=Calendar.getInstance();
//
//        c.setTimeInMillis(System.currentTimeMillis());
//
//        int year=c.get(Calendar.YEAR);
//
//        int monthOfYear=c.get(Calendar.MONTH);
//
//        int dayOfMonth=c.get(Calendar.DAY_OF_MONTH);
//
//        DatePickerDialog.OnDateSetListener listener=new DatePickerDialog.OnDateSetListener(){
//@Override
//
//public void onDateSet(DatePicker view,int year,
//
//        int monthOfYear,int dayOfMonth){
//// TODO Auto-generated method stub
//
//        dateYearMonth=String.format("%d%02d%02d",year,monthOfYear+1,dayOfMonth);
//
//        setTextValue(R.id.id_text_date,dateYearMonth);
//
//        requestData();
//
//        }
//
//        };
//
//        DatePickerDialog dateDialog=new DatePickerDialog(getActivity(),listener,
//
//        year,monthOfYear,dayOfMonth){
//@Override
//
//protected void onCreate(Bundle savedInstanceState){
//        super.onCreate(savedInstanceState);
//
//        }
//
//        };
//
//        dateDialog.show();
//
//        }
//
//        return false;
//
//        }
//
//        }

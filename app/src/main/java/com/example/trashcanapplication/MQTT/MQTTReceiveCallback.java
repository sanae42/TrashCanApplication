package com.example.trashcanapplication.MQTT;

import static android.content.ContentValues.TAG;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

// 继承自MqttCallbackExtended而非MqttCallback，可以重写connectComplete方法。MqttCallbackExtended继承自connectComplete
public class MQTTReceiveCallback implements MqttCallbackExtended {
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        Log.e("MqttCallbackBus", "MQTT_connectComplete:");
        //断开连接必须重新订阅才能收到之前订阅的session连接的消息
        if(reconnect){
            Log.e("MqttCallbackBus_重连订阅主题", "MQTT_connectComplete:");
            //这里是发送消息去重新订阅
//            String msg = "reconnect";
//            EventBus.getDefault().postSticky(msg);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        // 连接丢失后，一般在这里面进行重连
        System.out.println("连接断开，可以重连");
        Log.e("MqttCallbackBus>>>", "MQTT_connectionLost 掉线原因:"+cause.getMessage());
        cause.printStackTrace();
        //TODO:测试自动重连 此处的自动重连可以正常工作，和前面的mqttConnectOptions.setAutomaticReconnect(true);功能重复，但是暂未发现冲突
        MyMqttClient myMQTTClient = MyMqttClient.getInstance();
        myMQTTClient.reConnect();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
//        System.out.println("deliveryComplete---------" + token.isComplete());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // subscribe后得到的消息会执行到这里面
        System.out.print("接收消息主题 : " + topic);
        System.out.print("   接收消息Qos : " + message.getQos());
        System.out.println("   接收消息内容 : " + new String(message.getPayload()));
        //TODO:解决子线程操作布局的问题
        try {
//            Looper.prepare();
//            Toast.makeText(context, "messageArrived: "+new String(message.getPayload()), Toast.LENGTH_SHORT).show();
//            Looper.loop();
            String s =  new String(message.getPayload());
            EventBus.getDefault().post(s);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Context错误:"+e.getMessage()+" ");
        }

        Log.d(TAG, "接收数据: "+new String(message.getPayload()));
    }
}



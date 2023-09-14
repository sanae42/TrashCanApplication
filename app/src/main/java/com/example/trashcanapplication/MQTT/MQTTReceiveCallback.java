package com.example.trashcanapplication.MQTT;

import static android.content.ContentValues.TAG;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

/**
 * @Title：MQTTReceiveCallback.java
 * @Description: When an event related to mqttClient occurs, the program will call the corresponding method in MQTTReceiveCallback.
 * @author P Geng
 */
public class MQTTReceiveCallback implements MqttCallbackExtended {
    /**
     * Called when successfully connecting to the broker
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        Log.e("MqttCallbackBus", "MQTT_connectComplete:");
        if(reconnect){
        }
    }

    /**
     *  Called when disconnected, then get the MyMqttClient instance and call reConnect()
     */
    @Override
    public void connectionLost(Throwable cause) {
        // 连接丢失后，进行重连
        System.out.println("连接断开，可以重连");
        Log.e("MqttCallbackBus>>>", "MQTT_connectionLost 掉线原因:"+cause.getMessage());
        cause.printStackTrace();
        //TODO:测试自动重连 此处的自动重连可以正常工作，和前面的mqttConnectOptions.setAutomaticReconnect(true);功能重复，但是暂未发现冲突
        MyMqttClient myMQTTClient = MyMqttClient.getInstance();
        myMQTTClient.reConnect();
    }

    /**
     * Called when the message is successfully delivered
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
//        System.out.println("deliveryComplete---------" + token.isComplete());
    }

    /**
     * Called when a new message is received
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.print("Received a message:   ");
        System.out.print("topic : " + topic);
        System.out.print("   qos : " + message.getQos());
        System.out.println("   content : " + new String(message.getPayload()));

        try {
//            Looper.prepare();
//            Toast.makeText(context, "messageArrived: "+new String(message.getPayload()), Toast.LENGTH_SHORT).show();
//            Looper.loop();
            String s =  new String(message.getPayload());
            EventBus.getDefault().post(s);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Context error:"+e.getMessage()+" ");
        }

    }
}



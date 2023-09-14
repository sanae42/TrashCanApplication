package com.example.trashcanapplication.MQTT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @Title：MyMqttClient.java
 * @Description: A Java class that manages MQTT connections, subscription topics, and message sending and receiving
 * @author P Geng
 * Refer to: https://www.emqx.io/docs/en/v5.1/connect-emqx/java.html#paho-java-usage-example
 *           https://blog.csdn.net/a123123sdf/article/details/120948170
 */
public class MyMqttClient {
    private static MqttClient mqttClient = null;
    private static MemoryPersistence memoryPersistence = null;
    private static MqttConnectOptions mqttConnectOptions = null;
    private String ClientId;
    private static String IP = "47.98.247.122";
    private static List<String> TopicList = new ArrayList<String>();

    private static volatile MyMqttClient mInstance = null;

    private MqttCallback mqttCallback;

    private MyMqttClient(){
        mqttCallback = new MQTTReceiveCallback();
    }

    /**
     * Obtain a singleton of a class
     */
    public static MyMqttClient getInstance() {
        if (mInstance == null) {
            //https://blog.csdn.net/cgsyck/article/details/106251614
            synchronized (MyMqttClient.class) {//同步锁
                if (mInstance == null) {
                    mInstance = new MyMqttClient();
                }
            }
        }
        return mInstance;
    }

    /**
     * The method used for initialization is called in main() of the program
     */
    public void start(String id) {
        ClientId = id;

        //Initialize mqttConnectOptions
        mqttConnectOptions = new MqttConnectOptions();
        //Set whether to clear the session. If set to false here, it means that the server will keep the connection records of the client.
        // If set to true here, it means that every time it connects to the server, it will connect with a new identity
        mqttConnectOptions.setCleanSession(true);
        ////Set the connection timeout in seconds
        mqttConnectOptions.setConnectionTimeout(10);
        //Automatic reconnection(Not used here)
        //        mqttConnectOptions.setAutomaticReconnect(true);
        // Set the timeout time in seconds
        mqttConnectOptions.setConnectionTimeout(10);
        // Set the session heartbeat time unit to seconds. The server will send a message to the client every 1.5 * 20 seconds
        // to determine whether the client is online, but this method does not have a reconnection mechanism
        mqttConnectOptions.setKeepAliveInterval(20);
        //Set persistence method
        memoryPersistence = new MemoryPersistence();
        // Set client name
        mqttConnectOptions.setUserName(ClientId);

        try {
            mqttClient = new MqttClient("tcp://"+IP+":1883", ClientId, memoryPersistence);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        if(null != mqttClient) {
            if(!mqttClient.isConnected()) {
                //Add callback method
                mqttClient.setCallback(mqttCallback);
                //Create a connection
                try {
                    mqttClient.connect(MyMqttClient.mqttConnectOptions);
                } catch (MqttException e) {
                    // Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }else {
            System.out.println("mqttClient is null");
        }
        System.out.println("Connection status: "+mqttClient.isConnected());
    }

    /**
     * Close the connection
     */
    public void closeConnect() {
        //close memoryPersistence
        if(null != memoryPersistence) {
            try {
                memoryPersistence.close();
            } catch (MqttPersistenceException e) {
                e.printStackTrace();
            }
        }else {
            System.out.println("memoryPersistence is null");
        }

        //close connection
        if(null != mqttClient) {
            if(mqttClient.isConnected()) {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }else {
                System.out.println("mqttClient is not connect");
            }
        }else {
            System.out.println("mqttClient is null");
        }
    }

    /**
     * Publish Message
     */
    public void publishMessage(String pubTopic, String message, int qos) {
        if(null != mqttClient&& mqttClient.isConnected()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(qos);
            mqttMessage.setPayload(message.getBytes());

            MqttTopic topic = mqttClient.getTopic(pubTopic);

            if(null != topic) {
                try {
                    MqttDeliveryToken publish = topic.publish(mqttMessage);
                    if(!publish.isComplete()) {
                        System.out.print("Sent a message:");
                        System.out.print("   topic:"+pubTopic);
                        System.out.print("   qos:"+qos);
                        System.out.println("   content:"+message);
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }else {
            reConnect();
        }
    }

    /**
     * Reconnect
     */
    public void reConnect() {
        if(null != mqttClient) {
            if(!mqttClient.isConnected()) {
                if(null != mqttConnectOptions) {
                    //Need to rebind callback and resubscribe after reconnection
                    mqttClient.setCallback(mqttCallback);
                    try {
                        mqttClient.connect(mqttConnectOptions);
                    } catch (MqttException e) {
                        throw new RuntimeException(e);
                    }
                    for(String topic : TopicList){
                        try {
                            //default qos1
                            mqttClient.subscribe(topic,1);
                        } catch (MqttException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }else {
                    System.out.println("mqttConnectOptions is null");
                }
            }else {
                System.out.println("mqttClient is null or connect");
            }
        }else {
            start(ClientId);
        }
    }

    /**
     * Subscribe topic
     */
    public void subTopic(String t) {
        if(null != mqttClient&& mqttClient.isConnected()) {
            try {
                //Add the topic to the topic list
                TopicList.add(t);
                //default qos 1
                mqttClient.subscribe(t, 1);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }else {
            System.out.println("mqttClient is error");
        }
    }

    /**
     * Release singleton and its referenced resources
     */
    public static void release() {
        try {
            if (mInstance != null) {
                mInstance.closeConnect();
                mInstance = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

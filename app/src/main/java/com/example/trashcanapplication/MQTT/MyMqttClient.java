package com.example.trashcanapplication.MQTT;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MyMqttClient {
    public static MqttClient mqttClient = null;
    private static MemoryPersistence memoryPersistence = null;
    private static MqttConnectOptions mqttConnectOptions = null;
    private static String ClientName = "Android";
    private static String IP = "47.98.247.122";
//    private String[] topic = null;

    //volatile 修饰的成员变量在每次被线程访问时，都强制从共享内存中重新读取该成员变量的值。
    // 而且，当成员变量发生变化时，会强制线程将变化值回写到共享内存。这样在任何时刻，两个不同的线程总是看到某个成员变量的同一个值。
    private static volatile MyMqttClient mInstance = null;

    //环境
//     Context context = null;
    private MqttCallback mCallback;

    private MyMqttClient(){
        mCallback = new MQTTReceiveCallback();
    }

    //获得对象的静态方法
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

    /**初始化连接*/
    public void start(String clientId) {
        //初始化连接设置对象
        mqttConnectOptions = new MqttConnectOptions();
        //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，
        //这里设置为true表示每次连接到服务器都以新的身份连接
        mqttConnectOptions.setCleanSession(true);
        //设置连接超时时间，单位是秒
        mqttConnectOptions.setConnectionTimeout(10);
        //自动重连
        mqttConnectOptions.setAutomaticReconnect(true);
        // 设置超时时间 单位为秒
        mqttConnectOptions.setConnectionTimeout(10);
        // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
        mqttConnectOptions.setKeepAliveInterval(20);
        //设置持久化方式
        memoryPersistence = new MemoryPersistence();

        mqttConnectOptions.setUserName("Android客户端");
//        mqttConnectOptions.setPassword("public".toCharArray());

        if(null != clientId) {
            try {
                mqttClient = new MqttClient("tcp://"+IP+":1883", clientId,memoryPersistence);
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
//        System.out.println("连接状态："+mqttClient.isConnected());
        //设置连接和回调
        if(null != mqttClient) {
            if(!mqttClient.isConnected()) {
                //创建回调函数对象
//                MQTTReceiveCallback mqttReceiveCallback = new MQTTReceiveCallback(context);
                //客户端添加回调函数
                mqttClient.setCallback(mCallback);
                //创建连接
                try {
                    System.out.println("创建连接");
                    mqttClient.connect(MyMqttClient.mqttConnectOptions);
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }else {
            System.out.println("mqttClient为空");
        }
        System.out.println("连接状态"+mqttClient.isConnected());
    }

    /**关闭连接*/
    public void closeConnect() {
        //关闭存储方式
        if(null != memoryPersistence) {
            try {
                memoryPersistence.close();
            } catch (MqttPersistenceException e) {
                e.printStackTrace();
            }
        }else {
            System.out.println("memoryPersistence is null");
        }

        //关闭连接
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

    /**发布消息*/
    public void publishMessage(String pubTopic, String message, int qos) {
        if(null != mqttClient&& mqttClient.isConnected()) {
//            System.out.println("发布消息   "+mqttClient.isConnected());
//            System.out.println("id:"+mqttClient.getClientId());
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(qos);
            mqttMessage.setPayload(message.getBytes());

            MqttTopic topic = mqttClient.getTopic(pubTopic);

            if(null != topic) {
                try {
                    MqttDeliveryToken publish = topic.publish(mqttMessage);
                    if(!publish.isComplete()) {
                        System.out.print("消息发布成功:");
                        System.out.print("   主题:"+pubTopic);
                        System.out.print("   qos:"+qos);
                        System.out.println("   内容:"+message);
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }else {
            reConnect();
        }
    }

    /**重新连接*/
    public void reConnect() {
        if(null != mqttClient) {
            if(!mqttClient.isConnected()) {
                if(null != mqttConnectOptions) {
                    //TODO:重连后不能收到消息，可能需要在重连后重新绑定callback或重新订阅？
                    mqttClient.setCallback(mCallback);
                    try {
                        mqttClient.connect(mqttConnectOptions);
                    } catch (MqttException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        mqttClient.subscribe("TrashCanPub",1);
                    } catch (MqttException e) {
                        throw new RuntimeException(e);
                    }
//                    start(ClientName);
//                    subTopic("TrashCanPub");
                    try {
                        mqttClient.connect(mqttConnectOptions);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }else {
                    System.out.println("mqttConnectOptions is null");
                }
            }else {
                System.out.println("mqttClient is null or connect");
            }
        }else {
            start(ClientName);
        }
    }
    /**订阅主题*/
    public void subTopic(String t) {
        if(null != mqttClient&& mqttClient.isConnected()) {
            try {
                //默认qos为1
                mqttClient.subscribe(t, 1);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }else {
            System.out.println("mqttClient is error");
        }
    }

    /**清空主题*/
    public void cleanTopic(String topic) {
        if(null != mqttClient&& !mqttClient.isConnected()) {
            try {
                mqttClient.unsubscribe(topic);
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else {
            System.out.println("mqttClient is error");
        }
    }

    /**
     * 释放单例, 及其所引用的资源
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

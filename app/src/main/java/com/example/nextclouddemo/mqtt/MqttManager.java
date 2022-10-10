package com.example.nextclouddemo.mqtt;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

/**
 * @author Ai（陈祥林）
 * @date 2018/1/3  10:37
 * @email Webb@starcc.cc
 */
public class MqttManager {

    private static MqttManager mInstance = null;
    /**
     * Mqtt回调
     */
    private MqttCallback mCallback;
    /**
     * Mqtt客户端
     */
    private static MqttClient client;
    /**
     * Mqtt连接选项
     */
    private MqttConnectOptions conOpt;

    private MqttManager() {
        mCallback = new MqttCallbackBus();
    }

    public static MqttManager getInstance() {
        if (null == mInstance) {
            synchronized (MqttManager.class) {
                if (mInstance == null) {
                    mInstance = new MqttManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 释放单例, 及其所引用的资源
     */
    public static void release() {
        try {
            if (mInstance != null) {
                disConnect();
                mInstance = null;
            }
        } catch (Exception e) {
            Log.e("MqttManager", "release : " + e.toString());
        }
    }

    /**
     * 创建Mqtt 连接
     *
     * @param brokerUrl Mqtt服务器地址(tcp://xxxx:1863)
     * @param userName  用户名
     * @param password  密码
     * @param clientId  客户端Id
     */
    public void creatConnect(String brokerUrl, String userName, String password, String clientId, String topic) {
        // 获取默认的临时文件路径
        String tmpDir = System.getProperty("java.io.tmpdir");

        /*
         * MqttDefaultFilePersistence：
         * 将数据包保存到持久化文件中，
         * 在数据发送过程中无论程序是否奔溃、 网络好坏
         * 只要发送的数据包客户端没有收到，
         * 这个数据包会一直保存在文件中，
         * 直到发送成功为止。
         */
        // Mqtt的默认文件持久化
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
        try {
            // 构建包含连接参数的连接选择对象
            conOpt = new MqttConnectOptions();
            // 设置Mqtt版本
            conOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            // 设置清空Session，false表示服务器会保留客户端的连接记录，true表示每次以新的身份连接到服务器
            conOpt.setCleanSession(false);
            // 设置会话心跳时间，单位为秒
            // 客户端每隔10秒向服务端发送心跳包判断客户端是否在线
            conOpt.setKeepAliveInterval(90);

            conOpt.setMaxInflight(1000);
            // 设置账号
            if (userName != null) {
                conOpt.setUserName(userName);
            }
            // 设置密码
            if (password != null) {
                conOpt.setPassword(password.toCharArray());
            }
            // 最后的遗言(连接断开时， 发动"close"给订阅了topic该主题的用户告知连接已中断)
            conOpt.setWill(topic, "close".getBytes(), 2, false);
            // 客户端是否自动尝试重新连接到服务器
            conOpt.setAutomaticReconnect(true);
            // 创建MQTT客户端
            client = new MqttClient(brokerUrl, clientId, dataStore);
            // 设置回调
            client.setCallback(mCallback);
            // 连接
            doConnect();

        } catch (MqttException e) {
            Log.e("MqttManager", "creatConnect : " + e.toString());

        }

    }


    /**
     * 建立连接
     */
    public void doConnect() {
        if (client != null) {
            try {
                client.connect(conOpt);
            } catch (Exception e) {
                Log.e("MqttManager", "doConnect : " + e.toString());
            }
        }
    }


    /**
     * 发布消息
     *
     * @param topicName 主题名称
     * @param qos       质量(0,1,2)
     * @param payload   发送的内容
     */
    public void publish(String topicName, int qos, byte[] payload) {
        if (client != null && client.isConnected()) {
            // 创建和配置一个消息
            MqttMessage message = new MqttMessage(payload);
            message.setPayload(payload);
            message.setQos(qos);
            try {
                client.publish(topicName, message);
            } catch (MqttException e) {
                Log.e("MqttManager", "publish : " + e.toString());
            }
        }
    }


    public void publish(String topicName, int qos, String payload) {
        if (client != null && client.isConnected()) {
            // 创建和配置一个消息
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setPayload(payload.getBytes());
            message.setQos(qos);
            try {
                client.publish(topicName, message);
            } catch (MqttException e) {
                Log.e("MqttManager", "publish : " + e.toString());
            }
        }
    }


    /**
     * 订阅主题
     *
     * @param topicName 主题名称
     * @param qos      消息质量
     */
    public void subscribe(String topicName, int qos) {
        if (client != null && client.isConnected()) {
            try {
                client.subscribe(topicName, qos);
            } catch (MqttException e) {
                Log.e("MqttManager", "subscribe : " + e.toString());
            }
        }
    }


    /**
     * 取消连接
     */
    public static void disConnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }


    /**
     * 判断是否连接
     */
    public static boolean isConnected() {
        return client != null && client.isConnected();
    }
}
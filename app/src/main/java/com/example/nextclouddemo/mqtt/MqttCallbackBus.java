package com.example.nextclouddemo.mqtt;



import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

/**
 * @author Ai（陈祥林）
 * @date 2018/1/3  10:45
 * @email Webb@starcc.cc
 */
public class MqttCallbackBus implements MqttCallback {

    /**
     * 连接中断
     */
    @Override
    public void connectionLost(Throwable cause) {
        Log.e("MqttManager", "connectionLost cause : " + cause.toString());
        // 可在此方法内写重连的逻辑

        int number=99;
        for (int i = 0; i < number; i++) {
            try {
                Thread.sleep(1000);
                MqttManager.getInstance().doConnect();
            } catch (Exception e) {
                e.printStackTrace();
                //Thread.sleep(5000);
                System.err.println("连接失败,正在第"+i+"次尝试");
                continue;
            }
            return;
        }

    }


    /**
     * 消息送达
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.e("MqttManager", "messageArrived topic : " + topic + "\t MqttMessage : " + message.toString());
        EventBus.getDefault().post(message.toString());
    }


    /**
     * 交互完成
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.e("MqttManager", "deliveryComplete token : " + token.toString());
    }
}
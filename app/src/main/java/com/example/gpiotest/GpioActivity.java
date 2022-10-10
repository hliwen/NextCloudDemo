package com.example.gpiotest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;

import com.example.nextclouddemo.R;

import java.util.ArrayList;

public class GpioActivity extends Activity {

    private static final String TAG = "MqttManager";


    Button mRedLedTimerButton;
    Button mRedLedHeartbeatButton;
    Button mRedLedOnButton;
    Button mRedLedOffButton;
    Button mGreenLedTimerButton;
    Button mGreenLedHeartbeatButton;
    Button mGreenLedOnButton;
    Button mGreenLedOffButton;
    Button mPB2set1Button;
    Button mPB2set0Button;
    Button mPB3set1Button;
    Button mPB3set0Button;
    Button mPG12set1Button;
    Button mPG12set0Button;
    Button mPG13set1Button;
    Button mPG13set0Button;

    char mGpioCharB = 'b';
    char mGpioCharG = 'g';


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mRedLedTimerButton = (Button) findViewById(R.id.bt_red_led_timer);
        mRedLedHeartbeatButton = (Button) findViewById(R.id.bt_red_led_heartbeat);
        mRedLedOnButton = (Button) findViewById(R.id.bt_red_led_on);
        mRedLedOffButton = (Button) findViewById(R.id.bt_red_led_off);
        mGreenLedTimerButton = (Button) findViewById(R.id.bt_green_led_timer);
        mGreenLedHeartbeatButton = (Button) findViewById(R.id.bt_green_led_heartbeat);
        mGreenLedOnButton = (Button) findViewById(R.id.bt_green_led_on);
        mGreenLedOffButton = (Button) findViewById(R.id.bt_green_led_off);
        mPB2set1Button = (Button) findViewById(R.id.bt_pb2_set_1);
        mPB2set0Button = (Button) findViewById(R.id.bt_pb2_set_0);
        mPB3set1Button = (Button) findViewById(R.id.bt_pb3_set_1);
        mPB3set0Button = (Button) findViewById(R.id.bt_pb3_set_0);
        mPG12set1Button = (Button) findViewById(R.id.bt_pg12_set_1);
        mPG12set0Button = (Button) findViewById(R.id.bt_pg12_set_0);
        mPG13set1Button = (Button) findViewById(R.id.bt_pg13_set_1);
        mPG13set0Button = (Button) findViewById(R.id.bt_pg13_set_0);

        mRedLedTimerButton.setOnClickListener(mButtonListener);
        mRedLedHeartbeatButton.setOnClickListener(mButtonListener);
        mRedLedOnButton.setOnClickListener(mButtonListener);
        mRedLedOffButton.setOnClickListener(mButtonListener);
        mGreenLedTimerButton.setOnClickListener(mButtonListener);
        mGreenLedHeartbeatButton.setOnClickListener(mButtonListener);
        mGreenLedOnButton.setOnClickListener(mButtonListener);
        mGreenLedOffButton.setOnClickListener(mButtonListener);
        mPB2set1Button.setOnClickListener(mButtonListener);
        mPB2set0Button.setOnClickListener(mButtonListener);
        mPB3set1Button.setOnClickListener(mButtonListener);
        mPB3set0Button.setOnClickListener(mButtonListener);
        mPG12set1Button.setOnClickListener(mButtonListener);
        mPG12set0Button.setOnClickListener(mButtonListener);
        mPG13set1Button.setOnClickListener(mButtonListener);
        mPG13set0Button.setOnClickListener(mButtonListener);

        Log.d(TAG, "readGpio value :" + LedControl.readGpio('C', 3));

        String[] value = haveNoPermissions(GpioActivity.this);
        if (value.length > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(value, 111);
        }
    }


    public static final String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
    };

    @SuppressLint("WrongConstant")
    public static String[] haveNoPermissions(Context mActivity) {
        ArrayList<String> haveNo = new ArrayList<>();

        int id = 0;
        for (String permission : PERMISSIONS) {
            if (PermissionChecker.checkPermission(mActivity, permission, Binder.getCallingPid(), Binder.getCallingUid(), mActivity.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                haveNo.add(permission);
                Log.d(TAG, "haveNoPermissions: " + Build.BRAND);
                Log.i(TAG, "haveNoPermissions : " + permission);
            }
        }

        return haveNo.toArray(new String[haveNo.size()]);
    }

    private MyClickListener mButtonListener = new MyClickListener();

    class MyClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            int viewID = v.getId();
            if (viewID == R.id.bt_red_led_timer) {
                LedControl.nativeEnableLed(LedControl.LED_RED_TRIGGER_PATH, LedControl.LED_TIMER);
            } else if (viewID == R.id.bt_red_led_heartbeat) {
                LedControl.nativeEnableLed(LedControl.LED_RED_TRIGGER_PATH, LedControl.LED_HEARTBEAT);
            } else if (viewID == R.id.bt_red_led_on) {
                LedControl.nativeEnableLed(LedControl.LED_RED_TRIGGER_PATH, LedControl.LED_NONE);
                LedControl.nativeEnableLed(LedControl.LED_RED_BRIGHTNESS_PATH, LedControl.LED_ON);
            } else if (viewID == R.id.bt_red_led_off) {
                LedControl.nativeEnableLed(LedControl.LED_RED_TRIGGER_PATH, LedControl.LED_NONE);
                LedControl.nativeEnableLed(LedControl.LED_RED_BRIGHTNESS_PATH, LedControl.LED_OFF);
            } else if (viewID == R.id.bt_green_led_timer) {
                LedControl.nativeEnableLed(LedControl.LED_GREEN_TRIGGER_PATH, LedControl.LED_TIMER);
            } else if (viewID == R.id.bt_green_led_heartbeat) {
                LedControl.nativeEnableLed(LedControl.LED_GREEN_TRIGGER_PATH, LedControl.LED_HEARTBEAT);
            } else if (viewID == R.id.bt_green_led_on) {
                LedControl.nativeEnableLed(LedControl.LED_GREEN_TRIGGER_PATH, LedControl.LED_NONE);
                LedControl.nativeEnableLed(LedControl.LED_GREEN_BRIGHTNESS_PATH, LedControl.LED_ON);
            } else if (viewID == R.id.bt_green_led_off) {
                LedControl.nativeEnableLed(LedControl.LED_GREEN_TRIGGER_PATH, LedControl.LED_NONE);
                LedControl.nativeEnableLed(LedControl.LED_GREEN_BRIGHTNESS_PATH, LedControl.LED_OFF);
            } else if (viewID == R.id.bt_pb2_set_1) {
                LedControl.writeGpio(mGpioCharB, 2, 1);
            } else if (viewID == R.id.bt_pb2_set_0) {
                LedControl.writeGpio(mGpioCharB, 2, 0);
            } else if (viewID == R.id.bt_pb3_set_1) {
                LedControl.writeGpio(mGpioCharB, 3, 1);
            } else if (viewID == R.id.bt_pb3_set_0) {
                LedControl.writeGpio(mGpioCharB, 3, 0);
            } else if (viewID == R.id.bt_pg12_set_1) {
                LedControl.writeGpio(mGpioCharG, 12, 1);
            } else if (viewID == R.id.bt_pg12_set_0) {
                LedControl.writeGpio(mGpioCharG, 12, 0);
            } else if (viewID == R.id.bt_pg13_set_1) {
                LedControl.writeGpio(mGpioCharG, 13, 1);
            } else if (viewID == R.id.bt_pg13_set_0) {
                LedControl.writeGpio(mGpioCharG, 13, 0);
            } else {
                Log.d(TAG, "no match button");
            }
        }
    }


}
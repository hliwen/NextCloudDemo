package com.example.gpiotest;

import static android.content.ContentValues.TAG;

import android.util.Log;

/**
 * Created by moyuan on 17-9-29.
 */

public class LedControl {

    private static final String LED_BASE_PATH             ="/sys/class/leds/";
    private static final String LED_RED_PATH              =LED_BASE_PATH+"red_led/";
    private static final String LED_GREEN_PATH            =LED_BASE_PATH+"green_led/";
    public static final String LED_RED_TRIGGER_PATH       =LED_RED_PATH+"trigger";
    public static final String LED_RED_BRIGHTNESS_PATH    =LED_RED_PATH+"brightness";
    public static final String LED_GREEN_TRIGGER_PATH     =LED_GREEN_PATH+"trigger";
    public static final String LED_GREEN_BRIGHTNESS_PATH  =LED_GREEN_PATH+"brightness";
    public static final String LED_TIMER                  ="timer";
    public static final String LED_HEARTBEAT              ="heartbeat";
    public static final String LED_NONE                   ="none";
    public static final String LED_OFF                    ="1";
    public static final String LED_ON                     ="0";
    static {
        try {
            System.loadLibrary("native_led_control");
        } catch (UnsatisfiedLinkError ule) {
            Log.e(TAG, "UnsatisfiedLinkError library: " + ule);
            System.exit(1);
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException library: " + se);
            System.exit(1);
        }
    }

    public static native int nativeEnableLed(String path,String value);

    public static native int writeFile(String var0, String var1);

    public static native String readFile(String var0, int length);

    public static String readFile(String var0){
        return readFile(var0, 1);
    }

}

package com.example.gpiotest;

import static android.content.ContentValues.TAG;

import android.util.Log;

/**
 * Created by moyuan on 17-9-29.
 */

public class LedControl {

    public static int writeGpio(char group, int num, int value) {
        String dataPath = composePinPath(group, num).concat("/data");
        return LedControl.writeFile(dataPath, Integer.toString(value));
    }

    public static String readGpio(char group, int num) {
        String dataPath = composePinPath(group, num).concat("/data");
        Log.d("dataPath value ", dataPath);
        return LedControl.readFile(dataPath);
    }

    public static int setPull(char group, int num, int value) {
        String dataPath = composePinPath(group, num).concat("/pull");
        return LedControl.writeFile(dataPath, Integer.toString(value));
    }

    public static String getPull(char group, int num) {
        String dataPath = composePinPath(group, num).concat("/pull");
        return LedControl.readFile(dataPath);
    }

    public static int setDrvLevel(char group, int num, int value) {
        String dataPath = composePinPath(group, num).concat("/drv_level");
        return LedControl.writeFile(dataPath, Integer.toString(value));
    }

    public static String getDrvLevel(char group, int num) {
        String dataPath = composePinPath(group, num).concat("/drv_level");
        return LedControl.readFile(dataPath);
    }

    public static int setMulSel(char group, int num, int value) {
        String dataPath = composePinPath(group, num).concat("/mul_sel");
        return LedControl.writeFile(dataPath, Integer.toString(value));
    }

    public static String getMulSel(char group, int num) {
        String dataPath = composePinPath(group, num).concat("/mul_sel");
        return LedControl.readFile(dataPath);
    }

    private static String composePinPath(char group, int num) {
        String groupstr = String.valueOf(group).toUpperCase();
        String numstr = Integer.toString(num);
        return "/sys/class/gpio_sw/P".concat(groupstr).concat(numstr);
    }


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

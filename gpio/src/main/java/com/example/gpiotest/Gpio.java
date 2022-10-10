package com.example.gpiotest;

import android.util.Log;

public class Gpio {

    private Gpio() {
    }


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
}
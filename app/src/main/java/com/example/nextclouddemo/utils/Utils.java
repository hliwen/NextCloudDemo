package com.example.nextclouddemo.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.StatFs;

import com.example.nextclouddemo.utils.Log;

import androidx.core.content.PermissionChecker;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Utils {
    private static final String TAG = "MainActivitylog";


    public static String getTotalMemory(File path) {
        // 获得一个磁盘状态对象
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCount();    // 获得扇区的总数
        // 总空间
        String totalMemory = "" + (totalBlocks * blockSize / (1024 * 1024));
        android.util.Log.e(TAG, "adfafdd getTotalMemory: totalMemory =" + totalMemory);
        return totalMemory;
    }

    public static String getRemainMemory(File path) {
        // 获得一个磁盘状态对象
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        String availableMemory = "" + (availableBlocks * blockSize / (1024 * 1024));
        android.util.Log.e(TAG, "adfafdd getMemoryInfo:  availableMemory =" + availableMemory);
        return availableMemory;
    }


    @SuppressLint("SimpleDateFormat")
    private static DateFormat getDateFormat(String pattern) {
        String p = pattern;
        Map<String, DateFormat> formatMap = local.get();
        DateFormat df = formatMap.get(p);
        if (df == null) {
            df = new SimpleDateFormat(p);
            formatMap.put(p, df);
        }
        return df;
    }


    private static ThreadLocal<Map<String, DateFormat>> local = new ThreadLocal<Map<String, DateFormat>>() {
        @Override
        protected Map<String, DateFormat> initialValue() {
            return new HashMap<String, DateFormat>();
        }
    };

    public static String getyyyyMMString() {
        try {
            return getDateFormat("yyyyMM").format(new Date());
        } catch (Exception e) {

        }
        return System.currentTimeMillis() + "";
    }

    public static String getyyyyMMString(long time) {
        try {
            return getDateFormat("yyyyMM").format(new Date(time));
        } catch (Exception e) {

        }
        return System.currentTimeMillis() + "";
    }

    public static String getyyyyMMDDString(long time) {
        try {
            return getDateFormat("yyyyMMdd").format(new Date(time));
        } catch (Exception e) {

        }
        return System.currentTimeMillis() + "";
    }


    public static String getMMddString() {
        try {
            return getDateFormat("MMdd").format(new Date());
        } catch (Exception e) {

        }
        return System.currentTimeMillis() + "";
    }

    public static String getMMddHHmmString() {
        try {
            return getDateFormat("MMdd-HHmm").format(new Date());
        } catch (Exception e) {

        }
        return System.currentTimeMillis() + "";
    }

    public static String getyyMMddtring() {
        try {
            return getDateFormat("yyMMdd").format(new Date());
        } catch (Exception e) {

        }
        return System.currentTimeMillis() + "";
    }


    public static void closeAndroid() {

        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot -p"});  //关机
            proc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
    };

    @SuppressLint("WrongConstant")
    public static String[] haveNoPermissions(Context mActivity) {
        ArrayList<String> haveNo = new ArrayList<>();
        for (String permission : PERMISSIONS) {
            if (PermissionChecker.checkPermission(mActivity, permission, Binder.getCallingPid(), Binder.getCallingUid(), mActivity.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                haveNo.add(permission);
                Log.d(TAG, "haveNoPermissions: " + Build.BRAND);
                Log.i(TAG, "haveNoPermissions : " + permission);
            }
        }

        return haveNo.toArray(new String[haveNo.size()]);
    }


    public static void deleteAllFiles(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        if (dir.listFiles() != null)
            for (File file : dir.listFiles()) {
                if (file.isFile())
                    file.delete(); // 删除所有文件
                else if (file.isDirectory())
                    deleteAllFiles(file); // 递规的方式删除文件夹
            }
        dir.delete();// 删除目录本身
    }

}

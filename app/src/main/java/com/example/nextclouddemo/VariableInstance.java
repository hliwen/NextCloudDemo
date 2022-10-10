package com.example.nextclouddemo;

import java.io.File;
import java.util.ArrayList;

public class VariableInstance {
    private static VariableInstance instance = null;
    public int UploadMode = 2;//1 row,2 row+jpg,3 单张 ，4列表
    public ArrayList<Integer> uploadSelectIndexList;


    public int uploadNum = 0;
    public int downdNum = 0;

    public String sdCardDirRoot;

    public String PictureDirName;
    public String VideoDirName;
    public String LogcatDirName;

    public String TFCardPictureDir;
    public String TFCardVideoDir;
    public String LogcatDir;
    public boolean formarCamera;


    private VariableInstance() {
        PictureDirName = "CameraPath";
        VideoDirName = "VideoPath";
        LogcatDirName = "MLogcat";

        sdCardDirRoot = "/mnt/sdcard" + File.separator;
        TFCardPictureDir = sdCardDirRoot + PictureDirName;
        TFCardVideoDir = sdCardDirRoot + VideoDirName;
        LogcatDir = sdCardDirRoot + LogcatDirName;
    }


    public static VariableInstance getInstance() {
        if (instance == null) {
            synchronized (VariableInstance.class) {
                if (instance == null) {
                    instance = new VariableInstance();
                }
            }
        }
        return instance;
    }

}

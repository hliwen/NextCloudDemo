package com.example.nextclouddemo;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;

import com.example.nextclouddemo.model.UploadFileModel;
import com.example.nextclouddemo.utils.Log;
import com.example.nextclouddemo.utils.Utils;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;
import com.github.mjdev.libaums.partition.Partition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class USBMTPReceiver extends BroadcastReceiver {
    private static final String TAG = "MainActivitylog2";
    public static final String CHECK_PERMISSION = "CHECK_PERMISSION";
    public static final String CHECK_UPLOAD_PERMISSION = "CHECK_UPLOAD_PERMISSION";
    private ExecutorService scanerThreadExecutor;
    private ExecutorService uploadThreadExecutor;

    private UsbManager usbManager;
    private String tfcardpicturedir;

    public int deviceID = -1;
    public FileSystem uploadFs;
    public UsbFile uploadFileDirUsbFile;
    public UsbFile todayMonthUsbFile;
    private String YYMMDDString;
    private int YYMMDDInteger;


    public String todayMonthDirString;
    private DownloadFlieListener downloadFlieListener;
    private UsbDevice mUSBDevice;

    private ArrayList<PictureInfo> pictureInfos;

    public USBMTPReceiver(Context context, DownloadFlieListener downloadFlieListener) {
        this.tfcardpicturedir = VariableInstance.getInstance().TFCardPictureDir;
        this.downloadFlieListener = downloadFlieListener;
        todayMonthDirString = Utils.getyyyyMMString();
        YYMMDDString = Utils.getyyMMddtring();
        YYMMDDInteger = Integer.parseInt(YYMMDDString);
        currentDataMillis = System.currentTimeMillis();
        Log.e(TAG, "USBMTPReceiver: YYMMDDString =" + YYMMDDString + ",YYMMDDInteger =" + YYMMDDInteger);
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        pictureInfos = new ArrayList<>();


        File file = new File(tfcardpicturedir);
        //文件夹不存在就创建
        if (!file.exists()) {
            file.mkdir();
        }

        initUploadUSBDevice();
    }


    public void shutdownThread() {
        if (uploadThreadExecutor != null)
            uploadThreadExecutor.shutdown();
        if (uploadThreadExecutor != null)
            uploadThreadExecutor.shutdown();
        deviceID = 0;
    }

    public int getCapacity() {
        if (uploadFs == null)
            return 0;
        int capacity = (int) (uploadFs.getCapacity() / (1024 * 1024));
        return capacity;
    }

    public int getFreeSpace() {
        if (uploadFs == null)
            return 0;
        int freeSpace = (int) (uploadFs.getFreeSpace() / (1024 * 1024));
        return freeSpace;
    }

    public void formatCamera() {
        try {
            if (scanerThreadExecutor != null)
                scanerThreadExecutor.shutdown();
            pictureInfos.clear();
            downloadFlieListener.downloadComplete();
        } catch (Exception e) {

        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case UsbManager.ACTION_USB_DEVICE_ATTACHED: {
                mUSBDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (mUSBDevice == null) {
                    return;
                }
                try {
                    UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                    if (usbManager.hasPermission(mUSBDevice)) {
                        Log.e(TAG, "onReceive: hasPermission");
                        checkConnectedDevice(mUSBDevice);
                    } else {
                        @SuppressLint("UnspecifiedImmutableFlag")
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, new Intent(CHECK_PERMISSION), 0);
                        usbManager.requestPermission(mUSBDevice, pendingIntent);
                        Log.e(TAG, "onReceive: no hasPermission");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ACTION_USB_DEVICE_ATTACHED: Exception =" + e);
                }
            }
            break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED: {
                Log.e(TAG, "onReceive: 断开USB设备");
                mUSBDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (mUSBDevice == null)
                    return;
                try {
                    UsbMassStorageDevice usbMassStorageDevice = getUsbMass(mUSBDevice);
                    if (usbMassStorageDevice != null) {
                        usbMassStorageDevice.close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onReceive: 设备断开异常 e =" + e);
                }
                if (mUSBDevice.getDeviceId() != deviceID)
                    mUSBDevice = null;
            }
            break;
            case CHECK_PERMISSION:
                Log.e(TAG, "onReceive: CHECK_PERMISSION");
                if (mUSBDevice != null && mUSBDevice.getDeviceId() == deviceID)
                    return;
                checkConnectedDevice(mUSBDevice);
                break;
            case CHECK_UPLOAD_PERMISSION:
                Log.d(TAG, "onReceive: CHECK_UPLOAD_PERMISSION");
                initUploadUSBDevice();
                break;
            default:
                break;
        }
    }


    public void initUploadUSBDevice() {
        Log.e(TAG, "initUploadUSBDevice: ");
        if (uploadThreadExecutor == null)
            uploadThreadExecutor = Executors.newSingleThreadExecutor();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (usbManager == null)
                    usbManager = (UsbManager) MyApplication.getContext().getSystemService(Context.USB_SERVICE);
                if (usbManager == null) {
                    Log.e(TAG, "initUploadUSBDevice: usbManager==null");
                    return;
                }
                //获取所有已连接上的USB列表
                HashMap<String, UsbDevice> allConnectedUSBDeviceList = usbManager.getDeviceList();
                if (allConnectedUSBDeviceList == null || allConnectedUSBDeviceList.size() <= 0) {
                    downloadFlieListener.usbIntError("没有检查到上传USB设备");
                    return;
                }

                uploadFileDirUsbFile = null;
                todayMonthUsbFile = null;
                uploadFs = null;
                deviceID = -1;

                Log.d(TAG, "initUploadUSBDevice: " + "当前连接设备个数:" + allConnectedUSBDeviceList.size());
                for (UsbDevice usbDevice : allConnectedUSBDeviceList.values()) {
                    if (usbDevice == null)
                        continue;
                    //遍历连接的设备接口
                    for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                        UsbInterface usbInterface = usbDevice.getInterface(i);
                        if (usbInterface == null)
                            continue;
                        switch (usbInterface.getInterfaceClass()) {
                            case UsbConstants.USB_CLASS_MASS_STORAGE: {
                                Log.d(TAG, "initUploadUSBDevice: deviceID =" + deviceID);
                                if (usbManager.hasPermission(usbDevice)) {
                                    UsbMassStorageDevice device = getUsbMass(usbDevice);
                                    if (device == null) {
                                        Log.e(TAG, "usbDeviceScaner: device == null");
                                        continue;
                                    }
                                    try {
                                        device.init();
                                    } catch (Exception e) {
                                        Log.e(TAG, "readUSBDevice:device.init() error:" + e);
                                        continue;
                                    }

                                    if (device.getPartitions().size() <= 0) {
                                        Log.e(TAG, "readUSBDevice: " + "device.getPartitions().size() error");
                                        continue;
                                    }
                                    Partition partition = device.getPartitions().get(0);
                                    FileSystem currentFs = partition.getFileSystem();
                                    UsbFile mRootFolder = currentFs.getRootDirectory();

                                    try {
                                        UsbFile[] usbFileList = mRootFolder.listFiles();
                                        for (UsbFile usbFileItem : usbFileList) {
                                            if (usbFileItem.getName().contains(VariableInstance.getInstance().PictureDirName)) {
                                                uploadFs = currentFs;
                                                uploadFileDirUsbFile = usbFileItem;
                                                deviceID = usbDevice.getDeviceId();
                                                UsbFile[] usbFileList2 = uploadFileDirUsbFile.listFiles();
                                                for (UsbFile usbFileItem2 : usbFileList2) {
                                                    if (usbFileItem2.getName().contains(todayMonthDirString)) {
                                                        todayMonthUsbFile = usbFileItem2;
                                                        break;
                                                    }
                                                }
                                                if (todayMonthUsbFile == null)
                                                    todayMonthUsbFile = uploadFileDirUsbFile.createDirectory(todayMonthDirString);
                                            }
                                        }

                                        if (uploadFileDirUsbFile == null) {
                                            uploadFs = currentFs;
                                            uploadFileDirUsbFile = mRootFolder.createDirectory(VariableInstance.getInstance().PictureDirName);
                                            todayMonthUsbFile = uploadFileDirUsbFile.createDirectory(todayMonthDirString);
                                            deviceID = usbDevice.getDeviceId();
                                        }

                                    } catch (Exception e) {
                                        Log.e(TAG, "readUSBDevice: Exception =" + e);
                                    }
                                    Log.d(TAG, "usbDeviceScaner: deviceID =" + deviceID);
                                    if (deviceID == -1) {
                                        downloadFlieListener.usbIntError("deviceID == -1");
                                        continue;
                                    } else {
                                        uploadDeviceUSBPictureCount = 0;
                                        getUploadDeviceUSBPictureCount(uploadFileDirUsbFile);
                                        downloadFlieListener.initUploadUSBComplete(uploadDeviceUSBPictureCount);
                                    }
                                } else {
                                    Log.e(TAG, "usbDeviceScaner: hasPermission =false");
                                    @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getBroadcast(MyApplication.getContext(), 0, new Intent(CHECK_UPLOAD_PERMISSION), 0);
                                    usbManager.requestPermission(usbDevice, pendingIntent);
                                }
                            }
                            break;
                            default:
                                break;
                        }
                    }
                }
            }
        };
        uploadThreadExecutor.execute(runnable);
    }


    public int getUSBPictureCount() {
        if (uploadFileDirUsbFile == null)
            return -1;
        uploadDeviceUSBPictureCount = 0;
        getUploadDeviceUSBPictureCount(uploadFileDirUsbFile);

        return uploadDeviceUSBPictureCount;
    }

    private void checkConnectedDevice(UsbDevice usbDevice) {
        if (usbDevice == null)
            return;

        if (scanerThreadExecutor == null)
            scanerThreadExecutor = Executors.newSingleThreadExecutor();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (deviceID == -1) {
                    Log.d(TAG, "checkConnectedDevice: 初始化上传的usb未完成，deviceID =" + deviceID);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "checkConnectedDevice: 开始检查USB连接设备");

                if (usbManager == null)
                    usbManager = (UsbManager) MyApplication.getContext().getSystemService(Context.USB_SERVICE);
                if (usbManager == null) {
                    Log.e(TAG, "run: usbManager==null");
                    return;
                }

                for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                    UsbInterface usbInterface = usbDevice.getInterface(i);
                    if (usbInterface == null)
                        return;
                    switch (usbInterface.getInterfaceClass()) {
                        case UsbConstants.USB_CLASS_STILL_IMAGE:
                            downloadFlieListener.startDownload();
                            mtpDeviceScaner(usbDevice);
                            downloadFlieListener.downloadComplete();
                            break;
                        case UsbConstants.USB_CLASS_MASS_STORAGE:
                            downloadFlieListener.startDownload();
                            usbDeviceScaner(usbDevice);
                            downloadFlieListener.downloadComplete();
                            break;
                        default:
                            break;
                    }

                }

            }
        };
        scanerThreadExecutor.execute(runnable);
    }


    private void mtpDeviceScaner(UsbDevice usbDevice) {
        Log.d(TAG, "开始扫描数码相机");
        UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDevice);
        if (usbDeviceConnection == null) {
            Log.e(TAG, "数码相机打开失败 mtpDeviceScaner: usbDeviceConnection == null 结束扫描");
            return;
        }
        MtpDevice mtpDevice = new MtpDevice(usbDevice);
        if (mtpDevice == null || !mtpDevice.open(usbDeviceConnection)) {
            Log.e(TAG, "数码相机打开失败 mtpDevice == null || !mtpDevice.open(usbDeviceConnection) 结束扫描");
            return;
        }
        int[] storageIds = mtpDevice.getStorageIds();
        if (storageIds == null) {
            Log.e(TAG, "mtpDeviceScaner: 数码相机存储卷不可用 storageIds == null 结束扫描");
            return;
        }


        pictureInfos.clear();
        for (int storageId : storageIds) {
            int[] pictureHandlesItem = mtpDevice.getObjectHandles(storageId, 0, 0);
            if (pictureHandlesItem != null)
                for (int i : pictureHandlesItem) {
                    MtpObjectInfo mtpObjectInfo = mtpDevice.getObjectInfo(i);


                    if (mtpObjectInfo == null) {
                        continue;
                    }
                    String pictureName = mtpObjectInfo.getName();
                    String FileEnd = pictureName.substring(pictureName.lastIndexOf(".") + 1).toLowerCase();


                    if (isNeedDownloadFile(FileEnd)) {
                        if (VariableInstance.getInstance().formarCamera) {
                            mtpDevice.deleteObject(i);
                            continue;
                        }


                        long createDate = mtpObjectInfo.getDateCreated();

                        if (isOnTheSameDay(createDate))
                            pictureInfos.add(new PictureInfo(true, pictureName, createDate, i, null, null));
                    }
                }
        }
        Log.d(TAG, "readAllFileFromMTPDevice: 扫描到图片size =" + pictureInfos.size());

        Collections.sort(pictureInfos, new order());

        downloadFlieListener.scanerSize(pictureInfos.size());
        saveMTPPictureToUploadUSB(mtpDevice);
        usbDeviceConnection.close();
        mtpDevice.close();
    }

    private void usbDeviceScaner(UsbDevice usbDevice) {
        Log.d(TAG, "开始扫描数码usb");
        if (usbDevice == null) {
            Log.e(TAG, "usbDeviceScaner: USB设备为空 结束扫描");
            return;
        }
        if (usbDevice.getDeviceId() == deviceID) {
            Log.e(TAG, "usbDeviceScaner 检测到当前的U盘设备为上传usb, 结束扫描");
            return;
        }
        UsbMassStorageDevice device = getUsbMass(usbDevice);
        if (device == null) {
            Log.e(TAG, "usbDeviceScaner: device == null 结束扫描");
            return;
        }
        try {
            device.init();
        } catch (Exception e) {
            Log.e(TAG, "usbDeviceScaner: 结束扫描 device.init() error:" + e);
            return;
        }

        if (device.getPartitions().size() <= 0) {
            Log.e(TAG, "usbDeviceScaner: " + "device.getPartitions().size() error 结束扫描");
            return;
        }
        Partition partition = device.getPartitions().get(0);
        FileSystem currentFs = partition.getFileSystem();
        UsbFile mRootFolder = currentFs.getRootDirectory();

        pictureInfos.clear();
        readAllPicFileFromUSBDevice(currentFs, mRootFolder);
        Log.d(TAG, "usbDeviceScaner: " + "扫描到图片size =" + pictureInfos.size());
        Collections.sort(pictureInfos, new order());
        downloadFlieListener.scanerSize(pictureInfos.size());
        saveUSBFileToPhoneDevice();

    }


    private void saveMTPPictureToUploadUSB(MtpDevice mtpDevice) {
        Log.d(TAG, "readAllFileFromMTPDevice: readAllFileFromMTPDevice start ");
        if (mtpDevice == null) {
            return;
        }


        if (VariableInstance.getInstance().UploadMode == 3 || VariableInstance.getInstance().UploadMode == 4) {
            if (VariableInstance.getInstance().uploadSelectIndexList != null) {
                for (Integer integer : VariableInstance.getInstance().uploadSelectIndexList) {
                    try {
                        PictureInfo uploadSelectIndex = pictureInfos.remove(integer - 1);
                        if (uploadSelectIndex != null) {
                            saveMTPPictureToUploadUSB(mtpDevice, uploadSelectIndex, true);
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }


        for (PictureInfo pictureItem : pictureInfos) {
            saveMTPPictureToUploadUSB(mtpDevice, pictureItem, false);
        }

    }


    private void saveUSBFileToPhoneDevice() {

        if (VariableInstance.getInstance().UploadMode == 3 || VariableInstance.getInstance().UploadMode == 4) {
            if (VariableInstance.getInstance().uploadSelectIndexList != null) {
                for (Integer integer : VariableInstance.getInstance().uploadSelectIndexList) {
                    try {
                        PictureInfo uploadSelectIndex = pictureInfos.remove(integer - 1);
                        if (uploadSelectIndex != null) {
                            saveUSBFileToPhoneDevice(uploadSelectIndex, true);
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }
        for (PictureInfo pictureInfo : pictureInfos) {
            saveUSBFileToPhoneDevice(pictureInfo, false);
        }

    }

    private void saveUSBFileToPhoneDevice(PictureInfo pictureInfo, boolean isSelcet) {
        //写入文件
        FileOutputStream os = null;
        InputStream is = null;
        String pictureSaveLocalPath = null;
        File pictureSaveLocalFile = null;
        String pictureName = null;
        try {

            pictureName = pictureInfo.pictureName;
            pictureSaveLocalPath = tfcardpicturedir + File.separator + Utils.getyyMMddtring() + "-" + pictureName;
            pictureSaveLocalFile = new File(pictureSaveLocalPath);
            if (pictureSaveLocalFile.exists())
                pictureSaveLocalFile.delete();

            Log.d(TAG, "saveUSBFileToPhoneDevice: savePicturePath =" + pictureSaveLocalPath);
            os = new FileOutputStream(pictureSaveLocalPath);
            is = new UsbFileInputStream(pictureInfo.usbFile);

            int bytesRead = 0;
            byte[] buffer = new byte[pictureInfo.usbFileSystem.getChunkSize()];//作者的推荐写法是currentFs.getChunkSize()为buffer长度
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "saveUSBFileToPhoneDevice: FileNotFoundException =" + e);
        } catch (IOException e) {
            Log.e(TAG, "saveUSBFileToPhoneDevice: IOException =" + e);
        } finally {
            try {
                if (os != null) {
                    os.flush();
                    os.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "saveUSBFileToPhoneDevice:finally IOException =" + e);
            }
        }
        try {
            if (pictureSaveLocalFile != null && pictureSaveLocalFile.exists()) {
                boolean uploadSucceed = uploadToUSB(pictureSaveLocalFile);
                if (uploadSucceed) {
//                    pictureInfo.usbFile.delete();
                    boolean upload = false;
                    int UploadMode = VariableInstance.getInstance().UploadMode;
                    if (UploadMode == 1 || UploadMode == 2) {
                        String FileEnd = pictureName.substring(pictureName.lastIndexOf(".") + 1).toLowerCase();
                        if (UploadMode == 2) {
                            if (isJPGFormatFile(FileEnd))
                                pictureSaveLocalFile.delete();
                            else {
                                upload = true;
                            }
                        } else {
                            if (isRowFormatFile(FileEnd))
                                pictureSaveLocalFile.delete();
                            else {
                                upload = true;
                            }
                        }
                    } else {
                        if (isSelcet) {
                            upload = true;
                        } else
                            pictureSaveLocalFile.delete();
                    }
                    if (upload) {
                        downloadFlieListener.addUploadRemoteFile(new UploadFileModel(pictureSaveLocalPath));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private boolean uploadToUSB(File localFile) {
        if (localFile == null || !localFile.exists()) {
            Log.e(TAG, "uploadToUSB: \ntodayDir =" + "\n localFile =" + localFile);
            return false;
        }

        if (uploadFileDirUsbFile == null) {
            Log.e(TAG, "uploadToUSB: uploadFileDirUsbFile == null");
            return false;
        }

        long time = System.currentTimeMillis();
        long fileSize = 0;
        Log.d(TAG, "uploadToUSB: localFile =" + localFile);
        UsbFileOutputStream os = null;
        InputStream is = null;
        try {
            UsbFile create = todayMonthUsbFile.createFile(localFile.getName());
            os = new UsbFileOutputStream(create);
            is = new FileInputStream(localFile);
            fileSize = is.available();
            int bytesRead;
            byte[] buffer = new byte[uploadFs.getChunkSize()];
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            Log.e(TAG, "uploadToUSB: Exception =" + e);
        } finally {
            try {
                if (os != null) {
                    os.flush();
                    os.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
            }
        }
        VariableInstance.getInstance().downdNum++;


        String speed = fileSize / ((System.currentTimeMillis() - time) / 1000) / 1024 + "";
        Log.d(TAG, "uploadToUSB: 上传USB速度：speed =" + speed + ",fileSize =" + fileSize);
        downloadFlieListener.downloadNum(VariableInstance.getInstance().downdNum, speed);
        return true;
    }


    private void readAllPicFileFromUSBDevice(FileSystem fileSystem, UsbFile usbFile) {
        try {
            UsbFile[] usbFileList = usbFile.listFiles();
            for (UsbFile usbFileItem : usbFileList) {
                if (usbFileItem.isDirectory()) {
                    readAllPicFileFromUSBDevice(fileSystem, usbFileItem);
                } else {
                    //获取文件后缀
                    String fileName = usbFileItem.getName();
                    String FileEnd = fileName.substring(usbFileItem.getName().lastIndexOf(".") + 1).toLowerCase();

                    //检查文件后缀是不是图片格式
                    if (isNeedDownloadFile(FileEnd)) {
                        if (VariableInstance.getInstance().formarCamera) {
                            usbFile.delete();
                            continue;
                        }
                        long createDate = usbFileItem.createdAt();
                        if (isOnTheSameDay(createDate))
                            pictureInfos.add(new PictureInfo(false, fileName, createDate, 0, fileSystem, usbFileItem));
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "readAllPicFileFromUSBDevice: 遍历USB文件异常");
        }
    }

    public long MILLIS_IN_DAY = 1000L * 60 * 60 * 24;
    private long currentDataMillis;

    private boolean isOnTheSameDay(long createDate) {
        long interval = currentDataMillis - createDate;
        return interval < MILLIS_IN_DAY && interval > -1L * MILLIS_IN_DAY && toDay(createDate) == toDay(currentDataMillis);
    }

    private long toDay(long millis) {
        return (millis + TimeZone.getDefault().getOffset(millis)) / MILLIS_IN_DAY;
    }

    private int uploadDeviceUSBPictureCount;

    private void getUploadDeviceUSBPictureCount(UsbFile usbFile) {
        try {
            UsbFile[] usbFileList = usbFile.listFiles();
            for (UsbFile usbFileItem : usbFileList) {
                if (usbFileItem.isDirectory()) {
                    getUploadDeviceUSBPictureCount(usbFileItem);
                } else {
                    String FileEnd = usbFileItem.getName().substring(usbFileItem.getName().lastIndexOf(".") + 1).toLowerCase();
                    if (isNeedDownloadFile(FileEnd)) {
                        uploadDeviceUSBPictureCount++;
                    }
                }
            }
        } catch (Exception e) {

        }
    }


    private boolean isNeedDownloadFile(String FileEnd) {
        if (FileEnd.equals("jpg") || FileEnd.equals("nif") || FileEnd.equals("crw") || FileEnd.equals("raw")
                || FileEnd.equals("arw") || FileEnd.equals("nef") || FileEnd.equals("raf") || FileEnd.equals("crw")
                || FileEnd.equals("pef") || FileEnd.equals("rw2") || FileEnd.equals("dng") || FileEnd.equals("cr2"))
            return true;
        return false;
    }


    private boolean isRowFormatFile(String FileEnd) {
        if ((FileEnd.equals("nif") || FileEnd.equals("crw") || FileEnd.equals("raw")
                || FileEnd.equals("arw") || FileEnd.equals("nef") || FileEnd.equals("raf") || FileEnd.equals("crw")
                || FileEnd.equals("pef") || FileEnd.equals("rw2") || FileEnd.equals("dng") || FileEnd.equals("cr2")))
            return true;
        return false;
    }

    private boolean isJPGFormatFile(String FileEnd) {
        if ((FileEnd.equals("jpg")))
            return true;
        return false;
    }


    private void saveMTPPictureToUploadUSB(MtpDevice mtpDevice, PictureInfo pictureInfo, boolean isSelect) {
        //每一个mtpObjectInfo 是一个图片对象 检索MtpObjectInfo对象
        try {
            String scanFileName = pictureInfo.pictureName;

            String pictureSaveLocalPath = tfcardpicturedir + File.separator + Utils.getyyMMddtring() + "-" + scanFileName;

            File pictureTpmSaveFile = new File(pictureSaveLocalPath);
            if (pictureTpmSaveFile != null && pictureTpmSaveFile.exists())
                pictureTpmSaveFile.delete();
            mtpDevice.importFile(pictureInfo.mtpPictureID, pictureSaveLocalPath);

            if (pictureTpmSaveFile != null && pictureTpmSaveFile.exists()) {

                boolean uploadSucceed = uploadToUSB(pictureTpmSaveFile);

                if (uploadSucceed) {
//                    mtpDevice.deleteObject(pictureInfo.mtpPictureID);
                    boolean upload = false;
                    int UploadMode = VariableInstance.getInstance().UploadMode;
                    if (UploadMode == 1 || UploadMode == 2) {
                        String FileEnd = scanFileName.substring(scanFileName.lastIndexOf(".") + 1).toLowerCase();
                        if (UploadMode == 1) {
                            if (isJPGFormatFile(FileEnd))
                                pictureTpmSaveFile.delete();
                            else {
                                upload = true;
                            }
                        } else {
                            if (isRowFormatFile(FileEnd))
                                pictureTpmSaveFile.delete();
                            else {
                                upload = true;
                            }
                        }
                    } else {
                        if (isSelect) {
                            upload = true;
                        } else {
                            pictureTpmSaveFile.delete();
                        }
                    }
                    if (upload) {
                        downloadFlieListener.addUploadRemoteFile(new UploadFileModel(pictureSaveLocalPath));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "readAllFileFromMTPDevice: 图片复制出错");
        }
    }


    /**
     * USBDevice 转换成UsbMassStorageDevice 对象
     */
    private UsbMassStorageDevice getUsbMass(UsbDevice usbDevice) {
        UsbMassStorageDevice[] storageDevices = UsbMassStorageDevice.getMassStorageDevices(MyApplication.getContext());

        for (UsbMassStorageDevice device : storageDevices) {
            if (usbDevice.equals(device.getUsbDevice())) {
                return device;
            }
        }
        return null;
    }


    class PictureInfo {
        public boolean mtpModel;
        public String pictureName;
        public long pictureCreateData;

        public int mtpPictureID;

        public UsbFile usbFile;
        public FileSystem usbFileSystem;


        public PictureInfo(boolean mtpModel, String pictureName, long pictureCreateData, int mtpPictureID, FileSystem usbFileSystem, UsbFile usbFile) {
            this.mtpModel = mtpModel;
            this.pictureName = pictureName;
            this.pictureCreateData = pictureCreateData;
            this.mtpPictureID = mtpPictureID;
            this.usbFile = usbFile;
            this.usbFileSystem = usbFileSystem;
        }
    }


    public class order implements Comparator<PictureInfo> {

        @Override
        public int compare(PictureInfo lhs, PictureInfo rhs) {
            if (rhs.pictureCreateData > lhs.pictureCreateData)
                return 1;
            else if (rhs.pictureCreateData == lhs.pictureCreateData)
                return 0;
            return -1;
        }

    }

    public interface DownloadFlieListener {
        void addUploadRemoteFile(UploadFileModel uploadFileModel);

        void startDownload();

        void downloadComplete();

        void initUploadUSBComplete(int pictureCount);

        void usbIntError(String message);

        void scanerSize(int size);

        void downloadNum(int num, String speed);
    }


}


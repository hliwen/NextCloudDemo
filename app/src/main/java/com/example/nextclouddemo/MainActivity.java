package com.example.nextclouddemo;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.gpiotest.GpioActivity;
import com.example.gpiotest.LedControl;
import com.example.nextclouddemo.model.DeviceInfoModel;
import com.example.nextclouddemo.model.MyMessage;
import com.example.nextclouddemo.model.ServerUrlModel;
import com.example.nextclouddemo.model.UploadFileModel;
import com.example.nextclouddemo.mqtt.MqttManager;
import com.example.nextclouddemo.utils.Communication;
import com.example.nextclouddemo.utils.Log;
import com.example.nextclouddemo.utils.RemoteOperationUtils;
import com.example.nextclouddemo.utils.Utils;
import com.github.mjdev.libaums.fs.UsbFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class MainActivity extends Activity {

    private static final String Record1 = "Start,Record1;";
    private static final String Record2 = "Start,Record2;";
    private static final String FormatUSB = "Start,Format;";
    private static final String FormatTF = "Start,FormatTF;";
    private static final String FormatCamera = "Start,FormatCamera;";
    private static final String Upload = "Start,Upload;";
    private static final String UploadMode1 = "Set,UploadMode,1;";
    private static final String UploadMode2 = "Set,UploadMode,2;";
    private static final String UploadMode3 = "Set,UploadMode,3";
    private static final String UploadMode4 = "Set,UploadMode,4,";
    private static final String GetInfo = "Get,Info;";
    private static final String return2GImei = "return2GImei";
    private static final String UploadEndUploadUseTime = "Upload,End,UploadUseTime,";
    private static final String AppShutdownAck = "App,shutdown,ack;";


    private static final int close_device_timeout = 3 * 60 * 1000;
    private static final int close_device_timeout_a = 10 * 60 * 1000;
    private static final boolean phoneDebug = true;


    private static String TAG = "MainActivitylog";
    private MyHandler mHandler;
    private OwnCloudClient mClient;

    private USBMTPReceiver usbmtpReceiver;


    char mGpioCharB = 'b';
    private String returnImei;
    private String deveceName;
    private Communication communication;
    private boolean doingInit;
    private RemoteOperationUtils operationUtils;
    private boolean initRemoteSucceed;

    private TextView messageText;
    private String messageTextString;

    private CameraHelper mCameraHelper;
    private long lastOpenCameraTime;
    private RelativeLayout surfaceViewParent;


    private boolean remoteUploading;
    private boolean localDownling;
    private boolean openDeviceProtFlag;

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);


        mHandler = new MyHandler(MainActivity.this);

        surfaceViewParent = findViewById(R.id.surfaceViewParent);
        messageText = findViewById(R.id.messageText);
        updateServerStateUI(false);
        TextView 入网号 = findViewById(R.id.入网号);
        入网号.setText("入网号:" + getPhoneImei());
        TextView 相机状态 = findViewById(R.id.相机状态);
        相机状态.setText("相机状态:" + openDeviceProtFlag);

        mHandler.sendEmptyMessageDelayed(msg_close_device, close_device_timeout);
        EventBus.getDefault().register(this);

        communication = new Communication();
        operationUtils = new RemoteOperationUtils(remoteOperationListener);

        getUploadModel();
        String[] value = Utils.haveNoPermissions(MainActivity.this);
        if (value.length > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(value, 111);
        }

        File fileFolder = new File(VariableInstance.getInstance().TFCardPictureDir);
        if (!fileFolder.exists()) {
            fileFolder.mkdir();
        }


        openDeviceProt(false);
        openNetworkLed(true);

        initNetWork();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                registerUSBReceiver();
                openCamera();//TODO hu
            }
        }, 3000);

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(MyPhoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);


    }

    private int signalStrengthValue;

    PhoneStateListener MyPhoneListener = new PhoneStateListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            int asu = signalStrength.getGsmSignalStrength();
            signalStrengthValue = -113 + 2 * asu;
        }
    };


    public void initNetWork() {
        Log.e(TAG, "initNetWork: ");
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder request = new NetworkRequest.Builder();
        if (phoneDebug) {
            request.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            request.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            request.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        }


        NetworkRequest build = request.build();
        connectivityManager.requestNetwork(build, new ConnectivityManager.NetworkCallback() {
            public void onAvailable(Network network) {
                Log.e(TAG, "Network onAvailable: doingInit =" + doingInit);
                if (doingInit)
                    return;
                doingInit = true;
                initAddress();
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.e(TAG, "Network  onLost: ");
                doingInit = false;
                operationUtils.stopUploadThread();
                MqttManager.getInstance().release();
                openNetworkLed(false);
                openNetworkLed(true);
            }
        });
    }


    private void initAddress() {
        getInfo();
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                TextView 入网号 = findViewById(R.id.入网号);
                入网号.setText("入网号:" + getPhoneImei());
            }
        });

        Thread workThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerUrlModel serverUrlModel = communication.getServerUrl();
                    Log.e(TAG, "run: serverUrlModel =" + serverUrlModel);
                    if (serverUrlModel == null || serverUrlModel.responseCode != 200) {
                        mHandler.removeMessages(msg_reload_device_info);
                        mHandler.sendEmptyMessageDelayed(msg_reload_device_info, 10000);
                        updateServerStateUI(false);
                        return;
                    }
                    DeviceInfoModel deviceInfoModel = communication.getDeviceInfo(getPhoneImei());
                    if (serverUrlModel == null || deviceInfoModel.responseCode != 200) {
                        mHandler.removeMessages(msg_reload_device_info);
                        mHandler.sendEmptyMessageDelayed(msg_reload_device_info, 10000);
                        updateServerStateUI(false);
                        return;
                    }

                    mHandler.removeMessages(msg_reload_device_info);

                    Log.e(TAG, "run: deviceInfoModel =" + deviceInfoModel);

                    returnImei = deviceInfoModel.returnImei;
                    deveceName = deviceInfoModel.deveceName;
                    EventBus.getDefault().post(return2GImei);

                    mClient = OwnCloudClientFactory.createOwnCloudClient(serverUrlModel.serverUri, MainActivity.this, true);
                    mClient.setCredentials(OwnCloudCredentialsFactory.newBasicCredentials(deviceInfoModel.username, deviceInfoModel.password));
                    operationUtils.mClient = mClient;


                    boolean initRemoteSucceed1 = operationUtils.initRemoteDir(deviceInfoModel.deveceName);


                    initRemoteSucceed = initRemoteSucceed1;
                    updateServerStateUI(initRemoteSucceed);
                    if (initRemoteSucceed) {
                        mHandler.removeMessages(msg_close_device);
                        mHandler.removeMessages(msg_reload_device_info);
                        operationUtils.startUploadThread();
                        operationUtils.startVideoWorkThread();
                    } else {
                        mHandler.removeMessages(msg_reload_device_info);
                        mHandler.sendEmptyMessageDelayed(msg_reload_device_info, 10000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "run:Exception111 = " + e);
                    updateServerStateUI(false);
                    doingInit = false;
                }
            }
        });
        workThread.start();
    }


    private void initMqtt() {
        MqttManager
                .getInstance()
                .creatConnect(
                        "tcp://120.78.192.66:1883",
                        "devices",
                        "a1237891379",
                        "123",
                        "/camera/v1/device/" + returnImei + "/android");

        MqttManager.getInstance().subscribe("/camera/v2/device/" + returnImei + "/android/send", 1);
    }


    private USBMTPReceiver.DownloadFlieListener downloadFlieListener = new USBMTPReceiver.DownloadFlieListener() {
        @Override
        public void addUploadRemoteFile(UploadFileModel uploadFileModel) {
            Log.d(TAG, "downloadOneFileDone: uploading =" + remoteUploading);
            operationUtils.addUploadRemoteFile(uploadFileModel);
        }

        @Override
        public void startDownload() {
            Log.d(TAG, "startDownload: uploading =" + remoteUploading);
            localDownling = true;
            if (!remoteUploading)
                startDownLed(true);
            mHandler.removeMessages(msg_close_device);
        }

        @Override
        public void downloadComplete() {
            Log.d(TAG, "downloadComplete: remoteUploading =" + remoteUploading);
            localDownling = false;
            if (!remoteUploading)
                startDownLed(false);
            openDeviceProt(false);
            mHandler.removeMessages(msg_close_device);
            mHandler.sendEmptyMessageDelayed(msg_close_device, close_device_timeout);
            getInfo();
        }

        @Override
        public void initUploadUSBComplete(int pictureCount) {
            Log.d(TAG, "initUploadUSBComplete: ");
            PhotoSum = pictureCount;
            openDeviceProt(true);
            getInfo();
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    TextView U盘空间 = findViewById(R.id.U盘空间);
                    int capacity = 0;
                    int freeSpace = 0;
                    if (usbmtpReceiver != null) {
                        capacity = usbmtpReceiver.getCapacity();
                        freeSpace = usbmtpReceiver.getFreeSpace();
                    }
                    U盘空间.setText("U盘空间:" + "\ncapacity:" + capacity + "\nfreeSpace:" + freeSpace);

                    TextView U盘图片数量 = findViewById(R.id.U盘图片数量);
                    U盘图片数量.setText("U盘图片数量:" + pictureCount);
                }
            });
        }

        @Override
        public void usbIntError(String message) {
            Log.e(TAG, "usbIntError: message =" + message);
        }

        @Override
        public void scanerSize(int size) {
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    TextView 本次上传数量 = findViewById(R.id.本次上传数量);
                    本次上传数量.setText("本次上传数量:" + size);
                }
            });
        }

        @Override
        public void downloadNum(int num, String speed) {
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    TextView 已下载张数 = findViewById(R.id.已下载张数);
                    已下载张数.setText("已下载张数:" + VariableInstance.getInstance().downdNum + "\n上传USB速度:" + speed);
                }
            });
        }
    };

    RemoteOperationUtils.RemoteOperationListener remoteOperationListener = new RemoteOperationUtils.RemoteOperationListener() {
        @Override
        public void allFileUploadComplete(long totalTime) {
            Log.d(TAG, "allFileUploadComplete: downling =" + localDownling + ",totalTime =" + totalTime / 1000);
            remoteUploading = false;
            if (!localDownling) {
                restLed();
            }
            if (totalTime != 0)
                UploadUseTime = totalTime / 1000;
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    TextView 本次上传耗时 = findViewById(R.id.本次上传耗时);
                    本次上传耗时.setText("本次上传耗时:" + totalTime / 1000 + "s");
                }
            });
            getInfo();
            mHandler.removeMessages(msg_close_device);
            mHandler.sendEmptyMessageDelayed(msg_close_device, close_device_timeout);
        }

        @Override
        public void pictureUploadStart() {
            Log.d(TAG, "fileUploadStart: ");
            remoteUploading = true;
            mHandler.removeMessages(msg_close_device);
            startDownLed(false);
        }

        @Override
        public void pictureUploadEnd() {
            Log.d(TAG, "fileUploadEnd: ");
            remoteUploading = false;
        }


        @Override
        public void updateUploadSpeed(String speed) {
            Log.d(TAG, "updateUploadSpeed: speed =" + speed);
            saveUploadUploadSpeed(speed);

            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    TextView 已上传张数 = findViewById(R.id.已上传张数);
                    已上传张数.setText("已上传张数:" + VariableInstance.getInstance().uploadNum + "\n上传服务器速度：" + speed);
                }
            });
            getInfo();
        }


        @Override
        public void videoUploadStart() {
            mHandler.removeMessages(msg_close_device);
            startDownLed(false);
        }

        @Override
        public void uploadVideoComplete(boolean succeed) {
            Log.e(TAG, "uploadVideoComplete: 上传视频结果 " + succeed);
            mHandler.removeMessages(msg_close_device);
            mHandler.sendEmptyMessageDelayed(msg_close_device, close_device_timeout);
            if (!localDownling && !remoteUploading) {
                restLed();
            }
        }

        @Override
        public void uploadLogcatComplete() {
            getInfo();
            UploadEndUploadUseTime(UploadUseTime);
        }

        @Override
        public boolean isDownling() {
            return localDownling;
        }

        @Override
        public boolean isVideoPreviewing() {
            if (mCameraHelper != null && mCameraHelper.isPreview)
                return true;
            return false;
        }
    };


    private void registerUSBReceiver() {
        usbmtpReceiver = new USBMTPReceiver(getApplicationContext(), downloadFlieListener);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(USBMTPReceiver.CHECK_PERMISSION);
        intentFilter.addAction(USBMTPReceiver.CHECK_UPLOAD_PERMISSION);
        registerReceiver(usbmtpReceiver, intentFilter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (usbmtpReceiver != null) {
            usbmtpReceiver.shutdownThread();
            unregisterReceiver(usbmtpReceiver);
        }
        mHandler.removeCallbacksAndMessages(null);
        closeCamera();
        openDeviceProt(false);
        operationUtils.stopUploadThread();
        operationUtils.stopUploadVideoThread();
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(MyPhoneListener, PhoneStateListener.LISTEN_NONE);
        EventBus.getDefault().unregister(this);
        MqttManager.getInstance().release();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveLogMessage(MyMessage message) {
        if (message == null)
            return;
        messageTextString = messageTextString + "\n" + message.message + "\n";
        messageText.setText(messageTextString);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMqttMessage(String message) {
        Log.e(TAG, "receiveMqttMessage: message =" + message);
        if (message == null)
            return;

        if (message.contains(UploadMode3)) {
            UploadMode3(message);
            getInfo();
            return;
        }

        if (message.contains(UploadMode4)) {
            UploadMode4(message);
            getInfo();
            return;
        }

        switch (message) {
            case Record1:
                openCamera();
                break;
            case Record2:
                openCamera();
                break;
            case FormatUSB:
                formatUSB();
                break;
            case FormatTF:
                formatTF();
                break;
            case FormatCamera:
                formatCamera();
                break;
            case Upload:
                break;
            case UploadMode1:
                UploadMode1();
                getInfo();
                break;
            case UploadMode2:
                UploadMode2();
                getInfo();
                break;
            case GetInfo:
                getInfo();
                break;
            case return2GImei:
                initMqtt();
                break;
            case AppShutdownAck:
                AppShutdownAck();
                break;
            case UploadMode4:
                break;
        }
    }


    private boolean sendShutDown;

    private void UploadEndUploadUseTime(long useTime) {
        sendShutDown = true;
        mHandler.removeMessages(msg_send_ShutDown);
        mHandler.sendEmptyMessageDelayed(msg_send_ShutDown, close_device_timeout_a);
        sendMessageToMqtt(UploadEndUploadUseTime + useTime + ";");
    }

    private void AppShutdownAck() {
        if (sendShutDown) {
            mHandler.removeMessages(msg_send_ShutDown);
            mHandler.sendEmptyMessageDelayed(msg_send_ShutDown, 5000);
            sendShutDown = false;
        }
    }

    private void formatUSB() {
        Log.e(TAG, "formatUSB: ");
        if (usbmtpReceiver != null)
            delectUploadUSBFile(usbmtpReceiver.uploadFileDirUsbFile);
    }

    private void formatCamera() {

        VariableInstance.getInstance().formarCamera = true;

        if (usbmtpReceiver != null) {
            usbmtpReceiver.formatCamera();
        }
        openDeviceProt(false);
        openDeviceProt(true);
    }

    private void formatTF() {
        Log.e(TAG, "formatTF: ");

        File tfcardpicturedir = new File(VariableInstance.getInstance().TFCardPictureDir);
        if (tfcardpicturedir != null && tfcardpicturedir.exists()) {
            Utils.deleteAllFiles(tfcardpicturedir);
            File fileFolder = new File(VariableInstance.getInstance().TFCardPictureDir);
            if (!fileFolder.exists()) {
                fileFolder.mkdir();
            }
        }

        File tfcardvideodir = new File(VariableInstance.getInstance().TFCardVideoDir);
        if (tfcardvideodir != null && tfcardvideodir.exists()) {
            Utils.deleteAllFiles(tfcardvideodir);
            File fileFolder = new File(VariableInstance.getInstance().TFCardVideoDir);
            if (!fileFolder.exists()) {
                fileFolder.mkdir();
            }
        }

        File logcatdir = new File(VariableInstance.getInstance().LogcatDir);
        if (logcatdir != null && logcatdir.exists()) {
            Utils.deleteAllFiles(logcatdir);
            File fileFolder = new File(VariableInstance.getInstance().LogcatDir);
            if (!fileFolder.exists()) {
                fileFolder.mkdir();
            }
        }

    }


    private void delectUploadUSBFile(UsbFile usbFile) {
        Log.e(TAG, "delectUploadUSBFile: usbFile =" + usbFile);
        if (usbFile == null)
            return;
        Log.e(TAG, "delectUploadUSBFile: " + usbFile.getName());
        try {
            UsbFile[] usbFileList = usbFile.listFiles();
            for (UsbFile file : usbFileList) {
                file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "delectUploadUSBFile: Exception =" + e);
        }
        Log.d(TAG, "delectUploadUSBFile: 删除文件完成");
    }

    private void getInfo() {
        sendMessageToMqtt(serverGetInfo());
    }


    private void sendMessageToMqtt(String message) {
        Log.d(TAG, "sendMessageToMqtt: message =" + message);
        MqttManager.getInstance().publish("/camera/v2/device/" + returnImei + "/android/receive", 1, message);
    }

    private void UploadMode1() {
        VariableInstance.getInstance().UploadMode = 1;
        VariableInstance.getInstance().uploadSelectIndexList = null;
        saveUploadModel(null);
    }

    private void UploadMode2() {
        VariableInstance.getInstance().UploadMode = 2;
        VariableInstance.getInstance().uploadSelectIndexList = null;
        saveUploadModel(null);
    }

    private void UploadMode3(String message) {
        String num = message.substring(message.lastIndexOf(",") + 1, message.length() - 1).trim();
        try {
            int number = Integer.parseInt(num);
            Log.d(TAG, "UploadMode3: number =" + number);
            VariableInstance.getInstance().UploadMode = 3;
            VariableInstance.getInstance().uploadSelectIndexList = new ArrayList<>();
            VariableInstance.getInstance().uploadSelectIndexList.clear();
            VariableInstance.getInstance().uploadSelectIndexList.add(number);
            saveUploadModel(number + "");
        } catch (Exception e) {

        }
    }


    private void UploadMode4(String message) {
        message = message.substring(UploadMode4.length(), message.length() - 1);
        String[] data = message.split(",");
        VariableInstance.getInstance().UploadMode = 4;
        VariableInstance.getInstance().uploadSelectIndexList = new ArrayList<>();
        VariableInstance.getInstance().uploadSelectIndexList.clear();
        if (data != null && data.length > 0) {
            for (String datum : data) {
                try {
                    VariableInstance.getInstance().uploadSelectIndexList.add(Integer.parseInt(datum));
                } catch (Exception e) {

                }
            }
        }
        saveUploadModel(message);
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
    }

    private void closeCamera() {
        Log.d(TAG, "closeCamera: ");
        if (mCameraHelper != null)
            mCameraHelper.closeCamera();
        mCameraHelper = null;
        surfaceViewParent.removeAllViews();
    }

    private void openCamera() {
        if (System.currentTimeMillis() - lastOpenCameraTime < 2000) {
            Log.d(TAG, "openCamera: 点击录制过于频繁");
            return;
        }
        lastOpenCameraTime = System.currentTimeMillis();

        closeCamera();

        mCameraHelper = new CameraHelper(new CameraHelper.CameraHelperListener() {
            @Override
            public void addPicture(String path) {
                operationUtils.addVideoFile(path);
            }

            @Override
            public void finishPreview() {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        closeCamera();
                    }
                }, 100);
            }
        });

        surfaceViewParent.removeAllViews();
        SurfaceView surfaceView = new SurfaceView(MainActivity.this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        surfaceView.setLayoutParams(layoutParams);
        surfaceViewParent.addView(surfaceView);
        SurfaceHolder mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);


        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                if (mCameraHelper != null) {
                    mCameraHelper.openCamera(holder, MainActivity.this);
                    mCameraHelper.onStartPreview(width, height);
                }
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });
    }


    @SuppressLint("SetTextI18n")
    public void onClickHandler(View button) {
        if (button.getId() == R.id.downloadphoto) {
            openDeviceProt(false);
            openDeviceProt(true);
        } else if (button.getId() == R.id.guanji) {
            Utils.closeAndroid();
        } else if (button.getId() == R.id.aaa) {
            startActivity(new Intent(MainActivity.this, GpioActivity.class));
        } else if (button.getId() == R.id.openPreview) {
            openCamera();
        } else if (button.getId() == R.id.closePreview) {
            closeCamera();
        } else if (button.getId() == R.id.clearView) {
            messageTextString = "";
            messageText.setText(messageTextString);
        } else if (button.getId() == R.id.服务器状态) {
            TextView 服务器状态 = findViewById(R.id.服务器状态);
            服务器状态.setText("服务器状态：" + initRemoteSucceed);
        } else if (button.getId() == R.id.入网号) {
            TextView 入网号 = findViewById(R.id.入网号);
            入网号.setText("入网号:" + getPhoneImei());
        } else if (button.getId() == R.id.云端名称) {
            TextView 云端名称 = findViewById(R.id.云端名称);
            云端名称.setText("云端名称:" + deveceName);
        } else if (button.getId() == R.id.本次上传数量) {
            TextView 本次上传数量 = findViewById(R.id.本次上传数量);
            本次上传数量.setText("本次上传数量:");
        } else if (button.getId() == R.id.相机状态) {
            TextView 相机状态 = findViewById(R.id.相机状态);
            相机状态.setText("相机状态:" + openDeviceProtFlag);
        } else if (button.getId() == R.id.U盘空间) {
            TextView U盘空间 = findViewById(R.id.U盘空间);
            int capacity = usbmtpReceiver.getCapacity();
            int freeSpace = usbmtpReceiver.getFreeSpace();
            U盘空间.setText("U盘空间:" + "\ncapacity:" + capacity + "\nfreeSpace:" + freeSpace);
        }
    }

    private void updateServerStateUI(boolean succeed) {
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                TextView 服务器状态 = findViewById(R.id.服务器状态);
                服务器状态.setText("服务器状态：" + succeed);
                if (succeed) {
                    TextView 云端名称 = findViewById(R.id.云端名称);
                    云端名称.setText("云端名称:" + deveceName);

                    TextView 入网号 = findViewById(R.id.入网号);
                    入网号.setText("入网号:" + getPhoneImei());
                }
            }
        });
    }

    private int capacity = 0;
    private int freeSpace = 0;
    private int PhotoSum = 0;
    private long UploadUseTime;

    private String serverGetInfo() {
        int capacityA = 0;
        int freeSpaceA = 0;
        int PhotoSumA = 0;

        if (usbmtpReceiver != null) {
            capacityA = usbmtpReceiver.getCapacity();
            if (capacityA != 0)
                capacity = capacityA;
            freeSpaceA = usbmtpReceiver.getFreeSpace();
            if (freeSpaceA != 0)
                freeSpace = freeSpaceA;
            PhotoSumA = usbmtpReceiver.getUSBPictureCount();
            if (PhotoSumA != 0)
                PhotoSum = PhotoSumA;
        }

        String uploadModelString;

        if (VariableInstance.getInstance().UploadMode == 1) {
            uploadModelString = "1,0";

        } else if (VariableInstance.getInstance().UploadMode == 2) {
            uploadModelString = "2,0";

        } else if (VariableInstance.getInstance().UploadMode == 3) {
            if (VariableInstance.getInstance().uploadSelectIndexList == null || VariableInstance.getInstance().uploadSelectIndexList.size() == 0)
                uploadModelString = "3,0";
            else
                uploadModelString = "3," + VariableInstance.getInstance().uploadSelectIndexList.get(0);
        } else {
            if (VariableInstance.getInstance().uploadSelectIndexList == null || VariableInstance.getInstance().uploadSelectIndexList.size() == 0)
                uploadModelString = "4,0";
            else {
                uploadModelString = "4";
                for (Integer integer : VariableInstance.getInstance().uploadSelectIndexList) {
                    uploadModelString = uploadModelString + "," + integer;
                }
            }
        }

        String info = "4gCcid," + getPhoneNumber() +
                ";UploadSpeed," + getUploadploadSpeed() +
                ";4gCsq," + getSignalStrength() +
                ";SdFree," + freeSpace +
                ";SdFull," + capacity +
                ";PhotoSum," + PhotoSum +
                ";PhotoUploadThisTime," + VariableInstance.getInstance().uploadNum +
                ";UploadMode," + uploadModelString +
                ";UploadUseTime," + UploadUseTime + ";";
        Log.e(TAG, "serverGetInfo: info =" + info);
        return info;
    }

    private String getSignalStrength() {
        if (signalStrengthValue > 0)
            return "0";
        else if (signalStrengthValue > -55)
            return "4";
        else if (signalStrengthValue > -70)
            return "3";
        else if (signalStrengthValue > -85)
            return "2";
        else if (signalStrengthValue > -100)
            return "1";
        else
            return "1";
    }

    private String getPhoneNumber() {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            @SuppressLint({"HardwareIds", "MissingPermission"}) String number = telephonyManager.getLine1Number();
            Log.d(TAG, "getPhoneNumber: number =" + number);
            if (number == null) {
                number = "0";
            }
            return number.substring(3);
        } catch (Exception | Error e) {
            Log.e(TAG, "getPhoneNumber: Exception =" + e);
            return "0";
        }
    }

    private String getPhoneImei() {
        if (phoneDebug)
            return "867706050952138";
        try {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            @SuppressLint("HardwareIds") String imei = telephonyManager.getDeviceId();
            Log.d(TAG, "getPhoneImei: imei =" + imei);
            if (imei == null) {
                imei = "0";
            }
            return imei;
        } catch (Exception | Error e) {
            Log.e(TAG, "getPhoneImei: Exception =" + e);
            return "0";
        }
    }


    private void openDeviceProt(boolean open) {

        Log.d(TAG, "openDeviceProt: 设备通信端口 led: " + (open ? "打开" : "关闭") + ", 当前状态" + (openDeviceProtFlag ? "打开" : "关闭"));
        if (openDeviceProtFlag == true && open)
            return;
        openDeviceProtFlag = open;
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                TextView 相机状态 = findViewById(R.id.相机状态);
                相机状态.setText("相机状态:" + openDeviceProtFlag);
            }
        });

        if (phoneDebug)
            return;

        if (open) {
            LedControl.writeGpio(mGpioCharB, 2, 1);
        } else {
            LedControl.writeGpio(mGpioCharB, 2, 0);
        }
    }

    private void openNetworkLed(boolean open) {
        Log.d(TAG, "openNetworkLed: 网络端口 led: " + (open ? "打开" : "关闭"));
        if (phoneDebug)
            return;
        if (open) {
            LedControl.writeGpio(mGpioCharB, 3, 1);//打开网络
        } else {
            LedControl.writeGpio(mGpioCharB, 3, 0);//打开网络
        }
    }

    private void startDownLed(boolean start) {
        Log.d(TAG, "startDownLed: 下载 led: " + (start ? "打开" : "关闭"));
        if (phoneDebug)
            return;
        if (start) {
            LedControl.nativeEnableLed(LedControl.LED_RED_TRIGGER_PATH, LedControl.LED_TIMER);
        } else {
            LedControl.nativeEnableLed(LedControl.LED_RED_TRIGGER_PATH, LedControl.LED_HEARTBEAT);
        }
    }

    private void restLed() {
        Log.d(TAG, "restLed:  恢复 led ------");
        if (phoneDebug)
            return;
        LedControl.nativeEnableLed(LedControl.LED_RED_TRIGGER_PATH, LedControl.LED_NONE);
        LedControl.nativeEnableLed(LedControl.LED_RED_BRIGHTNESS_PATH, LedControl.LED_OFF);
    }


    private void saveUploadModel(String message) {
        SharedPreferences.Editor editor = getSharedPreferences("Cloud", MODE_PRIVATE).edit();
        editor.putInt("UploadMode", VariableInstance.getInstance().UploadMode);
        editor.putString("UploadModeMessage", message);
        editor.apply();
    }

    private void getUploadModel() {
        SharedPreferences sharedPreferences = getSharedPreferences("Cloud", MODE_PRIVATE);
        VariableInstance.getInstance().UploadMode = sharedPreferences.getInt("UploadMode", 0);
        String mssage = sharedPreferences.getString("UploadModeMessage", "1");

        if (VariableInstance.getInstance().UploadMode == 3 || VariableInstance.getInstance().UploadMode == 4) {
            VariableInstance.getInstance().uploadSelectIndexList = new ArrayList<>();
            VariableInstance.getInstance().uploadSelectIndexList.clear();
            String[] data = mssage.split(",");
            if (data != null && data.length > 0) {
                for (String datum : data) {
                    try {
                        VariableInstance.getInstance().uploadSelectIndexList.add(Integer.parseInt(datum));
                    } catch (Exception e) {

                    }
                }
            }


        }
    }

    private void saveUploadUploadSpeed(String uploadSpeed) {
        SharedPreferences.Editor editor = getSharedPreferences("Cloud", MODE_PRIVATE).edit();
        editor.putString("uploadSpeed", uploadSpeed);
        editor.apply();
    }

    private String getUploadploadSpeed() {
        SharedPreferences sharedPreferences = getSharedPreferences("Cloud", MODE_PRIVATE);
        String uploadSpeed = sharedPreferences.getString("uploadSpeed", "0");
        return uploadSpeed;
    }

    private boolean canCloseDevice() {
        if (remoteUploading || localDownling || !operationUtils.pictureIsThreadStop)
            return false;
        return true;
    }


    private static final int msg_reload_device_info = 2;
    private static final int msg_close_device = 3;
    private static final int msg_send_ShutDown = 4;

    private static class MyHandler extends Handler {
        private WeakReference<MainActivity> weakReference;

        MyHandler(MainActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = weakReference.get();
            if (activity == null || activity.mHandler == null) {
                return;
            }
            switch (msg.what) {

                case msg_reload_device_info:
                    activity.initAddress();
                    break;
                case msg_close_device:
                    if (activity.canCloseDevice()) {
                        activity.operationUtils.startUploadLocatThread();
                    } else {
                        activity.mHandler.removeMessages(msg_close_device);
                        activity.mHandler.sendEmptyMessageDelayed(msg_close_device, close_device_timeout);
                    }
                    break;
                case msg_send_ShutDown:
                    activity.sendShutDown = false;
                    Utils.closeAndroid();
                    break;
            }
        }
    }
}

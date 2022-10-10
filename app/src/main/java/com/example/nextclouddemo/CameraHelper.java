package com.example.nextclouddemo;

import android.annotation.SuppressLint;
import android.app.Activity;

import android.hardware.Camera;

import com.example.nextclouddemo.utils.Log;
import com.example.nextclouddemo.utils.Utils;

import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


public class CameraHelper {
    private static final String TAG = "MainActivitylog1";


    private Camera mCamera;
    private final Object cameraLock = new Object();
    private Camera.Parameters parameters;
    private Camera.CameraInfo currentInfo;
    private Camera.Size optimalSize;
    private String BASE_PATH;


    private int cameraId;
    private boolean isFocusing;
    private boolean isOpen;
    public boolean isPreview;
    CameraHelperListener cameraHelperListener;

    public CameraHelper(CameraHelperListener cameraHelperListener) {
        BASE_PATH = VariableInstance.getInstance().TFCardVideoDir;

        File basicDir = new File(BASE_PATH);
        if (!basicDir.exists())
            basicDir.mkdirs();

        this.cameraHelperListener = cameraHelperListener;
        cameraId = 0;
        isFocusing = false;
        isOpen = false;
        isPreview = false;
    }

    public boolean openCamera(SurfaceHolder surfaceHolder, Activity activity) {
        synchronized (cameraLock) {
            try {
                if (mCamera != null) {
                    closeCamera();
                }
                pictureCount = 1;

                int num = Camera.getNumberOfCameras();
                Log.e(TAG, "openCamera: num =" + num);
                if (num < 1) {
                    Log.w(TAG, "open camera failed,camera num:" + num);
                    return false;
                }
                mCamera = Camera.open(cameraId == 0 ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);
                if (mCamera == null) {
                    Log.w(TAG, "open camera failed.");
                    return false;
                }
                mCamera.setPreviewDisplay(surfaceHolder);
                isOpen = true;
                setCameraDisplayOrientation(activity);
                configParameters();
                return true;
            } catch (Exception e) {
                Log.e(TAG, "set camera exception:" + e.getMessage());
                closeCamera();
            }
        }
        return false;
    }

    public void closeCamera() {
        synchronized (cameraLock) {
            if (mCamera != null) {
                Log.d(TAG, "camera1closeCamera: ----------------");
                mCamera.setPreviewCallback(null);
                try {
                    mCamera.stopPreview();
                    mCamera.release();
                } catch (Exception e) {
                    Log.e(TAG, "closeCamera: Exception =" + e);
                } finally {
                    mCamera = null;
                    isOpen = false;
                }
            }
        }
    }

    public void onStartPreview(int width, int height) {
        if (isPreview) {
            return;
        }
        if (!isOpen) {
            return;
        }
        Log.d(TAG, "camera1onStartPreview: ----------------");
        try {
            parameters = mCamera.getParameters();
            List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
            optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes, mSupportedPreviewSizes, height, width);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            parameters.set("orientation", "portrait");
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains("continuous-video")) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            setParameters(parameters);
            mCamera.startPreview();
            handler.sendEmptyMessageDelayed(1, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        isPreview = true;
        autoFocus();
    }


    private void configParameters() {
        parameters = mCamera.getParameters();
        currentInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId == 0 ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT, currentInfo);
        setParameters(parameters);
    }

    private int cameraDisplayOrientation = 0;

    public int setCameraDisplayOrientation(Activity activity) {
        synchronized (cameraLock) {
            int cameraId = this.cameraId;
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Log.d(TAG, "setCameraDisplayOrientation : cameraId - " + cameraId + " , rotation : " + rotation + " , info.orientation : " + info.orientation);

            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
                default:
                    degrees = 0;
                    break;
            }
            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                cameraDisplayOrientation = (360 - result) % 360;
            } else {
                result = (info.orientation - degrees + 360) % 360;
                cameraDisplayOrientation = result;
            }
            Log.i(TAG, "rotate degress:" + result);
            try {
                mCamera.setDisplayOrientation(cameraDisplayOrientation);
            } catch (Exception e) {
                e.printStackTrace();
            }
            autoFocus();
            return result;
        }
    }

    public void autoFocus() {
        synchronized (cameraLock) {
            try {
                if (mCamera != null && isOpen && isPreview && !isFocusing) {
                    mCamera.cancelAutoFocus();
                    isFocusing = true;
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            Log.d(TAG, "onAutoFocus   : " + success);
                            isFocusing = false;
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "snoppa camera autofocus exceptiom:" + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    public void setParameters(Camera.Parameters parameters) {
        if (mCamera != null) {
            try {
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static Camera.Size getOptimalVideoSize(List<Camera.Size> supportedVideoSizes, List<Camera.Size> previewSizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        List<Camera.Size> videoSizes;
        if (supportedVideoSizes != null) {
            videoSizes = supportedVideoSizes;
        } else {
            videoSizes = previewSizes;
        }
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        for (Camera.Size size : videoSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : videoSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private void startTakePictureTimer() {

    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            takePicture();
        }
    };

    public void takePicture() {
        if (isPreview && mCamera != null) {
          try{
              mCamera.takePicture(null, null, new Camera.PictureCallback() {
                  @Override
                  public void onPictureTaken(byte[] data, Camera camera) {
                      new Thread(new Runnable() {
                          @Override
                          public void run() {
                              try {
                                  String fileName = Utils.getMMddHHmmString() + "-" + pictureCount + ".jpg";
                                  String filePaht = BASE_PATH + "/" + fileName;
                                  File file = new File(filePaht);
                                  FileOutputStream fos = new FileOutputStream(file);
                                  fos.write(data);
                                  fos.flush();
                                  fos.close();
                                  cameraHelperListener.addPicture(filePaht);
                                  pictureCount++;
                                  Log.d(TAG, "run: onPictureTaken pictureCount=" + pictureCount + ",fileName =" + fileName);
                                  if (pictureCount < 31) {
                                      handler.removeMessages(1);
                                      handler.sendEmptyMessageDelayed(1, 900);
                                  } else {
                                      handler.removeMessages(1);
                                      cameraHelperListener.finishPreview();
                                  }
                              } catch (IOException e) {
                                  handler.removeMessages(1);
                                  handler.sendEmptyMessageDelayed(1, 1000);
                              }
                          }
                      }).start();
                  }
              });
          }catch (Exception e){

          }
        }

    }


    public int pictureCount;


    public interface CameraHelperListener {
        void addPicture(String path);

        void finishPreview();
    }
}

package com.example.nextclouddemo.utils;

import com.example.nextclouddemo.LogcatHelper;
import com.example.nextclouddemo.VariableInstance;
import com.example.nextclouddemo.model.UploadFileModel;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RemoteOperationUtils {
    private static final String TAG = "MainActivitylog";

    private static int requestFailure = -1;
    private static int fileExist = 1;
    private static int noExist = 0;

    public OwnCloudClient mClient;
    private Thread pictureWorkThread;
    private Thread videoWorkThread;
    public boolean pictureIsThreadStop;
    public boolean videoIsThreadStop;

    private String todayMonthFileDir;
    private String todayVideoFileDir;

    private RemoteOperationListener remoteOperationListener;


    public volatile BlockingQueue<UploadFileModel> pictureFileListCache = new LinkedBlockingQueue<>(5000);
    public volatile BlockingQueue<String> videoFileListCache = new LinkedBlockingQueue<>(5000);

    public RemoteOperationUtils(RemoteOperationListener remoteOperationListener) {

        this.todayMonthFileDir = Utils.getyyyyMMString();
        this.todayVideoFileDir = Utils.getMMddString();

        this.remoteOperationListener = remoteOperationListener;
        this.pictureIsThreadStop = true;
        VariableInstance.getInstance().uploadNum = 0;

    }

    private static final String cameraDir = "CameraPicture";
    private static final String videoDir = "VideoPicture";
    private static final String logcatDir = "Locat";

    private String userNameDir;

    private String remoteLogcatDir;

    private String remoteCameraDir;
    private String remoteCameraTodayMonthDir;

    private String remoteVideoDir;
    private String remoteVideoTodayMonthDir;
    private String remoteVideoTodayDir;

    public boolean initRemoteDir(String userName) {
        userNameDir = FileUtils.PATH_SEPARATOR + userName + FileUtils.PATH_SEPARATOR;
        remoteLogcatDir = FileUtils.PATH_SEPARATOR + userName + FileUtils.PATH_SEPARATOR + logcatDir + FileUtils.PATH_SEPARATOR;

        remoteCameraDir = FileUtils.PATH_SEPARATOR + userName + FileUtils.PATH_SEPARATOR + cameraDir + FileUtils.PATH_SEPARATOR;
        remoteCameraTodayMonthDir = remoteCameraDir + todayMonthFileDir + FileUtils.PATH_SEPARATOR;

        remoteVideoDir = FileUtils.PATH_SEPARATOR + userName + FileUtils.PATH_SEPARATOR + videoDir + FileUtils.PATH_SEPARATOR;
        remoteVideoTodayMonthDir = remoteVideoDir + todayMonthFileDir + FileUtils.PATH_SEPARATOR;
        remoteVideoTodayDir = remoteVideoTodayMonthDir + todayVideoFileDir + FileUtils.PATH_SEPARATOR;


        int result = checkFileExit(FileUtils.PATH_SEPARATOR, userNameDir);
        if (result == requestFailure) {
            return false;
        }

        if (result == fileExist) {
            ReadFolderRemoteOperation refreshOperation = new ReadFolderRemoteOperation(userName);
            RemoteOperationResult remoteOperationResult = refreshOperation.execute(mClient);
            if (remoteOperationResult == null || !remoteOperationResult.isSuccess()) {
                Log.e(TAG, "initRemoteDir 获取文件列表失败: " + result);
                return false;
            }
            boolean exictP = false;
            boolean exictV = false;
            boolean exictL = false;
            for (Object obj : remoteOperationResult.getData()) {
                RemoteFile remoteFile = (RemoteFile) obj;
                if (remoteFile.getRemotePath().contains(cameraDir)) {
                    exictP = true;
                } else if (remoteFile.getRemotePath().contains(videoDir)) {
                    exictV = true;
                } else if (remoteFile.getRemotePath().contains(logcatDir)) {
                    exictL = true;
                }
            }

            boolean checkCameraPath = checkResult(exictP, remoteCameraDir, remoteCameraTodayMonthDir);
            if (!checkCameraPath)
                return false;

            boolean checkVideoPath = checkResult(exictV, remoteVideoDir, remoteVideoTodayMonthDir);

            createFilefolder(remoteVideoTodayDir);

            if (!checkVideoPath)
                return false;

            if (!exictL)
                createFilefolder(remoteLogcatDir);
        } else {
            boolean createResult = createFilefolder(userNameDir);
            if (!createResult)
                return false;
            createResult = createFilefolder(remoteCameraDir);
            if (!createResult)
                return false;
            createResult = createFilefolder(remoteCameraTodayMonthDir);
            if (!createResult)
                return false;
            createResult = createFilefolder(remoteVideoDir);
            if (!createResult)
                return false;
            createResult = createFilefolder(remoteVideoTodayMonthDir);
            if (!createResult)
                return false;
            createResult = createFilefolder(remoteVideoTodayDir);
            if (!createResult)
                return false;
        }
        return true;
    }


    private boolean checkResult(boolean exit, String dir1, String dir2) {
        if (exit) {
            int result = checkFileExit(dir1, dir2);
            if (result == requestFailure)
                return false;
            if (result == fileExist) {
                return true;
            } else {
                boolean createResult = createFilefolder(dir2);
                if (!createResult)
                    return false;
            }
        } else {
            boolean createResult = createFilefolder(dir1);
            if (!createResult)
                return false;
            createResult = createFilefolder(dir2);
            if (!createResult)
                return false;
        }
        return true;
    }


    public int checkFileExit(String remote, String dir) {
        Log.v(TAG, "checkFileExit: remote =" + remote + ",dir =" + dir);
        if (mClient == null)
            return requestFailure;
        ReadFolderRemoteOperation refreshOperation = new ReadFolderRemoteOperation(remote);
        RemoteOperationResult result = refreshOperation.execute(mClient);
        if (result == null || !result.isSuccess()) {
            Log.e(TAG, "checkFileExit 获取文件列表失败: " + result);
            return requestFailure;
        }
        boolean exict = false;
        for (Object obj : result.getData()) {
            RemoteFile remoteFile = (RemoteFile) obj;
            if (remoteFile.getRemotePath().equals(dir)) {
                exict = true;
                break;
            }
        }
        if (exict)
            return fileExist;
        else
            return noExist;
    }


    public boolean createFilefolder(String flieFolder) {
        Log.i(TAG, "createFilefolder: flieFolder =" + flieFolder);
        if (mClient == null)
            return false;
        CreateFolderRemoteOperation createOperation = new CreateFolderRemoteOperation(flieFolder, false);
        RemoteOperationResult result = createOperation.execute(mClient);
        if (result == null || !result.isSuccess()) {
            Log.e(TAG, "createImeiFilefolder 创建文件夹失败: " + result);
            return false;
        }
        return true;
    }


    private boolean uploadImageFileToRemote(UploadFileModel fileModel) {
        Log.e(TAG, "uploadImageFileToRemote: fileModel =" + fileModel);
        if (fileModel == null)
            return false;
        File file = new File(fileModel.localPath);
        if (file == null || !file.exists())
            return false;

        if (mClient == null) {
            if (!pictureFileListCache.contains(fileModel))
                pictureFileListCache.add(fileModel);
            return false;
        }

        long startUploadTime = System.currentTimeMillis();
        long fileSize = 0;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fileSize = fis.available();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        String remotePath = remoteCameraTodayMonthDir + file.getName();
        Long timeStampLong = file.lastModified() / 1000;
        String timeStamp = timeStampLong.toString();

        Log.e(TAG, "uploadImageFileToRemote: " + file.getAbsolutePath() + ",remotePath =" + remotePath + ",file.exit =" + file.exists());

        remoteOperationListener.pictureUploadStart();
        UploadFileRemoteOperation uploadOperation = new UploadFileRemoteOperation(file.getAbsolutePath(), remotePath, "image/png", timeStamp);
        RemoteOperationResult result = uploadOperation.execute(mClient);
        remoteOperationListener.pictureUploadEnd();
        if (result == null) {
            Log.e(TAG, "uploadFileToRemote: result == null ");
            if (!pictureFileListCache.contains(fileModel))
                pictureFileListCache.add(fileModel);
            return false;
        }
        boolean isSuccess = result.isSuccess();

        if (isSuccess) {
            VariableInstance.getInstance().uploadNum++;
            long totalTime = (System.currentTimeMillis() - startUploadTime) / 1000;
            Log.e(TAG, "uploadImageFileToRemote: " + totalTime + ",fileSize =" + fileSize);
            if (totalTime != 0) {
                remoteOperationListener.updateUploadSpeed(("" + (fileSize / totalTime / 1024)));
            }
            if (file.exists())
                file.delete();
        } else {
            if (!pictureFileListCache.contains(fileModel))
                pictureFileListCache.add(fileModel);
        }
        Log.d(TAG, "uploadFileToRemote: isSuccess =" + isSuccess);
        return isSuccess;
    }


    public void uploadVideo(File file) {
        Log.e(TAG, "uploadVideo:本地路径： file =" + file);
        if (file == null) {
            return;
        }
        Log.d(TAG, "uploadVideo: file.lenght =" + file.length());
        if (mClient == null)
            return;
        remoteOperationListener.videoUploadStart();
        String remotePath = remoteVideoTodayDir + file.getName();

        Long timeStampLong = file.lastModified() / 1000;
        String timeStamp = timeStampLong.toString();
        UploadFileRemoteOperation uploadOperation = new UploadFileRemoteOperation(file.getAbsolutePath(), remotePath, "image/png", timeStamp);
        RemoteOperationResult result = uploadOperation.execute(mClient);
        if (result == null || !result.isSuccess()) {
            remoteOperationListener.uploadVideoComplete(false);
        } else {
            remoteOperationListener.uploadVideoComplete(true);
            file.delete();
        }
    }


    public void addUploadRemoteFile(UploadFileModel uploadFileModel) {
        Log.e(TAG, "addUploadRemoteFile: uploadFileModel =" + uploadFileModel + ",fileListCache =" + pictureFileListCache.size());
        if (!pictureFileListCache.contains(uploadFileModel))
            pictureFileListCache.add(uploadFileModel);

        if (mClient != null)
            startUploadThread();
    }

    private long startUploadTime;

    public void addVideoFile(String path) {
        if (path == null)
            return;
        if (!videoFileListCache.contains(path))
            videoFileListCache.add(path);
        if (mClient == null) {
            return;
        }
        startVideoWorkThread();
    }

    public void startVideoWorkThread() {
        if (videoWorkThread != null)
            return;
        videoWorkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted() && !videoIsThreadStop) {
                    String fileModel = null;
                    try {
                        Log.e(TAG, "run: videoFileListCache.size =" + videoFileListCache.size());
                        fileModel = videoFileListCache.poll(3, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "startVideoWorkThread: e =" + e);
                    }

                    if (fileModel == null) {
                        boolean isVideoPreviewing = remoteOperationListener.isVideoPreviewing();
                        if (!isVideoPreviewing) {
                            File localTpmFlie = new File(VariableInstance.getInstance().TFCardVideoDir);
                            if (localTpmFlie != null && localTpmFlie.exists()) {
                                File[] files = localTpmFlie.listFiles();
                                if (files != null) {
                                    for (File file : files) {
                                        if (!videoFileListCache.contains(file.getAbsolutePath())) {
                                            videoFileListCache.add(file.getAbsolutePath());
                                        }
                                    }
                                }
                            }
                            if (videoFileListCache.size() == 0) {
                                stopUploadVideoThread();
                                return;
                            }
                        }
                    } else {
                        uploadVideo(new File(fileModel));
                    }
                }
            }
        });
        videoWorkThread.start();
    }


    public void startUploadThread() {
        Log.d(TAG, "startUploadThread: ");
        pictureIsThreadStop = false;
        if (pictureWorkThread != null)
            return;
        startUploadTime = System.currentTimeMillis();
        pictureWorkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted() && !pictureIsThreadStop) {
                    UploadFileModel fileModel = null;
                    try {
                        Log.e(TAG, "run: fileListCache.size =" + pictureFileListCache.size());
                        fileModel = pictureFileListCache.poll(20, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "startUploadThread: e =" + e);
                    }

                    if (fileModel == null) {
                        File localTpmFlie = new File(VariableInstance.getInstance().TFCardPictureDir);
                        if (localTpmFlie != null && localTpmFlie.exists()) {
                            File[] files = localTpmFlie.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    UploadFileModel uploadFileModel = new UploadFileModel(file.getAbsolutePath());
                                    Log.d(TAG, "run: uploadFileModel 上一次没传完的=" + uploadFileModel);
                                    if (!pictureFileListCache.contains(uploadFileModel)) {
                                        pictureFileListCache.add(uploadFileModel);
                                    }
                                }
                            }
                        }
                        if (pictureFileListCache.size() == 0 && !remoteOperationListener.isDownling()) {
                            remoteOperationListener.allFileUploadComplete(System.currentTimeMillis() - startUploadTime);
                            stopUploadThread();
                            return;
                        }
                    } else {
                        uploadImageFileToRemote(fileModel);
                    }
                }
            }
        });
        pictureWorkThread.start();
    }

    public void stopUploadVideoThread() {
        Log.e(TAG, "stopUploadVideoThread: ");
        if (videoWorkThread != null) {
            videoIsThreadStop = true;

            try {
                videoWorkThread.interrupt();
                videoWorkThread.join(100);
            } catch (Exception e) {
                Log.e(TAG, "stop: Exception = " + e);
                try {
                    videoWorkThread.interrupt();
                } catch (Exception e1) {
                    Log.e(TAG, "stop: Exception1 = " + e1);
                }
            }
        }
        videoWorkThread = null;
        videoFileListCache.clear();
    }

    public void stopUploadThread() {
        Log.e(TAG, "stopUploadThread: ");
        if (pictureWorkThread != null) {
            pictureIsThreadStop = true;
            remoteOperationListener.allFileUploadComplete(System.currentTimeMillis() - startUploadTime);
            try {
                pictureWorkThread.interrupt();
                pictureWorkThread.join(100);
            } catch (Exception e) {
                Log.e(TAG, "stop: Exception = " + e);
                try {
                    pictureWorkThread.interrupt();
                } catch (Exception e1) {
                    Log.e(TAG, "stop: Exception1 = " + e1);
                }
            }
        }
        pictureWorkThread = null;
        pictureFileListCache.clear();
    }

    public void startUploadLocatThread() {


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    LogcatHelper.getInstance().stop();
                    Thread.sleep(1000);
                    File logcatDir = new File(VariableInstance.getInstance().LogcatDir);
                    if (logcatDir != null && logcatDir.exists()) {
                        File[] files = logcatDir.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                String remotePath = remoteLogcatDir + file.getName();
                                Long timeStampLong = file.lastModified() / 1000;
                                String timeStamp = timeStampLong.toString();
                                UploadFileRemoteOperation uploadOperation = new UploadFileRemoteOperation(file.getAbsolutePath(), remotePath, "text/plain", timeStamp);
                                RemoteOperationResult result = uploadOperation.execute(mClient);

                                if (result.isSuccess())
                                    file.delete();
                            }
                        }
                    }
                } catch (Exception e) {

                }
                Log.e(TAG, "run: uploadLogcatComplete");
                remoteOperationListener.uploadLogcatComplete();
            }
        }).start();

    }

    public interface RemoteOperationListener {
        void allFileUploadComplete(long totalTime);

        void pictureUploadStart();

        void pictureUploadEnd();

        void videoUploadStart();

        void updateUploadSpeed(String speed);

        void uploadVideoComplete(boolean succeed);

        void uploadLogcatComplete();

        boolean isDownling();

        boolean isVideoPreviewing();
    }
}

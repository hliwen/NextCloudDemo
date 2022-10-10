package com.example.nextclouddemo.utils;

import android.net.Uri;

import com.example.nextclouddemo.model.DeviceInfoModel;
import com.example.nextclouddemo.model.ServerUrlModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Communication {

    private static final String TAG = "MainActivitylog";

    public ServerUrlModel getServerUrl() {
        ServerUrlModel serverUrlModel = new ServerUrlModel();

        URL url = null;
        try {
            url = new URL(UrlUtils.serverUri);
            HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
            int ResponseCode = urlcon.getResponseCode();
            serverUrlModel.responseCode = ResponseCode;
            if (ResponseCode != 200)
                return serverUrlModel;

            InputStream inputStream = urlcon.getInputStream();
            InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader reader = new BufferedReader(isr);
            String line;
            StringBuffer buffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            String content = buffer.toString();
            Log.d(TAG, "run:  nccontent = " + content);
            JSONObject jsonObject = new JSONObject(content);
            JSONArray jsonArray = new JSONArray(jsonObject.getString("data"));
            jsonObject = new JSONObject(jsonArray.getString(0));
            Uri serverUri = Uri.parse(jsonObject.getString("url"));
            serverUrlModel.serverUri = serverUri;
        } catch (Exception e) {
            Log.e(TAG, "getServerUrl: e =" + e);
        }
        return serverUrlModel;
    }

    public DeviceInfoModel getDeviceInfo(String PhoneImei) {
        DeviceInfoModel deviceInfoModel = new DeviceInfoModel();
        try {
            URL url = new URL(UrlUtils.deviceInfoPrefix + PhoneImei + UrlUtils.deviceInfoSuffix);
            HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
            int ResponseCode = urlcon.getResponseCode();
            deviceInfoModel.responseCode = ResponseCode;
            if (ResponseCode != 200) {
                return deviceInfoModel;
            }
            InputStream inputStream = urlcon.getInputStream();
            InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader reader = new BufferedReader(isr);

            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {//如果还没有读完
                buffer.append(line);//一直追加内容
            }
            String content = buffer.toString();
            Log.d(TAG, "run:getAccout content = " + content);
            JSONObject jsonObject = new JSONObject(content);
            JSONArray jsonArray = new JSONArray(jsonObject.getString("data"));
            jsonObject = new JSONObject(jsonArray.getString(0));

            JSONObject deviceObject = new JSONObject(jsonObject.getString("device"));

            deviceInfoModel.deveceName = deviceObject.getString("name");
            jsonArray = jsonObject.getJSONArray("deviceAttrList");
            jsonObject = new JSONObject(jsonArray.getString(1));
            deviceInfoModel.username = jsonObject.getString("attrVal");
            jsonObject = new JSONObject(jsonArray.getString(2));
            deviceInfoModel.password = jsonObject.getString("attrVal");
            deviceInfoModel.returnImei = jsonObject.getString("imei");
        } catch (Exception e) {
            Log.e(TAG, "getDeviceInfo: e =" + e);
        }
        return deviceInfoModel;
    }



}

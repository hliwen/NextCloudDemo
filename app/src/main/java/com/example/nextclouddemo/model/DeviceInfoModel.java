package com.example.nextclouddemo.model;

public class DeviceInfoModel {
    public String returnImei;
    public int responseCode;
    public String deveceName;
    public String username;
    public String password;
    @Override
    public String toString() {
        return "MainActivitylog DeviceInfoModel{" +
                "returnImei='" + returnImei + '\'' +
                ", responseCode=" + responseCode +
                ", userName='" + deveceName + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }


}

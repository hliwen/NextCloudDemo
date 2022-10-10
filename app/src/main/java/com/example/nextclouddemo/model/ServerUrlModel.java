package com.example.nextclouddemo.model;

import android.net.Uri;

public class ServerUrlModel {
    public int responseCode;

    @Override
    public String toString() {
        return "MainActivitylog ServerUrlModel{" +
                "responseCode=" + responseCode +
                ", serverUri=" + serverUri +
                '}';
    }

    public Uri serverUri;


}

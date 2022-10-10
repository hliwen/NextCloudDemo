package com.example.nextclouddemo.model;

import java.util.Objects;

public class UploadFileModel {
    public String localPath;


    public UploadFileModel( String localPath) {

        this.localPath = localPath;
    }


    @Override
    public String toString() {
        return "MainActivitylog UploadFileModel{" +
                "localPath='" + localPath + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UploadFileModel)) return false;
        UploadFileModel that = (UploadFileModel) o;
        return Objects.equals(localPath, that.localPath);
    }
}

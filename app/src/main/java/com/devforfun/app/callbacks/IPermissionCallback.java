package com.devforfun.app.callbacks;

public interface IPermissionCallback {

    public void storagePermissionGranted();

    public void storageReadPermissionGranted();

    public void storageWritePermissionGranted();

    public void cameraPermissionGranted();

}

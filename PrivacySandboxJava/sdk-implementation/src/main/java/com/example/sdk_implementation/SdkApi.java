package com.example.sdk_implementation;

import android.os.RemoteException;

import com.example.myaidllibrary.ISdkApi;

public class SdkApi extends ISdkApi.Stub {
    @Override
    public String sayHello(String message) throws RemoteException {
        return "Ack!";
    }
}

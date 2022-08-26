package com.example.sdkimplementation

import android.os.RemoteException
import com.example.exampleaidllibrary.ISdkApi

class SdkApi : ISdkApi.Stub() {
    @Throws(RemoteException::class)
    override fun sayHello(name: String): String {
        return "Hello, $name!"
    }
}
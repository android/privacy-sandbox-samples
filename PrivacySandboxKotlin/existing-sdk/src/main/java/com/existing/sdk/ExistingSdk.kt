package com.existing.sdk

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.privacysandbox.activity.client.createSdkActivityLauncher
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import com.example.api.SdkService
import com.example.api.SdkServiceFactory

class ExistingSdk(private val context: Context) {

    /** Initialize the SDK. If the SDK failed to initialize, return false, else true. */
    suspend fun initialize(): Boolean {
        // You can also have a fallback mechanism here, where if the SDK cannot be loaded in the SDK
        // runtime, initialize as you usually would.
        return loadSdkIfNeeded(context) != null
    }

    suspend fun createFile(size: Int): String? {
        if (!isSdkLoaded()) return null
        return loadSdkIfNeeded(context)?.createFile(size)
    }

    suspend fun showInterstitialAd(baseActivity: AppCompatActivity): Boolean {
        if (!isSdkLoaded()) return false
        val launcher = baseActivity.createSdkActivityLauncher { true }
        loadSdkIfNeeded(context)?.getInterstitial(launcher)
        return true
    }

    /** Keeps a reference to a sandboxed SDK and makes sure it's only loaded once. */
    internal companion object Loader {

        private const val TAG = "ExistingSdk"

        /**
         * Name of the SDK to be loaded.
         *
         * (needs to be the one defined in example-sdk-bundle/build.gradle)
         */
        private const val SDK_NAME = "com.example.sdk"

        private var remoteInstance: SdkService? = null

        suspend fun loadSdkIfNeeded(context: Context): SdkService? {
            try {
                // First we need to check if the SDK is already loaded. If it is we just return it.
                // The sandbox manager will throw an exception if we try to load an SDK that is
                // already loaded.
                if (remoteInstance != null) return remoteInstance

                // An [SdkSandboxManagerCompat], used to communicate with the sandbox and load SDKs.
                val sandboxManagerCompat = SdkSandboxManagerCompat.from(context)

                val sandboxedSdk = sandboxManagerCompat.loadSdk(SDK_NAME, Bundle.EMPTY)
                remoteInstance = SdkServiceFactory.wrapToSdkService(sandboxedSdk.getInterface()!!)
                return remoteInstance
            } catch (e: LoadSdkCompatException) {
                Log.e(TAG, "Failed to load SDK, error code: ${e.loadSdkErrorCode}", e)
                return null
            }
        }

        fun isSdkLoaded(): Boolean {
            return remoteInstance != null
        }
    }
}

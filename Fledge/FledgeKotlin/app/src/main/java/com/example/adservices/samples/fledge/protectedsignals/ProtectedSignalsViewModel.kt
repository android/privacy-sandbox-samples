package com.example.adservices.samples.fledge.protectedsignals

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.signals.ProtectedSignalsManager
import androidx.privacysandbox.ads.adservices.signals.UpdateSignalsRequest
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.TAG
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.appContext
import java.util.function.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFeatures.Ext12OptIn::class)
class ProtectedSignalsViewModel : ViewModel() {

  fun updateSignals(urlInput: String, statusReceiver: Consumer<String>) {
    viewModelScope.launch {
      try {
        val uri = Uri.parse(urlInput)
        statusReceiver.accept("Attempting signal update with URL $uri")

        val updateSignalsRequest = UpdateSignalsRequest(uri)

        withContext(Dispatchers.IO) {
          ProtectedSignalsManager.obtain(appContext)!!.updateSignals(updateSignalsRequest)
        }

        statusReceiver.accept("Signal update with URL $uri succeeded!")
      } catch (e: Exception) {
        statusReceiver.accept("Signal update failed with error: $e")
        Log.e(TAG, "Signal update failed with error: $e")
      }
    }
  }
}
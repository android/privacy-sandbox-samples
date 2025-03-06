package com.example.adservices.samples.fledge.util

import android.net.Uri
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import com.example.adservices.samples.fledge.util.gson.AdTechIdentifierConverter
import com.example.adservices.samples.fledge.util.gson.UriConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder

object CommonConstants {
  val gson: Gson = GsonBuilder()
    .registerTypeAdapter(AdTechIdentifier::class.java, AdTechIdentifierConverter())
    .registerTypeAdapter(Uri::class.java, UriConverter())
    .create()

  val emptyAdSelectionSignals = AdSelectionSignals("{}")
}
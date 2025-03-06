package com.example.adservices.samples.fledge.adselection.config

import android.net.Uri

data class OnDeviceAdSelectionJsonConfig(
  val baseUri: Uri?,
  val trustedScoringSignalsUri: Uri?,
  val decisionLogicUri: Uri?
)
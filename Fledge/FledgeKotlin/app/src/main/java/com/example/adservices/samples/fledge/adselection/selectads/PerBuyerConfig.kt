package com.example.adservices.samples.fledge.adselection.selectads

data class PerBuyerConfig(
  val buyerSignals: String,
  val buyerKvExperimentGroupId: Int?,
  val generateBidCodeVersion: Int?,
  val buyerDebugId: String,
)

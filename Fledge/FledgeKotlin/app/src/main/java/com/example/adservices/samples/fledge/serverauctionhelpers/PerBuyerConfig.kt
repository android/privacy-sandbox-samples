package com.example.adservices.samples.fledge.serverauctionhelpers

data class PerBuyerConfig(
    val buyerSignals: String,
    val buyerKvExperimentGroupId: Int?,
    val generateBidCodeVersion: Int?,
    val buyerDebugId: String
)

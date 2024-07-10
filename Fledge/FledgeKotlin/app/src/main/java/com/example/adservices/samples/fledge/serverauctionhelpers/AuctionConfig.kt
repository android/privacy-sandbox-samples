package com.example.adservices.samples.fledge.serverauctionhelpers

data class AuctionConfig(
        val sellerSignals: String,
        val auctionSignals: String,
        val buyerList: List<String>,
        val seller: String,
        val perBuyerConfig: Map<String?, PerBuyerConfig?>,
        val sellerDebugId: String,
        val buyerTimeoutMs: Int)

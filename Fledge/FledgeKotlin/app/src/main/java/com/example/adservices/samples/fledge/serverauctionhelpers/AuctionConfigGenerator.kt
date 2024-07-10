package com.example.adservices.samples.fledge.serverauctionhelpers

import java.util.Collections


object AuctionConfigGenerator {
    private const val HTTPS_SCHEME = "https"
    private const val SELLER_SIGNALS = "[[72]]"
    private const val AUCTION_SIGNALS = "{\"maxFloorCpmUsdMicros\": 6250}"
    private const val BUYER_SIGNALS = "[[42]]"
    private const val BUYER_DEBUG_ID = "buyer_123"
    private const val SELLER_DEBUG_ID = "seller_123"
    private const val BUYER_TIMEOUT_MS = 60000
    fun getAuctionConfig(seller: String, buyer: String): AuctionConfig {
        val perBuyerConfig = PerBuyerConfig(
            BUYER_SIGNALS,
            null,
            null,
            BUYER_DEBUG_ID)
        return AuctionConfig(
            SELLER_SIGNALS,
            AUCTION_SIGNALS,
            listOf(buyer),
            HTTPS_SCHEME + "://" + seller,
            Collections.singletonMap(buyer, perBuyerConfig),
            SELLER_DEBUG_ID,
            BUYER_TIMEOUT_MS)
    }
}

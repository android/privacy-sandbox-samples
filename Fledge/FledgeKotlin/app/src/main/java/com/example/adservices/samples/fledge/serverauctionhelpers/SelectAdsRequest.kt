package com.example.adservices.samples.fledge.serverauctionhelpers

/**
 * The request sent to a Bidding and Auction server.
 */
data class SelectAdsRequest(
        val protectedAudienceCiphertext: String,
        val auctionConfig: AuctionConfig,
        val clientType: String
)

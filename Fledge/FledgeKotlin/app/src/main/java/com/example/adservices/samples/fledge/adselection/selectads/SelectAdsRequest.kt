package com.example.adservices.samples.fledge.adselection.selectads

/**
 * The request sent to a Bidding and Auction server.
 */
data class SelectAdsRequest(
  val protectedAudienceCiphertext: String,
  val auctionConfig: ServerAuctionConfig,
  val clientType: String,
)

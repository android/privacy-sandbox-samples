package com.example.adservices.samples.fledge.serverauctionhelpers

/**
 * A simple SelectAdResponse POJO used to convert to and from json that will be returned as a
 * response by HTTPS POST SelectAd call.
 *
 *
 * https://github.com/privacysandbox/fledge-docs/blob/main/bidding_auction_services_api.md#sellerfrontend-service-and-api-endpoints
 */
class SelectAdsResponse {
    var auctionResultCiphertext: String? = null
}
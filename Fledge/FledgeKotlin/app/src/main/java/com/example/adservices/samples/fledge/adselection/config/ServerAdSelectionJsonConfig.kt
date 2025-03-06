package com.example.adservices.samples.fledge.adselection.config

import android.net.Uri
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import com.example.adservices.samples.fledge.adselection.selectads.PerBuyerConfig
import com.example.adservices.samples.fledge.adselection.selectads.ServerAuctionConfig

/** Contains all the required parameters to run an ad selection on the auction server. */
data class ServerAdSelectionJsonConfig(
  val buyer: AdTechIdentifier,
  val seller: AdTechIdentifier,
  val sellerSfeUri: Uri,
  var coordinatorOriginUri: Uri
) {
  fun getServerAuctionConfig(): ServerAuctionConfig {
    val perBuyerConfig = PerBuyerConfig(
      buyerSignals = BUYER_SIGNALS,
      buyerKvExperimentGroupId = null,
      generateBidCodeVersion = null,
      buyerDebugId = BUYER_DEBUG_ID
    )
    return ServerAuctionConfig(
      sellerSignals = SELLER_SIGNALS,
      auctionSignals = AUCTION_SIGNALS,
      buyerList = listOf(buyer.toString()),
      seller = "$HTTPS_SCHEME://$seller",
      perBuyerConfig = mapOf(buyer.toString() to perBuyerConfig),
      sellerDebugId = SELLER_DEBUG_ID,
      buyerTimeoutMs = BUYER_TIMEOUT_MS
    )
  }

  private companion object {
    const val HTTPS_SCHEME = "https"
    const val SELLER_SIGNALS = "[[72]]"
    const val AUCTION_SIGNALS = "{\"maxFloorCpmUsdMicros\": 6250}"
    const val BUYER_SIGNALS = "[[42]]"
    const val BUYER_DEBUG_ID = "buyer_123"
    const val SELLER_DEBUG_ID = "seller_123"
    const val BUYER_TIMEOUT_MS = 60000
  }
}
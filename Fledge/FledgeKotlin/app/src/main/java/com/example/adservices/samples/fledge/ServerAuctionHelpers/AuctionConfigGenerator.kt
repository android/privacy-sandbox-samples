/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.adservices.samples.fledge.ServerAuctionHelpers

object AuctionConfigGenerator {
  private const val HTTPS_SCHEME = "https"
  private const val SELLER_SIGNALS = "[[72]]"
  private const val AUCTION_SIGNALS = "{\"someFooAuctionSignal\": 42}"
  private const val BUYER_SIGNALS = "[[42]]"
  private const val BUYER_DEBUG_ID = "buyer_123"
  private const val SELLER_DEBUG_ID = "seller_123"
  private const val BUYER_TIMEOUT_MS = 60000

  fun getAuctionConfig(seller: String, buyer: String): AuctionConfig {
    val perBuyerConfig = PerBuyerConfig(
      buyerSignals = BUYER_SIGNALS,
      buyerDebugId = BUYER_DEBUG_ID
    )

    return AuctionConfig(
      sellerSignals = SELLER_SIGNALS,
      auctionSignals = AUCTION_SIGNALS,
      buyerList = listOf(buyer) as MutableList<String>,
      seller = "$HTTPS_SCHEME://$seller",
      perBuyerConfig = mapOf(buyer to perBuyerConfig) as MutableMap<String, PerBuyerConfig>,
      sellerDebugId = SELLER_DEBUG_ID,
      buyerTimeoutMs = BUYER_TIMEOUT_MS
    )
  }
}

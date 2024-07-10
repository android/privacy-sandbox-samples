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
package com.example.adservices.samples.fledge.ServerAuctionHelpers;

import java.util.Collections;

public class AuctionConfigGenerator {
  private static final String HTTPS_SCHEME = "https";
  private static final String SELLER_SIGNALS = "[[72]]";
  private static final String AUCTION_SIGNALS = "{\"maxFloorCpmUsdMicros\": 6250}";
  private static final String BUYER_SIGNALS = "[[42]]";
  private static final String BUYER_DEBUG_ID = "buyer_123";
  private static final String SELLER_DEBUG_ID = "seller_123";
  private static final int BUYER_TIMEOUT_MS = 60000;

  public static AuctionConfig getAuctionConfig(String seller, String buyer) {
    PerBuyerConfig perBuyerConfig =
        PerBuyerConfig.builder()
            .setBuyerSignals(BUYER_SIGNALS)
            .setBuyerDebugId(BUYER_DEBUG_ID)
            .build();

    return AuctionConfig.builder()
        .setSellerSignals(SELLER_SIGNALS)
        .setAuctionSignals(AUCTION_SIGNALS)
        .setBuyerList(Collections.singletonList(buyer))
        .setSeller(HTTPS_SCHEME + "://" + seller)
        .setPerBuyerConfig(Collections.singletonMap(buyer, perBuyerConfig))
        .setSellerDebugId(SELLER_DEBUG_ID)
        .setBuyerTimeoutMs(BUYER_TIMEOUT_MS)
        .build();
  }
}

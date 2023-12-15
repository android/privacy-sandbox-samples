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
package com.example.adservices.samples.signals.sampleapp.ServerAuctionHelpers;

import com.google.auto.value.AutoValue;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class AuctionConfig {
  public abstract String sellerSignals();
  public abstract String auctionSignals();
  public abstract List<String> buyerList();
  public abstract String seller();
  public abstract Map<String, PerBuyerConfig> perBuyerConfig();
  public abstract String sellerDebugId();
  public abstract int buyerTimeoutMs();

  public static Builder builder() {
    return new AutoValue_AuctionConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setSellerSignals(String value);
    public abstract Builder setAuctionSignals(String value);
    public abstract Builder setBuyerList(List<String> value);
    public abstract Builder setSeller(String value);
    public abstract Builder setPerBuyerConfig(Map<String, PerBuyerConfig> value);
    public abstract Builder setSellerDebugId(String value);
    public abstract Builder setBuyerTimeoutMs(int value);
    public abstract AuctionConfig build();
  }
}

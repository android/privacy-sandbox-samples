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

import com.google.auto.value.AutoValue;

/**
 * The request sent to a Bidding and Auction server.
 */
@AutoValue
public abstract class SelectAdsRequest {
  public abstract String getProtectedAudienceCiphertext();
  public abstract AuctionConfig getAuctionConfig();
  public abstract String getClientType();
  public static Builder builder() {
    return new AutoValue_SelectAdsRequest.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setAuctionConfig(AuctionConfig value);
    public abstract Builder setClientType(String value);
    public abstract Builder setProtectedAudienceCiphertext(String value);
    public abstract SelectAdsRequest build();
  }
}

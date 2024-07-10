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

import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PerBuyerConfig {
  public abstract String getBuyerSignals();
  @Nullable public abstract Integer getBuyerKvExperimentGroupId();
  @Nullable public abstract Integer getGenerateBidCodeVersion();
  public abstract String getBuyerDebugId();

  public static Builder builder() {
    return new AutoValue_PerBuyerConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setBuyerSignals(String value);
    public abstract Builder setBuyerKvExperimentGroupId(@Nullable Integer value);
    public abstract Builder setGenerateBidCodeVersion(@Nullable Integer value);
    public abstract Builder setBuyerDebugId(String value);
    public abstract PerBuyerConfig build();
  }
}

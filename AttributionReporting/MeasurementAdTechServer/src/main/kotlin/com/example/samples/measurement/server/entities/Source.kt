/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.example.samples.measurement.server.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Class for Source
 */
data class Source(
  @JsonProperty("ad_id")
  @get:JsonProperty("ad_id")
  val adId: String,

  @JsonProperty("event_response_headers")
  @get:JsonProperty("event_response_headers")
  val eventResponseHeaders: SourceResponseHeaders,

  @JsonProperty("navigation_response_headers")
  @get:JsonProperty("navigation_response_headers")
  val navigationResponseHeaders: SourceResponseHeaders,
)

data class SourceResponseHeaders(
  @JsonProperty(regKey)
  @get:JsonProperty(regKey)
  val registrationHeader: SourceRegistrationHeader,

  @JsonProperty(redirectKey)
  @get:JsonProperty(redirectKey)
  val attributionReportingRedirect: List<String>?,
) {
  companion object {
    const val regKey = "Attribution-Reporting-Register-Source"
    const val redirectKey = "Attribution-Reporting-Redirect"
  }
}

data class SourceRegistrationHeader(
  @JsonProperty("source_event_id")
  @get:JsonProperty("source_event_id")
  val sourceEventId: String,

  @JsonProperty("destination")
  @get:JsonProperty("destination")
  val destination: String,

  @JsonProperty("expiry")
  @get:JsonProperty("expiry")
  val expiry: String,

  @JsonProperty("priority")
  @get:JsonProperty("priority")
  val priority: String,

  @JsonProperty("filter_data")
  @get:JsonProperty("filter_data")
  val filterData: Map<String, List<String>>?,

  @JsonProperty("aggregation_keys")
  @get:JsonProperty("aggregation_keys")
  val aggregationKeys: Map<String, String>?,

  @JsonProperty("debug_key")
  @get:JsonProperty("debug_key")
  val debugKey: String?,
)
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
import com.fasterxml.jackson.databind.JsonNode

/**
 * Class for Trigger
 */
data class Trigger(
  @JsonProperty("conv_id")
  val convId: String,

  @JsonProperty("response_headers")
  val responseHeader: TriggerResponseHeader,
)

data class TriggerResponseHeader(
  @JsonProperty(regKey)
  @get:JsonProperty(regKey)
  val registrationHeader: List<TriggerRegistrationHeader>,

  @JsonProperty(aggDataRegKey)
  @get:JsonProperty(aggDataRegKey)
  val aggregatableDataRegistrationHeader: List<AggregatableTriggerData>?,

  @JsonProperty(aggValuesRegKey)
  @get:JsonProperty(aggValuesRegKey)
  val aggregatableValuesRegistrationHeader: JsonNode?,

  @JsonProperty(redirectKey)
  @get:JsonProperty(redirectKey)
  val attributionReportingRedirect: List<String>?,
) {
  companion object {
    const val regKey = "Attribution-Reporting-Register-Event-Trigger"
    const val aggDataRegKey = "Attribution-Reporting-Register-Aggregatable-Trigger-Data"
    const val aggValuesRegKey = "Attribution-Reporting-Register-Aggregatable-Values"
    const val redirectKey = "Attribution-Reporting-Redirect"
  }
}

data class TriggerRegistrationHeader(
  @JsonProperty("trigger_data")
  @get:JsonProperty("trigger_data")
  val triggerData: String,

  @JsonProperty("priority")
  @get:JsonProperty("priority")
  val priority: String,

  @JsonProperty("deduplication_key")
  @get:JsonProperty("deduplication_key")
  val deduplicationKey: String?,
)

data class AggregatableTriggerData(
  @JsonProperty("key_piece")
  @get:JsonProperty("key_piece")
  val keyPiece: String,

  @JsonProperty("source_keys")
  @get:JsonProperty("source_keys")
  val sourceKeys: List<String>,

  @JsonProperty("filters")
  @get:JsonProperty("filters")
  val filters: Map<String,List<String>>?,

  @JsonProperty("not_filters")
  @get:JsonProperty("not_filters")
  val notFilters: Map<String,List<String>>?,
)

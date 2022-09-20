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
 * Class for Aggregate Report.
 */
data class AggregateReport(
  @JsonProperty("shared_info")
  @get:JsonProperty("shared_info")
  val sharedInfo: String,

  @JsonProperty("aggregation_service_payloads")
  @get:JsonProperty("aggregation_service_payloads")
  val aggregationServicePayloads: List<AggregationServicePayload>,
)

data class AggregationServicePayload(
  @JsonProperty("payload")
  @get:JsonProperty("payload")
  val payload: String?,

  @JsonProperty("key_id")
  @get:JsonProperty("key_id")
  val keyId: String?,

  @JsonProperty("debug_cleartext_payload")
  @get:JsonProperty("debug_cleartext_payload")
  val debugCleartextPayload: String?,


  @JsonProperty("source_debug_key")
  @get:JsonProperty("source_debug_key")
  val sourceDebugKey: String?,


  @JsonProperty("trigger_debug_key")
  @get:JsonProperty("trigger_debug_key")
  val triggerDebugKey: String?,
)

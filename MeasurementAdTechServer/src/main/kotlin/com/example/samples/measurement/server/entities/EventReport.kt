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
 * Class for Event Report.
 */
data class EventReport(
  @JsonProperty("attribution_destination")
  @get:JsonProperty("attribution_destination")
  val attributionDestination: String,

  @JsonProperty("source_event_id")
  @get:JsonProperty("source_event_id")
  val sourceEventId: String,

  @JsonProperty("trigger_data")
  @get:JsonProperty("trigger_data")
  val triggerData: String,

  @JsonProperty("report_id")
  @get:JsonProperty("report_id")
  val reportId: String,

  @JsonProperty("source_type")
  @get:JsonProperty("source_type")
  val sourceType: String,

  @JsonProperty("randomized_trigger_rate")
  @get:JsonProperty("randomized_trigger_rate")
  val randomizedTriggerRate: Float,
)

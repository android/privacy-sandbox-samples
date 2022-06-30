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
package com.example.samples.measurement.server

import com.example.samples.measurement.server.entities.AggregateReport
import com.example.samples.measurement.server.entities.EventReport
import com.example.samples.measurement.server.entities.Source
import com.example.samples.measurement.server.entities.SourceResponseHeaders
import com.example.samples.measurement.server.entities.Trigger
import com.example.samples.measurement.server.entities.TriggerResponseHeader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.CollectionType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.filter.CommonsRequestLoggingFilter

typealias SourceRepo = InMemoryRepo<Source, String>
typealias TriggerRepo = InMemoryRepo<Trigger, String>
typealias EventReportRepo = InMemoryRepo<EventReport, String>
typealias AggregateReportRepo = InMemoryRepo<AggregateReport, String>

/**
 * Main application for handling initialization and running the server.
 */
@SpringBootApplication
class Application {

  @Bean
  fun getRequestFilter(): CommonsRequestLoggingFilter {
    val loggingFilter = CommonsRequestLoggingFilter()
    loggingFilter.setIncludeQueryString(true)
    loggingFilter.setIncludePayload(true)
    loggingFilter.setIncludeHeaders(true)
    return loggingFilter
  }

  /**
   * Repo for storing sources.
   */
  @Bean
  fun createSourceRepo(): SourceRepo {
    return InMemoryRepo()
  }

  /**
   * Repo for storing Triggers.
   */
  @Bean
  fun createTriggerRepo(): TriggerRepo {
    return InMemoryRepo()
  }

  /**
   * Repo for storing EventReport.
   */
  @Bean
  fun createEventReportRepo(): EventReportRepo {
    return InMemoryRepo()
  }

  /**
   * Repo for storing AggregateReport.
   */
  @Bean
  fun createAggregateReportRepo(): AggregateReportRepo {
    return InMemoryRepo()
  }

  /**
   * Function for reading a file and returning list of objects
   * @param filePath path of the file to read
   * @param clas class for the objects to be read
   */
  fun <T : Any> readFileData(filePath: String, clas: Class<T>): List<T> {
    val stream = ClassPathResource(filePath).inputStream
    val mapper = ObjectMapper()
    val outer: CollectionType = mapper.typeFactory.constructCollectionType(
      ArrayList::class.java, clas)
    return mapper.readValue(stream, outer)
  }

  @Bean
  fun init(
    @Autowired
    sourceRepo: SourceRepo,
    @Autowired
    triggerRepo: TriggerRepo,
  ) =
    CommandLineRunner {
      val sources = readFileData("data/sources.json", Source::class.java)
      sources.forEach {
        sourceRepo.save(it.adId, it)
      }

      val triggers = readFileData("data/triggers.json", Trigger::class.java)
      triggers.forEach {
        triggerRepo.save(it.convId, it)
      }
    }
}

fun main(args: Array<String>) {
  runApplication<Application>(*args)
}

@RestController
class MainController(
  @Autowired
  private val sourceRepo: SourceRepo,
  @Autowired
  private val triggerRepo: TriggerRepo,
  @Autowired
  private val eventReportRepo: EventReportRepo,
  @Autowired
  private val aggregateReportRepo: AggregateReportRepo,
) {
  /**
   * Retrieve Source registration data.
   * Endpoint: /source
   * Request query param: ad_id
   * Request header param: Attribution-Reporting-Source-Info"
   */
  @PostMapping(value = ["/source"], params = ["ad_id"])
  fun requestSource(
    @RequestParam("ad_id") id: String,
    @RequestHeader("Attribution-Reporting-Source-Info") type: String,
  ): ResponseEntity<Void> {
    val data = sourceRepo.findById(id)
    if (data.isEmpty) {
      return ResponseEntity.notFound().build()
    }
    val responseHeader = if (type == "navigation")
                            data.get().navigationResponseHeaders
                            else data.get().eventResponseHeaders

    val headers = HttpHeaders()
    headers.add(SourceResponseHeaders.regKey,
                ObjectMapper().writeValueAsString(responseHeader.registrationHeader))
    if (responseHeader.aggregatableRegistrationHeader != null) {
      headers.add(SourceResponseHeaders.aggRegKey,
                  ObjectMapper().writeValueAsString(responseHeader.aggregatableRegistrationHeader))
    }
    responseHeader.attributionReportingRedirect?.forEach { redirect ->
      headers.add(SourceResponseHeaders.redirectKey, redirect)
    }
    return ResponseEntity.ok()
      .headers(headers)
      .build()
  }

  /**
   * Retrieve Trigger registration data.
   * Endpoint: /trigger
   * Request query Param: conv_id
   */
  @PostMapping(value = ["/trigger"], params = ["conv_id"])
  fun requestTrigger(@RequestParam("conv_id") id: String): ResponseEntity<Void> {
    val data = triggerRepo.findById(id)
    if (data.isEmpty) {
      return ResponseEntity.notFound().build()
    }
    val trigger = data.get()
    val headers = HttpHeaders()
    headers.add(TriggerResponseHeader.regKey,
                ObjectMapper().writeValueAsString(trigger.responseHeader.registrationHeader))
    if (trigger.responseHeader.aggregatableDataRegistrationHeader != null) {
      headers.add(TriggerResponseHeader.aggDataRegKey,
                  ObjectMapper().writeValueAsString(
                              trigger.responseHeader.aggregatableDataRegistrationHeader))
    }
    if (trigger.responseHeader.aggregatableValuesRegistrationHeader != null) {
      headers.add(TriggerResponseHeader.aggValuesRegKey,
                  ObjectMapper().writeValueAsString(
                              trigger.responseHeader.aggregatableValuesRegistrationHeader))
    }
    trigger.responseHeader.attributionReportingRedirect?.forEach { redirect ->
      headers.add(TriggerResponseHeader.redirectKey, redirect)
    }
    return ResponseEntity.ok()
      .headers(headers)
      .build()
  }

  /**
   * Save new event reports.
   * Reports are saved by report_id. Old report will be overwritten if same report_id is used.
   * Endpoint: /.well-known/attribution-reporting/report-attribution
   * Request Body: EventReport
   */
  @PostMapping(value = ["/.well-known/attribution-reporting/report-attribution"])
  fun saveEventReport(@RequestBody eventReport: EventReport): ResponseEntity<Void> {
    eventReportRepo.save(eventReport.reportId, eventReport)
    return ResponseEntity.accepted().build()
  }

  /**
   * Retrieving all EventReports.
   * Endpoint: /event-reports
   */
  @GetMapping(value = ["/event-reports"])
  fun getEventReports(): List<EventReport> = eventReportRepo.getAll().toList()

  /**
   * Save new aggregate reports.
   * Reports are saved by report_id. Old report will be overwritten if same report_id is used.
   * Endpoint: /.well-known/attribution-reporting/report-aggregate-attribution
   * Request Body: AggregateReport
   */
  @PostMapping(value = ["/.well-known/attribution-reporting/report-aggregate-attribution"])
  fun saveAggregateReport(@RequestBody aggregateReport: AggregateReport): ResponseEntity<Void> {
    aggregateReportRepo.save(aggregateReport.sharedInfo.hashCode().toString(), aggregateReport)
    return ResponseEntity.accepted().build()
  }

  /**
   * Retrieving all AggregateReports.
   * Endpoint: /aggregate-reports
   */
  @GetMapping(value = ["/aggregate-reports"])
  fun getAggregateReports(): List<AggregateReport> = aggregateReportRepo.getAll().toList()
}

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
package com.example.adservices.samples.fledge.util

import android.widget.TextView
import java.util.LinkedList

/**
 * Class that manages a text view event log and shows the HISTORY_LENGTH most recent events
 *
 * @param display The TextView to manage.
 */
class EventLogManager(
  private val display: TextView,
) {
  private val title = "Event Log"
  private val historyLength = 30

  private val events = LinkedList<String>()

  init {
    render()
  }

  /**
   * Add an event string to the front of the event log.
   *
   * @param event The events string to add.
   */
  fun writeEvent(event: String) {
    synchronized(events) {
      events.add(event)
      if (events.size > historyLength) {
        events.remove()
      }
    }
    render()
  }

  fun flush() {
    synchronized(events) { events.clear() }
    render()
  }

  /** Re-renders the event log with the current events from [.mEvents]. */
  private fun render() {
    val output = StringBuilder()
    output.append("$title\n")
    var eventNumber = 1
    synchronized(events) {
      val it = events.descendingIterator()
      while (it.hasNext()) {
        output.append(eventNumber++).append(". ").append(it.next()).append("\n")
      }
      display.text = output
    }
  }
}

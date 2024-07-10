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
package com.example.adservices.samples.fledge.sampleapp

import android.widget.TextView
import java.util.LinkedList

/** Text that appears above the event log */
private const val TITLE = "Event Log"

/** The number of events to display */
private const val HISTORY_LENGTH = 30

/**
 * Class that manages a text view event log and shows the HISTORY_LENGTH most recent events
 *
 * @param display The TextView to manage.
 */
class EventLogManager(
    /** A text view to display the events */
    private val display: TextView
) {

    /** A queue of the HISTORY_LENGTH most recent events */
    private val events = LinkedList<String>()

    /**
     * Add an event string to the front of the event log.
     *
     * @param event The events string to add.
     */
    fun writeEvent(event: String) {
        synchronized(events) {
            events.add(event)
            if (events.size > HISTORY_LENGTH) {
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
        output.append(
            """
  $TITLE
  
  """
                .trimIndent()
        )
        var eventNumber = 1
        synchronized(events) {
            val it = events.descendingIterator()
            while (it.hasNext()) {
                output.append(eventNumber++).append(". ").append(it.next()).append("\n")
            }
            display.text = output
        }
    }

    /** Does the initial render. */
    init {
        render()
    }
}

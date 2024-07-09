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
package com.example.adservices.samples.signals.sampleapp

import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import java.util.LinkedList
import kotlin.jvm.Synchronized

/**
 * Class that manages a text view event log and shows the HISTORY_LENGTH most recent events
 */
class EventLogManager(
  /**
   * A text view to display the events
   */
  private val mDisplay: TextView
) {
  /**
   * The number of events to display
   */
  private val HISTORY_LENGTH = 30

  /**
   * Text that appears above the event log
   */
  private val TITLE = "Event Log"

  /**
   * A queue of the HISTORY_LENGTH most recent events
   */
  private val mEvents = LinkedList<String>()

  /**
   * Constructor takes only the TextView to manage.
   * @param display The TextView to manage.
   */
  init {
    mDisplay.movementMethod = ScrollingMovementMethod()
    render()
  }

  /**
   * Add an event string to the front of the event log.
   * @param event The events string to add.
   */
  fun writeEvent(event: String) {
    synchronized(mEvents) {
      mEvents.add(event)
      if (mEvents.size > HISTORY_LENGTH) {
        mEvents.remove()
      }
    }
    render()
  }

  fun flush() {
    synchronized(mEvents) { mEvents.clear() }
    render()
  }

  /**
   * Re-renders the event log with the current events from [.mEvents].
   */
  @Synchronized
  private fun render() {
    val output = StringBuilder()
    output.append(
      """
  $TITLE
  
  """.trimIndent()
    )
    var eventNumber = 1
    synchronized(mEvents) {
      val it = mEvents.descendingIterator()
      while (it.hasNext()) {
        output.append(eventNumber++).append(". ").append(it.next()).append("\n")
      }
      mDisplay.text = output
    }
  }
}
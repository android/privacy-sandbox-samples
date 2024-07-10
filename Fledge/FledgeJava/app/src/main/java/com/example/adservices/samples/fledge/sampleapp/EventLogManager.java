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
package com.example.adservices.samples.fledge.sampleapp;

import static com.example.adservices.samples.fledge.sampleapp.MainActivity.TAG;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.util.Iterator;
import java.util.LinkedList;

/** Class that manages a text view event log and shows the HISTORY_LENGTH most recent events */
public class EventLogManager {

    /** The number of events to display */
    private final int HISTORY_LENGTH = 30;

    /** Text that appears above the event log */
    private final String TITLE = "Event Log";

    /** A text view to display the events */
    private final TextView mDisplay;

    /** A queue of the HISTORY_LENGTH most recent events */
    private final LinkedList<String> mEvents = new LinkedList<>();

    /**
     * Constructor takes only the TextView to manage.
     *
     * @param display The TextView to manage.
     */
    public EventLogManager(TextView display) {
        this.mDisplay = display;
        this.mDisplay.setMovementMethod(new ScrollingMovementMethod());
        render();
    }

    /**
     * Add an event string to the front of the event log.
     *
     * @param event The events string to add.
     */
    public void writeEvent(String event) {
        synchronized (mEvents) {
            mEvents.add(event);
            if (mEvents.size() > HISTORY_LENGTH) {
                mEvents.remove();
            }
        }
        render();
    }

    /** Re-renders the event log with the current events from {@link #mEvents}. */
    private synchronized void render() {
        StringBuilder output = new StringBuilder();
        output.append(TITLE + "\n");
        int eventNumber = 1;
        synchronized (mEvents) {
            for (Iterator<String> it = mEvents.descendingIterator(); it.hasNext(); ) {
                output.append(eventNumber++).append(". ").append(it.next()).append("\n");
            }
            mDisplay.setText(output);
            Log.v(TAG, "Event log set to: " + output);
        }
    }
}

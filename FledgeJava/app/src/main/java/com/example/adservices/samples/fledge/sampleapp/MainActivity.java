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

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.adservices.samples.fledge.sampleapp.databinding.ActivityMainBinding;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Android application activity for testing FLEDGE API
 */
public class MainActivity extends AppCompatActivity {

    // Log tag
    public static final String TAG = "FledgeSample";

    // The sample buyer and seller for the custom audiences
    public static final String BUYER = "sample-buyer.sampleapp";
    public static final String SELLER = "sample-seller.sampleapp";

    // The names for the shirts and shoes custom audience
    private static final String SHOES_NAME = "shoes";
    private static final String SHIRTS_NAME = "shirts";

    // Shirts and shoes render URLS
    private static final Uri SHOES_RENDER_URL = Uri.parse("shoes-url.shoestld");
    private static final Uri SHIRTS_RENDER_URL = Uri.parse("shirts-url.shirtstld");

    // Executor to be used for API calls
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();

    // String to inform user a field in missing
    private static final String MISSING_FIELD_STRING_FORMAT = "ERROR: %s is missing, " +
        "restart the activity using the directions in the README. The app will not be usable " +
        "until this is done";

    // JS server URLs
    private Uri mBiddingUrl;
    private Uri mScoringUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        EventLogManager eventLog = new EventLogManager(binding.eventLog);

        try {
            // Set URLS
            mBiddingUrl = Uri.parse(getIntentOrError("biddingUrl", eventLog));
            mScoringUrl = Uri.parse(getIntentOrError("scoringUrl", eventLog));

            // Set up ad selection
            AdSelectionClient adClient = new AdSelectionClient(Collections.singletonList(BUYER),
                SELLER, () -> mScoringUrl, context, EXECUTOR);
            binding.runAdsButton.setOnClickListener(v ->
                adClient.runAdSelection(eventLog::writeEvent, binding.adSpace::setText));

            // Set up CAs
            String owner = context.getPackageName();
            CustomAudienceClient caClient = new CustomAudienceClient(owner, BUYER, context, EXECUTOR);
            binding.joinShoesButton.setOnClickListener(v ->
                caClient.joinCa(SHOES_NAME, mBiddingUrl, SHOES_RENDER_URL,
                    eventLog::writeEvent));
            binding.joinShirtsButton.setOnClickListener(v ->
                caClient.joinCa(SHIRTS_NAME, mBiddingUrl, SHIRTS_RENDER_URL,
                    eventLog::writeEvent));
            binding.leaveShoesButton.setOnClickListener(v ->
                caClient.leaveCa(SHOES_NAME, eventLog::writeEvent));
            binding.leaveShirtsButton.setOnClickListener(v ->
                caClient.leaveCa(SHIRTS_NAME, eventLog::writeEvent));
        } catch (Exception e) {
            Log.e(TAG, "Error when setting up app", e);
        }
    }

    /**
     * Gets a given intent extra or notifies the user that it is missing
     * @param intent The intent to get
     * @param eventLog An eventlog to write the error to
     * @return The string value of the intent specified.
     */
    private String getIntentOrError(String intent, EventLogManager eventLog) {
        String toReturn = getIntent().getStringExtra(intent);
        if (toReturn == null) {
            String message = String.format(MISSING_FIELD_STRING_FORMAT, intent);
            eventLog.writeEvent(message);
            throw new RuntimeException(message);
        }
        return toReturn;
    }
}
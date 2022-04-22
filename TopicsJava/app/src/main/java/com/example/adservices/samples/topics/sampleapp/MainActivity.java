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
package com.example.adservices.samples.topics.sampleapp;

import android.adservices.exceptions.GetTopicsException;
import android.adservices.topics.GetTopicsRequest;
import android.adservices.topics.GetTopicsResponse;
import android.adservices.topics.TopicsManager;
import android.content.Context;
import android.os.OutcomeReceiver;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Android application activity for testing Topics API by sending a call to
 * the 'getTopics()' function onResume. When a result is received it will be displayed
 * on screen in a text box, as well as displaying a text box showing what the current
 * package name is for the application. This project can be build with 11 different
 * flavors, each of which will assign a different package name corresponding to a
 * different suite of possible Topics.
 */
public class MainActivity extends AppCompatActivity {

    //TextView to display results from getTopics call
    TextView results;
    //TextView to display current package name which influences returned topics
    TextView packageNameDisplay;

    //On app creation setup view as well as assign variables for TextViews to display results
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        results = (TextView) findViewById(R.id.textView);
        packageNameDisplay = (TextView) findViewById(R.id.textView2);
    }

    //On Application Resume, call getTopics code. This can be used to facilitate automating population of topics data
    @Override
    protected void onResume()
    {
        super.onResume();
        packageNameDisplay.setText(getBaseContext().getPackageName());
        TopicGetter();
    }

    //TopicGetter holds all of the setup and code for creating a TopicsManager and getTopics call
    public void TopicGetter()
    {
        Context mContext = getBaseContext();
        TopicsManager mTopicsManager = mContext.getSystemService(TopicsManager.class);
        Executor mExecutor = Executors.newCachedThreadPool();
        GetTopicsRequest.Builder mGetTopicsRequest = new GetTopicsRequest.Builder().setSdkName("com.example.adtech");
        mTopicsManager.getTopics(mGetTopicsRequest.build(),mExecutor,mCallback);
    }

    //onResult is called when getTopics successfully comes back with an answer
    OutcomeReceiver mCallback = new OutcomeReceiver<GetTopicsResponse, GetTopicsException>() {
        @Override
        public void onResult(@NonNull GetTopicsResponse result)
        {
            List<String> topicsResult = result.getTopics();
            for(int i = 0; i < topicsResult.size(); i++){
                Log.i("Topic", topicsResult.get(i));
                if(results.isEnabled())
                {
                    results.setText(topicsResult.get(i));
                }
            }
            if(topicsResult.size() == 0)
            {
                Log.i("Topic", "Returned Empty");
                if(results.isEnabled())
                {
                    results.setText("Returned Empty");
                }
            }

        }

        //onError should not be returned, even invalid topics callers should simply return empty
        @Override
        public void onError(@NonNull GetTopicsException error)
        {
            // Handle error
            Log.i("Topic", "Experienced an Error, and did not return successfully");
        }
    };
}
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

import android.adservices.common.AdData;
import android.adservices.customaudience.CustomAudience;
import android.adservices.customaudience.CustomAudienceManager;
import android.adservices.customaudience.JoinCustomAudienceRequest;
import android.adservices.customaudience.LeaveCustomAudienceRequest;
import android.adservices.customaudience.TrustedBiddingData;
import android.adservices.exceptions.AdServicesException;
import android.content.Context;
import android.net.Uri;
import android.os.OutcomeReceiver;
import android.util.Log;
import androidx.annotation.NonNull;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.json.JSONObject;

/**
 * Wrapper for the FLEDGE Custom Audience API. Creating the client locks the user into a given owner
 * and buyer. In order to interact with the client they will first need to call the create method
 * to create a CA object. After that they can call joinCA and leaveCA.
 */
public class CustomAudienceClient {
  private final Executor mExecutor;
  private final String mOwner;
  private final String mBuyer;
  private final CustomAudienceManager mCaManager;

  /**
   * Initialize the CustomAudienceClient and set the owner and buyer.
   *
   * @param owner The owner field for custom audience created by this client.
   * @param buyer The buyer field for custom audience created by this client.
   * @param context The application context.
   * @param executor An executor to use with the FLEDGE API calls.
   */
  public CustomAudienceClient(String owner, String buyer, Context context, Executor executor) {
    mExecutor = executor;
    mOwner = owner;
    mBuyer = buyer;
    mCaManager = context.getSystemService(CustomAudienceManager.class);
  }

  /**
   * Joins a CA.
   *
   * @param name The name of the CA to join.
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  public void joinCa(String name, Uri biddingUrl, Uri renderUrl, Consumer<String> statusReceiver) {
    OutcomeReceiver<Void, AdServicesException> joinCAReceiver
        = new OutcomeReceiver<Void, AdServicesException>() {
      @Override
      public void onResult(@NonNull Void unused) {
        statusReceiver.accept("Joined " + name + " custom audience");
      }

      @Override
      public void onError(@NonNull AdServicesException e) {
        statusReceiver.accept("Error when joining " + name + " custom audience: "
            + e.getMessage());
        Log.e(MainActivity.TAG, "Exception during CA join process ", e);
      }
    };

    try {
      CustomAudience ca = new CustomAudience.Builder()
          .setOwner(mOwner)
          .setBuyer(mBuyer)
          .setName(name)
          .setDailyUpdateUrl(Uri.EMPTY)
          .setBiddingLogicUrl(biddingUrl)
          .setAds(Collections.singletonList(new AdData.Builder()
              .setRenderUrl(renderUrl)
              .setMetadata(new JSONObject().toString())
              .build()))
          .setActivationTime(Instant.now())
          .setExpirationTime(Instant.now().plus(Duration.ofDays(1)))
          .setTrustedBiddingData(new TrustedBiddingData.Builder()
              .setTrustedBiddingKeys(new ArrayList<>())
              .setTrustedBiddingUrl(Uri.EMPTY).build())
          .setUserBiddingSignals(new JSONObject().toString())
          .build();
      JoinCustomAudienceRequest joinRequest = (new JoinCustomAudienceRequest.Builder())
          .setCustomAudience(ca).build();
      mCaManager.joinCustomAudience(joinRequest, mExecutor, joinCAReceiver);
    } catch (Exception e) {
      statusReceiver.accept("Got the following exception when trying to join " + name
          + " custom audience: " + e);
      Log.e(MainActivity.TAG, "Exception calling joinCustomAudience", e);
    }
  }

  /**
   * Leaves a CA.
   *
   * @param name The name of the CA to leave.
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  public void leaveCa(String name, Consumer<String> statusReceiver) {
    OutcomeReceiver<Void, AdServicesException> leaveCAReceiver
        = new OutcomeReceiver<Void, AdServicesException>() {

      @Override
      public void onResult(@NonNull Void unused) {
        statusReceiver.accept("Left " + name + " custom audience");
      }

      @Override
      public void onError(@NonNull AdServicesException e) {
        statusReceiver.accept("Error when leaving " + name
            + " custom audience: " + e.getMessage());
      }
    };

    try {
      LeaveCustomAudienceRequest leaveRequest = (new LeaveCustomAudienceRequest.Builder())
          .setOwner(mOwner).setBuyer(mBuyer).setName(name).build();
      mCaManager.leaveCustomAudience(leaveRequest, mExecutor, leaveCAReceiver);
    } catch (Exception e) {
      statusReceiver.accept("Got the following exception when trying to leave " + name
          + " custom audience: " + e);
      Log.e(MainActivity.TAG, "Exception calling leaveCustomAudience", e);
    }
  }
}

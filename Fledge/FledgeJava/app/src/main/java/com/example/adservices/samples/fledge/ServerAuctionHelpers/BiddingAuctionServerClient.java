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
package com.example.adservices.samples.fledge.ServerAuctionHelpers;

import android.content.Context;
import android.util.Log;
import com.example.adservices.samples.fledge.sampleapp.MainActivity;
import com.google.common.io.BaseEncoding;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Class to call and receive response from Bidding Auction Server.
 */
public class BiddingAuctionServerClient {
  private static final Gson sGson =
      new GsonBuilder()
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create();
  private final Context mContext;
  public BiddingAuctionServerClient(Context context) {
    mContext = context;
  }

  public SelectAdsResponse runServerAuction(
      String sfeAddress,
      String seller,
      String buyer,
      byte[] adSelectionData) throws IOException {
    Log.i(MainActivity.TAG, "sfeAddress: " + sfeAddress + " seller: " + seller + " buyer: " + buyer);
    // Add contextual data
    SelectAdsRequest selectAdsRequest =
        SelectAdsRequest.builder()
            .setAuctionConfig(AuctionConfigGenerator.getAuctionConfig(seller, buyer))
            .setClientType("ANDROID")
            // Because we are making a HTTPS call, we need to encode the ciphertext byte array
            .setProtectedAudienceCiphertext(
                BaseEncoding.base64().encode(adSelectionData)).build();
    Log.d(MainActivity.TAG, selectAdsRequest.toString());
    return makeSelectAdsCall(sfeAddress, selectAdsRequest);
  }

  private static SelectAdsResponse makeSelectAdsCall(
      String sfeAddress, SelectAdsRequest request) throws IOException {
    String requestPayload = getSelectAdPayload(request);
    String response = makeHttpPostCall(sfeAddress, requestPayload);
    Log.d(MainActivity.TAG, "Response from b&a : " + response);
    return parseSelectAdResponse(response);
  }

  private static SelectAdsResponse parseSelectAdResponse(String jsonString) {
    return new GsonBuilder().create().fromJson(jsonString, SelectAdsResponse.class);
  }

  private static String getSelectAdPayload(SelectAdsRequest selectAdsRequest) {
    return sGson.toJson(selectAdsRequest);
  }

  private static String makeHttpPostCall(String address, String jsonInputString)
      throws IOException {
    URL url = new URL(address);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestMethod("POST");
    con.setRequestProperty("Content-Type", "application/json");
    con.setRequestProperty("Accept", "application/json");
    con.setDoOutput(true);
    try (OutputStream os = con.getOutputStream()) {
      byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
      Log.d(MainActivity.TAG, "HTTP Post call made with payload : " + jsonInputString);
    }

    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
      StringBuilder response = new StringBuilder();
      String responseLine;
      while ((responseLine = br.readLine()) != null) {
        response.append(responseLine.trim());
      }
      Log.d(MainActivity.TAG, "Response read : " + response);

      return response.toString();
    }
  }
}
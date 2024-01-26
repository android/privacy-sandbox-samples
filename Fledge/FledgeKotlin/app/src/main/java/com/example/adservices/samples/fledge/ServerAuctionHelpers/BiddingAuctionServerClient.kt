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
package com.example.adservices.samples.fledge.ServerAuctionHelpers

import android.content.Context
import android.util.Log
import com.example.adservices.samples.fledge.sampleapp.MainActivity
import com.example.adservices.samples.fledge.sampleapp.TAG
import com.google.common.io.BaseEncoding
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Class to call and receive response from Bidding Auction Server.
 */
class BiddingAuctionServerClient(private val context: Context) {
  companion object {
    private val sGson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create()
  }

  fun runServerAuction(
    sfeAddress: String,
    seller: String,
    buyer: String,
    adSelectionData: ByteArray,
  ): SelectAdsResponse {
    Log.i(TAG, "sfeAddress: $sfeAddress seller: $seller buyer: $buyer")
    val auctionConfig = AuctionConfigGenerator.getAuctionConfig(seller, buyer)
    val selectAdsRequest = SelectAdsRequest(
      auctionConfig = auctionConfig,
      clientType = "CLIENT_TYPE_ANDROID",
      protectedAudienceCiphertext = BaseEncoding.base64().encode(adSelectionData)
    )
    Log.d(TAG, selectAdsRequest.toString())
    return makeSelectAdsCall(sfeAddress, selectAdsRequest)
  }

  private fun makeSelectAdsCall(sfeAddress: String, request: SelectAdsRequest): SelectAdsResponse {
    val requestPayload = getSelectAdPayload(request)
    val response = makeHttpPostCall(sfeAddress, requestPayload)
    Log.d(TAG, "Response from b&a : $response")
    return parseSelectAdResponse(response)
  }

  private fun parseSelectAdResponse(jsonString: String): SelectAdsResponse {
    return GsonBuilder().create().fromJson(jsonString, SelectAdsResponse::class.java)
  }

  private fun getSelectAdPayload(selectAdsRequest: SelectAdsRequest): String {
    return sGson.toJson(selectAdsRequest)
  }

  private fun makeHttpPostCall(address: String, jsonInputString: String): String {
    val url = URL(address)
    val con = url.openConnection() as HttpURLConnection
    con.requestMethod = "POST"
    con.setRequestProperty("Content-Type", "application/json")
    con.setRequestProperty("Accept", "application/json")
    con.doOutput = true

    con.outputStream.use { os ->
      val input = jsonInputString.toByteArray(StandardCharsets.UTF_8)
      os.write(input, 0, input.size)
      Log.d(TAG, "HTTP Post call made with payload : $jsonInputString")
    }

    con.inputStream.bufferedReader(StandardCharsets.UTF_8).use { br ->
      val response = StringBuilder()
      var responseLine: String? = br.readLine()
      while (responseLine != null) {
        response.append(responseLine.trim())
        responseLine = br.readLine()
      }
      Log.d(TAG, "Response read : $response")
      return response.toString()
    }
  }
}

package com.example.adservices.samples.fledge.adselection

import android.util.Log
import androidx.privacysandbox.ads.adservices.adselection.GetAdSelectionDataOutcome
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import com.example.adservices.samples.fledge.adselection.selectads.SelectAdsRequest
import com.example.adservices.samples.fledge.adselection.selectads.SelectAdsResponse
import com.example.adservices.samples.fledge.adselection.selectads.ServerAuctionConfig
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.TAG
import com.google.common.io.BaseEncoding
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Class to call and receive response from Bidding Auction Server.
 */
class BiddingAuctionServerClient {
  private val gson: Gson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create()

  @OptIn(ExperimentalFeatures.Ext10OptIn::class)
  suspend fun runServerAuction(
    getAdSelectionDataOutcome: GetAdSelectionDataOutcome,
    auctionConfig: ServerAuctionConfig,
    sellerSfeUri: String,
    statusReceiver: Consumer<String>
  ): SelectAdsResponse {
    try {
      // Add contextual data
      val selectAdsRequest = SelectAdsRequest(
        protectedAudienceCiphertext = BaseEncoding.base64().encode(getAdSelectionDataOutcome.adSelectionData!!),
        auctionConfig = auctionConfig,
        clientType = CLIENT_TYPE
      )

      val selectAdsResponse = makeSelectAdsCall(sellerSfeUri, selectAdsRequest)

      statusReceiver.accept(
        "Server auction run successfully for ${getAdSelectionDataOutcome.adSelectionId}"
      )
      Log.v(TAG, "Server auction run successfully for ${getAdSelectionDataOutcome.adSelectionId}")
      return selectAdsResponse
    } catch (e: Exception) {
      statusReceiver.accept("Exception running server auction: ${e.message}")
      Log.e(TAG, "Exception running server auction: ${e.message}", e)
      throw e
    }
  }

  private suspend fun makeSelectAdsCall(sellerSfeUri: String, selectAdsRequest: SelectAdsRequest): SelectAdsResponse {
    val requestPayload = gson.toJson(selectAdsRequest)
    val response = makeHttpPostCall(sellerSfeUri, requestPayload)
    return GsonBuilder().create().fromJson(response, SelectAdsResponse::class.java)
  }

  private suspend fun makeHttpPostCall(address: String, jsonInputString: String): String {
    return withContext(Dispatchers.IO) {
      try {
        val url = URL(address)
        val con: HttpURLConnection = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", "application/json")
        con.setRequestProperty("Accept", "application/json")
        con.doOutput = true
        con.outputStream.use { os ->
          val input: ByteArray = jsonInputString.toByteArray(StandardCharsets.UTF_8)
          os.write(input, 0, input.size)
          Log.d(TAG, "HTTP Post call made with payload: $jsonInputString")
        }
        BufferedReader(InputStreamReader(con.inputStream, StandardCharsets.UTF_8))
          .useLines { lines ->
            val response = StringBuilder()
            lines.forEach { response.append(it.trim()) }
            Log.d(TAG, "Response read: $response")
            response.toString()
          }
      } catch (e: IOException) {
        Log.e(TAG, "Error making HTTP call: ${e.message}", e)
        throw e
      }
    }
  }

  companion object {
    const val CLIENT_TYPE = "CLIENT_TYPE_ANDROID"
  }
}
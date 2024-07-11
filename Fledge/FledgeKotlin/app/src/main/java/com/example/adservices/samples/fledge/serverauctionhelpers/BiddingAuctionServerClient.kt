package com.example.adservices.samples.fledge.serverauctionhelpers

import android.content.Context
import android.util.Log
import com.example.adservices.samples.fledge.sampleapp.TAG
import com.example.adservices.samples.fledge.serverauctionhelpers.AuctionConfigGenerator.getAuctionConfig
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


/**
 * Class to call and receive response from Bidding Auction Server.
 */
class BiddingAuctionServerClient(context: Context) {
    private val mContext: Context

    init {
        mContext = context
    }

    @Throws(IOException::class)
    fun runServerAuction(
            sfeAddress: String,
            seller: String,
            buyer: String,
            adSelectionData: ByteArray?): SelectAdsResponse {
        Log.i(TAG, "sfeAddress: $sfeAddress seller: $seller buyer: $buyer")
        // Add contextual data
        val selectAdsRequest = SelectAdsRequest(
            BaseEncoding.base64().encode(adSelectionData),
            getAuctionConfig(seller, buyer),
            "CLIENT_TYPE_ANDROID")
        Log.d(TAG, selectAdsRequest.toString())
        return makeSelectAdsCall(sfeAddress, selectAdsRequest)
    }

    companion object {
        private val sGson: Gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()

        @Throws(IOException::class)
        private fun makeSelectAdsCall(
                sfeAddress: String, request: SelectAdsRequest): SelectAdsResponse {
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

        @Throws(IOException::class)
        private fun makeHttpPostCall(address: String, jsonInputString: String): String {
            val url = URL(address)
            val con: HttpURLConnection = url.openConnection() as HttpURLConnection
            con.setRequestMethod("POST")
            con.setRequestProperty("Content-Type", "application/json")
            con.setRequestProperty("Accept", "application/json")
            con.setDoOutput(true)
            con.getOutputStream().use { os ->
                val input: ByteArray = jsonInputString.toByteArray(StandardCharsets.UTF_8)
                os.write(input, 0, input.size)
                Log.d(TAG, "HTTP Post call made with payload : $jsonInputString")
            }
            BufferedReader(InputStreamReader(con.inputStream, StandardCharsets.UTF_8))
                .useLines { lines ->
                    val response = StringBuilder()
                    lines.forEach { response.append(it.trim()) }
                    Log.d(TAG, "Response read : $response")
                    return response.toString()
                }
        }
    }
}

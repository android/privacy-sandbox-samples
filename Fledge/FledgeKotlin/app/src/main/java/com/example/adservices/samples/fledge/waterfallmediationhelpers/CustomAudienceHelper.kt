package com.example.adservices.samples.fledge.waterfallmediationhelpers

import androidx.annotation.RequiresApi
import com.example.adservices.samples.fledge.clients.CustomAudienceClient
import com.example.adservices.samples.fledge.clients.TestCustomAudienceClient
import android.adservices.common.AdTechIdentifier
import android.adservices.customaudience.CustomAudience
import android.adservices.customaudience.AddCustomAudienceOverrideRequest
import android.adservices.common.AdSelectionSignals
import com.example.adservices.samples.fledge.sampleapp.MainActivity
import android.adservices.common.AdData
import org.json.JSONObject
import android.adservices.customaudience.TrustedBiddingData
import android.net.Uri
import android.util.Log
import com.example.adservices.samples.fledge.waterfallmediationhelpers.Constants.TAG
import java.lang.Exception
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Helps creating CAs to set the environment for mediation activity.
 */
@RequiresApi(api = 34)
class CustomAudienceHelper(
  private val customAudienceClient: CustomAudienceClient,
  private val testCustomAudienceClient: TestCustomAudienceClient
) {
  fun configureCustomAudience(
    customAudienceName: String,
    bid: Double,
    baseUri: Uri,
    useOverrides: Boolean
  ): AdTechIdentifier {
    var baseUri = baseUri
    val uriFriendlyName = Constants.uriFriendlyString(customAudienceName)
    if (useOverrides) {
      baseUri = Uri.parse(String.format(Constants.DEFAULT_BASE_URI_FORMAT, uriFriendlyName))
    }
    val customAudience = getCustomAudience(customAudienceName, baseUri, bid)
    if (useOverrides) {
      addOverrideCustomAudience(customAudience)
    }
    joinCustomAudience(customAudience)
    Log.i(Constants.TAG, customAudience.buyer.toString() + " buyer is returned")
    return customAudience.buyer
  }

  private fun addOverrideCustomAudience(customAudience: CustomAudience) {
    val biddingLogicJs =
      String.format(Constants.BIDDING_LOGIC_JS, Constants.uriFriendlyString(customAudience.name))
    try {
      testCustomAudienceClient.overrideCustomAudienceRemoteInfo(
        AddCustomAudienceOverrideRequest.Builder()
          .setBuyer(customAudience.buyer)
          .setName(customAudience.name)
          .setBiddingLogicJs(biddingLogicJs)
          .setTrustedBiddingSignals(AdSelectionSignals.EMPTY)
          .build())!![10, TimeUnit.SECONDS]
    } catch (e: Exception) {
      Log.e(TAG, "Exception calling overrideCustomAudienceRemoteInfo", e)
    }
  }

  private fun joinCustomAudience(customAudience: CustomAudience) {
    try {
      customAudienceClient.joinCustomAudience(customAudience)[10, TimeUnit.SECONDS]
      Thread.sleep(1000)
    } catch (e: Exception) {
      Log.e(TAG, "Exception calling joinCustomAudience", e)
    }
  }

  private fun getCustomAudience(
    customAudienceName: String,
    baseUri: Uri,
    bid: Double
  ): CustomAudience {
    val uriFriendlyName = Constants.uriFriendlyString(customAudienceName)
    val userBiddingSignals =
      AdSelectionSignals.fromString(String.format(Constants.BID_SIGNALS_FORMAT, bid))
    val buyer = AdTechIdentifier.fromString(baseUri.host!!)
    val activationTime = Instant.now()
    val expirationTime = Instant.now().plus(Duration.ofDays(1))
    val ads = listOf(AdData.Builder()
                       .setRenderUri(getAdRenderUri(baseUri, uriFriendlyName))
                       .setMetadata(JSONObject().toString())
                       .build())
    val trustedBiddingData = TrustedBiddingData.Builder()
      .setTrustedBiddingKeys(emptyList())
      .setTrustedBiddingUri(getTrustedBiddingDataUri(baseUri))
      .build()
    return CustomAudience.Builder()
      .setBuyer(buyer)
      .setName(customAudienceName)
      .setActivationTime(activationTime)
      .setExpirationTime(expirationTime)
      .setDailyUpdateUri(getDailyUpdateUri(baseUri))
      .setUserBiddingSignals(userBiddingSignals)
      .setTrustedBiddingData(trustedBiddingData)
      .setBiddingLogicUri(getBiddingLogicUri(baseUri))
      .setAds(ads)
      .build()
  }

  private fun getBiddingLogicUri(baseUri: Uri): Uri {
    return baseUri.buildUpon().appendPath(Constants.BIDDING_URI_SUFFIX).build()
  }

  private fun getDailyUpdateUri(baseUri: Uri): Uri {
    return baseUri.buildUpon().appendPath(Constants.BIDDING_URI_SUFFIX).build()
  }

  private fun getTrustedBiddingDataUri(baseUri: Uri): Uri {
    return baseUri.buildUpon().appendPath(Constants.TRUSTED_BIDDING_URI_SUFFIX).build()
  }

  private fun getAdRenderUri(baseUri: Uri, adName: String?): Uri {
    return baseUri.buildUpon().appendPath(Constants.BIDDING_URI_SUFFIX).appendPath(adName).build()
  }
}
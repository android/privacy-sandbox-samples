package com.example.adservices.samples.fledge.WaterfallMediationHelpers;

import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.BIDDING_LOGIC_JS;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.BIDDING_URI_SUFFIX;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.BID_SIGNALS_FORMAT;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.DEFAULT_BASE_URI_FORMAT;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.TAG;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.TRUSTED_BIDDING_URI_SUFFIX;

import android.adservices.common.AdData;
import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.adservices.customaudience.AddCustomAudienceOverrideRequest;
import android.adservices.customaudience.CustomAudience;
import android.adservices.customaudience.TrustedBiddingData;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.RequiresApi;
import com.example.adservices.samples.fledge.clients.CustomAudienceClient;
import com.example.adservices.samples.fledge.clients.TestCustomAudienceClient;
import com.example.adservices.samples.fledge.sampleapp.MainActivity;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/**
 * Helps creating CAs to set the environment for mediation activity.
 */
@RequiresApi(api = 34)
public class CustomAudienceHelper {

  private final CustomAudienceClient customAudienceClient;
  private final TestCustomAudienceClient testCustomAudienceClient;

  public CustomAudienceHelper(
      CustomAudienceClient customAudienceClient,
      TestCustomAudienceClient testCustomAudienceClient) {
    this.customAudienceClient = customAudienceClient;
    this.testCustomAudienceClient = testCustomAudienceClient;
  }

  public AdTechIdentifier configureCustomAudience(String customAudienceName, double bid, Uri baseUri, boolean useOverrides) {
    String uriFriendlyName = Constants.uriFriendlyString(customAudienceName);
    if (useOverrides) {
      baseUri = Uri.parse(String.format(DEFAULT_BASE_URI_FORMAT, uriFriendlyName));
    }
    CustomAudience customAudience = getCustomAudience(customAudienceName, baseUri, bid);

    if(useOverrides) {
      addOverrideCustomAudience(customAudience);
    }

    joinCustomAudience(customAudience);
    Log.i(TAG, customAudience.getBuyer() + " buyer is returned");
    return customAudience.getBuyer();
  }

  private void addOverrideCustomAudience(CustomAudience customAudience) {

    String biddingLogicJs = String.format(BIDDING_LOGIC_JS, Constants.uriFriendlyString(customAudience.getName()));
    try {
      testCustomAudienceClient.overrideCustomAudienceRemoteInfo(
          new AddCustomAudienceOverrideRequest.Builder()
              .setBuyer(customAudience.getBuyer())
              .setName(customAudience.getName())
              .setBiddingLogicJs(biddingLogicJs)
              .setTrustedBiddingSignals(AdSelectionSignals.EMPTY)
              .build()).get(10, TimeUnit.SECONDS);
    } catch (Exception e) {
      Log.e(MainActivity.TAG, "Exception calling overrideCustomAudienceRemoteInfo", e);
    }
  }

  private void joinCustomAudience(CustomAudience customAudience) {
    try {
      customAudienceClient.joinCustomAudience(customAudience).get(10, TimeUnit.SECONDS);
      Thread.sleep(1000);
    } catch (Exception e) {
      Log.e(MainActivity.TAG, "Exception calling joinCustomAudience", e);
    }
  }

  private CustomAudience getCustomAudience(String customAudienceName, Uri baseUri, double bid) {
    String uriFriendlyName = Constants.uriFriendlyString(customAudienceName);

    AdSelectionSignals userBiddingSignals = AdSelectionSignals.fromString(String.format(BID_SIGNALS_FORMAT, bid));
    AdTechIdentifier buyer = AdTechIdentifier.fromString(baseUri.getHost());
    Instant activationTime = Instant.now();
    Instant expirationTime  = Instant.now().plus(Duration.ofDays(1));
    List<AdData> ads = Collections.singletonList(new AdData.Builder()
        .setRenderUri(getAdRenderUri(baseUri, uriFriendlyName))
        .setMetadata(new JSONObject().toString())
        .build());
    TrustedBiddingData trustedBiddingData = new TrustedBiddingData.Builder()
        .setTrustedBiddingKeys(Collections.emptyList())
        .setTrustedBiddingUri(getTrustedBiddingDataUri(baseUri))
        .build();

    return new CustomAudience.Builder()
        .setBuyer(buyer)
        .setName(customAudienceName)
        .setActivationTime(activationTime)
        .setExpirationTime(expirationTime)
        .setDailyUpdateUri(getDailyUpdateUri(baseUri))
        .setUserBiddingSignals(userBiddingSignals)
        .setTrustedBiddingData(trustedBiddingData)
        .setBiddingLogicUri(getBiddingLogicUri(baseUri))
        .setAds(ads)
        .build();
  }

  private Uri getBiddingLogicUri(Uri baseUri) {
    return baseUri.buildUpon().appendPath(BIDDING_URI_SUFFIX).build();
  }

  private Uri getDailyUpdateUri(Uri baseUri) {
    return baseUri.buildUpon().appendPath(BIDDING_URI_SUFFIX).build();
  }

  private Uri getTrustedBiddingDataUri(Uri baseUri) {
    return baseUri.buildUpon().appendPath(TRUSTED_BIDDING_URI_SUFFIX).build();
  }

  private Uri getAdRenderUri(Uri baseUri, String adName) {
    return baseUri.buildUpon().appendPath(BIDDING_URI_SUFFIX).appendPath(adName).build();
  }

}

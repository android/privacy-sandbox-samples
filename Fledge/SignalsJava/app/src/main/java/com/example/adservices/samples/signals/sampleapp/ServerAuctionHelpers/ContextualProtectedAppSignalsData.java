package com.example.adservices.samples.signals.sampleapp.ServerAuctionHelpers;

import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;
import java.util.List;

/**
 * Protected App Signals ad related data that Buyer adtechs can send via
 * contextual path to control which PAS ads participate in the auction.
 */
@AutoValue
public abstract class ContextualProtectedAppSignalsData {
  /** Raw Ad ids that can be used to lookup signals from the KV server. */
  @Nullable
  public abstract List<String> getAdRenderIds();
  /**
   * Bool indicating whether ads should also be fetched from ads retrieval
   * service.
   *
   * <p>If true, the Bidding server will send an Ad fetch request to the
   * Ad retrieval service and the request will contain the list of ad_render_ids
   * as an additional parameter.
   *
   * <p>If false, the ad_render_ids will be sent to the TEE K/V server to fetch
   * the ads related metadata.
   */
  public abstract boolean getFetchAdsFromRetrievalService();

  public static ContextualProtectedAppSignalsData.Builder builder() {
    return new AutoValue_ContextualProtectedAppSignalsData.Builder()
        .setFetchAdsFromRetrievalService(false);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setAdRenderIds(@Nullable List<String> value);
    public abstract Builder setFetchAdsFromRetrievalService(boolean value);
    public abstract ContextualProtectedAppSignalsData build();
  }
}

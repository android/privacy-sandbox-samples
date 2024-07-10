package com.example.adservices.samples.fledge.WaterfallMediationHelpers;

import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.BID_FLOOR_SIGNALS_FORMAT;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.DECISION_URI_SUFFIX;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.DEFAULT_BASE_URI_FORMAT;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.SCORING_LOGIC_WITH_BID_FLOOR_JS;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.TAG;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.TRUSTED_SCORING_SIGNALS_URI_SUFFIX;

import android.adservices.adselection.AddAdSelectionOverrideRequest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionConfig;
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionOutcome;
import androidx.privacysandbox.ads.adservices.adselection.ReportImpressionRequest;
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals;
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier;

import com.example.adservices.samples.fledge.clients.AdSelectionClient;
import com.example.adservices.samples.fledge.clients.TestAdSelectionClient;
import com.example.adservices.samples.fledge.sampleapp.EventLogManager;
import com.example.adservices.samples.fledge.sampleapp.MainActivity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Represents an {@code Ad Network SDK's} wrapper class.
 *
 * <p>{@code An Ad Network} can run ad selection and reporting in FLEDGE
 *
 * <p>To allow an {@code Ad Network} to be included in a mediation flow as a participant(third-party
 * or 3P) {@code Ad Network} also needs to implement a wrapper class called {@code NetworkAdapter}.
 * {@link NetworkAdapter} class is implemented as a combination of {@code Ad Network's SDK} + {@code
 * Network adapter}
 *
 * <p>
 *
 * <p>This class is expected to be implemented by third-party SDKs participating in mediation flow.
 * {@link MediationSdk} (who orchestrates the mediation flow can load/import a and use it to run ad
 * selection and reporting for the owner Network SDK.
 */
@RequiresApi(api = 34)
public class NetworkAdapter {

    protected final String networkName;
    protected final boolean useOverrides;
    protected final Uri baseUri;
    protected final String uriFriendlyName;
    protected final String baseUriString;
    protected final AdSelectionClient adSelectionClient;
    protected final TestAdSelectionClient testAdSelectionClient;
    protected final EventLogManager eventLog;
    private final List<AdTechIdentifier> buyers;
    private final AdSelectionConfig adSelectionConfig;
    private final double bidFloor;

    protected NetworkAdapter(
            String networkName,
            AdTechIdentifier buyer,
            Uri baseUri,
            boolean useOverrides,
            Executor executor,
            Context context,
            EventLogManager eventLog) {
        // If bid floor is not given we set it to 0
        //  (i.e. Mediation SDK don't have the bid floor concept so we set bid floor to zero to let
        // any
        //  bid pass by scoring)
        this(networkName, buyer, 0.0, baseUri, useOverrides, executor, context, eventLog);
    }

    public NetworkAdapter(
            String networkName,
            AdTechIdentifier buyer,
            double bidFloor,
            Uri baseUri,
            boolean useOverrides,
            Executor executor,
            Context context,
            EventLogManager eventLog) {
        uriFriendlyName = Constants.uriFriendlyString(networkName);
        this.networkName = networkName;
        this.bidFloor = bidFloor;
        this.eventLog = eventLog;
        this.useOverrides = useOverrides;
        this.baseUri =
                (!useOverrides)
                        ? baseUri
                        : Uri.parse(String.format(DEFAULT_BASE_URI_FORMAT, uriFriendlyName));

        buyers = Collections.singletonList(buyer);
        baseUriString = String.format(DEFAULT_BASE_URI_FORMAT, uriFriendlyName);
        adSelectionConfig = prepareAdSelectionConfig();
        adSelectionClient = new AdSelectionClient(context);
        testAdSelectionClient =
                new TestAdSelectionClient.Builder()
                        .setContext(context)
                        .setExecutor(executor)
                        .build();
    }

    @SuppressLint("NewApi")
    public AdSelectionOutcome runAdSelection() {
        if (useOverrides) {
            addAdSelectionOverrides();
        }

        AdSelectionOutcome adSelectionOutcome;
        try {
            adSelectionOutcome =
                    adSelectionClient.selectAds(adSelectionConfig).get(10, TimeUnit.SECONDS);
            Log.i(TAG, networkName + " adSelection success!");
            Thread.sleep(1000);
        } catch (Exception e) {
            Log.e(MainActivity.TAG, "Exception running ad selection for " + networkName + " " + e);
            adSelectionOutcome = AdSelectionOutcome.NO_OUTCOME;
        }
        return adSelectionOutcome;
    }

    public void reportImpressions(Long adSelectionId) {
        ReportImpressionRequest request =
                new ReportImpressionRequest(adSelectionId, prepareAdSelectionConfig());
        try {
            adSelectionClient.reportImpression(request).get(10, TimeUnit.SECONDS);
            writeEvent("Report impression succeeded for %s", adSelectionId);
        } catch (Exception e) {
            writeEvent("Report impression failed: %s", e);
        }
    }

    public void resetAdSelectionOverrides() {
        testAdSelectionClient.resetAllAdSelectionConfigRemoteOverrides();
    }

    public AdSelectionSignals getBidFloorSignals() {
        return new AdSelectionSignals(String.format(BID_FLOOR_SIGNALS_FORMAT, this.bidFloor));
    }

    public double getBidFloor() {
        return bidFloor;
    }

    public String getNetworkName() {
        return networkName;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s - %s", networkName, bidFloor);
    }

    protected void writeEvent(String eventFormat, Object... args) {
        eventLog.writeEvent(String.format(eventFormat, args));
    }

    private void addAdSelectionOverrides() {
        try {
            testAdSelectionClient
                    .overrideAdSelectionConfigRemoteInfo(
                            new AddAdSelectionOverrideRequest(
                                    new android.adservices.adselection.AdSelectionConfig.Builder()
                                            .setSeller(
                                                    android.adservices.common.AdTechIdentifier
                                                            .fromString(
                                                                    adSelectionConfig
                                                                            .getSeller()
                                                                            .getIdentifier()))
                                            .setAdSelectionSignals(
                                                    android.adservices.common.AdSelectionSignals
                                                            .fromString(
                                                                    adSelectionConfig
                                                                            .getAdSelectionSignals()
                                                                            .getSignals()))
                                            // DO NOT SUBMIT until fixing this:
                                            // .setBuyerSignedContextualAds()
                                            // .setPerBuyerSignals()
                                            .setCustomAudienceBuyers(
                                                    adSelectionConfig
                                                            .getCustomAudienceBuyers()
                                                            .stream()
                                                            .map(
                                                                    adTechIdentifier ->
                                                                            android.adservices
                                                                                    .common
                                                                                    .AdTechIdentifier
                                                                                    .fromString(
                                                                                            adTechIdentifier
                                                                                                    .getIdentifier()))
                                                            .collect(Collectors.toList()))
                                            .setDecisionLogicUri(
                                                    adSelectionConfig.getDecisionLogicUri())
                                            .setSellerSignals(
                                                    android.adservices.common.AdSelectionSignals
                                                            .fromString(
                                                                    adSelectionConfig
                                                                            .getSellerSignals()
                                                                            .getSignals()))
                                            .setTrustedScoringSignalsUri(
                                                    adSelectionConfig.getTrustedScoringSignalsUri())
                                            .build(),
                                    String.format(SCORING_LOGIC_WITH_BID_FLOOR_JS, uriFriendlyName),
                                    android.adservices.common.AdSelectionSignals.EMPTY))
                    .get(10, TimeUnit.SECONDS);
            Log.i(TAG, networkName + " adSelection overrides success!");
            writeEvent("Adds AdSelectionConfig overrides");

        } catch (Exception e) {
            Log.e(MainActivity.TAG, "Exception adding overrides for " + networkName + ": " + e);
        }
    }

    private AdSelectionConfig prepareAdSelectionConfig() {
        return new AdSelectionConfig(
                new AdTechIdentifier(Objects.requireNonNull(getDecisionLogicUri().getHost())),
                getDecisionLogicUri(),
                buyers,
                new AdSelectionSignals("{}"),
                getSellerSignals(),
                buyers.stream()
                        .collect(
                                Collectors.toMap(
                                        buyer -> buyer, buyer -> new AdSelectionSignals("{}"))),
                getTrustedScoringUri());
    }

    private Uri getDecisionLogicUri() {
        return baseUri.buildUpon().appendPath(DECISION_URI_SUFFIX).build();
    }

    private Uri getTrustedScoringUri() {
        return baseUri.buildUpon().appendPath(TRUSTED_SCORING_SIGNALS_URI_SUFFIX).build();
    }

    private AdSelectionSignals getSellerSignals() {
        return new AdSelectionSignals(String.format(BID_FLOOR_SIGNALS_FORMAT, bidFloor));
    }
}

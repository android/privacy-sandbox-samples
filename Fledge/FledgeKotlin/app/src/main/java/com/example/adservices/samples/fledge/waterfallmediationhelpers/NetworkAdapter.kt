package com.example.adservices.samples.fledge.waterfallmediationhelpers

import androidx.annotation.RequiresApi
import android.adservices.common.AdTechIdentifier
import android.adservices.common.AdSelectionSignals
import com.example.adservices.samples.fledge.sampleapp.EventLogManager
import android.adservices.adselection.AdSelectionOutcome
import android.adservices.adselection.AdSelectionConfig
import com.example.adservices.samples.fledge.clients.AdSelectionClient
import com.example.adservices.samples.fledge.clients.TestAdSelectionClient
import android.adservices.adselection.ReportImpressionRequest
import android.adservices.adselection.AddAdSelectionOverrideRequest
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.adservices.samples.fledge.waterfallmediationhelpers.Constants.TAG
import java.lang.Exception
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

/**
 * Represents an `Ad Network SDK's` wrapper class.
 *
 *
 * `An Ad Network` can run ad selection and reporting in FLEDGE
 *
 *
 * To allow an `Ad Network` to be included in a mediation flow as a participant(third-party
 * or 3P) `Ad Network` also needs to implement a wrapper class called `NetworkAdapter`.
 * [NetworkAdapter] class is implemented as a combination of `Ad Network's SDK` + `Network adapter`
 *
 *
 *
 *
 * This class is expected to be implemented by third-party SDKs participating in mediation flow.
 * [MediationSdk] (who orchestrates the mediation flow can load/import a and use it to run ad
 * selection and reporting for the owner Network SDK.
 */
@RequiresApi(api = 34)
open class NetworkAdapter(
  networkName: String,
  buyer: AdTechIdentifier?,
  bidFloor: Double,
  baseUri: Uri,
  useOverrides: Boolean,
  executor: Executor?,
  context: Context?,
  eventLog: EventLogManager
) {
  private val buyers: List<AdTechIdentifier>
  private val adSelectionConfig: AdSelectionConfig
  val bidFloor: Double
  val networkName: String
  protected val useOverrides: Boolean
  protected val baseUri: Uri
  protected val adSelectionClient: AdSelectionClient
  protected val testAdSelectionClient: TestAdSelectionClient
  private val uriFriendlyName: String
  private val baseUriString: String
  private val eventLog: EventLogManager

  protected constructor(
    networkName: String,
    buyer: AdTechIdentifier,
    baseUri: Uri,
    useOverrides: Boolean,
    executor: Executor,
    context: Context,
    eventLog: EventLogManager
  ) : this(networkName, buyer, 0.0, baseUri, useOverrides, executor, context, eventLog) {
    // If bid floor is not given we set it to 0
    //  (i.e. Mediation SDK don't have the bid floor concept so we set bid floor to zero to let any
    //  bid pass by scoring)
  }

  init {
    uriFriendlyName = Constants.uriFriendlyString(networkName)
    this.networkName = networkName
    this.bidFloor = bidFloor
    this.eventLog = eventLog
    this.useOverrides = useOverrides
    this.baseUri =
      if (!useOverrides) baseUri else Uri.parse(String.format(Constants.DEFAULT_BASE_URI_FORMAT,
                                                              uriFriendlyName))
    buyers = listOf(buyer!!)
    baseUriString = String.format(Constants.DEFAULT_BASE_URI_FORMAT, uriFriendlyName)
    adSelectionConfig = prepareAdSelectionConfig()
    adSelectionClient = AdSelectionClient.Builder()
      .setContext(context!!)
      .setExecutor(executor!!)
      .build()
    testAdSelectionClient = TestAdSelectionClient.Builder()
      .setContext(context)
      .setExecutor(executor)
      .build()
  }

  fun runAdSelection(): AdSelectionOutcome {
    if (useOverrides) {
      addAdSelectionOverrides()
    }
    var adSelectionOutcome: AdSelectionOutcome
    try {
      adSelectionOutcome = adSelectionClient.selectAds(adSelectionConfig)[10, TimeUnit.SECONDS]!!
      Log.i(TAG, "$networkName adSelection success!")
      Thread.sleep(1000)
    } catch (e: Exception) {
      Log.e(TAG, "Exception running ad selection for $networkName $e")
      adSelectionOutcome = AdSelectionOutcome.NO_OUTCOME
    }
    return adSelectionOutcome
  }

  fun reportImpressions(adSelectionId: Long?) {
    val request = ReportImpressionRequest(adSelectionId!!, prepareAdSelectionConfig())
    try {
      adSelectionClient.reportImpression(request)[10, TimeUnit.SECONDS]
      writeEvent("Report impression succeeded for %s", adSelectionId)
    } catch (e: Exception) {
      writeEvent("Report impression failed: %s", e)
    }
  }

  open fun resetAdSelectionOverrides() {
    testAdSelectionClient.resetAllAdSelectionConfigRemoteOverrides()
  }

  val bidFloorSignals: AdSelectionSignals
    get() = AdSelectionSignals.fromString(String.format(Constants.BID_FLOOR_SIGNALS_FORMAT,
                                                        bidFloor))

  override fun toString(): String {
    return String.format("%s - %s", networkName, bidFloor)
  }

  protected fun writeEvent(eventFormat: String?, vararg args: Any?) {
    eventLog.writeEvent(String.format(eventFormat!!, *args))
  }

  private fun addAdSelectionOverrides() {
    try {
      testAdSelectionClient.overrideAdSelectionConfigRemoteInfo(
        AddAdSelectionOverrideRequest(
          adSelectionConfig,
          String.format(Constants.SCORING_LOGIC_WITH_BID_FLOOR_JS, uriFriendlyName),
          AdSelectionSignals.EMPTY))!![10, TimeUnit.SECONDS]
      Log.i(Constants.TAG, "$networkName adSelection overrides success!")
      writeEvent("Adds AdSelectionConfig overrides")
    } catch (e: Exception) {
      Log.e(TAG, "Exception adding overrides for $networkName: $e")
    }
  }

  private fun prepareAdSelectionConfig(): AdSelectionConfig {
    return AdSelectionConfig.Builder()
      .setSeller(AdTechIdentifier.fromString(decisionLogicUri.host!!))
      .setDecisionLogicUri(decisionLogicUri)
      .setCustomAudienceBuyers(buyers)
      .setAdSelectionSignals(AdSelectionSignals.EMPTY)
      .setSellerSignals(sellerSignals)
      .setPerBuyerSignals(buyers.stream()
                            .collect(Collectors.toMap(
                              { buyer: AdTechIdentifier? -> buyer },
                              { buyer: AdTechIdentifier? -> AdSelectionSignals.EMPTY })))
      .setTrustedScoringSignalsUri(trustedScoringUri)
      .build()
  }

  private val decisionLogicUri: Uri
    get() = baseUri.buildUpon().appendPath(Constants.DECISION_URI_SUFFIX).build()
  private val trustedScoringUri: Uri
    get() = baseUri.buildUpon().appendPath(Constants.TRUSTED_SCORING_SIGNALS_URI_SUFFIX)
      .build()
  private val sellerSignals: AdSelectionSignals
    get() = AdSelectionSignals.fromString(String.format(Constants.BID_FLOOR_SIGNALS_FORMAT,
                                                                bidFloor))
}
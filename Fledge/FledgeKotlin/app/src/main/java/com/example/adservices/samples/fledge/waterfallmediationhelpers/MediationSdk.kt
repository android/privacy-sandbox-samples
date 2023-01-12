package com.example.adservices.samples.fledge.waterfallmediationhelpers

import android.adservices.adselection.AdSelectionFromOutcomesConfig
import android.adservices.adselection.AdSelectionOutcome
import android.adservices.adselection.AddAdSelectionFromOutcomesOverrideRequest
import android.adservices.common.AdSelectionSignals
import android.adservices.common.AdTechIdentifier
import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Pair
import androidx.annotation.RequiresApi
import com.example.adservices.samples.fledge.waterfallmediationhelpers.Constants.TAG
import com.example.adservices.samples.fledge.sampleapp.EventLogManager
import com.example.adservices.samples.fledge.sampleapp.databinding.WaterfallMediationActivityBinding
import com.google.common.base.Joiner
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * Represents an Ad Network SDK's wrapper class that can run Waterfall Mediation flow.
 *
 *
 * `An Ad Network` who is `a Mediation SDK` (first-party or 3P) can run mediation
 * flows on FLEDGE additional to ad selection and reporting.
 *
 *
 * `Mediation SDK` uses [NetworkAdapter]s of participating SDKs (third-party or 3P
 * SDKs) to trigger their ad selection and reporting flows.
 *
 *
 * This class is expected to be implemented by SDKs who are willing to run mediation flows.
 */
@RequiresApi(api = 34)
class MediationSdk(
  networkName: String,
  buyer: AdTechIdentifier,
  baseUri: Uri,
  useOverrides: Boolean,
  executor: Executor,
  context: Context,
  private val binding: WaterfallMediationActivityBinding,
  eventLog: EventLogManager,
  private val useOnlyAdditionalIds: Boolean
) : NetworkAdapter(networkName, buyer, baseUri, useOverrides, executor, context, eventLog) {
  @Throws(Exception::class)
  fun orchestrateMediation(mediationChain: List<NetworkAdapter>): Pair<AdSelectionOutcome, NetworkAdapter> {
    writeEvent("Mediation chain:\n%s", Joiner.on("\n").skipNulls().join(mediationChain))
    val outcome1p = runAdSelection()
    if (outcome1p.hasOutcome()) {
      writeEvent("%s auction result (1P ad): %s", networkName, outcome1p.adSelectionId)
    } else {
      writeEvent("%s auction not returned an ad. No 1P ad. Will iterate the chain without 1P ad anyways.",
                 networkName)
    }
    var outcome: AdSelectionOutcome
    for (network3p in mediationChain) {
      if (outcome1p.hasOutcome()) {
        writeEvent("Try to place %s before %s", networkName, network3p.networkName)
        if (runSelectOutcome(outcome1p, network3p).also { outcome = it }.hasOutcome()) {
          writeEvent("%s placed before! 1P ad wins: %s", networkName, outcome.adSelectionId)
          return Pair(outcome, this)
        }
        writeEvent("%s isn't placed before %s, running ad selection for it!",
                   networkName,
                   network3p.networkName)
      } else {
        writeEvent("No 1P ad, continue without comparing")
      }
      if (network3p.runAdSelection().also { outcome = it }.hasOutcome()) {
        writeEvent("%s's auction returned an ad: Winner: %s",
                   network3p.networkName,
                   outcome.adSelectionId)
        return Pair(outcome, network3p)
      }
      writeEvent("%s's auction not returned an ad. Moving to the next network",
                 network3p.networkName)
    }
    return Pair(outcome1p, this)
  }

  @Throws(Exception::class)
  fun runSelectOutcome(
    outcome1p: AdSelectionOutcome,
    network3p: NetworkAdapter
  ): AdSelectionOutcome {
    val config = prepareWaterfallConfig(outcome1p.adSelectionId, network3p.bidFloorSignals)
    if (useOverrides) {
      addAdSelectionFromOutcomesOverride(config)
    }
    val result: AdSelectionOutcome
    try {
      result = adSelectionClient.selectAds(config)[10, TimeUnit.SECONDS]!!
      Thread.sleep(1000)
    } catch (e: Exception) {
      Log.e(TAG, "Exception calling selectAds(AdSelectionFromOutcomesConfig)", e)
      throw e
    }
    return result
  }

  private fun addAdSelectionFromOutcomesOverride(config: AdSelectionFromOutcomesConfig) {
    try {
      testAdSelectionClient
        .overrideAdSelectionFromOutcomesConfigRemoteInfo(
          AddAdSelectionFromOutcomesOverrideRequest(config, Constants.WATERFALL_MEDIATION_LOGIC_JS,
                                                    AdSelectionSignals.EMPTY))
        .get(10, TimeUnit.SECONDS)
      Log.i(TAG, "$networkName adSelection overrides success!")
      writeEvent("Adds AdSelectionFromOutcomesConfig overrides")
    } catch (e: Exception) {
      Log.e(TAG, "Exception adding overrides for $networkName: $e")
    }
  }

  private fun prepareWaterfallConfig(
    outcome1pId: Long,
    bidFloorSignals: AdSelectionSignals?
  ): AdSelectionFromOutcomesConfig {
    // inject a flag to run only with "Additional ad selection ids from the UX"
    val outcomeIds: MutableList<Long> = ArrayList()
    writeEvent("useOnlyAdditionalIds flag: $useOnlyAdditionalIds")
    if (!useOnlyAdditionalIds) outcomeIds.add(outcome1pId)
    outcomeIds.addAll(additionalIdOrNothing)
    return AdSelectionFromOutcomesConfig.Builder()
      .setSeller(AdTechIdentifier.fromString(selectionLogicUri.host!!))
      .setAdSelectionIds(outcomeIds)
      .setSelectionSignals(bidFloorSignals!!)
      .setSelectionLogicUri(selectionLogicUri)
      .build()
  }

  override fun resetAdSelectionOverrides() {
    super.resetAdSelectionOverrides()
    testAdSelectionClient.resetAllAdSelectionFromOutcomesConfigRemoteOverrides()
  }

  private val additionalIdOrNothing: List<Long>
    get() {
      var additionalIdText: String
      return if (binding.adSelectionIdsToInclude.text.toString().also { additionalIdText = it }.isNotEmpty()) {
        val additionalIds: MutableList<Long> = ArrayList()
        for (longIdString in additionalIdText.split(",").toTypedArray()) {
          additionalIds.add(longIdString.replace(" ", "").toLong())
        }
        additionalIds
      } else {
        emptyList()
      }
    }
  private val selectionLogicUri: Uri
    get() = baseUri.buildUpon().appendPath(Constants.OUTCOME_SELECTION_URI_SUFFIX).build()
}
package com.example.adservices.samples.fledge.sampleapp

import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.adservices.samples.fledge.clients.CustomAudienceClient
import com.example.adservices.samples.fledge.clients.TestCustomAudienceClient
import android.os.Bundle
import com.example.adservices.samples.fledge.waterfallmediationhelpers.CustomAudienceHelper
import com.example.adservices.samples.fledge.waterfallmediationhelpers.NetworkAdapter
import com.example.adservices.samples.fledge.waterfallmediationhelpers.MediationSdk
import android.adservices.adselection.AdSelectionOutcome
import android.adservices.common.AdTechIdentifier
import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.TextView
import android.widget.EditText
import com.example.adservices.samples.fledge.waterfallmediationhelpers.Constants
import com.example.adservices.samples.fledge.sampleapp.databinding.WaterfallMediationActivityBinding
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.ArrayList
import java.util.Comparator
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Predicate
import java.util.stream.Collectors

/**
 * Waterfall Mediation Activity class that takes the input from user and demonstrated Waterfall
 * Mediation.
 *
 *
 * Each network needs to have both the `bid` and the `bid floor` to be able to
 * participate in mediation. If not then [android.adservices.customaudience.CustomAudience]
 * creation is skipped and that network will not be included in the `mediation chain`
 *
 */
@RequiresApi(api = 34)
class WaterfallMediationActivity : AppCompatActivity() {
  private var customAudienceClient: CustomAudienceClient? = null
  private var testCustomAudienceClient: TestCustomAudienceClient? = null
  private var binding: WaterfallMediationActivityBinding? = null
  private var eventLog: EventLogManager? = null
  private var executor: Executor? = null
  private var context: Context? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    executor = Executors.newCachedThreadPool()
    context = applicationContext
    binding = WaterfallMediationActivityBinding.inflate(
      layoutInflater)
    setContentView(binding!!.root)
    eventLog = EventLogManager(binding!!.eventLog)
    customAudienceClient =
      CustomAudienceClient.Builder().setContext(context!!).setExecutor(executor!!).build()
    testCustomAudienceClient =
      TestCustomAudienceClient.Builder().setContext(context!!).setExecutor(executor!!).build()
    binding!!.runWaterfallMediationButton.setOnClickListener { l: View? -> buttonOnClickRunWaterfallMediation() }
  }

  private fun buttonOnClickRunWaterfallMediation() {
    eventLog!!.flush()
    binding!!.adSpace.text = ""
    if (!Constants.hasTextNotEmpty(
        binding!!.network1pBid)
    ) {
      val errorMessage =
        "Mediation SDK has to have a valid bid. If you want to Mediation SDK not to find any ads then enter -1"
      writeEvent(errorMessage)
      throw IllegalStateException(errorMessage)
    }
    try {
      val caHelper = CustomAudienceHelper(
        customAudienceClient!!,
        testCustomAudienceClient!!)
      val mediationChain = configureMediationChain(caHelper)
      val mediationSdk = configureMediationSdk(caHelper)
      val winnerOutcomeAndNetwork = mediationSdk.orchestrateMediation(mediationChain)
      notifyOfResults(winnerOutcomeAndNetwork)
      resetAllOverrides(mediationSdk, mediationChain)
    } catch (e: Exception) {
      Log.e(Constants.TAG, "Mediation orchestration failed: $e")
      writeEvent("Error during mediation: %s", e.cause!!)
      binding!!.adSpace.text = getString(R.string.no_ad_found)
    }
  }

  private fun notifyOfResults(winner: Pair<AdSelectionOutcome, NetworkAdapter>) {
    if (winner.first.hasOutcome()) {
      writeEvent("Winner ad selection id is %s from %s",
                 winner.first.adSelectionId,
                 winner.second.networkName)
      binding!!.adSpace.text = String.format("Would display ad from %s", winner.first.renderUri)
      winner.second.reportImpressions(winner.first.adSelectionId)
    } else {
      writeEvent("No ad is found")
      binding!!.adSpace.text = getString(R.string.no_ad_found)
    }
  }

  private fun configureMediationChain(caHelper: CustomAudienceHelper): List<NetworkAdapter> {
    val requestList: MutableList<NetworkConfigurationRequest> = ArrayList()
    requestList.add(NetworkConfigurationRequest(binding!!.networkA,
                                                binding!!.networkABid,
                                                binding!!.networkABidFloor))
    requestList.add(NetworkConfigurationRequest(binding!!.networkB,
                                                binding!!.networkBBid,
                                                binding!!.networkBBidFloor))
    return requestList.stream()
      .filter { obj: NetworkConfigurationRequest -> obj.isEligibleToParticipate }
      .map { e: NetworkConfigurationRequest -> configureNetworkAdapter(caHelper, e) }
      .sorted(Comparator.comparing { obj: NetworkAdapter -> obj.bidFloor }
                .reversed())
      .collect(Collectors.toList())
  }

  private fun configureNetworkAdapter(
    caHelper: CustomAudienceHelper, request: NetworkConfigurationRequest
  ): NetworkAdapter {
    val buyer = caHelper.configureCustomAudience(request.buyerName,
                                                 request.networkBid,
                                                 request.baseUriOrNull,
                                                 request.useOverrides())
    return createNetworkAdapter(request.networkName,
                                buyer,
                                request.networkBidFloor,
                                request.baseUriOrNull,
                                request.useOverrides())
  }

  private fun configureMediationSdk(caHelper: CustomAudienceHelper): MediationSdk {
    val request = NetworkConfigurationRequest(binding!!.network1p, binding!!.network1pBid, null)
    val buyer = caHelper.configureCustomAudience(request.buyerName,
                                                 request.networkBid,
                                                 request.baseUriOrNull,
                                                 request.useOverrides())
    return createMediationSdk(request.networkName,
                              buyer,
                              request.baseUriOrNull,
                              request.useOverrides())
  }

  private fun createNetworkAdapter(
    networkName: String,
    buyer: AdTechIdentifier,
    bidFloor: Double,
    baseUri: Uri,
    useOverrides: Boolean
  ): NetworkAdapter {
    return NetworkAdapter(networkName,
                          buyer,
                          bidFloor,
                          baseUri,
                          useOverrides,
                          executor,
                          context,
                          eventLog!!)
  }

  private fun createMediationSdk(
    networkName: String,
    buyer: AdTechIdentifier,
    baseUri: Uri,
    useOverrides: Boolean
  ): MediationSdk {
    return MediationSdk(networkName,
                        buyer,
                        baseUri,
                        useOverrides,
                        executor!!,
                        context!!,
                        binding!!,
                        eventLog!!,
                        java.lang.Boolean.parseBoolean(getIntentOrNull(
                          Constants.USE_ONLY_ADDITIONAL_IDS_INTENT)))
  }

  private fun resetAllOverrides(mediationSdk: MediationSdk, mediationChain: List<NetworkAdapter>) {
    testCustomAudienceClient!!.resetAllCustomAudienceOverrides()
    mediationSdk.resetAdSelectionOverrides()
    for (networkAdapter in mediationChain) {
      networkAdapter.resetAdSelectionOverrides()
    }
  }

  private fun writeEvent(eventFormat: String, vararg args: Any) {
    eventLog!!.writeEvent(String.format(eventFormat, *args))
  }

  fun getIntentOrNull(intent: String?): String? {
    return getIntent().getStringExtra(intent)
  }

  internal inner class NetworkConfigurationRequest(
    var name: TextView,
    var bid: EditText?,
    var bidFloor: EditText?
  ) {
    var useOverrides = false
    val isEligibleToParticipate: Boolean
      get() = Constants.hasTextNotEmpty(bid) && Constants.hasTextNotEmpty(bidFloor)

    val baseUriOrNull: Uri
      get() {
        val toReturn = getIntentOrNull(Constants.toCamelCase(networkName))
        useOverrides = toReturn == null
        return if (useOverrides) Uri.EMPTY else Uri.parse(toReturn)
      }

    fun useOverrides(): Boolean {
      return useOverrides
    }

    val buyerName: String
      get() = Constants.getBuyerNameFromTextView(name)

    val networkName: String
      get() = Constants.getNetworkNameFromTextView(name)

    val networkBid: Double
      get() = Constants.getDoubleFromEditText(bid!!)  // if not eligible shouldn't hit this

    val networkBidFloor: Double
      get() = Constants.getDoubleFromEditText(bidFloor!!) // if not eligible shouldn't hit this
  }
}
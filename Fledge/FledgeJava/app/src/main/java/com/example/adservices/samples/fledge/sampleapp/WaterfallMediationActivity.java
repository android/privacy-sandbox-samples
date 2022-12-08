package com.example.adservices.samples.fledge.sampleapp;

import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.TAG;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.USE_ONLY_ADDITIONAL_IDS_INTENT;

import android.adservices.adselection.AdSelectionOutcome;
import android.adservices.common.AdTechIdentifier;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants;
import com.example.adservices.samples.fledge.WaterfallMediationHelpers.CustomAudienceHelper;
import com.example.adservices.samples.fledge.WaterfallMediationHelpers.MediationSdk;
import com.example.adservices.samples.fledge.WaterfallMediationHelpers.NetworkAdapter;
import com.example.adservices.samples.fledge.clients.CustomAudienceClient;
import com.example.adservices.samples.fledge.clients.TestCustomAudienceClient;
import com.example.adservices.samples.fledge.sampleapp.databinding.WaterfallMediationActivityBinding;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Waterfall Mediation Activity class that takes the input from user and demonstrated Waterfall
 * Mediation.
 *
 * <p>Each network needs to have both the {@code bid} and the {@code bid floor} to be able to
 * participate in mediation. If not then {@link android.adservices.customaudience.CustomAudience}
 * creation is skipped and that network will not be included in the {@code mediation chain}</p>
 *
 */
@RequiresApi(api = 34)
public class WaterfallMediationActivity extends AppCompatActivity {

  private CustomAudienceClient customAudienceClient;
  private TestCustomAudienceClient testCustomAudienceClient;
  private WaterfallMediationActivityBinding binding;
  private EventLogManager eventLog;
  private Executor executor;
  private Context context;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    executor = Executors.newCachedThreadPool();
    context = getApplicationContext();
    binding = WaterfallMediationActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    eventLog = new EventLogManager(binding.eventLog);

    customAudienceClient = new CustomAudienceClient.Builder().setContext(context).setExecutor(executor).build();
    testCustomAudienceClient = new TestCustomAudienceClient.Builder().setContext(context).setExecutor(executor).build();

    binding.runWaterfallMediationButton.setOnClickListener((l) -> buttonOnClickRunWaterfallMediation());
  }

  private void buttonOnClickRunWaterfallMediation() {
    eventLog.flush();
    binding.adSpace.setText("");
    if (!Constants.hasTextNotEmpty(binding.network1pBid)) {
      String errorMessage = "Mediation SDK has to have a valid bid. If you want to Mediation SDK not to find any ads then enter -1";
      writeEvent(errorMessage);
      throw new IllegalStateException(errorMessage);
    }

    try {
      CustomAudienceHelper caHelper = new CustomAudienceHelper(
          customAudienceClient,
          testCustomAudienceClient);

      List<NetworkAdapter> mediationChain = configureMediationChain(caHelper);
      MediationSdk mediationSdk = configureMediationSdk(caHelper);

      Pair<AdSelectionOutcome, NetworkAdapter> winnerOutcomeAndNetwork =
          mediationSdk.orchestrateMediation(mediationChain);

      notifyOfResults(winnerOutcomeAndNetwork);

      resetAllOverrides(mediationSdk, mediationChain);
    } catch (Exception e) {
      Log.e(TAG, "Mediation orchestration failed: " + e);
      writeEvent("Error during mediation: %s", e.getCause());
      binding.adSpace.setText(getString(R.string.no_ad_found));
    }
  }

  private void notifyOfResults(Pair<AdSelectionOutcome, NetworkAdapter> winner) {
    if (winner.first.hasOutcome()) {
      writeEvent("Winner ad selection id is %s from %s", winner.first.getAdSelectionId(), winner.second.getNetworkName());
      binding.adSpace.setText(String.format("Would display ad from %s", winner.first.getRenderUri()));
      winner.second.reportImpressions(winner.first.getAdSelectionId());
    } else {
      writeEvent("No ad is found");
      binding.adSpace.setText(getString(R.string.no_ad_found));
    }
  }

  private List<NetworkAdapter> configureMediationChain(CustomAudienceHelper caHelper) {
    List<NetworkConfigurationRequest> requestList = new ArrayList<>();
    requestList.add(new NetworkConfigurationRequest(binding.networkA, binding.networkABid, binding.networkABidFloor));
    requestList.add(new NetworkConfigurationRequest(binding.networkB, binding.networkBBid, binding.networkBBidFloor));
    requestList.add(new NetworkConfigurationRequest(binding.networkC, binding.networkCBid, binding.networkCBidFloor));

    return requestList.stream()
        .filter(NetworkConfigurationRequest::isEligibleToParticipate)
        .map(e -> configureNetworkAdapter(caHelper, e))
        .sorted(Comparator.comparing(NetworkAdapter::getBidFloor).reversed())
        .collect(Collectors.toList());
  }

  private NetworkAdapter configureNetworkAdapter(
      CustomAudienceHelper caHelper, NetworkConfigurationRequest request) {
    AdTechIdentifier buyer = caHelper.configureCustomAudience(request.getBuyerName(), request.getBid(), request.getBaseUriOrNull(), request.useOverrides());
    return createNetworkAdapter(request.getNetworkName(), buyer, request.getBidFloor(), request.getBaseUriOrNull(), request.useOverrides());
  }

  private MediationSdk configureMediationSdk(CustomAudienceHelper caHelper) {
    NetworkConfigurationRequest request = new NetworkConfigurationRequest(binding.network1p, binding.network1pBid, null);
    AdTechIdentifier buyer = caHelper.configureCustomAudience(request.getBuyerName(), request.getBid(), request.getBaseUriOrNull(), request.useOverrides());
    return createMediationSdk(request.getNetworkName(), buyer, request.getBaseUriOrNull(), request.useOverrides());
  }

  private NetworkAdapter createNetworkAdapter(String networkName, AdTechIdentifier buyer, double bidFloor, Uri baseUri, boolean useOverrides) {
    return new NetworkAdapter(networkName, buyer, bidFloor, baseUri, useOverrides, executor, context, eventLog);
  }

  private MediationSdk createMediationSdk(String networkName, AdTechIdentifier buyer, Uri baseUri, boolean useOverrides) {
    return new MediationSdk(networkName, buyer, baseUri, useOverrides, executor, context, binding, eventLog, Boolean.parseBoolean(getIntentOrNull(USE_ONLY_ADDITIONAL_IDS_INTENT)));
  }

  private void resetAllOverrides(MediationSdk mediationSdk, List<NetworkAdapter> mediationChain) {
    testCustomAudienceClient.resetAllCustomAudienceOverrides();
    mediationSdk.resetAdSelectionOverrides();
    for (NetworkAdapter networkAdapter: mediationChain) {
      networkAdapter.resetAdSelectionOverrides();
    }
  }

  private void writeEvent(String eventFormat, Object... args) {
    eventLog.writeEvent(String.format(eventFormat, args));
  }

  public String getIntentOrNull(String intent) {
    return getIntent().getStringExtra(intent);
  }

  class NetworkConfigurationRequest {
    TextView networkName;
    EditText networkBid;
    EditText networkBidFloor;
    boolean useOverrides;

    public NetworkConfigurationRequest(TextView networkName, EditText networkBid, EditText networkBidFloor) {
      this.networkName = networkName;
      this.networkBid = networkBid;
      this.networkBidFloor = networkBidFloor;
    }

    private boolean isEligibleToParticipate() {
      return Constants.hasTextNotEmpty(networkBid) && Constants.hasTextNotEmpty(networkBidFloor);
    }

    private Uri getBaseUriOrNull() {
      String toReturn = getIntentOrNull(Constants.toCamelCase(getNetworkName()));
      useOverrides = toReturn == null;
      return (useOverrides) ? Uri.EMPTY : Uri.parse(toReturn);
    }

    private boolean useOverrides() {
      return useOverrides;
    }

    private String getBuyerName() {
      return Constants.getBuyerNameFromTextView(networkName);
    }

    private String getNetworkName() {
      return Constants.getNetworkNameFromTextView(networkName);
    }

    private Double getBid() {
      return Constants.getDoubleFromEditText(networkBid);
    }

    private Double getBidFloor() {
      return Constants.getDoubleFromEditText(networkBidFloor);
    }
  }
}

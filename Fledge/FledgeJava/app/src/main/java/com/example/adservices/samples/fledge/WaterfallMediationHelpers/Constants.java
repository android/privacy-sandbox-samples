package com.example.adservices.samples.fledge.WaterfallMediationHelpers;

import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import com.google.common.base.CaseFormat;

@RequiresApi(api = 34)
public class Constants {

  // Hacky
  public static final String USE_ONLY_ADDITIONAL_IDS_INTENT = "useOnlyAdditionalIds";

  // Common
  public static final String TAG = "FledgeSample";
  public static final String DEFAULT_BASE_URI_FORMAT = "https://%s.com/";

  // Seller-side URIs
  public static final String DECISION_URI_SUFFIX = "scoring"; // calls real servers if specified
  public static final String TRUSTED_SCORING_SIGNALS_URI_SUFFIX = "scoring/trusted";
  public static final String OUTCOME_SELECTION_URI_SUFFIX = "waterfall_mediation"; // calls real servers if specified

  // Buyer-side URIs
  public static final String BIDDING_URI_SUFFIX = "bidding"; // calls real servers if specified
  public static final String DAILY_URI_SUFFIX = "bidding/daily";
  public static final String TRUSTED_BIDDING_URI_SUFFIX = "bidding/trusted"; // calls real servers if specified
  public static final String RENDER_URI_SUFFIX = "render";

  // Signals data contracts
  public static final String BID_FLOOR = "bid_floor";
  public static final String BID = "bid";
  public static final String BID_FLOOR_SIGNALS_FORMAT = "{" + BID_FLOOR + ":%s}";
  public static final String BID_SIGNALS_FORMAT = "{" + BID + ":%s}";

  public static final String BIDDING_LOGIC_JS =
      "function generateBid(ad, auction_signals, per_buyer_signals, trusted_bidding_signals, contextual_signals, user_signals, custom_audience_signals) {\n"
          + "    return {'status': 0, 'ad': ad, 'bid': user_signals.user_bidding_signals." + BID + "};\n"
          + "}\n"
          + "function reportWin(ad_selection_signals, per_buyer_signals, signals_for_buyer, contextual_signals, custom_audience_signals) {\n"
          + "  let reporting_address = 'www.test.com/reportingTo_%s';\n"
          + "  return {'status': 0, 'results': {'reporting_uri': reporting_address + '?ca=' + custom_audience_signals.name} };\n"
          + "}";
  public static final String SCORING_LOGIC_WITH_BID_FLOOR_JS =
      "function scoreAd(ad, bid, auction_config, seller_signals, trusted_scoring_signals,\n"
          + "    contextual_signal, user_signal, custom_audience_signal) {\n"
          + "    return {'status': 0, 'score': (bid >= seller_signals." + BID_FLOOR + ") ? bid : -1 };\n"
          + "}\n"
          + "function reportResult(ad_selection_config, render_uri, bid, contextual_signals) {\n"
          + "    // Add the address of your reporting server here\n"
          + "    let reporting_address = 'www.test.com/reportingTo%s';\n"
          + "    return {'status': 0, 'results': {'signals_for_buyer': '{\"signals_for_buyer\" : 1}'\n"
          + "            , 'reporting_uri': reporting_address + '?render_uri='\n"
          + "                + render_uri + '?bid=' + bid }};\n"
          + "}";
  public static final String WATERFALL_MEDIATION_LOGIC_JS =
      "function selectOutcome(outcomes, selection_signals) {\n"
          + "    const outcome_1p = outcomes[0];\n"
          + "    const bid_floor = selection_signals." + BID_FLOOR + ";\n"
          + "    return {'status': 0, 'result': (outcome_1p.bid >= bid_floor) ? outcome_1p : null};\n"
          + "}";

  public static double getDoubleFromEditText(EditText editText) {
    return Double.parseDouble(editText.getText().toString());
  }

  public static String getNetworkNameFromTextView(TextView textView) {
    return textView.getText().toString().split("\n")[0];
  }

  public static String getBuyerNameFromTextView(TextView textView) {
    return textView.getText().toString()
        .split("\n")[1]
        .split(" ")[1]
        .replace(")", "");
  }

  public static boolean hasTextNotEmpty(TextView textView) {
    return stringNotNullAndNotEmpty(textView.getText().toString());
  }

  public static boolean stringNotNullAndNotEmpty(String str) {
    return str != null && !str.isEmpty();
  }

  public static String uriFriendlyString(String str) {
    return str.toLowerCase().replace(" ", "");
  }

  public static String toCamelCase(String str) {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str.replace(" ", ""));
  }
}

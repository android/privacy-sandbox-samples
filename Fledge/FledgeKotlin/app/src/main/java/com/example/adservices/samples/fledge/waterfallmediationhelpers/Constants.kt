package com.example.adservices.samples.fledge.waterfallmediationhelpers

import androidx.annotation.RequiresApi
import android.widget.EditText
import android.widget.TextView
import com.google.common.base.CaseFormat
import java.util.Locale

@RequiresApi(api = 34)
object Constants {
  // Hacky
  const val USE_ONLY_ADDITIONAL_IDS_INTENT = "useOnlyAdditionalIds"

  // Common
  const val TAG = "FledgeSample"
  const val DEFAULT_BASE_URI_FORMAT = "https://%s.com/"

  // Seller-side URIs
  const val DECISION_URI_SUFFIX = "scoring" // calls real servers if specified
  const val TRUSTED_SCORING_SIGNALS_URI_SUFFIX = "trustedScoringSignals"
  const val OUTCOME_SELECTION_URI_SUFFIX = "waterfall_mediation" // calls real servers if specified

  // Buyer-side URIs
  const val BIDDING_URI_SUFFIX = "bidding" // calls real servers if specified
  const val DAILY_URI_SUFFIX = "dailyUpdate"
  const val TRUSTED_BIDDING_URI_SUFFIX = "trustedBidding" // calls real servers if specified
  const val RENDER_URI_SUFFIX = "render"

  // Signals data contracts
  const val BID_FLOOR = "bid_floor"
  const val BID = "bid"
  const val BID_FLOOR_SIGNALS_FORMAT = "{" + BID_FLOOR + ":%s}"
  const val BID_SIGNALS_FORMAT = "{" + BID + ":%s}"
  const val BIDDING_LOGIC_JS =
    """function generateBid(ad, auction_signals, per_buyer_signals, trusted_bidding_signals, contextual_signals, user_signals, custom_audience_signals) {
    return {'status': 0, 'ad': ad, 'bid': user_signals.user_bidding_signals.$BID};
}
function reportWin(ad_selection_signals, per_buyer_signals, signals_for_buyer, contextual_signals, custom_audience_signals) {
  let reporting_address = 'www.test.com/reportingTo_%s';
  return {'status': 0, 'results': {'reporting_uri': reporting_address + '?ca=' + custom_audience_signals.name} };
}"""
  const val SCORING_LOGIC_WITH_BID_FLOOR_JS =
    """function scoreAd(ad, bid, auction_config, seller_signals, trusted_scoring_signals,
    contextual_signal, user_signal, custom_audience_signal) {
    return {'status': 0, 'score': (bid >= seller_signals.$BID_FLOOR) ? bid : -1 };
}
function reportResult(ad_selection_config, render_uri, bid, contextual_signals) {
    // Add the address of your reporting server here
    let reporting_address = 'www.test.com/reportingTo%s';
    return {'status': 0, 'results': {'signals_for_buyer': '{"signals_for_buyer" : 1}'
            , 'reporting_uri': reporting_address + '?render_uri='
                + render_uri + '?bid=' + bid }};
}"""
  const val WATERFALL_MEDIATION_LOGIC_JS = """function selectOutcome(outcomes, selection_signals) {
    const outcome_1p = outcomes[0];
    const bid_floor = selection_signals.$BID_FLOOR;
    return {'status': 0, 'result': (outcome_1p.bid >= bid_floor) ? outcome_1p : null};
}"""

  fun getDoubleFromEditText(editText: EditText): Double {
    return editText.text.toString().toDouble()
  }

  fun getDoubleFromEditText(editText: TextView): Double {
    return editText.text.toString().toDouble()
  }

  fun getNetworkNameFromTextView(textView: TextView): String {
    return textView.text.toString().split("\n").toTypedArray()[0]
  }

  fun getBuyerNameFromTextView(textView: TextView): String {
    return textView.text.toString()
      .split("\n").toTypedArray()[1]
      .split(" ").toTypedArray()[1]
      .replace(")", "")
  }

  fun hasTextNotEmpty(textView: TextView?): Boolean {
    if (textView != null) {
      return stringNotNullAndNotEmpty(textView.text.toString())
    }
    return false;
  }

  fun hasTextNotEmpty(textView: EditText?): Boolean {
    if (textView != null) {
      return stringNotNullAndNotEmpty(textView.text.toString())
    }
    return false;
  }

  private fun stringNotNullAndNotEmpty(str: String?): Boolean {
    return str != null && str.isNotEmpty()
  }

  fun uriFriendlyString(str: String): String {
    return str.lowercase(Locale.getDefault()).replace(" ", "")
  }

  fun toCamelCase(str: String): String {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str.replace(" ", ""))
  }
}
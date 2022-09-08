/*
* Copyright (C) 2022 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
/**
 * Generates a bid of 10 for the shoes CA, and a bid of 5 otherwise
 */
function generateBid(ad, auction_signals, per_buyer_signals,
  trusted_bidding_signals, contextual_signals, user_signals,
  custom_audience_signals) {
  var bid = 5;
  if (custom_audience_signals.name === "shoes") {
    bid = 10;
  }
  return {'status': 0, 'ad': ad, 'bid': bid };
}

function reportWin(ad_selection_signals, per_buyer_signals, signals_for_buyer,
 contextual_signals, custom_audience_signals) {
  // Add the address of your reporting server here
  let reporting_address = 'https://reporting.example.com';
  return {'status': 0, 'results': {'reporting_uri':
         reporting_address + '?ca=' + custom_audience_signals.name} };
}
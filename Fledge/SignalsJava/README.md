## Sample app for signals API
This sample demonstrates the creation and storage of signals using the Protected App Signals libraries.

*NOTE: In order for this sample to work, you must first be running a Bidding and Auction server as well as have the necessary server side components for Buyers and Sellers as shown in the [Protected App Signals Development Guide](https://developer.android.com/design-for-safety/privacy-sandbox/guides/protected-audience/protected-app-signals).*


This app contains two text boxes with attached buttons: one that takes a URL to fetch signals from, and one that takes an auction server URL. One possible way to test the APIs with the sample app would be to:

1. Create an emulator or physical device that is running an API 34 SDK extension 12 image.
2. Run the following:
```
adb shell am start -n com.google.android.adservices.api/com.android.adservices.ui.settings.activities.AdServicesSettingsMainActivity
```
Then select the option shown to consent to app-suggested ads
3. Run the following command to enable the relevant APIs.
```
adb shell device_config set_sync_disabled_for_tests persistent
adb shell device_config put adservices fledge_custom_audience_service_kill_switch false
adb shell device_config put adservices fledge_select_ads_kill_switch false
adb shell device_config put adservices fledge_on_device_auction_kill_switch false
adb shell device_config put adservices fledge_auction_server_kill_switch false
adb shell "device_config put adservices disable_fledge_enrollment_check true"
adb shell device_config put adservices ppapi_app_allow_list '\*'
adb shell device_config put adservices fledge_auction_server_overall_timeout_ms 60000
adb shell setprop debug.adservices.consent_manager_debug_mode true
adb shell device_config put adservices protected_signals_enabled true
adb shell device_config put adservices protected_signals_periodic_encoding_enabled true
```
4. Restart the device
5. Set up a server with valid encoding logic (See "encodeSignals Example" section below)
6. Set up a server with a valid Protected App Signals s JSON, say example.com/protectedsignals. Ensure the JSON includes an `update_encoder` field that points to your encoding logic. (See "Signals Examples" section below)
7. Set up bidding and auction servers at a different URL (See the [Protected App Signals Development Guide](https://developer.android.com/design-for-safety/privacy-sandbox/guides/protected-audience/protected-app-signals) for instructions on how to do this), say the seller front-end is example.com/sellerfrontend
8. Set up a server hosting the auction keys used by the auction server. Say example.com/auctionkeys. If using the setup instructions from the developer guide, your server should host the following content:
```
{
"keys": [{
"id": "4000000000000000",
"key": "87ey8XZPXAd+/+ytKv2GFUWW5j9zdepSJ2G4gebDwyM="
}]
}
```
9. Override the device's auction keys to point to the auction key server. It is important to run this step before attempting to run an auction to prevent incorrect keys from being cached.
```
adb shell device_config put adservices fledge_auction_server_auction_key_fetch_uri 'example.com/auctionkeys'
```
10. Build, install, and launch the sample app
11. In the Signals JSON URL box enter example.com/protectedsignals (Can be done quickly with the adb shell input text command)
12. Click the `UPDATE SIGNALS` button
13. Run the following command to encode the signals immediately
```
adb shell cmd jobscheduler run -f com.google.android.adservices.api 29
```
14. In the Auction Server URL box enter `example.com/sellerfrontend`
15. Click `RUN AUCTION`
16. Observe the auction results.

### Troubleshooting
- There are some known issues as of the release of DP10 which can cause requests to time out, as well as a loop of messages informing the user that Play Services have Stopped Working. Restarting the device should ameliorate this issue.
- If an auction fails to run, your first step in troubleshooting should be to re-run the auction due to a known issue which can cause intermittent failures. 

## Samples
### Signals examples
Adds a signal with a key of 0 and a value of 1.
```
{
  "put": {
    "AA==": "AQ=="
  },
  "update_encoder": {
    "action": "REGISTER",
    "endpoint": "https://example.com/example_script"
  }
}
```

Adds a signal with a key of 1 and a value of 2.
```
{
  "put": {
    "AQ==": "Ag=="
  },
  "update_encoder": {
    "action": "REGISTER",
    "endpoint": "https://example.com/example_script"
  }
}
```

### encodeSignals example
Encodes each signal into two bytes, with the first byte being the first byte of the signal key and the second byte being the first byte of the signal value.
```
function encodeSignals(signals, maxSize) {
  // if there are no signals don't write a payload
  if (signals.size === 0) {
      return {};
  }

  let result = new Uint8Array(signals.size * 2);
  let index = 0;

  for (const [key, values] of signals.entries()) {
    result[index++] = key[0];
    result[index++] = values[0].signal_value[0];
  }

  return { 'status': 0, 'results': result};
}
```

# Overview

The Measurement sample app showcases how to use the Attribution Reporting API
* Initializes a `MeasurementManager` client in `AppModule`
* Provides two buttons to register the source event from `SourceFragment` via `registerSource`
  1. when user clicks `Register Click Event`, the click event is registered.
  2. when user clicks `Register View Event`, the view event is registered.
* Provides a button to register trigger event from `TriggerFragment` via `registerTrigger` 
  1. when user clicks `Register Trigger`, the action is registered.

# Set up development environment
To test functionality of the Attribution Reporting API, you need to [set up your development environment]. This process involves installing the proper SDK and the device images needed to test functionality on an emulator.

# Server URL Configuration
The Attribution Reporting API sends event reports to the configured server.
We can easily configure the Server URL from the options menu on the top right side of the app.


# Steps to test
1. Configure Server URL from the options menu
2. On the app and click `Register Click Event` button
3. Switch tabs and click `Register Trigger` button
4. Force AttributionJobService 
   * `adb shell cmd jobscheduler run -f com.google.android.adservices.api 5`
5. Open phone time & date settings. Disable automatic time & date. Then set the date 3 days forward.
6. Force ReportingJobService
    * `adb shell cmd jobscheduler run -f com.google.android.adservices.api 3`
    * `adb shell cmd jobscheduler run -f com.google.android.adservices.api 7`

You can verify server interactions by examining the request/response logs of the
configured server.

For a full overview of how Attribution Reporting works, read the [design proposal]. Review the [developer guide] for details on API usage and systems integration.

[design proposal]: https://developer.android.com/privacy-sandbox/attribution
[set up your development environment]: https://developer.android.com/design-for-safety/privacy-sandbox/setup
[developer guide]: https://developer.android.com/privacy-sandbox/guides/attribution

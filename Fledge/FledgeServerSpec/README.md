# Protected Audience (formerly known as FLEDGE) and Protected App Signals Server Spec

## Creating a Mock Server

To use a mock server for fetching and joining custom audiences, updating
protected app signals, on-device ad selection and reporting, you will need to
set up 10 HTTPS endpoints that your test device or emulator can access. They
are:

1. A buyer bidding logic endpoint that serves the sample `BiddingLogic.js`
   JavaScript in this directory.
2. A [bidding signals] endpoint that serves the sample `BiddingSignals.json` in
   this directory.
3. A seller scoring logic endpoint that serves the sample `ScoringLogic.js`
   JavaScript in this directory.
4. A [scoring signals] endpoint that serves the sample `ScoringSignals.json` in
   this directory.
5. A winning buyer impression reporting endpoint. Modify the `reporting_address`
   variable in the `BiddingLogic.js` file to match this endpoint.
6. A seller impression reporting endpoint. Modify the `reporting_address`
   variable in the `ScoringLogic.js` file to match this endpoint.
7. A buyer daily update endpoint that serves the sample
   `DailyUpdateResponse.json` JSON object in this directory. Modify the
   `trusted_bidding_uri` and `render_uri` fields to match this endpoint's
   domain.
8. A [fetchAndJoinCustomAudience API] endpoint, `/fetch/ca`, that serves the
   sample `FetchAndJoinResponse.json` JSON object in this directory. Modify the
   `daily_update_uri`, `bidding_logic_uri`, `trusted_bidding_uri` and
   `render_uri` fields to match this endpoint's domain.
9. An [encode signals] endpoint that serves the sample `EncodeSignals.js`
   JavaScript in this directory.
10. An [updateSignals API] endpoint that serves the sample
    `UpdateSignalsResponse.json` in this directory. Modify the
    `encode-signals-script` variable to match the signals encode logic endpoint
    you have previously set up.

The impression reporting endpoints need only return a 200 status code -- the
response content does not matter. To verify that impressions were reported,
check the call logs for endpoints 3 and 4.

### OpenAPI Definitions

For convenience, we have provided OpenAPI definitions for how these endpoints
could be run in `mock-server.json`, which manages endpoints 1-7. In order for `mock-server.json` to be usable, the
`report_address` variable in the contained javascript string must be updated to
the address of the server, and the `trusted_bidding_uri` and `render_uri` fields in the daily fetch response should be changed to match the
domain of the server.

### Set-Up Directions With OpenAPI Specs

1. Find a server mocking tool which can run servers based on OpenAPI specs.
2. Import the `mock-server.json` spec and begin running the server.
3. Replace both occurrences of `reporting.example.com` in `mock-server.json`
   file with the URL of the server.
4. Replace all occurrences of `js.example.com` in the daily fetch response with
   the URL of the server.
5. Monitor the call log of the server to see data reported by FLEDGE.

[bidding signals]: https://developer.android.com/design-for-safety/privacy-sandbox/fledge#ad-selection-ad-tech-platform-managed-trusted-server

[scoring signals]: https://developer.android.com/design-for-safety/privacy-sandbox/fledge#ad-selection-ad-tech-platform-managed-trusted-server

[fetchAndJoinCustomAudience API]: https://developer.android.com/design-for-safety/privacy-sandbox/protected-audience#custom-audience-delegation

[encode signals]: https://developers.google.com/privacy-sandbox/private-advertising/protected-audience/android/protected-app-signals-developer-guide#signals_encoding_api

[updateSignals API]: https://developers.google.com/privacy-sandbox/private-advertising/protected-audience/android/protected-app-signals-developer-guide#update_signals_api
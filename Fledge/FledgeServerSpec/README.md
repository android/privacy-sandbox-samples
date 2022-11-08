# FLEDGE Server Spec

## Option 1: Remote Overrides (Default)

To enable the usage of the remote overrides APIs in the sample app, you will
need to set up a reporting HTTPS endpoint that your test device or emulator can
access. This endpoint will act as a termination point for reporting to a buyer
and seller after ad selection is complete.

The remote overrides will use the sample `BiddingLogic.js` and `ScoringLogic.js`
JavaScript in this directory during ad selection.

In addition, 'trustedScoringSignals' will be added to the 'adSelectionOverride'
and 'trustedBiddingSignals' will be added to each 'customAudienceOverride'.
These signals will be JSON string objects and will be used during ad selection.

This is what the signals will look like:

```
    private static final String TRUSTED_SCORING_SIGNALS =
        "{\n"
            + "\t\"render_uri_1\": \"signals_for_1\",\n"
            + "\t\"render_uri_2\": \"signals_for_2\"\n"
            + "}";

    private static final String TRUSTED_BIDDING_SIGNALS =
        "{\n"
            + "\t\"example\": \"example\",\n"
            + "\t\"valid\": \"Also valid\",\n"
            + "\t\"list\": \"list\",\n"
            + "\t\"of\": \"of\",\n"
            + "\t\"keys\": \"trusted bidding signal Values\"\n"
            + "}";
```

### OpenAPI Definitions

For convenience, we have provided OpenAPI definitions for how the reporting
endpoint could be run in `mock-server.json`.

### Set-Up Directions With OpenAPI Specs

1. Find a server mocking tool which can run servers based on OpenAPI specs.
2. Import the `mock-server.json` spec and begin running a server.
3. Monitor the call log of the reporting server to see data reported by FLEDGE.

The impression reporting endpoint need only return a 200 status code -- the
response content does not matter. To verify that impressions were reported,
check the call logs for the reporting endpoint.

## Option 2: Mock Server
To instead use a mock server for ad selection and reporting, you will need to set
up 7 HTTPS endpoints that your test device or emulator can access. They are:

1. A buyer bidding logic endpoint that serves the sample `BiddingLogic.js`
   JavaScript in this directory.

2. A [bidding signals](https://developer.android.com/design-for-safety/privacy-sandbox/fledge#ad-selection-ad-tech-platform-managed-trusted-server)
   endpoint that serves the sample `BiddingSignals.json` in this directory.

3. A seller scoring logic endpoint that serves the sample `ScoringLogic.js`
   JavaScript in this directory.
4. A [scoring signals](https://developer.android.com/design-for-safety/privacy-sandbox/fledge#ad-selection-ad-tech-platform-managed-trusted-server)
   endpoint that serves the sample `ScoringSignals.json` in this directory.

5. A winning buyer impression reporting endpoint. Modify the reporting_address
   variable in the `BiddingLogic.js` file to match this endpoint.

6. A seller impression reporting endpoint. Modify the reporting_address variable
   in the `ScoringLogic.js` file to match this endpoint.

7. A buyer daily fetch endpoint that serves the sample `DailyUpdateResponse.json`
   JSON object in this directory.  Modify the `trusted_bidding_uri` and
   `render_uri` fields to match this endpoint's domain.


The impression reporting endpoints need only return a 200 status code -- the
response content does not matter. To verify that impressions were reported,
check the call logs for endpoints 3 and 4.

### OpenAPI Definitions

For convenience, we have provided OpenAPI definitions for how these endpoints
could be run in `mock-server.json`, which manages endpoints all 1-7. In order for `mock-server.json` to be usable, the
`report_address` variable in the contained javascript string must be updated to
the address of the server, and the `trusted_bidding_uri` and `render_uri` fields in the daily fetch response should be changed to match the
domain of the server.

### Set-Up Directions With OpenAPI Specs

1. Find a server mocking tool which can run servers based on OpenAPI specs.
2. Import the `mock-server.json` spec and begin running the server.
3. Replace both occurrences of "reporting.example.com" in `mock-server.json`
   file with the URL of the server.
4. Replace all occurrences of "js.example.com" in the daily fetch response with
   the URL of the server.
5. Monitor the call log of the server to see data reported by FLEDGE.

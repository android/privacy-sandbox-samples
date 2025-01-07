# FLEDGE Server Spec

> [!WARNING]
> This branch contains an example of an unreleased version of the
> on-device bidding logic. For the recommended version, see
> [the `main` branch](https://github.com/android/privacy-sandbox-samples/tree/main/Fledge/FledgeServerSpec)
> of this repo and
> [the official documentation](https://developers.google.com/privacy-sandbox/private-advertising/protected-audience/android/developer-guide/index.md#javascript-ad).

## Single SSP Auctions
To run a single SSP Auctions, you can follow one of the options below for to 
either use overrides or one server to represent your SSP.

### Option 1: Remote Overrides (Default)

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

#### OpenAPI Definitions

For convenience, we have provided OpenAPI definitions for how the reporting
endpoint could be run in `mock-server.json`.

##### Set-Up Directions With OpenAPI Specs

1. Find a server mocking tool which can run servers based on OpenAPI specs.
2. Import the `mock-server-v2-bidding-logic.json` or `mock-server-v3-bidding-logic.json` spec and begin running a server.
3. Monitor the call log of the reporting server to see data reported by FLEDGE.

The reporting endpoint need only return a 200 status code -- the response content does not matter.
To verify that impressions and interactions were reported, check the call logs for the reporting endpoint.

### Option 2: Mock Server
To instead use a mock server for ad selection and reporting, you will need to set
up 7 HTTPS endpoints that your test device or emulator can access. They are:

1. A buyer bidding logic endpoint '/bidding' that serves the sample `BiddingLogicV2.js` and `BiddingLogicV3.js` JavaScript in this directory, depending on the header received.
 1. If the request has header `x_fledge_buyer_bidding_logic_version:3`, the server should return `BiddingLogicV3.js` with header `x_fledge_buyer_bidding_logic_version:3`;
 2. Otherwise, should return `BiddingLogicV2.js` with no additional header.

2. A [bidding signals](https://developer.android.com/design-for-safety/privacy-sandbox/fledge#ad-selection-ad-tech-platform-managed-trusted-server) '/bidding/trusted'
   endpoint that serves the sample `BiddingSignals.json` in this directory.

3. A seller scoring logic endpoint '/scoring' that serves the sample `ScoringLogic.js`
   JavaScript in this directory.

4. A [scoring signals](https://developer.android.com/design-for-safety/privacy-sandbox/fledge#ad-selection-ad-tech-platform-managed-trusted-server)
   endpoint that serves the sample `ScoringSignals.json` in this directory.

5. A winning buyer reporting endpoint. Modify the reporting_address
   variable in the `BiddingLogic.js` file to match this endpoint.

6. A seller reporting endpoint. Modify the reporting_address variable
   in the `ScoringLogic.js` file to match this endpoint.

7. A buyer daily update endpoint that serves the sample `DailyUpdateResponse.json`
   JSON object in this directory.  Modify the `trusted_bidding_uri` and
   `render_uri` fields to match this endpoint's domain.

8. A [fetchAndJoinCustomAudience API](https://developer.android.com/design-for-safety/privacy-sandbox/protected-audience#custom-audience-delegation) endpoint,
'/fetch/ca', that serves the sample `FetchAndJoinResponse.json` JSON object in this directory. Modify the `daily_update_uri`, `bidding_logic_uri`, `trusted_bidding_uri` and
   `render_uri` fields to match this endpoint's domain.

The reporting endpoints should be able to accept both GET requests for impression reporting and POST requests for interaction reporting.
The reporting endpoints need only return a 200 status code -- the response content does not matter. To verify that impressions and interactions were reported,
check the call logs for endpoints 5 and 6.

#### OpenAPI Definitions

For convenience, we have provided OpenAPI definitions for how these endpoints
could be run in `mock-server-v2-bidding-logic.json` and `mock-server-v3-bidding-logic.json`, which manages endpoints all 1-7. In order for the json file to be usable, the
`report_address` variable in the contained javascript string must be updated to
the address of the server, and the `trusted_bidding_uri` and `render_uri` fields in the daily fetch response should be changed to match the
domain of the server.

##### Set-Up Directions With OpenAPI Specs

1. Find a server mocking tool which can run servers based on OpenAPI specs.
2. Import the `mock-server.json` spec and begin running the server.
3. Replace both occurrences of "reporting.example.com" in `mock-server.json`
   file with the URL of the server.
4. Replace all occurrences of "js.example.com" in the daily fetch response with
   the URL of the server.
5. Monitor the call log of the server to see data reported by FLEDGE.

##### Buyer Bidding Logic Version Config
Due to the limitation of OpenAPI spec, returning a response with varied headers is not supported. We provided 2 json files `mock-server-v2-bidding-logic.json` and `mock-server-v3-bidding-logic.json` for v2 and v3 buyer bidding logic respectively.

* Option 1: Config 2 servers to vendor out different version of bidding logic
* Option 2: Import 1 server spec and config the `\bidding` endpoint following mock server instructions.

##  Waterfall Mediation
To demonstrate Waterfall mediation you need to create at least 2 SSPs; one 
mediation SSP and at least one participant SSP. You have two options; using 
overrides or setting up mock servers.

### Option 1: Remote Overrides (Default)
Follow Single SSP Auctions, Option 1 to set up overrides. Repeat that process 
for each SSP. Addition to bidding and scoring, use the sample 
`WaterfallMediationOutcomeSelectionLogic.js` JavaScript in this directory.

### Option 2: Mock Server
Follow Single SSP Auction, Option 2 to set up mock servers. Repeat that process 
for each SSP. For the SSP that will orchestrate the waterfall mediation, Add a 
waterfall mediation logic endpoint that serves the sample 
`WaterfallMediationOutcomeSelectionLogic.js` JavaScript in this directory.

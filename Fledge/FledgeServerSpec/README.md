# Fledge Server Spec

## Option 1: Remote Overrides (Default)
To enable the usage of the remote overrides APIs in the sample app, you will 
need to set up a reporting HTTPS endpoint that your test device or emulator 
can access. This endpoint will serve both the sample `BiddingLogic.js` 
and `ScoringLogic.js` Javascript in this directory.

In addition, 'trustedScoringSignals' will be added to the 'adSelectionOverride' and
'trustedBiddingSignals' will be added to each 'customAudienceOverride'.

These signals will be JSON string objets and will be used during ad selection.

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

### OpenApi Definitions
For convenience, we have provided OpenApi definitions for how these this endpoint
could be run in `reporting-server.json`.

### Set-Up Directions With OpenAPI Specs
1. Find a server mocking tool which can run servers based on OpenAPI specs.
2. Import the `reporting-server.json` spec and begin running a server.
3. Monitor the call log of the reporting server to see data reported by FLEDGE.

The impression reporting endpoint need only return a 200 status code -- the
response content does not matter. To verify that impressions were reported, check
the call logs for the reporting endpoint.

## Option 2: Mock Server
To instead use a mockserver for ad selection and reporting, you will need to set 
up 4 HTTPS endpoints that your test device or emulator can access. They are:

1. A buyer bidding logic endpoint that serves the sample `BiddingLogic.js` JavaScript
   in this directory.
2. A seller scoring logic endpoint that serves the sample `ScoringLogic.js` JavaScript
   in this directory.
3. A winning buyer impression reporting endpoint. Modify the reporting_address  
   variable in the `BiddingLogic.js` file to match this endpoint.
4. A seller impression reporting endpoint. Modify the reporting_address variable in
   the `ScoringLogic.js` file to match this endpoint.

The impression reporting endpoints need only return a 200 status code -- the 
response content does not matter. To verify that impressions were reported, check 
the call logs for endpoints 3 and 4. 


### OpenApi Definitions 

For convenience, we have provided OpenApi definitions for how these these endpoints 
could be run in the files `js-server.json` and `reporting-server.json`. Where
`js-server.json` manages endpoints 1 and 2 and `reporting-server.json` manages 
endpoints 3 and 4. In order for `js-server.json` to be usable, the report_address
variable in the contained javascript string must be updated to the address of 
the reporting server. 

### Set-Up Directions With OpenAPI Specs

1. Find a server mocking tool which can run servers based on OpenAPI specs. 
2. Import the `reporting-server.json` spec and begin running a server. 
3. Replace both occurences of "reporting.example.com" in the `js-server.json`
   file with the URL of the reporting server. 
4. Import the `js-server.json` spec and begin running the js server. 
5. Monitor the call log of the reporting server to see data reported by FLEDGE.

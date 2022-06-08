# Fledge Server Spec
In order for ad selection and impression reporting to work, you will need to set 
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


## OpenApi Definitions 

For convenience, we have provided OpenApi definitions for how these these endpoints 
could be run in the files `js-server.json` and `reporting-server.json`. Where
`js-server.json` manages endpoints 1 and 2 and `reporting-server.json` manages 
endpoints 3 and 4. In order for `js-server.json` to be usable, the report_address
variable in the contained javascript string must be updated to the address of 
the reporting server. 

## Set-Up Direction With OpenAPI Specs

1. Find a server mocking tool which can run servers based on OpenAPI specs. 
2. Import the `reporting-server.json` spec and begin running a server. 
3. Replace both occurences of "reporting.example.com" in the `js-server.json`
   file with the URL of the reporting server. 
4. Import the `js-server.json` spec and begin running the js server. 
5. Monitor the call log of the reporting server to see data reported by FLEDGE.

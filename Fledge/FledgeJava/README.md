## Overview

The FLEDGE sample app demonstrates how to initialize and call the Custom Audience 
and Ad Selection APIs in FLEDGE.

The sample app has two client classes, `AdSelectionClient` and
`CustomAudienceClient` that interact with the two FLEDGE APIs. `CustomAudienceClient`
wraps the API calls that allow apps to join and leave custom audiences (CAs) while
`AdSelectionClient` wraps the functionality that allows apps to run ad auctions
and report impressions based on the results of those auctions.

Note that this sample includes several build flavors. Developers should use the "preview" flavor
and can follow the commands described in this README as is. For OEMs to use the required build
flavor, you will need to update the "Preview" part of the commands to "Oems", all other instructions
are the same.

## About the FLEDGE API

For a full overview of how FLEDGE works, read the [design proposal]. Review the 
[developer guide] for details on API usage and systems integration.

## Set up development environment

To test functionality of the FLEDGE API, you need to [set up your development
environment].

## Set up bidding code and impression reporting endpoints

In order for ad selection components of the app to work, you must follow the 
directions for a setting up a server located in the [FledgeServerSpec]
folder.

## Launching the app

First install the app on your device by running 
```shell
./gradlew installPreviewDebug
```
Once the app is installed on your device, you must launch it with these options:

If you only wish to use the remote overrides you can run:
```shell
adb shell am start -n com.example.adservices.samples.fledge.sampleapp/.MainActivity -e reportingUrl [reporting endpoint]
```

Otherwise, to optionally use remote overrides or mock servers, run:
```shell
adb shell am start -n com.example.adservices.samples.fledge.sampleapp/.MainActivity -e reportingUrl [reporting endpoint] -e biddingUrl [bidding endpoint] -e scoringUrl [scoring endpoint]
```
These commands will inform the app where your server endpoints are running.

## Manage custom audiences and run ad selection

To view the end-to-end functionality of FLEDGE, you can use the toggles in the 
middle of the app to join and leave the "shirts" CA and the "shoes" CA which
represent custom audiences for advertising shirts and shoes respectively.

Once you have joined one or both of the custom audiences, you can run ad selection
by pressing the "Run Ad Selection" button. This will trigger an ad auction. The
ad auction will pull the bidding and scoring logic, in the form of JavaScript files,
from two web endpoints. With the way the JavaScript is set up,
if the phone is part of both CAs the shoes CA will outbid the shirts CA and win
the auction.

Once the ad auction is complete, the app with show the URL that would normally
be used the render the ad in the top purple TextView. The app will then trigger
impression reporting which will run the reportWin and reportResults functions from the
JavaScript files pulled down during the ad auction. These JavaScript files will
instruct FLEDGE to send some data back to two additional endpoints
which will then display the HTTPS calls they received in the second and third
TextViews.

## Updating custom audiences in the background

The FLEDGE background fetch job will run periodically, deleting custom audiences
that have passed their configured expiration time and updating custom audiences
that have not been updated or joined in the last day.  To test these cases, you
can use the toggles in the middle of the app to join the "short expiry" and
"invalid fields" CAs.

A CA that has expired is no longer eligible to participate in ad selection, and
the "short expiry" CA is configured to expire 30 seconds after it is joined.
Running ad selection with only the "short expiry" CA will succeed in the 30
second window but should fail afterwards without seeing any valid bids.  Once
the background fetch job has run, the "short expiry" CA should additionally be
removed from the device.

A CA that is incomplete, or which was joined with invalid or missing fields,
will not be eligible to participate in ad selection until the CA has been
updated with valid fields.  Using the test app, this update will occur during
the background fetch job.  Additionally, any CAs that have been joined will also
be updated, if they haven't already been joined or updated within the last day.

[design proposal]: https://developer.android.com/privacy-sandbox/fledge
[set up your development environment]: https://developer.android.com/design-for-safety/privacy-sandbox/setup
[developer guide]: https://developer.android.com/design-for-safety/privacy-sandbox/guides/fledge
[FledgeServerSpec]: ../FledgeServerSpec

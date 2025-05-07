SDK Runtime and Privacy Preserving APIs Repository
==================================================

This repository contains a set of individual Android Studio projects to help you get started writing apps using the SDK Runtime and Privacy Preserving APIs (PPAPIs).

Branches
-----------
Due to the dynamic nature of this project, there are three branches provided.
Please ensure you are using the correct branch for your needs.

* [main](https://github.com/android/privacy-sandbox-samples) - contains sample
  applications targeting Beta releases.
* [dev-preview-main](https://github.com/android/privacy-sandbox-samples/tree/dev-preview-main) - contains
sample applications targeting Developer Preview releases. This provides early access to new features for early testing.
* [jetpack-main](https://github.com/android/privacy-sandbox-samples/tree/jetpack-main) - contains
versions of the sample applications that utilize Jetpack libraries to interface with the Privacy Sandbox.

The Privacy Sandbox on Android is currently in Alpha and it is not recommended to deploy or use these samples other than to test your own infrastructure.

Note: It is recommended to use [Android Studio
Canary](https://developer.android.com/studio/preview). As we work to support the
latest features for Privacy Sandbox, there may be some issues using Stable
releases.

Read below for a description of each sample.


Samples
----------

* **[TopicsKotlin](TopicsKotlin)** (Kotlin) - Demonstrates how to initialize and call the Topics API. 

* **[TopicsJava](TopicsJava)** (Java) - Demonstrates how to initialize and call the Topics API. 

* **[Fledge](Fledge)** - Contains components for demonstrating FLEDGE APIs.
  * **[FledgeKotlin](Fledge/FledgeKotlin)** (Kotlin) - Demonstrates how to initialize and call the FLEDGE APIs. 

  * **[FledgeJava](Fledge/FledgeJava)** (Java) - Demonstrates how to initialize and call the FLEDGE API. 

  * **[FledgeServerSpec](Fledge/FledgeServerSpec)** (OpenApi 3.1) - Sample FLEDGE server specs that can be used generate mock servers for delivering Javascript files to FLEDGE and receiving impression reports.  

* **[PrivacySandboxKotlin](PrivacySandboxKotlin)** (Kotlin) - Demonstrates how to create an SDK that will run in a separate process. This sample contains both an app, and an SDK to show the interaction between them.

* **[AttributionReporting](AttributionReporting)** - Contains components for demonstrating Attribution Reporting API.
   * **[MeasurementAdTechServer](AttributionReporting/MeasurementAdTechServer)** (Kotlin) - Sample AdTech server to facilitate demonstration of Measurement APIs by the measurement sample app.

   * **[MeasurementAdTechServerSpec](AttributionReporting/MeasurementAdTechServerSpec)** (OpenApi 3.1) - Sample AdTech server spec that can be used generate a mock server for interaction with measurement sample app.

   * **[MeasurementSampleApp](AttributionReporting/MeasurementSampleApp)** (Kotlin) - Demonstrates how to initialize and use Attribution Reporting API


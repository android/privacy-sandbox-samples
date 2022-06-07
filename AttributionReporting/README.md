Attribution Reporting
==================================================

This folder contains following:

1) [Sample Server](MeasurementAdTechServer)
2) [Sample Server Spec](MeasurementAdTechServerSpec)
3) [Sample App](MeasurementSampleApp/)


## How to setup the environment

This setup has 2 components, an app and a server which will communicate with the Privacy Sandbox Measurement APIs.

### Setup the server

There are 2 ways to setup the server:
1) Use the `OpenApi 3.1` spec provided [here](MeasurementAdTechServerSpec) to generate a mock server using various tools available online. 
2) Setup your own server with the code provided [here](MeasurementAdTechServer). Please keep in mind that server needs to have SSL(https://) enabled.

### Configure App

Configure the Server URL from the options menu on the top right side of the app.
Please refer to app's [README.md](MeasurementSampleApp/README.md) for detailed instruction on how to use the API.
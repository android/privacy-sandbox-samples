# Privacy Sandbox (Sample Client App)

This is a sample project to present a client application which loads 
and interacts with already installed SDK through privacy sandbox

This sample has a simple activity with couple of buttons to:
- load the installed SDK inside the sandbox.
- request a SurfacePackage from the sandbox to present inside the application.
It also contains a SurfaceView to show the SurfacePackage inside it.

## Usage
- Make sure the SDK mentioned in the AndroidManifest.xml file is already 
installed to the target device.
- Install this Sample Application to the same target device.
- Click on the "load SDK" button, a toast should show that SDK loaded successfully.
- Click on the "Request Webview", this should remote render a webview from the 
  sandbox to be viewed inside the activity.

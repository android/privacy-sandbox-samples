# Privacy Sandbox Sample - Kotlin

This is a sample project describing how to
- implement an SDK to be compatible with SDK Runtime.
- develop a client app which loads and interacts with an installed
  SDK through privacy sandbox.

When opening the project, you may be asked to upgrade the Android Gradle Plugin version. Please
do so by selecting "Upgrade" in the popup window.

Note that this sample includes several build flavors. Developers should use the "preview" flavor
and can follow the commands described in this README as is. For OEMs to use the required build
flavor, you will need to update the "Preview" part of the commands to "Oems", all other instructions
are the same.

## Running the Sample

The Privacy Sandbox should have a class which extends `SandboxedSdkProvider`.
This class works as an entry point for the sandbox to interact with the SDK.
The Privacy Sandbox SDK provider library is implemented in the `sdk-implementation` module.

The client app is implemented in the `client-app` module. This app interacts with the SDK running
in the Privacy Sandbox.

There are two methods for building and installing the SDK. The preferred option is use Android
Studio's UI to handle building and deploying the SDK and launching the client app. However, it is 
possible to build the app bundle and install the APK via the command line, then run the client app.

### From the UI
In Android Studio, edit your run configuration as follows:
Edit run configurations > client-app > Deploy > Default APK. Then, under Launch Options,
Launch > Specified Activity > Activity > `com.example.client.MainActivity`

Press the run button. Your app should launch and you can proceed to the
[Testing the client](#testing-the-client) section.

### Command Line
Build the APK bundle by running 

```shell
./gradlew client-app:buildPrivacySandboxSdkApksForPreviewDebug
```

This will output a location where the APK is generated. It is automatically signed with your local
debug key for now.

Then, install the APK to your device:
```shell
adb install -t path/to/your/standalone.apk
```

Finally, run the client app via Android Studio's UI.

### Testing the client

- Click on the "load SDK" button, a toast should show that SDK loaded successfully.
- Click on the "Request Webview", this should remote render a webview from the
  sandbox to be viewed inside the activity.

- For more information, please read the [documentation](https://developer.android.com/design-for-safety/privacy-sandbox/guides/sdk-runtime).


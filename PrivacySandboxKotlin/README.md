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

## Defining the SDK API
The SDK running in the Privacy Sandbox needs a public API defined with Kotlin interfaces and
annotated with Privacy Sandbox tool annotations. This allows us to generate an SDK provider that
compatible with your custom interfaces. To use it just extend `AbstractSandboxedSdkProviderCompat`,
it will be generated in the same package that defined the `@PrivacySandboxService` interface.

## Running the Sample
The sample contains a working SDK in the `example-sdk` module. The SDK is bundled for release and
app consumption in the `example-sdk-bundle` module, this is where the SDK version, package name and
signing information is defined.

The client app is implemented in the `client-app` module and is capable of loading and interacting
with the example SDK.

There are two methods for building and installing the SDK. The preferred option is use Android
Studio's UI to handle building and deploying the SDK and launching the client app. However, it is 
possible to build the app bundle and install the APK via the command line, then run the client app.

### Setting up your device
You will need to override a few flags to get the Privacy Sandbox enabled on your device. Before
installing the app, run the following commands:

```shell
adb shell device_config put adservices adservice_system_service_enabled false
adb shell device_config put adservices global_kill_switch false
adb shell device_config put adservices disable_sdk_sandbox false
adb shell device_config put adservices sdksandbox_customized_sdk_context_enabled true
```

### Launch sample from the UI
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
- Click on the "Show banner view" after the SDK is loaded and a banner rendered by the SDK will be
  displayed. If you click it, an Activity customized by the SDK will be launched.

- For more information, please read the [documentation](https://developer.android.com/design-for-safety/privacy-sandbox/guides/sdk-runtime).

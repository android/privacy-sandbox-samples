# Privacy Sandbox Sample - Kotlin

This is a sample project describing how to
- implement an SDK to be compatible with SDK Runtime.
- develop a client app which loads and interacts with an already-installed
  SDK through privacy sandbox.

## SDK provider

The Privacy Sandbox should have a class which extends `SandboxedSdkProvider`.
This class works as an entry point for the sandbox to interact with the SDK.

The Privacy Sandbox SDK provider library is implemented in the `sdk-implementation` module.

There are two methods for building and installing the SDK. The current implementation uses features
available in the latest Android Studio Canary Electric Eel 9 and later canary versions. With minor
modifications it is possible to build in stable versions of Android Studio. Follow the instructions
corresponding to which Android Studio version you are using.

### Android Studio Canary
Build the APK bundle by running 

```shell
./gradlew client-app:buildPrivacySandboxSdkApksForDebug
```

This will output a location where the APK is generated. It is automatically signed with your local
debug key for now.

Then, install the APK to your device:
```shell
adb install -t path/to/your/standalone.apk
```

Proceed to the [Client app](#client-app) section.

### Android Studio Stable
Install the SDK provider app by running

```shell
./gradlew sdk-app:installDebug
```

This app has no UI, so move on to running the client app.

When deploying though Android Studio, edit the Run/Debug Configuration for the sdk-app and set
the Launch Options, Launch setting to 'Nothing'. There is no Activity to start, so it would fail.

## Client app

The client app is implemented in the `client-app` module. How you run the client depends on which
version of Android Studio you are using.

### Android Studio Canary
In Android Studio, edit your run configuration as follows:
Edit run configurations > client-app > Deploy > Default APK

Press the run button. Your app should launch and you can proceed to the
[Testing the client](#testing-the-client) section.

### Android Studio Stable

You will need to uncomment and update `<uses-sdk-library>` attribute in the `AndroidManifest.xml`:

- Update your certDigest with your local debug.keystore
```
keytool -list -keystore ~/.android/debug.keystore
```
The default password is `android`
Update the `application/uses-sdk-library/android:certDigest` in `client-app/src/AndroidManifest.xml`
with the `androiddebugkey` SHA-256 certificate fingerprint.
Example: `BA:4E:E2:0E:9C:AA:AA:58:50:F2:...:3F:83:B8:56:C0:08:98`
- Make sure the SDK mentioned in the `AndroidManifest.xml` file is already
  installed to the target device.
- Run the client app on the same target device from Android Studio or by running
  ```shell
  ./gradlew client-app:installDebug
  adb shell am start -n com.example.privacysandbox.client/com.example.client.MainActivity
  ```
  
### Testing the client

- Click on the "load SDK" button, a toast should show that SDK loaded successfully.
- Click on the "Request Webview", this should remote render a webview from the
  sandbox to be viewed inside the activity.

- For more information, please read the [documentation](https://developer.android.com/design-for-safety/privacy-sandbox/guides/sdk-runtime).


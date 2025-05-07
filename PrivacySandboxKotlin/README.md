# SDK Runtime Sample App

This project provides an example of how privacy-preserving SDKs are built and consumed in the [SDK Runtime](https://privacysandbox.google.com/private-advertising/sdk-runtime): an Android 14 environment -with backward compatibility support through Jetpack-, that allows third-party SDKs to run in isolation from the app process, providing stronger safeguards for user data, increased security for apps and SDKs, and independent distribution.

- [Key concepts](#key-concepts)
- [Project structure](#project-structure)
- [Run the sample](#run-the-sample)
- [Debug the sample](#debug-the-sample)

## Key concepts

For each app, there is one SDK Runtime process with a defined set of permissions and restrictions.

SDKs running inside this process are called **Runtime-Enabled SDKs**, or RE SDKs for short.

SDK developers can choose to build a translation SDK to help apps with migration.
These SDKs, which are aware of the SDK Runtime and interact with it, are called **Runtime Aware**, or RA SDKs.

Learn more about building RE SDKs in the [SDK development guide](https://privacysandbox.google.com/private-advertising/sdk-runtime/developer-guide).

## Project structure

This sample illustrates an advertising use case, consisting of a [mediation platform](https://privacysandbox.google.com/private-advertising/sdk-runtime/mediation) in the SDK Runtime which mediates two ad networks: one in the SDK Runtime, and one statically linked to the app. Each ad network sample has its own adapter.

The project has the following modules:

![Project structure diagram](https://github.com/notmariazoe/privacy-sandbox-samples/blob/SDKRTsample-readme-update/PrivacySandboxKotlin/sdkrt-sample-diagram.png?raw=true)

- **client-app**: An app that uses the `runtime-aware-sdk` to communicate with the `runtime-enabled-sdk`.
- **runtime-enabled-sdk**: An SDK made to run in the SDK Runtime environment, also known as a Runtime Enabled (RE) SDK. In this example this RE SDK emulates the use case of a mediation SDK, with calls to other RE SDKs and statically-linked SDKs.
- **runtime-aware-sdk**: The Runtime Aware SDK, which is a statically linked SDK that serves as a translation layer between the client app and the RE SDK (`runtime-enabled-sdk`).
- **in-app-mediatee-sdk**: A statically-linked sample ad network SDK that `runtime-enabled-sdk` mediates. This SDK is not runtime-aware, and serves as an example of an SDK not specifically built to work with the SDK Runtime.
- **in-app-mediatee-sdk-adapter**: A statically linked, runtime-aware SDK that serves as mediation adapter for our static in-app sample ad network, `in-app-mediatee-sdk`.
- **mediatee-sdk**: A runtime-enabled sample ad network SDK that `runtime-enabled-sdk` mediates.
- **mediatee-sdk-adapter**: A runtime-enabled SDK that works as a mediation adapter for our mediation runtime-enabled SDK, `mediatee-sdk`.

### Android SDK Bundles (ASBs)

Runtime-enabled SDKs have to be built as an [Android SDK Bundle (ASB)](https://developer.android.com/studio/command-line/bundletool#asb-format) before they can be published to an app store.

Bundles are where the SDK version, package name and signing information are defined, among others.
They are defined through the metadata of a library module, in their `build.gradle` file, and they're required for the project to build and compile.

When an app or SDK wants to consume a runtime-enabled SDK, it has to depend on its bundle module, not the SDK module.

This project contains the **mediatee-sdk-adapter-bundle**, **mediatee-sdk-bundle**, and **runtime-enabled-sdk-bundle** bundle modules.

## Run the sample app

The following section explains how to prepare your environment to launch the sample app, and debug code that executes in the SDK Runtime.

### Set up your dev environment

Make sure you have upgraded to the latest version of Android Studio.

- Help menu > Find action > Type "Check for updates"

Depending on the Android version of your device or emulator, the sample Runtime-Enabled SDKs will either run on the SDK Runtime process, or statically linked to the app in backward compatible mode, transparently.

To get SDKs to run in the SDK Runtime, you'll need to be on Android 14 or higher.

#### Enable the SDK Runtime on a physical device

While the SDK Runtime is available in all GMS devices with Android 14 or higher, it's behind a configuration flag.

To enable it, you can run the following commands:

```shell
adb shell device_config put adservices global_kill_switch false
adb shell device_config put adservices disable_sdk_sandbox false
```

This isn't necessary on emulators.

### Launch and use the client app

- Open the sample app project in Android Studio.
- Press the Run button to install the SDKs and launch the client app.
- Click Initialize SDK. A toast should show that SDK loaded successfully.
- Click Show Banner View. A banner rendered by the SDK will be
 displayed. If you click it, an Activity customized by the SDK will be launched.

For more information, read the [documentation](https://privacysandbox.google.com/private-advertising/sdk-runtime).

## Debug the sample app

You can debug your client-side app as usual, but there is one more thing to do if you need to debug code in any of the Runtime-Enabled SDKs.

### Debug the runtime-enabled code

As RE SDKs run in a different process than the test app, you have to configure Android Studio to attach a debugger to the SDK Runtime process.

Otherwise, breakpoints in any of the RE SDKs won't work, failing silently.

You can configure Android Studio to attach a debugger to the SDK Runtime process when debugging the app.
For this, the SDK Runtime process has to be already running. If you need to debug the initialization of an RE SDK, and the SDK Runtime process, read the following section.

To attach a debugger to the SDKRT process, you have to:

- In the Run menu, click **Attach debugger to Android Process**.
- Select **Show all processes**.
- Find a process called `<CLIENT_APP_PROCESS>_sdk_sandbox`. In this case, it will be called `com.example.privacysandbox.client_sdk_sandbox`.
- Select `com.example.privacysandbox.client_sdk_sandbox`, and click **OK**.

**Note that** to be able to debug the SDK Runtime process, the client app has to be debuggable. Building with the debug variant should suffice.

### Debug initialization of the runtime-enable code

Since the previous instructions require the SDK Runtime process to be already running, if you want to debug the initialization method, you'll have to start the app's SDK Runtime process manually first.

To start the SDK Runtime process:

- Ensure the client app is already running.
- In the terminal, enter the following commands:
 - `adb shell cmd deviceidle tempwhitelist com.example.privacysandbox.client`
 - `adb shell cmd sdk_sandbox start com.example.privacysandbox.client`
- In the **Run** menu, click **Attach debugger to Android Process**.

Once you have started the SDK Runtime process, will be able to attach a debugger to it, and debug any breakpoints in the initialization method:

- Select `com.example.privacysandbox.client_sdk_sandbox` and click **OK**.
- Click Initialize SDK in the app.

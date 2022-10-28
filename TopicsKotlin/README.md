## Overview

The Topics sample app demonstrates how to initialize and call the Topics API,
constructing a topics request inside of the `AdvertisingTopicsClient` class.
This request can be used to fetch a list of topics that have been assigned to
a given user.

Note that this sample includes several build flavors. Developers should use the "preview" flavor
and can follow the commands described in this README as is. For OEMs to use the required build
flavor, you will need to update the "Preview" part of the commands to "Oems", all other instructions
are the same.

## About the Topics API

The Topics API infers coarse-grained interest signals on-device based on a
user's app usage. These signals, called _topics_, are shared with advertisers to
support interest-based advertising (IBA) use cases without requiring tracking of
individual users across apps.

Every package that makes a call to `getTopics()` is assigned a topic from an
open source standardized taxonomy. For more information and guidance on how
to file change suggestions, visit the [taxonomy repository][taxonomy].

In this sample, we have 11 different sample apps. When an app that’s already
been assigned a topic from the taxonomy calls `getTopics()`, that topic is
returned throughout the current epoch for that app, as well as other apps that
represent overlapping interests. The end result is that, when a user runs many
applications that call into the Topics API, the relative popularity of different
topics entries in a given epoch helps infer the user’s strongest interests.

For a more complete explanation of the Topics API functionality, read the
[design proposal].

# Testing

To test functionality of the Topics API, you need to [set up your development
environment](https://developer.android.com/design-for-safety/privacy-sandbox/setup).
This process involves installing the proper SDK and the device
images needed to test functionality on either a physical device or an emulator.

to enable the Topics API, you’ll need to declare and configure ad services
permissions, and you'll need to run the following commands:

```shell
adb shell device_config put adservices ppapi_app_allow_list \"*\"

adb shell setprop debug.adservices.disable_topics_enrollment_check true
```

Install and launch the Topics sample app on your device with the Privacy Sandbox
on Android image installed. `getTopics()` is called each time the `onResume()`
function is executed and the Topics sample app can be interacted with in the
foreground.

To receive a non-empty result, you must wait for a specific length of time,
called an _epoch_, before the topics are recalculated. By default, an epoch is 7
days. For testing purposes, you can modify the epoch length to get a result more
quickly. To set the epoch period to 5 minutes (300000 milliseconds), execute the
following command in a terminal window:

``` shell
adb shell setprop debug.adservices.topics_epoch_job_period_ms 300000
```

To verify that the epoch length has been adjusted properly, you can use the
`getprop` command. If the above command was successful, then the following
command should return `300000`.

``` shell
adb shell getprop debug.adservices.topics_epoch_job_period_ms
```

In some test cases you may want to start a new epoch immediately and not wait
for the job period to elapse, you can force the system to do so with this
command:

``` shell
adb shell cmd jobscheduler run -f com.google.android.adservices.api  2
```
Note: You should only attempt to run this command after you’ve already called
`getTopics()` at least once. Additionally,  do not attempt to run this command
more than once per epoch.

Now you should again launch the Topics sample app on your device. A `TextView`
object should now appear, which displays either a returned Topic result or a
message that it has been "Returned Empty". The `getTopics()` request returns
an integer ID value for the Topics that are associated with your app. For
these samples we’ve additionally added an array of the taxonomy string values
so that they can be clearly displayed.  If you expect a result and it is still
displaying "Returned Empty", try the following:


* Wait a moment for the epoch calculation to propagate.
* Close and relaunch the app, which will invoke `getTopics()` via
  `onResume()`. Then, run the `jobscheduler` command shown above again, to start
  another epoch.

This sample project is built with 11 different build flavors, each of which
modifies the package name and title. This setup demonstrates a variety of
installed apps that are assigned various topic values. Selecting a different
build flavor will have the Topics API evaluate the app differently using the
on-device classifier and therefore be eligible for a different set of topics
results. The on-device classifier assigns a topic from the taxonomy based on
the app’s title and description.

When testing, you may find it useful to try running different combinations of
test packages to better understand how the system determines which Topics to
return based on overlapping popularity.

To help automate the process of installing and running each of the application
flavors, you can execute the following script from inside of the `TopicsKotlin`
directory, which will install and run each application once. After that, data
should be populated after the following epoch begins, at which point you can
observe the following:

* Which topics are associated with each application.
* Commonly-overlapping topics between samples are more likely to be returned.

Note: Before executing the following you should install and run at least 1 of
the test applications in order to ensure that `getTopics()` is called. After one
of the test apps has been installed and run, you should then run the command to
reduce epoch length so that the batch run will quickly yield useful results.

``` bash
#For UNIX/Linux Users:

#!/bin/bash
# Per Topics data/seed app
for i in {0..10} # Number of apps to install
do
  # Build and install on device or emulator
  echo "${installationString:= installSampleapp${i}_PreviewDebug}"
  ./gradlew ${installationString}
  unset installationString
  # Run app 10 times
   for j in {1..10}
   do
      echo "Running app $i, instance number $j"
      # Start app's main Activity
	adb shell am start -n "com.example.adservices.samples.topics.sampleapp$i/com.example.adservices.samples.topics.sampleapp.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
      # "Press/Swipe" Home button
     adb shell input keyevent KEYCODE_HOME
  done
done
```

In addition to the above, you can use the following script to automatically
uninstall all of the sample applications:

``` bash
for i in {0..10} # Number of apps to uninstall
do
  echo "${uninstallString:= uninstallSampleapp${i}_PreviewDebug}"
  ./gradlew ${uninstallString}
  unset uninstallString
done
```

Once you’ve finished testing using the sample apps provided, try changing the
app title and description when building in order to see different results
returned from the on-device classifier. Use the button in the app to launch
the settings UI and view the Topics data for the device similar to how it
will be presented to end users.

[design proposal]: https://developer.android.com/design-for-safety/privacy-sandbox/topics#how-it-works
[set up your development environment]: https://developer.android.com/design-for-safety/privacy-sandbox/setup
[taxonomy]: https://github.com/privacysandbox/topics-android

## Overview
 
The Topics sample app demonstrates how to initialize and call the Topics API in
the following ways:
 
* Constructs a topics request inside of the `AdvertisingTopicsClient` class.
  This request can be used to fetch a list of topics that have been assigned to
  a given user.
* Creates a button inside of the `MainActivity` which, when pressed,
  initiates the call to retrieve those topics and to display them on screen.
 
## About the Topics API
 
The Topics API infers coarse-grained interest signals on-device based on a
user's app usage. These signals, called _topics_, are shared with advertisers to
support interest-based advertising (IBA) use cases without requiring tracking of
individual users across apps.
 
Every package that makes a call to `getTopics()` is assigned a topic from a
standardized [taxonomy].
 
In this sample, we have 11 different sample apps. When an app that’s already
been assigned a topic from the taxonomy calls `getTopics()`, that topic is
returned throughout the current epoch for that app, as well as other apps that
represent overlapping interests. The end result is that, when a user runs many
applications that call into the Topics API, the relative popularity of different
topics entries in a given epoch helps infer the user’s strongest interests.
 
For a more complete explanation of the Topics API functionality, read the
[design proposal]:
 
# Testing
 
To test functionality of the Topics API, you need to [set up your development
environment]. This process involves installing the proper SDK and the device
images needed to test functionality on either a physical device or an emulator.
 
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
message that it has been "Returned Empty". If you expect a result and it is
still displaying "Returned Empty", try the following:
 
* Wait a moment for the epoch calculation to propagate.
* Close and relaunch the app, which will invoke `getTopics()` via
  `onResume()`. Then, run the `jobscheduler` command shown above again, to start
  another epoch.
 
This sample project is built with 11 different build flavors, each of which
modifies the package name, representing 11 test package names included in the
taxonomy. This setup demonstrates a variety of installed apps that are assigned
various topic values. Selecting a different build flavor will have the Topics
API treat the application as a different entry in the taxonomy, and therefore
eligible for a different set of topics results (This functionality is specific
to Developer Preview 1, with future releases slated to include on-device
classification so that the package will not need to be included in the
classification table in order to receive a result). The current results will be
based on the [taxonomy for Chrome][taxonomy]. In a future release this will be
updated to a Taxonomy specific to Android.
 
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
  echo "${installationString:= installSampleapp${i}_Debug}"
  ./gradlew ${installationString}
  unset installationString
done
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
  echo "${uninstallString:= uninstallSampleapp${i}_Debug}"
  ./gradlew ${uninstallString}
  unset uninstallString
done
```
 
[design proposal]: https://developer.android.com/design-for-safety/privacy-sandbox/topics#how-it-works
[set up your development environment]: https://developer.android.com/design-for-safety/privacy-sandbox/setup
[taxonomy]: https://github.com/patcg-individual-drafts/topics/blob/main/taxonomy_v1.md

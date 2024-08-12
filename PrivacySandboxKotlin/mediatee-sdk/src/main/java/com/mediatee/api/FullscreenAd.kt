package com.mediatee.api

import androidx.privacysandbox.activity.core.SdkActivityLauncher
import androidx.privacysandbox.tools.PrivacySandboxInterface

@PrivacySandboxInterface
interface FullscreenAd {
    suspend fun show(activityLauncher: SdkActivityLauncher)
}
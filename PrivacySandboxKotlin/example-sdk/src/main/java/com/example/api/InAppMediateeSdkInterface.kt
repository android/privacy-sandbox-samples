package com.example.api

import androidx.privacysandbox.tools.PrivacySandboxCallback

@PrivacySandboxCallback
interface InAppMediateeSdkInterface {
    suspend fun show()
}
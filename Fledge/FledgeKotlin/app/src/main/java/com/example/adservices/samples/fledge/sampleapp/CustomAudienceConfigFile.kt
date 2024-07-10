package com.example.adservices.samples.fledge.sampleapp

import android.adservices.customaudience.CustomAudience
import android.adservices.customaudience.FetchAndJoinCustomAudienceRequest
import com.google.common.collect.ImmutableList

class CustomAudienceConfigFile(
    val customAudiences: ImmutableList<CustomAudience>,
    val fetchAndJoinCustomAudiences: ImmutableList<FetchAndJoinCustomAudienceRequest>
)

package com.example.adservices.samples.fledge.sampleapp

import android.adservices.customaudience.CustomAudience
import android.adservices.customaudience.FetchAndJoinCustomAudienceRequest
import com.google.common.collect.ImmutableBiMap

class CustomAudienceConfigFile(
    val customAudiences: ImmutableBiMap<String, CustomAudience>,
    val fetchAndJoinCustomAudiences: ImmutableBiMap<String, FetchAndJoinCustomAudienceRequest>
)

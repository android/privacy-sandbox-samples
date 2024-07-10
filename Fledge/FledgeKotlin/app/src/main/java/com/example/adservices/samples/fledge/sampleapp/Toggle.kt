package com.example.adservices.samples.fledge.sampleapp

interface Toggle {
    val label: String?

    fun onSwitchToggle(active: Boolean): Boolean
}

package com.example.adservices.samples.fledge.sampleapp

import android.adservices.common.AdSelectionSignals
import android.adservices.customaudience.AddCustomAudienceOverrideRequest
import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("NewApi")
class RemoteOverridesToggle(
    private val overrideScoringLogic: String,
    private val overrideScoringSignals: AdSelectionSignals,
    private val mCustomAudienceRemoteOverrides: List<AddCustomAudienceOverrideRequest>,
    private val adSelectionWrapper: AdSelectionWrapper,
    private val mCustomAudienceWrapper: CustomAudienceWrapper,
    private val mEventLog: EventLogManager,
    private val mContext: Context
) : Toggle {
    override val label: String
        get() = mContext.getString(R.string.override_switch_text)

    override fun onSwitchToggle(active: Boolean): Boolean {
        if (active) {
            adSelectionWrapper.overrideAdSelection(
                { event: String? ->
                    mEventLog.writeEvent(
                        event!!
                    )
                }, overrideScoringLogic, overrideScoringSignals
            )
            for (customAudienceOverrideRequest in mCustomAudienceRemoteOverrides) {
                mCustomAudienceWrapper.addCAOverride(
                    customAudienceOverrideRequest.name,
                    customAudienceOverrideRequest.buyer,
                    customAudienceOverrideRequest.biddingLogicJs,
                    customAudienceOverrideRequest.trustedBiddingSignals
                ) { event: String? ->
                    mEventLog.writeEvent(
                        event!!
                    )
                }
            }
        } else {
            adSelectionWrapper.resetAdSelectionOverrides { event: String? ->
                mEventLog.writeEvent(
                    event!!
                )
            }
            mCustomAudienceWrapper.resetCAOverrides { event: String? ->
                mEventLog.writeEvent(
                    event!!
                )
            }
        }
        return true
    }
}

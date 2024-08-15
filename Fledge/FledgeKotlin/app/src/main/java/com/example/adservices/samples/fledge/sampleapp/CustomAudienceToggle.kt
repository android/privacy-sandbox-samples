package com.example.adservices.samples.fledge.sampleapp

import android.adservices.customaudience.CustomAudience
import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("NewApi")
class CustomAudienceToggle internal constructor(
    private val mLabelName: String,
    private val mCustomAudience: CustomAudience,
    private val mCustomAudienceWrapper: CustomAudienceWrapper,
    private val mEventLog: EventLogManager,
    private val mContext: Context,
) : Toggle {
    override val label: String
        get() = mContext.getString(R.string.ca_toggle, mLabelName)

    override fun onSwitchToggle(active: Boolean): Boolean {
        return if (active) joinCustomAudience() else leaveCustomAudience()
    }

    private fun joinCustomAudience(): Boolean {
        mCustomAudienceWrapper.joinCa(mCustomAudience, mEventLog::writeEvent)
        return true
    }

    private fun leaveCustomAudience(): Boolean {
        mCustomAudienceWrapper.leaveCa(mCustomAudience.name, mCustomAudience.buyer, mEventLog::writeEvent)
        return true
    }
}

package com.example.adservices.samples.fledge.sampleapp

import android.adservices.common.AdTechIdentifier
import android.adservices.customaudience.FetchAndJoinCustomAudienceRequest
import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("NewApi")
class FetchAndJoinCustomAudienceToggle internal constructor(
    private val mLabelName: String,
    private val mFetchAndJoinCustomAudienceRequest: FetchAndJoinCustomAudienceRequest,
    private val mCustomAudienceWrapper: CustomAudienceWrapper,
    private val mEventLog: EventLogManager,
    private val mContext: Context
) : Toggle {
    override val label: String
        get() = mContext.getString(
            R.string.fetch_and_join_ca_toggle, mLabelName
        )

    override fun onSwitchToggle(active: Boolean): Boolean {
        return if (active) joinCustomAudience() else leaveCustomAudience()
    }

    private fun joinCustomAudience(): Boolean {
        mCustomAudienceWrapper.fetchAndJoinCa(mFetchAndJoinCustomAudienceRequest, mEventLog::writeEvent)
        return true
    }

    private fun leaveCustomAudience(): Boolean {
        mCustomAudienceWrapper.leaveCa(
            mFetchAndJoinCustomAudienceRequest.name!!,
            AdTechIdentifier.fromString(mFetchAndJoinCustomAudienceRequest.fetchUri.host!!),
            mEventLog::writeEvent
        )
        return true
    }
}

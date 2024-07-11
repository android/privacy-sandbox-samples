package com.example.adservices.samples.fledge.sampleapp

import android.adservices.customaudience.CustomAudience
import android.adservices.customaudience.CustomAudienceManager
import android.adservices.customaudience.JoinCustomAudienceRequest
import android.adservices.customaudience.LeaveCustomAudienceRequest
import android.annotation.SuppressLint
import android.content.Context
import android.os.OutcomeReceiver
import android.util.Log
import java.util.concurrent.Executor

@SuppressLint("NewApi")
class CustomAudienceToggle internal constructor(
    private val mCustomAudience: CustomAudience,
    private val mCustomAudienceManager: CustomAudienceManager,
    private val mExecutor: Executor,
    private val mEventLog: EventLogManager,
    private val mContext: Context
) : Toggle {
    override val label: String
        get() = mContext.getString(R.string.ca_toggle, mCustomAudience.name)

    override fun onSwitchToggle(newValue: Boolean): Boolean {
        return if (newValue) joinCustomAudience() else leaveCustomAudience()
    }

    fun joinCustomAudience(): Boolean {
        mCustomAudienceManager.joinCustomAudience(
            JoinCustomAudienceRequest.Builder().setCustomAudience(mCustomAudience).build(),
            mExecutor,
            object : OutcomeReceiver<Any?, Exception> {
                override fun onResult(o: Any?) {
                    mEventLog.writeEvent(
                        "Joined custom audience successfully: "
                                + mCustomAudience.name
                    )
                    Log.d(MainActivity.TAG, "Joined CA:$mCustomAudience")
                }

                override fun onError(error: Exception) {
                    mEventLog.writeEvent(
                        String.format(
                            "Failed to join custom audience %s (%s)",
                            mCustomAudience.name, error.message
                        )
                    )
                }
            })
        return true
    }

    fun leaveCustomAudience(): Boolean {
        mCustomAudienceManager.leaveCustomAudience(
            LeaveCustomAudienceRequest.Builder()
                .setName(mCustomAudience.name)
                .setBuyer(mCustomAudience.buyer)
                .build(),
            mExecutor,
            object : OutcomeReceiver<Any?, Exception> {
                override fun onResult(o: Any?) {
                    mEventLog.writeEvent(
                        "Left custom audience successfully: " + mCustomAudience.name
                    )
                }

                override fun onError(error: Exception) {
                    mEventLog.writeEvent(
                        String.format(
                            "Failed to leave custom audience %s as %s",
                            mCustomAudience.name, error.message
                        )
                    )
                }
            })
        return true
    }
}

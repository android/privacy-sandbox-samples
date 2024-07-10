package com.example.adservices.samples.fledge.sampleapp

import android.adservices.common.AdTechIdentifier
import android.adservices.customaudience.CustomAudienceManager
import android.adservices.customaudience.FetchAndJoinCustomAudienceRequest
import android.adservices.customaudience.LeaveCustomAudienceRequest
import android.annotation.SuppressLint
import android.content.Context
import android.os.OutcomeReceiver
import android.util.Log
import java.util.Objects
import java.util.concurrent.Executor

@SuppressLint("NewApi")
class FetchAndJoinCustomAudienceToggle internal constructor(
    private val mFetchAndJoinCustomAudienceRequest: FetchAndJoinCustomAudienceRequest,
    private val mCustomAudienceManager: CustomAudienceManager,
    private val mExecutor: Executor,
    private val mEventLog: EventLogManager,
    private val mContext: Context
) : Toggle {
    override val label: String
        get() = mContext.getString(
            R.string.fetch_and_join_ca_toggle, mFetchAndJoinCustomAudienceRequest.name
        )

    override fun onSwitchToggle(newValue: Boolean): Boolean {
        return if (newValue) joinCustomAudience() else leaveCustomAudience()
    }

    fun joinCustomAudience(): Boolean {
        mCustomAudienceManager.fetchAndJoinCustomAudience(
            mFetchAndJoinCustomAudienceRequest,
            mExecutor,
            object : OutcomeReceiver<Any?, Exception> {
                override fun onResult(o: Any?) {
                    mEventLog.writeEvent(
                        "Fetched and joined custom audience successfully: "
                                + mFetchAndJoinCustomAudienceRequest.fetchUri
                    )
                    Log.d(MainActivity.TAG, "Joined CA:$mFetchAndJoinCustomAudienceRequest")
                }

                override fun onError(error: Exception) {
                    mEventLog.writeEvent(
                        String.format(
                            "Failed to fetch and join custom audience %s (%s)",
                            mFetchAndJoinCustomAudienceRequest.fetchUri,
                            error.message
                        )
                    )
                }
            })
        return true
    }

    fun leaveCustomAudience(): Boolean {
        mCustomAudienceManager.leaveCustomAudience(
            LeaveCustomAudienceRequest.Builder()
                .setName(
                    Objects.requireNonNull(
                        mFetchAndJoinCustomAudienceRequest.name
                    ).toString()
                )
                .setBuyer(
                    AdTechIdentifier.fromString(
                        Objects.requireNonNull(
                            mFetchAndJoinCustomAudienceRequest
                                .fetchUri
                                .host
                        ).toString()
                    )
                )
                .build(),
            mExecutor,
            object : OutcomeReceiver<Any?, Exception> {
                override fun onResult(o: Any?) {
                    mEventLog.writeEvent(
                        "Left custom audience successfully: "
                                + mFetchAndJoinCustomAudienceRequest.name
                    )
                }

                override fun onError(error: Exception) {
                    mEventLog.writeEvent(
                        String.format(
                            "Failed to leave custom audience %s as %s",
                            mFetchAndJoinCustomAudienceRequest.name,
                            error.message
                        )
                    )
                }
            })
        return true
    }
}

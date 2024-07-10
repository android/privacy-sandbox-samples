package com.example.adservices.samples.fledge.sampleapp

import android.adservices.customaudience.CustomAudienceManager
import android.annotation.SuppressLint
import android.content.Context
import org.json.JSONException
import java.io.IOException
import java.util.concurrent.Executor

@SuppressLint("NewApi")
class ToggleProvider(
    private val mContext: Context,
    private val mEventLog: EventLogManager,
    private val mCustomAudienceManager: CustomAudienceManager,
    private val mAdSelectionWrapper: AdSelectionWrapper,
    config: ConfigUris,
    executor: Executor
) {
    private val mConfigFileLoader = ConfigFileLoader(mContext, config, mEventLog::writeEvent)
    private val mExecutor = executor

    @get:Throws(JSONException::class, IOException::class)
    val toggles: List<Toggle>
        get() {
            val toggles: MutableList<Toggle> = ArrayList()

            val data = mConfigFileLoader.loadCustomAudienceConfigFile(CONFIG_JSON)
            val remoteOverridesConfigFile =
                mConfigFileLoader.loadRemoteOverridesConfigFile(REMOTE_OVERRIDES_JSON)
            if (remoteOverridesConfigFile.hasOverrides()) {
                toggles.add(
                    RemoteOverridesToggle(
                        remoteOverridesConfigFile.scoringLogic,
                        remoteOverridesConfigFile.trustedScoringSignals,
                        remoteOverridesConfigFile.overrides,
                        mAdSelectionWrapper,
                        CustomAudienceWrapper(mExecutor, mContext),
                        mEventLog,
                        mContext
                    )
                )
            }

            for (customAudience in data.customAudiences) {
                toggles.add(
                    CustomAudienceToggle(
                        customAudience,
                        mCustomAudienceManager,
                        mExecutor,
                        mEventLog,
                        mContext
                    )
                )
            }
            for (fetchAndJoinCustomAudienceRequest in data.fetchAndJoinCustomAudiences) {
                toggles.add(
                    FetchAndJoinCustomAudienceToggle(
                        fetchAndJoinCustomAudienceRequest,
                        mCustomAudienceManager,
                        mExecutor,
                        mEventLog,
                        mContext
                    )
                )
            }
            return toggles
        }

    companion object {
        // Replace these with different file paths if desired.
        const val REMOTE_OVERRIDES_JSON: String = "RemoteOverrides.json"
        const val CONFIG_JSON: String = "DefaultConfig.json"
    }
}

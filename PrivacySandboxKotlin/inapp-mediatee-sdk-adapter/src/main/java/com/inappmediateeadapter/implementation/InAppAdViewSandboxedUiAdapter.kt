package com.inappmediateeadapter.implementation

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionConstants
import androidx.privacysandbox.ui.provider.AbstractSandboxedUiAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import java.util.concurrent.Executor

/**
 * SandboxedUiAdapter containing the Ad view from the inapp mediatee.
 *
 * This is to be shared with the mediator running in Runtime, which will then return it to the app
 * when app requests for winning ad.
 */
class InAppAdViewSandboxedUiAdapter(private val mediateeAdView: View):
    AbstractSandboxedUiAdapter(), SandboxedUiAdapter {
    /**
     * Opens a new session to display remote UI.
     * The session will handle notifications from and to the client.
     * We consider the client the owner of the SandboxedSdkView.
     *
     @param context The client's context.
     * @param sessionConstants Constants related to the session, such as the presentation id.
     * @param initialWidth The initial width of the adapter's view.
     * @param initialHeight The initial height of the adapter's view.
     * @param isZOrderOnTop Whether the session's view should be drawn on top of other views.
     * @param clientExecutor The executor to use for client callbacks.
     * @param client A UI adapter representing the client of this single session.
     */
    override fun openSession(
        context: Context,
        sessionConstants: SessionConstants,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SandboxedUiAdapter.SessionClient
    ) {
        val session = SdkUiSession(clientExecutor, mediateeAdView)
        clientExecutor.execute {
            client.onSessionOpened(session)
        }
    }
}

private class SdkUiSession(clientExecutor: Executor, mediateeAdView: View) :
    AbstractSandboxedUiAdapter.AbstractSession() {

    /** A scope for launching coroutines in the client executor. */
    private val scope = CoroutineScope(clientExecutor.asCoroutineDispatcher() + Job())

    override val view: View = mediateeAdView

    override fun close() {
        // Notifies that the client has closed the session. It's a good opportunity to dispose
        // any resources that were acquired to maintain the session.
        scope.cancel()
    }

    override fun notifyConfigurationChanged(configuration: Configuration) {
        // Notifies that the device configuration has changed and affected the app.
    }

    override fun notifyResized(width: Int, height: Int) {
        // Notifies that the size of the presentation area in the app has changed.
    }

    override fun notifyUiChanged(uiContainerInfo: Bundle) {
        // Notify the session when the presentation state of its UI container has changed.
    }

    override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
        // Notifies that the Z order has changed for the UI associated by this session.
    }
}
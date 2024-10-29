package com.inappmediateeadapter.implementation

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionObserverFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import java.util.concurrent.Executor

class SandboxedUiAdapterImpl(private val mediateeAdView: View): SandboxedUiAdapter {
    override fun openSession(
        context: Context,
        windowInputToken: IBinder,
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

    override fun addObserverFactory(sessionObserverFactory: SessionObserverFactory) {
        // Adds a [SessionObserverFactory] with a [SandboxedUiAdapter] for tracking UI presentation
        // state across UI sessions. This has no effect on already open sessions.
    }

    override fun removeObserverFactory(sessionObserverFactory: SessionObserverFactory) {
        // Removes a [SessionObserverFactory] from a [SandboxedUiAdapter], if it has been
        // previously added with [addObserverFactory].
    }
}

private class SdkUiSession(clientExecutor: Executor, mediateeAdView: View) :
    SandboxedUiAdapter.Session {

    /** A scope for launching coroutines in the client executor. */
    private val scope = CoroutineScope(clientExecutor.asCoroutineDispatcher() + Job())

    override val signalOptions: Set<String> = setOf()

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
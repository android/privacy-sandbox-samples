package com.example.adservices.samples.fledge.clients

import android.os.OutcomeReceiver

interface NullableOutcomeReceiver<R, E : Throwable?> : OutcomeReceiver<R, E> {
  @Suppress("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
  override fun onResult(result: R)

  @Suppress("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
  override fun onError(error: E)
}
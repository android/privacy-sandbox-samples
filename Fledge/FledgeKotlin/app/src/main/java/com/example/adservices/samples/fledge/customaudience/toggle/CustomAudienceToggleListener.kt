package com.example.adservices.samples.fledge.customaudience.toggle

import java.util.function.Consumer

interface CustomAudienceToggleListener {
  /**
   * Called when the state of a toggle is changed.
   *
   * @param toggle The custom audience toggle that was changed.
   * @param isChecked The new checked state of the toggle.
   * @param statusReceiver A consumer function returning a string indicating the outcome of the toggle action.
   */
  fun onSwitchToggle(
    toggle: CustomAudienceToggle,
    isChecked: Boolean,
    statusReceiver: Consumer<String>
  )
}
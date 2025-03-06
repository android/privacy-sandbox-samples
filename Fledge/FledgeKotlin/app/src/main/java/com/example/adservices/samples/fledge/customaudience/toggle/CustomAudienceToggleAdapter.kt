/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.adservices.samples.fledge.customaudience.toggle

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.TAG
import com.example.adservices.samples.fledge.customaudience.toggle.CustomAudienceToggle.FetchAndJoinCustomAudienceToggle
import com.example.adservices.samples.fledge.customaudience.toggle.CustomAudienceToggle.JoinCustomAudienceToggle
import com.example.adservices.samples.fledge.sampleapp.R
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.function.Consumer

class CustomAudienceToggleAdapter(
  private val statusReceiver: Consumer<String>,
  private val listener: CustomAudienceToggleListener
) : ListAdapter<CustomAudienceToggle, CustomAudienceToggleAdapter.ViewHolder>(DiffCallback) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context)
        .inflate(R.layout.layout_toggle, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val switchView = holder.switch
    val toggle = getItem(position)

    switchView.text = switchView.context.getString(toggle.labelPrefixResId, toggle.label)

    switchView.setOnCheckedChangeListener { _, isChecked ->
      Log.v(TAG, "Option ${toggle.label} is checked $isChecked")
      listener.onSwitchToggle(toggle, isChecked, statusReceiver)
    }
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val switch: SwitchMaterial = itemView.findViewById(R.id.switch_view)
  }

  private object DiffCallback: DiffUtil.ItemCallback<CustomAudienceToggle>() {
    override fun areItemsTheSame(oldItem: CustomAudienceToggle, newItem: CustomAudienceToggle): Boolean {
      return oldItem.label == newItem.label
    }

    @OptIn(ExperimentalFeatures.Ext10OptIn::class)
    override fun areContentsTheSame(oldItem: CustomAudienceToggle, newItem: CustomAudienceToggle): Boolean {
      return when(oldItem) {
        is JoinCustomAudienceToggle -> oldItem.customAudience == (newItem as JoinCustomAudienceToggle).customAudience
        is FetchAndJoinCustomAudienceToggle -> oldItem.request == (newItem as FetchAndJoinCustomAudienceToggle).request
      }
    }
  }
}
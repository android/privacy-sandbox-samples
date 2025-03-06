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
package com.example.adservices.samples.fledge.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.example.adservices.samples.fledge.sampleapp.R
import com.example.adservices.samples.fledge.sampleapp.databinding.ActivityMainBinding
import com.example.adservices.samples.fledge.util.VersionCompatUtil.isSdkCompatible
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : FragmentActivity() {
  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    if(!isSdkCompatible(5, 9)) {
      showAlertDialog(
        title = getString(R.string.unsupported_sdk_extension_title),
        message = getString(R.string.unsupported_sdk_extension_desc)) {
        finishAffinity()
      }
    } else {
      setupView()
    }
  }

  private fun setupView() {
    binding.viewPager.visibility = View.VISIBLE
    binding.viewPager.adapter = MainActivityFragmentStateAdapter(this)

    TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
      tab.text = Tab.mainActivityTabList[position]?.title
    }.attach()
  }

  private fun showAlertDialog(title: String, message: String, onClick: () -> Unit) {
    AlertDialog.Builder(this)
      .setTitle(title)
      .setMessage(message)
      .setPositiveButton("OK") { _, _ ->
        onClick.invoke()
      }
      .show()
  }
}

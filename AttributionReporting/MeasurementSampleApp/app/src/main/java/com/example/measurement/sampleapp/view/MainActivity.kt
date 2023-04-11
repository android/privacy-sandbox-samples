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
package com.example.measurement.sampleapp.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.measurement.sampleapp.R
import com.example.measurement.sampleapp.databinding.ActivityMainBinding

import android.widget.EditText
import com.example.measurement.sampleapp.view.base.BaseActivity
import com.example.measurement.sampleapp.viewmodel.MainViewModel

/*
* MainActivity
* This is the main activity that contains SourceFragment, TriggerFragment and ReportFragment.
* */
class MainActivity : BaseActivity() {

  /*
  * binding
  * This is the view binding reference to access view elements.
  * */
  private lateinit var binding: ActivityMainBinding

  /*
  * mainViewModel
  * This is the Main ViewModel reference that delegates storage operations.
  * */
  private val mainViewModel by lazy { provideViewModel<MainViewModel>() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val navView: BottomNavigationView = binding.navView

    val navController = findNavController(R.id.nav_host_fragment_activity_main)

    val appBarConfiguration = AppBarConfiguration(setOf(
      R.id.navigation_source, R.id.navigation_trigger))
    setupActionBarWithNavController(navController, appBarConfiguration)
    navView.setupWithNavController(navController)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.menu_server_source_url) {
      showServerSourceUrlDialog()
    } else if (item.itemId == R.id.menu_server_trigger_url) {
      showServerTriggerUrlDialog()
    }
    return super.onOptionsItemSelected(item)
  }

  /*
  * showServerUrlDialog
  * This method shows a dialog in which the server url is configurable
  * */
  private fun showServerSourceUrlDialog(){
    val editText = EditText(this).apply { setText(mainViewModel.getServerSourceUrl()) }
    val alert = AlertDialog.Builder(this).apply {
     setTitle(R.string.server_source_url)
     setView(editText)
     setPositiveButton(R.string.save) { _, _ ->
        mainViewModel.setServerSourceUrl(editText.text.toString())
      }
    }
    alert.show()
  }

  private fun showServerTriggerUrlDialog(){
    val editText = EditText(this).apply { setText(mainViewModel.getServerTriggerUrl()) }
    val alert = AlertDialog.Builder(this).apply {
      setTitle(R.string.server_trigger_url)
      setView(editText)
      setPositiveButton(R.string.save) { _, _ ->
        mainViewModel.setServerTriggerUrl(editText.text.toString())
      }
    }
    alert.show()
  }

}
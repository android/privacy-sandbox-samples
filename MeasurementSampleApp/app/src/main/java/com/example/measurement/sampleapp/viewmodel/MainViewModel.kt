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
package com.example.measurement.sampleapp.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.measurement.sampleapp.di.module.DEFAULT_CONVERSION_REGISTRATION_ID
import com.example.measurement.sampleapp.di.module.DEFAULT_SERVER_URL
import com.example.measurement.sampleapp.di.module.DEFAULT_SOURCE_REGISTRATION_ID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


/*
* Datastore Keys
* */
private const val DATASTORE_KEY_SERVER_URL = "DATASTORE_KEY_SERVER_URL"
private const val DATASTORE_KEY_SOURCE_REGISTRATION_ID = "DATASTORE_KEY_SOURCE_REGISTRATION_ID"
private const val DATASTORE_KEY_CONVERSION_REGISTRATION_ID= "DATASTORE_KEY_CONVERSION_REGISTRATION_ID"

/*
* dataStore
* This is the DataStore instance.
* */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
  name = "measurement-sample-app-datastore"
)

/*
* MainViewModel
* This is the Main viewModel that handles storage operations.
* */
class MainViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

  /*
  * These are methods to read and write to local storage.
  * */
  
     fun setServerUrl(serverUrl: String) {
       viewModelScope.launch {
        getApplication<Application>().applicationContext.dataStore.edit { preferences ->
          preferences[stringPreferencesKey(DATASTORE_KEY_SERVER_URL)] = serverUrl
        }
      }
     }

   fun getServerUrl(): String {
     val serverUrl : String
     runBlocking(Dispatchers.IO){
      serverUrl = getApplication<Application>().applicationContext.dataStore.data.map { preferences ->
        preferences[stringPreferencesKey(DATASTORE_KEY_SERVER_URL)] ?: DEFAULT_SERVER_URL
      }.first()
    }
     return serverUrl
  }
  

  fun setSourceRegistrationId(sourceRegistrationId: String) {
    viewModelScope.launch {
      getApplication<Application>().applicationContext.dataStore.edit { preferences ->
        preferences[stringPreferencesKey(DATASTORE_KEY_SOURCE_REGISTRATION_ID)] = sourceRegistrationId
      }
    }
  }
  
  fun getSourceRegistrationId(): String {
    val serverUrl : String
    runBlocking(Dispatchers.IO){
      serverUrl = getApplication<Application>().applicationContext.dataStore.data.map { preferences ->
        preferences[stringPreferencesKey(DATASTORE_KEY_SOURCE_REGISTRATION_ID)] ?: DEFAULT_SOURCE_REGISTRATION_ID
      }.first()
    }
    return serverUrl
  }


  fun setConvId(convId: String) {
    viewModelScope.launch {
      getApplication<Application>().applicationContext.dataStore.edit { preferences ->
        preferences[stringPreferencesKey(DATASTORE_KEY_CONVERSION_REGISTRATION_ID)] = convId
      }
    }
  }

  fun getConversionRegistrationId(): String {
    val serverUrl : String
    runBlocking(Dispatchers.IO){
      serverUrl = getApplication<Application>().applicationContext.dataStore.data.map { preferences ->
        preferences[stringPreferencesKey(DATASTORE_KEY_CONVERSION_REGISTRATION_ID)] ?: DEFAULT_CONVERSION_REGISTRATION_ID
      }.first()
    }
    return serverUrl
  }

}
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
package com.example.measurement.sampleapp.di.module

import android.adservices.measurement.MeasurementManager
import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides

/*
* Default values
* */
var DEFAULT_SERVER_URL  = "https://measurement.sample.server.url.com"
var DEFAULT_SOURCE_REGISTRATION_ID = "1"
var DEFAULT_CONVERSION_REGISTRATION_ID = "1"

/*
* AppModule
* This class provides app modules. Modules that are used in the app.
* */
@Module
internal class AppModule() {
  @Provides
  fun provideContext(application: Application): Context = application.applicationContext

  @Provides
  fun provideMeasurementManager(context: Context): MeasurementManager =
   context.getSystemService(MeasurementManager::class.java)
}
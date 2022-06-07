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
package com.example.measurement.sampleapp.di.component

import dagger.Component
import android.app.Application
import com.example.measurement.sampleapp.MeasurementSampleApp
import com.example.measurement.sampleapp.di.module.AppModule
import com.example.measurement.sampleapp.di.module.ViewModelModule
import com.example.measurement.sampleapp.di.module.ViewModule
import dagger.BindsInstance
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector

/*
* AppComponent
* This is the component class that annotates the interface Builder
* */
@Component(modules = [AndroidInjectionModule::class, AppModule::class, ViewModule::class, ViewModelModule::class])
interface AppComponent : AndroidInjector<MeasurementSampleApp> {

/*
* Builder
* This is an interface that is used to build AppComponent
* */
  @Component.Builder
  interface Builder {
    @BindsInstance
    fun application(application: Application): Builder

    fun build(): AppComponent
  }

/*
* Injects the MeasurementSampleApp
* */
  override fun inject(application: MeasurementSampleApp)
}
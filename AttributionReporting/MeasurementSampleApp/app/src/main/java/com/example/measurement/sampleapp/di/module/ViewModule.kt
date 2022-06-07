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

import com.example.measurement.sampleapp.view.MainActivity
import com.example.measurement.sampleapp.view.SourceFragment
import com.example.measurement.sampleapp.view.TriggerFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

/*
* ViewModule
* This class provides view modules. Fragment, Activity and Custom View modules.
* */
@Module
internal interface ViewModule {
  @ContributesAndroidInjector
  fun provideMainActivity(): MainActivity

  @ContributesAndroidInjector
  fun provideSourceFragment(): SourceFragment

  @ContributesAndroidInjector
  fun provideTriggerFragment(): TriggerFragment
}
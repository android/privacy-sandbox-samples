/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.mediateeadapter.api

import androidx.privacysandbox.tools.PrivacySandboxService

/**
 * SDK Service for the Runtime-enabled Adapter - which is empty for our sample.
 *
 * Every Runtime-enabled SDK has to define a PrivacySandboxService, which is the interface used to
 * communicate with the SDK.
 */
@PrivacySandboxService
interface SdkService
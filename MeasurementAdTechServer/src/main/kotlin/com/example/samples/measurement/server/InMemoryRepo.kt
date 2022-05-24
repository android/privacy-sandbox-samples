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
package com.example.samples.measurement.server

import java.util.Optional
import org.springframework.stereotype.Repository

/**
 * Class for storing objects in memory by ID.
 */
@Repository
class InMemoryRepo<T : Any, ID> {
  private val dataStore: HashMap<ID, T> = HashMap()

  fun findById(id: ID): Optional<T> {
    return Optional.ofNullable(dataStore[id])
  }

  fun save(id: ID, obj: T) {
    dataStore[id] = obj
  }

  fun getAll(): Iterable<T> {
    return dataStore.values
  }
}

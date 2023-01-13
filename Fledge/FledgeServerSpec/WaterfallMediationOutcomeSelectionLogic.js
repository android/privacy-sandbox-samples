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

/**
 * Returns the outcome passed if its bid is higher than the bid floor,
 * otherwise returns null
 */
function selectOutcome(outcomes, selection_signals) {
    const outcome_1p = outcomes[0];
    const bid_floor = selection_signals.bid_floor;
    return {'status': 0, 'result': (outcome_1p.bid >= bid_floor) ? outcome_1p : null};
}

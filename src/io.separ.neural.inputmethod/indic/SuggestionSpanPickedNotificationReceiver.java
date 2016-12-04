/*
 * Copyright (C) 2011 The Android Open Source Project
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

package io.separ.neural.inputmethod.indic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.style.SuggestionSpan;
import android.util.Log;

import io.separ.neural.inputmethod.indic.define.DebugFlags;

public final class SuggestionSpanPickedNotificationReceiver extends BroadcastReceiver {
    private static final boolean DBG = DebugFlags.DEBUG_ENABLED;
    private static final String TAG =
            SuggestionSpanPickedNotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SuggestionSpan.ACTION_SUGGESTION_PICKED.equals(intent.getAction())) {
            if (DBG) {
                final String before = intent.getStringExtra(
                        SuggestionSpan.SUGGESTION_SPAN_PICKED_BEFORE);
                final String after = intent.getStringExtra(
                        SuggestionSpan.SUGGESTION_SPAN_PICKED_AFTER);
                Log.d(TAG, "Received notification picked: " + before + ',' + after);
            }
        }
    }
}

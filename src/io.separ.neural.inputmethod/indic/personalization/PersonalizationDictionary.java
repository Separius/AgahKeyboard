/*
 * Copyright (C) 2013 The Android Open Source Project
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

package io.separ.neural.inputmethod.indic.personalization;

import android.content.Context;

import java.io.File;
import java.util.Locale;

import io.separ.neural.inputmethod.annotations.UsedForTesting;
import io.separ.neural.inputmethod.indic.Dictionary;

public class PersonalizationDictionary extends DecayingExpandableBinaryDictionaryBase {
    /* package */ static final String NAME = PersonalizationDictionary.class.getSimpleName();

    // TODO: Make this constructor private
    /* package */ PersonalizationDictionary(final Context context, final Locale locale) {
        super(context, getDictName(NAME, locale, null /* dictFile */), locale,
                Dictionary.TYPE_PERSONALIZATION, null /* dictFile */);
    }

    @UsedForTesting
    public static PersonalizationDictionary getDictionary(final Context context,
            final Locale locale, final File dictFile, final String dictNamePrefix) {
        return PersonalizationHelper.getPersonalizationDictionary(context, locale);
    }
}

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

package com.android.inputmethod.latin.utils;

import java.util.HashSet;

import io.separ.neural.inputmethod.dictionarypack.DictionarySettingsFragment;
import io.separ.neural.inputmethod.indic.about.AboutPreferences;
import io.separ.neural.inputmethod.indic.settings.AdvancedSettingsFragment;
import io.separ.neural.inputmethod.indic.settings.AppearanceSettingsFragment;
import io.separ.neural.inputmethod.indic.settings.CorrectionSettingsFragment;
import io.separ.neural.inputmethod.indic.settings.CustomInputStyleSettingsFragment;
import io.separ.neural.inputmethod.indic.settings.DebugSettingsFragment;
import io.separ.neural.inputmethod.indic.settings.GestureSettingsFragment;
import io.separ.neural.inputmethod.indic.settings.MultiLingualSettingsFragment;
import io.separ.neural.inputmethod.indic.settings.PreferencesSettingsFragment;
import io.separ.neural.inputmethod.indic.settings.SettingsFragment;
import io.separ.neural.inputmethod.indic.settings.ThemeSettingsFragment;
import io.separ.neural.inputmethod.indic.spellcheck.SpellCheckerSettingsFragment;
import io.separ.neural.inputmethod.indic.userdictionary.UserDictionaryAddWordFragment;
import io.separ.neural.inputmethod.indic.userdictionary.UserDictionaryList;
import io.separ.neural.inputmethod.indic.userdictionary.UserDictionaryLocalePicker;
import io.separ.neural.inputmethod.indic.userdictionary.UserDictionarySettings;

public class FragmentUtils {
    private static final HashSet<String> sLatinImeFragments = new HashSet<>();
    static {
        sLatinImeFragments.add(DictionarySettingsFragment.class.getName());
        sLatinImeFragments.add(AboutPreferences.class.getName());
        sLatinImeFragments.add(PreferencesSettingsFragment.class.getName());
        //sLatinImeFragments.add(AppearanceSettingsFragment.class.getName());
        sLatinImeFragments.add(ThemeSettingsFragment.class.getName());
        //sLatinImeFragments.add(MultiLingualSettingsFragment.class.getName());
        sLatinImeFragments.add(CustomInputStyleSettingsFragment.class.getName());
        //sLatinImeFragments.add(GestureSettingsFragment.class.getName());
        sLatinImeFragments.add(CorrectionSettingsFragment.class.getName());
        sLatinImeFragments.add(AdvancedSettingsFragment.class.getName());
        sLatinImeFragments.add(DebugSettingsFragment.class.getName());
        sLatinImeFragments.add(SettingsFragment.class.getName());
        sLatinImeFragments.add(SpellCheckerSettingsFragment.class.getName());
        sLatinImeFragments.add(UserDictionaryAddWordFragment.class.getName());
        sLatinImeFragments.add(UserDictionaryList.class.getName());
        sLatinImeFragments.add(UserDictionaryLocalePicker.class.getName());
        sLatinImeFragments.add(UserDictionarySettings.class.getName());
    }

    public static boolean isValidFragment(String fragmentName) {
        return sLatinImeFragments.contains(fragmentName);
    }
}

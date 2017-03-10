/*
 * Copyright (C) 2014 The Android Open Source Project
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

package io.separ.neural.inputmethod.indic.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.inputmethod.keyboard.KeyboardTheme;

import io.separ.neural.inputmethod.indic.R;
import io.separ.neural.inputmethod.indic.settings.RadioButtonPreference.OnRadioButtonClickedListener;

/**
 * "Keyboard theme" settings sub screen.
 */
public final class ThemeSettingsFragment extends SubScreenFragment
        implements OnRadioButtonClickedListener {
    private String mSelectedThemeId;

    static class KeyboardThemePreference extends RadioButtonPreference {
        final String mThemeId;

        KeyboardThemePreference(final Context context, final String name, final String id) {
            super(context);
            setTitle(name);
            mThemeId = id;
        }
    }

    static void updateKeyboardThemeSummary(final Preference pref) {
        //final Resources res = pref.getContext().getResources();
        //final SharedPreferences prefs = pref.getSharedPreferences();
        /*final KeyboardTheme keyboardTheme = KeyboardTheme.getKeyboardTheme(prefs);
        final String keyboardThemeId = String.valueOf(keyboardTheme.mThemeId);
        final String[] keyboardThemeNames = res.getStringArray(R.array.keyboard_theme_names);
        final String[] keyboardThemeIds = res.getStringArray(R.array.keyboard_theme_ids);
        for (int index = 0; index < keyboardThemeNames.length; index++) {
            if (keyboardThemeId.equals(keyboardThemeIds[index])) {
                pref.setSummary(keyboardThemeNames[index]);
                return;
            }
        }*/
    }

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_theme);
        final PreferenceScreen screen = getPreferenceScreen();
        final Resources res = getResources();
        final String[] keyboardThemeNames = res.getStringArray(R.array.keyboard_theme_names);
        final String[] keyboardThemeIds = res.getStringArray(R.array.keyboard_theme_ids);
        for (int index = 0; index < keyboardThemeNames.length; index++) {
            final KeyboardThemePreference pref = new KeyboardThemePreference(
                    getActivity(), keyboardThemeNames[index], keyboardThemeIds[index]);
            screen.addPreference(pref);
            pref.setOnRadioButtonClickedListener(this);
        }
        final SharedPreferences prefs = getSharedPreferences();
        //final KeyboardTheme keyboardTheme = getCurrentTheme(prefs);/*KeyboardTheme.getKeyboardTheme(prefs);*/
        //mSelectedThemeId = String.valueOf(keyboardTheme.mThemeId);
        mSelectedThemeId = String.valueOf(prefs.getString("KeyboardTheme", "adaptive_theme"));
    }

    @Override
    public void onRadioButtonClicked(final RadioButtonPreference preference) {
        if (preference instanceof KeyboardThemePreference) {
            final KeyboardThemePreference pref = (KeyboardThemePreference)preference;
            mSelectedThemeId = pref.mThemeId;
            updateSelected();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSelected();
    }

    @Override
    public void onPause() {
        super.onPause();
        getSharedPreferences().edit().putString("KeyboardTheme", mSelectedThemeId).apply();
        //KeyboardTheme.saveKeyboardThemeId(mSelectedThemeId, getSharedPreferences());
    }

    private void updateSelected() {
        final PreferenceScreen screen = getPreferenceScreen();
        final int count = screen.getPreferenceCount();
        for (int index = 0; index < count; index++) {
            final Preference preference = screen.getPreference(index);
            if (preference instanceof KeyboardThemePreference) {
                final KeyboardThemePreference pref = (KeyboardThemePreference)preference;
                final boolean selected = mSelectedThemeId.equals(pref.mThemeId);
                pref.setSelected(selected);
            }
        }
    }
}

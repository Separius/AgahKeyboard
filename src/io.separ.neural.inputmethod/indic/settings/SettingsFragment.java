/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.inputmethod.latin.utils.ApplicationUtils;
import com.android.inputmethodcommon.InputMethodSettingsFragment;

import io.separ.neural.inputmethod.indic.R;

public final class SettingsFragment extends InputMethodSettingsFragment {
    // We don't care about menu grouping.
    private static final int NO_MENU_GROUP = Menu.NONE;
    // The first menu item id and order.
    private static final int MENU_RATE = Menu.FIRST;
    // The second menu item id and order.
    private static final int MENU_SHARE = Menu.FIRST + 1;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        setHasOptionsMenu(true);
        setInputMethodSettingsCategoryTitle(R.string.language_selection_title);
        setSubtypeEnablerTitle(R.string.select_language);
        addPreferencesFromResource(R.xml.prefs);
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.setTitle(
                ApplicationUtils.getActivityTitleResId(getActivity(), SettingsActivity.class));
        /*if (!Settings.SHOW_MULTILINGUAL_SETTINGS) {
            final Preference multilingualOptions = findPreference(Settings.SCREEN_MULTILINGUAL);
            preferenceScreen.removePreference(multilingualOptions);
        }*/
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        //if (FeedbackUtils.isHelpAndFeedbackFormSupported()) {
        menu.add(NO_MENU_GROUP, MENU_SHARE /* itemId */,
                    MENU_SHARE /* order */, R.string.share_app);
        //}
        menu.add(NO_MENU_GROUP, MENU_RATE /* itemId */, MENU_RATE /* order */, R.string.rate_app);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == MENU_SHARE) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Agah Keyboard");
            String sAux = "Let me recommend you this application\n"+"https://cafebazaar.ir/app/io.separ.neural.inputmethod.indic/?l=fa";
            intent.putExtra(Intent.EXTRA_TEXT, sAux);
            startActivity(Intent.createChooser(intent, "Choose one"));
            return true;
        }
        if (itemId == MENU_RATE) {
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setData(Uri.parse("bazaar://details?id="+"io.separ.neural.inputmethod.indic"));
            intent.setPackage("com.farsitel.bazaar");
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

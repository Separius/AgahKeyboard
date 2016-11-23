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

import android.util.Log;

import com.android.inputmethod.keyboard.ProximityInfo;
import com.android.inputmethod.latin.PrevWordsInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

import io.separ.neural.inputmethod.indic.SuggestedWords.SuggestedWordInfo;
import io.separ.neural.inputmethod.indic.settings.SettingsValuesForSuggestion;

/**
 * Class for a collection of dictionaries that behave like one dictionary.
 */
public final class DictionaryCollection extends Dictionary {
    private final String TAG = DictionaryCollection.class.getSimpleName();
    protected final CopyOnWriteArrayList<Dictionary> mDictionaries;

    public DictionaryCollection(final String dictType) {
        super(dictType);
        mDictionaries = new CopyOnWriteArrayList<>();
    }

    public DictionaryCollection(final String dictType, final Dictionary... dictionaries) {
        super(dictType);
        if (null == dictionaries) {
            mDictionaries = new CopyOnWriteArrayList<>();
        } else {
            mDictionaries = new CopyOnWriteArrayList<>(dictionaries);
            mDictionaries.removeAll(Collections.singleton(null));
        }
    }

    public DictionaryCollection(final String dictType, final Collection<Dictionary> dictionaries) {
        super(dictType);
        mDictionaries = new CopyOnWriteArrayList<>(dictionaries);
        mDictionaries.removeAll(Collections.singleton(null));
    }

    @Override
    public ArrayList<SuggestedWordInfo> getSuggestions(final WordComposer composer,
            final PrevWordsInfo prevWordsInfo, final ProximityInfo proximityInfo,
            final SettingsValuesForSuggestion settingsValuesForSuggestion,
            final int sessionId, final float[] inOutLanguageWeight) {
        final CopyOnWriteArrayList<Dictionary> dictionaries = mDictionaries;
        if (dictionaries.isEmpty()) return null;
        // To avoid creating unnecessary objects, we get the list out of the first
        // dictionary and add the rest to it if not null, hence the get(0)
        ArrayList<SuggestedWordInfo> suggestions = dictionaries.get(0).getSuggestions(composer,
                prevWordsInfo, proximityInfo, settingsValuesForSuggestion, sessionId,
                inOutLanguageWeight);
        if (null == suggestions) suggestions = new ArrayList<>();
        final int length = dictionaries.size();
        for (int i = 1; i < length; ++ i) {
            final ArrayList<SuggestedWordInfo> sugg = dictionaries.get(i).getSuggestions(composer,
                    prevWordsInfo, proximityInfo, settingsValuesForSuggestion, sessionId,
                    inOutLanguageWeight);
            if (null != sugg) suggestions.addAll(sugg);
        }
        return suggestions;
    }

    @Override
    public boolean isInDictionary(final String word) {
        for (int i = mDictionaries.size() - 1; i >= 0; --i)
            if (mDictionaries.get(i).isInDictionary(word)) return true;
        return false;
    }

    @Override
    public int getFrequency(final String word) {
        int maxFreq = -1;
        for (int i = mDictionaries.size() - 1; i >= 0; --i) {
            final int tempFreq = mDictionaries.get(i).getFrequency(word);
            maxFreq = Math.max(tempFreq, maxFreq);
        }
        return maxFreq;
    }

    @Override
    public int getMaxFrequencyOfExactMatches(final String word) {
        int maxFreq = -1;
        for (int i = mDictionaries.size() - 1; i >= 0; --i) {
            final int tempFreq = mDictionaries.get(i).getMaxFrequencyOfExactMatches(word);
            maxFreq = Math.max(tempFreq, maxFreq);
        }
        return maxFreq;
    }

    @Override
    public boolean isInitialized() {
        return !mDictionaries.isEmpty();
    }

    @Override
    public void close() {
        for (final Dictionary dict : mDictionaries)
            dict.close();
    }

    // Warning: this is not thread-safe. Take necessary precaution when calling.
    public void addDictionary(final Dictionary newDict) {
        if (null == newDict) return;
        if (mDictionaries.contains(newDict)) {
            Log.w(TAG, "This collection already contains this dictionary: " + newDict);
        }
        mDictionaries.add(newDict);
    }

    // Warning: this is not thread-safe. Take necessary precaution when calling.
    public void removeDictionary(final Dictionary dict) {
        if (mDictionaries.contains(dict)) {
            mDictionaries.remove(dict);
        } else {
            Log.w(TAG, "This collection does not contain this dictionary: " + dict);
        }
    }
}

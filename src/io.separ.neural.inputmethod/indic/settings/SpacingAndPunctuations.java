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

import android.content.res.Resources;

import com.android.inputmethod.keyboard.internal.MoreKeySpec;
import com.android.inputmethod.latin.utils.StringUtils;

import java.util.Arrays;
import java.util.Locale;

import io.separ.neural.inputmethod.annotations.UsedForTesting;
import io.separ.neural.inputmethod.indic.Constants;
import io.separ.neural.inputmethod.indic.PunctuationSuggestions;
import io.separ.neural.inputmethod.indic.R;

public final class SpacingAndPunctuations {
    private final int[] mSortedSymbolsPrecededBySpace;
    private final int[] mSortedSymbolsFollowedBySpace;
    private final int[] mSortedSymbolsClusteringTogether;
    private final int[] mSortedWordConnectors;
    public final int[] mSortedWordSeparators;
    private final int[] mSortedSentenceTerminators;
    public final PunctuationSuggestions mSuggestPuncList;
    private final int mSentenceSeparator;
    public final String mSentenceSeparatorAndSpace;
    public final boolean mCurrentLanguageHasSpaces;
    public final boolean mUsesAmericanTypography;
    public final boolean mUsesGermanRules;

    public SpacingAndPunctuations(final Resources res) {
        // To be able to binary search the code point. See {@link #isUsuallyPrecededBySpace(int)}.
        mSortedSymbolsPrecededBySpace = StringUtils.toSortedCodePointArray(
                res.getString(R.string.symbols_preceded_by_space));
        // To be able to binary search the code point. See {@link #isUsuallyFollowedBySpace(int)}.
        mSortedSymbolsFollowedBySpace = StringUtils.toSortedCodePointArray(
                res.getString(R.string.symbols_followed_by_space));
        mSortedSymbolsClusteringTogether = StringUtils.toSortedCodePointArray(
                res.getString(R.string.symbols_clustering_together));
        // To be able to binary search the code point. See {@link #isWordConnector(int)}.
        mSortedWordConnectors = StringUtils.toSortedCodePointArray(
                res.getString(R.string.symbols_word_connectors));
        mSortedWordSeparators = StringUtils.toSortedCodePointArray(
                res.getString(R.string.symbols_word_separators));
        mSentenceSeparator = res.getInteger(R.integer.sentence_separator);
        mSentenceSeparatorAndSpace = new String(new int[] {
                mSentenceSeparator, Constants.CODE_SPACE }, 0, 2);
        mCurrentLanguageHasSpaces = res.getBoolean(R.bool.current_language_has_spaces);
        final Locale locale = res.getConfiguration().locale;
        // Heuristic: we use American Typography rules because it's the most common rules for all
        // English variants. German rules (not "German typography") also have small gotchas.
        mUsesAmericanTypography = Locale.ENGLISH.getLanguage().equals(locale.getLanguage());
        mUsesGermanRules = Locale.GERMAN.getLanguage().equals(locale.getLanguage());
        final String[] suggestPuncsSpec = MoreKeySpec.splitKeySpecs(
                res.getString(R.string.suggested_punctuations));
        mSuggestPuncList = PunctuationSuggestions.newPunctuationSuggestions(suggestPuncsSpec);
        mSortedSentenceTerminators = StringUtils.toSortedCodePointArray(
                res.getString(R.string.symbols_sentence_terminators));
    }

    @UsedForTesting
    public SpacingAndPunctuations(final SpacingAndPunctuations model,
            final int[] overrideSortedWordSeparators) {
        mSortedSymbolsPrecededBySpace = model.mSortedSymbolsPrecededBySpace;
        mSortedSymbolsFollowedBySpace = model.mSortedSymbolsFollowedBySpace;
        mSortedSymbolsClusteringTogether = model.mSortedSymbolsClusteringTogether;
        mSortedWordConnectors = model.mSortedWordConnectors;
        mSortedWordSeparators = overrideSortedWordSeparators;
        mSuggestPuncList = model.mSuggestPuncList;
        mSentenceSeparator = model.mSentenceSeparator;
        mSentenceSeparatorAndSpace = model.mSentenceSeparatorAndSpace;
        mCurrentLanguageHasSpaces = model.mCurrentLanguageHasSpaces;
        mUsesAmericanTypography = model.mUsesAmericanTypography;
        mUsesGermanRules = model.mUsesGermanRules;
        mSortedSentenceTerminators = model.mSortedSentenceTerminators;
    }

    public boolean isWordSeparator(final int code) {
        return Arrays.binarySearch(mSortedWordSeparators, code) >= 0;
    }

    public boolean isWordConnector(final int code) {
        return Arrays.binarySearch(mSortedWordConnectors, code) >= 0;
    }

    public boolean isWordCodePoint(final int code) {
        return Character.isLetter(code) || isWordConnector(code);
    }

    public boolean isUsuallyPrecededBySpace(final int code) {
        return Arrays.binarySearch(mSortedSymbolsPrecededBySpace, code) >= 0;
    }

    public boolean isUsuallyFollowedBySpace(final int code) {
        return Arrays.binarySearch(mSortedSymbolsFollowedBySpace, code) >= 0;
    }

    public boolean isClusteringSymbol(final int code) {
        return Arrays.binarySearch(mSortedSymbolsClusteringTogether, code) >= 0;
    }

    public boolean isSentenceSeparator(final int code) {
        return code == mSentenceSeparator;
    }

    public String dump() {
        return "mSortedSymbolsPrecededBySpace = " +
                "" + Arrays.toString(mSortedSymbolsPrecededBySpace) +
                "\n   mSortedSymbolsFollowedBySpace = " +
                "" + Arrays.toString(mSortedSymbolsFollowedBySpace) +
                "\n   mSortedWordConnectors = " +
                "" + Arrays.toString(mSortedWordConnectors) +
                "\n   mSortedWordSeparators = " +
                "" + Arrays.toString(mSortedWordSeparators) +
                "\n   mSuggestPuncList = " +
                "" + mSuggestPuncList +
                "\n   mSentenceSeparator = " +
                "" + mSentenceSeparator +
                "\n   mSentenceSeparatorAndSpace = " +
                "" + mSentenceSeparatorAndSpace +
                "\n   mCurrentLanguageHasSpaces = " +
                "" + mCurrentLanguageHasSpaces +
                "\n   mUsesAmericanTypography = " +
                "" + mUsesAmericanTypography +
                "\n   mUsesGermanRules = " +
                "" + mUsesGermanRules;
    }

    public boolean isSentenceTerminator(final int code) {
        return Arrays.binarySearch(mSortedSentenceTerminators, code) >= 0;
    }
}

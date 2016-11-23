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

package com.android.inputmethod.latin.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.TreeSet;

import io.separ.neural.inputmethod.indic.SuggestedWords.SuggestedWordInfo;
import io.separ.neural.inputmethod.indic.define.ProductionFlags;

/**
 * A TreeSet of SuggestedWordInfo that is bounded in size and throws everything that's smaller
 * than its limit
 */
public final class SuggestionResults extends TreeSet<SuggestedWordInfo> {
    public final Locale mLocale;
    public final ArrayList<SuggestedWordInfo> mRawSuggestions;
    // TODO: Instead of a boolean , we may want to include the context of this suggestion results,
    // such as {@link PrevWordsInfo}.
    public final boolean mIsBeginningOfSentence;
    private final int mCapacity;

    public SuggestionResults(final Locale locale, final int capacity,
            final boolean isBeginningOfSentence) {
        this(locale, sSuggestedWordInfoComparator, capacity, isBeginningOfSentence);
    }

    private SuggestionResults(final Locale locale, final Comparator<SuggestedWordInfo> comparator,
            final int capacity, final boolean isBeginningOfSentence) {
        super(comparator);
        mLocale = locale;
        mCapacity = capacity;
        if (ProductionFlags.INCLUDE_RAW_SUGGESTIONS) {
            mRawSuggestions = new ArrayList<>();
        } else {
            mRawSuggestions = null;
        }
        mIsBeginningOfSentence = isBeginningOfSentence;
    }

    @Override
    public boolean add(final SuggestedWordInfo e) {
        if (size() < mCapacity) return super.add(e);
        if (comparator().compare(e, last()) > 0) return false;
        super.add(e);
        pollLast(); // removes the last element
        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends SuggestedWordInfo> e) {
        return null != e && super.addAll(e);
    }

    private static final class SuggestedWordInfoComparator
            implements Comparator<SuggestedWordInfo> {
        // This comparator ranks the word info with the higher frequency first. That's because
        // that's the order we want our elements in.
        @Override
        public int compare(final SuggestedWordInfo o1, final SuggestedWordInfo o2) {
            if (o1.mScore > o2.mScore) return -1;
            if (o1.mScore < o2.mScore) return 1;
            if (o1.mCodePointCount < o2.mCodePointCount) return -1;
            if (o1.mCodePointCount > o2.mCodePointCount) return 1;
            return o1.mWord.compareTo(o2.mWord);
        }
    }

    private static final SuggestedWordInfoComparator sSuggestedWordInfoComparator =
            new SuggestedWordInfoComparator();
}

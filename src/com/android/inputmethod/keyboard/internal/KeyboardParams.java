/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.inputmethod.keyboard.internal;

import android.util.SparseIntArray;

import com.android.inputmethod.keyboard.Key;
import com.android.inputmethod.keyboard.KeyboardId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import io.separ.neural.inputmethod.indic.Constants;

public class KeyboardParams {
    public KeyboardId mId;
    public int mThemeId;

    /** Total height and width of the keyboard, including the paddings and keys */
    public int mOccupiedHeight;
    public int mOccupiedWidth;

    /** Base height and width of the keyboard used to calculate rows' or keys' heights and
     *  widths
     */
    public int mBaseHeight;
    public int mBaseWidth;

    public int mTopPadding;
    public int mBottomPadding;
    public int mLeftPadding;
    public int mRightPadding;

    public KeyVisualAttributes mKeyVisualAttributes;

    public int mDefaultRowHeight;
    public int mDefaultKeyWidth;
    public int mHorizontalGap;
    public int mVerticalGap;

    public int mMoreKeysTemplate;
    public int mMaxMoreKeysKeyboardColumn;

    public int GRID_WIDTH;
    public int GRID_HEIGHT;

    // Keys are sorted from top-left to bottom-right order.
    public final SortedSet<Key> mSortedKeys = new TreeSet<>(ROW_COLUMN_COMPARATOR);
    public final ArrayList<Key> mShiftKeys = new ArrayList<>();
    public final ArrayList<Key> mAltCodeKeysWhileTyping = new ArrayList<>();
    public final KeyboardIconsSet mIconsSet = new KeyboardIconsSet();
    public final KeyboardTextsSet mTextsSet = new KeyboardTextsSet();
    public final KeyStylesSet mKeyStyles = new KeyStylesSet(mTextsSet);

    public KeysCache mKeysCache;

    public int mMostCommonKeyHeight = 0;
    public int mMostCommonKeyWidth = 0;

    public boolean mProximityCharsCorrectionEnabled;

    public final TouchPositionCorrection mTouchPositionCorrection =
            new TouchPositionCorrection();

    // Comparator to sort {@link Key}s from top-left to bottom-right order.
    private static final Comparator<Key> ROW_COLUMN_COMPARATOR = new Comparator<Key>() {
        @Override
        public int compare(final Key lhs, final Key rhs) {
            if (lhs.getY() < rhs.getY()) return -1;
            if (lhs.getY() > rhs.getY()) return 1;
            if (lhs.getX() < rhs.getX()) return -1;
            if (lhs.getX() > rhs.getX()) return 1;
            return 0;
        }
    };

    protected void clearKeys() {
        mSortedKeys.clear();
        mShiftKeys.clear();
        clearHistogram();
    }

    public void onAddKey(final Key newKey) {
        final Key key = (mKeysCache != null) ? mKeysCache.get(newKey) : newKey;
        final boolean isSpacer = key.isSpacer();
        if (isSpacer && key.getWidth() == 0) {
            // Ignore zero width {@link Spacer}.
            return;
        }
        mSortedKeys.add(key);
        if (isSpacer) {
            return;
        }
        updateHistogram(key);
        if (key.getCode() == Constants.CODE_SHIFT) {
            mShiftKeys.add(key);
        }
        if (key.altCodeWhileTyping()) {
            mAltCodeKeysWhileTyping.add(key);
        }
    }

    private int mMaxHeightCount = 0;
    private int mMaxWidthCount = 0;
    private final SparseIntArray mHeightHistogram = new SparseIntArray();
    private final SparseIntArray mWidthHistogram = new SparseIntArray();

    private void clearHistogram() {
        mMostCommonKeyHeight = 0;
        mMaxHeightCount = 0;
        mHeightHistogram.clear();

        mMaxWidthCount = 0;
        mMostCommonKeyWidth = 0;
        mWidthHistogram.clear();
    }

    private static int updateHistogramCounter(final SparseIntArray histogram, final int key) {
        final int index = histogram.indexOfKey(key);
        final int count = (index >= 0 ? histogram.get(key) : 0) + 1;
        histogram.put(key, count);
        return count;
    }

    private void updateHistogram(final Key key) {
        final int height = key.getHeight() + mVerticalGap;
        final int heightCount = updateHistogramCounter(mHeightHistogram, height);
        if (heightCount > mMaxHeightCount) {
            mMaxHeightCount = heightCount;
            mMostCommonKeyHeight = height;
        }

        final int width = key.getWidth() + mHorizontalGap;
        final int widthCount = updateHistogramCounter(mWidthHistogram, width);
        if (widthCount > mMaxWidthCount) {
            mMaxWidthCount = widthCount;
            mMostCommonKeyWidth = width;
        }
    }
}

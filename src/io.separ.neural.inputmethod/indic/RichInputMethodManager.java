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

package io.separ.neural.inputmethod.indic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.android.inputmethod.latin.utils.AdditionalSubtypeUtils;
import com.android.inputmethod.latin.utils.SubtypeLocaleUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.separ.neural.inputmethod.compat.InputMethodManagerCompatWrapper;
import io.separ.neural.inputmethod.indic.settings.Settings;

import static io.separ.neural.inputmethod.indic.Constants.Subtype.KEYBOARD_MODE;

/**
 * Enrichment class for InputMethodManager to simplify interaction and add functionality.
 */
public final class RichInputMethodManager {
    private static final String TAG = RichInputMethodManager.class.getSimpleName();

    private RichInputMethodManager() {
        // This utility class is not publicly instantiable.
    }

    private static final RichInputMethodManager sInstance = new RichInputMethodManager();

    private InputMethodManagerCompatWrapper mImmWrapper;
    private InputMethodInfoCache mInputMethodInfoCache;
    final HashMap<InputMethodInfo, List<InputMethodSubtype>>
            mSubtypeListCacheWithImplicitlySelectedSubtypes = new HashMap<>();
    final HashMap<InputMethodInfo, List<InputMethodSubtype>>
            mSubtypeListCacheWithoutImplicitlySelectedSubtypes = new HashMap<>();

    private static final int INDEX_NOT_FOUND = -1;

    public static RichInputMethodManager getInstance() {
        sInstance.checkInitialized();
        return sInstance;
    }

    public static void init(final Context context) {
        sInstance.initInternal(context);
    }

    private boolean isInitialized() {
        return mImmWrapper != null;
    }

    private void checkInitialized() {
        if (!isInitialized()) {
            throw new RuntimeException(TAG + " is used before initialization");
        }
    }

    private void initInternal(final Context context) {
        if (isInitialized()) {
            return;
        }
        mImmWrapper = new InputMethodManagerCompatWrapper(context);
        mInputMethodInfoCache = new InputMethodInfoCache(
                mImmWrapper.mImm, context.getPackageName());

        // Initialize additional subtypes.
        SubtypeLocaleUtils.init(context);
        final InputMethodSubtype[] additionalSubtypes = getAdditionalSubtypes(context);
        setAdditionalInputMethodSubtypes(additionalSubtypes);
    }

    public static InputMethodSubtype[] getAdditionalSubtypes(final Context context) {
        SubtypeLocaleUtils.init(context);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String prefAdditionalSubtypes = Settings.readPrefAdditionalSubtypes(
                prefs, context.getResources());
        return AdditionalSubtypeUtils.createAdditionalSubtypesArray(prefAdditionalSubtypes);
    }

    public InputMethodManager getInputMethodManager() {
        checkInitialized();
        return mImmWrapper.mImm;
    }

    public List<InputMethodSubtype> getMyEnabledInputMethodSubtypeList(
            boolean allowsImplicitlySelectedSubtypes) {
        return getEnabledInputMethodSubtypeList(
                getInputMethodInfoOfThisIme(), allowsImplicitlySelectedSubtypes);
    }

    public boolean switchToNextInputMethod(final IBinder token, final boolean onlyCurrentIme) {
        if (mImmWrapper.switchToNextInputMethod(token, onlyCurrentIme)) {
            return true;
        }
        // Was not able to call {@link InputMethodManager#switchToNextInputMethodIBinder,boolean)}
        // because the current device is running ICS or previous and lacks the API.
        return switchToNextInputSubtypeInThisIme(token, onlyCurrentIme) || switchToNextInputMethodAndSubtype(token);
    }

    private boolean switchToNextInputSubtypeInThisIme(final IBinder token,
            final boolean onlyCurrentIme) {
        final InputMethodManager imm = mImmWrapper.mImm;
        final InputMethodSubtype currentSubtype = imm.getCurrentInputMethodSubtype();
        final List<InputMethodSubtype> enabledSubtypes = getMyEnabledInputMethodSubtypeList(
                true /* allowsImplicitlySelectedSubtypes */);
        final int currentIndex = getSubtypeIndexInList(currentSubtype, enabledSubtypes);
        if (currentIndex == INDEX_NOT_FOUND) {
            Log.w(TAG, "Can't find current subtype in enabled subtypes: subtype="
                    + SubtypeLocaleUtils.getSubtypeNameForLogging(currentSubtype));
            return false;
        }
        final int nextIndex = (currentIndex + 1) % enabledSubtypes.size();
        if (nextIndex <= currentIndex && !onlyCurrentIme) {
            // The current subtype is the last or only enabled one and it needs to switch to
            // next IME.
            return false;
        }
        final InputMethodSubtype nextSubtype = enabledSubtypes.get(nextIndex);
        setInputMethodAndSubtype(token, nextSubtype);
        return true;
    }

    private boolean switchToNextInputMethodAndSubtype(final IBinder token) {
        final InputMethodManager imm = mImmWrapper.mImm;
        final List<InputMethodInfo> enabledImis = imm.getEnabledInputMethodList();
        final int currentIndex = getImiIndexInList(getInputMethodInfoOfThisIme(), enabledImis);
        if (currentIndex == INDEX_NOT_FOUND) {
            Log.w(TAG, "Can't find current IME in enabled IMEs: IME package="
                    + getInputMethodInfoOfThisIme().getPackageName());
            return false;
        }
        final InputMethodInfo nextImi = getNextNonAuxiliaryIme(currentIndex, enabledImis);
        final List<InputMethodSubtype> enabledSubtypes = getEnabledInputMethodSubtypeList(nextImi,
                true /* allowsImplicitlySelectedSubtypes */);
        if (enabledSubtypes.isEmpty()) {
            // The next IME has no subtype.
            imm.setInputMethod(token, nextImi.getId());
            return true;
        }
        final InputMethodSubtype firstSubtype = enabledSubtypes.get(0);
        imm.setInputMethodAndSubtype(token, nextImi.getId(), firstSubtype);
        return true;
    }

    private static int getImiIndexInList(final InputMethodInfo inputMethodInfo,
            final List<InputMethodInfo> imiList) {
        final int count = imiList.size();
        for (int index = 0; index < count; index++) {
            final InputMethodInfo imi = imiList.get(index);
            if (imi.equals(inputMethodInfo)) {
                return index;
            }
        }
        return INDEX_NOT_FOUND;
    }

    // This method mimics {@link InputMethodManager#switchToNextInputMethod(IBinder,boolean)}.
    private static InputMethodInfo getNextNonAuxiliaryIme(final int currentIndex,
            final List<InputMethodInfo> imiList) {
        final int count = imiList.size();
        for (int i = 1; i < count; i++) {
            final int nextIndex = (currentIndex + i) % count;
            final InputMethodInfo nextImi = imiList.get(nextIndex);
            if (!isAuxiliaryIme(nextImi)) {
                return nextImi;
            }
        }
        return imiList.get(currentIndex);
    }

    // Copied from {@link InputMethodInfo}. See how auxiliary of IME is determined.
    private static boolean isAuxiliaryIme(final InputMethodInfo imi) {
        final int count = imi.getSubtypeCount();
        if (count == 0) {
            return false;
        }
        for (int index = 0; index < count; index++) {
            final InputMethodSubtype subtype = imi.getSubtypeAt(index);
            if (!subtype.isAuxiliary()) {
                return false;
            }
        }
        return true;
    }

    private static class InputMethodInfoCache {
        private final InputMethodManager mImm;
        private final String mImePackageName;

        private InputMethodInfo mCachedValue;

        public InputMethodInfoCache(final InputMethodManager imm, final String imePackageName) {
            mImm = imm;
            mImePackageName = imePackageName;
        }

        public synchronized InputMethodInfo get() {
            if (mCachedValue != null) {
                return mCachedValue;
            }
            for (final InputMethodInfo imi : mImm.getInputMethodList()) {
                if (imi.getPackageName().equals(mImePackageName)) {
                    mCachedValue = imi;
                    return imi;
                }
            }
            throw new RuntimeException("Input method id for " + mImePackageName + " not found.");
        }

        public synchronized void clear() {
            mCachedValue = null;
        }
    }

    public InputMethodInfo getInputMethodInfoOfThisIme() {
        return mInputMethodInfoCache.get();
    }

    public String getInputMethodIdOfThisIme() {
        return getInputMethodInfoOfThisIme().getId();
    }

    public boolean checkIfSubtypeBelongsToThisImeAndEnabled(final InputMethodSubtype subtype) {
        return checkIfSubtypeBelongsToImeAndEnabled(getInputMethodInfoOfThisIme(), subtype);
    }

    public boolean checkIfSubtypeBelongsToThisImeAndImplicitlyEnabled(
            final InputMethodSubtype subtype) {
        final boolean subtypeEnabled = checkIfSubtypeBelongsToThisImeAndEnabled(subtype);
        final boolean subtypeExplicitlyEnabled = checkIfSubtypeBelongsToList(
                subtype, getMyEnabledInputMethodSubtypeList(
                        false /* allowsImplicitlySelectedSubtypes */));
        return subtypeEnabled && !subtypeExplicitlyEnabled;
    }

    public boolean checkIfSubtypeBelongsToImeAndEnabled(final InputMethodInfo imi,
            final InputMethodSubtype subtype) {
        return checkIfSubtypeBelongsToList(subtype, getEnabledInputMethodSubtypeList(imi,
                true /* allowsImplicitlySelectedSubtypes */));
    }

    private static boolean checkIfSubtypeBelongsToList(final InputMethodSubtype subtype,
            final List<InputMethodSubtype> subtypes) {
        return getSubtypeIndexInList(subtype, subtypes) != INDEX_NOT_FOUND;
    }

    private static int getSubtypeIndexInList(final InputMethodSubtype subtype,
            final List<InputMethodSubtype> subtypes) {
        final int count = subtypes.size();
        for (int index = 0; index < count; index++) {
            final InputMethodSubtype ims = subtypes.get(index);
            if (ims.equals(subtype)) {
                return index;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public boolean checkIfSubtypeBelongsToThisIme(final InputMethodSubtype subtype) {
        return getSubtypeIndexInIme(subtype, getInputMethodInfoOfThisIme()) != INDEX_NOT_FOUND;
    }

    private static int getSubtypeIndexInIme(final InputMethodSubtype subtype,
            final InputMethodInfo imi) {
        final int count = imi.getSubtypeCount();
        for (int index = 0; index < count; index++) {
            final InputMethodSubtype ims = imi.getSubtypeAt(index);
            if (ims.equals(subtype)) {
                return index;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public InputMethodSubtype getCurrentInputMethodSubtype(
            final InputMethodSubtype defaultSubtype) {
        final InputMethodSubtype currentSubtype = mImmWrapper.mImm.getCurrentInputMethodSubtype();
        return (currentSubtype != null) ? currentSubtype : defaultSubtype;
    }

    public boolean hasMultipleEnabledIMEsOrSubtypes(final boolean shouldIncludeAuxiliarySubtypes) {
        final List<InputMethodInfo> enabledImis = mImmWrapper.mImm.getEnabledInputMethodList();
        return hasMultipleEnabledSubtypes(shouldIncludeAuxiliarySubtypes, enabledImis);
    }

    public boolean hasMultipleEnabledSubtypesInThisIme(
            final boolean shouldIncludeAuxiliarySubtypes) {
        final List<InputMethodInfo> imiList = Collections.singletonList(
                getInputMethodInfoOfThisIme());
        return hasMultipleEnabledSubtypes(shouldIncludeAuxiliarySubtypes, imiList);
    }

    private boolean hasMultipleEnabledSubtypes(final boolean shouldIncludeAuxiliarySubtypes,
            final List<InputMethodInfo> imiList) {
        // Number of the filtered IMEs
        int filteredImisCount = 0;

        for (InputMethodInfo imi : imiList) {
            // We can return true immediately after we find two or more filtered IMEs.
            if (filteredImisCount > 1) return true;
            final List<InputMethodSubtype> subtypes = getEnabledInputMethodSubtypeList(imi, true);
            // IMEs that have no subtypes should be counted.
            if (subtypes.isEmpty()) {
                ++filteredImisCount;
                continue;
            }

            int auxCount = 0;
            for (InputMethodSubtype subtype : subtypes) {
                if (subtype.isAuxiliary()) {
                    ++auxCount;
                }
            }
            final int nonAuxCount = subtypes.size() - auxCount;

            // IMEs that have one or more non-auxiliary subtypes should be counted.
            // If shouldIncludeAuxiliarySubtypes is true, IMEs that have two or more auxiliary
            // subtypes should be counted as well.
            if (nonAuxCount > 0 || (shouldIncludeAuxiliarySubtypes && auxCount > 1)) {
                ++filteredImisCount;
            }
        }

        if (filteredImisCount > 1) {
            return true;
        }
        final List<InputMethodSubtype> subtypes = getMyEnabledInputMethodSubtypeList(true);
        int keyboardCount = 0;
        // imm.getEnabledInputMethodSubtypeList(null, true) will return the current IME's
        // both explicitly and implicitly enabled input method subtype.
        // (The current IME should be LatinIME.)
        for (InputMethodSubtype subtype : subtypes) {
            if (KEYBOARD_MODE.equals(subtype.getMode())) {
                ++keyboardCount;
            }
        }
        return keyboardCount > 1;
    }

    public InputMethodSubtype findSubtypeByLocaleAndKeyboardLayoutSet(final String localeString,
            final String keyboardLayoutSetName) {
        final InputMethodInfo myImi = getInputMethodInfoOfThisIme();
        final int count = myImi.getSubtypeCount();
        for (int i = 0; i < count; i++) {
            final InputMethodSubtype subtype = myImi.getSubtypeAt(i);
            final String layoutName = SubtypeLocaleUtils.getKeyboardLayoutSetName(subtype);
            if (localeString.equals(subtype.getLocale())
                    && keyboardLayoutSetName.equals(layoutName)) {
                return subtype;
            }
        }
        return null;
    }

    public void setInputMethodAndSubtype(final IBinder token, final InputMethodSubtype subtype) {
        mImmWrapper.mImm.setInputMethodAndSubtype(
                token, getInputMethodIdOfThisIme(), subtype);
    }

    public void setAdditionalInputMethodSubtypes(final InputMethodSubtype[] subtypes) {
        mImmWrapper.mImm.setAdditionalInputMethodSubtypes(
                getInputMethodIdOfThisIme(), subtypes);
        // Clear the cache so that we go read the {@link InputMethodInfo} of this IME and list of
        // subtypes again next time.
        clearSubtypeCaches();
    }

    private List<InputMethodSubtype> getEnabledInputMethodSubtypeList(final InputMethodInfo imi,
            final boolean allowsImplicitlySelectedSubtypes) {
        final HashMap<InputMethodInfo, List<InputMethodSubtype>> cache =
                allowsImplicitlySelectedSubtypes
                ? mSubtypeListCacheWithImplicitlySelectedSubtypes
                : mSubtypeListCacheWithoutImplicitlySelectedSubtypes;
        final List<InputMethodSubtype> cachedList = cache.get(imi);
        if (null != cachedList) return cachedList;
        final List<InputMethodSubtype> result = mImmWrapper.mImm.getEnabledInputMethodSubtypeList(
                imi, allowsImplicitlySelectedSubtypes);
        cache.put(imi, result);
        return result;
    }

    public void clearSubtypeCaches() {
        mSubtypeListCacheWithImplicitlySelectedSubtypes.clear();
        mSubtypeListCacheWithoutImplicitlySelectedSubtypes.clear();
        mInputMethodInfoCache.clear();
    }

    public boolean shouldOfferSwitchingToNextInputMethod(final IBinder binder,
            boolean defaultValue) {
        // Use the default value instead on Jelly Bean MR2 and previous where
        // {@link InputMethodManager#shouldOfferSwitchingToNextInputMethod} isn't yet available
        // and on KitKat where the API is still just a stub to return true always.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return defaultValue;
        }
        return mImmWrapper.shouldOfferSwitchingToNextInputMethod(binder);
    }
}

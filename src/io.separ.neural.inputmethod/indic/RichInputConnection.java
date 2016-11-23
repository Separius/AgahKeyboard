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

import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import com.android.inputmethod.latin.PrevWordsInfo;
import com.android.inputmethod.latin.utils.CapsModeUtils;
import com.android.inputmethod.latin.utils.DebugLogUtils;
import com.android.inputmethod.latin.utils.PrevWordsInfoUtils;
import com.android.inputmethod.latin.utils.ScriptUtils;
import com.android.inputmethod.latin.utils.SpannableStringUtils;
import com.android.inputmethod.latin.utils.StringUtils;
import com.android.inputmethod.latin.utils.TextRange;

import java.util.Arrays;

import io.separ.neural.inputmethod.compat.InputConnectionCompatUtils;
import io.separ.neural.inputmethod.indic.settings.SpacingAndPunctuations;

/**
 * Enrichment class for InputConnection to simplify interaction and add functionality.
 *
 * This class serves as a wrapper to be able to simply add hooks to any calls to the underlying
 * InputConnection. It also keeps track of a number of things to avoid having to call upon IPC
 * all the time to find out what text is in the buffer, when we need it to determine caps mode
 * for example.
 */
public final class RichInputConnection {
    private static final String TAG = RichInputConnection.class.getSimpleName();
    private static final boolean DBG = false;
    private static final boolean DEBUG_PREVIOUS_TEXT = false;
    private static final boolean DEBUG_BATCH_NESTING = false;
    // Provision for long words and separators between the words.
    private static final int LOOKBACK_CHARACTER_NUM = Constants.DICTIONARY_MAX_WORD_LENGTH
            * (Constants.MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1) /* words */
            + Constants.MAX_PREV_WORD_COUNT_FOR_N_GRAM /* separators */;
    private static final int INVALID_CURSOR_POSITION = -1;

    /**
     * This variable contains an expected value for the selection start position. This is where the
     * cursor or selection start may end up after all the keyboard-triggered updates have passed. We
     * keep this to compare it to the actual selection start to guess whether the move was caused by
     * a keyboard command or not.
     * It's not really the selection start position: the selection start may not be there yet, and
     * in some cases, it may never arrive there.
     */
    private int mExpectedSelStart = INVALID_CURSOR_POSITION; // in chars, not code points
    /**
     * The expected selection end.  Only differs from mExpectedSelStart if a non-empty selection is
     * expected.  The same caveats as mExpectedSelStart apply.
     */
    private int mExpectedSelEnd = INVALID_CURSOR_POSITION; // in chars, not code points
    /**
     * This contains the committed text immediately preceding the cursor and the composing
     * text if any. It is refreshed when the cursor moves by calling upon the TextView.
     */
    private final StringBuilder mCommittedTextBeforeComposingText = new StringBuilder();
    /**
     * This contains the currently composing text, as LatinIME thinks the TextView is seeing it.
     */
    private final StringBuilder mComposingText = new StringBuilder();

    /**
     * This variable is a temporary object used in
     * {@link #commitTextWithBackgroundColor(CharSequence, int, int)} to avoid object creation.
     */
    private SpannableStringBuilder mTempObjectForCommitText = new SpannableStringBuilder();
    /**
     * This variable is used to track whether the last committed text had the background color or
     * not.
     * TODO: Omit this flag if possible.
     */
    private boolean mLastCommittedTextHasBackgroundColor = false;

    private final InputMethodService mParent;

    InputConnection mIC;
    int mNestLevel;
    public RichInputConnection(final InputMethodService parent) {
        mParent = parent;
        mIC = null;
        mNestLevel = 0;
    }

    private void checkConsistencyForDebug() {
        final ExtractedTextRequest r = new ExtractedTextRequest();
        r.hintMaxChars = 0;
        r.hintMaxLines = 0;
        r.token = 1;
        r.flags = 0;
        final ExtractedText et = mIC.getExtractedText(r, 0);
        final CharSequence beforeCursor = getTextBeforeCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE,
                0);
        final StringBuilder internal = new StringBuilder(mCommittedTextBeforeComposingText)
                .append(mComposingText);
        if (null == et || null == beforeCursor) return;
        final int actualLength = Math.min(beforeCursor.length(), internal.length());
        if (internal.length() > actualLength) {
            internal.delete(0, internal.length() - actualLength);
        }
        final String reference = (beforeCursor.length() <= actualLength) ? beforeCursor.toString()
                : beforeCursor.subSequence(beforeCursor.length() - actualLength,
                        beforeCursor.length()).toString();
        if (et.selectionStart != mExpectedSelStart
                || !(reference.equals(internal.toString()))) {
            final String context = "Expected selection start = " + mExpectedSelStart
                    + "\nActual selection start = " + et.selectionStart
                    + "\nExpected text = " + internal.length() + " " + internal
                    + "\nActual text = " + reference.length() + " " + reference;
            ((LatinIME)mParent).debugDumpStateAndCrashWithException(context);
        } else {
            Log.e(TAG, DebugLogUtils.getStackTrace(2));
            Log.e(TAG, "Exp <> Actual : " + mExpectedSelStart + " <> " + et.selectionStart);
        }
    }

    public void beginBatchEdit() {
        if (++mNestLevel == 1) {
            mIC = mParent.getCurrentInputConnection();
            if (null != mIC) {
                mIC.beginBatchEdit();
            }
        } else {
            if (DBG) {
                throw new RuntimeException("Nest level too deep");
            } else {
                Log.e(TAG, "Nest level too deep : " + mNestLevel);
            }
        }
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
    }

    public void endBatchEdit() {
        if (mNestLevel <= 0) Log.e(TAG, "Batch edit not in progress!"); // TODO: exception instead
        if (--mNestLevel == 0 && null != mIC) {
            mIC.endBatchEdit();
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
    }

    /**
     * Reset the cached text and retrieve it again from the editor.
     *
     * This should be called when the cursor moved. It's possible that we can't connect to
     * the application when doing this; notably, this happens sometimes during rotation, probably
     * because of a race condition in the framework. In this case, we just can't retrieve the
     * data, so we empty the cache and note that we don't know the new cursor position, and we
     * return false so that the caller knows about this and can retry later.
     *
     * @param newSelStart the new position of the selection start, as received from the system.
     * @param newSelEnd the new position of the selection end, as received from the system.
     * @param shouldFinishComposition whether we should finish the composition in progress.
     * @return true if we were able to connect to the editor successfully, false otherwise. When
     *   this method returns false, the caches could not be correctly refreshed so they were only
     *   reset: the caller should try again later to return to normal operation.
     */
    public boolean resetCachesUponCursorMoveAndReturnSuccess(final int newSelStart,
            final int newSelEnd, final boolean shouldFinishComposition) {
        mExpectedSelStart = newSelStart;
        mExpectedSelEnd = newSelEnd;
        mComposingText.setLength(0);
        final boolean didReloadTextSuccessfully = reloadTextCache();
        if (!didReloadTextSuccessfully) {
            Log.d(TAG, "Will try to retrieve text later.");
            return false;
        }
        if (null != mIC && shouldFinishComposition) {
            mIC.finishComposingText();
        }
        return true;
    }

    /**
     * Reload the cached text from the InputConnection.
     *
     * @return true if successful
     */
    private boolean reloadTextCache() {
        mCommittedTextBeforeComposingText.setLength(0);
        mIC = mParent.getCurrentInputConnection();
        // Call upon the inputconnection directly since our own method is using the cache, and
        // we want to refresh it.
        final CharSequence textBeforeCursor = null == mIC ? null :
                mIC.getTextBeforeCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
        if (null == textBeforeCursor) {
            // For some reason the app thinks we are not connected to it. This looks like a
            // framework bug... Fall back to ground state and return false.
            mExpectedSelStart = INVALID_CURSOR_POSITION;
            mExpectedSelEnd = INVALID_CURSOR_POSITION;
            Log.e(TAG, "Unable to connect to the editor to retrieve text.");
            return false;
        }
        mCommittedTextBeforeComposingText.append(textBeforeCursor);
        return true;
    }

    private void checkBatchEdit() {
        if (mNestLevel != 1) {
            // TODO: exception instead
            Log.e(TAG, "Batch edit level incorrect : " + mNestLevel);
            Log.e(TAG, DebugLogUtils.getStackTrace(4));
        }
    }

    public void finishComposingText() {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        // TODO: this is not correct! The cursor is not necessarily after the composing text.
        // In the practice right now this is only called when input ends so it will be reset so
        // it works, but it's wrong and should be fixed.
        mCommittedTextBeforeComposingText.append(mComposingText);
        mComposingText.setLength(0);
        // TODO: Clear this flag in setComposingRegion() and setComposingText() as well if needed.
        mLastCommittedTextHasBackgroundColor = false;
        if (null != mIC) {
            mIC.finishComposingText();
        }
    }

    static int firstDivergence(String str1, String str2) {
        int length = str1.length() > str2.length() ? str2.length() : str1.length();
        for(int i = 0; i < length; i++) {
            if(str1.charAt(i) != str2.charAt(i)) {
                return i;
            }
        }
        return length - 1; // Default
    }

    /**
     * Synonym of {@code commitTextWithBackgroundColor(text, newCursorPosition, Color.TRANSPARENT}.
     * @param text The text to commit. This may include styles.
     * See {@link InputConnection#commitText(CharSequence, int)}.
     * @param newCursorPosition The new cursor position around the text.
     * See {@link InputConnection#commitText(CharSequence, int)}.
     */
    public void commitText(final CharSequence text, final int newCursorPosition) {
        commitTextWithBackgroundColor(text, newCursorPosition, Color.TRANSPARENT, text.length());
    }

    /**
     * Calls {@link InputConnection#commitText(CharSequence, int)} with the given background color.
     * @param text The text to commit. This may include styles.
     * See {@link InputConnection#commitText(CharSequence, int)}.
     * @param newCursorPosition The new cursor position around the text.
     * See {@link InputConnection#commitText(CharSequence, int)}.
     * @param color The background color to be attached. Set {@link Color#TRANSPARENT} to disable
     * the background color. Note that this method specifies {@link BackgroundColorSpan} with
     * {@link Spanned#SPAN_COMPOSING} flag, meaning that the background color persists until
     * {@link #finishComposingText()} is called.
     * @param coloredTextLength the length of text, in Java chars, which should be rendered with
     * the given background color.
     */
    public void commitTextWithBackgroundColor(final CharSequence text, final int newCursorPosition,
            final int color, final int coloredTextLength) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        mCommittedTextBeforeComposingText.append(text);
        // TODO: the following is exceedingly error-prone. Right now when the cursor is in the
        // middle of the composing word mComposingText only holds the part of the composing text
        // that is before the cursor, so this actually works, but it's terribly confusing. Fix this.
        mExpectedSelStart += text.length() - mComposingText.length();
        mExpectedSelEnd = mExpectedSelStart;
        mComposingText.setLength(0);
        mLastCommittedTextHasBackgroundColor = false;
        if (null != mIC) {
            if (color == Color.TRANSPARENT) {
                mIC.commitText(text, newCursorPosition);
            } else {
                mTempObjectForCommitText.clear();
                mTempObjectForCommitText.append(text);
                final BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(color);
                final int spanLength = Math.min(coloredTextLength, text.length());
                mTempObjectForCommitText.setSpan(backgroundColorSpan, 0, spanLength,
                        Spanned.SPAN_COMPOSING | Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                mIC.commitText(mTempObjectForCommitText, newCursorPosition);
                mLastCommittedTextHasBackgroundColor = true;
            }
        }
    }

    /**
     * Removes the background color from the highlighted text if necessary. Should be called while
     * there is no on-going composing text.
     *
     * <p>CAVEAT: This method internally calls {@link InputConnection#finishComposingText()}.
     * Be careful of any unexpected side effects.</p>
     */
    public void removeBackgroundColorFromHighlightedTextIfNecessary() {
        // TODO: We haven't yet full tested if we really need to check this flag or not. Omit this
        // flag if everything works fine without this condition.
        if (!mLastCommittedTextHasBackgroundColor) {
            return;
        }
        if (mComposingText.length() > 0) {
            Log.e(TAG, "clearSpansWithComposingFlags should be called when composing text is " +
                    "empty. mComposingText=" + mComposingText);
            return;
        }
        finishComposingText();
    }

    public CharSequence getSelectedText(final int flags) {
        return (null == mIC) ? null : mIC.getSelectedText(flags);
    }

    public boolean canDeleteCharacters() {
        return mExpectedSelStart > 0;
    }

    /**
     * Gets the caps modes we should be in after this specific string.
     *
     * This returns a bit set of TextUtils#CAP_MODE_*, masked by the inputType argument.
     * This method also supports faking an additional space after the string passed in argument,
     * to support cases where a space will be added automatically, like in phantom space
     * state for example.
     * Note that for English, we are using American typography rules (which are not specific to
     * American English, it's just the most common set of rules for English).
     *
     * @param inputType a mask of the caps modes to test for.
     * @param spacingAndPunctuations the values of the settings to use for locale and separators.
     * @param hasSpaceBefore if we should consider there should be a space after the string.
     * @return the caps modes that should be on as a set of bits
     */
    public int getCursorCapsMode(final int inputType,
            final SpacingAndPunctuations spacingAndPunctuations, final boolean hasSpaceBefore) {
        mIC = mParent.getCurrentInputConnection();
        if (null == mIC) return Constants.TextUtils.CAP_MODE_OFF;
        if (!TextUtils.isEmpty(mComposingText)) {
            if (hasSpaceBefore) {
                // If we have some composing text and a space before, then we should have
                // MODE_CHARACTERS and MODE_WORDS on.
                return (TextUtils.CAP_MODE_CHARACTERS | TextUtils.CAP_MODE_WORDS) & inputType;
            } else {
                // We have some composing text - we should be in MODE_CHARACTERS only.
                return TextUtils.CAP_MODE_CHARACTERS & inputType;
            }
        }
        // TODO: this will generally work, but there may be cases where the buffer contains SOME
        // information but not enough to determine the caps mode accurately. This may happen after
        // heavy pressing of delete, for example DEFAULT_TEXT_CACHE_SIZE - 5 times or so.
        // getCapsMode should be updated to be able to return a "not enough info" result so that
        // we can get more context only when needed.
        if (TextUtils.isEmpty(mCommittedTextBeforeComposingText) && 0 != mExpectedSelStart) {
            if (!reloadTextCache()) {
                Log.w(TAG, "Unable to connect to the editor. "
                        + "Setting caps mode without knowing text.");
            }
        }
        // This never calls InputConnection#getCapsMode - in fact, it's a static method that
        // never blocks or initiates IPC.
        return CapsModeUtils.getCapsMode(mCommittedTextBeforeComposingText, inputType,
                spacingAndPunctuations, hasSpaceBefore);
    }

    public int getCodePointBeforeCursor() {
        final int length = mCommittedTextBeforeComposingText.length();
        if (length < 1) return Constants.NOT_A_CODE;
        return Character.codePointBefore(mCommittedTextBeforeComposingText, length);
    }

    public CharSequence getTextBeforeCursor(final int n, final int flags) {
        final int cachedLength =
                mCommittedTextBeforeComposingText.length() + mComposingText.length();
        // If we have enough characters to satisfy the request, or if we have all characters in
        // the text field, then we can return the cached version right away.
        // However, if we don't have an expected cursor position, then we should always
        // go fetch the cache again (as it happens, INVALID_CURSOR_POSITION < 0, so we need to
        // test for this explicitly)
        if (INVALID_CURSOR_POSITION != mExpectedSelStart
                && (cachedLength >= n || cachedLength >= mExpectedSelStart)) {
            final StringBuilder s = new StringBuilder(mCommittedTextBeforeComposingText);
            // We call #toString() here to create a temporary object.
            // In some situations, this method is called on a worker thread, and it's possible
            // the main thread touches the contents of mComposingText while this worker thread
            // is suspended, because mComposingText is a StringBuilder. This may lead to crashes,
            // so we call #toString() on it. That will result in the return value being strictly
            // speaking wrong, but since this is used for basing bigram probability off, and
            // it's only going to matter for one getSuggestions call, it's fine in the practice.
            s.append(mComposingText.toString());
            if (s.length() > n) {
                s.delete(0, s.length() - n);
            }
            return s;
        }
        mIC = mParent.getCurrentInputConnection();
        return (null == mIC) ? null : mIC.getTextBeforeCursor(n, flags);
    }

    public CharSequence getTextAfterCursor(final int n, final int flags) {
        mIC = mParent.getCurrentInputConnection();
        return (null == mIC) ? null : mIC.getTextAfterCursor(n, flags);
    }

    public void deleteSurroundingText(final int beforeLength, final int afterLength) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        // TODO: the following is incorrect if the cursor is not immediately after the composition.
        // Right now we never come here in this case because we reset the composing state before we
        // come here in this case, but we need to fix this.
        final int remainingChars = mComposingText.length() - beforeLength;
        if (remainingChars >= 0) {
            mComposingText.setLength(remainingChars);
        } else {
            mComposingText.setLength(0);
            // Never cut under 0
            final int len = Math.max(mCommittedTextBeforeComposingText.length()
                    + remainingChars, 0);
            mCommittedTextBeforeComposingText.setLength(len);
        }
        if (mExpectedSelStart > beforeLength) {
            mExpectedSelStart -= beforeLength;
            mExpectedSelEnd -= beforeLength;
        } else {
            // There are fewer characters before the cursor in the buffer than we are being asked to
            // delete. Only delete what is there, and update the end with the amount deleted.
            mExpectedSelEnd -= mExpectedSelStart;
            mExpectedSelStart = 0;
        }
        if (null != mIC) {
            mIC.deleteSurroundingText(beforeLength, afterLength);
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
    }

    public void performEditorAction(final int actionId) {
        mIC = mParent.getCurrentInputConnection();
        if (null != mIC) {
            mIC.performEditorAction(actionId);
        }
    }

    public void sendKeyEvent(final KeyEvent keyEvent) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
            // This method is only called for enter or backspace when speaking to old applications
            // (target SDK <= 15 (Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)), or for digits.
            // When talking to new applications we never use this method because it's inherently
            // racy and has unpredictable results, but for backward compatibility we continue
            // sending the key events for only Enter and Backspace because some applications
            // mistakenly catch them to do some stuff.
            switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_ENTER:
                mCommittedTextBeforeComposingText.append("\n");
                mExpectedSelStart += 1;
                mExpectedSelEnd = mExpectedSelStart;
                break;
            case KeyEvent.KEYCODE_DEL:
                if (0 == mComposingText.length()) {
                    if (mCommittedTextBeforeComposingText.length() > 0) {
                        mCommittedTextBeforeComposingText.delete(
                                mCommittedTextBeforeComposingText.length() - 1,
                                mCommittedTextBeforeComposingText.length());
                    }
                } else {
                    mComposingText.delete(mComposingText.length() - 1, mComposingText.length());
                }
                if (mExpectedSelStart > 0 && mExpectedSelStart == mExpectedSelEnd) {
                    // TODO: Handle surrogate pairs.
                    mExpectedSelStart -= 1;
                }
                mExpectedSelEnd = mExpectedSelStart;
                break;
            case KeyEvent.KEYCODE_UNKNOWN:
                if (null != keyEvent.getCharacters()) {
                    mCommittedTextBeforeComposingText.append(keyEvent.getCharacters());
                    mExpectedSelStart += keyEvent.getCharacters().length();
                    mExpectedSelEnd = mExpectedSelStart;
                }
                break;
            default:
                final String text = StringUtils.newSingleCodePointString(keyEvent.getUnicodeChar());
                mCommittedTextBeforeComposingText.append(text);
                mExpectedSelStart += text.length();
                mExpectedSelEnd = mExpectedSelStart;
                break;
            }
        }
        if (null != mIC) {
            mIC.sendKeyEvent(keyEvent);
        }
    }

    public void setComposingRegion(final int start, final int end) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        final CharSequence textBeforeCursor =
                getTextBeforeCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE + (end - start), 0);
        mCommittedTextBeforeComposingText.setLength(0);
        if (!TextUtils.isEmpty(textBeforeCursor)) {
            // The cursor is not necessarily at the end of the composing text, but we have its
            // position in mExpectedSelStart and mExpectedSelEnd. In this case we want the start
            // of the text, so we should use mExpectedSelStart. In other words, the composing
            // text starts (mExpectedSelStart - start) characters before the end of textBeforeCursor
            final int indexOfStartOfComposingText =
                    Math.max(textBeforeCursor.length() - (mExpectedSelStart - start), 0);
            mComposingText.append(textBeforeCursor.subSequence(indexOfStartOfComposingText,
                    textBeforeCursor.length()));
            mCommittedTextBeforeComposingText.append(
                    textBeforeCursor.subSequence(0, indexOfStartOfComposingText));
        }
        if (null != mIC) {
            mIC.setComposingRegion(start, end);
        }
    }

    public void setComposingText(final CharSequence text, final int newCursorPosition) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        mExpectedSelStart += text.length() - mComposingText.length();
        mExpectedSelEnd = mExpectedSelStart;
        mComposingText.setLength(0);
        mComposingText.append(text);
        // TODO: support values of newCursorPosition != 1. At this time, this is never called with
        // newCursorPosition != 1.
        if (null != mIC) {
            mIC.setComposingText(text, newCursorPosition);
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
    }

    /**
     * Set the selection of the text editor.
     *
     * Calls through to {@link InputConnection#setSelection(int, int)}.
     *
     * @param start the character index where the selection should start.
     * @param end the character index where the selection should end.
     * @return Returns true on success, false on failure: either the input connection is no longer
     * valid when setting the selection or when retrieving the text cache at that point, or
     * invalid arguments were passed.
     */
    public boolean setSelection(final int start, final int end) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        if (start < 0 || end < 0) {
            return false;
        }
        mExpectedSelStart = start;
        mExpectedSelEnd = end;
        if (null != mIC) {
            final boolean isIcValid = mIC.setSelection(start, end);
            if (!isIcValid) {
                return false;
            }
        }
        return reloadTextCache();
    }

    public void commitCorrection(final CorrectionInfo correctionInfo) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        // This has no effect on the text field and does not change its content. It only makes
        // TextView flash the text for a second based on indices contained in the argument.
        if (null != mIC) {
            mIC.commitCorrection(correctionInfo);
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
    }

    public void commitCompletion(final CompletionInfo completionInfo) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        CharSequence text = completionInfo.getText();
        // text should never be null, but just in case, it's better to insert nothing than to crash
        if (null == text) text = "";
        mCommittedTextBeforeComposingText.append(text);
        mExpectedSelStart += text.length() - mComposingText.length();
        mExpectedSelEnd = mExpectedSelStart;
        mComposingText.setLength(0);
        if (null != mIC) {
            mIC.commitCompletion(completionInfo);
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
    }

    @SuppressWarnings("unused")
    public PrevWordsInfo getPrevWordsInfoFromNthPreviousWord(
            final SpacingAndPunctuations spacingAndPunctuations, final int n) {
        mIC = mParent.getCurrentInputConnection();
        if (null == mIC) {
            return PrevWordsInfo.EMPTY_PREV_WORDS_INFO;
        }
        final CharSequence prev = getTextBeforeCursor(LOOKBACK_CHARACTER_NUM, 0);
        if (DEBUG_PREVIOUS_TEXT && null != prev) {
            final int checkLength = LOOKBACK_CHARACTER_NUM - 1;
            final String reference = prev.length() <= checkLength ? prev.toString()
                    : prev.subSequence(prev.length() - checkLength, prev.length()).toString();
            // TODO: right now the following works because mComposingText holds the part of the
            // composing text that is before the cursor, but this is very confusing. We should
            // fix it.
            final StringBuilder internal = new StringBuilder()
                    .append(mCommittedTextBeforeComposingText).append(mComposingText);
            if (internal.length() > checkLength) {
                internal.delete(0, internal.length() - checkLength);
                if (!(reference.equals(internal.toString()))) {
                    final String context =
                            "Expected text = " + internal + "\nActual text = " + reference;
                    ((LatinIME)mParent).debugDumpStateAndCrashWithException(context);
                }
            }
        }
        return PrevWordsInfoUtils.getPrevWordsInfoFromNthPreviousWord(
                prev, spacingAndPunctuations, n);
    }

    private static boolean isSeparator(final int code, final int[] sortedSeparators) {
        return Arrays.binarySearch(sortedSeparators, code) >= 0;
    }

    private static boolean isPartOfCompositionForScript(final int codePoint,
            final SpacingAndPunctuations spacingAndPunctuations, final int scriptId, final boolean transliteration) {
        // We always consider word connectors part of compositions.
        return spacingAndPunctuations.isWordConnector(codePoint)
                // Otherwise, it's part of composition if it's part of script and not a separator.
                || (!spacingAndPunctuations.isWordSeparator(codePoint)
                        && (transliteration || ScriptUtils.isLetterPartOfScript(codePoint, scriptId)));
    }

    /**
     * Returns the text surrounding the cursor.
     *
     * @param spacingAndPunctuations the rules for spacing and punctuation
     * @param scriptId the script we consider to be writing words, as one of ScriptUtils.SCRIPT_*
     * @return a range containing the text surrounding the cursor
     */
    public TextRange getWordRangeAtCursor(final SpacingAndPunctuations spacingAndPunctuations,
            final int scriptId, final boolean transliteration) {
        mIC = mParent.getCurrentInputConnection();
        if (mIC == null) {
            return null;
        }
        final CharSequence before = mIC.getTextBeforeCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE,
                InputConnection.GET_TEXT_WITH_STYLES);
        final CharSequence after = mIC.getTextAfterCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE,
                InputConnection.GET_TEXT_WITH_STYLES);
        if (before == null || after == null) {
            return null;
        }

        // Going backward, find the first breaking point (separator)
        int startIndexInBefore = before.length();
        while (startIndexInBefore > 0) {
            final int codePoint = Character.codePointBefore(before, startIndexInBefore);
            if (!isPartOfCompositionForScript(codePoint, spacingAndPunctuations, scriptId, transliteration)) {
                break;
            }
            --startIndexInBefore;
            if (Character.isSupplementaryCodePoint(codePoint)) {
                --startIndexInBefore;
            }
        }

        // Find last word separator after the cursor
        int endIndexInAfter = -1;
        while (++endIndexInAfter < after.length()) {
            final int codePoint = Character.codePointAt(after, endIndexInAfter);
            if (!isPartOfCompositionForScript(codePoint, spacingAndPunctuations, scriptId, transliteration)) {
                break;
            }
            if (Character.isSupplementaryCodePoint(codePoint)) {
                ++endIndexInAfter;
            }
        }

        final boolean hasUrlSpans =
                SpannableStringUtils.hasUrlSpans(before, startIndexInBefore, before.length())
                || SpannableStringUtils.hasUrlSpans(after, 0, endIndexInAfter);
        // We don't use TextUtils#concat because it copies all spans without respect to their
        // nature. If the text includes a PARAGRAPH span and it has been split, then
        // TextUtils#concat will crash when it tries to concat both sides of it.
        return new TextRange(
                SpannableStringUtils.concatWithNonParagraphSuggestionSpansOnly(before, after),
                        startIndexInBefore, before.length() + endIndexInAfter, before.length(),
                        hasUrlSpans);
    }

    public boolean isCursorTouchingWord(final SpacingAndPunctuations spacingAndPunctuations) {
        if (isCursorFollowedByWordCharacter(spacingAndPunctuations)) {
            // If what's after the cursor is a word character, then we're touching a word.
            return true;
        }
        final String textBeforeCursor = mCommittedTextBeforeComposingText.toString();
        int indexOfCodePointInJavaChars = textBeforeCursor.length();
        int consideredCodePoint = 0 == indexOfCodePointInJavaChars ? Constants.NOT_A_CODE
                : textBeforeCursor.codePointBefore(indexOfCodePointInJavaChars);
        // Search for the first non word-connector char
        if (spacingAndPunctuations.isWordConnector(consideredCodePoint)) {
            indexOfCodePointInJavaChars -= Character.charCount(consideredCodePoint);
            consideredCodePoint = 0 == indexOfCodePointInJavaChars ? Constants.NOT_A_CODE
                    : textBeforeCursor.codePointBefore(indexOfCodePointInJavaChars);
        }
        return !(Constants.NOT_A_CODE == consideredCodePoint
                || spacingAndPunctuations.isWordSeparator(consideredCodePoint)
                || spacingAndPunctuations.isWordConnector(consideredCodePoint));
    }

    public boolean isCursorFollowedByWordCharacter(
            final SpacingAndPunctuations spacingAndPunctuations) {
        final CharSequence after = getTextAfterCursor(1, 0);
        if (TextUtils.isEmpty(after)) {
            return false;
        }
        final int codePointAfterCursor = Character.codePointAt(after, 0);
        if (spacingAndPunctuations.isWordSeparator(codePointAfterCursor)
                || spacingAndPunctuations.isWordConnector(codePointAfterCursor)) {
            return false;
        }
        return true;
    }

    public void removeTrailingSpace() {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        final int codePointBeforeCursor = getCodePointBeforeCursor();
        if (Constants.CODE_SPACE == codePointBeforeCursor) {
            deleteSurroundingText(1, 0);
        }
    }

    public boolean sameAsTextBeforeCursor(final CharSequence text) {
        final CharSequence beforeText = getTextBeforeCursor(text.length(), 0);
        return TextUtils.equals(text, beforeText);
    }

    public boolean revertDoubleSpacePeriod() {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        // Here we test whether we indeed have a period and a space before us. This should not
        // be needed, but it's there just in case something went wrong.
        final CharSequence textBeforeCursor = getTextBeforeCursor(2, 0);
        if (!TextUtils.equals(Constants.STRING_PERIOD_AND_SPACE, textBeforeCursor)) {
            // Theoretically we should not be coming here if there isn't ". " before the
            // cursor, but the application may be changing the text while we are typing, so
            // anything goes. We should not crash.
            Log.d(TAG, "Tried to revert double-space combo but we didn't find "
                    + "\"" + Constants.STRING_PERIOD_AND_SPACE + "\" just before the cursor.");
            return false;
        }
        // Double-space results in ". ". A backspace to cancel this should result in a single
        // space in the text field, so we replace ". " with a single space.
        deleteSurroundingText(2, 0);
        final String singleSpace = " ";
        commitText(singleSpace, 1);
        return true;
    }

    public boolean revertSwapPunctuation() {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        // Here we test whether we indeed have a space and something else before us. This should not
        // be needed, but it's there just in case something went wrong.
        final CharSequence textBeforeCursor = getTextBeforeCursor(2, 0);
        // NOTE: This does not work with surrogate pairs. Hopefully when the keyboard is able to
        // enter surrogate pairs this code will have been removed.
        if (TextUtils.isEmpty(textBeforeCursor)
                || (Constants.CODE_SPACE != textBeforeCursor.charAt(1))) {
            // We may only come here if the application is changing the text while we are typing.
            // This is quite a broken case, but not logically impossible, so we shouldn't crash,
            // but some debugging log may be in order.
            Log.d(TAG, "Tried to revert a swap of punctuation but we didn't "
                    + "find a space just before the cursor.");
            return false;
        }
        deleteSurroundingText(2, 0);
        final String text = " " + textBeforeCursor.subSequence(0, 1);
        commitText(text, 1);
        return true;
    }

    /**
     * Heuristic to determine if this is an expected update of the cursor.
     *
     * Sometimes updates to the cursor position are late because of their asynchronous nature.
     * This method tries to determine if this update is one, based on the values of the cursor
     * position in the update, and the currently expected position of the cursor according to
     * LatinIME's internal accounting. If this is not a belated expected update, then it should
     * mean that the user moved the cursor explicitly.
     * This is quite robust, but of course it's not perfect. In particular, it will fail in the
     * case we get an update A, the user types in N characters so as to move the cursor to A+N but
     * we don't get those, and then the user places the cursor between A and A+N, and we get only
     * this update and not the ones in-between. This is almost impossible to achieve even trying
     * very very hard.
     *
     * @param oldSelStart The value of the old selection in the update.
     * @param newSelStart The value of the new selection in the update.
     * @param oldSelEnd The value of the old selection end in the update.
     * @param newSelEnd The value of the new selection end in the update.
     * @return whether this is a belated expected update or not.
     */
    public boolean isBelatedExpectedUpdate(final int oldSelStart, final int newSelStart,
            final int oldSelEnd, final int newSelEnd) {
        // This update is "belated" if we are expecting it. That is, mExpectedSelStart and
        // mExpectedSelEnd match the new values that the TextView is updating TO.
        if (mExpectedSelStart == newSelStart && mExpectedSelEnd == newSelEnd) return true;
        // This update is not belated if mExpectedSelStart and mExpectedSelEnd match the old
        // values, and one of newSelStart or newSelEnd is updated to a different value. In this
        // case, it is likely that something other than the IME has moved the selection endpoint
        // to the new value.
        if (mExpectedSelStart == oldSelStart && mExpectedSelEnd == oldSelEnd
                && (oldSelStart != newSelStart || oldSelEnd != newSelEnd)) return false;
        // If neither of the above two cases hold, then the system may be having trouble keeping up
        // with updates. If 1) the selection is a cursor, 2) newSelStart is between oldSelStart
        // and mExpectedSelStart, and 3) newSelEnd is between oldSelEnd and mExpectedSelEnd, then
        // assume a belated update.
        return (newSelStart == newSelEnd)
                && (newSelStart - oldSelStart) * (mExpectedSelStart - newSelStart) >= 0
                && (newSelEnd - oldSelEnd) * (mExpectedSelEnd - newSelEnd) >= 0;
    }

    /**
     * Looks at the text just before the cursor to find out if it looks like a URL.
     *
     * The weakest point here is, if we don't have enough text bufferized, we may fail to realize
     * we are in URL situation, but other places in this class have the same limitation and it
     * does not matter too much in the practice.
     */
    public boolean textBeforeCursorLooksLikeURL() {
        return StringUtils.lastPartLooksLikeURL(mCommittedTextBeforeComposingText);
    }

    /**
     * Looks at the text just before the cursor to find out if we are inside a double quote.
     *
     * As with #textBeforeCursorLooksLikeURL, this is dependent on how much text we have cached.
     * However this won't be a concrete problem in most situations, as the cache is almost always
     * long enough for this use.
     */
    public boolean isInsideDoubleQuoteOrAfterDigit() {
        return StringUtils.isInsideDoubleQuoteOrAfterDigit(mCommittedTextBeforeComposingText);
    }

    /**
     * Try to get the text from the editor to expose lies the framework may have been
     * telling us. Concretely, when the device rotates, the frameworks tells us about where the
     * cursor used to be initially in the editor at the time it first received the focus; this
     * may be completely different from the place it is upon rotation. Since we don't have any
     * means to get the real value, try at least to ask the text view for some characters and
     * detect the most damaging cases: when the cursor position is declared to be much smaller
     * than it really is.
     */
    public void tryFixLyingCursorPosition() {
        final CharSequence textBeforeCursor = getTextBeforeCursor(
                Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
        if (null == textBeforeCursor) {
            mExpectedSelStart = mExpectedSelEnd = Constants.NOT_A_CURSOR_POSITION;
        } else {
            final int textLength = textBeforeCursor.length();
            if (textLength < Constants.EDITOR_CONTENTS_CACHE_SIZE
                    && (textLength > mExpectedSelStart
                            ||  mExpectedSelStart < Constants.EDITOR_CONTENTS_CACHE_SIZE)) {
                // It should not be possible to have only one of those variables be
                // NOT_A_CURSOR_POSITION, so if they are equal, either the selection is zero-sized
                // (simple cursor, no selection) or there is no cursor/we don't know its pos
                final boolean wasEqual = mExpectedSelStart == mExpectedSelEnd;
                mExpectedSelStart = textLength;
                // We can't figure out the value of mLastSelectionEnd :(
                // But at least if it's smaller than mLastSelectionStart something is wrong,
                // and if they used to be equal we also don't want to make it look like there is a
                // selection.
                if (wasEqual || mExpectedSelStart > mExpectedSelEnd) {
                    mExpectedSelEnd = mExpectedSelStart;
                }
            }
        }
    }

    public int getExpectedSelectionStart() {
        return mExpectedSelStart;
    }

    public int getExpectedSelectionEnd() {
        return mExpectedSelEnd;
    }

    /**
     * @return whether there is a selection currently active.
     */
    public boolean hasSelection() {
        return mExpectedSelEnd != mExpectedSelStart;
    }

    public boolean isCursorPositionKnown() {
        return INVALID_CURSOR_POSITION != mExpectedSelStart;
    }

    /**
     * Work around a bug that was present before Jelly Bean upon rotation.
     *
     * Before Jelly Bean, there is a bug where setComposingRegion and other committing
     * functions on the input connection get ignored until the cursor moves. This method works
     * around the bug by wiggling the cursor first, which reactivates the connection and has
     * the subsequent methods work, then restoring it to its original position.
     *
     * On platforms on which this method is not present, this is a no-op.
     */
    public void maybeMoveTheCursorAroundAndRestoreToWorkaroundABug() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            if (mExpectedSelStart > 0) {
                mIC.setSelection(mExpectedSelStart - 1, mExpectedSelStart - 1);
            } else {
                mIC.setSelection(mExpectedSelStart + 1, mExpectedSelStart + 1);
            }
            mIC.setSelection(mExpectedSelStart, mExpectedSelEnd);
        }
    }

    private boolean mCursorAnchorInfoMonitorEnabled = false;

    /**
     * Requests the editor to call back {@link InputMethodManager#updateCursorAnchorInfo}.
     * @param enableMonitor {@code true} to request the editor to call back the method whenever the
     * cursor/anchor position is changed.
     * @param requestImmediateCallback {@code true} to request the editor to call back the method
     * as soon as possible to notify the current cursor/anchor position to the input method.
     * @return {@code true} if the request is accepted. Returns {@code false} otherwise, which
     * includes "not implemented" or "rejected" or "temporarily unavailable" or whatever which
     * prevents the application from fulfilling the request. (TODO: Improve the API when it turns
     * out that we actually need more detailed error codes)
     */
    public boolean requestCursorUpdates(final boolean enableMonitor,
            final boolean requestImmediateCallback) {
        mIC = mParent.getCurrentInputConnection();
        final boolean scheduled;
        if (null != mIC) {
            scheduled = InputConnectionCompatUtils.requestCursorUpdates(mIC, enableMonitor,
                    requestImmediateCallback);
        } else {
            scheduled = false;
        }
        mCursorAnchorInfoMonitorEnabled = (scheduled && enableMonitor);
        return scheduled;
    }

    /**
     * @return {@code true} if the application reported that the monitor mode of
     * {@link InputMethodService#onUpdateCursorAnchorInfo(CursorAnchorInfo)} is currently enabled.
     */
    public boolean isCursorAnchorInfoMonitorEnabled() {
        return mCursorAnchorInfoMonitorEnabled;
    }
}

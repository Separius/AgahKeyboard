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

package io.separ.neural.inputmethod.indic.inputlogic;

import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.SuggestionSpan;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;

import com.android.inputmethod.keyboard.KeyboardSwitcher;
import com.android.inputmethod.keyboard.ProximityInfo;
import com.android.inputmethod.keyboard.TextDecorator;
import com.android.inputmethod.keyboard.TextDecoratorUiOperator;
import com.android.inputmethod.latin.PrevWordsInfo;
import com.android.inputmethod.latin.utils.AsyncResultHolder;
import com.android.inputmethod.latin.utils.InputTypeUtils;
import com.android.inputmethod.latin.utils.RecapitalizeStatus;
import com.android.inputmethod.latin.utils.StringUtils;
import com.android.inputmethod.latin.utils.TextRange;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import io.separ.neural.inputmethod.compat.CursorAnchorInfoCompatWrapper;
import io.separ.neural.inputmethod.compat.SuggestionSpanUtils;
import io.separ.neural.inputmethod.event.Event;
import io.separ.neural.inputmethod.event.InputTransaction;
import io.separ.neural.inputmethod.indic.Constants;
import io.separ.neural.inputmethod.indic.Dictionary;
import io.separ.neural.inputmethod.indic.DictionaryFacilitator;
import io.separ.neural.inputmethod.indic.InputPointers;
import io.separ.neural.inputmethod.indic.LastComposedWord;
import io.separ.neural.inputmethod.indic.LatinIME;
import io.separ.neural.inputmethod.indic.RichInputConnection;
import io.separ.neural.inputmethod.indic.Suggest;
import io.separ.neural.inputmethod.indic.Suggest.OnGetSuggestedWordsCallback;
import io.separ.neural.inputmethod.indic.SuggestedWords;
import io.separ.neural.inputmethod.indic.SuggestedWords.SuggestedWordInfo;
import io.separ.neural.inputmethod.indic.WordComposer;
import io.separ.neural.inputmethod.indic.define.DebugFlags;
import io.separ.neural.inputmethod.indic.define.ProductionFlags;
import io.separ.neural.inputmethod.indic.settings.SettingsValues;
import io.separ.neural.inputmethod.indic.settings.SettingsValuesForSuggestion;
import io.separ.neural.inputmethod.indic.settings.SpacingAndPunctuations;
import io.separ.neural.inputmethod.indic.suggestions.SuggestionStripViewAccessor;

/**
 * This class manages the input logic.
 */
public final class InputLogic {
    private static final String TAG = InputLogic.class.getSimpleName();

    // TODO : Remove this member when we can.
    private final LatinIME mLatinIME;
    private final SuggestionStripViewAccessor mSuggestionStripViewAccessor;

    // Never null.
    private InputLogicHandler mInputLogicHandler = InputLogicHandler.NULL_HANDLER;

    // TODO : make all these fields private as soon as possible.
    // Current space state of the input method. This can be any of the above constants.
    private int mSpaceState;
    // Never null
    public SuggestedWords mSuggestedWords = SuggestedWords.EMPTY;
    public final Suggest mSuggest;
    private final DictionaryFacilitator mDictionaryFacilitator;

    private final TextDecorator mTextDecorator = new TextDecorator(new TextDecorator.Listener() {
        @Override
        public void onClickComposingTextToAddToDictionary(final String word) {
            mLatinIME.addWordToUserDictionary(word);
            mLatinIME.dismissAddToDictionaryHint();
        }
    });

    public LastComposedWord mLastComposedWord = LastComposedWord.NOT_A_COMPOSED_WORD;
    // This has package visibility so it can be accessed from InputLogicHandler.
    /* package */ final WordComposer mWordComposer;
    public final RichInputConnection mConnection;
    private final RecapitalizeStatus mRecapitalizeStatus = new RecapitalizeStatus();

    private int mDeleteCount;
    private long mLastKeyTime;
    public final TreeSet<Long> mCurrentlyPressedHardwareKeys = new TreeSet<>();

    // Keeps track of most recently inserted text (multi-character key) for reverting
    private String mEnteredText;

    // TODO: This boolean is persistent state and causes large side effects at unexpected times.
    // Find a way to remove it for readability.
    private boolean mIsAutoCorrectionIndicatorOn;
    private long mDoubleSpacePeriodCountdownStart;

    private boolean isIndic;
    private boolean isTransliteration;
    /**
     * Create a new instance of the input logic.
     * @param latinIME the instance of the parent LatinIME. We should remove this when we can.
     * @param suggestionStripViewAccessor an object to access the suggestion strip view.
     * @param dictionaryFacilitator facilitator for getting suggestions and updating user history
     * dictionary.
     */
    public InputLogic(final LatinIME latinIME,
            final SuggestionStripViewAccessor suggestionStripViewAccessor,
            final DictionaryFacilitator dictionaryFacilitator) {
        mLatinIME = latinIME;
        mSuggestionStripViewAccessor = suggestionStripViewAccessor;
        mWordComposer = new WordComposer();
        mConnection = new RichInputConnection(latinIME);
        mInputLogicHandler = InputLogicHandler.NULL_HANDLER;
        mSuggest = new Suggest(dictionaryFacilitator);
        mDictionaryFacilitator = dictionaryFacilitator;
    }

    /**
     * Initializes the input logic for input in an editor.
     *
     * Call this when input starts or restarts in some editor (typically, in onStartInputView).
     *
     * @param combiningSpec the combining spec string for this subtype
     * @param settingsValues the current settings values
     */
    public void startInput(final String combiningSpec, final SettingsValues settingsValues) {
        mEnteredText = null;
        mWordComposer.restartCombining(combiningSpec);
        resetComposingState(true /* alsoResetLastComposedWord */);
        mDeleteCount = 0;
        mSpaceState = SpaceState.NONE;
        mRecapitalizeStatus.disable(); // Do not perform recapitalize until the cursor is moved once
        mCurrentlyPressedHardwareKeys.clear();
        mSuggestedWords = SuggestedWords.EMPTY;
        // In some cases (namely, after rotation of the device) editorInfo.initialSelStart is lying
        // so we try using some heuristics to find out about these and fix them.
        mConnection.tryFixLyingCursorPosition();
        cancelDoubleSpacePeriodCountdown();
        if (InputLogicHandler.NULL_HANDLER == mInputLogicHandler) {
            mInputLogicHandler = new InputLogicHandler(mLatinIME, this);
        } else {
            mInputLogicHandler.reset();
        }

        if (ProductionFlags.ENABLE_CURSOR_ANCHOR_INFO_CALLBACK) {
            // AcceptTypedWord feature relies on CursorAnchorInfo.
            if (settingsValues.mShouldShowUiToAcceptTypedWord) {
                mConnection.requestCursorUpdates(true /* enableMonitor */,
                        true /* requestImmediateCallback */);
            }
            mTextDecorator.reset();
        }
    }

    /**
     * Call this when the subtype changes.
     * @param combiningSpec the spec string for the combining rules
     * @param settingsValues the current settings values
     */
    public void onSubtypeChanged(final String combiningSpec, final SettingsValues settingsValues) {
        finishInput();
        startInput(combiningSpec, settingsValues);
    }

    /**
     * Call this when the orientation changes.
     * @param settingsValues the current values of the settings.
     */
    public void onOrientationChange(final SettingsValues settingsValues) {
        // If !isComposingWord, #commitTyped() is a no-op, but still, it's better to avoid
        // the useless IPC of {begin,end}BatchEdit.
        if (mWordComposer.isComposingWord()) {
            mConnection.beginBatchEdit();
            // If we had a composition in progress, we need to commit the word so that the
            // suggestionsSpan will be added. This will allow resuming on the same suggestions
            // after rotation is finished.
            commitTyped(settingsValues, LastComposedWord.NOT_A_SEPARATOR);
            mConnection.endBatchEdit();
        }
    }

    /**
     * Clean up the input logic after input is finished.
     */
    public void finishInput() {
        if (mWordComposer.isComposingWord()) {
            mConnection.finishComposingText();
        }
        resetComposingState(true /* alsoResetLastComposedWord */);
        mInputLogicHandler.reset();
    }

    // Normally this class just gets out of scope after the process ends, but in unit tests, we
    // create several instances of LatinIME in the same process, which results in several
    // instances of InputLogic. This cleans up the associated handler so that tests don't leak
    // handlers.
    public void recycle() {
        final InputLogicHandler inputLogicHandler = mInputLogicHandler;
        mInputLogicHandler = InputLogicHandler.NULL_HANDLER;
        inputLogicHandler.destroy();
        mDictionaryFacilitator.closeDictionaries();
    }

    /**
     * React to a string input.
     *
     * This is triggered by keys that input many characters at once, like the ".com" key or
     * some additional keys for example.
     *
     * @param settingsValues the current values of the settings.
     * @param event the input event containing the data.
     * @return the complete transaction object
     */
    public InputTransaction onTextInput(final SettingsValues settingsValues, final Event event,
            final int keyboardShiftMode,
            // TODO: remove this argument
            final LatinIME.UIHandler handler) {
        final String rawText = event.getTextToCommit().toString();
        final InputTransaction inputTransaction = new InputTransaction(settingsValues, event,
                SystemClock.uptimeMillis(), mSpaceState,
                getActualCapsMode(settingsValues, keyboardShiftMode));
        mConnection.beginBatchEdit();
        if (mWordComposer.isComposingWord()) {
            commitCurrentAutoCorrection(settingsValues, rawText, handler);
        } else {
            resetComposingState(true /* alsoResetLastComposedWord */);
        }
        handler.postUpdateSuggestionStrip(SuggestedWords.INPUT_STYLE_TYPING);
        final String text = performSpecificTldProcessingOnTextInput(rawText);
        if (SpaceState.PHANTOM == mSpaceState) {
            promotePhantomSpace(settingsValues);
        }
        mConnection.commitText(text, 1);
        mConnection.endBatchEdit();
        // Space state must be updated before calling updateShiftState
        mSpaceState = SpaceState.NONE;
        mEnteredText = text;
        inputTransaction.setDidAffectContents();
        inputTransaction.requireShiftUpdate(InputTransaction.SHIFT_UPDATE_NOW);
        return inputTransaction;
    }

    /**
     * Determines whether "Touch again to save" should be shown or not.
     * @param suggestionInfo the suggested word chosen by the user.
     * @return {@code true} if we should show the "Touch again to save" hint.
     */
    private boolean shouldShowAddToDictionaryHint(final SuggestedWordInfo suggestionInfo) {
        // We should show the "Touch again to save" hint if the user pressed the first entry
        // AND it's in none of our current dictionaries (main, user or otherwise).
        return (suggestionInfo.isKindOf(SuggestedWordInfo.KIND_TYPED)
                || suggestionInfo.isKindOf(SuggestedWordInfo.KIND_OOV_CORRECTION))
                && !mDictionaryFacilitator.isValidWord(suggestionInfo.mWord, true /* ignoreCase */)
                && mDictionaryFacilitator.isUserDictionaryEnabled();
    }

    /**
     * A suggestion was picked from the suggestion strip.
     * @param settingsValues the current values of the settings.
     * @param suggestionInfo the suggestion info.
     * @param keyboardShiftState the shift state of the keyboard, as returned by
     *     {@link com.android.inputmethod.keyboard.KeyboardSwitcher#getKeyboardShiftMode()}
     * @return the complete transaction object
     */
    // Called from {@link SuggestionStripView} through the {@link SuggestionStripView#Listener}
    // interface
    public InputTransaction onPickSuggestionManually(final SettingsValues settingsValues,
            final SuggestedWordInfo suggestionInfo, final int keyboardShiftState,
            // TODO: remove these arguments
            final int currentKeyboardScriptId, final LatinIME.UIHandler handler) {
        final SuggestedWords suggestedWords = mSuggestedWords;
        final String suggestion = suggestionInfo.mWord;
        // If this is a punctuation picked from the suggestion strip, pass it to onCodeInput
        if (suggestion.length() == 1 && suggestedWords.isPunctuationSuggestions()) {
            // Word separators are suggested before the user inputs something.
            // Rely on onCodeInput to do the complicated swapping/stripping logic consistently.
            final Event event = Event.createPunctuationSuggestionPickedEvent(suggestionInfo);
            return onCodeInput(settingsValues, event, keyboardShiftState,
                    currentKeyboardScriptId, handler);
        }

        final Event event = Event.createSuggestionPickedEvent(suggestionInfo);
        final InputTransaction inputTransaction = new InputTransaction(settingsValues,
                event, SystemClock.uptimeMillis(), mSpaceState, keyboardShiftState);
        // Manual pick affects the contents of the editor, so we take note of this. It's important
        // for the sequence of language switching.
        inputTransaction.setDidAffectContents();
        mConnection.beginBatchEdit();
        if (SpaceState.PHANTOM == mSpaceState && suggestion.length() > 0
                // In the batch input mode, a manually picked suggested word should just replace
                // the current batch input text and there is no need for a phantom space.
                && !mWordComposer.isBatchMode()) {
            final int firstChar = Character.codePointAt(suggestion, 0);
            if (!settingsValues.isWordSeparator(firstChar)
                    || settingsValues.isUsuallyPrecededBySpace(firstChar)) {
                promotePhantomSpace(settingsValues);
            }
        }

        // TODO: We should not need the following branch. We should be able to take the same
        // code path as for other kinds, use commitChosenWord, and do everything normally. We will
        // however need to reset the suggestion strip right away, because we know we can't take
        // the risk of calling commitCompletion twice because we don't know how the app will react.
        if (suggestionInfo.isKindOf(SuggestedWordInfo.KIND_APP_DEFINED)) {
            mSuggestedWords = SuggestedWords.EMPTY;
            mSuggestionStripViewAccessor.setNeutralSuggestionStrip();
            inputTransaction.requireShiftUpdate(InputTransaction.SHIFT_UPDATE_NOW);
            resetComposingState(true /* alsoResetLastComposedWord */);
            mConnection.commitCompletion(suggestionInfo.mApplicationSpecifiedCompletionInfo);
            mConnection.endBatchEdit();
            return inputTransaction;
        }

        final boolean shouldShowAddToDictionaryHint = shouldShowAddToDictionaryHint(suggestionInfo);
        commitChosenWord(settingsValues, suggestion, LastComposedWord.COMMIT_TYPE_MANUAL_PICK,
                LastComposedWord.NOT_A_SEPARATOR);
        mConnection.endBatchEdit();
        // Don't allow cancellation of manual pick
        mLastComposedWord.deactivate();
        // Space state must be updated before calling updateShiftState
        mSpaceState = SpaceState.PHANTOM;
        inputTransaction.requireShiftUpdate(InputTransaction.SHIFT_UPDATE_NOW);

        if (shouldShowAddToDictionaryHint) {
            mSuggestionStripViewAccessor.showAddToDictionaryHint(suggestion);
        } else {
            // If we're not showing the "Touch again to save", then update the suggestion strip.
            // That's going to be predictions (or punctuation suggestions), so INPUT_STYLE_NONE.
            handler.postUpdateSuggestionStrip(SuggestedWords.INPUT_STYLE_NONE);
        }
        return inputTransaction;
    }

    /**
     * Consider an update to the cursor position. Evaluate whether this update has happened as
     * part of normal typing or whether it was an explicit cursor move by the user. In any case,
     * do the necessary adjustments.
     * @param oldSelStart old selection start
     * @param oldSelEnd old selection end
     * @param newSelStart new selection start
     * @param newSelEnd new selection end
     * @param settingsValues the current values of the settings.
     * @return whether the cursor has moved as a result of user interaction.
     */
    public boolean onUpdateSelection(final int oldSelStart, final int oldSelEnd,
            final int newSelStart, final int newSelEnd, final SettingsValues settingsValues) {
        if (mConnection.isBelatedExpectedUpdate(oldSelStart, newSelStart, oldSelEnd, newSelEnd)) {
            return false;
        }
        // TODO: the following is probably better done in resetEntireInputState().
        // it should only happen when the cursor moved, and the very purpose of the
        // test below is to narrow down whether this happened or not. Likewise with
        // the call to updateShiftState.
        // We set this to NONE because after a cursor move, we don't want the space
        // state-related special processing to kick in.
        mSpaceState = SpaceState.NONE;

        final boolean selectionChangedOrSafeToReset =
                oldSelStart != newSelStart || oldSelEnd != newSelEnd // selection changed
                || !mWordComposer.isComposingWord(); // safe to reset
        final boolean hasOrHadSelection = (oldSelStart != oldSelEnd || newSelStart != newSelEnd);
        final int moveAmount = newSelStart - oldSelStart;
        // As an added small gift from the framework, it happens upon rotation when there
        // is a selection that we get a wrong cursor position delivered to startInput() that
        // does not get reflected in the oldSel{Start,End} parameters to the next call to
        // onUpdateSelection. In this case, we may have set a composition, and when we're here
        // we realize we shouldn't have. In theory, in this case, selectionChangedOrSafeToReset
        // should be true, but that is if the framework had taken that wrong cursor position
        // into account, which means we have to reset the entire composing state whenever there
        // is or was a selection regardless of whether it changed or not.
        if (hasOrHadSelection || !settingsValues.needsToLookupSuggestions()
                || (selectionChangedOrSafeToReset
                        && !mWordComposer.moveCursorByAndReturnIfInsideComposingWord(moveAmount))) {
            // If we are composing a word and moving the cursor, we would want to set a
            // suggestion span for recorrection to work correctly. Unfortunately, that
            // would involve the keyboard committing some new text, which would move the
            // cursor back to where it was. Latin IME could then fix the position of the cursor
            // again, but the asynchronous nature of the calls results in this wreaking havoc
            // with selection on double tap and the like.
            // Another option would be to send suggestions each time we set the composing
            // text, but that is probably too expensive to do, so we decided to leave things
            // as is.
            // Also, we're posting a resume suggestions message, and this will update the
            // suggestions strip in a few milliseconds, so if we cleared the suggestion strip here
            // we'd have the suggestion strip noticeably janky. To avoid that, we don't clear
            // it here, which means we'll keep outdated suggestions for a split second but the
            // visual result is better.
            resetEntireInputState(newSelStart, newSelEnd, false /* clearSuggestionStrip */);
        } else {
            // resetEntireInputState calls resetCachesUponCursorMove, but forcing the
            // composition to end. But in all cases where we don't reset the entire input
            // state, we still want to tell the rich input connection about the new cursor
            // position so that it can update its caches.
            mConnection.resetCachesUponCursorMoveAndReturnSuccess(
                    newSelStart, newSelEnd, false /* shouldFinishComposition */);
        }

        // The cursor has been moved : we now accept to perform recapitalization
        mRecapitalizeStatus.enable();
        // We moved the cursor and need to invalidate the indicator right now.
        mTextDecorator.reset();
        // Remaining background color that was used for the add-to-dictionary indicator should be
        // removed.
        mConnection.removeBackgroundColorFromHighlightedTextIfNecessary();
        // We moved the cursor. If we are touching a word, we need to resume suggestion.
        mLatinIME.mHandler.postResumeSuggestions(false /* shouldIncludeResumedWordInSuggestions */,
                true /* shouldDelay */);
        // Stop the last recapitalization, if started.
        mRecapitalizeStatus.stop();
        return true;
    }

    /**
     * React to a code input. It may be a code point to insert, or a symbolic value that influences
     * the keyboard behavior.
     *
     * Typically, this is called whenever a key is pressed on the software keyboard. This is not
     * the entry point for gesture input; see the onBatchInput* family of functions for this.
     *
     * @param settingsValues the current settings values.
     * @param event the event to handle.
     * @param keyboardShiftMode the current shift mode of the keyboard, as returned by
     *     {@link com.android.inputmethod.keyboard.KeyboardSwitcher#getKeyboardShiftMode()}
     * @return the complete transaction object
     */
    public InputTransaction onCodeInput(final SettingsValues settingsValues, final Event event,
            final int keyboardShiftMode,
            // TODO: remove these arguments
            final int currentKeyboardScriptId, final LatinIME.UIHandler handler) {
        final Event processedEvent = mWordComposer.processEvent(event);
        final InputTransaction inputTransaction = new InputTransaction(settingsValues,
                processedEvent, SystemClock.uptimeMillis(), mSpaceState,
                getActualCapsMode(settingsValues, keyboardShiftMode));
        if (processedEvent.mKeyCode != Constants.CODE_DELETE
                || inputTransaction.mTimestamp > mLastKeyTime + Constants.LONG_PRESS_MILLISECONDS) {
            mDeleteCount = 0;
        }
        mLastKeyTime = inputTransaction.mTimestamp;
        mConnection.beginBatchEdit();
        if (!mWordComposer.isComposingWord()) {
            // TODO: is this useful? It doesn't look like it should be done here, but rather after
            // a word is committed.
            mIsAutoCorrectionIndicatorOn = false;
        }

        // TODO: Consolidate the double-space period timer, mLastKeyTime, and the space state.
        if (processedEvent.mCodePoint != Constants.CODE_SPACE) {
            cancelDoubleSpacePeriodCountdown();
        }

        Event currentEvent = processedEvent;
        while (null != currentEvent) {
            if (currentEvent.isConsumed()) {
                handleConsumedEvent(currentEvent, inputTransaction);
            } else if (currentEvent.isFunctionalKeyEvent()) {
                handleFunctionalEvent(currentEvent, inputTransaction, currentKeyboardScriptId,
                        handler);
            } else {
                handleNonFunctionalEvent(currentEvent, inputTransaction, handler);
            }
            currentEvent = currentEvent.mNextEvent;
        }
        if (!inputTransaction.didAutoCorrect() && processedEvent.mKeyCode != Constants.CODE_SHIFT
                && processedEvent.mKeyCode != Constants.CODE_CAPSLOCK
                && processedEvent.mKeyCode != Constants.CODE_SWITCH_ALPHA_SYMBOL)
            mLastComposedWord.deactivate();
        if (Constants.CODE_DELETE != processedEvent.mKeyCode) {
            mEnteredText = null;
        }
        mConnection.endBatchEdit();
        return inputTransaction;
    }

    public void onStartBatchInput(final SettingsValues settingsValues,
            // TODO: remove these arguments
            final KeyboardSwitcher keyboardSwitcher, final LatinIME.UIHandler handler) {
        mInputLogicHandler.onStartBatchInput();
        handler.showGesturePreviewAndSuggestionStrip(
                SuggestedWords.EMPTY, false /* dismissGestureFloatingPreviewText */);
        handler.cancelUpdateSuggestionStrip();
        ++mAutoCommitSequenceNumber;
        mConnection.beginBatchEdit();
        if (!mWordComposer.isComposingWord()) {
            mConnection.removeBackgroundColorFromHighlightedTextIfNecessary();
        } else {
            if (mWordComposer.isCursorFrontOrMiddleOfComposingWord()) {
                // If we are in the middle of a recorrection, we need to commit the recorrection
                // first so that we can insert the batch input at the current cursor position.
                // We also need to unlearn the original word that is now being corrected.
                resetEntireInputState(mConnection.getExpectedSelectionStart(),
                        mConnection.getExpectedSelectionEnd(), true /* clearSuggestionStrip */);
            } else if (mWordComposer.isSingleLetter()) {
                // We auto-correct the previous (typed, not gestured) string iff it's one character
                // long. The reason for this is, even in the middle of gesture typing, you'll still
                // tap one-letter words and you want them auto-corrected (typically, "i" in English
                // should become "I"). However for any longer word, we assume that the reason for
                // tapping probably is that the word you intend to type is not in the dictionary,
                // so we do not attempt to correct, on the assumption that if that was a dictionary
                // word, the user would probably have gestured instead.
                commitCurrentAutoCorrection(settingsValues, LastComposedWord.NOT_A_SEPARATOR,
                        handler);
            } else {
                commitTyped(settingsValues, LastComposedWord.NOT_A_SEPARATOR);
            }
        }
        final int codePointBeforeCursor = mConnection.getCodePointBeforeCursor();
        if (Character.isLetterOrDigit(codePointBeforeCursor)
                || settingsValues.isUsuallyFollowedBySpace(codePointBeforeCursor)) {
            final boolean autoShiftHasBeenOverriden = keyboardSwitcher.getKeyboardShiftMode() !=
                    getCurrentAutoCapsState(settingsValues);
            mSpaceState = SpaceState.PHANTOM;
            if (!autoShiftHasBeenOverriden) {
                // When we change the space state, we need to update the shift state of the
                // keyboard unless it has been overridden manually. This is happening for example
                // after typing some letters and a period, then gesturing; the keyboard is not in
                // caps mode yet, but since a gesture is starting, it should go in caps mode,
                // unless the user explictly said it should not.
                keyboardSwitcher.requestUpdatingShiftState(getCurrentAutoCapsState(settingsValues),
                        getCurrentRecapitalizeState());
            }
        }
        mConnection.endBatchEdit();
        mWordComposer.setCapitalizedModeAtStartComposingTime(
                getActualCapsMode(settingsValues, keyboardSwitcher.getKeyboardShiftMode()));
    }

    /* The sequence number member is only used in onUpdateBatchInput. It is increased each time
     * auto-commit happens. The reason we need this is, when auto-commit happens we trim the
     * input pointers that are held in a singleton, and to know how much to trim we rely on the
     * results of the suggestion process that is held in mSuggestedWords.
     * However, the suggestion process is asynchronous, and sometimes we may enter the
     * onUpdateBatchInput method twice without having recomputed suggestions yet, or having
     * received new suggestions generated from not-yet-trimmed input pointers. In this case, the
     * mIndexOfTouchPointOfSecondWords member will be out of date, and we must not use it lest we
     * remove an unrelated number of pointers (possibly even more than are left in the input
     * pointers, leading to a crash).
     * To avoid that, we increase the sequence number each time we auto-commit and trim the
     * input pointers, and we do not use any suggested words that have been generated with an
     * earlier sequence number.
     */
    private int mAutoCommitSequenceNumber = 1;
    public void onUpdateBatchInput(final SettingsValues settingsValues,
            final InputPointers batchPointers,
            // TODO: remove these arguments
            final KeyboardSwitcher keyboardSwitcher) {
        if (settingsValues.mPhraseGestureEnabled) {
            final SuggestedWordInfo candidate = mSuggestedWords.getAutoCommitCandidate();
            // If these suggested words have been generated with out of date input pointers, then
            // we skip auto-commit (see comments above on the mSequenceNumber member).
            if (null != candidate
                    && mSuggestedWords.mSequenceNumber >= mAutoCommitSequenceNumber) {
                if (candidate.mSourceDict.shouldAutoCommit(candidate)) {
                    final String[] commitParts = candidate.mWord.split(Constants.WORD_SEPARATOR, 2);
                    batchPointers.shift(candidate.mIndexOfTouchPointOfSecondWord);
                    promotePhantomSpace(settingsValues);
                    mConnection.commitText(commitParts[0], 0);
                    mSpaceState = SpaceState.PHANTOM;
                    keyboardSwitcher.requestUpdatingShiftState(
                            getCurrentAutoCapsState(settingsValues), getCurrentRecapitalizeState());
                    mWordComposer.setCapitalizedModeAtStartComposingTime(getActualCapsMode(
                            settingsValues, keyboardSwitcher.getKeyboardShiftMode()));
                    ++mAutoCommitSequenceNumber;
                }
            }
        }
        mInputLogicHandler.onUpdateBatchInput(batchPointers, mAutoCommitSequenceNumber);
    }

    public void onEndBatchInput(final InputPointers batchPointers) {
        mInputLogicHandler.updateTailBatchInput(batchPointers, mAutoCommitSequenceNumber);
        ++mAutoCommitSequenceNumber;
    }

    // TODO: remove this argument
    public void onCancelBatchInput(final LatinIME.UIHandler handler) {
        mInputLogicHandler.onCancelBatchInput();
        handler.showGesturePreviewAndSuggestionStrip(
                SuggestedWords.EMPTY, true /* dismissGestureFloatingPreviewText */);
    }

    // TODO: on the long term, this method should become private, but it will be difficult.
    // Especially, how do we deal with InputMethodService.onDisplayCompletions?
    public void setSuggestedWords(final SuggestedWords suggestedWords,
            final SettingsValues settingsValues, final LatinIME.UIHandler handler) {

        if (SuggestedWords.EMPTY != suggestedWords) {
            final String autoCorrection;
            if (suggestedWords.mWillAutoCorrect) {
                autoCorrection = suggestedWords.getWord(SuggestedWords.INDEX_OF_AUTO_CORRECTION);
            } else {
                // We can't use suggestedWords.getWord(SuggestedWords.INDEX_OF_TYPED_WORD)
                // because it may differ from mWordComposer.mTypedWord.
                autoCorrection = suggestedWords.mTypedWord;
            }
            mWordComposer.setAutoCorrection(autoCorrection);
        }
        mSuggestedWords = suggestedWords;
        final boolean newAutoCorrectionIndicator = suggestedWords.mWillAutoCorrect;

        // Put a blue underline to a word in TextView which will be auto-corrected.
        if (mIsAutoCorrectionIndicatorOn != newAutoCorrectionIndicator
                && mWordComposer.isComposingWord()) {
            mIsAutoCorrectionIndicatorOn = newAutoCorrectionIndicator;
            final CharSequence textWithUnderline =
                    getTextWithUnderline(mWordComposer.getTypedWord());
            // TODO: when called from an updateSuggestionStrip() call that results from a posted
            // message, this is called outside any batch edit. Potentially, this may result in some
            // janky flickering of the screen, although the display speed makes it unlikely in
            // the practice.
            setComposingTextInternal(textWithUnderline, 1);
        }
    }

    /**
     * Handle a consumed event.
     *
     * Consumed events represent events that have already been consumed, typically by the
     * combining chain.
     *
     * @param event The event to handle.
     * @param inputTransaction The transaction in progress.
     */
    private void handleConsumedEvent(final Event event, final InputTransaction inputTransaction) {
        // A consumed event may have text to commit and an update to the composing state, so
        // we evaluate both. With some combiners, it's possible than an event contains both
        // and we enter both of the following if clauses.
        final CharSequence textToCommit = event.getTextToCommit();
        if (!TextUtils.isEmpty(textToCommit)) {
            mConnection.commitText(textToCommit, 1);
            inputTransaction.setDidAffectContents();
        }
        if (mWordComposer.isComposingWord()) {
            setComposingTextInternal(mWordComposer.getTypedWord(), 1);
            inputTransaction.setDidAffectContents();
            inputTransaction.setRequiresUpdateSuggestions();
        }
    }

    /**
     * Handle a functional key event.
     *
     * A functional event is a special key, like delete, shift, emoji, or the settings key.
     * Non-special keys are those that generate a single code point.
     * This includes all letters, digits, punctuation, separators, emoji. It excludes keys that
     * manage keyboard-related stuff like shift, language switch, settings, layout switch, or
     * any key that results in multiple code points like the ".com" key.
     *
     * @param event The event to handle.
     * @param inputTransaction The transaction in progress.
     */
    private void handleFunctionalEvent(final Event event, final InputTransaction inputTransaction,
            // TODO: remove these arguments
            final int currentKeyboardScriptId, final LatinIME.UIHandler handler) {
        switch (event.mKeyCode) {
            case Constants.CODE_DELETE:
                handleBackspaceEvent(event, inputTransaction, currentKeyboardScriptId);
                // Backspace is a functional key, but it affects the contents of the editor.
                inputTransaction.setDidAffectContents();
                break;
            case Constants.CODE_SHIFT:
                performRecapitalization(inputTransaction.mSettingsValues);
                inputTransaction.requireShiftUpdate(InputTransaction.SHIFT_UPDATE_NOW);
                if (mSuggestedWords.isPrediction()) {
                    inputTransaction.setRequiresUpdateSuggestions();
                }
                break;
            case Constants.CODE_CAPSLOCK:
                // Note: Changing keyboard to shift lock state is handled in
                // {@link KeyboardSwitcher#onCodeInput(int)}.
                break;
            case Constants.CODE_SYMBOL_SHIFT:
                // Note: Calling back to the keyboard on the symbol Shift key is handled in
                // {@link #onPressKey(int,int,boolean)} and {@link #onReleaseKey(int,boolean)}.
                break;
            case Constants.CODE_SWITCH_ALPHA_SYMBOL:
                // Note: Calling back to the keyboard on symbol key is handled in
                // {@link #onPressKey(int,int,boolean)} and {@link #onReleaseKey(int,boolean)}.
                break;
            case Constants.CODE_SETTINGS:
                onSettingsKeyPressed();
                break;
            case Constants.CODE_SHORTCUT:
                // We need to switch to the shortcut IME. This is handled by LatinIME since the
                // input logic has no business with IME switching.
                break;
            case Constants.CODE_ACTION_NEXT:
                performEditorAction(EditorInfo.IME_ACTION_NEXT);
                break;
            case Constants.CODE_ACTION_PREVIOUS:
                performEditorAction(EditorInfo.IME_ACTION_PREVIOUS);
                break;
            case Constants.CODE_LANGUAGE_SWITCH:
                handleLanguageSwitchKey();
                break;
            case Constants.CODE_EMOJI:
                // Note: Switching emoji keyboard is being handled in
                // {@link KeyboardState#onCodeInput(int,int)}.
                break;
            case Constants.CODE_ALPHA_FROM_EMOJI:
                // Note: Switching back from Emoji keyboard to the main keyboard is being
                // handled in {@link KeyboardState#onCodeInput(int,int)}.
                break;
            case Constants.CODE_SHIFT_ENTER:
                // TODO: remove this object
                final Event tmpEvent = Event.createSoftwareKeypressEvent(Constants.CODE_ENTER,
                        event.mKeyCode, event.mX, event.mY, event.isKeyRepeat());
                handleNonSpecialCharacterEvent(tmpEvent, inputTransaction, handler);
                // Shift + Enter is treated as a functional key but it results in adding a new
                // line, so that does affect the contents of the editor.
                inputTransaction.setDidAffectContents();
                break;
            case Constants.CODE_INLINESETTINGS:
                break;
            default:
                throw new RuntimeException("Unknown key code : " + event.mKeyCode);
        }
    }

    /**
     * Handle an event that is not a functional event.
     *
     * These events are generally events that cause input, but in some cases they may do other
     * things like trigger an editor action.
     *
     * @param event The event to handle.
     * @param inputTransaction The transaction in progress.
     */
    private void handleNonFunctionalEvent(final Event event,
            final InputTransaction inputTransaction,
            // TODO: remove this argument
            final LatinIME.UIHandler handler) {
        inputTransaction.setDidAffectContents();
        switch (event.mCodePoint) {
            case Constants.CODE_ENTER:
                final EditorInfo editorInfo = getCurrentInputEditorInfo();
                final int imeOptionsActionId =
                        InputTypeUtils.getImeOptionsActionIdFromEditorInfo(editorInfo);
                if (InputTypeUtils.IME_ACTION_CUSTOM_LABEL == imeOptionsActionId) {
                    // Either we have an actionLabel and we should performEditorAction with
                    // actionId regardless of its value.
                    performEditorAction(editorInfo.actionId);
                } else if (EditorInfo.IME_ACTION_NONE != imeOptionsActionId) {
                    // We didn't have an actionLabel, but we had another action to execute.
                    // EditorInfo.IME_ACTION_NONE explicitly means no action. In contrast,
                    // EditorInfo.IME_ACTION_UNSPECIFIED is the default value for an action, so it
                    // means there should be an action and the app didn't bother to set a specific
                    // code for it - presumably it only handles one. It does not have to be treated
                    // in any specific way: anything that is not IME_ACTION_NONE should be sent to
                    // performEditorAction.
                    performEditorAction(imeOptionsActionId);
                } else {
                    // No action label, and the action from imeOptions is NONE: this is a regular
                    // enter key that should input a carriage return.
                    handleNonSpecialCharacterEvent(event, inputTransaction, handler);
                }
                break;
            default:
                handleNonSpecialCharacterEvent(event, inputTransaction, handler);
                break;
        }
    }

    /**
     * Handle inputting a code point to the editor.
     *
     * Non-special keys are those that generate a single code point.
     * This includes all letters, digits, punctuation, separators, emoji. It excludes keys that
     * manage keyboard-related stuff like shift, language switch, settings, layout switch, or
     * any key that results in multiple code points like the ".com" key.
     *
     * @param event The event to handle.
     * @param inputTransaction The transaction in progress.
     */
    private void handleNonSpecialCharacterEvent(final Event event,
            final InputTransaction inputTransaction,
            // TODO: remove this argument
            final LatinIME.UIHandler handler) {
        if (!mWordComposer.isComposingWord()) {
            mConnection.removeBackgroundColorFromHighlightedTextIfNecessary();
            // In case the "add to dictionary" hint was still displayed.
            // TODO: Do we really need to check if we have composing text here?
            if (mSuggestionStripViewAccessor.isShowingAddToDictionaryHint()) {
                mSuggestionStripViewAccessor.dismissAddToDictionaryHint();
                mTextDecorator.reset();
            }
        }

        final int codePoint = event.mCodePoint;
        mSpaceState = SpaceState.NONE;
        if (inputTransaction.mSettingsValues.isWordSeparator(codePoint)
                || Character.getType(codePoint) == Character.OTHER_SYMBOL) {
            handleSeparatorEvent(event, inputTransaction, handler);
        } else {
            if (SpaceState.PHANTOM == inputTransaction.mSpaceState) {
                if (mWordComposer.isCursorFrontOrMiddleOfComposingWord()) {
                    // If we are in the middle of a recorrection, we need to commit the recorrection
                    // first so that we can insert the character at the current cursor position.
                    // We also need to unlearn the original word that is now being corrected.
                    resetEntireInputState(mConnection.getExpectedSelectionStart(),
                            mConnection.getExpectedSelectionEnd(), true /* clearSuggestionStrip */);
                } else {
                    commitTyped(inputTransaction.mSettingsValues, LastComposedWord.NOT_A_SEPARATOR);
                }
            }
            handleNonSeparatorEvent(event, inputTransaction.mSettingsValues, inputTransaction);
        }
    }

    /**
     * Handle a non-separator.
     * @param event The event to handle.
     * @param settingsValues The current settings values.
     * @param inputTransaction The transaction in progress.
     */
    private void handleNonSeparatorEvent(final Event event, final SettingsValues settingsValues,
            final InputTransaction inputTransaction) {
        final int codePoint = event.mCodePoint;
        // TODO: refactor this method to stop flipping isComposingWord around all the time, and
        // make it shorter (possibly cut into several pieces). Also factor
        // handleNonSpecialCharacterEvent which has the same name as other handle* methods but is
        // not the same.
        boolean isComposingWord = mWordComposer.isComposingWord();

        // TODO: remove isWordConnector() and use isUsuallyFollowedBySpace() instead.
        // See onStartBatchInput() to see how to do it.
        if (SpaceState.PHANTOM == inputTransaction.mSpaceState
                && !settingsValues.isWordConnector(codePoint)) {
            if (isComposingWord) {
                // Sanity check
                throw new RuntimeException("Should not be composing here");
            }
            promotePhantomSpace(settingsValues);
        }

        if (mWordComposer.isCursorFrontOrMiddleOfComposingWord()) {
            // If we are in the middle of a recorrection, we need to commit the recorrection
            // first so that we can insert the character at the current cursor position.
            // We also need to unlearn the original word that is now being corrected.
            resetEntireInputState(mConnection.getExpectedSelectionStart(),
                    mConnection.getExpectedSelectionEnd(), true /* clearSuggestionStrip */);
            isComposingWord = false;
        }
        // We want to find out whether to start composing a new word with this character. If so,
        // we need to reset the composing state and switch isComposingWord. The order of the
        // tests is important for good performance.
        // We only start composing if we're not already composing.
        if (!isComposingWord
        // We only start composing if this is a word code point. Essentially that means it's a
        // a letter or a word connector.
                && settingsValues.isWordCodePoint(codePoint)
        // We never go into composing state if suggestions are not requested.
                && settingsValues.needsToLookupSuggestions() &&
        // In languages with spaces, we only start composing a word when we are not already
        // touching a word. In languages without spaces, the above conditions are sufficient.
                (!mConnection.isCursorTouchingWord(settingsValues.mSpacingAndPunctuations)
                        || !settingsValues.mSpacingAndPunctuations.mCurrentLanguageHasSpaces)) {
            // Reset entirely the composing state anyway, then start composing a new word unless
            // the character is a word connector. The idea here is, word connectors are not
            // separators and they should be treated as normal characters, except in the first
            // position where they should not start composing a word.
            isComposingWord = !settingsValues.mSpacingAndPunctuations.isWordConnector(codePoint);
            // Here we don't need to reset the last composed word. It will be reset
            // when we commit this one, if we ever do; if on the other hand we backspace
            // it entirely and resume suggestions on the previous word, we'd like to still
            // have touch coordinates for it.
            resetComposingState(false /* alsoResetLastComposedWord */);
        }
        if (isComposingWord) {
            mWordComposer.applyProcessedEvent(event);
            // If it's the first letter, make note of auto-caps state
            if (mWordComposer.isSingleLetter()) {
                mWordComposer.setCapitalizedModeAtStartComposingTime(inputTransaction.mShiftState);
            }
            setComposingTextInternal(getTextWithUnderline(mWordComposer.getTypedWord()), 1);
        } else {
            final boolean swapWeakSpace = tryStripSpaceAndReturnWhetherShouldSwapInstead(event,
                    inputTransaction);

            if (swapWeakSpace && trySwapSwapperAndSpace(event, inputTransaction)) {
                mSpaceState = SpaceState.WEAK;
            } else {
                sendKeyCodePoint(settingsValues, codePoint, isTransliteration);
            }
        }
        inputTransaction.setRequiresUpdateSuggestions();
    }

    /**
     * Handle input of a separator code point.
     * @param event The event to handle.
     * @param inputTransaction The transaction in progress.
     */
    private void handleSeparatorEvent(final Event event, final InputTransaction inputTransaction,
            // TODO: remove this argument
            final LatinIME.UIHandler handler) {
        final int codePoint = event.mCodePoint;
        final SettingsValues settingsValues = inputTransaction.mSettingsValues;
        final boolean wasComposingWord = mWordComposer.isComposingWord();
        // We avoid sending spaces in languages without spaces if we were composing.
        final boolean shouldAvoidSendingCode = Constants.CODE_SPACE == codePoint
                && !settingsValues.mSpacingAndPunctuations.mCurrentLanguageHasSpaces
                && wasComposingWord;
        if (mWordComposer.isCursorFrontOrMiddleOfComposingWord()) {
            // If we are in the middle of a recorrection, we need to commit the recorrection
            // first so that we can insert the separator at the current cursor position.
            // We also need to unlearn the original word that is now being corrected.
            resetEntireInputState(mConnection.getExpectedSelectionStart(),
                    mConnection.getExpectedSelectionEnd(), true /* clearSuggestionStrip */);
        }
        // isComposingWord() may have changed since we stored wasComposing
        if (mWordComposer.isComposingWord()) {
            if (settingsValues.mAutoCorrectionEnabledPerUserSettings) {
                final String separator = shouldAvoidSendingCode ? LastComposedWord.NOT_A_SEPARATOR
                        : StringUtils.newSingleCodePointString(codePoint);
                commitCurrentAutoCorrection(settingsValues, separator, handler);
                inputTransaction.setDidAutoCorrect();
            } else {
                commitTyped(settingsValues,
                        StringUtils.newSingleCodePointString(codePoint));
            }
        }

        final boolean swapWeakSpace = tryStripSpaceAndReturnWhetherShouldSwapInstead(event,
                inputTransaction);

        final boolean isInsideDoubleQuoteOrAfterDigit = Constants.CODE_DOUBLE_QUOTE == codePoint
                && mConnection.isInsideDoubleQuoteOrAfterDigit();

        final boolean needsPrecedingSpace;
        if (SpaceState.PHANTOM != inputTransaction.mSpaceState) {
            needsPrecedingSpace = false;
        } else if (Constants.CODE_DOUBLE_QUOTE == codePoint) {
            // Double quotes behave like they are usually preceded by space iff we are
            // not inside a double quote or after a digit.
            needsPrecedingSpace = !isInsideDoubleQuoteOrAfterDigit;
        } else
            needsPrecedingSpace = !(settingsValues.mSpacingAndPunctuations.isClusteringSymbol(codePoint)
                    && settingsValues.mSpacingAndPunctuations.isClusteringSymbol(
                    mConnection.getCodePointBeforeCursor())) && settingsValues.isUsuallyPrecededBySpace(codePoint);

        if (needsPrecedingSpace) {
            promotePhantomSpace(settingsValues);
        }

        if (tryPerformDoubleSpacePeriod(event, inputTransaction)) {
            mSpaceState = SpaceState.DOUBLE;
            inputTransaction.setRequiresUpdateSuggestions();
        } else if (swapWeakSpace && trySwapSwapperAndSpace(event, inputTransaction)) {
            mSpaceState = SpaceState.SWAP_PUNCTUATION;
            mSuggestionStripViewAccessor.setNeutralSuggestionStrip();
        } else if (Constants.CODE_SPACE == codePoint) {
            if (!mSuggestedWords.isPunctuationSuggestions()) {
                mSpaceState = SpaceState.WEAK;
            }

            startDoubleSpacePeriodCountdown(inputTransaction);
            if (wasComposingWord || mSuggestedWords.isEmpty()) {
                inputTransaction.setRequiresUpdateSuggestions();
            }

            if (!shouldAvoidSendingCode) {
                sendKeyCodePoint(settingsValues, codePoint, false);
            }
        } else {
            if ((SpaceState.PHANTOM == inputTransaction.mSpaceState
                    && settingsValues.isUsuallyFollowedBySpace(codePoint))
                    || (Constants.CODE_DOUBLE_QUOTE == codePoint
                            && isInsideDoubleQuoteOrAfterDigit)) {
                // If we are in phantom space state, and the user presses a separator, we want to
                // stay in phantom space state so that the next keypress has a chance to add the
                // space. For example, if I type "Good dat", pick "day" from the suggestion strip
                // then insert a comma and go on to typing the next word, I want the space to be
                // inserted automatically before the next word, the same way it is when I don't
                // input the comma. A double quote behaves like it's usually followed by space if
                // we're inside a double quote.
                // The case is a little different if the separator is a space stripper. Such a
                // separator does not normally need a space on the right (that's the difference
                // between swappers and strippers), so we should not stay in phantom space state if
                // the separator is a stripper. Hence the additional test above.
                mSpaceState = SpaceState.PHANTOM;
            }

            sendKeyCodePoint(settingsValues, codePoint, false);

            // Set punctuation right away. onUpdateSelection will fire but tests whether it is
            // already displayed or not, so it's okay.
            mSuggestionStripViewAccessor.setNeutralSuggestionStrip();
        }

        inputTransaction.requireShiftUpdate(InputTransaction.SHIFT_UPDATE_NOW);
    }

    /**
     * Handle a press on the backspace key.
     * @param event The event to handle.
     * @param inputTransaction The transaction in progress.
     */
    private void handleBackspaceEvent(final Event event, final InputTransaction inputTransaction,
            // TODO: remove this argument, put it into settingsValues
            final int currentKeyboardScriptId) {
        mSpaceState = SpaceState.NONE;
        mDeleteCount++;

        // In many cases after backspace, we need to update the shift state. Normally we need
        // to do this right away to avoid the shift state being out of date in case the user types
        // backspace then some other character very fast. However, in the case of backspace key
        // repeat, this can lead to flashiness when the cursor flies over positions where the
        // shift state should be updated, so if this is a key repeat, we update after a small delay.
        // Then again, even in the case of a key repeat, if the cursor is at start of text, it
        // can't go any further back, so we can update right away even if it's a key repeat.
        final int shiftUpdateKind =
                event.isKeyRepeat() && mConnection.getExpectedSelectionStart() > 0
                ? InputTransaction.SHIFT_UPDATE_LATER : InputTransaction.SHIFT_UPDATE_NOW;
        inputTransaction.requireShiftUpdate(shiftUpdateKind);

        if (mWordComposer.isCursorFrontOrMiddleOfComposingWord()) {
            // If we are in the middle of a recorrection, we need to commit the recorrection
            // first so that we can remove the character at the current cursor position.
            // We also need to unlearn the original word that is now being corrected.
            resetEntireInputState(mConnection.getExpectedSelectionStart(),
                    mConnection.getExpectedSelectionEnd(), true /* clearSuggestionStrip */);
            // When we exit this if-clause, mWordComposer.isComposingWord() will return false.
        }
        if (mWordComposer.isComposingWord()) {
            if (mWordComposer.isBatchMode()) {
                final String rejectedSuggestion = mWordComposer.getTypedWord();
                mWordComposer.reset();
                mWordComposer.setRejectedBatchModeSuggestion(rejectedSuggestion);
                if (!TextUtils.isEmpty(rejectedSuggestion)) {
                    mDictionaryFacilitator.removeWordFromPersonalizedDicts(rejectedSuggestion);
                }
            } else {
                mWordComposer.applyProcessedEvent(event);
            }
            if (mWordComposer.isComposingWord()) {
                setComposingTextInternal(getTextWithUnderline(mWordComposer.getTypedWord()), 1);
            } else {
                mConnection.commitText("", 1);
            }
            inputTransaction.setRequiresUpdateSuggestions();
        } else {
            if (mLastComposedWord.canRevertCommit()) {
                revertCommit(inputTransaction, inputTransaction.mSettingsValues);
                return;
            }
            if (mEnteredText != null && mConnection.sameAsTextBeforeCursor(mEnteredText)) {
                // Cancel multi-character input: remove the text we just entered.
                // This is triggered on backspace after a key that inputs multiple characters,
                // like the smiley key or the .com key.
                mConnection.deleteSurroundingText(mEnteredText.length(), 0);
                mEnteredText = null;
                // If we have mEnteredText, then we know that mHasUncommittedTypedChars == false.
                // In addition we know that spaceState is false, and that we should not be
                // reverting any autocorrect at this point. So we can safely return.
                return;
            }
            if (SpaceState.DOUBLE == inputTransaction.mSpaceState) {
                cancelDoubleSpacePeriodCountdown();
                if (mConnection.revertDoubleSpacePeriod()) {
                    // No need to reset mSpaceState, it has already be done (that's why we
                    // receive it as a parameter)
                    inputTransaction.setRequiresUpdateSuggestions();
                    mWordComposer.setCapitalizedModeAtStartComposingTime(
                            WordComposer.CAPS_MODE_OFF);
                    return;
                }
            } else if (SpaceState.SWAP_PUNCTUATION == inputTransaction.mSpaceState) {
                if (mConnection.revertSwapPunctuation()) {
                    // Likewise
                    return;
                }
            }

            // No cancelling of commit/double space/swap: we have a regular backspace.
            // We should backspace one char and restart suggestion if at the end of a word.
            if (mConnection.hasSelection()) {
                // If there is a selection, remove it.
                // We also need to unlearn the selected text.
                final int numCharsDeleted = mConnection.getExpectedSelectionEnd()
                        - mConnection.getExpectedSelectionStart();
                mConnection.setSelection(mConnection.getExpectedSelectionEnd(),
                        mConnection.getExpectedSelectionEnd());
                mConnection.deleteSurroundingText(numCharsDeleted, 0);
            } else {
                // There is no selection, just delete one character.
                if (Constants.NOT_A_CURSOR_POSITION == mConnection.getExpectedSelectionEnd()) {
                    // This should never happen.
                    Log.e(TAG, "Backspace when we don't know the selection position");
                }
                if (inputTransaction.mSettingsValues.isBeforeJellyBean() ||
                        inputTransaction.mSettingsValues.mInputAttributes.isTypeNull()) {
                    // There are two possible reasons to send a key event: either the field has
                    // type TYPE_NULL, in which case the keyboard should send events, or we are
                    // running in backward compatibility mode. Before Jelly bean, the keyboard
                    // would simulate a hardware keyboard event on pressing enter or delete. This
                    // is bad for many reasons (there are race conditions with commits) but some
                    // applications are relying on this behavior so we continue to support it for
                    // older apps, so we retain this behavior if the app has target SDK < JellyBean.
                    sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL);
                    if (mDeleteCount > Constants.DELETE_ACCELERATE_AT) {
                        sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL);
                    }
                } else {
                    final int codePointBeforeCursor = mConnection.getCodePointBeforeCursor();
                    if (codePointBeforeCursor == Constants.NOT_A_CODE) {
                        // HACK for backward compatibility with broken apps that haven't realized
                        // yet that hardware keyboards are not the only way of inputting text.
                        // Nothing to delete before the cursor. We should not do anything, but many
                        // broken apps expect something to happen in this case so that they can
                        // catch it and have their broken interface react. If you need the keyboard
                        // to do this, you're doing it wrong -- please fix your app.
                        mConnection.deleteSurroundingText(1, 0);
                        return;
                    }
                    final int lengthToDelete =
                            Character.isSupplementaryCodePoint(codePointBeforeCursor) ? 2 : 1;
                    mConnection.deleteSurroundingText(lengthToDelete, 0);
                    if (mDeleteCount > Constants.DELETE_ACCELERATE_AT) {
                        final int codePointBeforeCursorToDeleteAgain =
                                mConnection.getCodePointBeforeCursor();
                        if (codePointBeforeCursorToDeleteAgain != Constants.NOT_A_CODE) {
                            final int lengthToDeleteAgain = Character.isSupplementaryCodePoint(
                                    codePointBeforeCursorToDeleteAgain) ? 2 : 1;
                            mConnection.deleteSurroundingText(lengthToDeleteAgain, 0);
                        }
                    }
                }
            }
            if (inputTransaction.mSettingsValues
                    .isSuggestionsEnabledPerUserSettings()
                    && inputTransaction.mSettingsValues.mSpacingAndPunctuations
                            .mCurrentLanguageHasSpaces
                    && !mConnection.isCursorFollowedByWordCharacter(
                            inputTransaction.mSettingsValues.mSpacingAndPunctuations)) {
                restartSuggestionsOnWordTouchedByCursor(inputTransaction.mSettingsValues,
                        true /* shouldIncludeResumedWordInSuggestions */, currentKeyboardScriptId);
            }
        }
    }

    /**
     * Handle a press on the language switch key (the "globe key")
     */
    private void handleLanguageSwitchKey() {
        mLatinIME.switchToNextSubtype();
    }

    /**
     * Swap a space with a space-swapping punctuation sign.
     *
     * This method will check that there are two characters before the cursor and that the first
     * one is a space before it does the actual swapping.
     * @param event The event to handle.
     * @param inputTransaction The transaction in progress.
     * @return true if the swap has been performed, false if it was prevented by preliminary checks.
     */
    private boolean trySwapSwapperAndSpace(final Event event,
            final InputTransaction inputTransaction) {
        final int codePointBeforeCursor = mConnection.getCodePointBeforeCursor();
        if (Constants.CODE_SPACE != codePointBeforeCursor) {
            return false;
        }
        mConnection.deleteSurroundingText(1, 0);
        final String text = event.getTextToCommit() + " ";
        mConnection.commitText(text, 1);
        inputTransaction.requireShiftUpdate(InputTransaction.SHIFT_UPDATE_NOW);
        return true;
    }

    /*
     * Strip a trailing space if necessary and returns whether it's a swap weak space situation.
     * @param event The event to handle.
     * @param inputTransaction The transaction in progress.
     * @return whether we should swap the space instead of removing it.
     */
    private boolean tryStripSpaceAndReturnWhetherShouldSwapInstead(final Event event,
            final InputTransaction inputTransaction) {
        final int codePoint = event.mCodePoint;
        final boolean isFromSuggestionStrip = event.isSuggestionStripPress();
        if (Constants.CODE_ENTER == codePoint &&
                SpaceState.SWAP_PUNCTUATION == inputTransaction.mSpaceState) {
            mConnection.removeTrailingSpace();
            return false;
        }
        if ((SpaceState.WEAK == inputTransaction.mSpaceState
                || SpaceState.SWAP_PUNCTUATION == inputTransaction.mSpaceState)
                && isFromSuggestionStrip) {
            if (inputTransaction.mSettingsValues.isUsuallyPrecededBySpace(codePoint)) {
                return false;
            }
            if (inputTransaction.mSettingsValues.isUsuallyFollowedBySpace(codePoint)) {
                return true;
            }
            mConnection.removeTrailingSpace();
        }
        return false;
    }

    public void startDoubleSpacePeriodCountdown(final InputTransaction inputTransaction) {
        mDoubleSpacePeriodCountdownStart = inputTransaction.mTimestamp;
    }

    public void cancelDoubleSpacePeriodCountdown() {
        mDoubleSpacePeriodCountdownStart = 0;
    }

    public boolean isDoubleSpacePeriodCountdownActive(final InputTransaction inputTransaction) {
        return inputTransaction.mTimestamp - mDoubleSpacePeriodCountdownStart
                < inputTransaction.mSettingsValues.mDoubleSpacePeriodTimeout;
    }

    /**
     * Apply the double-space-to-period transformation if applicable.
     *
     * The double-space-to-period transformation means that we replace two spaces with a
     * period-space sequence of characters. This typically happens when the user presses space
     * twice in a row quickly.
     * This method will check that the double-space-to-period is active in settings, that the
     * two spaces have been input close enough together, that the typed character is a space
     * and that the previous character allows for the transformation to take place. If all of
     * these conditions are fulfilled, this method applies the transformation and returns true.
     * Otherwise, it does nothing and returns false.
     *
     * @param event The event to handle.
     * @param inputTransaction The transaction in progress.
     * @return true if we applied the double-space-to-period transformation, false otherwise.
     */
    private boolean tryPerformDoubleSpacePeriod(final Event event,
            final InputTransaction inputTransaction) {
        // Check the setting, the typed character and the countdown. If any of the conditions is
        // not fulfilled, return false.
        if (!inputTransaction.mSettingsValues.mUseDoubleSpacePeriod
                || Constants.CODE_SPACE != event.mCodePoint
                || !isDoubleSpacePeriodCountdownActive(inputTransaction)) {
            return false;
        }
        // We only do this when we see one space and an accepted code point before the cursor.
        // The code point may be a surrogate pair but the space may not, so we need 3 chars.
        final CharSequence lastTwo = mConnection.getTextBeforeCursor(3, 0);
        if (null == lastTwo) return false;
        final int length = lastTwo.length();
        if (length < 2) return false;
        if (lastTwo.charAt(length - 1) != Constants.CODE_SPACE) return false;
        // We know there is a space in pos -1, and we have at least two chars. If we have only two
        // chars, isSurrogatePairs can't return true as charAt(1) is a space, so this is fine.
        final int firstCodePoint =
                Character.isSurrogatePair(lastTwo.charAt(0), lastTwo.charAt(1)) ?
                        Character.codePointAt(lastTwo, length - 3) : lastTwo.charAt(length - 2);
        if (canBeFollowedByDoubleSpacePeriod(firstCodePoint)) {
            cancelDoubleSpacePeriodCountdown();
            mConnection.deleteSurroundingText(1, 0);
            final String textToInsert = inputTransaction.mSettingsValues.mSpacingAndPunctuations
                    .mSentenceSeparatorAndSpace;
            mConnection.commitText(textToInsert, 1);
            inputTransaction.requireShiftUpdate(InputTransaction.SHIFT_UPDATE_NOW);
            inputTransaction.setRequiresUpdateSuggestions();
            return true;
        }
        return false;
    }

    /**
     * Returns whether this code point can be followed by the double-space-to-period transformation.
     *
     * See #maybeDoubleSpaceToPeriod for details.
     * Generally, most word characters can be followed by the double-space-to-period transformation,
     * while most punctuation can't. Some punctuation however does allow for this to take place
     * after them, like the closing parenthesis for example.
     *
     * @param codePoint the code point after which we may want to apply the transformation
     * @return whether it's fine to apply the transformation after this code point.
     */
    private static boolean canBeFollowedByDoubleSpacePeriod(final int codePoint) {
        // TODO: This should probably be a blacklist rather than a whitelist.
        // TODO: This should probably be language-dependant...
        return Character.isLetterOrDigit(codePoint)
                || codePoint == Constants.CODE_SINGLE_QUOTE
                || codePoint == Constants.CODE_DOUBLE_QUOTE
                || codePoint == Constants.CODE_CLOSING_PARENTHESIS
                || codePoint == Constants.CODE_CLOSING_SQUARE_BRACKET
                || codePoint == Constants.CODE_CLOSING_CURLY_BRACKET
                || codePoint == Constants.CODE_CLOSING_ANGLE_BRACKET
                || codePoint == Constants.CODE_PLUS
                || codePoint == Constants.CODE_PERCENT
                || Character.getType(codePoint) == Character.OTHER_SYMBOL;
    }

    /**
     * Performs a recapitalization event.
     * @param settingsValues The current settings values.
     */
    private void performRecapitalization(final SettingsValues settingsValues) {
        if (!mConnection.hasSelection() || !mRecapitalizeStatus.mIsEnabled()) {
            return; // No selection or recapitalize is disabled for now
        }
        final int selectionStart = mConnection.getExpectedSelectionStart();
        final int selectionEnd = mConnection.getExpectedSelectionEnd();
        final int numCharsSelected = selectionEnd - selectionStart;
        if (numCharsSelected > Constants.MAX_CHARACTERS_FOR_RECAPITALIZATION) {
            // We bail out if we have too many characters for performance reasons. We don't want
            // to suck possibly multiple-megabyte data.
            return;
        }
        // If we have a recapitalize in progress, use it; otherwise, start a new one.
        if (!mRecapitalizeStatus.isStarted()
                || !mRecapitalizeStatus.isSetAt(selectionStart, selectionEnd)) {
            final CharSequence selectedText =
                    mConnection.getSelectedText(0 /* flags, 0 for no styles */);
            if (TextUtils.isEmpty(selectedText)) return; // Race condition with the input connection
            mRecapitalizeStatus.start(selectionStart, selectionEnd, selectedText.toString(),
                    settingsValues.mLocale,
                    settingsValues.mSpacingAndPunctuations.mSortedWordSeparators);
            // We trim leading and trailing whitespace.
            mRecapitalizeStatus.trim();
        }
        mConnection.finishComposingText();
        mRecapitalizeStatus.rotate();
        mConnection.setSelection(selectionEnd, selectionEnd);
        mConnection.deleteSurroundingText(numCharsSelected, 0);
        mConnection.commitText(mRecapitalizeStatus.getRecapitalizedString(), 0);
        mConnection.setSelection(mRecapitalizeStatus.getNewCursorStart(),
                mRecapitalizeStatus.getNewCursorEnd());
    }

    private void performAdditionToUserHistoryDictionary(final SettingsValues settingsValues,
            final String suggestion, final PrevWordsInfo prevWordsInfo) {
        // If correction is not enabled, we don't add words to the user history dictionary.
        // That's to avoid unintended additions in some sensitive fields, or fields that
        // expect to receive non-words.
        if (!settingsValues.mAutoCorrectionEnabledPerUserSettings) return;

        if (TextUtils.isEmpty(suggestion)) return;
        final boolean wasAutoCapitalized =
                mWordComposer.wasAutoCapitalized() && !mWordComposer.isMostlyCaps();
        final int timeStampInSeconds = (int)TimeUnit.MILLISECONDS.toSeconds(
                System.currentTimeMillis());
        mDictionaryFacilitator.addToUserHistory(suggestion, wasAutoCapitalized,
                prevWordsInfo, timeStampInSeconds, settingsValues.mBlockPotentiallyOffensive);
    }

    public void performUpdateSuggestionStripSync(final SettingsValues settingsValues,
            final int inputStyle) {
        // Check if we have a suggestion engine attached.
        if (!settingsValues.needsToLookupSuggestions()) {
            if (mWordComposer.isComposingWord()) {
                Log.w(TAG, "Called updateSuggestionsOrPredictions but suggestions were not "
                        + "requested!");
            }
            // Clear the suggestions strip.
            mSuggestionStripViewAccessor.showSuggestionStrip(SuggestedWords.EMPTY);
            return;
        }

        if (!mWordComposer.isComposingWord() && !settingsValues.mBigramPredictionEnabled) {
            mSuggestionStripViewAccessor.setNeutralSuggestionStrip();
            return;
        }

        final AsyncResultHolder<SuggestedWords> holder = new AsyncResultHolder<>();
        mInputLogicHandler.getSuggestedWords(inputStyle, SuggestedWords.NOT_A_SEQUENCE_NUMBER,
                new OnGetSuggestedWordsCallback() {
                    @Override
                    public void onGetSuggestedWords(final SuggestedWords suggestedWords) {
                        final String typedWord = mWordComposer.getTypedWord();
                        // Show new suggestions if we have at least one. Otherwise keep the old
                        // suggestions with the new typed word. Exception: if the length of the
                        // typed word is <= 1 (after a deletion typically) we clear old suggestions.
                        if (suggestedWords.size() > 1 || typedWord.length() <= 1) {
                            holder.set(suggestedWords);
                        } else {
                            holder.set(retrieveOlderSuggestions(typedWord, mSuggestedWords));
                        }
                    }
                }
        );

        // This line may cause the current thread to wait.
        final SuggestedWords suggestedWords = holder.get(null,
                Constants.GET_SUGGESTED_WORDS_TIMEOUT);
        if (suggestedWords != null) {
            mSuggestionStripViewAccessor.showSuggestionStrip(suggestedWords);
        }
    }

    /**
     * Check if the cursor is touching a word. If so, restart suggestions on this word, else
     * do nothing.
     *
     * @param settingsValues the current values of the settings.
     * @param shouldIncludeResumedWordInSuggestions whether to include the word on which we resume
     *   suggestions in the suggestion list.
     */
    // TODO: make this private.
    public void restartSuggestionsOnWordTouchedByCursor(final SettingsValues settingsValues,
            final boolean shouldIncludeResumedWordInSuggestions,
            // TODO: remove this argument, put it into settingsValues
            final int currentKeyboardScriptId) {
        // HACK: We may want to special-case some apps that exhibit bad behavior in case of
        // recorrection. This is a temporary, stopgap measure that will be removed later.
        // TODO: remove this.
        if (settingsValues.isBrokenByRecorrection()
        // Recorrection is not supported in languages without spaces because we don't know
        // how to segment them yet.
                || !settingsValues.mSpacingAndPunctuations.mCurrentLanguageHasSpaces
        // If no suggestions are requested, don't try restarting suggestions.
                || !settingsValues.needsToLookupSuggestions()
        // If we are currently in a batch input, we must not resume suggestions, or the result
        // of the batch input will replace the new composition. This may happen in the corner case
        // that the app moves the cursor on its own accord during a batch input.
                || mInputLogicHandler.isInBatchInput()
        // If the cursor is not touching a word, or if there is a selection, return right away.
                || mConnection.hasSelection()
        // If we don't know the cursor location, return.
                || mConnection.getExpectedSelectionStart() < 0) {
            mSuggestionStripViewAccessor.setNeutralSuggestionStrip();
            return;
        }
        final int expectedCursorPosition = mConnection.getExpectedSelectionStart();
        if (!mConnection.isCursorTouchingWord(settingsValues.mSpacingAndPunctuations)) {
            // Show predictions.
            mWordComposer.setCapitalizedModeAtStartComposingTime(WordComposer.CAPS_MODE_OFF);
            mLatinIME.mHandler.postUpdateSuggestionStrip(SuggestedWords.INPUT_STYLE_RECORRECTION);
            return;
        }
        final TextRange range = mConnection.getWordRangeAtCursor(
                settingsValues.mSpacingAndPunctuations, currentKeyboardScriptId, isTransliteration);
        if (null == range) return; // Happens if we don't have an input connection at all
        if (range.length() <= 0) {
            // Race condition, or touching a word in a non-supported script.
            mLatinIME.setNeutralSuggestionStrip();
            return;
        }
        // If for some strange reason (editor bug or so) we measure the text before the cursor as
        // longer than what the entire text is supposed to be, the safe thing to do is bail out.
        if (range.mHasUrlSpans) return; // If there are links, we don't resume suggestions. Making
        // edits to a linkified text through batch commands would ruin the URL spans, and unless
        // we take very complicated steps to preserve the whole link, we can't do things right so
        // we just do not resume because it's safer.
        final int numberOfCharsInWordBeforeCursor = range.getNumberOfCharsInWordBeforeCursor();
        if (numberOfCharsInWordBeforeCursor > expectedCursorPosition) return;
        final ArrayList<SuggestedWordInfo> suggestions = new ArrayList<>();
        final String typedWord = range.mWord.toString();
        if (shouldIncludeResumedWordInSuggestions) {
            suggestions.add(new SuggestedWordInfo(typedWord,
                    SuggestedWords.MAX_SUGGESTIONS + 1,
                    SuggestedWordInfo.KIND_TYPED, Dictionary.DICTIONARY_USER_TYPED,
                    SuggestedWordInfo.NOT_AN_INDEX /* indexOfTouchPointOfSecondWord */,
                    SuggestedWordInfo.NOT_A_CONFIDENCE /* autoCommitFirstWordConfidence */));
        }
        if (!isResumableWord(settingsValues, typedWord)) {
            mSuggestionStripViewAccessor.setNeutralSuggestionStrip();
            return;
        }
        int i = 0;
        for (final SuggestionSpan span : range.getSuggestionSpansAtWord()) {
            for (final String s : span.getSuggestions()) {
                ++i;
                if (!TextUtils.equals(s, typedWord)) {
                    suggestions.add(new SuggestedWordInfo(s,
                            SuggestedWords.MAX_SUGGESTIONS - i,
                            SuggestedWordInfo.KIND_RESUMED, Dictionary.DICTIONARY_RESUMED,
                            SuggestedWordInfo.NOT_AN_INDEX /* indexOfTouchPointOfSecondWord */,
                            SuggestedWordInfo.NOT_A_CONFIDENCE
                                    /* autoCommitFirstWordConfidence */));
                }
            }
        }
        final int[] codePoints = StringUtils.toCodePointArray(typedWord);
        // We want the previous word for suggestion. If we have chars in the word
        // before the cursor, then we want the word before that, hence 2; otherwise,
        // we want the word immediately before the cursor, hence 1.
        final PrevWordsInfo prevWordsInfo = getPrevWordsInfoFromNthPreviousWordForSuggestion(
                settingsValues.mSpacingAndPunctuations,
                0 == numberOfCharsInWordBeforeCursor ? 1 : 2);
        mWordComposer.setComposingWord(codePoints,
                mLatinIME.getCoordinatesForCurrentKeyboard(codePoints));
        mWordComposer.setCursorPositionWithinWord(
                typedWord.codePointCount(0, numberOfCharsInWordBeforeCursor));
        mConnection.maybeMoveTheCursorAroundAndRestoreToWorkaroundABug();
        mConnection.setComposingRegion(expectedCursorPosition - numberOfCharsInWordBeforeCursor,
                expectedCursorPosition + range.getNumberOfCharsInWordAfterCursor());
        if (suggestions.size() <= (shouldIncludeResumedWordInSuggestions ? 1 : 0)) {
            // If there weren't any suggestion spans on this word, suggestions#size() will be 1
            // if shouldIncludeResumedWordInSuggestions is true, 0 otherwise. In this case, we
            // have no useful suggestions, so we will try to compute some for it instead.
            mInputLogicHandler.getSuggestedWords(Suggest.SESSION_ID_TYPING,
                    SuggestedWords.NOT_A_SEQUENCE_NUMBER, new OnGetSuggestedWordsCallback() {
                        @Override
                        public void onGetSuggestedWords(
                                final SuggestedWords suggestedWordsIncludingTypedWord) {
                            final SuggestedWords suggestedWords;
                            if (suggestedWordsIncludingTypedWord.size() > 1
                                    && !shouldIncludeResumedWordInSuggestions) {
                                // We were able to compute new suggestions for this word.
                                // Remove the typed word, since we don't want to display it in this
                                // case. The #getSuggestedWordsExcludingTypedWordForRecorrection()
                                // method sets willAutoCorrect to false.
                                suggestedWords = suggestedWordsIncludingTypedWord
                                        .getSuggestedWordsExcludingTypedWordForRecorrection();
                            } else {
                                // No saved suggestions, and we were unable to compute any good one
                                // either. Rather than displaying an empty suggestion strip, we'll
                                // display the original word alone in the middle.
                                // Since there is only one word, willAutoCorrect is false.
                                suggestedWords = suggestedWordsIncludingTypedWord;
                            }
                            mIsAutoCorrectionIndicatorOn = false;
                            mLatinIME.mHandler.showSuggestionStrip(suggestedWords);
                        }});
        } else {
            // We found suggestion spans in the word. We'll create the SuggestedWords out of
            // them, and make willAutoCorrect false. We make typedWordValid false, because the
            // color of the word in the suggestion strip changes according to this parameter,
            // and false gives the correct color.
            final SuggestedWords suggestedWords = new SuggestedWords(suggestions,
                    null /* rawSuggestions */, typedWord, false /* typedWordValid */,
                    false /* willAutoCorrect */, false /* isObsoleteSuggestions */,
                    SuggestedWords.INPUT_STYLE_RECORRECTION, SuggestedWords.NOT_A_SEQUENCE_NUMBER);
            mIsAutoCorrectionIndicatorOn = false;
            mLatinIME.mHandler.showSuggestionStrip(suggestedWords);
        }
    }

    /**
     * Reverts a previous commit with auto-correction.
     *
     * This is triggered upon pressing backspace just after a commit with auto-correction.
     *
     * @param inputTransaction The transaction in progress.
     * @param settingsValues the current values of the settings.
     */
    private void revertCommit(final InputTransaction inputTransaction,
            final SettingsValues settingsValues) {
        final CharSequence originallyTypedWord = mLastComposedWord.mTypedWord;
        final String originallyTypedWordString =
                originallyTypedWord != null ? originallyTypedWord.toString() : "";
        final CharSequence committedWord = mLastComposedWord.mCommittedWord;
        final String committedWordString = committedWord.toString();
        final int cancelLength = committedWord.length();
        final String separatorString = mLastComposedWord.mSeparatorString;
        // We want java chars, not codepoints for the following.
        final int separatorLength = separatorString.length();
        // TODO: should we check our saved separator against the actual contents of the text view?
        final int deleteLength = cancelLength + separatorLength;
        if (DebugFlags.DEBUG_ENABLED) {
            if (mWordComposer.isComposingWord()) {
                throw new RuntimeException("revertCommit, but we are composing a word");
            }
            final CharSequence wordBeforeCursor =
                    mConnection.getTextBeforeCursor(deleteLength, 0).subSequence(0, cancelLength);
            if (!TextUtils.equals(committedWord, wordBeforeCursor)) {
                throw new RuntimeException("revertCommit check failed: we thought we were "
                        + "reverting \"" + committedWord
                        + "\", but before the cursor we found \"" + wordBeforeCursor + '"');
            }
        }
        mConnection.deleteSurroundingText(deleteLength, 0);
        if (!TextUtils.isEmpty(committedWord)) {
            mDictionaryFacilitator.removeWordFromPersonalizedDicts(committedWordString);
        }
        final String stringToCommit = originallyTypedWord + separatorString;
        final SpannableString textToCommit = new SpannableString(stringToCommit);
        if (committedWord instanceof SpannableString) {
            final SpannableString committedWordWithSuggestionSpans = (SpannableString)committedWord;
            final Object[] spans = committedWordWithSuggestionSpans.getSpans(0,
                    committedWord.length(), Object.class);
            final int lastCharIndex = textToCommit.length() - 1;
            // We will collect all suggestions in the following array.
            final ArrayList<String> suggestions = new ArrayList<>();
            // First, add the committed word to the list of suggestions.
            suggestions.add(committedWordString);
            for (final Object span : spans) {
                // If this is a suggestion span, we check that the locale is the right one, and
                // that the word is not the committed word. That should mostly be the case.
                // Given this, we add it to the list of suggestions, otherwise we discard it.
                if (span instanceof SuggestionSpan) {
                    final SuggestionSpan suggestionSpan = (SuggestionSpan)span;
                    if (!suggestionSpan.getLocale().equals(
                            inputTransaction.mSettingsValues.mLocale.toString())) {
                        continue;
                    }
                    for (final String suggestion : suggestionSpan.getSuggestions()) {
                        if (!suggestion.equals(committedWordString)) {
                            suggestions.add(suggestion);
                        }
                    }
                } else {
                    // If this is not a suggestion span, we just add it as is.
                    textToCommit.setSpan(span, 0 /* start */, lastCharIndex /* end */,
                            committedWordWithSuggestionSpans.getSpanFlags(span));
                }
            }
            // Add the suggestion list to the list of suggestions.
            textToCommit.setSpan(new SuggestionSpan(inputTransaction.mSettingsValues.mLocale,
                    suggestions.toArray(new String[suggestions.size()]), 0 /* flags */),
                    0 /* start */, lastCharIndex /* end */, 0 /* flags */);
        }

        final boolean shouldShowAddToDictionaryForTypedWord =
                shouldShowAddToDictionaryForTypedWord(mLastComposedWord, settingsValues);

        if (inputTransaction.mSettingsValues.mSpacingAndPunctuations.mCurrentLanguageHasSpaces) {
            // For languages with spaces, we revert to the typed string, but the cursor is still
            // after the separator so we don't resume suggestions. If the user wants to correct
            // the word, they have to press backspace again.
            if (shouldShowAddToDictionaryForTypedWord) {
                mConnection.commitTextWithBackgroundColor(textToCommit, 1,
                        settingsValues.mTextHighlightColorForAddToDictionaryIndicator,
                        originallyTypedWordString.length());
            } else {
                mConnection.commitText(textToCommit, 1);
            }
        } else {
            // For languages without spaces, we revert the typed string but the cursor is flush
            // with the typed word, so we need to resume suggestions right away.
            final int[] codePoints = StringUtils.toCodePointArray(stringToCommit);
            mWordComposer.setComposingWord(codePoints,
                    mLatinIME.getCoordinatesForCurrentKeyboard(codePoints));
            if (shouldShowAddToDictionaryForTypedWord) {
                setComposingTextInternalWithBackgroundColor(textToCommit, 1,
                        settingsValues.mTextHighlightColorForAddToDictionaryIndicator,
                        originallyTypedWordString.length());
            } else {
                setComposingTextInternal(textToCommit, 1);
            }
        }
        // Don't restart suggestion yet. We'll restart if the user deletes the separator.
        mLastComposedWord = LastComposedWord.NOT_A_COMPOSED_WORD;

        if (shouldShowAddToDictionaryForTypedWord) {
            // Due to the API limitation as of L, we cannot reliably retrieve the reverted text
            // when the separator causes line breaking. Until this API limitation is addressed in
            // the framework, show the indicator only when the separator doesn't contain
            // line-breaking characters.
            if (!StringUtils.hasLineBreakCharacter(separatorString)) {
                mTextDecorator.showAddToDictionaryIndicator(originallyTypedWordString,
                        mConnection.getExpectedSelectionStart(),
                        mConnection.getExpectedSelectionEnd());
            }
            mSuggestionStripViewAccessor.showAddToDictionaryHint(originallyTypedWordString);
        } else {
            // We have a separator between the word and the cursor: we should show predictions.
            inputTransaction.setRequiresUpdateSuggestions();
        }
    }

    /**
     * Factor in auto-caps and manual caps and compute the current caps mode.
     * @param settingsValues the current settings values.
     * @param keyboardShiftMode the current shift mode of the keyboard. See
     *   KeyboardSwitcher#getKeyboardShiftMode() for possible values.
     * @return the actual caps mode the keyboard is in right now.
     */
    private int getActualCapsMode(final SettingsValues settingsValues,
            final int keyboardShiftMode) {
        if (keyboardShiftMode != WordComposer.CAPS_MODE_AUTO_SHIFTED) {
            return keyboardShiftMode;
        }
        final int auto = getCurrentAutoCapsState(settingsValues);
        if (0 != (auto & TextUtils.CAP_MODE_CHARACTERS)) {
            return WordComposer.CAPS_MODE_AUTO_SHIFT_LOCKED;
        }
        if (0 != auto) {
            return WordComposer.CAPS_MODE_AUTO_SHIFTED;
        }
        return WordComposer.CAPS_MODE_OFF;
    }

    /**
     * Gets the current auto-caps state, factoring in the space state.
     *
     * This method tries its best to do this in the most efficient possible manner. It avoids
     * getting text from the editor if possible at all.
     * This is called from the KeyboardSwitcher (through a trampoline in LatinIME) because it
     * needs to know auto caps state to display the right layout.
     *
     * @param settingsValues the relevant settings values
     * @return a caps mode from TextUtils.CAP_MODE_* or Constants.TextUtils.CAP_MODE_OFF.
     */
    public int getCurrentAutoCapsState(final SettingsValues settingsValues) {
        if (!settingsValues.mAutoCap || isIndic) return Constants.TextUtils.CAP_MODE_OFF;

        final EditorInfo ei = getCurrentInputEditorInfo();
        if (ei == null) return Constants.TextUtils.CAP_MODE_OFF;
        final int inputType = ei.inputType;
        // Warning: this depends on mSpaceState, which may not be the most current value. If
        // mSpaceState gets updated later, whoever called this may need to be told about it.
        return mConnection.getCursorCapsMode(inputType, settingsValues.mSpacingAndPunctuations,
                SpaceState.PHANTOM == mSpaceState);
    }

    public int getCurrentRecapitalizeState() {
        if (!mRecapitalizeStatus.isStarted()
                || !mRecapitalizeStatus.isSetAt(mConnection.getExpectedSelectionStart(),
                        mConnection.getExpectedSelectionEnd())) {
            // Not recapitalizing at the moment
            return RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE;
        }
        return mRecapitalizeStatus.getCurrentMode();
    }

    /**
     * @return the editor info for the current editor
     */
    private EditorInfo getCurrentInputEditorInfo() {
        return mLatinIME.getCurrentInputEditorInfo();
    }

    /**
     * Get information fo previous words from the nth previous word before the cursor as context
     * for the suggestion process.
     * @param spacingAndPunctuations the current spacing and punctuations settings.
     * @param nthPreviousWord reverse index of the word to get (1-indexed)
     * @return the information of previous words
     */
    // TODO: Make this private
    public PrevWordsInfo getPrevWordsInfoFromNthPreviousWordForSuggestion(
            final SpacingAndPunctuations spacingAndPunctuations, final int nthPreviousWord) {
        if (spacingAndPunctuations.mCurrentLanguageHasSpaces) {
            // If we are typing in a language with spaces we can just look up the previous
            // word information from textview.
            return mConnection.getPrevWordsInfoFromNthPreviousWord(
                    spacingAndPunctuations, nthPreviousWord);
        } else {
            return LastComposedWord.NOT_A_COMPOSED_WORD == mLastComposedWord ?
                    PrevWordsInfo.BEGINNING_OF_SENTENCE :
                            new PrevWordsInfo(new PrevWordsInfo.WordInfo(
                                    mLastComposedWord.mCommittedWord.toString()));
        }
    }

    /**
     * Tests the passed word for resumability.
     *
     * We can resume suggestions on words whose first code point is a word code point (with some
     * nuances: check the code for details).
     *
     * @param settings the current values of the settings.
     * @param word the word to evaluate.
     * @return whether it's fine to resume suggestions on this word.
     */
    private static boolean isResumableWord(final SettingsValues settings, final String word) {
        final int firstCodePoint = word.codePointAt(0);
        return settings.isWordCodePoint(firstCodePoint)
                && Constants.CODE_SINGLE_QUOTE != firstCodePoint
                && Constants.CODE_DASH != firstCodePoint;
    }

    /**
     * @param actionId the action to perform
     */
    private void performEditorAction(final int actionId) {
        mConnection.performEditorAction(actionId);
    }

    /**
     * Perform the processing specific to inputting TLDs.
     *
     * Some keys input a TLD (specifically, the ".com" key) and this warrants some specific
     * processing. First, if this is a TLD, we ignore PHANTOM spaces -- this is done by type
     * of character in onCodeInput, but since this gets inputted as a whole string we need to
     * do it here specifically. Then, if the last character before the cursor is a period, then
     * we cut the dot at the start of ".com". This is because humans tend to type "www.google."
     * and then press the ".com" key and instinctively don't expect to get "www.google..com".
     *
     * @param text the raw text supplied to onTextInput
     * @return the text to actually send to the editor
     */
    private String performSpecificTldProcessingOnTextInput(final String text) {
        if (text.length() <= 1 || text.charAt(0) != Constants.CODE_PERIOD
                || !Character.isLetter(text.charAt(1))) {
            // Not a tld: do nothing.
            return text;
        }
        // We have a TLD (or something that looks like this): make sure we don't add
        // a space even if currently in phantom mode.
        mSpaceState = SpaceState.NONE;
        final int codePointBeforeCursor = mConnection.getCodePointBeforeCursor();
        // If no code point, #getCodePointBeforeCursor returns NOT_A_CODE_POINT.
        if (Constants.CODE_PERIOD == codePointBeforeCursor) {
            return text.substring(1);
        } else {
            return text;
        }
    }

    /**
     * Handle a press on the settings key.
     */
    private void onSettingsKeyPressed() {
        mLatinIME.displaySettingsDialog();
    }

    /**
     * Resets the whole input state to the starting state.
     *
     * This will clear the composing word, reset the last composed word, clear the suggestion
     * strip and tell the input connection about it so that it can refresh its caches.
     *
     * @param newSelStart the new selection start, in java characters.
     * @param newSelEnd the new selection end, in java characters.
     * @param clearSuggestionStrip whether this method should clear the suggestion strip.
     */
    // TODO: how is this different from startInput ?!
    private void resetEntireInputState(final int newSelStart, final int newSelEnd,
            final boolean clearSuggestionStrip) {
        final boolean shouldFinishComposition = mWordComposer.isComposingWord();
        resetComposingState(true /* alsoResetLastComposedWord */);
        if (clearSuggestionStrip) {
            mSuggestionStripViewAccessor.setNeutralSuggestionStrip();
        }
        mConnection.resetCachesUponCursorMoveAndReturnSuccess(newSelStart, newSelEnd,
                shouldFinishComposition);
    }

    /**
     * Resets only the composing state.
     *
     * Compare #resetEntireInputState, which also clears the suggestion strip and resets the
     * input connection caches. This only deals with the composing state.
     *
     * @param alsoResetLastComposedWord whether to also reset the last composed word.
     */
    private void resetComposingState(final boolean alsoResetLastComposedWord) {
        mWordComposer.reset();
        if (alsoResetLastComposedWord) {
            mLastComposedWord = LastComposedWord.NOT_A_COMPOSED_WORD;
        }
    }

    /**
     * Make a {@link io.separ.neural.inputmethod.indic.SuggestedWords} object containing a typed word
     * and obsolete suggestions.
     * See {@link io.separ.neural.inputmethod.indic.SuggestedWords#getTypedWordAndPreviousSuggestions(
     *      String, io.separ.neural.inputmethod.indic.SuggestedWords)}.
     * @param typedWord The typed word as a string.
     * @param previousSuggestedWords The previously suggested words.
     * @return Obsolete suggestions with the newly typed word.
     */
    private static SuggestedWords retrieveOlderSuggestions(final String typedWord,
                                                           final SuggestedWords previousSuggestedWords) {
        final SuggestedWords oldSuggestedWords =
                previousSuggestedWords.isPunctuationSuggestions() ? SuggestedWords.EMPTY
                        : previousSuggestedWords;
        final ArrayList<SuggestedWords.SuggestedWordInfo> typedWordAndPreviousSuggestions =
                SuggestedWords.getTypedWordAndPreviousSuggestions(typedWord, oldSuggestedWords);
        return new SuggestedWords(typedWordAndPreviousSuggestions, null /* rawSuggestions */,
                false /* typedWordValid */, false /* hasAutoCorrectionCandidate */,
                true /* isObsoleteSuggestions */, oldSuggestedWords.mInputStyle);
    }

    /**
     * Gets a chunk of text with or the auto-correction indicator underline span as appropriate.
     *
     * This method looks at the old state of the auto-correction indicator to put or not put
     * the underline span as appropriate. It is important to note that this does not correspond
     * exactly to whether this word will be auto-corrected to or not: what's important here is
     * to keep the same indication as before.
     * When we add a new code point to a composing word, we don't know yet if we are going to
     * auto-correct it until the suggestions are computed. But in the mean time, we still need
     * to display the character and to extend the previous underline. To avoid any flickering,
     * the underline should keep the same color it used to have, even if that's not ultimately
     * the correct color for this new word. When the suggestions are finished evaluating, we
     * will call this method again to fix the color of the underline.
     *
     * @param text the text on which to maybe apply the span.
     * @return the same text, with the auto-correction underline span if that's appropriate.
     */
    // TODO: Shouldn't this go in some *Utils class instead?
    private CharSequence getTextWithUnderline(final String text) {
        return mIsAutoCorrectionIndicatorOn
                ? SuggestionSpanUtils.getTextWithAutoCorrectionIndicatorUnderline(mLatinIME, text)
                : text;
    }

    /**
     * Sends a DOWN key event followed by an UP key event to the editor.
     *
     * If possible at all, avoid using this method. It causes all sorts of race conditions with
     * the text view because it goes through a different, asynchronous binder. Also, batch edits
     * are ignored for key events. Use the normal software input methods instead.
     *
     * @param keyCode the key code to send inside the key event.
     */
    private void sendDownUpKeyEvent(final int keyCode) {
        final long eventTime = SystemClock.uptimeMillis();
        mConnection.sendKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
        mConnection.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
    }

    /**
     * Sends a code point to the editor, using the most appropriate method.
     *
     * Normally we send code points with commitText, but there are some cases (where backward
     * compatibility is a concern for example) where we want to use deprecated methods.
     *
     * @param settingsValues the current values of the settings.
     * @param codePoint the code point to send.
     */
    // TODO: replace these two parameters with an InputTransaction
    private void sendKeyCodePoint(final SettingsValues settingsValues, final int codePoint, boolean transliteration) {
        // TODO: Remove this special handling of digit letters.
        // For backward compatibility. See {@link InputMethodService#sendKeyChar(char)}.
        if (codePoint >= '0' && codePoint <= '9') {
            sendDownUpKeyEvent(codePoint - '0' + KeyEvent.KEYCODE_0);
            return;
        }

        // TODO: we should do this also when the editor has TYPE_NULL
        if (Constants.CODE_ENTER == codePoint && settingsValues.isBeforeJellyBean()) {
            // Backward compatibility mode. Before Jelly bean, the keyboard would simulate
            // a hardware keyboard event on pressing enter or delete. This is bad for many
            // reasons (there are race conditions with commits) but some applications are
            // relying on this behavior so we continue to support it for older apps.
            sendDownUpKeyEvent(KeyEvent.KEYCODE_ENTER);
        } else {
            mConnection.commitText(StringUtils.newSingleCodePointString(codePoint), 1);
        }
    }

    /**
     * Promote a phantom space to an actual space.
     *
     * This essentially inserts a space, and that's it. It just checks the options and the text
     * before the cursor are appropriate before doing it.
     *
     * @param settingsValues the current values of the settings.
     */
    private void promotePhantomSpace(final SettingsValues settingsValues) {
        if (settingsValues.shouldInsertSpacesAutomatically()
                && settingsValues.mSpacingAndPunctuations.mCurrentLanguageHasSpaces
                && !mConnection.textBeforeCursorLooksLikeURL()) {
            sendKeyCodePoint(settingsValues, Constants.CODE_SPACE, false);
        }
    }

    /**
     * Do the final processing after a batch input has ended. This commits the word to the editor.
     * @param settingsValues the current values of the settings.
     * @param suggestedWords suggestedWords to use.
     */
    public void onUpdateTailBatchInputCompleted(final SettingsValues settingsValues,
            final SuggestedWords suggestedWords,
            // TODO: remove this argument
            final KeyboardSwitcher keyboardSwitcher) {
        final String batchInputText = suggestedWords.isEmpty() ? null : suggestedWords.getWord(0);
        if (TextUtils.isEmpty(batchInputText)) {
            return;
        }
        Log.e("SEPAR", "onUpdateTailBatchInputCompleted: "+batchInputText);
        mConnection.beginBatchEdit();
        if (SpaceState.PHANTOM == mSpaceState) {
            promotePhantomSpace(settingsValues);
        }
        final SuggestedWordInfo autoCommitCandidate = mSuggestedWords.getAutoCommitCandidate();
        // Commit except the last word for phrase gesture if the top suggestion is eligible for auto
        // commit.
        if (settingsValues.mPhraseGestureEnabled && null != autoCommitCandidate) {
            // Find the last space
            final int indexOfLastSpace = batchInputText.lastIndexOf(Constants.CODE_SPACE) + 1;
            if (0 != indexOfLastSpace) {
                mConnection.commitText(batchInputText.substring(0, indexOfLastSpace), 1);
                final SuggestedWords suggestedWordsForLastWordOfPhraseGesture =
                        suggestedWords.getSuggestedWordsForLastWordOfPhraseGesture();
                mLatinIME.showSuggestionStrip(suggestedWordsForLastWordOfPhraseGesture);
            }
            final String lastWord = batchInputText.substring(indexOfLastSpace);
            mWordComposer.setBatchInputWord(lastWord);
            setComposingTextInternal(lastWord, 1);
        } else {
            mWordComposer.setBatchInputWord(batchInputText);
            setComposingTextInternal(batchInputText, 1);
        }
        mConnection.endBatchEdit();
        // Space state must be updated before calling updateShiftState
        mSpaceState = SpaceState.PHANTOM;
        keyboardSwitcher.requestUpdatingShiftState(getCurrentAutoCapsState(settingsValues),
                getCurrentRecapitalizeState());
    }

    /**
     * Commit the typed string to the editor.
     *
     * This is typically called when we should commit the currently composing word without applying
     * auto-correction to it. Typically, we come here upon pressing a separator when the keyboard
     * is configured to not do auto-correction at all (because of the settings or the properties of
     * the editor). In this case, `separatorString' is set to the separator that was pressed.
     * We also come here in a variety of cases with external user action. For example, when the
     * cursor is moved while there is a composition, or when the keyboard is closed, or when the
     * user presses the Send button for an SMS, we don't auto-correct as that would be unexpected.
     * In this case, `separatorString' is set to NOT_A_SEPARATOR.
     *
     * @param settingsValues the current values of the settings.
     * @param separatorString the separator that's causing the commit, or NOT_A_SEPARATOR if none.
     */
    // TODO: Make this private
    public void commitTyped(final SettingsValues settingsValues, final String separatorString) {
        if (!mWordComposer.isComposingWord()) return;
        final String typedWord = mWordComposer.getTypedWord();
        if (typedWord.length() > 0) {
            commitChosenWord(settingsValues, typedWord,
                    LastComposedWord.COMMIT_TYPE_USER_TYPED_WORD, separatorString);
        }
    }

    /**
     * Commit the current auto-correction.
     *
     * This will commit the best guess of the keyboard regarding what the user meant by typing
     * the currently composing word. The IME computes suggestions and assigns a confidence score
     * to each of them; when it's confident enough in one suggestion, it replaces the typed string
     * by this suggestion at commit time. When it's not confident enough, or when it has no
     * suggestions, or when the settings or environment does not allow for auto-correction, then
     * this method just commits the typed string.
     * Note that if suggestions are currently being computed in the background, this method will
     * block until the computation returns. This is necessary for consistency (it would be very
     * strange if pressing space would commit a different word depending on how fast you press).
     *
     * @param settingsValues the current value of the settings.
     * @param separator the separator that's causing the commit to happen.
     */
    private void commitCurrentAutoCorrection(final SettingsValues settingsValues,
            final String separator,
            // TODO: Remove this argument.
            final LatinIME.UIHandler handler) {
        // Complete any pending suggestions query first
        if (handler.hasPendingUpdateSuggestions()) {
            handler.cancelUpdateSuggestionStrip();
            // To know the input style here, we should retrieve the in-flight "update suggestions"
            // message and read its arg1 member here. However, the Handler class does not let
            // us retrieve this message, so we can't do that. But in fact, we notice that
            // we only ever come here when the input style was typing. In the case of batch
            // input, we update the suggestions synchronously when the tail batch comes. Likewise
            // for application-specified completions. As for recorrections, we never auto-correct,
            // so we don't come here either. Hence, the input style is necessarily
            // INPUT_STYLE_TYPING.
            performUpdateSuggestionStripSync(settingsValues, SuggestedWords.INPUT_STYLE_TYPING);
        }
        final String typedAutoCorrection = mWordComposer.getAutoCorrectionOrNull();
        final String typedWord = mWordComposer.getTypedWord();
        final String autoCorrection = (typedAutoCorrection != null)
                ? typedAutoCorrection : typedWord;
        if (autoCorrection != null) {
            if (TextUtils.isEmpty(typedWord)) {
                throw new RuntimeException("We have an auto-correction but the typed word "
                        + "is empty? Impossible! I must commit suicide.");
            }
            commitChosenWord(settingsValues, autoCorrection,
                    LastComposedWord.COMMIT_TYPE_DECIDED_WORD, separator);
            if (!typedWord.equals(autoCorrection)) {
                // This will make the correction flash for a short while as a visual clue
                // to the user that auto-correction happened. It has no other effect; in particular
                // note that this won't affect the text inside the text field AT ALL: it only makes
                // the segment of text starting at the supplied index and running for the length
                // of the auto-correction flash. At this moment, the "typedWord" argument is
                // ignored by TextView.
                mConnection.commitCorrection(new CorrectionInfo(
                        mConnection.getExpectedSelectionEnd() - autoCorrection.length(),
                        typedWord, autoCorrection));
            }
        }
    }

    /**
     * Commits the chosen word to the text field and saves it for later retrieval.
     *
     * @param settingsValues the current values of the settings.
     * @param chosenWord the word we want to commit.
     * @param commitType the type of the commit, as one of LastComposedWord.COMMIT_TYPE_*
     * @param separatorString the separator that's causing the commit, or NOT_A_SEPARATOR if none.
     */
    private void commitChosenWord(final SettingsValues settingsValues, final String chosenWord,
            final int commitType, final String separatorString) {
        final SuggestedWords suggestedWords = mSuggestedWords;
        final CharSequence chosenWordWithSuggestions =
                SuggestionSpanUtils.getTextWithSuggestionSpan(mLatinIME, chosenWord,
                        suggestedWords);
        // When we are composing word, get previous words information from the 2nd previous word
        // because the 1st previous word is the word to be committed. Otherwise get previous words
        // information from the 1st previous word.
        final PrevWordsInfo prevWordsInfo = mConnection.getPrevWordsInfoFromNthPreviousWord(
                settingsValues.mSpacingAndPunctuations, mWordComposer.isComposingWord() ? 2 : 1);
        mConnection.commitText(chosenWordWithSuggestions, 1);
        // Add the word to the user history dictionary
        performAdditionToUserHistoryDictionary(settingsValues, chosenWord, prevWordsInfo);
        // TODO: figure out here if this is an auto-correct or if the best word is actually
        // what user typed. Note: currently this is done much later in
        // LastComposedWord#didCommitTypedWord by string equality of the remembered
        // strings.
        mLastComposedWord = mWordComposer.commitWord(commitType,
                chosenWordWithSuggestions, separatorString, prevWordsInfo);
    }

    /**
     * Retry resetting caches in the rich input connection.
     *
     * When the editor can't be accessed we can't reset the caches, so we schedule a retry.
     * This method handles the retry, and re-schedules a new retry if we still can't access.
     * We only retry up to 5 times before giving up.
     *
     * @param tryResumeSuggestions Whether we should resume suggestions or not.
     * @param remainingTries How many times we may try again before giving up.
     * @return whether true if the caches were successfully reset, false otherwise.
     */
    // TODO: make this private
    public boolean retryResetCachesAndReturnSuccess(final boolean tryResumeSuggestions,
            final int remainingTries,
            // TODO: remove these arguments
            final LatinIME.UIHandler handler) {
        final boolean shouldFinishComposition = mConnection.hasSelection()
                || !mConnection.isCursorPositionKnown();
        if (!mConnection.resetCachesUponCursorMoveAndReturnSuccess(
                mConnection.getExpectedSelectionStart(), mConnection.getExpectedSelectionEnd(),
                shouldFinishComposition)) {
            if (0 < remainingTries) {
                handler.postResetCaches(tryResumeSuggestions, remainingTries - 1);
                return false;
            }
            // If remainingTries is 0, we should stop waiting for new tries, however we'll still
            // return true as we need to perform other tasks (for example, loading the keyboard).
        }
        mConnection.tryFixLyingCursorPosition();
        if (tryResumeSuggestions) {
            // This is triggered when starting input anew, so we want to include the resumed
            // word in suggestions.
            handler.postResumeSuggestions(true /* shouldIncludeResumedWordInSuggestions */,
                    true /* shouldDelay */);
        }
        return true;
    }

    public void getSuggestedWords(final SettingsValues settingsValues,
            final ProximityInfo proximityInfo, final int keyboardShiftMode, final int inputStyle,
            final int sequenceNumber, final OnGetSuggestedWordsCallback callback) {
        mWordComposer.adviseCapitalizedModeBeforeFetchingSuggestions(
                getActualCapsMode(settingsValues, keyboardShiftMode));
        mSuggest.getSuggestedWords(mWordComposer,
                getPrevWordsInfoFromNthPreviousWordForSuggestion(
                        settingsValues.mSpacingAndPunctuations,
                        // Get the word on which we should search the bigrams. If we are composing
                        // a word, it's whatever is *before* the half-committed word in the buffer,
                        // hence 2; if we aren't, we should just skip whitespace if any, so 1.
                        mWordComposer.isComposingWord() ? 2 : 1),
                proximityInfo,
                new SettingsValuesForSuggestion(settingsValues.mBlockPotentiallyOffensive,
                        settingsValues.mPhraseGestureEnabled,
                        settingsValues.mAdditionalFeaturesSettingValues),
                settingsValues.mAutoCorrectionEnabledPerUserSettings,
                inputStyle, sequenceNumber, callback);
    }

    /**
     * Used as an injection point for each call of
     * {@link RichInputConnection#setComposingText(CharSequence, int)}.
     *
     * <p>Currently using this method is optional and you can still directly call
     * {@link RichInputConnection#setComposingText(CharSequence, int)}, but it is recommended to
     * use this method whenever possible to optimize the behavior of {@link TextDecorator}.<p>
     * <p>TODO: Should we move this mechanism to {@link RichInputConnection}?</p>
     *
     * @param newComposingText the composing text to be set
     * @param newCursorPosition the new cursor position
     */
    private void setComposingTextInternal(final CharSequence newComposingText,
            final int newCursorPosition) {
        setComposingTextInternalWithBackgroundColor(newComposingText, newCursorPosition,
                Color.TRANSPARENT, newComposingText.length());
    }

    /**
     * Equivalent to {@link #setComposingTextInternal(CharSequence, int)} except that this method
     * allows to set {@link BackgroundColorSpan} to the composing text with the given color.
     *
     * <p>TODO: Currently the background color is exclusive with the black underline, which is
     * automatically added by the framework. We need to change the framework if we need to have both
     * of them at the same time.</p>
     * <p>TODO: Should we move this method to {@link RichInputConnection}?</p>
     *
     * @param newComposingText the composing text to be set
     * @param newCursorPosition the new cursor position
     * @param backgroundColor the background color to be set to the composing text. Set
     * {@link Color#TRANSPARENT} to disable the background color.
     * @param coloredTextLength the length of text, in Java chars, which should be rendered with
     * the given background color.
     */
    private void setComposingTextInternalWithBackgroundColor(final CharSequence newComposingText,
            final int newCursorPosition, final int backgroundColor, final int coloredTextLength) {
        final CharSequence composingTextToBeSet;
        if (backgroundColor == Color.TRANSPARENT) {
            composingTextToBeSet = newComposingText;
        } else {
            final SpannableString spannable = new SpannableString(newComposingText);
            final BackgroundColorSpan backgroundColorSpan =
                    new BackgroundColorSpan(backgroundColor);
            final int spanLength = Math.min(coloredTextLength, spannable.length());
            spannable.setSpan(backgroundColorSpan, 0, spanLength,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
            composingTextToBeSet = spannable;
        }
        mConnection.setComposingText(composingTextToBeSet, newCursorPosition);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Following methods are tentatively placed in this class for the integration with
    // TextDecorator.
    // TODO: Decouple things that are not related to the input logic.
    //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sets the UI operator for {@link TextDecorator}.
     * @param uiOperator the UI operator which should be associated with {@link TextDecorator}.
     */
    public void setTextDecoratorUi(final TextDecoratorUiOperator uiOperator) {
        mTextDecorator.setUiOperator(uiOperator);
    }

    /**
     * Must be called from {@link InputMethodService#onUpdateCursorAnchorInfo(CursorAnchorInfo)} is
     * called.
     * @param info The wrapper object with which we can access cursor/anchor info.
     */
    public void onUpdateCursorAnchorInfo(final CursorAnchorInfoCompatWrapper info) {
        mTextDecorator.onUpdateCursorAnchorInfo(info);
    }

    /**
     * Must be called when {@link InputMethodService#updateFullscreenMode} is called.
     * @param isFullscreen {@code true} if the input method is in full-screen mode.
     */
    public void onUpdateFullscreenMode(final boolean isFullscreen) {
        mTextDecorator.notifyFullScreenMode(isFullscreen);
    }

    /**
     * Must be called from {@link LatinIME#addWordToUserDictionary(String)}.
     */
    public void onAddWordToUserDictionary() {
        mConnection.removeBackgroundColorFromHighlightedTextIfNecessary();
        mTextDecorator.reset();
    }

    /**
     * Returns whether the add to dictionary indicator should be shown or not.
     * @param lastComposedWord the last composed word information.
     * @param settingsValues the current settings value.
     * @return {@code true} if the commit indicator should be shown.
     */
    private boolean shouldShowAddToDictionaryForTypedWord(final LastComposedWord lastComposedWord,
            final SettingsValues settingsValues) {
        if (!mConnection.isCursorAnchorInfoMonitorEnabled()) {
            // We cannot help in this case because we are heavily relying on this new API.
            return false;
        }
        if (!settingsValues.mShouldShowUiToAcceptTypedWord) {
            return false;
        }
        return !TextUtils.isEmpty(lastComposedWord.mTypedWord) && !TextUtils.equals(lastComposedWord.mTypedWord, lastComposedWord.mCommittedWord) && mDictionaryFacilitator.isUserDictionaryEnabled() && !mDictionaryFacilitator.isValidWord(lastComposedWord.mTypedWord, true /* ignoreCase */);
    }

    public void setIndic(boolean flag) {
        isIndic = flag;
    }
}

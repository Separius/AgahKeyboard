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

package com.android.inputmethod.keyboard.internal;

import android.text.TextUtils;
import android.util.Log;

import com.android.inputmethod.latin.utils.RecapitalizeStatus;

import io.separ.neural.inputmethod.Utils.FontUtils;
import io.separ.neural.inputmethod.indic.Constants;

/**
 * Keyboard state machine.
 *
 * This class contains all keyboard state transition logic.
 *
 * The input events are {@link #onLoadKeyboard(int, int)}, {@link #onSaveKeyboardState()},
 * {@link #onPressKey(int,boolean,int,int)}, {@link #onReleaseKey(int,boolean,int,int)},
 * {@link #onCodeInput(int,int,int)}, {@link #onFinishSlidingInput(int,int)},
 * {@link #onUpdateShiftState(int,int)}, {@link #onResetKeyboardStateToAlphabet(int,int)}.
 *
 * The actions are {@link SwitchActions}'s methods.
 */
public final class KeyboardState {
    private static final String TAG = KeyboardState.class.getSimpleName();
    private static final boolean DEBUG_EVENT = false;
    private static final boolean DEBUG_ACTION = false;

    public interface SwitchActions {
        void setAlphabetKeyboard();
        void setAlphabetManualShiftedKeyboard();
        void setAlphabetAutomaticShiftedKeyboard();
        void setAlphabetShiftLockedKeyboard();
        void setAlphabetShiftLockShiftedKeyboard();
        void setEmojiKeyboard();
        void setSymbolsKeyboard();
        void setSymbolsShiftedKeyboard();

        /**
         * Request to call back {@link KeyboardState#onUpdateShiftState(int, int)}.
         */
        void requestUpdatingShiftState(final int currentAutoCapsState,
                                       final int currentRecapitalizeState);

        void startDoubleTapShiftKeyTimer();
        boolean isInDoubleTapShiftKeyTimeout();
        void cancelDoubleTapShiftKeyTimer();
    }

    private final SwitchActions mSwitchActions;

    private final ShiftKeyState mShiftKeyState = new ShiftKeyState("Shift");
    private final ModifierKeyState mSymbolKeyState = new ModifierKeyState("Symbol");

    // TODO: Merge {@link #mSwitchState}, {@link #mIsAlphabetMode}, {@link #mAlphabetShiftState},
    // {@link #mIsSymbolShifted}, {@link #mPrevMainKeyboardWasShiftLocked}, and
    // {@link #mPrevSymbolsKeyboardWasShifted} into single state variable.
    private static final int SWITCH_STATE_ALPHA = 0;
    private static final int SWITCH_STATE_SYMBOL_BEGIN = 1;
    private static final int SWITCH_STATE_SYMBOL = 2;
    private static final int SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL = 3;
    private static final int SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE = 4;
    private static final int SWITCH_STATE_MOMENTARY_ALPHA_SHIFT = 5;
    private int mSwitchState = SWITCH_STATE_ALPHA;

    // TODO: Consolidate these two mode booleans into one integer to distinguish between alphabet,
    // symbols, and emoji mode.
    private boolean mIsAlphabetMode;
    private boolean mIsEmojiMode;
    private final AlphabetShiftState mAlphabetShiftState = new AlphabetShiftState();
    private boolean mIsSymbolShifted;
    private boolean mPrevMainKeyboardWasShiftLocked;
    private boolean mPrevSymbolsKeyboardWasShifted;
    private int mRecapitalizeMode;

    // For handling double tap.
    private boolean mIsInAlphabetUnshiftedFromShifted;
    private boolean mIsInDoubleTapShiftKey;

    private final SavedKeyboardState mSavedKeyboardState = new SavedKeyboardState();

    static final class SavedKeyboardState {
        public boolean mIsValid;
        public boolean mIsAlphabetMode;
        public boolean mIsAlphabetShiftLocked;
        public boolean mIsEmojiMode;
        public int mShiftMode;

        @Override
        public String toString() {
            if (!mIsValid) return "INVALID";
            if (mIsAlphabetMode) {
                if (mIsAlphabetShiftLocked) return "ALPHABET_SHIFT_LOCKED";
                return "ALPHABET_" + shiftModeToString(mShiftMode);
            } else if (mIsEmojiMode) {
                return "EMOJI";
            } else {
                return "SYMBOLS_" + shiftModeToString(mShiftMode);
            }
        }
    }

    public KeyboardState(final SwitchActions switchActions) {
        mSwitchActions = switchActions;
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE;
    }

    public void onLoadKeyboard(final int currentAutoCapsState,
            final int currentRecapitalizeState) {
        if (DEBUG_EVENT) {
            Log.d(TAG, "onLoadKeyboard: " + this);
        }
        // Reset alphabet shift state.
        mAlphabetShiftState.setShiftLocked(false);
        mPrevMainKeyboardWasShiftLocked = false;
        mPrevSymbolsKeyboardWasShifted = false;
        mShiftKeyState.onRelease();
        mSymbolKeyState.onRelease();
        onRestoreKeyboardState(currentAutoCapsState, currentRecapitalizeState);
    }

    private static final int UNSHIFT = 0;
    private static final int MANUAL_SHIFT = 1;
    private static final int AUTOMATIC_SHIFT = 2;
    private static final int SHIFT_LOCK_SHIFTED = 3;

    public void onSaveKeyboardState() {
        final SavedKeyboardState state = mSavedKeyboardState;
        state.mIsAlphabetMode = mIsAlphabetMode;
        state.mIsEmojiMode = mIsEmojiMode;
        if (mIsAlphabetMode) {
            state.mIsAlphabetShiftLocked = mAlphabetShiftState.isShiftLocked();
            state.mShiftMode = mAlphabetShiftState.isAutomaticShifted() ? AUTOMATIC_SHIFT
                    : (mAlphabetShiftState.isShiftedOrShiftLocked() ? MANUAL_SHIFT : UNSHIFT);
        } else {
            state.mIsAlphabetShiftLocked = mPrevMainKeyboardWasShiftLocked;
            state.mShiftMode = mIsSymbolShifted ? MANUAL_SHIFT : UNSHIFT;
        }
        state.mIsValid = true;
        if (DEBUG_EVENT) {
            Log.d(TAG, "onSaveKeyboardState: saved=" + state + ' ' + this);
        }
    }

    private void onRestoreKeyboardState(final int currentAutoCapsState,
            final int currentRecapitalizeState) {
        final SavedKeyboardState state = mSavedKeyboardState;
        if (DEBUG_EVENT) {
            Log.d(TAG, "onRestoreKeyboardState: saved=" + state + ' ' + this);
        }
        if (!state.mIsValid || state.mIsAlphabetMode) {
            setAlphabetKeyboard(currentAutoCapsState, currentRecapitalizeState);
        } else if (state.mIsEmojiMode) {
            setEmojiKeyboard();
        } else {
            if (state.mShiftMode == MANUAL_SHIFT) {
                setSymbolsShiftedKeyboard();
            } else {
                setSymbolsKeyboard();
            }
        }

        if (!state.mIsValid) return;
        state.mIsValid = false;

        if (state.mIsAlphabetMode) {
            setShiftLocked(state.mIsAlphabetShiftLocked);
            if (!state.mIsAlphabetShiftLocked) {
                setShifted(state.mShiftMode);
            }
            return;
        }
        if (state.mIsEmojiMode) {
            setEmojiKeyboard();
            return;
        }
        // Symbol mode
        if (state.mShiftMode == MANUAL_SHIFT) {
            setSymbolsShiftedKeyboard();
        } else {
            mPrevMainKeyboardWasShiftLocked = state.mIsAlphabetShiftLocked;
        }
    }

    private void setShifted(final int shiftMode) {
        if (DEBUG_ACTION) {
            Log.d(TAG, "setShifted: shiftMode=" + shiftModeToString(shiftMode) + ' ' + this);
        }
        if (!mIsAlphabetMode) return;
        final int prevShiftMode;
        if (mAlphabetShiftState.isAutomaticShifted()) {
            prevShiftMode = AUTOMATIC_SHIFT;
        } else if (mAlphabetShiftState.isManualShifted()) {
            prevShiftMode = MANUAL_SHIFT;
        } else {
            prevShiftMode = UNSHIFT;
        }
        switch (shiftMode) {
        case AUTOMATIC_SHIFT:
            mAlphabetShiftState.setAutomaticShifted();
            if (shiftMode != prevShiftMode) {
                mSwitchActions.setAlphabetAutomaticShiftedKeyboard();
            }
            break;
        case MANUAL_SHIFT:
            mAlphabetShiftState.setShifted(true);
            if (shiftMode != prevShiftMode) {
                mSwitchActions.setAlphabetManualShiftedKeyboard();
            }
            break;
        case UNSHIFT:
            mAlphabetShiftState.setShifted(false);
            if (shiftMode != prevShiftMode) {
                mSwitchActions.setAlphabetKeyboard();
            }
            break;
        case SHIFT_LOCK_SHIFTED:
            mAlphabetShiftState.setShifted(true);
            mSwitchActions.setAlphabetShiftLockShiftedKeyboard();
            break;
        }
    }

    private void setShiftLocked(final boolean shiftLocked) {
        if (DEBUG_ACTION) {
            Log.d(TAG, "setShiftLocked: shiftLocked=" + shiftLocked + ' ' + this);
        }
        if (!mIsAlphabetMode) return;
        if (shiftLocked && (!mAlphabetShiftState.isShiftLocked()
                || mAlphabetShiftState.isShiftLockShifted())) {
            mSwitchActions.setAlphabetShiftLockedKeyboard();
        }
        if (!shiftLocked && mAlphabetShiftState.isShiftLocked()) {
            mSwitchActions.setAlphabetKeyboard();
        }
        mAlphabetShiftState.setShiftLocked(shiftLocked);
    }

    private void toggleAlphabetAndSymbols(final int currentAutoCapsState,
            final int currentRecapitalizeState) {
        if (DEBUG_ACTION) {
            Log.d(TAG, "toggleAlphabetAndSymbols: " + this);
        }
        if (mIsAlphabetMode) {
            mPrevMainKeyboardWasShiftLocked = mAlphabetShiftState.isShiftLocked();
            if (mPrevSymbolsKeyboardWasShifted) {
                setSymbolsShiftedKeyboard();
            } else {
                setSymbolsKeyboard();
            }
            mPrevSymbolsKeyboardWasShifted = false;
        } else {
            mPrevSymbolsKeyboardWasShifted = mIsSymbolShifted;
            setAlphabetKeyboard(currentAutoCapsState, currentRecapitalizeState);
            if (mPrevMainKeyboardWasShiftLocked) {
                setShiftLocked(true);
            }
            mPrevMainKeyboardWasShiftLocked = false;
        }
    }

    // TODO: Remove this method. Come up with a more comprehensive way to reset the keyboard layout
    // when a keyboard layout set doesn't get reloaded in LatinIME.onStartInputViewInternal().
    private void resetKeyboardStateToAlphabet(final int currentAutoCapsState,
            final int currentRecapitalizeState) {
        if (DEBUG_ACTION) {
            Log.d(TAG, "resetKeyboardStateToAlphabet: " + this);
        }
        if (mIsAlphabetMode) return;

        mPrevSymbolsKeyboardWasShifted = mIsSymbolShifted;
        setAlphabetKeyboard(currentAutoCapsState, currentRecapitalizeState);
        if (mPrevMainKeyboardWasShiftLocked) {
            setShiftLocked(true);
        }
        mPrevMainKeyboardWasShiftLocked = false;
    }

    private void toggleShiftInSymbols() {
        if (mIsSymbolShifted) {
            setSymbolsKeyboard();
        } else {
            setSymbolsShiftedKeyboard();
        }
    }

    private void setAlphabetKeyboard(final int currentAutoCapsState,
            final int currentRecapitalizeState) {
        if (DEBUG_ACTION) {
            Log.d(TAG, "setAlphabetKeyboard");
        }

        mSwitchActions.setAlphabetKeyboard();
        mIsAlphabetMode = true;
        mIsEmojiMode = false;
        mIsSymbolShifted = false;
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE;
        mSwitchState = SWITCH_STATE_ALPHA;
        mSwitchActions.requestUpdatingShiftState(currentAutoCapsState, currentRecapitalizeState);
    }

    private void setSymbolsKeyboard() {
        if (DEBUG_ACTION) {
            Log.d(TAG, "setSymbolsKeyboard");
        }
        mSwitchActions.setSymbolsKeyboard();
        mIsAlphabetMode = false;
        mIsSymbolShifted = false;
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE;
        // Reset alphabet shift state.
        mAlphabetShiftState.setShiftLocked(false);
        mSwitchState = SWITCH_STATE_SYMBOL_BEGIN;
    }

    private void setSymbolsShiftedKeyboard() {
        if (DEBUG_ACTION) {
            Log.d(TAG, "setSymbolsShiftedKeyboard");
        }
        mSwitchActions.setSymbolsShiftedKeyboard();
        mIsAlphabetMode = false;
        mIsSymbolShifted = true;
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE;
        // Reset alphabet shift state.
        mAlphabetShiftState.setShiftLocked(false);
        mSwitchState = SWITCH_STATE_SYMBOL_BEGIN;
    }

    private void setEmojiKeyboard() {
        if (DEBUG_ACTION) {
            Log.d(TAG, "setEmojiKeyboard");
        }
        FontUtils.setIsEmoji(true);
        mIsAlphabetMode = false;
        mIsEmojiMode = true;
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE;
        // Remember caps lock mode and reset alphabet shift state.
        mPrevMainKeyboardWasShiftLocked = mAlphabetShiftState.isShiftLocked();
        mAlphabetShiftState.setShiftLocked(false);
        mSwitchActions.setEmojiKeyboard();
    }

    public void onPressKey(final int code, final boolean isSinglePointer,
            final int currentAutoCapsState, final int currentRecapitalizeState) {
        if (DEBUG_EVENT) {
            Log.d(TAG, "onPressKey: code=" + Constants.printableCode(code) + " single="
                    + isSinglePointer + " autoCaps=" + currentAutoCapsState + ' ' + this);
        }
        if (code != Constants.CODE_SHIFT) {
            // Because the double tap shift key timer is to detect two consecutive shift key press,
            // it should be canceled when a non-shift key is pressed.
            mSwitchActions.cancelDoubleTapShiftKeyTimer();
        }
        if (code == Constants.CODE_SHIFT) {
            onPressShift();
        } else if (code == Constants.CODE_CAPSLOCK) {
            // Nothing to do here. See {@link #onReleaseKey(int,boolean)}.
        } else if (code == Constants.CODE_SWITCH_ALPHA_SYMBOL) {
            onPressSymbol(currentAutoCapsState, currentRecapitalizeState);
        } else {
            mShiftKeyState.onOtherKeyPressed();
            mSymbolKeyState.onOtherKeyPressed();
            // It is required to reset the auto caps state when all of the following conditions
            // are met:
            // 1) two or more fingers are in action
            // 2) in alphabet layout
            // 3) not in all characters caps mode
            // As for #3, please note that it's required to check even when the auto caps mode is
            // off because, for example, we may be in the #1 state within the manual temporary
            // shifted mode.
            if (!isSinglePointer && mIsAlphabetMode
                    && currentAutoCapsState != TextUtils.CAP_MODE_CHARACTERS) {
                final boolean needsToResetAutoCaps = mAlphabetShiftState.isAutomaticShifted()
                        || (mAlphabetShiftState.isManualShifted() && mShiftKeyState.isReleasing());
                if (needsToResetAutoCaps) {
                    mSwitchActions.setAlphabetKeyboard();
                }
            }
        }
    }

    public void onReleaseKey(final int code, final boolean withSliding,
            final int currentAutoCapsState, final int currentRecapitalizeState) {
        if (DEBUG_EVENT) {
            Log.d(TAG, "onReleaseKey: code=" + Constants.printableCode(code)
                    + " sliding=" + withSliding + ' ' + this);
        }
        if (code == Constants.CODE_SHIFT) {
            onReleaseShift(withSliding, currentAutoCapsState, currentRecapitalizeState);
        } else if (code == Constants.CODE_CAPSLOCK) {
            setShiftLocked(!mAlphabetShiftState.isShiftLocked());
        } else if (code == Constants.CODE_SWITCH_ALPHA_SYMBOL) {
            onReleaseSymbol(withSliding, currentAutoCapsState, currentRecapitalizeState);
        }
    }

    private void onPressSymbol(final int currentAutoCapsState,
            final int currentRecapitalizeState) {
        toggleAlphabetAndSymbols(currentAutoCapsState, currentRecapitalizeState);
        mSymbolKeyState.onPress();
        mSwitchState = SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL;
    }

    private void onReleaseSymbol(final boolean withSliding, final int currentAutoCapsState,
            final int currentRecapitalizeState) {
        if (mSymbolKeyState.isChording()) {
            // Switch back to the previous keyboard mode if the user chords the mode change key and
            // another key, then releases the mode change key.
            toggleAlphabetAndSymbols(currentAutoCapsState, currentRecapitalizeState);
        } else if (!withSliding) {
            // If the mode change key is being released without sliding, we should forget the
            // previous symbols keyboard shift state and simply switch back to symbols layout
            // (never symbols shifted) next time the mode gets changed to symbols layout.
            mPrevSymbolsKeyboardWasShifted = false;
        }
        mSymbolKeyState.onRelease();
    }

    public void onUpdateShiftState(final int autoCaps, final int recapitalizeMode) {
        if (DEBUG_EVENT) {
            Log.d(TAG, "onUpdateShiftState: autoCaps=" + autoCaps + ", recapitalizeMode="
                    + recapitalizeMode + ' ' + this);
        }
        mRecapitalizeMode = recapitalizeMode;
        updateAlphabetShiftState(autoCaps, recapitalizeMode);
    }

    // TODO: Remove this method. Come up with a more comprehensive way to reset the keyboard layout
    // when a keyboard layout set doesn't get reloaded in LatinIME.onStartInputViewInternal().
    public void onResetKeyboardStateToAlphabet(final int currentAutoCapsState,
            final int currentRecapitalizeState) {
        if (DEBUG_EVENT) {
            Log.d(TAG, "onResetKeyboardStateToAlphabet: " + this);
        }
        FontUtils.setIsEmoji(false);
        resetKeyboardStateToAlphabet(currentAutoCapsState, currentRecapitalizeState);
    }

    private void updateShiftStateForRecapitalize(final int recapitalizeMode) {
        switch (recapitalizeMode) {
        case RecapitalizeStatus.CAPS_MODE_ALL_UPPER:
            setShifted(SHIFT_LOCK_SHIFTED);
            break;
        case RecapitalizeStatus.CAPS_MODE_FIRST_WORD_UPPER:
            setShifted(AUTOMATIC_SHIFT);
            break;
        case RecapitalizeStatus.CAPS_MODE_ALL_LOWER:
        case RecapitalizeStatus.CAPS_MODE_ORIGINAL_MIXED_CASE:
        default:
            setShifted(UNSHIFT);
        }
    }

    private void updateAlphabetShiftState(final int autoCaps, final int recapitalizeMode) {
        if (!mIsAlphabetMode) return;
        if (RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE != recapitalizeMode) {
            // We are recapitalizing. Match the keyboard to the current recapitalize state.
            updateShiftStateForRecapitalize(recapitalizeMode);
            return;
        }
        if (!mShiftKeyState.isReleasing()) {
            // Ignore update shift state event while the shift key is being pressed (including
            // chording).
            return;
        }
        if (!mAlphabetShiftState.isShiftLocked() && !mShiftKeyState.isIgnoring()) {
            if (mShiftKeyState.isReleasing() && autoCaps != Constants.TextUtils.CAP_MODE_OFF) {
                // Only when shift key is releasing, automatic temporary upper case will be set.
                setShifted(AUTOMATIC_SHIFT);
            } else {
                setShifted(mShiftKeyState.isChording() ? MANUAL_SHIFT : UNSHIFT);
            }
        }
    }

    private void onPressShift() {
        // If we are recapitalizing, we don't do any of the normal processing, including
        // importantly the double tap timer.
        if (RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE != mRecapitalizeMode) {
            return;
        }
        if (mIsAlphabetMode) {
            mIsInDoubleTapShiftKey = mSwitchActions.isInDoubleTapShiftKeyTimeout();
            if (!mIsInDoubleTapShiftKey) {
                // This is first tap.
                mSwitchActions.startDoubleTapShiftKeyTimer();
            }
            if (mIsInDoubleTapShiftKey) {
                if (mAlphabetShiftState.isManualShifted() || mIsInAlphabetUnshiftedFromShifted) {
                    // Shift key has been double tapped while in manual shifted or automatic
                    // shifted state.
                    setShiftLocked(true);
                } else {
                    // Shift key has been double tapped while in normal state. This is the second
                    // tap to disable shift locked state, so just ignore this.
                }
            } else {
                if (mAlphabetShiftState.isShiftLocked()) {
                    // Shift key is pressed while shift locked state, we will treat this state as
                    // shift lock shifted state and mark as if shift key pressed while normal
                    // state.
                    setShifted(SHIFT_LOCK_SHIFTED);
                    mShiftKeyState.onPress();
                } else if (mAlphabetShiftState.isAutomaticShifted()) {
                    // Shift key is pressed while automatic shifted, we have to move to manual
                    // shifted.
                    setShifted(MANUAL_SHIFT);
                    mShiftKeyState.onPress();
                } else if (mAlphabetShiftState.isShiftedOrShiftLocked()) {
                    // In manual shifted state, we just record shift key has been pressing while
                    // shifted state.
                    mShiftKeyState.onPressOnShifted();
                } else {
                    // In base layout, chording or manual shifted mode is started.
                    setShifted(MANUAL_SHIFT);
                    mShiftKeyState.onPress();
                }
            }
        } else {
            // In symbol mode, just toggle symbol and symbol more keyboard.
            toggleShiftInSymbols();
            mSwitchState = SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE;
            mShiftKeyState.onPress();
        }
    }

    private void onReleaseShift(final boolean withSliding, final int currentAutoCapsState,
            final int currentRecapitalizeState) {
        if (RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE != mRecapitalizeMode) {
            // We are recapitalizing. We should match the keyboard state to the recapitalize
            // state in priority.
            updateShiftStateForRecapitalize(mRecapitalizeMode);
        } else if (mIsAlphabetMode) {
            final boolean isShiftLocked = mAlphabetShiftState.isShiftLocked();
            mIsInAlphabetUnshiftedFromShifted = false;
            if (mIsInDoubleTapShiftKey) {
                // Double tap shift key has been handled in {@link #onPressShift}, so that just
                // ignore this release shift key here.
                mIsInDoubleTapShiftKey = false;
            } else if (mShiftKeyState.isChording()) {
                if (mAlphabetShiftState.isShiftLockShifted()) {
                    // After chording input while shift locked state.
                    setShiftLocked(true);
                } else {
                    // After chording input while normal state.
                    setShifted(UNSHIFT);
                }
                // After chording input, automatic shift state may have been changed depending on
                // what characters were input.
                mShiftKeyState.onRelease();
                mSwitchActions.requestUpdatingShiftState(currentAutoCapsState,
                        currentRecapitalizeState);
                return;
            } else if (mAlphabetShiftState.isShiftLockShifted() && withSliding) {
                // In shift locked state, shift has been pressed and slid out to other key.
                setShiftLocked(true);
            } else if (mAlphabetShiftState.isManualShifted() && withSliding) {
                // Shift has been pressed and slid out to other key.
                mSwitchState = SWITCH_STATE_MOMENTARY_ALPHA_SHIFT;
            } else if (isShiftLocked && !mAlphabetShiftState.isShiftLockShifted()
                    && (mShiftKeyState.isPressing() || mShiftKeyState.isPressingOnShifted())
                    && !withSliding) {
                // Shift has been long pressed, ignore this release.
            } else if (isShiftLocked && !mShiftKeyState.isIgnoring() && !withSliding) {
                // Shift has been pressed without chording while shift locked state.
                setShiftLocked(false);
            } else if (mAlphabetShiftState.isShiftedOrShiftLocked()
                    && mShiftKeyState.isPressingOnShifted() && !withSliding) {
                // Shift has been pressed without chording while shifted state.
                setShifted(UNSHIFT);
                mIsInAlphabetUnshiftedFromShifted = true;
            } else if (mAlphabetShiftState.isManualShiftedFromAutomaticShifted()
                    && mShiftKeyState.isPressing() && !withSliding) {
                // Shift has been pressed without chording while manual shifted transited from
                // automatic shifted
                setShifted(UNSHIFT);
                mIsInAlphabetUnshiftedFromShifted = true;
            }
        } else {
            // In symbol mode, switch back to the previous keyboard mode if the user chords the
            // shift key and another key, then releases the shift key.
            if (mShiftKeyState.isChording()) {
                toggleShiftInSymbols();
            }
        }
        mShiftKeyState.onRelease();
    }

    public void onFinishSlidingInput(final int currentAutoCapsState,
            final int currentRecapitalizeState) {
        if (DEBUG_EVENT) {
            Log.d(TAG, "onFinishSlidingInput: " + this);
        }
        // Switch back to the previous keyboard mode if the user cancels sliding input.
        switch (mSwitchState) {
        case SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL:
            toggleAlphabetAndSymbols(currentAutoCapsState, currentRecapitalizeState);
            break;
        case SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE:
            toggleShiftInSymbols();
            break;
        case SWITCH_STATE_MOMENTARY_ALPHA_SHIFT:
            setAlphabetKeyboard(currentAutoCapsState, currentRecapitalizeState);
            break;
        }
    }

    private static boolean isSpaceOrEnter(final int c) {
        return c == Constants.CODE_SPACE || c == Constants.CODE_ENTER;
    }

    public void onCodeInput(final int code, final int currentAutoCapsState,
            final int currentRecapitalizeState) {
        if (DEBUG_EVENT) {
            Log.d(TAG, "onCodeInput: code=" + Constants.printableCode(code)
                    + " autoCaps=" + currentAutoCapsState + ' ' + this);
        }

        switch (mSwitchState) {
        case SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL:
            if (code == Constants.CODE_SWITCH_ALPHA_SYMBOL) {
                // Detected only the mode change key has been pressed, and then released.
                if (mIsAlphabetMode) {
                    mSwitchState = SWITCH_STATE_ALPHA;
                } else {
                    mSwitchState = SWITCH_STATE_SYMBOL_BEGIN;
                }
            }
            break;
        case SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE:
            if (code == Constants.CODE_SHIFT) {
                // Detected only the shift key has been pressed on symbol layout, and then
                // released.
                mSwitchState = SWITCH_STATE_SYMBOL_BEGIN;
            }
            break;
        case SWITCH_STATE_SYMBOL_BEGIN:
            if (mIsEmojiMode) {
                // When in the Emoji keyboard, we don't want to switch back to the main layout even
                // after the user hits an emoji letter followed by an enter or a space.
                break;
            }
            if (!isSpaceOrEnter(code) && (Constants.isLetterCode(code)
                    || code == Constants.CODE_OUTPUT_TEXT)) {
                mSwitchState = SWITCH_STATE_SYMBOL;
            }
            break;
        case SWITCH_STATE_SYMBOL:
            // Switch back to alpha keyboard mode if user types one or more non-space/enter
            // characters followed by a space/enter.
            if (isSpaceOrEnter(code)) {
                toggleAlphabetAndSymbols(currentAutoCapsState, currentRecapitalizeState);
                mPrevSymbolsKeyboardWasShifted = false;
            }
            break;
        }

        // If the code is a letter, update keyboard shift state.
        if (Constants.isLetterCode(code)) {
            updateAlphabetShiftState(currentAutoCapsState, currentRecapitalizeState);
        } else if (code == Constants.CODE_EMOJI) {
            setEmojiKeyboard();
        } else if (code == Constants.CODE_ALPHA_FROM_EMOJI) {
            FontUtils.setIsEmoji(false);
            setAlphabetKeyboard(currentAutoCapsState, currentRecapitalizeState);
        }
    }

    static String shiftModeToString(final int shiftMode) {
        switch (shiftMode) {
        case UNSHIFT: return "UNSHIFT";
        case MANUAL_SHIFT: return "MANUAL";
        case AUTOMATIC_SHIFT: return "AUTOMATIC";
        default: return null;
        }
    }

    private static String switchStateToString(final int switchState) {
        switch (switchState) {
        case SWITCH_STATE_ALPHA: return "ALPHA";
        case SWITCH_STATE_SYMBOL_BEGIN: return "SYMBOL-BEGIN";
        case SWITCH_STATE_SYMBOL: return "SYMBOL";
        case SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL: return "MOMENTARY-ALPHA-SYMBOL";
        case SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE: return "MOMENTARY-SYMBOL-MORE";
        case SWITCH_STATE_MOMENTARY_ALPHA_SHIFT: return "MOMENTARY-ALPHA_SHIFT";
        default: return null;
        }
    }

    @Override
    public String toString() {
        return "[keyboard=" + (mIsAlphabetMode ? mAlphabetShiftState.toString()
                : (mIsSymbolShifted ? "SYMBOLS_SHIFTED" : "SYMBOLS"))
                + " shift=" + mShiftKeyState
                + " symbol=" + mSymbolKeyState
                + " switch=" + switchStateToString(mSwitchState) + ']';
    }
}

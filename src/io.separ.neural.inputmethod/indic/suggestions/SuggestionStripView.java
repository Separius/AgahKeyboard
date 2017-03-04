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

package io.separ.neural.inputmethod.indic.suggestions;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.inputmethod.keyboard.Keyboard;
import com.android.inputmethod.keyboard.MainKeyboardView;
import com.android.inputmethod.keyboard.MoreKeysPanel;
import com.android.inputmethod.latin.utils.ImportantNoticeUtils;

import java.util.ArrayList;

import io.separ.neural.inputmethod.accessibility.AccessibilityUtils;
import io.separ.neural.inputmethod.colors.ColorProfile;
import io.separ.neural.inputmethod.indic.AudioAndHapticFeedbackManager;
import io.separ.neural.inputmethod.indic.Constants;
import io.separ.neural.inputmethod.indic.R;
import io.separ.neural.inputmethod.indic.SuggestedWords;
import io.separ.neural.inputmethod.indic.SuggestedWords.SuggestedWordInfo;
import io.separ.neural.inputmethod.indic.define.DebugFlags;
import io.separ.neural.inputmethod.indic.settings.Settings;
import io.separ.neural.inputmethod.indic.settings.SettingsValues;

public final class SuggestionStripView extends RelativeLayout implements OnClickListener,
        OnLongClickListener {

    public void updateColor(ColorProfile colorProfile) {
        setBackgroundColor(colorProfile.getPrimary());
        mVoiceKey.setColorFilter(colorProfile.getTextColor());
        mLayoutHelper.updateColor(colorProfile);
    }

    public interface Listener {
        void addWordToUserDictionary(String word);
        void showImportantNoticeContents();
        void pickSuggestionManually(SuggestedWordInfo word);
        void onCodeInput(int primaryCode, int x, int y, boolean isKeyRepeat);
    }

    static final boolean DBG = DebugFlags.DEBUG_ENABLED;
    private static final float DEBUG_INFO_TEXT_SIZE_IN_DIP = 6.0f;

    private final ViewGroup mSuggestionsStrip;
    private final ImageButton mVoiceKey;
    //private final ImageButton mSettingsKey;
    private final ViewGroup mAddToDictionaryStrip;
    //private final View mImportantNoticeStrip;
    MainKeyboardView mMainKeyboardView;

    private final ArrayList<TextView> mWordViews = new ArrayList<>();
    private final ArrayList<TextView> mDebugInfoViews = new ArrayList<>();

    Listener mListener;
    private SuggestedWords mSuggestedWords = SuggestedWords.EMPTY;

    private final SuggestionStripLayoutHelper mLayoutHelper;
    private final StripVisibilityGroup mStripVisibilityGroup;

    private static class StripVisibilityGroup {
        private final View mSuggestionStripView;
        private final View mSuggestionsStrip;
        private final View mAddToDictionaryStrip;
        //private final View mImportantNoticeStrip;

        public StripVisibilityGroup(final View suggestionStripView,
                final ViewGroup suggestionsStrip, final ViewGroup addToDictionaryStrip) {
            mSuggestionStripView = suggestionStripView;
            mSuggestionsStrip = suggestionsStrip;
            mAddToDictionaryStrip = addToDictionaryStrip;
            //mImportantNoticeStrip = importantNoticeStrip;
            showSuggestionsStrip();
        }

        public void setLayoutDirection(final boolean isRtlLanguage) {
            final int layoutDirection = isRtlLanguage ? ViewCompat.LAYOUT_DIRECTION_RTL
                    : ViewCompat.LAYOUT_DIRECTION_LTR;
            ViewCompat.setLayoutDirection(mSuggestionStripView, layoutDirection);
            ViewCompat.setLayoutDirection(mSuggestionsStrip, layoutDirection);
            ViewCompat.setLayoutDirection(mAddToDictionaryStrip, layoutDirection);
            //ViewCompat.setLayoutDirection(mImportantNoticeStrip, layoutDirection);
        }

        public void showSuggestionsStrip() {
            mSuggestionsStrip.setVisibility(VISIBLE);
            mAddToDictionaryStrip.setVisibility(INVISIBLE);
            //mImportantNoticeStrip.setVisibility(INVISIBLE);
        }

        public void showAddToDictionaryStrip() {
            mSuggestionsStrip.setVisibility(INVISIBLE);
            mAddToDictionaryStrip.setVisibility(VISIBLE);
            //mImportantNoticeStrip.setVisibility(INVISIBLE);
        }

        public void showImportantNoticeStrip() {
            mSuggestionsStrip.setVisibility(INVISIBLE);
            mAddToDictionaryStrip.setVisibility(INVISIBLE);
            //mImportantNoticeStrip.setVisibility(VISIBLE);
        }

        public boolean isShowingAddToDictionaryStrip() {
            return mAddToDictionaryStrip.getVisibility() == VISIBLE;
        }
    }

    /**
     * Construct a {@link SuggestionStripView} for showing suggestions to be picked by the user.
     * @param context
     * @param attrs
     */
    public SuggestionStripView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.suggestionStripViewStyle);
    }

    public SuggestionStripView(final Context context, final AttributeSet attrs,
            final int defStyle) {
        super(context, attrs, defStyle);

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.suggestions_strip, this);

        mSuggestionsStrip = (ViewGroup)findViewById(R.id.suggestions_strip);
        mVoiceKey = (ImageButton)findViewById(R.id.suggestions_strip_voice_key);
        //mSettingsKey = (ImageButton)findViewById(R.id.suggestions_strip_settings_key);
        mAddToDictionaryStrip = (ViewGroup)findViewById(R.id.add_to_dictionary_strip);
        //mImportantNoticeStrip = findViewById(R.id.important_notice_strip);
        mStripVisibilityGroup = new StripVisibilityGroup(this, mSuggestionsStrip,
                mAddToDictionaryStrip);

        ArrayList<View> mDividerViews = new ArrayList<>();
        for (int pos = 0; pos < SuggestedWords.MAX_SUGGESTIONS; pos++) {
            final TextView word = new TextView(context, null, R.attr.suggestionWordStyle);
            word.setOnClickListener(this);
            word.setOnLongClickListener(this);
            mWordViews.add(word);
            final View divider = inflater.inflate(R.layout.suggestion_divider, null);
            mDividerViews.add(divider);
            final TextView info = new TextView(context, null, R.attr.suggestionWordStyle);
            info.setTextColor(Color.WHITE);
            info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, DEBUG_INFO_TEXT_SIZE_IN_DIP);
            mDebugInfoViews.add(info);
        }

        mLayoutHelper = new SuggestionStripLayoutHelper(
                context, attrs, defStyle, mWordViews, mDividerViews, mDebugInfoViews);

        final TypedArray keyboardAttr = context.obtainStyledAttributes(attrs,
                R.styleable.Keyboard, defStyle, R.style.SuggestionStripView);
        final Drawable iconVoice = keyboardAttr.getDrawable(R.styleable.Keyboard_iconShortcutKey);
        //final Drawable iconSettings = keyboardAttr.getDrawable(R.styleable.Keyboard_iconSettingsKey);
        keyboardAttr.recycle();
        mVoiceKey.setImageDrawable(iconVoice);
        mVoiceKey.setOnClickListener(this);
        //mSettingsKey.setImageDrawable(iconSettings);
        //mSettingsKey.setOnClickListener(this);
    }

    /**
     * A connection back to the input method.
     * @param listener
     */
    public void setListener(final Listener listener, final View inputView) {
        mListener = listener;
        mMainKeyboardView = (MainKeyboardView)inputView.findViewById(R.id.keyboard_view);
    }

    public void updateVisibility(final boolean shouldBeVisible, final boolean isFullscreenMode) {
        final int visibility = shouldBeVisible ? VISIBLE : (isFullscreenMode ? GONE : INVISIBLE);
        setVisibility(visibility);
        final SettingsValues currentSettingsValues = Settings.getInstance().getCurrent();
        mVoiceKey.setVisibility(shouldBeVisible ? (currentSettingsValues.mShowsVoiceInputKey ? VISIBLE : INVISIBLE) : INVISIBLE);
        //mSettingsKey.setVisibility(shouldBeVisible ? VISIBLE : INVISIBLE);
    }

    public void setSuggestions(final SuggestedWords suggestedWords, final boolean isRtlLanguage) {
        clear();
        mStripVisibilityGroup.setLayoutDirection(isRtlLanguage);
        mSuggestedWords = suggestedWords;
        mLayoutHelper.layoutAndReturnStartIndexOfMoreSuggestions(mSuggestedWords, mSuggestionsStrip, this);
        mStripVisibilityGroup.showSuggestionsStrip();
    }

    public boolean isShowingAddToDictionaryHint() {
        return mStripVisibilityGroup.isShowingAddToDictionaryStrip();
    }

    public void showAddToDictionaryHint(final String word) {
        mLayoutHelper.layoutAddToDictionaryHint(word, mAddToDictionaryStrip);
        // {@link TextView#setTag()} is used to hold the word to be added to dictionary. The word
        // will be extracted at {@link #onClick(View)}.
        mAddToDictionaryStrip.setTag(word);
        mAddToDictionaryStrip.setOnClickListener(this);
        mStripVisibilityGroup.showAddToDictionaryStrip();
    }

    public boolean dismissAddToDictionaryHint() {
        if (isShowingAddToDictionaryHint()) {
            clear();
            return true;
        }
        return false;
    }

    // This method checks if we should show the important notice (checks on permanent storage if
    // it has been shown once already or not, and if in the setup wizard). If applicable, it shows
    // the notice. In all cases, it returns true if it was shown, false otherwise.
    /*public boolean maybeShowImportantNoticeTitle() {
        if (!ImportantNoticeUtils.shouldShowImportantNotice(getContext())) {
            return false;
        }
        if (getWidth() <= 0) {
            return false;
        }
        final String importantNoticeTitle = ImportantNoticeUtils.getNextImportantNoticeTitle(
                getContext());
        if (TextUtils.isEmpty(importantNoticeTitle)) {
            return false;
        }
        mLayoutHelper.layoutImportantNotice(mImportantNoticeStrip, importantNoticeTitle);
        mStripVisibilityGroup.showImportantNoticeStrip();
        mImportantNoticeStrip.setOnClickListener(this);
        return true;
    }*/

    public void clear() {
        mSuggestionsStrip.removeAllViews();
        removeAllDebugInfoViews();
        mStripVisibilityGroup.showSuggestionsStrip();
    }

    private void removeAllDebugInfoViews() {
        // The debug info views may be placed as children views of this {@link SuggestionStripView}.
        for (final View debugInfoView : mDebugInfoViews) {
            final ViewParent parent = debugInfoView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup)parent).removeView(debugInfoView);
            }
        }
    }

    @Override
    public boolean onLongClick(final View view) {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
                Constants.NOT_A_CODE, this);
        return false;
    }

    // Working variables for {@link onInterceptTouchEvent(MotionEvent)} and
    // {@link onTouchEvent(MotionEvent)}.
    private int mLastX;
    private int mLastY;
    private int mOriginX;
    private int mOriginY;
    private boolean mNeedsToTransformTouchEventToHoverEvent;

    @Override
    public boolean dispatchPopulateAccessibilityEvent(final AccessibilityEvent event) {
        // Don't populate accessibility event with suggested words and voice key.
        return true;
    }

    @Override
    public void onClick(final View view) {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
                Constants.CODE_UNSPECIFIED, this);
        /*if (view == mImportantNoticeStrip) {
            mListener.showImportantNoticeContents();
            return;
        }*/
        if (view == mVoiceKey) {
            mListener.onCodeInput(Constants.CODE_SHORTCUT,
                    Constants.SUGGESTION_STRIP_COORDINATE, Constants.SUGGESTION_STRIP_COORDINATE,
                    false /* isKeyRepeat */);
            return;
        }
        /*if (view == mSettingsKey) {
            mListener.onCodeInput(Constants.CODE_INLINESETTINGS,
                    Constants.SUGGESTION_STRIP_COORDINATE, Constants.SUGGESTION_STRIP_COORDINATE,
                    false *//* isKeyRepeat *//*);
            return;
        }*/
        final Object tag = view.getTag();
        // {@link String} tag is set at {@link #showAddToDictionaryHint(String,CharSequence)}.
        if (tag instanceof String) {
            final String wordToSave = (String)tag;
            mListener.addWordToUserDictionary(wordToSave);
            clear();
            return;
        }

        // {@link Integer} tag is set at
        // {@link SuggestionStripLayoutHelper#setupWordViewsTextAndColor(SuggestedWords,int)} and
        // {@link SuggestionStripLayoutHelper#layoutPunctuationSuggestions(SuggestedWords,ViewGroup}
        if (tag instanceof Integer) {
            final int index = (Integer) tag;
            if (index >= mSuggestedWords.size()) {
                return;
            }
            final SuggestedWordInfo wordInfo = mSuggestedWords.getInfo(index);
            mListener.pickSuggestionManually(wordInfo);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        // Called by the framework when the size is known. Show the important notice if applicable.
        // This may be overriden by showing suggestions later, if applicable.
        /*if (oldw <= 0 && w > 0) {
            maybeShowImportantNoticeTitle();
        }*/
    }
}

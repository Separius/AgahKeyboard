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

package io.separ.neural.inputmethod.indic.suggestions;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.view.ViewCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.inputmethod.latin.utils.AutoCorrectionUtils;
import com.android.inputmethod.latin.utils.ResourceUtils;
import com.android.inputmethod.latin.utils.SubtypeLocaleUtils;
import com.android.inputmethod.latin.utils.ViewLayoutUtils;

import java.util.ArrayList;

import io.separ.neural.inputmethod.Utils.FontUtils;
import io.separ.neural.inputmethod.accessibility.AccessibilityUtils;
import io.separ.neural.inputmethod.annotations.UsedForTesting;
import io.separ.neural.inputmethod.colors.ColorProfile;
import io.separ.neural.inputmethod.indic.PunctuationSuggestions;
import io.separ.neural.inputmethod.indic.R;
import io.separ.neural.inputmethod.indic.SuggestedWords;
import io.separ.neural.inputmethod.indic.SuggestedWords.SuggestedWordInfo;
import io.separ.neural.inputmethod.indic.define.DebugFlags;
import io.separ.neural.inputmethod.indic.settings.Settings;
import io.separ.neural.inputmethod.indic.settings.SettingsValues;

final class SuggestionStripLayoutHelper {
    private static final int DEFAULT_SUGGESTIONS_COUNT_IN_STRIP = 3;
    private static final float DEFAULT_CENTER_SUGGESTION_PERCENTILE = 0.40f;
    private static final int DEFAULT_MAX_MORE_SUGGESTIONS_ROW = 2;
    private static final int PUNCTUATIONS_IN_STRIP = 5;
    private static final float MIN_TEXT_XSCALE = 0.70f;

    public final int mPadding;
    public final int mDividerWidth;
    public final int mSuggestionsStripHeight;
    private final int mSuggestionsCountInStrip;

    // The index of these {@link ArrayList} is the position in the suggestion strip. The indices
    // increase towards the right for LTR scripts and the left for RTL scripts, starting with 0.
    // The position of the most important suggestion is in {@link #mCenterPositionInStrip}
    private final ArrayList<TextView> mWordViews;
    private final ArrayList<View> mDividerViews;
    private final ArrayList<TextView> mDebugInfoViews;

    private int mColorValidTypedWord;
    private int mColorTypedWord;
    private int mColorAutoCorrect;
    private int mColorSuggested;
    private final float mAlphaObsoleted;
    private final float mCenterSuggestionWeight;
    private final int mCenterPositionInStrip;
    private final int mTypedWordPositionWhenAutocorrect;
    private static final String MORE_SUGGESTIONS_HINT = "\u2026";
    private static final String LEFTWARDS_ARROW = "\u2190";
    private static final String RIGHTWARDS_ARROW = "\u2192";

    private static final CharacterStyle BOLD_SPAN = new StyleSpan(Typeface.BOLD);
    private static final CharacterStyle UNDERLINE_SPAN = new UnderlineSpan();

    private final int mSuggestionStripOptions;
    // These constants are the flag values of
    // {@link R.styleable#SuggestionStripView_suggestionStripOptions} attribute.
    private static final int AUTO_CORRECT_BOLD = 0x01;
    private static final int AUTO_CORRECT_UNDERLINE = 0x02;
    private static final int VALID_TYPED_WORD_BOLD = 0x04;

    public SuggestionStripLayoutHelper(final Context context, final AttributeSet attrs,
            final int defStyle, final ArrayList<TextView> wordViews,
            final ArrayList<View> dividerViews, final ArrayList<TextView> debugInfoViews) {
        mWordViews = wordViews;
        mDividerViews = dividerViews;
        mDebugInfoViews = debugInfoViews;

        final TextView wordView = wordViews.get(0);
        final View dividerView = dividerViews.get(0);
        mPadding = wordView.getCompoundPaddingLeft() + wordView.getCompoundPaddingRight();
        dividerView.measure(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mDividerWidth = dividerView.getMeasuredWidth();

        final Resources res = wordView.getResources();
        mSuggestionsStripHeight = res.getDimensionPixelSize(
                R.dimen.config_suggestions_strip_height);

        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SuggestionStripView, defStyle, R.style.SuggestionStripView);
        mSuggestionStripOptions = a.getInt(
                R.styleable.SuggestionStripView_suggestionStripOptions, 0);
        mAlphaObsoleted = ResourceUtils.getFraction(a,
                R.styleable.SuggestionStripView_alphaObsoleted, 1.0f);
        mColorValidTypedWord = a.getColor(R.styleable.SuggestionStripView_colorValidTypedWord, 0);
        mColorTypedWord = a.getColor(R.styleable.SuggestionStripView_colorTypedWord, 0);
        mColorAutoCorrect = a.getColor(R.styleable.SuggestionStripView_colorAutoCorrect, 0);
        mColorSuggested = a.getColor(R.styleable.SuggestionStripView_colorSuggested, 0);
        mSuggestionsCountInStrip = a.getInt(
                R.styleable.SuggestionStripView_suggestionsCountInStrip,
                DEFAULT_SUGGESTIONS_COUNT_IN_STRIP);
        mCenterSuggestionWeight = ResourceUtils.getFraction(a,
                R.styleable.SuggestionStripView_centerSuggestionPercentile,
                DEFAULT_CENTER_SUGGESTION_PERCENTILE);
        mCenterPositionInStrip = mSuggestionsCountInStrip / 2;
        // Assuming there are at least three suggestions. Also, note that the suggestions are
        // laid out according to script direction, so this is left of the center for LTR scripts
        // and right of the center for RTL scripts.
        mTypedWordPositionWhenAutocorrect = mCenterPositionInStrip - 1;
    }

    private CharSequence getStyledSuggestedWord(final SuggestedWords suggestedWords,
            final int indexInSuggestedWords) {
        if (indexInSuggestedWords >= suggestedWords.size()) {
            return null;
        }
        final String word = suggestedWords.getLabel(indexInSuggestedWords);
        // TODO: don't use the index to decide whether this is the auto-correction/typed word, as
        // this is brittle
        final boolean isAutoCorrection = suggestedWords.mWillAutoCorrect
                && indexInSuggestedWords == SuggestedWords.INDEX_OF_AUTO_CORRECTION;
        final boolean isTypedWordValid = suggestedWords.mTypedWordValid
                && indexInSuggestedWords == SuggestedWords.INDEX_OF_TYPED_WORD;
        if (!isAutoCorrection && !isTypedWordValid) {
            return word;
        }

        final int len = word.length();
        final Spannable spannedWord = new SpannableString(word);
        final int options = mSuggestionStripOptions;
        if ((isAutoCorrection && (options & AUTO_CORRECT_BOLD) != 0)
                || (isTypedWordValid && (options & VALID_TYPED_WORD_BOLD) != 0)) {
            spannedWord.setSpan(BOLD_SPAN, 0, len, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        if (isAutoCorrection && (options & AUTO_CORRECT_UNDERLINE) != 0) {
            spannedWord.setSpan(UNDERLINE_SPAN, 0, len, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return spannedWord;
    }

    /**
     * Convert an index of {@link SuggestedWords} to position in the suggestion strip.
     * @param indexInSuggestedWords the index of {@link SuggestedWords}.
     * @param suggestedWords the suggested words list
     * @return Non-negative integer of the position in the suggestion strip.
     *         Negative integer if the word of the index shouldn't be shown on the suggestion strip.
     */
    private int getPositionInSuggestionStrip(final int indexInSuggestedWords,
            final SuggestedWords suggestedWords) {
        final SettingsValues settingsValues = Settings.getInstance().getCurrent();
        final boolean shouldOmitTypedWord = shouldOmitTypedWord(suggestedWords.mInputStyle,
                settingsValues.mGestureFloatingPreviewTextEnabled,
                settingsValues.mShouldShowUiToAcceptTypedWord);
        return getPositionInSuggestionStrip(indexInSuggestedWords, suggestedWords.mWillAutoCorrect,
                settingsValues.mShouldShowUiToAcceptTypedWord && shouldOmitTypedWord,
                mCenterPositionInStrip, mTypedWordPositionWhenAutocorrect);
    }

    @UsedForTesting
    static boolean shouldOmitTypedWord(final int inputStyle,
            final boolean gestureFloatingPreviewTextEnabled,
            final boolean shouldShowUiToAcceptTypedWord) {
        final boolean omitTypedWord = (inputStyle == SuggestedWords.INPUT_STYLE_TYPING)
                || (inputStyle == SuggestedWords.INPUT_STYLE_TAIL_BATCH)
                || (inputStyle == SuggestedWords.INPUT_STYLE_UPDATE_BATCH
                        && gestureFloatingPreviewTextEnabled);
        return shouldShowUiToAcceptTypedWord && omitTypedWord;
    }

    @UsedForTesting
    static int getPositionInSuggestionStrip(final int indexInSuggestedWords,
            final boolean willAutoCorrect, final boolean omitTypedWord,
            final int centerPositionInStrip, final int typedWordPositionWhenAutoCorrect) {
        if (omitTypedWord) {
            if (indexInSuggestedWords == SuggestedWords.INDEX_OF_TYPED_WORD) {
                // Ignore.
                return -1;
            }
            if (indexInSuggestedWords == SuggestedWords.INDEX_OF_AUTO_CORRECTION) {
                // Center in the suggestion strip.
                return centerPositionInStrip;
            }
            // If neither of those, the order in the suggestion strip is left of the center first
            // then right of the center, to both edges of the suggestion strip.
            // For example, center-1, center+1, center-2, center+2, and so on.
            final int offsetFromCenter = (indexInSuggestedWords % 2) == 0 ? -(indexInSuggestedWords / 2) : (indexInSuggestedWords / 2);
            return centerPositionInStrip + offsetFromCenter;
        }
        final int indexToDisplayMostImportantSuggestion;
        final int indexToDisplaySecondMostImportantSuggestion;
        if (willAutoCorrect) {
            indexToDisplayMostImportantSuggestion = SuggestedWords.INDEX_OF_AUTO_CORRECTION;
            indexToDisplaySecondMostImportantSuggestion = SuggestedWords.INDEX_OF_TYPED_WORD;
        } else {
            indexToDisplayMostImportantSuggestion = SuggestedWords.INDEX_OF_TYPED_WORD;
            indexToDisplaySecondMostImportantSuggestion = SuggestedWords.INDEX_OF_AUTO_CORRECTION;
        }
        if (indexInSuggestedWords == indexToDisplayMostImportantSuggestion) {
            // Center in the suggestion strip.
            return centerPositionInStrip;
        }
        if (indexInSuggestedWords == indexToDisplaySecondMostImportantSuggestion) {
            // Center-1.
            return typedWordPositionWhenAutoCorrect;
        }
        // If neither of those, the order in the suggestion strip is right of the center first
        // then left of the center, to both edges of the suggestion strip.
        // For example, Center+1, center-2, center+2, center-3, and so on.
        final int n = indexInSuggestedWords + 1;
        final int offsetFromCenter = (n % 2) == 0 ? -(n / 2) : (n / 2);
        return centerPositionInStrip + offsetFromCenter;
    }

    private int getSuggestionTextColor(final SuggestedWords suggestedWords,
            final int indexInSuggestedWords) {
        // Use identity for strings, not #equals : it's the typed word if it's the same object
        final boolean isTypedWord = suggestedWords.getInfo(indexInSuggestedWords).isKindOf(
                SuggestedWordInfo.KIND_TYPED);

        final int color;
        if (indexInSuggestedWords == SuggestedWords.INDEX_OF_AUTO_CORRECTION
                && suggestedWords.mWillAutoCorrect) {
            color = mColorAutoCorrect;
        } else if (isTypedWord && suggestedWords.mTypedWordValid) {
            color = mColorValidTypedWord;
        } else if (isTypedWord) {
            color = mColorTypedWord;
        } else {
            color = mColorSuggested;
        }
        if (DebugFlags.DEBUG_ENABLED && suggestedWords.size() > 1) {
            // If we auto-correct, then the autocorrection is in slot 0 and the typed word
            // is in slot 1.
            if (indexInSuggestedWords == SuggestedWords.INDEX_OF_AUTO_CORRECTION
                    && suggestedWords.mWillAutoCorrect
                    && AutoCorrectionUtils.shouldBlockAutoCorrectionBySafetyNet(
                            suggestedWords.getLabel(SuggestedWords.INDEX_OF_AUTO_CORRECTION),
                            suggestedWords.getLabel(SuggestedWords.INDEX_OF_TYPED_WORD))) {
                return 0xFFFF0000;
            }
        }

        if (suggestedWords.mIsObsoleteSuggestions && !isTypedWord) {
            return applyAlpha(color, mAlphaObsoleted);
        }
        return color;
    }

    private static int applyAlpha(final int color, final float alpha) {
        final int newAlpha = (int)(Color.alpha(color) * alpha);
        return Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static void addDivider(final ViewGroup stripView, final View dividerView) {
        stripView.addView(dividerView);
        final LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams)dividerView.getLayoutParams();
        params.gravity = Gravity.CENTER;
    }

    /**
     * Layout suggestions to the suggestions strip. And returns the start index of more
     * suggestions.
     *
     * @param suggestedWords suggestions to be shown in the suggestions strip.
     * @param stripView the suggestions strip view.
     * @param placerView the view where the debug info will be placed.
     * @return the start index of more suggestions.
     */
    public int layoutAndReturnStartIndexOfMoreSuggestions(final SuggestedWords suggestedWords,
            final ViewGroup stripView, final ViewGroup placerView) {
        if (suggestedWords.isPunctuationSuggestions()) {
            return layoutPunctuationsAndReturnStartIndexOfMoreSuggestions(
                    (PunctuationSuggestions)suggestedWords, stripView);
        }

        final int startIndexOfMoreSuggestions = setupWordViewsAndReturnStartIndexOfMoreSuggestions(
                suggestedWords, mSuggestionsCountInStrip);
        final TextView centerWordView = mWordViews.get(mCenterPositionInStrip);
        final int stripWidth = stripView.getWidth();
        final int centerWidth = getSuggestionWidth(mCenterPositionInStrip, stripWidth);
        if (suggestedWords.size() == 1 || getTextScaleX(centerWordView.getText(), centerWidth,
                centerWordView.getPaint()) < MIN_TEXT_XSCALE) {
            // Layout only the most relevant suggested word at the center of the suggestion strip
            // by consolidating all slots in the strip.
            final int countInStrip = 1;
            layoutWord(mCenterPositionInStrip, stripWidth - mPadding);
            stripView.addView(centerWordView);
            setLayoutWeight(centerWordView, 1.0f, ViewGroup.LayoutParams.MATCH_PARENT);
            if (SuggestionStripView.DBG) {
                layoutDebugInfo(mCenterPositionInStrip, placerView, stripWidth);
            }
            final Integer lastIndex = (Integer)centerWordView.getTag();
            return (lastIndex == null ? 0 : lastIndex) + 1;
        }

        final int countInStrip = mSuggestionsCountInStrip;
        int x = 0;
        for (int positionInStrip = 0; positionInStrip < countInStrip; positionInStrip++) {
            if (positionInStrip != 0) {
                final View divider = mDividerViews.get(positionInStrip);
                // Add divider if this isn't the left most suggestion in suggestions strip.
                addDivider(stripView, divider);
                x += divider.getMeasuredWidth();
            }

            final int width = getSuggestionWidth(positionInStrip, stripWidth);
            final TextView wordView = layoutWord(positionInStrip, width);
            stripView.addView(wordView);
            setLayoutWeight(wordView, getSuggestionWeight(positionInStrip),
                    ViewGroup.LayoutParams.MATCH_PARENT);
            x += wordView.getMeasuredWidth();

            if (SuggestionStripView.DBG) {
                layoutDebugInfo(positionInStrip, placerView, x);
            }
        }
        return startIndexOfMoreSuggestions;
    }

    /**
     * Format appropriately the suggested word in {@link #mWordViews} specified by
     * <code>positionInStrip</code>. When the suggested word doesn't exist, the corresponding
     * {@link TextView} will be disabled and never respond to user interaction. The suggested word
     * may be shrunk or ellipsized to fit in the specified width.
     *
     * The <code>positionInStrip</code> argument is the index in the suggestion strip. The indices
     * increase towards the right for LTR scripts and the left for RTL scripts, starting with 0.
     * The position of the most important suggestion is in {@link #mCenterPositionInStrip}. This
     * usually doesn't match the index in <code>suggedtedWords</code> -- see
     * {@link #getPositionInSuggestionStrip(int,SuggestedWords)}.
     *
     * @param positionInStrip the position in the suggestion strip.
     * @param width the maximum width for layout in pixels.
     * @return the {@link TextView} containing the suggested word appropriately formatted.
     */
    private TextView layoutWord(final int positionInStrip, final int width) {
        final TextView wordView = mWordViews.get(positionInStrip);
        final CharSequence word = wordView.getText();
        wordView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        // {@link StyleSpan} in a content description may cause an issue of TTS/TalkBack.
        // Use a simple {@link String} to avoid the issue.
        wordView.setContentDescription(TextUtils.isEmpty(word) ? null : word.toString());
        final CharSequence text = getEllipsizedText(word, width, wordView.getPaint());
        final float scaleX = getTextScaleX(word, width, wordView.getPaint());
        wordView.setText(text); // TextView.setText() resets text scale x to 1.0.
        wordView.setTextScaleX(Math.max(scaleX, MIN_TEXT_XSCALE));
        // A <code>wordView</code> should be disabled when <code>word</code> is empty in order to
        // make it unclickable.
        // With accessibility touch exploration on, <code>wordView</code> should be enabled even
        // when it is empty to avoid announcing as "disabled".
        wordView.setEnabled(!TextUtils.isEmpty(word)
                || AccessibilityUtils.getInstance().isTouchExplorationEnabled());
        return wordView;
    }

    private void layoutDebugInfo(final int positionInStrip, final ViewGroup placerView,
            final int x) {
        final TextView debugInfoView = mDebugInfoViews.get(positionInStrip);
        final CharSequence debugInfo = debugInfoView.getText();
        if (debugInfo == null) {
            return;
        }
        placerView.addView(debugInfoView);
        debugInfoView.measure(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int infoWidth = debugInfoView.getMeasuredWidth();
        final int y = debugInfoView.getMeasuredHeight();
        ViewLayoutUtils.placeViewAt(
                debugInfoView, x - infoWidth, y, infoWidth, debugInfoView.getMeasuredHeight());
    }

    private int getSuggestionWidth(final int positionInStrip, final int maxWidth) {
        final int paddings = mPadding * mSuggestionsCountInStrip;
        final int dividers = mDividerWidth * (mSuggestionsCountInStrip - 1);
        final int availableWidth = maxWidth - paddings - dividers;
        return (int)(availableWidth * getSuggestionWeight(positionInStrip));
    }

    private float getSuggestionWeight(final int positionInStrip) {
        if (positionInStrip == mCenterPositionInStrip) {
            return mCenterSuggestionWeight;
        }
        // TODO: Revisit this for cases of 5 or more suggestions
        return (1.0f - mCenterSuggestionWeight) / (mSuggestionsCountInStrip - 1);
    }

    private int setupWordViewsAndReturnStartIndexOfMoreSuggestions(
            final SuggestedWords suggestedWords, final int maxSuggestionInStrip) {
        // Clear all suggestions first
        for (int positionInStrip = 0; positionInStrip < maxSuggestionInStrip; ++positionInStrip) {
            final TextView wordView = mWordViews.get(positionInStrip);
            wordView.setText(null);
            wordView.setTag(null);
            // Make this inactive for touches in {@link #layoutWord(int,int)}.
            if (SuggestionStripView.DBG) {
                mDebugInfoViews.get(positionInStrip).setText(null);
            }
        }
        int count = 0;
        int indexInSuggestedWords;
        for (indexInSuggestedWords = 0; indexInSuggestedWords < suggestedWords.size()
                && count < maxSuggestionInStrip; indexInSuggestedWords++) {
            final int positionInStrip =
                    getPositionInSuggestionStrip(indexInSuggestedWords, suggestedWords);
            if (positionInStrip < 0) {
                continue;
            }
            final TextView wordView = mWordViews.get(positionInStrip);
            // {@link TextView#getTag()} is used to get the index in suggestedWords at
            // {@link SuggestionStripView#onClick(View)}.
            wordView.setTag(indexInSuggestedWords);
            wordView.setText(getStyledSuggestedWord(suggestedWords, indexInSuggestedWords));
            wordView.setTextColor(getSuggestionTextColor(suggestedWords, indexInSuggestedWords));
            wordView.setTypeface(FontUtils.getTypeface());
            if (SuggestionStripView.DBG) {
                mDebugInfoViews.get(positionInStrip).setText(
                        suggestedWords.getDebugString(indexInSuggestedWords));
            }
            count++;
        }
        return indexInSuggestedWords;
    }

    private int layoutPunctuationsAndReturnStartIndexOfMoreSuggestions(
            final PunctuationSuggestions punctuationSuggestions, final ViewGroup stripView) {
        final int countInStrip = Math.min(punctuationSuggestions.size(), PUNCTUATIONS_IN_STRIP);
        for (int positionInStrip = 0; positionInStrip < countInStrip; positionInStrip++) {
            if (positionInStrip != 0) {
                // Add divider if this isn't the left most suggestion in suggestions strip.
                addDivider(stripView, mDividerViews.get(positionInStrip));
            }

            final TextView wordView = mWordViews.get(positionInStrip);
            final String punctuation = punctuationSuggestions.getLabel(positionInStrip);
            // {@link TextView#getTag()} is used to get the index in suggestedWords at
            // {@link SuggestionStripView#onClick(View)}.
            wordView.setTag(positionInStrip);
            wordView.setText(punctuation);
            wordView.setContentDescription(punctuation);
            wordView.setTextScaleX(1.0f);
            wordView.setCompoundDrawables(null, null, null, null);
            wordView.setTextColor(mColorAutoCorrect);
            stripView.addView(wordView);
            setLayoutWeight(wordView, 1.0f, mSuggestionsStripHeight);
        }
        return countInStrip;
    }

    public void layoutAddToDictionaryHint(final String word, final ViewGroup addToDictionaryStrip) {
        final boolean shouldShowUiToAcceptTypedWord = Settings.getInstance().getCurrent()
                .mShouldShowUiToAcceptTypedWord;
        final int stripWidth = addToDictionaryStrip.getWidth();
        final int width = shouldShowUiToAcceptTypedWord ? stripWidth
                : stripWidth - mDividerWidth - mPadding * 2;

        final TextView wordView = (TextView)addToDictionaryStrip.findViewById(R.id.word_to_save);
        wordView.setTextColor(mColorTypedWord);
        final int wordWidth = (int)(width * mCenterSuggestionWeight);
        final CharSequence wordToSave = getEllipsizedText(word, wordWidth, wordView.getPaint());
        final float wordScaleX = wordView.getTextScaleX();
        wordView.setText(wordToSave);
        wordView.setTextScaleX(wordScaleX);
        setLayoutWeight(wordView, mCenterSuggestionWeight, ViewGroup.LayoutParams.MATCH_PARENT);
        final int wordVisibility = shouldShowUiToAcceptTypedWord ? View.GONE : View.VISIBLE;
        wordView.setVisibility(wordVisibility);
        addToDictionaryStrip.findViewById(R.id.word_to_save_divider).setVisibility(wordVisibility);

        final Resources res = addToDictionaryStrip.getResources();
        final CharSequence hintText;
        final int hintWidth;
        final float hintWeight;
        final TextView hintView = (TextView)addToDictionaryStrip.findViewById(
                R.id.hint_add_to_dictionary);
        if (shouldShowUiToAcceptTypedWord) {
            hintText = res.getText(R.string.hint_add_to_dictionary_without_word);
            hintWidth = width;
            hintWeight = 1.0f;
            hintView.setGravity(Gravity.CENTER);
        } else {
            final boolean isRtlLanguage = (ViewCompat.getLayoutDirection(addToDictionaryStrip)
                    == ViewCompat.LAYOUT_DIRECTION_RTL);
            final String arrow = isRtlLanguage ? RIGHTWARDS_ARROW : LEFTWARDS_ARROW;
            final boolean isRtlSystem = SubtypeLocaleUtils.isRtlLanguage(
                    res.getConfiguration().locale);
            final CharSequence hint = res.getText(R.string.hint_add_to_dictionary);
            hintText = (isRtlLanguage == isRtlSystem) ? (arrow + hint) : (hint + arrow);
            hintWidth = width - wordWidth;
            hintWeight = 1.0f - mCenterSuggestionWeight;
            hintView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        }
        hintView.setTextColor(mColorAutoCorrect);
        final float hintScaleX = getTextScaleX(hintText, hintWidth, hintView.getPaint());
        hintView.setText(hintText);
        hintView.setTextScaleX(hintScaleX);
        setLayoutWeight(hintView, hintWeight, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public void layoutImportantNotice(final View importantNoticeStrip,
            final String importantNoticeTitle) {
        /*final TextView titleView = (TextView)importantNoticeStrip.findViewById(
                R.id.important_notice_title);
        final int width = titleView.getWidth() - titleView.getPaddingLeft()
                - titleView.getPaddingRight();
        titleView.setTextColor(mColorAutoCorrect);
        titleView.setText(importantNoticeTitle);
        titleView.setTextScaleX(1.0f); // Reset textScaleX.
        final float titleScaleX = getTextScaleX(importantNoticeTitle, width, titleView.getPaint());
        titleView.setTextScaleX(titleScaleX);*/
    }

    static void setLayoutWeight(final View v, final float weight, final int height) {
        final ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            final LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams)lp;
            llp.weight = weight;
            llp.width = 0;
            llp.height = height;
        }
    }

    private static float getTextScaleX(final CharSequence text, final int maxWidth,
            final TextPaint paint) {
        paint.setTextScaleX(1.0f);
        final int width = getTextWidth(text, paint);
        if (width <= maxWidth || maxWidth <= 0) {
            return 1.0f;
        }
        return maxWidth / (float)width;
    }

    private static CharSequence getEllipsizedText(final CharSequence text, final int maxWidth,
            final TextPaint paint) {
        if (text == null) {
            return null;
        }
        final float scaleX = getTextScaleX(text, maxWidth, paint);
        if (scaleX >= MIN_TEXT_XSCALE) {
            paint.setTextScaleX(scaleX);
            return text;
        }

        // Note that TextUtils.ellipsize() use text-x-scale as 1.0 if ellipsize is needed. To
        // get squeezed and ellipsized text, passes enlarged width (maxWidth / MIN_TEXT_XSCALE).
        final float upscaledWidth = maxWidth / MIN_TEXT_XSCALE;
        CharSequence ellipsized = TextUtils.ellipsize(
                text, paint, upscaledWidth, TextUtils.TruncateAt.MIDDLE);
        // For an unknown reason, ellipsized seems to return a text that does indeed fit inside the
        // passed width according to paint.measureText, but not according to paint.getTextWidths.
        // But when rendered, the text seems to actually take up as many pixels as returned by
        // paint.getTextWidths, hence problem.
        // To save this case, we compare the measured size of the new text, and if it's too much,
        // try it again removing the difference. This may still give a text too long by one or
        // two pixels so we take an additional 2 pixels cushion and call it a day.
        // TODO: figure out why getTextWidths and measureText don't agree with each other, and
        // remove the following code.
        final float ellipsizedTextWidth = getTextWidth(ellipsized, paint);
        if (upscaledWidth <= ellipsizedTextWidth) {
            ellipsized = TextUtils.ellipsize(
                    text, paint, upscaledWidth - (ellipsizedTextWidth - upscaledWidth) - 2,
                    TextUtils.TruncateAt.MIDDLE);
        }
        paint.setTextScaleX(MIN_TEXT_XSCALE);
        return ellipsized;
    }

    private static int getTextWidth(final CharSequence text, final TextPaint paint) {
        if (TextUtils.isEmpty(text)) {
            return 0;
        }
        final Typeface savedTypeface = paint.getTypeface();
        paint.setTypeface(getTextTypeface(text));
        final int len = text.length();
        final float[] widths = new float[len];
        final int count = paint.getTextWidths(text, 0, len, widths);
        int width = 0;
        for (int i = 0; i < count; i++) {
            width += Math.round(widths[i] + 0.5f);
        }
        paint.setTypeface(savedTypeface);
        return width;
    }

    private static Typeface getTextTypeface(final CharSequence text) {
        if (!(text instanceof SpannableString)) {
            return Typeface.DEFAULT;
        }

        final SpannableString ss = (SpannableString)text;
        final StyleSpan[] styles = ss.getSpans(0, text.length(), StyleSpan.class);
        if (styles.length == 0) {
            return Typeface.DEFAULT;
        }

        if (styles[0].getStyle() == Typeface.BOLD) {
            return Typeface.DEFAULT_BOLD;
        }
        // TODO: BOLD_ITALIC, ITALIC case?
        return Typeface.DEFAULT;
    }

    public void updateColor(ColorProfile colorProfile) {
        mColorValidTypedWord = colorProfile.getTextColor();
        mColorTypedWord = colorProfile.getTextColor();
        mColorAutoCorrect = colorProfile.getTextColor();
        mColorSuggested = colorProfile.getTextColor();
    }
}

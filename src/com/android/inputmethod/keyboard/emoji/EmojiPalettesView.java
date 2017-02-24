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

package com.android.inputmethod.keyboard.emoji;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;

import com.android.inputmethod.keyboard.KeyboardActionListener;
import com.android.inputmethod.keyboard.KeyboardLayoutSet;
import com.android.inputmethod.keyboard.KeyboardView;
import com.android.inputmethod.keyboard.actionrow.FrequentEmojiHandler;
import com.android.inputmethod.keyboard.emojifast.EmojiPageModel;
import com.android.inputmethod.keyboard.emojifast.EmojiPageView;
import com.android.inputmethod.keyboard.emojifast.EmojiPages;
import com.android.inputmethod.keyboard.emojifast.RecentEmojiPageModel;
import com.android.inputmethod.keyboard.internal.KeyDrawParams;
import com.android.inputmethod.keyboard.internal.KeyVisualAttributes;
import com.android.inputmethod.keyboard.internal.KeyboardIconsSet;
import com.android.inputmethod.latin.utils.ResourceUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.separ.neural.inputmethod.Utils.FontUtils;
import io.separ.neural.inputmethod.colors.ColorManager;
import io.separ.neural.inputmethod.colors.ColorProfile;
import io.separ.neural.inputmethod.indic.Constants;
import io.separ.neural.inputmethod.indic.R;
import io.separ.neural.inputmethod.indic.SubtypeSwitcher;

import static io.separ.neural.inputmethod.Utils.ColorUtils.colorProfile;
import static io.separ.neural.inputmethod.indic.Constants.NOT_A_COORDINATE;

/**
 * View class to implement Emoji palettes.
 * The Emoji keyboard consists of group of views layout/emoji_palettes_view.
 * <ol>
 * <li> Emoji category tabs.
 * <li> Delete button.
 * <li> Emoji keyboard pages that can be scrolled by swiping horizontally or by selecting a tab.
 * <li> Back to main keyboard button and enter button.
 * </ol>
 * Because of the above reasons, this class doesn't extend {@link KeyboardView}.
 */
public final class EmojiPalettesView extends LinearLayout implements OnTabChangeListener,
        ViewPager.OnPageChangeListener, View.OnClickListener, View.OnTouchListener,
        ColorManager.OnColorChange {
    private final int mFunctionalKeyBackgroundId;
    private final int mSpacebarBackgroundId;
    private final boolean mCategoryIndicatorEnabled;
    private final int mCategoryIndicatorDrawableResId;
    private final int mCategoryIndicatorBackgroundResId;
    private final int mCategoryPageIndicatorBackground;
    private final DeleteKeyOnTouchListener mDeleteKeyOnTouchListener;
    private EmojiPagerAdapter mEmojiPalettesAdapter;
    private final EmojiLayoutParams mEmojiLayoutParams;

    private LinearLayout mEmojiTopBar;
    private LinearLayout mActionBar;
    private ImageButton mDeleteKey;
    private TextView mAlphabetKeyLeft;
    //private TextView mAlphabetKeyRight;
    // TODO: Remove this workaround.
    private View mSpacebarIcon;
    private TabHost mTabHost;
    private ViewPager mEmojiPager;
    private int mCurrentPagerPosition = 0;
    //private View mSpacebar;

    private List<EmojiPageModel> models;
    private RecentEmojiPageModel recentModel;

    private KeyboardActionListener mKeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER;

    private final EmojiCategory mEmojiCategory;

    public EmojiPalettesView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.emojiPalettesViewStyle);
    }

    public EmojiPalettesView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        final TypedArray keyboardViewAttr = context.obtainStyledAttributes(attrs,
                R.styleable.KeyboardView, defStyle, R.style.KeyboardView);
        final int keyBackgroundId = keyboardViewAttr.getResourceId(
                R.styleable.KeyboardView_keyBackground, 0);
        mFunctionalKeyBackgroundId = keyboardViewAttr.getResourceId(
                R.styleable.KeyboardView_functionalKeyBackground, keyBackgroundId);
        mSpacebarBackgroundId = keyboardViewAttr.getResourceId(
                R.styleable.KeyboardView_spacebarBackground, keyBackgroundId);
        keyboardViewAttr.recycle();
        final KeyboardLayoutSet.Builder builder = new KeyboardLayoutSet.Builder(
                context, null /* editorInfo */);
        final Resources res = context.getResources();
        mEmojiLayoutParams = new EmojiLayoutParams(res);
        builder.setSubtype(SubtypeSwitcher.getInstance().getEmojiSubtype());
        builder.setKeyboardGeometry(ResourceUtils.getDefaultKeyboardWidth(res),
                mEmojiLayoutParams.mEmojiKeyboardHeight);
        final KeyboardLayoutSet layoutSet = builder.build();
        final TypedArray emojiPalettesViewAttr = context.obtainStyledAttributes(attrs,
                R.styleable.EmojiPalettesView, defStyle, R.style.EmojiPalettesView);
        mEmojiCategory = new EmojiCategory(PreferenceManager.getDefaultSharedPreferences(context),
                res, layoutSet, emojiPalettesViewAttr);
        mCategoryIndicatorEnabled = emojiPalettesViewAttr.getBoolean(
                R.styleable.EmojiPalettesView_categoryIndicatorEnabled, false);
        mCategoryIndicatorDrawableResId = emojiPalettesViewAttr.getResourceId(
                R.styleable.EmojiPalettesView_categoryIndicatorDrawable, 0);
        mCategoryIndicatorBackgroundResId = emojiPalettesViewAttr.getResourceId(
                R.styleable.EmojiPalettesView_categoryIndicatorBackground, 0);
        mCategoryPageIndicatorBackground = emojiPalettesViewAttr.getColor(
                R.styleable.EmojiPalettesView_categoryPageIndicatorBackground, 0);
        emojiPalettesViewAttr.recycle();
        mDeleteKeyOnTouchListener = new DeleteKeyOnTouchListener(context);
        initializePageModels();
    }

    private void initializePageModels() {
        this.models = new LinkedList<>();
        this.recentModel = new RecentEmojiPageModel(getContext());
        this.models.add(recentModel);
        this.models.addAll(EmojiPages.PAGES);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final Resources res = getContext().getResources();
        // The main keyboard expands to the entire this {@link KeyboardView}.
        final int width = ResourceUtils.getDefaultKeyboardWidth(res)
                + getPaddingLeft() + getPaddingRight();
        final int height = ResourceUtils.getDefaultKeyboardHeight(res)
                + res.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
                + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    private void addTab(final TabHost host, final int categoryId) {
        final String tabId = EmojiCategory.getCategoryName(categoryId, 0 /* categoryPageId */);
        final TabHost.TabSpec tspec = host.newTabSpec(tabId);
        tspec.setContent(R.id.emoji_keyboard_dummy);
        final ImageView iconView = (ImageView)LayoutInflater.from(getContext()).inflate(
                R.layout.emoji_keyboard_tab_icon, null);
        iconView.setBackgroundColor(mCategoryPageIndicatorBackground);
        iconView.setImageResource(mEmojiCategory.getCategoryTabIcon(categoryId));
        iconView.setContentDescription(mEmojiCategory.getAccessibilityDescription(categoryId));
        tspec.setIndicator(iconView);
        host.addTab(tspec);
    }

    @Override
    protected void onFinishInflate() {
        mEmojiTopBar = (LinearLayout)findViewById(R.id.emoji_top_bar);
        mTabHost = (TabHost)findViewById(R.id.emoji_category_tabhost);
        mTabHost.setup();
        for (final EmojiCategory.CategoryProperties properties
                : mEmojiCategory.getShownCategories()) {
            addTab(mTabHost, properties.mCategoryId);
        }
        mTabHost.setOnTabChangedListener(this); //TODO
        final TabWidget tabWidget = mTabHost.getTabWidget();
        tabWidget.setStripEnabled(mCategoryIndicatorEnabled);
        if (mCategoryIndicatorEnabled) {
            // On TabWidget's strip, what looks like an indicator is actually a background.
            // And what looks like a background are actually left and right drawables.
            tabWidget.setBackgroundResource(mCategoryIndicatorDrawableResId);
            tabWidget.setLeftStripDrawable(mCategoryIndicatorBackgroundResId);
            tabWidget.setRightStripDrawable(mCategoryIndicatorBackgroundResId);
        }

        mEmojiPalettesAdapter = new EmojiPagerAdapter(getContext(),
                models,
                new EmojiPageView.EmojiSelectionListener() {
                    @Override
                    public void onEmojiSelected(String emoji) {
                        recentModel.onCodePointSelected(emoji);
                        mKeyboardActionListener.onEmojiInput(emoji);
                    }
                });

        mEmojiPager = (ViewPager)findViewById(R.id.emoji_keyboard_pager);
        mEmojiPager.setAdapter(mEmojiPalettesAdapter);
        if (recentModel.getEmoji().length == 0)
            mEmojiPager.setCurrentItem(1);
        mEmojiPager.setOnPageChangeListener(this);
        mEmojiPager.setOffscreenPageLimit(0);
        mEmojiPager.setPersistentDrawingCache(PERSISTENT_NO_CACHE);
        mEmojiLayoutParams.setPagerProperties(mEmojiPager);

        setCurrentCategoryId(mEmojiCategory.getCurrentCategoryId(), true /* force */);

        mActionBar = (LinearLayout)findViewById(R.id.emoji_action_bar);
        mEmojiLayoutParams.setActionBarProperties(mActionBar);

        // deleteKey depends only on OnTouchListener.
        mDeleteKey = (ImageButton)findViewById(R.id.emoji_keyboard_delete);
        mDeleteKey.setBackgroundResource(mFunctionalKeyBackgroundId);
        mDeleteKey.setTag(Constants.CODE_DELETE);
        mDeleteKey.setOnTouchListener(mDeleteKeyOnTouchListener);

        // {@link #mAlphabetKeyLeft}, {@link #mAlphabetKeyRight, and spaceKey depend on
        // {@link View.OnClickListener} as well as {@link View.OnTouchListener}.
        // {@link View.OnTouchListener} is used as the trigger of key-press, while
        // {@link View.OnClickListener} is used as the trigger of key-release which does not occur
        // if the event is canceled by moving off the finger from the view.
        // The text on alphabet keys are set at
        // {@link #startEmojiPalettes(String,int,float,Typeface)}.
        mAlphabetKeyLeft = (TextView)findViewById(R.id.emoji_keyboard_alphabet_left);
        mAlphabetKeyLeft.setBackgroundResource(mFunctionalKeyBackgroundId);
        mAlphabetKeyLeft.setTag(Constants.CODE_ALPHA_FROM_EMOJI);
        mAlphabetKeyLeft.setOnTouchListener(this);
        mAlphabetKeyLeft.setOnClickListener(this);
        /*mAlphabetKeyRight = (TextView)findViewById(R.id.emoji_keyboard_alphabet_right);
        mAlphabetKeyRight.setBackgroundResource(mFunctionalKeyBackgroundId);
        mAlphabetKeyRight.setTag(Constants.CODE_ALPHA_FROM_EMOJI);
        mAlphabetKeyRight.setOnTouchListener(this);
        mAlphabetKeyRight.setOnClickListener(this);*/
        /*mSpacebar = findViewById(R.id.emoji_keyboard_space);
        mSpacebar.setBackgroundResource(mSpacebarBackgroundId);
        mSpacebar.setTag(Constants.CODE_SPACE);
        mSpacebar.setOnTouchListener(this);
        mSpacebar.setOnClickListener(this);
        mEmojiLayoutParams.setKeyProperties(mSpacebar);
        mSpacebarIcon = findViewById(R.id.emoji_keyboard_space_icon);*/
    }

    public static class EmojiPagerAdapter extends PagerAdapter
    {
        private Context                context;
        private List<EmojiPageModel>   pages;
        private EmojiPageView.EmojiSelectionListener listener;

        public EmojiPagerAdapter(@NonNull Context context,
                                 @NonNull List<EmojiPageModel> pages,
                                 @Nullable EmojiPageView.EmojiSelectionListener listener)
        {
            super();
            this.context  = context;
            this.pages    = pages;
            this.listener = listener;
        }

        @Override
        public CharSequence getPageTitle(int index){
            return "Text";
        }

        @Override
        public int getCount() {
            return pages.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            EmojiPageView page = new EmojiPageView(context);
            page.setModel(pages.get(position));
            page.setEmojiSelectedListener(listener);
            container.addView(page);
            return page;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            EmojiPageView current = (EmojiPageView) object;
            current.onSelected();
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    public void onColorChange(ColorProfile newProfile){
        int darkBackground = colorProfile.getPrimaryDark();
        int iconColor = colorProfile.getIcon();
        if(mActionBar != null){
            mActionBar.setBackgroundColor(darkBackground);
        }
        if(mDeleteKey != null) {
            mDeleteKey.setBackgroundColor(darkBackground);
            mDeleteKey.setColorFilter(iconColor);
        }
        if(mAlphabetKeyLeft != null) {
            mAlphabetKeyLeft.setBackgroundColor(darkBackground);
            mAlphabetKeyLeft.setTextColor(iconColor);
            mAlphabetKeyLeft.setText(mAlphabetKeyLeft.getText());
        }
        /*if(mAlphabetKeyRight != null) {
            mAlphabetKeyRight.setBackgroundColor(darkBackground);
            mAlphabetKeyRight.setTextColor(iconColor);
            mAlphabetKeyRight.setText(mAlphabetKeyRight.getText());
        }*/
        if(mTabHost != null){
            TabWidget tabWidget = mTabHost.getTabWidget();
            for(int i=0; i<tabWidget.getChildCount(); ++i) {
                ImageView currentTab = (ImageView)tabWidget.getChildTabViewAt(i);
                currentTab.setBackgroundColor(darkBackground);
                currentTab.setColorFilter(iconColor);
            }
            tabWidget.setBackgroundColor(darkBackground);
        }
        if(mEmojiTopBar != null)
            mEmojiTopBar.setBackgroundColor(darkBackground);
        /*if(mSpacebar != null) {
            Drawable newBackground = mSpacebar.getBackground();
            newBackground.setColorFilter(colorProfile.getPrimary(), PorterDuff.Mode.OVERLAY);
            mSpacebar.setBackground(newBackground);
        }*/
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        // Add here to the stack trace to nail down the {@link IllegalArgumentException} exception
        // in MotionEvent that sporadically happens.
        // TODO: Remove this override method once the issue has been addressed.
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onTabChanged(final String tabId) {
        final int categoryId = mEmojiCategory.getCategoryId(tabId);
        setCurrentCategoryId(categoryId, false /* force */);
    }

    @Override
    public void onPageSelected(final int position) {
        final Pair<Integer, Integer> newPos =
                mEmojiCategory.getCategoryIdAndPageIdFromPagePosition(position);
        setCurrentCategoryId(newPos.first /* categoryId */, false /* force */);
        mCurrentPagerPosition = position;
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        // Ignore this message. Only want the actual page selected.
    }

    @Override
    public void onPageScrolled(final int position, final float positionOffset,
            final int positionOffsetPixels) {
    }

    /**
     * Called from {@link EmojiPageKeyboardView} through {@link android.view.View.OnTouchListener}
     * interface to handle touch events from View-based elements such as the space bar.
     * Note that this method is used only for observing {@link MotionEvent#ACTION_DOWN} to trigger
     * {@link KeyboardActionListener#onPressKey}. {@link KeyboardActionListener#onReleaseKey} will
     * be covered by {@link #onClick} as long as the event is not canceled.
     */
    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        final Object tag = v.getTag();
        if (!(tag instanceof Integer)) {
            return false;
        }
        final int code = (Integer) tag;
        mKeyboardActionListener.onPressKey(
                code, 0 /* repeatCount */, true /* isSinglePointer */);
        // It's important to return false here. Otherwise, {@link #onClick} and touch-down visual
        // feedback stop working.
        return false;
    }

    /**
     * Called from {@link EmojiPageKeyboardView} through {@link android.view.View.OnClickListener}
     * interface to handle non-canceled touch-up events from View-based elements such as the space
     * bar.
     */
    @Override
    public void onClick(View v) {
        final Object tag = v.getTag();
        if (!(tag instanceof Integer)) {
            return;
        }
        final int code = (Integer) tag;
        mKeyboardActionListener.onCodeInput(code, NOT_A_COORDINATE, NOT_A_COORDINATE,
                false /* isKeyRepeat */);
        mKeyboardActionListener.onReleaseKey(code, false /* withSliding */);
    }

    public void setHardwareAcceleratedDrawingEnabled(final boolean enabled) {
        if (!enabled) return;
        // TODO: Should use LAYER_TYPE_SOFTWARE when hardware acceleration is off?
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    private static void setupAlphabetKey(final TextView alphabetKey, final String label,
            final KeyDrawParams params) {
        alphabetKey.setText(label);
        alphabetKey.setTextColor(params.mFunctionalTextColor);
        alphabetKey.setTextSize(TypedValue.COMPLEX_UNIT_PX, params.mLabelSize);
        alphabetKey.setTypeface(FontUtils.getLocaleTypeface());
    }

    public void startEmojiPalettes(final String switchToAlphaLabel,
            final KeyVisualAttributes keyVisualAttr, final KeyboardIconsSet iconSet) {
        final int deleteIconResId = iconSet.getIconResourceId(KeyboardIconsSet.NAME_DELETE_KEY);
        if (deleteIconResId != 0) {
            mDeleteKey.setImageResource(deleteIconResId);
        }
        final int spacebarResId = iconSet.getIconResourceId(KeyboardIconsSet.NAME_SPACE_KEY);
        if (spacebarResId != 0) {
            // TODO: Remove this workaround to place the spacebar icon.
            mSpacebarIcon.setBackgroundResource(spacebarResId);
        }
        final KeyDrawParams params = new KeyDrawParams();
        params.updateParams(mEmojiLayoutParams.getActionBarHeight(), keyVisualAttr);
        setupAlphabetKey(mAlphabetKeyLeft, switchToAlphaLabel, params);
        //setupAlphabetKey(mAlphabetKeyRight, switchToAlphaLabel, params);
        mEmojiPager.setAdapter(mEmojiPalettesAdapter);
        mEmojiPager.setCurrentItem(mCurrentPagerPosition);
        setCurrentCategoryId(mEmojiCategory.getCurrentCategoryId(), true);
    }

    public void stopEmojiPalettes() {
        recentModel.persist();
        mEmojiPager.setAdapter(null);
    }

    public void setKeyboardActionListener(final KeyboardActionListener listener) {
        mKeyboardActionListener = listener;
        mDeleteKeyOnTouchListener.setKeyboardActionListener(mKeyboardActionListener);
    }

    private void setCurrentCategoryId(final int categoryId, final boolean force) {
        final int oldCategoryId = mEmojiCategory.getCurrentCategoryId();
        if (oldCategoryId == categoryId && !force)
            return;
        mEmojiCategory.setCurrentCategoryId(categoryId);
        final int newTabId = mEmojiCategory.getTabIdFromCategoryId(categoryId);
        mTabHost.setCurrentTab(newTabId);
        mEmojiPager.setCurrentItem(newTabId, true);
    }

    private static class DeleteKeyOnTouchListener implements OnTouchListener {
        static final long MAX_REPEAT_COUNT_TIME = TimeUnit.SECONDS.toMillis(30);
        final long mKeyRepeatStartTimeout;
        final long mKeyRepeatInterval;

        public DeleteKeyOnTouchListener(Context context) {
            final Resources res = context.getResources();
            mKeyRepeatStartTimeout = res.getInteger(R.integer.config_key_repeat_start_timeout);
            mKeyRepeatInterval = res.getInteger(R.integer.config_key_repeat_interval);
            mTimer = new CountDownTimer(MAX_REPEAT_COUNT_TIME, mKeyRepeatInterval) {
                @Override
                public void onTick(long millisUntilFinished) {
                    final long elapsed = MAX_REPEAT_COUNT_TIME - millisUntilFinished;
                    if (elapsed < mKeyRepeatStartTimeout) {
                        return;
                    }
                    onKeyRepeat();
                }
                @Override
                public void onFinish() {
                    onKeyRepeat();
                }
            };
        }

        /** Key-repeat state. */
        private static final int KEY_REPEAT_STATE_INITIALIZED = 0;
        // The key is touched but auto key-repeat is not started yet.
        private static final int KEY_REPEAT_STATE_KEY_DOWN = 1;
        // At least one key-repeat event has already been triggered and the key is not released.
        private static final int KEY_REPEAT_STATE_KEY_REPEAT = 2;

        private KeyboardActionListener mKeyboardActionListener =
                KeyboardActionListener.EMPTY_LISTENER;

        // TODO: Do the same things done in PointerTracker
        private final CountDownTimer mTimer;
        private int mState = KEY_REPEAT_STATE_INITIALIZED;
        private int mRepeatCount = 0;

        public void setKeyboardActionListener(final KeyboardActionListener listener) {
            mKeyboardActionListener = listener;
        }

        @Override
        public boolean onTouch(final View v, final MotionEvent event) {
            switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(v);
                return true;
            case MotionEvent.ACTION_MOVE:
                final float x = event.getX();
                final float y = event.getY();
                if (x < 0.0f || v.getWidth() < x || y < 0.0f || v.getHeight() < y) {
                    // Stop generating key events once the finger moves away from the view area.
                    onTouchCanceled(v);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                onTouchUp(v);
                return true;
            }
            return false;
        }

        private void handleKeyDown() {
            mKeyboardActionListener.onPressKey(
                    Constants.CODE_DELETE, mRepeatCount, true /* isSinglePointer */);
        }

        private void handleKeyUp() {
            mKeyboardActionListener.onCodeInput(Constants.CODE_DELETE,
                    NOT_A_COORDINATE, NOT_A_COORDINATE, false /* isKeyRepeat */);
            mKeyboardActionListener.onReleaseKey(
                    Constants.CODE_DELETE, false /* withSliding */);
            ++mRepeatCount;
        }

        private void onTouchDown(final View v) {
            mTimer.cancel();
            mRepeatCount = 0;
            handleKeyDown();
            v.setPressed(true /* pressed */);
            mState = KEY_REPEAT_STATE_KEY_DOWN;
            mTimer.start();
        }

        private void onTouchUp(final View v) {
            mTimer.cancel();
            if (mState == KEY_REPEAT_STATE_KEY_DOWN) {
                handleKeyUp();
            }
            v.setPressed(false /* pressed */);
            mState = KEY_REPEAT_STATE_INITIALIZED;
        }

        private void onTouchCanceled(final View v) {
            mTimer.cancel();
            v.setBackgroundColor(Color.TRANSPARENT);
            mState = KEY_REPEAT_STATE_INITIALIZED;
        }

        // Called by {@link #mTimer} in the UI thread as an auto key-repeat signal.
        void onKeyRepeat() {
            switch (mState) {
            case KEY_REPEAT_STATE_INITIALIZED:
                // Basically this should not happen.
                break;
            case KEY_REPEAT_STATE_KEY_DOWN:
                // Do not call {@link #handleKeyDown} here because it has already been called
                // in {@link #onTouchDown}.
                handleKeyUp();
                mState = KEY_REPEAT_STATE_KEY_REPEAT;
                break;
            case KEY_REPEAT_STATE_KEY_REPEAT:
                handleKeyDown();
                handleKeyUp();
                break;
            }
        }
    }
}

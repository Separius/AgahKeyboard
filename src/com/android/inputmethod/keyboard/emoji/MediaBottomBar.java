package com.android.inputmethod.keyboard.emoji;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.inputmethod.keyboard.ChangeRichModeListener;
import com.android.inputmethod.keyboard.KeyboardActionListener;
import com.android.inputmethod.keyboard.internal.KeyDrawParams;
import com.android.inputmethod.keyboard.internal.KeyVisualAttributes;
import com.android.inputmethod.keyboard.internal.KeyboardIconsSet;
import com.android.inputmethod.latin.utils.ResourceUtils;

import java.util.concurrent.TimeUnit;

import io.separ.neural.inputmethod.Utils.FontUtils;
import io.separ.neural.inputmethod.colors.ColorManager;
import io.separ.neural.inputmethod.colors.ColorProfile;
import io.separ.neural.inputmethod.indic.Constants;
import io.separ.neural.inputmethod.indic.R;

import static io.separ.neural.inputmethod.indic.Constants.NOT_A_COORDINATE;

/**
 * Created by sepehr on 2/24/17.
 */

public class MediaBottomBar extends LinearLayout implements View.OnTouchListener, ColorManager.OnColorChange, View.OnClickListener {
    private final DeleteKeyOnTouchListener mDeleteKeyOnTouchListener;
    private final ModeSwitchOnTouchListenet mModeSwitchOnTouchListener;
    private ImageButton mDeleteKey;
    private ImageButton mStickerSwitch;
    private ImageButton mEmojiSwitch;
    private TextView mAlphabetKeyLeft;
    private KeyboardActionListener mKeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER;
    private EmojiLayoutParams mEmojiLayoutParams;
    private final int mFunctionalKeyBackgroundId;
    private ChangeRichModeListener mModeSwitchListener;

    public MediaBottomBar(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.emojiPalettesViewStyle);
    }

    public MediaBottomBar(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        final TypedArray keyboardViewAttr = context.obtainStyledAttributes(attrs,
                R.styleable.KeyboardView, defStyle, R.style.KeyboardView);
        final int keyBackgroundId = keyboardViewAttr.getResourceId(
                R.styleable.KeyboardView_keyBackground, 0);
        mFunctionalKeyBackgroundId = keyboardViewAttr.getResourceId(
                R.styleable.KeyboardView_functionalKeyBackground, keyBackgroundId);
        keyboardViewAttr.recycle();
        final Resources res = context.getResources();
        mEmojiLayoutParams = new EmojiLayoutParams(res);
        mDeleteKeyOnTouchListener = new DeleteKeyOnTouchListener(context);
        mModeSwitchOnTouchListener = new ModeSwitchOnTouchListenet();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final Resources res = getContext().getResources();
        final int width = ResourceUtils.getDefaultKeyboardWidth(res)
                + getPaddingLeft() + getPaddingRight();
        final int height = (res.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onFinishInflate() {
        mEmojiLayoutParams.setActionBarProperties(this);
        mDeleteKey = (ImageButton)findViewById(R.id.emoji_keyboard_delete);
        mDeleteKey.setBackgroundResource(mFunctionalKeyBackgroundId);
        mDeleteKey.setTag(Constants.CODE_DELETE);
        mDeleteKey.setOnTouchListener(mDeleteKeyOnTouchListener);
        mAlphabetKeyLeft = (TextView)findViewById(R.id.emoji_keyboard_alphabet_left);
        mAlphabetKeyLeft.setBackgroundResource(mFunctionalKeyBackgroundId);
        mAlphabetKeyLeft.setTag(Constants.CODE_ALPHA_FROM_EMOJI);
        mAlphabetKeyLeft.setOnTouchListener(this);
        mAlphabetKeyLeft.setOnClickListener(this);
        mEmojiSwitch = (ImageButton)findViewById(R.id.switch_to_emoji);
        mEmojiSwitch.setBackgroundResource(mFunctionalKeyBackgroundId);
        mEmojiSwitch.setOnTouchListener(mModeSwitchOnTouchListener);
        mEmojiSwitch.setTag("rich_emoji_mode");
        mStickerSwitch = (ImageButton)findViewById(R.id.switch_to_sticker);
        mStickerSwitch.setBackgroundResource(mFunctionalKeyBackgroundId);
        mStickerSwitch.setOnTouchListener(mModeSwitchOnTouchListener);
        mStickerSwitch.setTag("rich_sticker_mode");
        mModeSwitchOnTouchListener.setChilds(mEmojiSwitch, mStickerSwitch);
        mEmojiSwitch.setAlpha(1.f);
        mStickerSwitch.setAlpha(0.5f);
    }

    public void onColorChange(ColorProfile newProfile){
        int secondary = newProfile.getSecondary();
        int iconColor = newProfile.getIconOnSecondary();
        setBackgroundColor(secondary);
        if(mDeleteKey != null) {
            mDeleteKey.setBackgroundColor(secondary);
            mDeleteKey.setColorFilter(iconColor);
        }
        if(mAlphabetKeyLeft != null) {
            mAlphabetKeyLeft.setBackgroundColor(secondary);
            mAlphabetKeyLeft.setTextColor(iconColor);
            //mAlphabetKeyLeft.setText(mAlphabetKeyLeft.getText());
        }
        if(mEmojiSwitch != null){
            mEmojiSwitch.setBackgroundColor(secondary);
            mEmojiSwitch.setColorFilter(iconColor);
        }
        if(mStickerSwitch != null){
            mStickerSwitch.setBackgroundColor(secondary);
            mStickerSwitch.setColorFilter(iconColor);
        }
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        // Add here to the stack trace to nail down the {@link IllegalArgumentException} exception
        // in MotionEvent that sporadically happens.
        // TODO: Remove this override method once the issue has been addressed.
        return super.dispatchTouchEvent(ev);
    }

    private static void setupAlphabetKey(final TextView alphabetKey, final String label,
                                         final KeyDrawParams params) {
        alphabetKey.setText(label);
        //alphabetKey.setTextColor(params.mFunctionalTextColor);
        //alphabetKey.setTextSize(TypedValue.COMPLEX_UNIT_PX, params.mLabelSize);
        alphabetKey.setTypeface(FontUtils.getLocaleTypeface());
    }

    public void startMediaBottomBar(final String switchToAlphaLabel,
                                   final KeyVisualAttributes keyVisualAttr, final KeyboardIconsSet iconSet) {
        final int deleteIconResId = iconSet.getIconResourceId(KeyboardIconsSet.NAME_DELETE_KEY);
        if (deleteIconResId != 0)
            mDeleteKey.setImageResource(deleteIconResId);
        final KeyDrawParams params = new KeyDrawParams();
        params.updateParams(mEmojiLayoutParams.getActionBarHeight(), keyVisualAttr);
        setupAlphabetKey(mAlphabetKeyLeft, switchToAlphaLabel, params);
    }

    public void setKeyboardActionListener(final KeyboardActionListener listener) {
        mKeyboardActionListener = listener;
        mDeleteKeyOnTouchListener.setKeyboardActionListener(mKeyboardActionListener);
    }

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

    public void setSwitchActionListener(ChangeRichModeListener listener){
        mModeSwitchListener = listener;
    }

    private class ModeSwitchOnTouchListenet implements OnTouchListener {
        View v1, v2;

        @Override
        public boolean onTouch(final View v, final MotionEvent event) {
            if(mModeSwitchListener != null) {
                final Object tag = v.getTag();
                if (!(tag instanceof String))
                    return false;
                final int res = mModeSwitchListener.change((String)tag);
                if(res == 1){
                    v1.setAlpha(1.0f);
                    v2.setAlpha(0.5f);
                }
                else if(res == 2){
                    v1.setAlpha(0.5f);
                    v2.setAlpha(1.0f);
                }
            }
            return true;
        }

        public void setChilds(View mEmojiSwitch, View mStickerSwitch) {
            v1 = mEmojiSwitch;
            v2 = mStickerSwitch;
        }
    }
}

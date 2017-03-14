package com.android.inputmethod.keyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.android.inputmethod.keyboard.emoji.EmojiPalettesView;
import com.android.inputmethod.keyboard.emoji.MediaBottomBar;
import com.android.inputmethod.keyboard.internal.KeyVisualAttributes;
import com.android.inputmethod.keyboard.internal.KeyboardIconsSet;
import com.android.inputmethod.keyboard.sticker.StickerView;

import io.separ.neural.inputmethod.colors.ColorManager;
import io.separ.neural.inputmethod.indic.R;

/**
 * Created by sepehr on 3/5/17.
 */

public class RichMediaView extends LinearLayout implements ChangeRichModeListener{
    private EmojiPalettesView mEmojiPalettesView;
    private MediaBottomBar mMediaBottomBar;
    private StickerView mStickerView;
    private boolean emojiIsActive;

    public void setGone() {
        mEmojiPalettesView.setVisibility(View.GONE);
        mMediaBottomBar.setVisibility(View.GONE);
        mStickerView.setVisibility(View.GONE);
        mEmojiPalettesView.stopEmojiPalettes();
    }

    public void changeLayout(){
        if(emojiIsActive) {
            mEmojiPalettesView.startEmojiPalettes();
            mEmojiPalettesView.setVisibility(VISIBLE);
            mStickerView.setVisibility(GONE);
        }else {
            mEmojiPalettesView.setVisibility(View.GONE);
            mStickerView.setVisibility(VISIBLE);
        }
    }

    public void setEmojiKeyboard(String switchToAlpha, KeyVisualAttributes keyVisualAttributes, KeyboardIconsSet keyboardIconsSet) {
        changeLayout();
        mMediaBottomBar.setVisibility(View.VISIBLE);
        mMediaBottomBar.startMediaBottomBar(switchToAlpha, keyVisualAttributes, keyboardIconsSet);
    }

    public boolean isShowingEmojiPalettes() {
        return (mStickerView != null && mStickerView.isShown())||(mEmojiPalettesView != null && mEmojiPalettesView.isShown());
    }

    public View getVisibleKeyboardView() {
        if(emojiIsActive)
            return mEmojiPalettesView;
        return mStickerView;
    }

    public void deallocateMemory() {
        if (mEmojiPalettesView != null)
            mEmojiPalettesView.stopEmojiPalettes();
    }

    public RichMediaView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public void setUp(View view, boolean hardwareAccelerated, KeyboardActionListener actionListener){
        mEmojiPalettesView = (EmojiPalettesView) this.findViewById(R.id.emoji_palettes_view);
        mMediaBottomBar = (MediaBottomBar) view.findViewById(R.id.media_bottom_bar);
        mEmojiPalettesView.setHardwareAcceleratedDrawingEnabled(hardwareAccelerated);
        mEmojiPalettesView.setKeyboardActionListener(actionListener);
        mMediaBottomBar.setKeyboardActionListener(actionListener);
        mMediaBottomBar.setSwitchActionListener(this);
        ColorManager.addObserver(mEmojiPalettesView);
        ColorManager.addObserver(mMediaBottomBar);
        mStickerView = (StickerView) this.findViewById(R.id.sticker_view);
        emojiIsActive = true;
        ColorManager.addObserver(mStickerView);
    }

    @Override
    public int change(String to){
        if(to == null)
            return 0;
        if(to.equals("rich_emoji_mode")) {
            emojiIsActive = true;
            changeLayout();
            return 1;
        }
        else if(to.equals("rich_sticker_mode")) {
            emojiIsActive = false;
            changeLayout();
            return 2;
        }
        else
            return 0;
    }

    /*@Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final Resources res = getContext().getResources();
        // The main keyboard expands to the entire this {@link KeyboardView}.
        final int width = ResourceUtils.getDefaultKeyboardWidth(res)
                + getPaddingLeft() + getPaddingRight();
        final int height = ResourceUtils.getDefaultKeyboardHeight(res)
                //- res.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
                + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }*/
}

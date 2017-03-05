package com.android.inputmethod.keyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.android.inputmethod.keyboard.emoji.EmojiPalettesView;
import com.android.inputmethod.keyboard.emoji.MediaBottomBar;
import com.android.inputmethod.keyboard.sticker.StickerView;
import com.android.inputmethod.keyboard.internal.KeyVisualAttributes;
import com.android.inputmethod.keyboard.internal.KeyboardIconsSet;

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
        emojiIsActive = false;
        ColorManager.addObserver(mStickerView);
    }

    @Override
    public void change(String to){
        if(to == null)
            return;
        if(to.equals("rich_emoji_mode"))
            emojiIsActive = true;
        else if(to.equals("rich_sticker_mode"))
            emojiIsActive = false;
        else
            return;
        changeLayout();
    }
}

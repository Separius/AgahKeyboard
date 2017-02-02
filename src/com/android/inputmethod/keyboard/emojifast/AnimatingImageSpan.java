package com.android.inputmethod.keyboard.emojifast;

import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

/**
 * Created by sepehr on 2/2/17.
 */
public class AnimatingImageSpan extends ImageSpan {
    public AnimatingImageSpan(Drawable drawable, Drawable.Callback callback) {
        super(drawable, ALIGN_BOTTOM);
        drawable.setCallback(callback);
    }
}

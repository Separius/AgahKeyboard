package com.android.inputmethod.keyboard.emojifast;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.widget.TextView;

import io.separ.neural.inputmethod.indic.R;

/**
 * Created by sepehr on 2/2/17.
 */
public class EmojiSpan extends AnimatingImageSpan {
    private final int            size;
    private final Paint.FontMetricsInt fm;

    public EmojiSpan(@NonNull Drawable drawable, @NonNull TextView tv) {
        super(drawable, tv);
        fm   = tv.getPaint().getFontMetricsInt();
        size = fm != null ? Math.abs(fm.descent) + Math.abs(fm.ascent)
                : tv.getResources().getDimensionPixelSize(R.dimen.conversation_item_body_text_size);
        getDrawable().setBounds(0, 0, size, size);
    }

    @Override public int getSize(Paint paint, CharSequence text, int start, int end,
                                 Paint.FontMetricsInt fm)
    {
        if (fm != null && this.fm != null) {
            fm.ascent  = this.fm.ascent;
            fm.descent = this.fm.descent;
            fm.top     = this.fm.top;
            fm.bottom  = this.fm.bottom;
            return size;
        } else {
            return super.getSize(paint, text, start, end, fm);
        }
    }
}

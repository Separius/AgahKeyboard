package io.separ.neural.inputmethod.indic.inlinesettings;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import io.separ.neural.inputmethod.indic.R;

public final class InlineSettingsView extends LinearLayout{
    public InlineSettingsView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.emojiPalettesViewStyle);
    }
    public InlineSettingsView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }
}

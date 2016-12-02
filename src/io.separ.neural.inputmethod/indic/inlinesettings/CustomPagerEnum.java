package io.separ.neural.inputmethod.indic.inlinesettings;

import io.separ.neural.inputmethod.indic.R;

/**
 * Created by sepehr on 12/2/16.
 */

public enum CustomPagerEnum {
    RED("Png", R.layout.view_red),
    BLUE("Gif", R.layout.view_red),
    ORANGE("Webp", R.layout.view_red);

    private String mTitleResId;
    private int mLayoutResId;

    CustomPagerEnum(String titleResId, int layoutResId) {
        mTitleResId = titleResId;
        mLayoutResId = layoutResId;
    }

    public String getTitleResId() {
        return mTitleResId;
    }

    public int getLayoutResId() {
        return mLayoutResId;
    }
}

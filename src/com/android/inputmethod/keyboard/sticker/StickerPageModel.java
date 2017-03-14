package com.android.inputmethod.keyboard.sticker;

import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;

/**
 * Created by sepehr on 3/5/17.
 */
public class StickerPageModel {
    @AttrRes
    private final int iconAttr;
    @NonNull
    private final String[] pack;
    @NonNull
    private final String name;

    int getIconAttr(){
        return iconAttr;
    }

    String[] getPack(){
        return pack;
    }

    String getName(){
        return name;
    }

    public StickerPageModel(@NonNull String name, @AttrRes int iconAttr, @NonNull String[] pack) {
        this.name = name;
        this.iconAttr  = iconAttr;
        this.pack   = pack;
    }
}

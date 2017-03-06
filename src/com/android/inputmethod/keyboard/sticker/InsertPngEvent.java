package com.android.inputmethod.keyboard.sticker;

/**
 * Created by sepehr on 3/5/17.
 */
public class InsertPngEvent {
    public String base;
    public String name;
    public boolean isSticker;

    public InsertPngEvent(boolean isSticker, String base, String name) {
        this.isSticker = isSticker;
        this.base = base;
        this.name = name;
    }
}

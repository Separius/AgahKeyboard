package com.android.inputmethod.keyboard.emojifast;

/**
 * Created by sepehr on 2/1/17.
 */

public interface EmojiPageModel {
    int getIconAttr();
    String[] getEmoji();
    boolean hasSpriteMap();
    String getSprite();
    boolean isDynamic();
}

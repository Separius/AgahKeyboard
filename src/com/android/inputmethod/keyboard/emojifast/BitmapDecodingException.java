package com.android.inputmethod.keyboard.emojifast;

/**
 * Created by sepehr on 2/2/17.
 */
public class BitmapDecodingException extends Exception {

    public BitmapDecodingException(String s) {
        super(s);
    }

    public BitmapDecodingException(Exception nested) {
        super(nested);
    }
}

package com.android.inputmethod.keyboard.emojifast;

/**
 * Created by sepehr on 2/1/17.
 */
public interface FutureTaskListener<V> {
    public void onSuccess(V result);
    public void onFailure(Throwable error);
}

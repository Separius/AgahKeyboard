package com.android.inputmethod.keyboard.emojifast;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by sepehr on 2/2/17.
 */
public class Util {
    public static Handler handler = new Handler(Looper.getMainLooper());

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static void assertMainThread() {
        if (!isMainThread()) {
            throw new AssertionError("Main-thread assertion failed.");
        }
    }

    public static void runOnMain(Runnable runnable) {
        if (isMainThread()) runnable.run();
        else                handler.post(runnable);
    }
}

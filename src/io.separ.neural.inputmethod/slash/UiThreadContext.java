package io.separ.neural.inputmethod.slash;

import android.os.Looper;

/**
 * Created by sepehr on 3/2/17.
 */
public class UiThreadContext {
    public static void assertUiThread() {
        if (!isInUiThread()) {
            throw new RuntimeException("This call must be in UI thread");
        }
    }

    public static boolean isInUiThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    public static void assertBackgroundThread() {
        if (isInUiThread()) {
            throw new RuntimeException("This call must be in background thread");
        }
    }
}

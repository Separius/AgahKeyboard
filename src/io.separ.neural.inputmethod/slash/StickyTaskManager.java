package io.separ.neural.inputmethod.slash;

import android.os.Bundle;

/**
 * Created by sepehr on 3/2/17.
 */
public class StickyTaskManager {
    public static final String CONTEXT_ID = "CONTEXT_ID";
    private static long idCounter;
    protected final long affinityId;

    static {
        idCounter = System.currentTimeMillis();
    }

    public StickyTaskManager(Bundle inState) {
        UiThreadContext.assertUiThread();
        if (inState == null || !inState.containsKey(CONTEXT_ID)) {
            long j = idCounter;
            idCounter = 1 + j;
            this.affinityId = j;
            return;
        }
        this.affinityId = inState.getLong(CONTEXT_ID);
    }

    public void onSaveInstanceState(Bundle outState) {
        UiThreadContext.assertUiThread();
        outState.putLong(CONTEXT_ID, this.affinityId);
    }

    public boolean isTaskForMe(StickyTask stickyTask) {
        return stickyTask.affinityId == this.affinityId;
    }
}

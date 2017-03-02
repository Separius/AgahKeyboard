package io.separ.neural.inputmethod.slash;

/**
 * Created by sepehr on 3/2/17.
 */
public abstract class StickyTask extends Task {
    protected final long affinityId;

    protected StickyTask(StickyTaskManager taskManager) {
        if (taskManager != null) {
            this.affinityId = taskManager.affinityId;
        } else {
            this.affinityId = -1;
        }
    }
}

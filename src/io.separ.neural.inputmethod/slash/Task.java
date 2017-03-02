package io.separ.neural.inputmethod.slash;

import android.content.Context;

/**
 * Created by sepehr on 3/2/17.
 */
public abstract class Task {
    protected transient BaseTaskQueue myQueue;

    protected abstract boolean handleError(Context context, Throwable th);

    protected abstract void run(Context context) throws Throwable;

    public void setMyQueue(BaseTaskQueue myQueue) {
        this.myQueue = myQueue;
    }

    protected void onComplete(Context context) {
    }
}

package io.separ.neural.inputmethod.slash;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sepehr on 3/2/17.
 */

public abstract class BaseQuerySearchTask extends Task {
    private String authorizedStatus;
    private List<RSearchItem> items;
    private volatile boolean mCanceled;
    private boolean mItemsSetCalled;
    private boolean networkError;
    private final String query;
    private final String service;

    public boolean isNetworkError() {
        return this.networkError;
    }

    public void setNetworkError(boolean networkError) {
        this.networkError = networkError;
    }

    public BaseQuerySearchTask(String service, String query) {
        this.mItemsSetCalled = false;
        this.service = service;
        this.query = query;
        this.items = new ArrayList();
    }

    public String getService() {
        return this.service;
    }

    public String getQuery() {
        return this.query;
    }

    public void setResults(List<RSearchItem> items) {
        setResults(items, null);
    }

    public void setResults(List<RSearchItem> items, String authorizedStatus) {
        this.mItemsSetCalled = true;
        this.items = items;
        this.authorizedStatus = authorizedStatus;
        for(RSearchItem i : items){
            i.setPreviewUrl(i.getUrl());
        }
    }

    protected boolean handleError(Context context, Throwable e) {
        if (e instanceof NetworkException)
            return true;
        return false;
    }

    protected void onComplete(Context context) {
        if (!isCanceled()) {
            if (this.mItemsSetCalled) {
                EventBusExt.getDefault().post(new SearchResultsEvent(getQuery(), this.service, this.items, this.authorizedStatus));
            } else {
                EventBusExt.getDefault().post(new SearchRetryErrorEvent(getService(), getQuery(), this.networkError));
            }
        }
    }

    public String toString() {
        return getClass().getSimpleName() + " - Param: " + this.service + " , " + (TextUtils.isEmpty(this.query) ? "NULL" : this.query);
    }

    public void cancel() {
        this.mCanceled = true;
    }

    public boolean isCanceled() {
        return this.mCanceled;
    }
}

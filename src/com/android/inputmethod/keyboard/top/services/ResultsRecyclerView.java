package com.android.inputmethod.keyboard.top.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.separ.neural.inputmethod.indic.R;
import io.separ.neural.inputmethod.slash.RSearchItem;
import io.separ.neural.inputmethod.slash.RServiceItem;

/**
 * Created by sepehr on 3/2/17.
 */
public class ResultsRecyclerView extends RecyclerView {
    private SearchItemArrayAdapter mAdapter;
    private boolean mChangeListHeight;
    private RServiceItem mCurrentService;
    private String mCurrentSlash;
    private Runnable mLoadNextPhotosPage;
    private String mPreviousAuthorizedStatus;
    private String mPreviousSlash;
    private ArrayList<RSearchItem> mSearchResults;

    class C04573 implements Runnable {
        C04573() {
        }

        public void run() {
            ResultsRecyclerView.this.mAdapter.setPageLoadingListener(null);
            //TaskQueue.loadQueueDefault(NeuralApplication.getInstance()).execute(new PhotosSearchItemsTask(ResultsRecyclerView.this.mCurrentSlash, "", ResultsRecyclerView.this.mAdapter.getItemCount()));
        }
    }

    public ResultsRecyclerView(Context context) {
        super(context);
        this.mCurrentSlash = "";
        this.mCurrentService = null;
        this.mPreviousSlash = "";
        this.mPreviousAuthorizedStatus = "";
        this.mChangeListHeight = true;
        this.mLoadNextPhotosPage = new C04573();
        this.mChangeListHeight = false;
        setClipToPadding(false);
        setPadding((int) TypedValue.applyDimension(1, 10.0f, getResources().getDisplayMetrics()), 0, 0, 0);
        init();
    }

    public ResultsRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ResultsRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mCurrentSlash = "";
        this.mCurrentService = null;
        this.mPreviousSlash = "";
        this.mPreviousAuthorizedStatus = "";
        this.mChangeListHeight = true;
        this.mLoadNextPhotosPage = new C04573();
        init();
    }

    private void init() {
        this.mAdapter = new SearchItemArrayAdapter(getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), 0, false);
        setHasFixedSize(true);
        setLayoutManager(layoutManager);
        setItemAnimator(null);
        setAdapter(this.mAdapter);
    }

    public void setCurrentSlash(String mCurrentSlash) {
        this.mCurrentSlash = mCurrentSlash;
    }

    public void setPreviousSlash(String mPreviousSlash) {
        this.mPreviousSlash = mPreviousSlash;
    }

    public String getCurrentSlash() {
        return this.mCurrentSlash;
    }

    public String getPreviousSlash() {
        return this.mPreviousSlash;
    }

    public boolean serviceChanged() {
        return !this.mCurrentSlash.equals(this.mPreviousSlash);
    }

    public boolean setService(String slash) {
        if (this.mCurrentSlash.equals(slash)) {
            return false;
        }
        this.mCurrentSlash = slash;
        this.mCurrentService = RServiceItem.serviceItemHashMap.get(slash);
        this.mAdapter.setServiceItem(this.mCurrentService);
        if (this.mChangeListHeight) {
            if (RServiceItem.PHOTOS.equals(slash)) {
                getLayoutParams().height = (int) getContext().getResources().getDimension(R.dimen.search_results_container_height_big);
                getAdapter().setImageHeightBig();
            } else {
                getLayoutParams().height = (int) getContext().getResources().getDimension(R.dimen.search_results_container_height);
                getAdapter().setImageHeightSmall();
            }
        }
        return true;
    }

    public void setItems(List<RSearchItem> items) {
        if (this.mAdapter.getItemCount() > 0 && RServiceItem.PHOTOS.equals(this.mCurrentSlash) && !isShowingLoadingItems()) {
            this.mAdapter.addAll((Collection) items);
        } else if (items == null || items.isEmpty()) {
            setEmptyItem();
        } else {
            this.mAdapter.setItems(items);
        }
        if (RServiceItem.PHOTOS.equals(this.mCurrentSlash)) {
            //this.mAdapter.setPageLoadingListener(this.mLoadNextPhotosPage);
        } else {
            this.mAdapter.setPageLoadingListener(null);
        }
    }

    private void invalidateReactViewHack() {
        scrollBy(1, 0);
        scrollBy(-1, 0);
    }

    public void setEmptyItem() {
        getAdapter().clear();
        RSearchItem item = new RSearchItem();
        item.setDisplayType(RSearchItem.GENERIC_MESSAGE_TYPE);
        item.setTitle(getResources().getString(R.string.service_result_empty));
        this.mAdapter.add(item);
        //invalidateReactViewHack();
    }

    public void setLoadingItems() {
        getAdapter().clear();
        RSearchItem item = new RSearchItem();
        item.setDisplayType(RSearchItem.LOADING_TYPE);
        this.mAdapter.add(item);
        this.mAdapter.add(item);
        this.mAdapter.add(item);
    }

    public boolean trySetUnauthPreviewItems(String authorizedStatus, boolean categoryRequiresAuth) {
        if (requiresPermissionAccess()) {
            setPermissionItem();
            return true;
        } else
            return false;
        /*else if (!requiresUnauthPreview(authorizedStatus, categoryRequiresAuth)) {
            return false;
        } else {
            setUnauthPreviewItems();
            return true;
        }*/
    }

    public boolean requiresPermissionAccess() {
        if (RServiceItem.CONTACTS.equals(mCurrentService.getSlash()) && ContextCompat.checkSelfPermission(getContext(), "android.permission.READ_CONTACTS") != 0)
            return true;
        return false;
    }

    private boolean requiresUnauthPreview(String authorizedStatus, boolean categoryRequiresAuth) {
        if (this.mCurrentService == null) {
            return false;
        }
        return true;
    }

    private void setPermissionItem() {
        getAdapter().clear();
        RSearchItem connectItem = new RSearchItem();
        connectItem.setDisplayType(RSearchItem.PERMISSION_REQUIRED_TYPE);
        connectItem.setService(mCurrentService.getSlash());
        connectItem.setOutput(getContext().getString(R.string.contacts_permission));
        connectItem.setTitle("Contacts");
        this.mAdapter.add(connectItem);
        //invalidateReactViewHack();
    }

    private void setUnauthPreviewItems() {
        getAdapter().clear();
        RSearchItem connectItem = new RSearchItem();
        connectItem.setDisplayType(RSearchItem.CONNECT_TO_USE_TYPE);
        connectItem.setService(this.mCurrentSlash);
        this.mAdapter.add(connectItem);
        //invalidateReactViewHack();
    }

    public boolean isShowingUnauthPreviewItems() {
        return getAdapter().getItemCount() > 0 && RSearchItem.CONNECT_TO_USE_TYPE.equals(((RSearchItem) getAdapter().getItem(0)).getDisplayType());
    }

    public boolean isShowingLoadingItems() {
        return getAdapter().getItemCount() > 0 && (RSearchItem.LOADING_TYPE.equals(((RSearchItem) getAdapter().getItem(0)).getDisplayType()) || RSearchItem.PERMISSION_REQUIRED_TYPE.equals(((RSearchItem) getAdapter().getItem(0)).getDisplayType()));
    }

    public void setType(String type) {
        /*if (this.mSearchResults != null) {
            this.mSearchResults.removeChangeListener(this.mSharesListener);
        }
        if (RSearchItem.MEDIA_TYPE.equals(type)) {
            this.mSearchResults = this.mRealm.where(RSearchItem.class).contains("displayType", RSearchItem.MEDIA_TYPE, Case.INSENSITIVE).findAllSortedAsync("addedTimeStamp", Sort.DESCENDING);
            this.mSearchResults.addChangeListener(this.mSharesListener);
            return;
        }
        this.mSearchResults = this.mRealm.where(RSearchItem.class).contains("displayType", RSearchItem.DEFAULT_TYPE, Case.INSENSITIVE).findAllSortedAsync("addedTimeStamp", Sort.DESCENDING);
        this.mSearchResults.addChangeListener(this.mSharesListener);*/
    }

    public void drop() {
        /*if (this.mSearchResults != null) {
            this.mSearchResults.removeChangeListener(this.mSharesListener);
        }
        if (this.mCurrentService != null) {
            this.mCurrentService.removeChangeListener(this.mServiceListener);
        }*/
    }

    public SearchItemArrayAdapter getAdapter() {
        return (SearchItemArrayAdapter) super.getAdapter();
    }

    public static void openPreview(Context context, String url) {
        Intent intent = new Intent("android.intent.action.VIEW").setData(Uri.parse(url));
        intent.addFlags(268435456);
        context.startActivity(intent);
    }

    public void setAuthorizedStatusListener(Runnable listener) {
        //this.mAuthorizedListener = listener;
    }

    private void listenToAuthorizationChange() {
        /*if (this.mCurrentService != null) {
            this.mPreviousAuthorizedStatus = this.mCurrentService.getAuthorizedStatus();
            this.mCurrentService.addChangeListener(this.mServiceListener);
        }*/
    }
}

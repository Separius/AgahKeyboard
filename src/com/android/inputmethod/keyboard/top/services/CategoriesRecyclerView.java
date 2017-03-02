package com.android.inputmethod.keyboard.top.services;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * Created by sepehr on 3/2/17.
 */
public class CategoriesRecyclerView extends RecyclerView {
    private CategoriesArrayAdapter mAdapter;
    private RealmResults<RCategory> mCategoriesResults;
    private String mCurrentSlash;

    public CategoriesRecyclerView(Context context) {
        super(context);
        this.mCurrentSlash = "";
        init();
    }

    public CategoriesRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CategoriesRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mCurrentSlash = "";
        init();
    }

    private void init() {
        this.mAdapter = new CategoriesArrayAdapter(getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), 0, false);
        setHasFixedSize(true);
        setLayoutManager(layoutManager);
        setItemAnimator(null);
        setAdapter(this.mAdapter);
    }

    public int setService(String slash) {
        if (!this.mCurrentSlash.equals(slash)) {
            getAdapter().setSelectedItem(0);
            this.mCurrentSlash = slash;
            this.mCategoriesResults = this.mRealm.where(RCategory.class).equalTo(NotificationCompatApi24.CATEGORY_SERVICE, Integer.valueOf(((RServiceItem) this.mRealm.where(RServiceItem.class).equalTo("slash", this.mCurrentSlash).findFirst()).getId())).findAllSorted("order");
            getAdapter().setItems(this.mCategoriesResults);
        }
        if (this.mCategoriesResults == null) {
            return 0;
        }
        return this.mCategoriesResults.size();
    }

    public boolean firstCategoryRequireAuth() {
        return firstCategoryRequireAuth(this.mCategoriesResults);
    }

    public static boolean firstCategoryRequireAuth(RealmResults<RCategory> results) {
        if (results == null) {
            return false;
        }
        for (int i = 0; i <= results.size() - 1; i++) {
            if (!"aac".equals(((RCategory) results.get(i)).getType())) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return this.mCategoriesResults == null || this.mCategoriesResults.isEmpty();
    }

    public void drop() {
    }

    public CategoriesArrayAdapter getAdapter() {
        return (CategoriesArrayAdapter) super.getAdapter();
    }
}

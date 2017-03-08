package com.android.inputmethod.keyboard.top.services;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

import io.separ.neural.inputmethod.slash.RCategory;

/**
 * Created by sepehr on 3/2/17.
 */
public class CategoriesRecyclerView extends RecyclerView {
    private CategoriesArrayAdapter mAdapter;
    private List<RCategory> mCategoriesResults;
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
            /*if(slash.equals("giphy") || slash.equals("foursquare"))
                this.mCategoriesResults = RCategory.categoriesHashMap.get(slash);
            else*/
                this.mCategoriesResults = new ArrayList<>();
            getAdapter().setItems(this.mCategoriesResults);
        }
        if (this.mCategoriesResults == null) {
            return 0;
        }
        return this.mCategoriesResults.size();
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

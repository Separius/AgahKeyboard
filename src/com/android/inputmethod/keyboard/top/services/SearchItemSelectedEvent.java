package com.android.inputmethod.keyboard.top.services;

import io.separ.neural.inputmethod.slash.RSearchItem;

/**
 * Created by sepehr on 3/2/17.
 */
public class SearchItemSelectedEvent {
    private RSearchItem item;
    public final Integer position;
    private String slash;

    public SearchItemSelectedEvent(String slash, RSearchItem item, Integer position) {
        this.slash = slash;
        this.item = item;
        this.position = position;
    }

    public RSearchItem getItem() {
        return this.item;
    }

    public String getSlash() {
        return this.slash;
    }
}

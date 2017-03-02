package io.separ.neural.inputmethod.slash;

import com.google.gson.annotations.SerializedName;

/**
 * Created by sepehr on 3/2/17.
 */

public class RCategory{
    private String action;
    @SerializedName("default")
    private boolean defaultState;
    private int id;
    private String name;
    private int order;
    private String params;
    private int service;
    private String type;

    public String getAction() {
        return action;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}

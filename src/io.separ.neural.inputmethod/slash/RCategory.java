package io.separ.neural.inputmethod.slash;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public RCategory(){}

    public RCategory(String name, String type, String action){
        this.name = name;
        this.type = type;
        this.action = action;
    }

    public final static HashMap<String, List<RCategory>> categoriesHashMap = new HashMap<>();

    static {
        categoriesHashMap.put("foursquare", new ArrayList<RCategory>(6));
        categoriesHashMap.get("foursquare").add(0, new RCategory("Trending", "act", "trending"));
        categoriesHashMap.get("foursquare").add(1, new RCategory("Near Me", "act", "nearme"));
        categoriesHashMap.get("foursquare").add(2, new RCategory("Fun", "act", "fun"));
        categoriesHashMap.get("foursquare").add(3, new RCategory("Food", "act", "food"));
        categoriesHashMap.get("foursquare").add(4, new RCategory("Coffee", "act", "Coffee"));
        categoriesHashMap.get("foursquare").add(5, new RCategory("Nightlife", "act", "nightlife"));
        categoriesHashMap.put("giphy", new ArrayList<RCategory>(4));
        categoriesHashMap.get("giphy").add(0, new RCategory("Trending", "act", "trending"));
        categoriesHashMap.get("giphy").add(1, new RCategory("LoL", "sug", "lol"));
        categoriesHashMap.get("giphy").add(2, new RCategory("Cool", "sug", "cool"));
        categoriesHashMap.get("giphy").add(3, new RCategory("funny", "sug", "funny"));
    }
}

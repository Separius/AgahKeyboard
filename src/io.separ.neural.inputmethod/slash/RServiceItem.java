package io.separ.neural.inputmethod.slash;

import com.google.gson.annotations.SerializedName;

/**
 * Created by sepehr on 3/2/17.
 */

public class RServiceItem {
    public static final String AUTHORIZED = "authorized";
    public static final int BASE_KEY_CODE = -5000;
    public static final String CONTACTS = "contacts";
    public static final String FACEBOOK = "facebook";
    public static final String FOURSQUARE = "foursquare";
    public static final String LOCATION = "location";
    public static final int MIN_LOCAL_ID = 123456;
    public static final String PHOTOS = "photos";
    public static final String PIN = "pin";
    public static final String RECENT = "recent";
    public static String RECENTS = null;
    public static String SEARCH = null;
    public static final String UNAUTHORIZED = "unauthorized";
    @SerializedName("auth_post_message")
    private String authPostMessage;
    @SerializedName("auth_pre_message")
    private String authPreMessage;
    private String authorizedStatus;
    @SerializedName("categories_enabled")
    private boolean categoriesEnabled;
    @SerializedName("description")
    private String description;
    @SerializedName("description_short")
    private String descriptionShort;
    private boolean enabled;
    @SerializedName("id")
    private int id;
    @SerializedName("image_baricon")
    private String imageBar;
    @SerializedName("image_dark")
    private String imageDark;
    @SerializedName("image_light")
    private String imageLight;
    private long lastUsed;
    private boolean local;
    @SerializedName("location_aware")
    private boolean location_aware;
    private boolean myslash;
    @SerializedName("name")
    private String name;
    @SerializedName("order")
    private int order;
    private int order2;
    @SerializedName("prepopulate")
    private boolean prepopulate;
    private String provider;
    private String resId;
    @SerializedName("search_placeholder")
    private String searchPlaceholder;
    @SerializedName("slash")
    private String slash;
    private String socialapp;
    private String staticContent;
    @SerializedName("static")
    private boolean staticService;
    private boolean mySlash;

    public String getImageLight() {
        return imageLight;
    }

    public String getSlash() {
        return slash;
    }

    public String getImageDark() {
        return imageDark;
    }

    public String getResId() {
        return resId;
    }

    public boolean isMyslash() {
        return myslash;
    }

    public String getImageBar() {
        return imageBar;
    }

    public String getSearchPlaceholder() {
        return searchPlaceholder;
    }

    public void setSlash(String slash) {
        this.slash = slash;
    }

    public void setMySlash(boolean mySlash) {
        this.mySlash = mySlash;
    }

    public boolean isLocation_aware() {
        return location_aware;
    }
}

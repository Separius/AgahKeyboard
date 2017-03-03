package io.separ.neural.inputmethod.slash;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

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

    public static HashMap<String, RServiceItem> serviceItemHashMap = new HashMap<>();

    public RServiceItem(){}

    public RServiceItem(boolean hasCategory, String desc, String shortDesc, boolean isEnabled, int serviceId,
                        boolean isLocal, boolean location, boolean prepop, String placeHolder, String slash){
        this.categoriesEnabled = hasCategory;
        this.description = desc;
        this.descriptionShort = shortDesc;
        this.enabled = isEnabled;
        this.id = serviceId;
        this.local = isLocal;
        this.location_aware = location;
        this.prepopulate = prepop;
        this.searchPlaceholder = placeHolder;
        this.slash = slash;
    }

    static {
        serviceItemHashMap.put("giphy", new RServiceItem(true, "Giphy lets you search and share trending GIFS, reactions, stickers, & more",
                "Share animated GIFs from Giphy", true, 223, false, false, true, "Search OMG, vintage, and more GIFs...", "giphy"));
        serviceItemHashMap.put("maps", new RServiceItem(false, "maps lets you share your location or any address on Google Maps",
                "Search and Share locations on Google Maps", true, 217, false, true, false, "Search for locations and addresses...", "maps"));
        serviceItemHashMap.put("google", new RServiceItem(false, "Google lets you search the web and share any Google link within your keyboard",
                "Search the web right from your keyboard", true, 210, false, false, false, "Search the web...", "google"));
        serviceItemHashMap.put("contacts", new RServiceItem(false, "Contacts lets you easily share your Contact information with friends",
                "Easily share contact info from your Contacts", true, 233, true, false, false, "Search for your contacts...", "contacts"));
        serviceItemHashMap.put("foursquare", new RServiceItem(true, "Foursquare lets you search and share venues, restaurants, coffee shops, & more",
                "Share nearby restaurants and venues on Foursquare", true, 220, false, true, true, "Search nearby places and restaurants...", "foursquare"));
    }
}

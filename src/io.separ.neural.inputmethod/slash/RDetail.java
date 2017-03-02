package io.separ.neural.inputmethod.slash;

import com.google.gson.annotations.SerializedName;

/**
 * Created by sepehr on 3/2/17.
 */
public class RDetail {
    @SerializedName("actual_id")
    private String actualId;
    private String album;
    private String channel;
    private RLocation coordinates;
    @SerializedName("created_at")
    private String createdAt;
    private String date;
    private String distance;
    private String duration;
    private String favorites;
    private String license;
    @SerializedName("place_id")
    private String placeId;
    @SerializedName("playback_count")
    private String playbackCount;
    private String price;
    private String rating;
    private String retweets;
    @SerializedName("view_count")
    private String viewCount;
    private String views;
}

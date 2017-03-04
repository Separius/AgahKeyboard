package io.separ.neural.inputmethod.slash;

import com.google.gson.annotations.SerializedName;

/**
 * Created by sepehr on 3/2/17.
 */
public class RSearchItem {
    public static final String CONNECT_TO_USE_TYPE = "connect_to_use";
    public static final String DEFAULT_TYPE = "default";
    public static final String GENERIC_MESSAGE_TYPE = "generic_message";
    public static final String LOADING_TYPE = "loading";
    public static final String MEDIA_TYPE = "media";
    public static final String PERMISSION_REQUIRED_TYPE = "permission_required";
    private long addedTimeStamp;
    private RDetail detail;
    @SerializedName("display_type")
    private String displayType;
    private RImage image;
    @SerializedName("image_large")
    private RImage imageLarge;
    private String output;
    private String service;
    @SerializedName("slash-short")
    private String slashShort;
    private String subtitle;
    private String title;
    private String uid;
    private String uri;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setImage(RImage image) {
        this.image = image;
    }

    private String url;
    private CharSequence previewUrl;
    private String correctService;
    private String anyOutput;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getTitle() {
        return title;
    }

    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getOutput() {
        return output;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public CharSequence getPreviewUrl() {
        return previewUrl;
    }

    public RImage getImage() {
        return image;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public RDetail getDetail() {
        return detail;
    }

    public String getDisplayType() {
        return displayType;
    }

    public String getCorrectService() {
        return correctService;
    }

    public String getService() {
        return service;
    }

    public String getSlashShort() {
        return slashShort;
    }

    public String getAnyOutput() {
        return anyOutput;
    }

    public String getUrl() {
        return url;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public void setDetail(RDetail detail) {
        this.detail = detail;
    }
}

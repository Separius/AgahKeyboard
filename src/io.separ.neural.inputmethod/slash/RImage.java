package io.separ.neural.inputmethod.slash;

/**
 * Created by sepehr on 3/2/17.
 */
public class RImage {
    private String height;
    private int resId;
    private String size;
    private String url;
    private String width;
    private float aspectRatio;

    public float getAspectRatio() {
        return aspectRatio;
    }

    public String getUrl() {
        return url;
    }

    public int getResId() {
        return resId;
    }

    public String getWidth() {
        return width;
    }

    public String getHeight() {
        return height;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

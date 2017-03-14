package com.android.inputmethod.keyboard.top.services.tenor;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

import io.separ.neural.inputmethod.slash.RImage;
import io.separ.neural.inputmethod.slash.RSearchItem;

import static io.separ.neural.inputmethod.slash.RSearchItem.MEDIA_TYPE;

/**
 * Created by sepehr on 3/4/17.
 */
public class TenorResult {
    private TenorComposite composite;
    private double created;
    @SerializedName("hasaudio")
    private boolean hasAudio;
    private String id;
    private ArrayList<TenorMediaCollection> media;
    private ArrayList<String> tags;
    private String title;
    public String url;

    public RSearchItem toSearchItem() {
        RSearchItem retVal = new RSearchItem();
        retVal.setTitle(null);
        retVal.setSubtitle(null);
        retVal.setDetail(null);
        retVal.setOutput(title);
        retVal.setPreviewUrl(media.get(0).gif.url);
        retVal.setUrl(media.get(0).tinygif.url);
        RImage tmp = new RImage();
        tmp.setUrl(media.get(0).tinygif.url);
        retVal.setImage(tmp);
        retVal.setService("giphy");
        retVal.setDisplayType(MEDIA_TYPE);
        return retVal;
    }
}

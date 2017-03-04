package com.android.inputmethod.keyboard.top.services.tenor;

import io.separ.neural.inputmethod.slash.NetworkException;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Created by sepehr on 3/4/17.
 */

public interface TenorApi {
    @GET("/search?safesearch=strict&key=W6Z9DLZUSVTD")
    TenorResponse getSearchResults(@Query("tag") String str) throws NetworkException;

    @GET("/tags?type=featured,explore&key=W6Z9DLZUSVTD")
    TenorTagCollection getTags() throws NetworkException;

    @GET("/trending?key=W6Z9DLZUSVTD")
    TenorResponse getTrending() throws NetworkException;
}

package io.separ.neural.inputmethod.slash;

/**
 * Created by sepehr on 3/2/17.
 */
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Query;

public interface MonkeyApi {
    @GET("/search?format=json")
    SearchResult getSearchResultsWithAction(@Header("Authorization") String str, @Query("s") String str2, @Query("a") String str3) throws NetworkException;

    @GET("/search?format=json")
    SearchResult getSearchResultsWithAction(@Header("Authorization") String str, @Query("s") String str2, @Query("a") String str3, @Query("lat") double d, @Query("long") double d2) throws NetworkException;

    @GET("/search?format=json")
    SearchResult getSearchResultsWithQuery(@Header("Authorization") String str, @Query("s") String str2, @Query("q") String str3) throws NetworkException;

    @GET("/search?format=json")
    SearchResult getSearchResultsWithQuery(@Header("Authorization") String str, @Query("s") String str2, @Query("q") String str3, @Query("lat") double d, @Query("long") double d2) throws NetworkException;
}
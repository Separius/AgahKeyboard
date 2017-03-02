package io.separ.neural.inputmethod.slash;

/**
 * Created by sepehr on 3/2/17.
 */
public class SearchRetryErrorEvent {
    private final boolean networkError;
    private String query;
    private String source;

    public SearchRetryErrorEvent(String source, String query, boolean networkError) {
        this.query = query;
        this.source = source;
        this.networkError = networkError;
    }

    public String getSource() {
        return this.source;
    }

    public String getQuery() {
        return this.query;
    }

    public boolean isNetworkError() {
        return this.networkError;
    }
}

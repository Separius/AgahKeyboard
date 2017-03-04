package io.separ.neural.inputmethod.slash;

import android.content.Context;
import android.text.TextUtils;

import com.android.inputmethod.keyboard.top.services.tenor.TenorApiManager;

/**
 * Created by sepehr on 3/5/17.
 */
public class TenorQuerySearchTask extends BaseQuerySearchTask {
    private String query;

    public TenorQuerySearchTask(String query) {
        super("giphy", query);
        this.query = query;
    }

    protected void run(Context context) throws Exception {
        SearchResult result;
        if(TextUtils.isEmpty(query))
            result = TenorApiManager.getInstance(true).getTrending().toSearchResult();
        else
            result = TenorApiManager.getInstance(true).getSearchResults(query).toSearchResult();
        setResults(result.getItems(), result.getMetaStatus());
    }

    protected boolean handleError(Context context, Throwable e) {
        return true;
    }
}

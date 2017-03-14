package io.separ.neural.inputmethod.slash;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;

import io.nlopez.smartlocation.SmartLocation;

/**
 * Created by sepehr on 3/2/17.
 */

public class ServiceQuerySearchTask extends BaseQuerySearchTask {
    private String mAction;
    private boolean mIsLocationAware;
    private boolean mUseCache;

    public ServiceQuerySearchTask(String slash, String query, String action, boolean isLocationAware, boolean useCache) {
        super(slash, query);
        this.mAction = action;
        this.mIsLocationAware = isLocationAware;
        this.mUseCache = useCache;
    }

    protected void run(Context context) throws Exception {
        SearchResult result;
        Location location = null;
        if (this.mIsLocationAware) {
            try {
                location = SmartLocation.with(context).location().getLastLocation();
                if (LatLngUtils.isEmpty(location))
                    SmartLocation.with(context).location().oneFix().start(null);
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        String header = "";
        if (TextUtils.isEmpty(this.mAction)) {
            if (!this.mIsLocationAware || LatLngUtils.isEmpty(location)) {
                result = MonkeyApiManager.getInstance(this.mUseCache).getSearchResultsWithQuery(header, getService(), getQuery());
            } else {
                result = MonkeyApiManager.getInstance(this.mUseCache).getSearchResultsWithQuery(header, getService(), getQuery(), location.getLatitude(), location.getLongitude());
            }
        } else if (!this.mIsLocationAware || LatLngUtils.isEmpty(location)) {
            result = MonkeyApiManager.getInstance(this.mUseCache).getSearchResultsWithQuery(header, getService(), "tehran");
        } else {
            result = MonkeyApiManager.getInstance(this.mUseCache).getSearchResultsWithAction(header, getService(), this.mAction, location.getLatitude(), location.getLongitude());
        }
        setResults(result.getItems(), result.getMetaStatus());
    }

    protected boolean handleError(Context context, Throwable e) {
        return true;
    }
}

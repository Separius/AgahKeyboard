package com.android.inputmethod.keyboard.top.services.tenor;

import com.google.gson.GsonBuilder;

import io.separ.neural.inputmethod.slash.BaseApiManager;
import io.separ.neural.inputmethod.slash.NeuralApplication;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

/**
 * Created by sepehr on 3/4/17.
 */

public class TenorApiManager extends BaseApiManager {
    private static TenorApi mCachedTenor;
    private static RestAdapter mCachedRestAdapter;
    private static TenorApi mTenor;
    private static RestAdapter mRestAdapter;

    static {
        mTenor = null;
        mRestAdapter = null;
        mCachedRestAdapter = null;
        mCachedTenor = null;
    }

    private TenorApiManager() {
    }

    public static synchronized TenorApi getCacheInstance() {
        TenorApi tenorApi;
        synchronized (TenorApiManager.class) {
            if (mCachedRestAdapter == null) {
                mCachedRestAdapter = getRestAdapter(new OkClient(BaseApiManager.createCachedClient(NeuralApplication.getInstance().getApplicationContext())));
                mCachedTenor = mCachedRestAdapter.create(TenorApi.class);
            }
            tenorApi = mCachedTenor;
        }
        return tenorApi;
    }

    public static synchronized TenorApi getInstance(boolean useCache) {
        TenorApi cacheInstance;
        synchronized (TenorApiManager.class) {
            if (useCache) {
                cacheInstance = getCacheInstance();
            } else {
                cacheInstance = getInstance();
            }
        }
        return cacheInstance;
    }

    public static synchronized TenorApi getInstance() {
        TenorApi monkeyApi;
        synchronized (TenorApiManager.class) {
            if (mRestAdapter == null) {
                mRestAdapter = getRestAdapter(new OkClient(BaseApiManager.getHttpClient()));
                mTenor = mRestAdapter.create(TenorApi.class);
            }
            monkeyApi = mTenor;
        }
        return monkeyApi;
    }

    private static RestAdapter getRestAdapter(OkClient client) {
        return new RestAdapter.Builder().setEndpoint("https://api.tenor.co/v1").setErrorHandler(BaseApiManager.getErrorHandler()).setLogLevel(RestAdapter.LogLevel.NONE).setClient(client).setConverter(new GsonConverter(new GsonBuilder().create())).build();
    }
}

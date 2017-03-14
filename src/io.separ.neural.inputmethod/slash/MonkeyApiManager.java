package io.separ.neural.inputmethod.slash;

import com.google.gson.GsonBuilder;

import io.separ.neural.inputmethod.Utils.DeviceUuidFactory;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

/**
 * Created by sepehr on 3/2/17.
 */

public class MonkeyApiManager extends BaseApiManager {
    private static MonkeyApi mCachedMonkey;
    private static RestAdapter mCachedRestAdapter;
    private static MonkeyApi mMonkey;
    private static RestAdapter mRestAdapter;

    /* renamed from: co.touchlab.inputmethod.latin.monkey.network.MonkeyApiManager.2 */
    static class C04212 implements RequestInterceptor {
        C04212() {
        }

        public void intercept(RequestFacade request) {
            request.addQueryParam("v", "2");
            request.addQueryParam("trackerId", DeviceUuidFactory.getDeviceUuid().toString());
            request.addQueryParam("client", "android_10309");
        }
    }

    static {
        mMonkey = null;
        mRestAdapter = null;
        mCachedRestAdapter = null;
        mCachedMonkey = null;
    }

    private MonkeyApiManager() {
    }

    public static synchronized MonkeyApi getCacheInstance() {
        MonkeyApi monkeyApi;
        synchronized (MonkeyApiManager.class) {
            if (mCachedRestAdapter == null) {
                mCachedRestAdapter = getRestAdapter(new OkClient(BaseApiManager.createCachedClient(NeuralApplication.getInstance().getApplicationContext())));
                mCachedMonkey = (MonkeyApi) mCachedRestAdapter.create(MonkeyApi.class);
            }
            monkeyApi = mCachedMonkey;
        }
        return monkeyApi;
    }

    public static synchronized MonkeyApi getInstance(boolean useCache) {
        MonkeyApi cacheInstance;
        synchronized (MonkeyApiManager.class) {
            if (useCache) {
                cacheInstance = getCacheInstance();
            } else {
                cacheInstance = getInstance();
            }
        }
        return cacheInstance;
    }

    public static synchronized MonkeyApi getInstance() {
        MonkeyApi monkeyApi;
        synchronized (MonkeyApiManager.class) {
            if (mRestAdapter == null) {
                mRestAdapter = getRestAdapter(new OkClient(BaseApiManager.getHttpClient()));
                mMonkey = (MonkeyApi) mRestAdapter.create(MonkeyApi.class);
            }
            monkeyApi = mMonkey;
        }
        return monkeyApi;
    }

    private static RestAdapter getRestAdapter(OkClient client) {
        return new RestAdapter.Builder().setEndpoint("https://platform.tapslash.com/api").setErrorHandler(BaseApiManager.getErrorHandler()).setLogLevel(RestAdapter.LogLevel.NONE).setClient(client).setConverter(new GsonConverter(new GsonBuilder().create())).setRequestInterceptor(new C04212()).build();
    }
}

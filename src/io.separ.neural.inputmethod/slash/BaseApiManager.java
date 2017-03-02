package io.separ.neural.inputmethod.slash;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RetrofitError;

/**
 * Created by sepehr on 3/2/17.
 */

public class BaseApiManager {
    protected static final int CONNECT_TIMEOUT = 20;
    protected static final int READ_TIMEOUT = 20;

    /* renamed from: co.touchlab.inputmethod.latin.monkey.network.BaseApiManager.1 */
    static class C04161 implements RequestInterceptor {
        C04161() {
        }

        public void intercept(RequestFacade request) {
            request.addHeader("User-Agent", System.getProperty("http.agent") + " (App v1.3.09)");
            request.addHeader("Api", Integer.toString(2));
        }
    }

    /* renamed from: co.touchlab.inputmethod.latin.monkey.network.BaseApiManager.2 */
    static class C04172 implements ErrorHandler {
        C04172() {
        }

        public Throwable handleError(RetrofitError cause) {
            if (cause.getKind() == RetrofitError.Kind.NETWORK) {
                return new NetworkException(cause.getCause());
            }
            return cause;
        }
    }

    /* renamed from: co.touchlab.inputmethod.latin.monkey.network.BaseApiManager.3 */
    static class C04183 implements Interceptor {
        final /* synthetic */ Context val$context;

        C04183(Context context) {
            this.val$context = context;
        }

        public Response intercept(Chain chain) throws IOException {
            return chain.proceed(chain.request().newBuilder().build()).newBuilder().removeHeader("Pragma").removeHeader("Cache-Control").header("Cache-Control", BaseApiManager.isOnline(this.val$context) ? "public, max-age=300" : "public, only-if-cached, max-stale=3600").build();
        }
    }

    /* renamed from: co.touchlab.inputmethod.latin.monkey.network.BaseApiManager.4 */
    static class C04194 implements Interceptor {
        final /* synthetic */ Context val$context;

        C04194(Context context) {
            this.val$context = context;
        }

        public Response intercept(Chain chain) throws IOException {
            return chain.proceed(chain.request().newBuilder().build()).newBuilder().removeHeader("Pragma").removeHeader("Cache-Control").header("Cache-Control", BaseApiManager.isOnline(this.val$context) ? "public, max-age=300" : "public, only-if-cached, max-stale=3600").build();
        }
    }

    protected static RequestInterceptor getRequestInterceptor() {
        return new C04161();
    }

    protected static ErrorHandler getErrorHandler() {
        return new C04172();
    }

    protected static OkHttpClient getHttpClient() {
        return getHttpClient(READ_TIMEOUT, READ_TIMEOUT);
    }

    protected static OkHttpClient getHttpClient(int readTimeout, int connectTimeout) {
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setReadTimeout((long) readTimeout, TimeUnit.SECONDS);
        okHttpClient.setConnectTimeout((long) connectTimeout, TimeUnit.SECONDS);
        return okHttpClient;
    }

    protected static OkHttpClient createCachedClient(Context context) {
        Cache cache = new Cache(new File(context.getCacheDir(), "cache_file"), 20971520);
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setCache(cache);
        okHttpClient.interceptors().add(new C04183(context));
        okHttpClient.networkInterceptors().add(new C04194(context));
        return okHttpClient;
    }

    private static boolean isOnline(Context context) {
        NetworkInfo netInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}

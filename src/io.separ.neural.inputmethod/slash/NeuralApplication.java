package io.separ.neural.inputmethod.slash;

import android.app.Application;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import com.android.inputmethod.latin.utils.UncachedInputMethodManagerUtils;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

import io.separ.neural.inputmethod.indic.settings.Settings;

/**
 * Created by sepehr on 3/2/17.
 */

public class NeuralApplication extends Application {
    private static NeuralApplication mInstance;
    private static TaskQueue mServiceQueue;

    private static final String QUEUE_SERVICE_REQUEST = "QUEUE_SERVICE_REQUEST";

    public static TaskQueue getNetworkTaskQueue() {
        if (mServiceQueue == null) {
            synchronized (NeuralApplication.class) {
                if (mServiceQueue == null) {
                    mServiceQueue = TaskQueue.loadQueue(mInstance, QUEUE_SERVICE_REQUEST);
                }
            }
        }
        return mServiceQueue;
    }

    public void onCreate() {
        super.onCreate();
        mInstance = this;
        Settings.init(this);
        Fresco.initialize(this, ImagePipelineConfig.newBuilder(this).setDownsampleEnabled(true).build());
    }

    public static NeuralApplication getInstance() {
        return mInstance;
    }

    public static boolean isKeyboardEnabledAndSet(Context context) {
        InputMethodManager mImm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        return UncachedInputMethodManagerUtils.isThisImeEnabled(context, mImm) && UncachedInputMethodManagerUtils.isThisImeCurrent(context, mImm);
    }
}

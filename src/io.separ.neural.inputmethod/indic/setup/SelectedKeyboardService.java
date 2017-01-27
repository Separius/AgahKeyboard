package io.separ.neural.inputmethod.indic.setup;

/**
 * Created by sepehr on 1/27/17.
 */
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

public class SelectedKeyboardService extends IntentService {
    private static final String TAG;

    static {
        TAG = SelectedKeyboardService.class.getSimpleName();
    }

    public SelectedKeyboardService() {
        super("SelectedKeyboardService");
    }

    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    protected void onHandleIntent(Intent intent) {
        String packageLocal = getPackageName();
        boolean isInputDeviceEnabled = false;
        while (!isInputDeviceEnabled) {
            for (InputMethodInfo inputMethod : ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).getEnabledInputMethodList()) {
                if (inputMethod.getPackageName().equals(packageLocal)) {
                    isInputDeviceEnabled = true;
                }
            }
        }
        Intent newIntent = new Intent(this, IntroActivity.class);
        newIntent.addFlags(268435456);
        newIntent.putExtra(IntroActivity.RESTART_FROM_ENABLE_KEY, true);
        startActivity(newIntent);
    }
}
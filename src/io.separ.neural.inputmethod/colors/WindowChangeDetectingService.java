package io.separ.neural.inputmethod.colors;

/**
 * Created by sepehr on 1/26/17.
 */

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.accessibility.AccessibilityEvent;

import io.separ.neural.inputmethod.indic.LastComposedWord;

public class WindowChangeDetectingService extends AccessibilityService {
    private static final String APP_PACKAGE = "io.separ.neural.inputmethod";
    private static ActivityInfo activityInfo;
    private static String windowTitle;

    public static ActivityInfo getActivityInfo() {
        return activityInfo;
    }

    public static String getWindowTitle() {
        return windowTitle;
    }

    public static boolean isActivity() {
        return activityInfo != null;
    }

    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = 32;
        config.feedbackType = 16;
        config.flags = 2;
        setServiceInfo(config);
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == 32 && event.getPackageName() != APP_PACKAGE) {
            if (event.getPackageName() == null || event.getClassName() == null) {
                activityInfo = null;
                return;
            }
            activityInfo = tryGetActivity(new ComponentName(event.getPackageName().toString(), event.getClassName().toString()));
            if (event.getText().size() <= 0) {
                windowTitle = LastComposedWord.NOT_A_SEPARATOR;
            } else if (event.getText().get(0) != null) {
                windowTitle = ((CharSequence) event.getText().get(0)).toString();
            }
        }
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public void onInterrupt() {
        activityInfo = null;
        windowTitle = LastComposedWord.NOT_A_SEPARATOR;
    }
}
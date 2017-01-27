package io.separ.neural.inputmethod.indic.setup;

/**
 * Created by sepehr on 1/27/17.
 */

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.inputmethod.InputMethodManager;
import com.android.inputmethod.latin.utils.UncachedInputMethodManagerUtils;
import com.github.paolorotolo.appintro.AppIntro2;
import io.separ.neural.inputmethod.indic.settings.Settings;

public class IntroActivity extends AppIntro2 {
    public static final String RESTART_FROM_ENABLE_KEY = "RestartFromEnable";
    public static final String[] permissions;
    private EnableFragment enableFragment;
    private InputMethodManager mImm;

    static {
        permissions = new String[]{"android.permission.READ_CONTACTS", "android.permission.READ_PHONE_STATE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mImm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        showSkipButton(false);
        addSlide(new WelcomeFragment());
        this.enableFragment = new EnableFragment();
        addSlide(this.enableFragment);
        addSlide(new LanguageFragment());
        addSlide(new SmartKeyboardFragment());
    }

    public void onDonePressed(Fragment currentFragment) {
        Settings.writeWizardCompleted(PreferenceManager.getDefaultSharedPreferences(this), true);
    }

    protected void onResume() {
        super.onResume();
        if (shouldRestartFromEnableFragment()) {
            this.pager.setCurrentItem(1, false);
        }
    }

    private boolean shouldRestartFromEnableFragment() {
        boolean shouldRestart = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(RESTART_FROM_ENABLE_KEY, false);
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean(RESTART_FROM_ENABLE_KEY, false).apply();
        return shouldRestart;
    }

    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        if (newFragment != null && !(newFragment instanceof WelcomeFragment)) {
            if (newFragment.getClass().equals(EnableFragment.class) && !(isEnabled() && isSelected())) {
                setCompletedState(false);
            }
        }
    }

    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        onDonePressed(currentFragment);
    }

    protected void setCompletedState(boolean completed) {
        setNextPageSwipeLock(!completed);
    }

    protected boolean checkPermissionGiven() {
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != 0) {
                allGranted = false;
            }
        }
        return allGranted;
    }

    protected boolean isEnabled() {
        return UncachedInputMethodManagerUtils.isThisImeEnabled(this, this.mImm);
    }

    public boolean isSelected() {
        return UncachedInputMethodManagerUtils.isThisImeCurrent(this, this.mImm);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (isSelected()) {
            setCompletedState(true);
            this.enableFragment.setCompletedState();
        }
    }
}


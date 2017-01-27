package io.separ.neural.inputmethod.indic.setup;

/**
 * Created by sepehr on 1/27/17.
 */
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import io.separ.neural.inputmethod.indic.R;

public class EnableFragment extends AnimatedBackgroundGradientFragment {
    private static final int PERMISSION_REQUEST_CODE = 12;
    private static final String TAG;
    private EnableButtonLayout auth;
    private EnableButtonLayout enable;
    private InputMethodManager mImm;
    private View root;
    private EnableButtonLayout select;

    /* renamed from: org.smc.inputmethod.indic.appintro.EnableFragment.1 */
    class C06011 implements OnClickListener {
        C06011() {
        }

        public void onClick(View v) {
            EnableFragment.this.authorization();
        }
    }

    /* renamed from: org.smc.inputmethod.indic.appintro.EnableFragment.2 */
    class C06022 implements OnClickListener {
        C06022() {
        }

        public void onClick(View v) {
            EnableFragment.this.enable();
        }
    }

    /* renamed from: org.smc.inputmethod.indic.appintro.EnableFragment.3 */
    class C06033 implements OnClickListener {
        C06033() {
        }

        public void onClick(View v) {
            EnableFragment.this.selectKeyboard();
        }
    }

    static {
        TAG = EnableFragment.class.getSimpleName();
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.root = inflater.inflate(R.layout.intro_enable_layout, container, false);
        setupTransition((TransitionDrawable) this.root.getBackground());
        this.mImm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        this.auth = (EnableButtonLayout) this.root.findViewById(R.id.auth);
        this.enable = (EnableButtonLayout) this.root.findViewById(R.id.enable);
        this.select = (EnableButtonLayout) this.root.findViewById(R.id.select_keyboard);
        this.auth.setOnClickListener(new C06011());
        this.enable.setOnClickListener(new C06022());
        this.select.setOnClickListener(new C06033());
        updateState();
        return this.root;
    }

    private void updateState() {
        IntroActivity introActivity = (IntroActivity) getActivity();
        if (introActivity.checkPermissionGiven()) {
            this.auth.setCompletedState();
        }
        if (introActivity.isEnabled()) {
            this.enable.setCompletedState();
        }
        if (introActivity.isSelected()) {
            this.select.setCompletedState();
            introActivity.setCompletedState(true);
        }
    }

    private void selectKeyboard() {
        if (this.select.isCompleted()) {
            showDoTheNextStepPopUp();
        } else {
            showInputPicker();
        }
    }

    private void enable() {
        if (this.enable.isCompleted()) {
            showDoTheNextStepPopUp();
        } else {
            invokeLanguageAndInputSettings();
        }
    }

    private void authorization() {
        if (this.auth.isCompleted()) {
            showDoTheNextStepPopUp();
        } else {
            ActivityCompat.requestPermissions(getActivity(), IntroActivity.permissions, PERMISSION_REQUEST_CODE);
        }
    }

    public void onResume() {
        super.onResume();
        updateState();
    }

    private void showInputPicker() {
        this.mImm.showInputMethodPicker();
    }

    private void showDoTheNextStepPopUp() {
        Toast.makeText(getContext(), "Completed, pass to the next step :)", Toast.LENGTH_LONG).show();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE /*12*/:
                int grantCounter = 0;
                for (int grantResult : grantResults) {
                    if (grantResult == 0) {
                        grantCounter++;
                    }
                }
                if (grantCounter == permissions.length) {
                    this.auth.setCompletedState();
                }
            default:
        }
    }

    private void invokeLanguageAndInputSettings() {
        getActivity().startService(new Intent(getActivity(), SelectedKeyboardService.class));
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean(IntroActivity.RESTART_FROM_ENABLE_KEY, true).apply();
        Intent intent = new Intent();
        intent.setAction("android.settings.INPUT_METHOD_SETTINGS");
        intent.addCategory("android.intent.category.DEFAULT");
        startActivityForResult(intent, 2000);
    }

    public void setCompletedState() {
        this.select.setCompletedState();
        ((IntroActivity) getActivity()).setCompletedState(true);
    }
}
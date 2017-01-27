package io.separ.neural.inputmethod.indic.settings;

/**
 * Created by sepehr on 1/27/17.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import io.separ.neural.inputmethod.indic.setup.IntroActivity;

public class StartActivity extends AppCompatActivity {
    private static final String TAG;

    static {
        TAG = StartActivity.class.getSimpleName();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (Settings.readWizardCompleted(prefs)) {
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        prefs.edit().clear().apply();*/
        startActivity(new Intent(this, IntroActivity.class));
    }

    protected String getActivityName() {
        return TAG;
    }
}
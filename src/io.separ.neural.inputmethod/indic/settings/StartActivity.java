package io.separ.neural.inputmethod.indic.settings;

/**
 * Created by sepehr on 1/27/17.
 */

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import io.separ.neural.inputmethod.slash.NeuralApplication;

public class StartActivity extends AppCompatActivity {
    class C04931 implements Runnable {
        final Intent val$intent;

        C04931(Intent intent) {
            this.val$intent = intent;
        }

        public void run() {
            StartActivity.this.startActivity(this.val$intent);
            if (!StartActivity.this.isFinishing()) {
                StartActivity.this.finish();
            }
            //StartActivity.this.overridePendingTransition(R.anim.fade_in, R.anim.nothing);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        Intent intent;
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        //setContentView((int) R.layout.slash_splash);
        if (NeuralApplication.isKeyboardEnabledAndSet(this)) {
            intent = new Intent(this, SettingsActivity.class);
        } else {
            intent = new Intent(this, SetupWizardActivity.class);
        }
        intent.addFlags(335544320);
        new Handler().postDelayed(new C04931(intent), 0);
    }
}
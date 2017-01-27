package com.android.inputmethodcommon;

import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.Toast;

import io.separ.neural.inputmethod.indic.R;

public class PermissionActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.READ_CONTACTS") == 0 || checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == 0 || checkSelfPermission("android.permission.READ_PHONE_STATE") == 0 || !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("permission", true)) {
            finish();
            return;
        }
        requestPermissions(new String[]{"android.permission.READ_CONTACTS", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_PHONE_STATE"}, 2);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 2 && grantResults[0] == 0 && grantResults[1] == 0) {
            finish();
        } else {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("permission", false).apply();
            Toast.makeText(this, R.string.pemission_message, Toast.LENGTH_LONG).show();
            finish();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

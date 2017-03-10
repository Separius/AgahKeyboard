package com.android.inputmethodcommon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import io.separ.neural.inputmethod.indic.R;

public class PermissionActivity extends AppCompatActivity {
    private static final String PERMISSION = "permission";
    /*public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.READ_CONTACTS") == 0 ) {
            finish();
            return;
        }
        requestPermissions(new String[]{"android.permission.READ_CONTACTS", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_PHONE_STATE"}, 2);
    }*/

    public static void startActivity(Context context) {
        startActivity(context, null);
    }

    public static void startActivity(Context context, String permission) {
        Intent activityIntent = new Intent(context, PermissionActivity.class);
        activityIntent.setFlags(268435456);
        if (permission != null) {
            activityIntent.putExtra(PERMISSION, permission);
        }
        context.startActivity(activityIntent);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 2 && grantResults[0] == 0 && grantResults[1] == 0) {
            finish();
        }/* else {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("permission", false).apply();
            Toast.makeText(this, R.string.pemission_message, Toast.LENGTH_LONG).show();
            finish();
        }*/
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

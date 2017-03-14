package io.separ.neural.inputmethod.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.UUID;

import io.separ.neural.inputmethod.slash.NeuralApplication;

/**
 * Created by sepehr on 3/2/17.
 */

public class DeviceUuidFactory {
    protected static final String PREFS_DEVICE_ID = "device_id2";
    protected static UUID uuid;

    public static UUID getDeviceUuid() {
        return getDeviceUuid(NeuralApplication.getInstance().getApplicationContext());
    }

    public static UUID getDeviceUuid(Context context) {
        if (uuid == null) {
            synchronized (DeviceUuidFactory.class) {
                if (uuid == null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    String id = prefs.getString(PREFS_DEVICE_ID, null);
                    if (id != null) {
                        uuid = UUID.fromString(id);
                    } else {
                        uuid = UUID.randomUUID();
                        prefs.edit().putString(PREFS_DEVICE_ID, uuid.toString()).apply();
                    }
                }
            }
        }
        return uuid;
    }
}
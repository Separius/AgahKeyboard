package io.separ.neural.inputmethod.colors;

/**
 * Created by sepehr on 1/26/17.
 */

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.KeyCharacterMap;
import android.view.ViewConfiguration;

public class NavManager implements OnSharedPreferenceChangeListener, ServiceConnection {
    private final Context context;
    private boolean hasNavBar;
    private boolean isBind;
    private NavService navService;
    private boolean notHide;
    private boolean settingsEnabled;

    public void onServiceConnected(ComponentName className, IBinder service) {
        this.navService = ((NavService.NavBinder) service).getService();
        this.isBind = true;
        this.navService.show(true);
    }

    public void onServiceDisconnected(ComponentName arg0) {
        this.navService = null;
        this.isBind = false;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        createService();
        //killService();
    }

    public void setNotHide(boolean notHide) {
        this.notHide = notHide;
    }

    public void hide() {
        if (this.navService != null && !this.notHide) {
            this.navService.hide();
        }
    }

    public void show() {
        if (this.navService != null && this.settingsEnabled && this.hasNavBar && !this.notHide) {
            this.navService.show(true);
        }
        this.notHide = false;
    }

    public NavManager(Context context) {
        this.notHide = false;
        this.context = context.getApplicationContext();
        this.settingsEnabled = true;
        boolean hasNavBar = hasNavBar(context);
        this.hasNavBar = hasNavBar;
        if (hasNavBar) {
            PreferenceManager.getDefaultSharedPreferences(this.context).registerOnSharedPreferenceChangeListener(this);
            if (this.settingsEnabled) {
                createService();
            }
        }
    }

    private void createService() {
        if (this.navService == null) {
            this.context.bindService(new Intent(this.context, NavService.class), this, Context.BIND_AUTO_CREATE);
            return;
        }
        this.navService.registerScreen();
    }

    public void killService() {
        if (this.navService != null && this.isBind) {
            this.context.unbindService(this);
            this.isBind = false;
        }
    }

    private boolean hasNavBar(Context context) {
        Resources resources = context.getResources();
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            return resources.getBoolean(id);
        }
        return (ViewConfiguration.get(context).hasPermanentMenuKey() || KeyCharacterMap.deviceHasKey(4)) ? false : true;
    }
}

package io.separ.neural.inputmethod.colors;

/**
 * Created by sepehr on 1/26/17.
 */

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.android.inputmethod.keyboard.KeyboardSwitcher;

public class NavService extends Service implements ColorManager.OnColorChange, OnAttachStateChangeListener {
    private Animator anim;
    private View barColor;
    private NavBinder binder;
    private boolean hasRegistred;
    private ScreenReceiver mReceiver;
    private LayoutParams params;
    private boolean screenOn;
    private boolean viewAdded;
    private WindowManager wm;

    /* renamed from: com.android.inputmethodcommon.NavService.1 */
    class C02911 implements Runnable {
        C02911() {
        }

        @SuppressLint({"NewApi"})
        public void run() {
            if (NavService.this.barColor.isAttachedToWindow()) {
                int cx;
                int cy;
                if (NavService.this.anim == null) {
                    cx = NavService.this.barColor.getWidth() / 2;
                    cy = NavService.this.barColor.getHeight() / 2;
                    NavService.this.anim = ViewAnimationUtils.createCircularReveal(NavService.this.barColor, cx, cy, 0.0f, (float) Math.hypot((double) cx, (double) cy));
                    NavService.this.anim.setDuration(300);
                }
                if (KeyboardSwitcher.getInstance().getmLatinIME().isInputViewShown()) {
                    NavService.this.barColor.setVisibility(View.VISIBLE);
                    if (NavService.this.anim.isStarted() || NavService.this.anim.isRunning()) {
                        cx = NavService.this.barColor.getWidth() / 2;
                        cy = NavService.this.barColor.getHeight() / 2;
                        NavService.this.anim = ViewAnimationUtils.createCircularReveal(NavService.this.barColor, cx, cy, 0.0f, (float) Math.hypot((double) cx, (double) cy));
                        NavService.this.anim.setDuration(300);
                        NavService.this.anim.start();
                        return;
                    }
                    NavService.this.anim.start();
                }
            }
        }
    }

    public class NavBinder extends Binder {
        public NavService getService() {
            return NavService.this;
        }
    }

    class ScreenReceiver extends BroadcastReceiver {
        ScreenReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (((KeyguardManager) context.getSystemService(KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode()) {
                NavService.this.hide();
            } else if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                NavService.this.screenOn = false;
                NavService.this.hide();
            } else if (intent.getAction().equals("android.intent.action.USER_PRESENT")) {
                NavService.this.screenOn = true;
                if (KeyboardSwitcher.getInstance().getmLatinIME().isInputViewShown()) {
                    NavService.this.show(false);
                }
            }
        }
    }

    public NavService() {
        this.screenOn = true;
        this.anim = null;
        this.viewAdded = false;
        this.binder = new NavBinder();
    }

    public void onViewAttachedToWindow(View view) {
        show(true);
    }

    public void onViewDetachedFromWindow(View view) {
        hide();
    }

    @Nullable
    @SuppressLint({"NewApi"})
    public IBinder onBind(Intent intent) {
        ColorManager.addObserver(this);
        registerScreen();
        this.wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int sizeX = getWindowWidth(this.wm);
        int screenHeight = getWindowHeight(this.wm);
        int sizeY = getNavbarHeight();
        this.params = new LayoutParams(2006, 1832, -3);
        this.params.width = sizeX;
        this.params.height = sizeY;
        this.params.gravity = 8388659;
        this.params.x = 0;
        this.params.y = screenHeight;
        this.barColor = new View(this);
        this.barColor.addOnAttachStateChangeListener(this);
        return this.binder;
    }

    private int getNavbarHeight() {
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private int getWindowWidth(WindowManager manager) {
        Display display = manager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    private int getWindowHeight(WindowManager manager) {
        Display display = manager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    public void onColorChange(ColorProfile newProfile) {
        if (this.barColor != null) {
            this.barColor.setBackgroundColor(newProfile.getSecondary());
        }
    }

    public void registerScreen() {
        if (!this.hasRegistred) {
            IntentFilter filter = new IntentFilter("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.USER_PRESENT");
            this.mReceiver = new ScreenReceiver();
            registerReceiver(this.mReceiver, filter);
            this.hasRegistred = true;
            this.screenOn = confirmScreenOn(this);
        }
    }

    private void terminate() {
        hide();
        if (this.hasRegistred) {
            unregisterReceiver(this.mReceiver);
            this.hasRegistred = false;
        }
    }

    public void hide() {
        if (this.barColor != null) {
            this.barColor.setVisibility(View.GONE);
            if (this.anim != null) {
                this.anim.cancel();
            }
        }
    }

    @SuppressLint({"NewApi"})
    public void show(boolean animate) {
        if (VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            return;
            //KeyboardSwitcher.getInstance().getmLatinIME().getOverlayPermission();
        }
        if (KeyboardSwitcher.getInstance().getmLatinIME().isInputViewShown()) {
            if (!(this.barColor.isAttachedToWindow() || this.viewAdded || (VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)))) {
                this.wm.addView(this.barColor, this.params);
                this.barColor.setVisibility(View.GONE);
                this.viewAdded = true;
            }
            if (this.barColor == null || this.barColor.getVisibility() == View.VISIBLE || !this.screenOn) {
                if (this.barColor != null) {
                    this.barColor.setVisibility(View.GONE);
                    return;
                }
                return;
            } else if (!this.barColor.isAttachedToWindow()) {
                return;
            } else {
                if (VERSION.SDK_INT < 21 || !animate) {
                    this.barColor.setVisibility(View.VISIBLE);
                    return;
                } else {
                    this.barColor.post(new C02911());
                    return;
                }
            }
        }
        hide();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    public boolean onUnbind(Intent intent) {
        terminate();
        return super.onUnbind(intent);
    }

    public void onDestroy() {
        terminate();
        super.onDestroy();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.barColor != null && this.viewAdded) {
            this.wm.removeViewImmediate(this.barColor);
            this.viewAdded = false;
        }
        int sizeX = getWindowWidth(this.wm);
        int screenHeight = getWindowHeight(this.wm);
        int sizeY = getNavbarHeight();
        this.params = new LayoutParams(2006, 1832, -3);
        this.params.width = sizeX;
        this.params.height = sizeY;
        this.params.gravity = 8388659;
        this.params.x = 0;
        this.params.y = screenHeight;
        show(false);
    }

    private boolean confirmScreenOn(Context context) {
        if (VERSION.SDK_INT < 21) {
            return ((PowerManager) context.getSystemService(POWER_SERVICE)).isScreenOn();
        }
        for (Display display : ((DisplayManager) context.getSystemService(DISPLAY_SERVICE)).getDisplays()) {
            if (display.getState() != 1) {
                return true;
            }
        }
        return false;
    }
}
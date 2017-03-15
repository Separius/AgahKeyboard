package io.separ.neural.inputmethod.colors;

/**
 * Created by sepehr on 1/26/17.
 */

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build.VERSION;
import android.support.v7.graphics.Palette;

public class ColorExtractor {
    private static int[] getPrimaryAttrs(Resources res, String packageName) {
        return new int[]{res.getIdentifier("colorPrimary", "attr", packageName), res.getIdentifier("android:colorPrimary", "attr", packageName), 16843827};
    }

    private static int[] getPrimaryDarkAttrs(Resources res, String packageName) {
        return new int[]{res.getIdentifier("colorPrimaryDark", "attr", packageName), res.getIdentifier("android:colorPrimaryDark", "attr", packageName), 16843828};
    }

    private static int[] getAccentAttrs(Resources res, String packageName) {
        return new int[]{res.getIdentifier("colorAccent", "attr", packageName), res.getIdentifier("android:colorAccent", "attr", packageName), 16843829};
    }

    protected static int getContextColor(Theme theme, int[] attrs) {
        TypedArray a = theme.obtainStyledAttributes(attrs);
        int color = a.getColor(1, a.getColor(0, a.getColor(2, 1000)));
        a.recycle();
        return color;
    }

    protected static int getActivityColor(Resources res, ActivityInfo info, int[] attrs) throws Exception {
        Theme theme = res.newTheme();
        theme.applyStyle(info.theme, false);
        TypedArray a = theme.obtainStyledAttributes(attrs);
        int color = a.getColor(1, a.getColor(0, a.getColor(2, 1000)));
        a.recycle();
        return color;
    }

    protected static int getLaunchActivityColor(Context context, String packageName, int[] attrs) throws Exception {
        PackageManager pm = context.getPackageManager();
        Theme theme = pm.getResourcesForApplication(packageName).newTheme();
        theme.applyStyle(pm.getActivityInfo(pm.getLaunchIntentForPackage(packageName).getComponent(), 0).theme, false);
        TypedArray a = theme.obtainStyledAttributes(attrs);
        int color = a.getColor(1, a.getColor(0, a.getColor(2, 1000)));
        a.recycle();
        return color;
    }

    protected static int getApplicationColor(Context context, String packageName, int[] attrs) throws Exception {
        PackageManager pm = context.getPackageManager();
        Theme theme = pm.getResourcesForApplication(packageName).newTheme();
        theme.applyStyle(pm.getPackageInfo(packageName, 128).applicationInfo.theme, false);
        TypedArray a = theme.obtainStyledAttributes(attrs);
        int color = a.getColor(1, a.getColor(0, a.getColor(2, 1000)));
        a.recycle();
        return color;
    }

    protected static ColorProfile getActivityProfile(Context context, ActivityInfo info) throws Exception {
        Resources res = context.getPackageManager().getResourcesForApplication(info.packageName);
        return new ColorProfile(getActivityColor(res, info, getPrimaryAttrs(res, info.packageName)), getActivityColor(res, info, getPrimaryDarkAttrs(res, info.packageName)), getActivityColor(res, info, getAccentAttrs(res, info.packageName)));
    }

    protected static ColorProfile getLaunchActivityProfile(Context context, String packageName) throws Exception {
        Resources res = context.getPackageManager().getResourcesForApplication(packageName);
        return new ColorProfile(getLaunchActivityColor(context, packageName, getPrimaryAttrs(res, packageName)), getLaunchActivityColor(context, packageName, getPrimaryDarkAttrs(res, packageName)), getLaunchActivityColor(context, packageName, getAccentAttrs(res, packageName)));
    }

    protected static ColorProfile getApplicationProfile(Context context, String packageName) throws Exception {
        Resources res = context.getPackageManager().getResourcesForApplication(packageName);
        return new ColorProfile(getApplicationColor(context, packageName, getPrimaryAttrs(res, packageName)), getApplicationColor(context, packageName, getPrimaryDarkAttrs(res, packageName)), getApplicationColor(context, packageName, getAccentAttrs(res, packageName)));
    }

    protected static int getActivityPrimaryColor(Context context, ActivityInfo info) throws Exception {
        Resources res = context.getPackageManager().getResourcesForApplication(info.packageName);
        return getActivityColor(res, info, getPrimaryAttrs(res, info.packageName));
    }

    protected static int getLaunchActivityColor(Context context, String packageName) throws Exception {
        return getLaunchActivityColor(context, packageName, getPrimaryAttrs(context.getPackageManager().getResourcesForApplication(packageName), packageName));
    }

    protected static int getApplicationColor(Context context, String packageName) throws Exception {
        return getApplicationColor(context, packageName, getPrimaryAttrs(context.getPackageManager().getResourcesForApplication(packageName), packageName));
    }

    protected static int getIconColor(Context context, String packageName) throws NameNotFoundException {
        BitmapDrawable icon = (BitmapDrawable) context.getPackageManager().getApplicationIcon(packageName);
        if (VERSION.SDK_INT < 21) {
            return Bitmap.createScaledBitmap(icon.getBitmap(), 1, 1, true).getPixel(0, 0);
        }
        Palette p = Palette.from(icon.getBitmap()).generate();
        int mutedColor = p.getMutedColor(1000);
        return mutedColor != 1000 ? mutedColor : p.getVibrantColor(1000);
    }

    private static int tryGetColor(Resources resources, String packageName, String[] possibleNames) {
        int i = 0;
        while (i < possibleNames.length) {
            try {
                return resources.getColor(resources.getIdentifier(possibleNames[i], "color", packageName));
            } catch (NotFoundException e) {
                i++;
            }
        }
        return 1000;
    }

    protected static ColorProfile getColorProfile(Context context, String packageName) {
        try {
            Resources resources = context.getPackageManager().getResourcesForApplication(packageName);
            return new ColorProfile(tryGetColor(resources, packageName, new String[]{"primary", "colorPrimary", "colorprimary", "color_primary", "primaryColor"}), tryGetColor(resources, packageName, new String[]{"primaryDark", "colorPrimaryDark", "colorprimarydark", "color_primary_dark", "primaryColorDark", "colorDark", "darkColor", "darkcolor", "dark"}), tryGetColor(resources, packageName, new String[]{"accent", "colorAccent", "coloraccent", "color_accent", "accentColor", "contrast"}));
        } catch (NameNotFoundException e) {
            return new ColorProfile();
        }
    }
}
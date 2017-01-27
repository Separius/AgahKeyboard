package io.separ.neural.inputmethod.Utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import java.util.HashMap;

import io.separ.neural.inputmethod.colors.ColorProfile;

import static io.separ.neural.inputmethod.colors.ColorUtils.setProfileFromApp;

/**
 * Created by sepehr on 11/23/16.
 */

public class ColorUtils {
    private static final HashMap<String, ColorProfile> appColors = new HashMap<>();

    public static ColorProfile colorProfile;

    public static void getColor(@NonNull Context context, int uid){
        PackageManager pm = context.getPackageManager();
        String appName = pm.getNameForUid(uid);
        getColor(context, appName);
    }

    public static ColorProfile getColor(@NonNull Context context, String appName){
        if(!appColors.containsKey(appName)) {
            colorProfile = setProfileFromApp(context, appName);
            appColors.put(appName, colorProfile);
        }else
            colorProfile = appColors.get(appName);
        return colorProfile;
    }

    public static void drawBackground(Canvas canvas, int mDrawColor, io.separ.neural.inputmethod.colors.ColorUtils.ForceType forceType) {
        Rect canvasBound;
        Drawable d;
        canvasBound = canvas.getClipBounds();
        d = getBackground(canvasBound.width(), canvasBound.height(), mDrawColor);
        d.setBounds(canvasBound);
        d.draw(canvas);
    }

    public static Drawable getBackground(int width, int height, int color) {
        return buildFlat(width, height, color);
    }

    private static Drawable buildFlat(int width, int height, int color) {
        ColorDrawable d = new ColorDrawable(color);
        d.setBounds(0, height, width, 0);
        return d;
    }
}

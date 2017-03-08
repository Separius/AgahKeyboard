package io.separ.neural.inputmethod.Utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;

import io.separ.neural.inputmethod.colors.ColorDatabase;
import io.separ.neural.inputmethod.colors.ColorProfile;
import io.separ.neural.inputmethod.colors.SpecialRules;
import io.separ.neural.inputmethod.colors.WindowChangeDetectingService;

import static android.graphics.Color.parseColor;
import static io.separ.neural.inputmethod.colors.ColorUtils.NO_COLOR;
import static io.separ.neural.inputmethod.colors.ColorUtils.setProfileFromApp;

/**
 * Created by sepehr on 11/23/16.
 */

public class ColorUtils {

    public static ColorProfile colorProfile = new ColorProfile();

    private static final String BATTERY_COLOR = "#f5511e";

    private static boolean isBatterySaverOn(Context context) {
        return Build.VERSION.SDK_INT >= 21 && ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isPowerSaveMode();
    }

    public static ColorProfile getColor(@NonNull Context context, String packageName, boolean skipDataBase){
        if (isBatterySaverOn(context)) {
            colorProfile.setProfile(parseColor(BATTERY_COLOR), darkerColor(parseColor(BATTERY_COLOR)), -1);
            return colorProfile;
        }
        String[] strArr;
        Integer color;
        if (skipDataBase || !ColorDatabase.existPackage(context, packageName) || SpecialRules.overrideStandardColor(packageName)) {
            strArr = new String[1];
            strArr[0] = WindowChangeDetectingService.getWindowTitle();
            color = SpecialRules.getColor(packageName, context);
            if (color == null) {
                colorProfile = setProfileFromApp(context, packageName);
            } else {
                colorProfile.setPrimary(color);
            }
            return colorProfile;
        }
        colorProfile.setPrimary(parseColor(ColorDatabase.getColors(context, packageName)[0]));
        return colorProfile;
    }

    public static ColorProfile getColor(@NonNull Context context, String packageName){
        return getColor(context, packageName, false);
    }

    static int darkerColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] * 0.8f;
        return Color.HSVToColor(hsv);
    }

    public static void drawBackground(Canvas canvas, int mDrawColor) {
        Rect canvasBound;
        Drawable d;
        canvasBound = canvas.getClipBounds();
        d = new ColorDrawable(mDrawColor);
        d.setBounds(canvasBound);
        d.draw(canvas);
    }
}

package io.separ.neural.inputmethod.Utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;

import io.separ.neural.inputmethod.colors.ColorDatabase;
import io.separ.neural.inputmethod.colors.ColorProfile;
import io.separ.neural.inputmethod.colors.SpecialRules;
import io.separ.neural.inputmethod.colors.WindowChangeDetectingService;

import static android.graphics.Color.parseColor;
import static io.separ.neural.inputmethod.colors.ColorUtils.NO_COLOR;
import static io.separ.neural.inputmethod.colors.ColorUtils.getTextColor;
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

    public static ColorProfile getColor(@NonNull Context context, String packageName){
        if (isBatterySaverOn(context)) {
            colorProfile.setProfile(parseColor(BATTERY_COLOR), darkerColor(parseColor(BATTERY_COLOR)), -1);
            return colorProfile;
        }
        String[] strArr;
        Integer color;
        if (!ColorDatabase.existPackage(context, packageName) || SpecialRules.overrideStandardColor(packageName)) {
            strArr = new String[1];
            strArr[0] = WindowChangeDetectingService.getWindowTitle();
            //if the mode is adaptive(read from sharedPrefrences){
            String theme = PreferenceManager.getDefaultSharedPreferences(context).getString("KeyboardTheme", "adaptive_theme");
            if(theme.equals("adaptive_theme")) {
                color = SpecialRules.getColor(packageName, context);
                if (color == null) {
                    colorProfile = setProfileFromApp(context, packageName);
                    Log.e("catch_spotify", packageName+"::getPrimColor: "+ colorProfile.getPrimary());
                    Log.e("catch_spotify", packageName+"::getTextColor: "+ getTextColor(colorProfile));
                } else {
                    colorProfile.setPrimary(color);
                }
                return colorProfile;
            }else{ //black or blue _theme
                colorProfile.setPrimary(parseColor(ColorDatabase.getColors(context, theme+"_primary")[0]));
                colorProfile.setIcon(parseColor(ColorDatabase.getColors(context, theme+"_secondary")[0]));
                return colorProfile;
            }
            //}else{read from currentTheme}
        }
        colorProfile.setPrimary(parseColor(ColorDatabase.getColors(context, packageName)[0]));
        return colorProfile;
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

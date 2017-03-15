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

import io.separ.neural.inputmethod.colors.ColorDatabase;
import io.separ.neural.inputmethod.colors.ColorProfile;
import io.separ.neural.inputmethod.colors.SpecialRules;
import io.separ.neural.inputmethod.colors.WindowChangeDetectingService;

import static android.graphics.Color.parseColor;
import static io.separ.neural.inputmethod.colors.ColorUtils.setProfileFromApp;

/**
 * Created by sepehr on 11/23/16.
 */

public class ColorUtils {

    public static ColorProfile colorProfile = new ColorProfile();

    public static ColorProfile getColor(@NonNull Context context, String packageName){
        String[] strArr;
        Integer color;
        if (!ColorDatabase.existPackage(context, packageName) || SpecialRules.overrideStandardColor(packageName)) {
            strArr = new String[1];
            strArr[0] = WindowChangeDetectingService.getWindowTitle();
            String theme = PreferenceManager.getDefaultSharedPreferences(context).getString("KeyboardTheme", "adaptive_theme");
            if(theme.equals("adaptive_theme")) {
                color = SpecialRules.getColor(packageName, context);
                if (color == null) {
                    colorProfile = setProfileFromApp(context, packageName);
                } else {
                    colorProfile.setPrimary(color);
                }
                return colorProfile;
            }else{ //black or blue _theme
                colorProfile.setProfile(parseColor(ColorDatabase.getColors(context, theme+"_primary")[0]), 1000, parseColor(ColorDatabase.getColors(context, theme+"_secondary")[0]));
                return colorProfile;
            }
        }
        colorProfile.setPrimary(parseColor(ColorDatabase.getColors(context, packageName)[0]));
        return colorProfile;
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

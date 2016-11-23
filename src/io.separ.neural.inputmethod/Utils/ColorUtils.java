package io.separ.neural.inputmethod.Utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;

import java.util.HashMap;

/**
 * Created by sepehr on 11/23/16.
 */

public class ColorUtils {
    private static final HashMap<String, Integer> appColors = new HashMap<>();

    public static int getColor(@NonNull Context context, int uid){
        PackageManager pm = context.getPackageManager();
        String appName = pm.getNameForUid(uid);
        if(appColors.containsKey(appName))
            return appColors.get(appName);
        try {
            Context c = context.createPackageContext(appName, Context.CONTEXT_IGNORE_SECURITY);
            Resources res = c.getResources();
            Resources.Theme theme = res.newTheme();
            theme.applyStyle(pm.getPackageInfo(appName, PackageManager.GET_META_DATA).applicationInfo.theme, false);
            TypedArray a = theme.obtainStyledAttributes(new int[] {res.getIdentifier("android:colorPrimary", "attr", appName), res.getIdentifier("colorPrimary", "attr", appName)});
            int color = a.getColor(0, a.getColor(1, 0));
            a.recycle();
            if(color != 0) {
                appColors.put(appName, color);
                return color;
            } else {
                appColors.put(appName, Color.WHITE);
                return Color.WHITE;
            }
        } catch (Exception e) {
            e.printStackTrace();
            appColors.put(appName, Color.WHITE);
            return Color.WHITE;
        }
    }
}

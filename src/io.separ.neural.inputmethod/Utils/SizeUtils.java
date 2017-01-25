package io.separ.neural.inputmethod.Utils;

/**
 * Created by sepehr on 1/25/17.
 */

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class SizeUtils {
    public static int dpFromPx(Context context, float px) {
        return (int) (px / context.getResources().getDisplayMetrics().density);
    }

    public static int pxFromDp(Context context, float dp) {
        return (int) TypedValue.applyDimension(1, dp, context.getResources().getDisplayMetrics());
    }

    public static int getScreenHeightInDp(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) (((float) displayMetrics.heightPixels) / displayMetrics.density);
    }

    public static int getScreenHeightInPx(Context context) {
        return (int) ((float) context.getResources().getDisplayMetrics().heightPixels);
    }

    public static int getScreenWidthInDp(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) (((float) displayMetrics.widthPixels) / displayMetrics.density);
    }
}
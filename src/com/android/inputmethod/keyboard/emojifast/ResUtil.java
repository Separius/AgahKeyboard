package com.android.inputmethod.keyboard.emojifast;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ArrayRes;
import android.support.annotation.AttrRes;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;

/**
 * Created by sepehr on 2/2/17.
 */
public class ResUtil {

    public static int getColor(Context context, @AttrRes int attr) {
        final TypedArray styledAttributes = context.obtainStyledAttributes(new int[]{attr});
        final int        result           = styledAttributes.getColor(0, -1);
        styledAttributes.recycle();
        return result;
    }

    public static int getDrawableRes(Context c, @AttrRes int attr) {
        return getDrawableRes(c.getTheme(), attr);
    }

    public static int getDrawableRes(Resources.Theme theme, @AttrRes int attr) {
        final TypedValue out = new TypedValue();
        theme.resolveAttribute(attr, out, true);
        return out.resourceId;
    }

    public static Drawable getDrawable(Context c, @AttrRes int attr) {
        return ContextCompat.getDrawable(c, getDrawableRes(c, attr));
    }

    public static int[] getResourceIds(Context c, @ArrayRes int array) {
        final TypedArray typedArray  = c.getResources().obtainTypedArray(array);
        final int[]      resourceIds = new int[typedArray.length()];
        for (int i = 0; i < typedArray.length(); i++) {
            resourceIds[i] = typedArray.getResourceId(i, 0);
        }
        typedArray.recycle();
        return resourceIds;
    }
}
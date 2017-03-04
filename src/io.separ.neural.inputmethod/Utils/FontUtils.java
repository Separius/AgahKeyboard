package io.separ.neural.inputmethod.Utils;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;

import java.util.HashMap;

/**
 * Created by sepehr on 11/16/16.
 */

public class FontUtils {
    private static final HashMap<String, Typeface> mTypefaces = new HashMap<>();

    private static String currentLocale;

    private static boolean isEmoji;

    public static void initialize(@NonNull final Context newContext){
        //mTypefaces.put("emoji", Typeface.createFromAsset(newContext.getResources().getAssets(), "fonts/emojione-android.ttf"));
        mTypefaces.put("fa", Typeface.createFromAsset(newContext.getResources().getAssets(), "fonts/Samim.ttf"));
    }

    public static Typeface getTypeface(String locale){
        /*if(isEmoji)
            return mTypefaces.get("emoji");
        else*/
            return mTypefaces.get(locale);
    }

    public static Typeface getLocaleTypeface(){
        return mTypefaces.get(currentLocale);
    }

    public static Typeface getTypeface(){
        return getTypeface(currentLocale);
    }

    public static void setCurrentLocale(String newLocale){
        currentLocale = newLocale;
    }

    public static void setIsEmoji(boolean emoji){
        isEmoji = emoji;
    }

    public static Typeface getCurrentLocaleTypeface(){
        return mTypefaces.get(currentLocale);
    }
}

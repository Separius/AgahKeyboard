package com.android.inputmethod.keyboard.top.services;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.inputmethod.keyboard.KeyboardTheme;
import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.File;

import io.separ.neural.inputmethod.indic.R;
import io.separ.neural.inputmethod.slash.NeuralApplication;
import io.separ.neural.inputmethod.slash.RImage;
import io.separ.neural.inputmethod.slash.RSearchItem;
import io.separ.neural.inputmethod.slash.RServiceItem;

/**
 * Created by sepehr on 3/2/17.
 */
public class ImageUtils {
    //public static final int[] CATEGORIES_BGS;
    //public static final int[] CATEGORIES_TEXT_COLORS;
    public static final int[] KEYBOARD_BAR_COLORS;
    private static int materialIndex;
    private static int[] sMaterialColors;
    private static int sThemeId;

    static {
        sMaterialColors = new int[]{-6543440, -3285959, -26624, -14575885};
        materialIndex = 0;
        KEYBOARD_BAR_COLORS = new int[]{-1, -1, -10786704, -1, -3355444, -12434878, -1};
        //CATEGORIES_BGS = new int[]{R.drawable.m_dark_category_bg, R.drawable.holo_dark_category_bg, R.drawable.m_light_category_bg, R.drawable.holo_dark_category_bg, R.drawable.zck1_category_bg, R.drawable.zck2_category_bg, R.drawable.zck3_category_bg};
        //CATEGORIES_TEXT_COLORS = new int[]{R.color.m_dark_category_text, R.color.holo_dark_category_text, R.color.m_light_category_text, R.color.holo_dark_category_text, R.color.zck1_category_text, R.color.zck2_category_text, R.color.zck3_category_text};
        sThemeId = -1;
    }

    public static void showColoredImage(SimpleDraweeView target, RServiceItem serviceItem) {
        showColoredImage(target, serviceItem, false);
    }

    public static void showColoredImage(SimpleDraweeView target, RServiceItem serviceItem, boolean forceLightTheme) {
        if ((isLightTheme() || forceLightTheme) && !TextUtils.isEmpty(serviceItem.getImageLight())) {
            setPlaceholder(target, getDrawableId("cache_" + serviceItem.getSlash() + "_light"));
            target.setImageURI(Uri.parse(serviceItem.getImageLight()));
        } else if (!isLightTheme() && !TextUtils.isEmpty(serviceItem.getImageDark())) {
            setPlaceholder(target, getDrawableId("cache_" + serviceItem.getSlash() + "_dark"));
            target.setImageURI(Uri.parse(serviceItem.getImageDark()));
        } else if (TextUtils.isEmpty(serviceItem.getResId())) {
            showImageFromRessources(target, getDrawableId("icon_myslash"), false, 0);
        } else {
            showImageFromRessources(target, getDrawableId("cache_" + serviceItem.getResId() + "_light"), false, 0);
        }
    }

    public static void showSearchItemImage(SimpleDraweeView target, RSearchItem searchItem) {
        Uri uri;
        RImage image = searchItem.getImage();
        if (TextUtils.isEmpty(image.getUrl())) {
            if (image.getResId() != -1) {
                uri = new Uri.Builder().scheme(UriUtil.LOCAL_RESOURCE_SCHEME).path(String.valueOf(image.getResId())).build();
            } else {
                uri = Uri.parse("");
            }
        } else if (image.getUrl().startsWith("/")) {
            uri = Uri.fromFile(new File(image.getUrl()));
        } else {
            uri = Uri.parse(image.getUrl());
        }
        ImageRequestBuilder requestBuilder = ImageRequestBuilder.newBuilderWithSource(uri);
        if (!TextUtils.isEmpty(image.getWidth())) {
            int width = Integer.parseInt(image.getWidth());
            int height = Integer.parseInt(image.getHeight());
            if (width > 0 && height > 0) {
                requestBuilder.setResizeOptions(new ResizeOptions(width, height));
            }
        }
        target.setController((Fresco.newDraweeControllerBuilder().setImageRequest(requestBuilder.build()).setAutoPlayAnimations(true)).build());
        int[] iArr = sMaterialColors;
        int i = materialIndex;
        materialIndex = i + 1;
        Drawable drawable = new ColorDrawable(iArr[i % sMaterialColors.length]);
        GenericDraweeHierarchy hierarchy = (GenericDraweeHierarchy) target.getHierarchy();
        if ("4sq".equals(searchItem.getService())) {
            //hierarchy.setBackgroundImage(new ColorDrawable(-1513240));
        }
        hierarchy.setPlaceholderImage(drawable);
        hierarchy.setFadeDuration(100);
    }/*

    public static void showMonochromeImage(SimpleDraweeView target, RServiceItem serviceItem, int colorFilter) {
        if (serviceItem.isMyslash()) {
            String slashPrefix = "/" + serviceItem.getSlash().substring(0, Math.min(3, serviceItem.getSlash().length()));
            target.setImageURI(Uri.parse(serviceItem.getSlash()));
            target.setImageURI(Uri.parse(serviceItem.getSlash()));
            Drawable drawable = TextDrawable.builder().beginConfig().fontSize((int) (NeuralApplication.getInstance().getResources().getDimension(R.dimen.config_suggestions_strip_height) * 0.34f)).textColor(colorFilter).bold().endConfig().buildRect(slashPrefix.toUpperCase(), 0);
            drawable.setPadding(new Rect());
            target.getHierarchy().setPlaceholderImage(drawable);
            return;
        }
        target.setColorFilter(new PorterDuffColorFilter(colorFilter, PorterDuff.Mode.SRC_ATOP));
        int ressourceId = getDrawableId("cache_" + serviceItem.getSlash() + "_bar");
        if (ressourceId != 0) {
            ((GenericDraweeHierarchy) target.getHierarchy()).setPlaceholderImage(NeuralApplication.getInstance().getResources().getDrawable(ressourceId), ScalingUtils.ScaleType.FIT_CENTER);
        } else {
            ((GenericDraweeHierarchy) target.getHierarchy()).setPlaceholderImage(null);
        }
        if (TextUtils.isEmpty(serviceItem.getImageBar())) {
            target.setImageURI(Uri.parse(""));
        } else {
            target.setImageURI(Uri.parse(serviceItem.getImageBar()));
        }
    }*/

    public static void showImageFromRessources(SimpleDraweeView target, int resId, boolean applyFilter, int colorFilter) {
        Uri uri = new Uri.Builder().scheme(UriUtil.LOCAL_RESOURCE_SCHEME).path(String.valueOf(resId)).build();
        if (applyFilter) {
            target.setColorFilter(new PorterDuffColorFilter(colorFilter, PorterDuff.Mode.SRC_ATOP));
        }
        target.setImageURI(uri);
    }

    public static void updateTheme(Context context) {
        sThemeId = KeyboardTheme.getKeyboardTheme(PreferenceManager.getDefaultSharedPreferences(context)).mThemeId;
    }

    public static int getThemeId() {
        return sThemeId;
    }

    public static boolean isLightTheme(int themeId) {
        return themeId == 2 || themeId == 5;
    }

    public static boolean isLightTheme() {
        return isLightTheme(sThemeId);
    }

    /*//public static int getCategoryBg() {
        return CATEGORIES_BGS[sThemeId];
    }

    public static int getCategoryTextColor(Context context) {
        return context.getResources().getColor(CATEGORIES_TEXT_COLORS[sThemeId]);
    }*/

    private static int getDrawableId(String resName) {
        return NeuralApplication.getInstance().getResources().getIdentifier(resName, "drawable", NeuralApplication.getInstance().getPackageName());
    }

    private static void setPlaceholder(SimpleDraweeView target, int resId) {
        if (resId != 0) {
            target.getHierarchy().setPlaceholderImage(NeuralApplication.getInstance().getResources().getDrawable(resId), ScalingUtils.ScaleType.FIT_CENTER);
        } else {
            target.getHierarchy().setPlaceholderImage(null);
        }
    }
}

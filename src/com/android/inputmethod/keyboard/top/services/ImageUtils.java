package com.android.inputmethod.keyboard.top.services;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.File;

import io.separ.neural.inputmethod.slash.NeuralApplication;
import io.separ.neural.inputmethod.slash.RImage;
import io.separ.neural.inputmethod.slash.RSearchItem;
import io.separ.neural.inputmethod.slash.RServiceItem;

/**
 * Created by sepehr on 3/2/17.
 */
public class ImageUtils {
    public static final int[] KEYBOARD_BAR_COLORS;
    private static int materialIndex;
    private static int[] sMaterialColors;
    private static int sThemeId;

    static {
        sMaterialColors = new int[]{-6543440, -3285959, -26624, -14575885};
        materialIndex = 0;
        KEYBOARD_BAR_COLORS = new int[]{-1, -1, -10786704, -1, -3355444, -12434878, -1};
        sThemeId = -1;
    }

    public static void showColoredImage(SimpleDraweeView target, RServiceItem serviceItem) {
        showColoredImage(target, serviceItem, false);
    }

    public static void showColoredImage(SimpleDraweeView target, RServiceItem serviceItem, boolean forceLightTheme) {
        showImageFromRessources(target, getDrawableId("cache_" + serviceItem.getSlash() + "_light"), false, 0);
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
        GenericDraweeHierarchy hierarchy = target.getHierarchy();
        if ("4sq".equals(searchItem.getService())) {
            hierarchy.setBackgroundImage(new ColorDrawable(-1513240));
        }
        hierarchy.setPlaceholderImage(drawable);
        hierarchy.setFadeDuration(100);
    }

    public static void showImageFromRessources(SimpleDraweeView target, int resId, boolean applyFilter, int colorFilter) {
        Uri uri = new Uri.Builder().scheme(UriUtil.LOCAL_RESOURCE_SCHEME).path(String.valueOf(resId)).build();
        if (applyFilter) {
            target.setColorFilter(new PorterDuffColorFilter(colorFilter, PorterDuff.Mode.SRC_ATOP));
        }
        target.setImageURI(uri);
    }

    public static int getThemeId() {
        return sThemeId;
    }

    public static int getDrawableId(String resName) {
        return NeuralApplication.getInstance().getResources().getIdentifier(resName, "drawable", NeuralApplication.getInstance().getPackageName());
    }
}

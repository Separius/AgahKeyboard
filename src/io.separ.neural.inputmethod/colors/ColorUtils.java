package io.separ.neural.inputmethod.colors;

/**
 * Created by sepehr on 1/26/17.
 */

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;

public class ColorUtils {
    private static final int[] DEFAULT_COLORS;
    public static final String MATERIAL_LIGHT = "#eceff1";
    public static final int NO_COLOR = 1000;
    static final int NUM_COLORS = 1;

    public static String convertColor(int color) {
        Object[] objArr = new Object[NUM_COLORS];
        objArr[0] = Integer.valueOf(ViewCompat.MEASURED_SIZE_MASK & color);
        return String.format("#%06X", objArr);
    }

    public enum ButtonType {
        NONE,
        FLAT,
        BIG
    }

    static {
        DEFAULT_COLORS = new int[]{NO_COLOR, -1644826, -4342339, -657931};
    }

    public static ButtonType getButtonType() {
        return ButtonType.NONE;
    }

    public static ColorProfile setProfileFromApp(Context context, String packageName) {
        ColorProfile profile = new ColorProfile();
        try {
            if (profile.isInvalid() && WindowChangeDetectingService.isActivity()) {
                profile = ColorExtractor.getActivityProfile(context, WindowChangeDetectingService.getActivityInfo());
            }
            if (profile.isInvalid()) {
                profile = ColorExtractor.getApplicationProfile(context, packageName);
            }
            if (profile.isInvalid()) {
                profile = ColorExtractor.getLaunchActivityProfile(context, packageName);
            }
            try {
                if (profile.isInvalid()) {
                    profile = ColorExtractor.getColorProfile(context, packageName);
                }
                if (!profile.isInvalid()) {
                    return profile;
                }
                profile.setPrimary(ColorExtractor.getIconColor(context, packageName));
                return profile;
            } catch (Exception e) {
                return new ColorProfile(Color.parseColor(MATERIAL_LIGHT), NO_COLOR, NO_COLOR);
            }
        } catch (Exception e2) {
            try {
                if (profile.isInvalid()) {
                    profile = ColorExtractor.getColorProfile(context, packageName);
                }
                if (!profile.isInvalid()) {
                    return profile;
                }
                profile.setPrimary(ColorExtractor.getIconColor(context, packageName));
                return profile;
            } catch (Exception e3) {
                return new ColorProfile(Color.parseColor(MATERIAL_LIGHT), NO_COLOR, NO_COLOR);
            }
        } catch (Throwable th) {
            try {
                if (profile.isInvalid()) {
                    profile = ColorExtractor.getColorProfile(context, packageName);
                }
                if (profile.isInvalid()) {
                    profile.setPrimary(ColorExtractor.getIconColor(context, packageName));
                }
            } catch (Exception e4) {
                profile = new ColorProfile(Color.parseColor(MATERIAL_LIGHT), NO_COLOR, NO_COLOR);
                return profile;
            }
        }
        return profile;
    }

    static boolean isGrey(int color) {
        return Color.red(color) == Color.blue(color) && Color.blue(color) == Color.green(color);
    }

    static boolean isDefaultColor(int color) {
        boolean z = false;
        int[] iArr = DEFAULT_COLORS;
        int length = iArr.length;
        for (int i = 0; i < length; i += NUM_COLORS) {
            if (iArr[i] == color) {
                return true;
            }
        }
        if (isGrey(color) || color == Color.parseColor(MATERIAL_LIGHT)) {
            z = true;
        }
        return z;
    }

    public static boolean isColorDark(int color) {
        return 1.0d - ((((0.299d * ((double) Color.red(color))) + (0.587d * ((double) Color.green(color)))) + (0.114d * ((double) Color.blue(color)))) / 255.0d) >= 0.2d;
    }

    static int darkerColor(int color, float darkness) {
        if (color == NO_COLOR) {
            return color;
        }
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] * darkness;
        return Color.HSVToColor(hsv);
    }

    static int darkerColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] * 0.75f;
        return Color.HSVToColor(hsv);
    }

    public static int lightColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] / 0.75f;
        return Color.HSVToColor(hsv);
    }

    public static int flipBrightness(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        if(hsv[2] < 0.5)
            hsv[2] = hsv[2] + 0.4f;
        else
            hsv[2] = hsv[2] - 0.4f;
        return Color.HSVToColor(hsv);
    }

    public static int getTextColor() {
        ColorProfile lastProfile = ColorManager.getLastProfile();
        return getTextColor(lastProfile);
    }

    public static int getTextColor(ColorProfile lastProfile) {
        if (getColorDistance(lastProfile.getPrimary(), lastProfile.getAccent()) >= 70.0d)
            return lastProfile.getAccent();
        if (isColorDark(lastProfile.getPrimary()))
            return lightColor(lastProfile.getAccent(), 0.4f);
        else
            return darkerColor(lastProfile.getAccent(), 0.4f);
    }

    static double getColorDistance(int color1, int color2) {
        float[] yuv1 = getYUV(Color.red(color1), Color.green(color1), Color.blue(color1));
        float[] yuv2 = getYUV(Color.red(color2), Color.green(color2), Color.blue(color2));
        return Math.sqrt((Math.pow((double) (yuv1[0] - yuv2[0]), 2.0d) + Math.pow((double) (yuv1[NUM_COLORS] - yuv2[NUM_COLORS]), 2.0d)) + Math.pow((double) (yuv1[2] - yuv2[2]), 2.0d));
    }

    private static float[] getYUV(int red, int green, int blue) {
        float[][] m = new float[][]{new float[]{0.299f, 0.578f, 0.114f}, new float[]{-0.14713f, -0.28886f, 0.436f}, new float[]{0.615f, -0.51499f, -0.10001f}};
        return new float[]{((m[0][0] * ((float) red)) + (m[0][NUM_COLORS] * ((float) green))) + (m[0][2] * ((float) blue)), ((m[NUM_COLORS][0] * ((float) red)) + (m[NUM_COLORS][NUM_COLORS] * ((float) green))) + (m[NUM_COLORS][2] * ((float) blue)), ((m[2][0] * ((float) red)) + (m[2][NUM_COLORS] * ((float) green))) + (m[2][2] * ((float) blue))};
    }

    public static int lightColor(int color, float deep) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] / deep;
        return Color.HSVToColor(hsv);
    }

    static int getAccent(int color) {
        return isColorDark(color) ? lightColor(color, 0.4f) : darkerColor(color, 0.4f);
    }


}
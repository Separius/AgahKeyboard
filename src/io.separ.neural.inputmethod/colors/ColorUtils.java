package io.separ.neural.inputmethod.colors;

/**
 * Created by sepehr on 1/26/17.
 */

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.PowerManager;
import android.support.v4.view.ViewCompat;

public class ColorUtils {
    private static final String BATTERY_COLOR = "#f5511e";
    private static final int[] DEFAULT_COLORS;
    private static final int DISTANCE_THRESHOLD = 70;
    public static final String MATERIAL_LIGHT = "#eceff1";
    public static final int NO_COLOR = 1000;
    static final int NUM_COLORS = 1;
    private static final String TAG;

    /* renamed from: com.android.inputmethodcommon.ColorUtils.1 */
    static /* synthetic */ class C02881 {
        static final /* synthetic */ int[] $SwitchMap$com$android$inputmethodcommon$ColorUtils$ColorMode;

        static {
            $SwitchMap$com$android$inputmethodcommon$ColorUtils$ColorMode = new int[ColorMode.values().length];
            try {
                $SwitchMap$com$android$inputmethodcommon$ColorUtils$ColorMode[ColorMode.FIXED.ordinal()] = ColorUtils.NUM_COLORS;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$inputmethodcommon$ColorUtils$ColorMode[ColorMode.ADAPT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$inputmethodcommon$ColorUtils$ColorMode[ColorMode.AMOLED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public enum ButtonType {
        NONE,
        FLAT,
        BIG
    }

    private enum ColorMode {
        FIXED,
        ADAPT,
        AMOLED
    }

    public enum ForceType {
        NONE,
        ONLY_FLAT,
        NOT_GRADIENT
    }

    static {
        DEFAULT_COLORS = new int[]{NO_COLOR, -1644826, -4342339, -657931};
        TAG = ColorUtils.class.getSimpleName();
    }

    public static boolean amoledTheme() {
        return false;
    }

    public static boolean overrideGestureTrail() {
        return false;
    }

    public static boolean fixedColor() {
        return false;
    }

    public static ButtonType getButtonType() {
        return ButtonType.NONE;
    }

    private static int getFixedColor() {
        return 0;
    }

    private static boolean getBatterySaver() {
        return false;
    }

    private static ColorMode getColorMode() {
        return ColorMode.ADAPT;
    }

    private static boolean isBatterySaverOn(Context context) {
        return VERSION.SDK_INT >= 21 && ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isPowerSaveMode();
    }

    static ColorProfile getProfile(Context context, String packageName) {
        ColorProfile newProfile = new ColorProfile();
        if (getBatterySaver() && isBatterySaverOn(context)) {
            newProfile.setProfile(Color.parseColor(BATTERY_COLOR), darkerColor(Color.parseColor(BATTERY_COLOR)), -1);
            return newProfile;
        }
        newProfile = setProfileFromApp(context, packageName);
        return newProfile;
    }

    static int getNoColor() {
        return Color.parseColor(MATERIAL_LIGHT);
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

    static int getPrimaryColor(Context context, String packageName) throws NameNotFoundException {
        int color = NO_COLOR;
        try {
            if (WindowChangeDetectingService.isActivity()) {
                color = ColorExtractor.getActivityPrimaryColor(context, WindowChangeDetectingService.getActivityInfo());
            }
            if (color == NO_COLOR || isDefaultColor(color)) {
                color = ColorExtractor.getLaunchActivityColor(context, packageName);
            }
            if (color == NO_COLOR || isDefaultColor(color)) {
                color = ColorExtractor.getApplicationColor(context, packageName);
            }
            if (color == NO_COLOR || isDefaultColor(color)) {
                color = ColorExtractor.getIconColor(context, packageName);
            }
        } catch (Exception e) {
        }
        return color;
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
            hsv[2] = hsv[2] * 0.8f;
            return Color.HSVToColor(hsv);
        }

        private static int darkerColor(int color, int deep) {
            switch (deep) {
                case 0 /*0*/:
                    return color;
                case NUM_COLORS /*1*/:
                    return darkerColor(color);
                default:
                    return darkerColor(darkerColor(color, deep - 1));
            }
        }

        public static int lightColor(int color) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] = hsv[2] / 0.8f;
            return Color.HSVToColor(hsv);
        }

        public static int getContrastColor(int color) {
            return (!isColorDark(color) || color == lightColor(color)) ? darkerColor(color) : lightColor(color);
        }

        public static int getTextColor() {
            return ColorManager.getLastProfile().getTextColor();
        }

        public static int getTextColor(int mainColor) {
            ColorProfile lastProfile = ColorManager.getLastProfile();
            if (getColorDistance(lastProfile.getPrimary(), lastProfile.getAccent()) >= 70.0d) {
                return lastProfile.getAccent();
            }
            if (isColorDark(lastProfile.getPrimary())) {
                return lightColor(lastProfile.getAccent(), 0.4f);
            }
            return darkerColor(lastProfile.getAccent(), 0.4f);
        }

        public static int getPathColor() {
            ColorProfile lastProfile = ColorManager.getLastProfile();
            return lastProfile.getAccent();
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

        private static int lightColor(int color, float deep) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] = hsv[2] / deep;
            return Color.HSVToColor(hsv);
        }

        static String convertColor(int color) {
            Object[] objArr = new Object[NUM_COLORS];
            objArr[0] = Integer.valueOf(ViewCompat.MEASURED_SIZE_MASK & color);
            return String.format("#%06X", objArr);
        }

        static boolean isColor(String color) {
            try {
                Color.parseColor(color);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private static boolean canGetBackground() {
            return true;
        }

        static int getAccent(int color) {
            return isColorDark(color) ? lightColor(color) : darkerColor(color);
        }


    }
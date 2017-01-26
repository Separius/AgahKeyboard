package io.separ.neural.inputmethod.colors;

/**
 * Created by sepehr on 1/26/17.
 */

import android.graphics.Color;

public class ColorProfile {
    private int accent;
    private float darkFactor;
    private int primary;
    private int primaryDark;
    private int text;
    private int icon;

    public ColorProfile() {
        this.primary = 1000;
        this.primaryDark = 1000;
        this.accent = 1000;
        this.darkFactor = 1.f;
        this.primary = 1000;
        this.primaryDark = 1000;
        this.accent = 1000;
        this.text = 1000;
        this.icon = 1000;
    }

    private boolean isInvertDark() {
        return ColorUtils.getButtonType() == ColorUtils.ButtonType.FLAT;
    }

    public ColorProfile(int primary, int primaryDark, int accent) {
        this.primary = 1000;
        this.primaryDark = 1000;
        this.accent = 1000;
        this.darkFactor = 1.f;
        this.text = 1000;
        this.icon = 1000;
        setProfile(primary, primaryDark, accent);
    }

    public boolean isInvalid() {
        return this.primary == 1000 || ColorUtils.isDefaultColor(this.primary);
    }

    public boolean isValid() {
        return (isInvalid() || this.primaryDark == 1000 || ColorUtils.isDefaultColor(this.primaryDark)) ? false : true;
    }

    public boolean isPerfect() {
        return (!isValid() || this.accent == 1000 || ColorUtils.isDefaultColor(this.accent)) ? false : true;
    }

    public boolean isPrimaryDark() {
        return ColorUtils.isColorDark(this.primary);
    }

    public void setDarkFactor(float darkness) {
        this.darkFactor = darkness;
    }

    public void resetProfile() {
        this.primary = 1000;
        this.primaryDark = 1000;
        this.accent = 1000;
        this.text = 1000;
        this.icon = 1000;
    }

    public void setProfile(int primary, int primaryDark, int accent) {
        resetProfile();
        this.primary = primary;
        if (primaryDark == 1000 || ((!ColorUtils.isGrey(primary) && ColorUtils.isGrey(primaryDark)) || ColorUtils.getColorDistance(primary, primaryDark) >= 80.0d)) {
            this.primaryDark = ColorUtils.darkerColor(primary);
        } else {
            this.primaryDark = primaryDark;
        }
        if (accent == 1000 || ColorUtils.getColorDistance(primary, accent) <= 30.0d) {
            this.accent = ColorUtils.getAccent(primary);
        } else {
            this.accent = accent;
        }
        double luminance = Color.red(primary)/255.0*0.2126 + Color.green(primary)/255.0*0.7152+Color.blue(primary)/255.0*0.0722;
        if(luminance < 0.5)
            this.text = Color.WHITE;
        else
            this.text = Color.BLACK;
        /*float[] hsv = new float[3];
        Color.RGBToHSV(Color.red(primary), Color.green(primary), Color.blue(primary), hsv);
        hsv[0] = ((int)hsv[0]+180)%360;
        hsv[1] = 1.f - hsv[1];
        hsv[1] = 1.f - hsv[2];
        this.icon = Color.HSVToColor(Color.alpha(primary), hsv);*/
        this.icon = Color.rgb(Color.red(primary) ^ 0x80, Color.green(primary) ^ 0x80, Color.blue(primary) ^ 0x80);
    }

    public int getIcon(){
        return icon;
    }

    public int getText(){
        return text;
    }

    public void setProfile(ColorProfile newProfile) {
        resetProfile();
        this.primary = newProfile.primary;
        this.primaryDark = newProfile.primaryDark;
        this.accent = newProfile.accent;
        this.text = newProfile.text;
        this.icon = newProfile.icon;
    }

    public int getPrimary() {
        if (isInvertDark()) {
            return getPrimaryDarkIgnore();
        }
        if (this.primary != 1000) {
            return ColorUtils.darkerColor(this.primary, this.darkFactor);
        }
        return Color.parseColor(ColorUtils.MATERIAL_LIGHT);
    }

    public int getPrimaryIgnore() {
        if (this.primary != 1000) {
            return ColorUtils.darkerColor(this.primary, this.darkFactor);
        }
        return Color.parseColor(ColorUtils.MATERIAL_LIGHT);
    }

    public int getPrimaryDarkIgnore() {
        if (this.primaryDark != 1000) {
            return ColorUtils.darkerColor(this.primaryDark, this.darkFactor);
        }
        return ColorUtils.darkerColor(getPrimaryIgnore());
    }

    public void setPrimary(int primary) {
        setProfile(primary, 1000, 1000);
    }

    public int getPrimaryDark() {
        if (isInvertDark()) {
            return getPrimaryIgnore();
        }
        if (this.primaryDark != 1000) {
            return ColorUtils.darkerColor(this.primaryDark, this.darkFactor);
        }
        return ColorUtils.darkerColor(getPrimaryIgnore());
    }

    public int getPrimaryDarker(float factor) {
        return ColorUtils.darkerColor(this.primaryDark, factor);
    }

    public int getTextColor() {
        return ColorUtils.getTextColor(getPrimaryIgnore());
    }

    public int getTextColorForcingColor(int hoverColor) {
        return ColorUtils.getTextColor(hoverColor);
    }

    public int getAccent() {
        if (this.accent != 1000) {
            return this.accent;
        }
        return ColorUtils.lightColor(this.primary);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ColorProfile that = (ColorProfile) o;
        if (this.primary == that.primary && this.primaryDark == that.primaryDark && this.accent == that.accent) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (((this.primary * 31) + this.primaryDark) * 31) + this.accent;
    }
}

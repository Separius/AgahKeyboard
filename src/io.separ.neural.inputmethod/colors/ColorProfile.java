package io.separ.neural.inputmethod.colors;

/**
 * Created by sepehr on 1/26/17.
 */

import android.graphics.Color;

import static io.separ.neural.inputmethod.colors.ColorUtils.darkerColor;
import static io.separ.neural.inputmethod.colors.ColorUtils.getColorDistance;
import static io.separ.neural.inputmethod.colors.ColorUtils.lightColor;

public class ColorProfile {
    private int accent;
    private int primary;
    private int primaryDark;
    private int text;
    private int icon;

    public ColorProfile() {
        resetProfile();
    }

    public ColorProfile(int primary, int primaryDark, int accent) {
        setProfile(primary, primaryDark, accent);
    }

    public boolean isInvalid() {
        return this.primary == 1000 || ColorUtils.isDefaultColor(this.primary);
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
        if (primaryDark == 1000 || ((!ColorUtils.isGrey(primary) && ColorUtils.isGrey(primaryDark)) || getColorDistance(primary, primaryDark) >= 80.0d)) {
            this.primaryDark = darkerColor(primary);
        } else {
            this.primaryDark = primaryDark;
        }
        if (accent == 1000 || getColorDistance(primary, accent) <= 30.0d) {
            this.accent = ColorUtils.getAccent(primary);
        } else {
            this.accent = accent;
        }
        double luminance = Color.red(primary) / 255.0 * 0.2126 + Color.green(primary) / 255.0 * 0.7152 + Color.blue(primary) / 255.0 * 0.0722;
        if (luminance < 0.5)
            this.text = Color.WHITE;
        else
            this.text = Color.BLACK;
        this.icon = getIcon(primary);
    }

    public static int getIcon(int primary){
        return Color.rgb(Color.red(primary) ^ 0x80, Color.green(primary) ^ 0x80, Color.blue(primary) ^ 0x80);
    }

    public int getIconOnSecondary() {
        /*int tmp = getSecondary();
        return Color.rgb(Color.red(tmp) ^ 0x80, Color.green(tmp) ^ 0x80, Color.blue(tmp) ^ 0x80);*/
        int tmp = getIcon();
        if (text == Color.BLACK)
            return darkerColor(tmp);
        else
            return lightColor(tmp);
    }

    public int getIcon() {
        if (this.accent != 1000) {
            if (getColorDistance(getPrimary(), getAccent()) >= 70.0d)
                return getAccent();
        }
            /*if (isColorDark(getPrimary()))
                return lightColor(getAccent(), 0.4f);
            return darkerColor(getAccent(), 0.4f);
        } else*/
        return icon;
    }

    public int getTextColor() {
        return getIconOnSecondary();
        //return ColorUtils.getTextColor();
    }

    public int getText() {
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
        return this.primary;
    }

    public int getPrimaryIgnore() {
        if (this.primary != 1000) {
            return primary;
        }
        return Color.parseColor(ColorUtils.MATERIAL_LIGHT);
    }

    public void setPrimary(int primary) {
        setProfile(primary, 1000, 1000);
    }

    public int getPrimaryDark() {
        if (this.primaryDark != 1000) {
            return primaryDark;
        }
        return darkerColor(getPrimaryIgnore());
    }

    public int getSecondary() {
        if (text == Color.BLACK)
            return getPrimaryDark();
        else
            return getPrimaryLight();
    }

    private int getPrimaryLight() {
        return lightColor(this.primary);
    }

    public int getAccent() {
        if (this.accent != 1000) {
            return this.accent;
        }
        return lightColor(this.primary);
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

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void setAccent(int accent) {
        this.accent = accent;
    }
}

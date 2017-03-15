package io.separ.neural.inputmethod.colors;

/**
 * Created by sepehr on 1/26/17.
 */

import android.graphics.Color;

import com.mattyork.colours.Colour;

import static io.separ.neural.inputmethod.colors.ColorUtils.darkerColor;
import static io.separ.neural.inputmethod.colors.ColorUtils.flipBrightness;
import static io.separ.neural.inputmethod.colors.ColorUtils.getColorDistance;
import static io.separ.neural.inputmethod.colors.ColorUtils.isColorDark;
import static io.separ.neural.inputmethod.colors.ColorUtils.lightColor;

public class ColorProfile {
    private int accent;
    private int primary;
    private int text;
    private int secondary;
    private int secondaryAccent;

    public ColorProfile() {
        resetProfile();
    }

    public ColorProfile(int primary, int primaryDark, int accent) {
        setProfile(primary, primaryDark, accent);
    }

    public boolean isInvalid() {
        return primary == 1000 || ColorUtils.isDefaultColor(primary);
    }

    public void resetProfile() {
        primary = 1000;
        accent = 1000;
        text = 1000;
        secondary = 1000;
        secondaryAccent = 1000;
    }

    public void setProfile(int primary, int primaryDark, int accent) {
        this.primary = primary;
        //double luminance = Color.red(primary) / 255.0 * 0.2126 + Color.green(primary) / 255.0 * 0.7152 + Color.blue(primary) / 255.0 * 0.0722;
        //int[] complementaryColors = Colour.colorSchemeOfType(primary, Colour.ColorScheme.ColorSchemeMonochromatic); //=> returns 4 colours
        if (isColorDark(primary)){
            text = Color.WHITE;
            secondary = lightColor(primary);
        } else {
            text = Color.BLACK;
            secondary = darkerColor(primary);
        }
        //secondary = flipBrightness(primary);
        //secondary = complementaryColors[1];
        /*if ((accent != 1000) && (getColorDistance(primary, accent) >= 70.0d)) {
            this.accent = accent;
            this.secondaryAccent = accent;
        }else{*/
            this.accent = ColorUtils.getAccent(primary);
            //this.accent = complementaryColors[2];
            this.secondaryAccent = ColorUtils.getAccent(secondary);
            //this.secondaryAccent = complementaryColors[0];
        //}
    }

    //TODO
    public static int getIcon(int primary){
        return Color.rgb(Color.red(primary) ^ 0x80, Color.green(primary) ^ 0x80, Color.blue(primary) ^ 0x80);
    }

    public int getIconOnSecondary() {
        return secondaryAccent;
    }

    public int getIcon() {
        return accent;
    }

    public int getTextColor() {
        //return primary;
        return secondaryAccent;
    }

    public int getText() {
        return text;
    }

    public void setProfile(ColorProfile newProfile) {
        primary = newProfile.primary;
        accent = newProfile.accent;
        text = newProfile.text;
        secondary = newProfile.secondary;
        secondaryAccent = newProfile.secondaryAccent;
    }

    public int getPrimary() {
        return primary;
    }

    public void setPrimary(int primary) {
        setProfile(primary, 1000, 1000);
    }

    //TODO bug when color is complete black or white?
    public int getSecondary() {
        return secondary;
    }

    public int getAccent() {
        return accent;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ColorProfile that = (ColorProfile) o;
        if (this.primary == that.primary && this.accent == that.accent) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return ((this.primary * 31) * 31) + this.accent;
    }
}

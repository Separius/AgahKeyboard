package io.separ.neural.inputmethod.colors;

/**
 * Created by sepehr on 1/26/17.
 */

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import static io.separ.neural.inputmethod.Utils.ColorUtils.getColor;

public class ColorManager {
    private static ColorProfile lastProfile;
    private static List<OnColorChange> observers;
    private final OnFinishCalculateProfile masterObserver;

    public interface OnColorChange {
        void onColorChange(ColorProfile colorProfile);
    }

    public interface OnFinishCalculateProfile {
        void finishCalculatingProfile();
    }

    static {
        observers = new ArrayList();
        lastProfile = new ColorProfile();
    }

    public ColorManager(OnFinishCalculateProfile masterObserver) {
        this.masterObserver = masterObserver;
    }

    public static void addObserver(OnColorChange observer) {
        if (observers.contains(observer))
            observers.remove(observer);
        observers.add(observer);
    }

    public static void addObserverAndCall(OnColorChange observer) {
        addObserver(observer);
        observer.onColorChange(lastProfile);
    }

    private static void notifyObservers() {
        for (OnColorChange observer : observers) {
            if (observer != null) {
                observer.onColorChange(lastProfile);
            }
        }
    }

    public static ColorProfile getLastProfile() {
        return lastProfile;
    }

    public void calculateProfile(Context context, String packageName) {
        ColorProfile colorProfile = getColor(context, packageName);
        if (!lastProfile.equals(colorProfile)) {
            //TODO, cool!
            //KeyboardTheme.saveKeyboardThemeId((colorProfile.getText()== Color.BLACK)? "3" : "4", PreferenceManager.getDefaultSharedPreferences(context));
            lastProfile.setProfile(colorProfile);
            notifyObservers();
        }
        this.masterObserver.finishCalculatingProfile();
    }
}

package io.separ.neural.inputmethod.slash;

import android.location.Location;

/**
 * Created by sepehr on 3/2/17.
 */
public class LatLngUtils {
    public static boolean isEmpty(Location location) {
        return location == null || (location.getLatitude() == 0.0d && location.getLongitude() == 0.0d);
    }
}

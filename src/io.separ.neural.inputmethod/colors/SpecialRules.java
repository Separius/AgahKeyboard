package io.separ.neural.inputmethod.colors;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import io.separ.neural.inputmethod.indic.Constants;
import io.separ.neural.inputmethod.indic.LastComposedWord;
import io.separ.neural.inputmethod.indic.R;

/**
 * Created by sepehr on 3/8/17.
 */

public class SpecialRules {
    private static final String TAG;

    /* renamed from: com.android.inputmethodcommon.SpecialRules.1 */
    static /* synthetic */ class C03051 {
        static final /* synthetic */ int[] $SwitchMap$com$android$inputmethodcommon$SpecialRules$Rule;

        static {
            $SwitchMap$com$android$inputmethodcommon$SpecialRules$Rule = new int[Rule.values().length];
            try {
                $SwitchMap$com$android$inputmethodcommon$SpecialRules$Rule[Rule.GOOGLE_MESSENGER.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$inputmethodcommon$SpecialRules$Rule[Rule.TINDER.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$inputmethodcommon$SpecialRules$Rule[Rule.INSTAGRAM.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$inputmethodcommon$SpecialRules$Rule[Rule.PLAY_STORE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$inputmethodcommon$SpecialRules$Rule[Rule.CHROME.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    public enum Rule {
        GOOGLE_MESSENGER,
        INSTAGRAM,
        PLAY_STORE,
        CHROME,
        TINDER,
        NONE
    }

    static {
        TAG = SpecialRules.class.getSimpleName();
    }

    public static Rule getSpecialRule(String packageName) {
        int obj = -1;
        switch (packageName.hashCode()) {
            /*case -1430093937:
                if (packageName.equals("com.google.android.apps.messaging")) {
                    obj = 1;
                    break;
                }
                break;*/
            case -1221330953:
                if (packageName.equals("com.chrome.beta")) {
                    obj = 6;
                    break;
                }
                break;
            case -1046965711:
                if (packageName.equals("com.android.vending")) {
                    obj = 3;
                    break;
                }
                break;
            case -662003450:
                if (packageName.equals("com.instagram.android")) {
                    obj = 2;
                    break;
                }
                break;
            case 256457446:
                if (packageName.equals("com.android.chrome")) {
                    obj = 4;
                    break;
                }
                break;
            case 1245639141:
                if (packageName.equals("com.tinder")) {
                    obj = 0;
                    break;
                }
                break;
            case 1900266798:
                if (packageName.equals("com.chrome.dev")) {
                    obj = 5;
                    break;
                }
                break;
        }
        switch (obj) {
            case 0:
                return Rule.TINDER;
            case 1:
                return Rule.GOOGLE_MESSENGER;
            case 2:
                return Rule.INSTAGRAM;
            case 3:
                return Rule.PLAY_STORE;
            case 4:
                return Rule.CHROME;
            case 5:
                return Rule.CHROME;
            case 6:
                return Rule.CHROME;
            default:
                return Rule.NONE;
        }
    }

    public static boolean overrideStandardColor(String packageName) {
        switch (C03051.$SwitchMap$com$android$inputmethodcommon$SpecialRules$Rule[getSpecialRule(packageName).ordinal()]) {
            case 1:
                return true;
            default:
                return false;
        }
    }

    public static Integer getColor(String packageName, Context context, String... params) {
        return getColor(getSpecialRule(packageName), context, params);
    }

    private static Integer getColor(Rule rule, Context context, String... params) {
        switch (C03051.$SwitchMap$com$android$inputmethodcommon$SpecialRules$Rule[rule.ordinal()]) {
            case 1:
                return getContactColor(context, params[0]);
            case 2:
                return getTinder();
            case 3:
                return getInstagram();
            case 4:
                return getPlayStore();
            case 5:
                return getChrome();
            default:
                return null;
        }
    }

    public static void addColor(Rule rule, Context context, int color, String... params) {
        if (rule == Rule.GOOGLE_MESSENGER) {
            ColorDatabase.addColor(context, "com.google.android.apps.messaging", params[0], ColorUtils.convertColor(color));
        }
    }

    public static void deleteColor(Rule rule, Context context, String... params) {
        if (rule == Rule.GOOGLE_MESSENGER) {
            ColorDatabase.deletePackageTitle(context, "com.google.android.apps.messaging", params[0]);
        }
    }

    private static int getTinder() {
        return Color.parseColor("#FF6E5F");
    }

    private static int getInstagram() {
        return Color.parseColor("#105687");
    }

    private static int getChrome() {
        return Color.parseColor("#039be5");
    }

    private static int getPlayStore() {
        return Color.parseColor("#0f9d58");
    }

    private static int getContactColor(Context context, String contactName) {
        if (contactName == null || contactName.equals(LastComposedWord.NOT_A_SEPARATOR)) {
            return 1000;
        }
        if (ColorDatabase.existPackageTitle(context, "com.google.android.apps.messaging", contactName)) {
            return Color.parseColor(ColorDatabase.getColor(context, "com.google.android.apps.messaging", contactName));
        }
        if (!isAccessibilitySettingsOn(context) || !hasContactPermission(context)) {
            return 1000;
        }
        TypedArray sColors = context.getResources().obtainTypedArray(R.array.letter_tile_colors);
        int colorIndex = Math.abs(contactName.hashCode()) % sColors.length();
        if (!contactName.replaceAll(Constants.WORD_SEPARATOR, LastComposedWord.NOT_A_SEPARATOR).matches("[0-9]+")) {
            String displayName = null;
            String lookupKey = null;
            Cursor mapContact = context.getContentResolver().query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(contactName)), new String[]{"display_name", "lookup"}, null, null, null);
            if (mapContact != null && mapContact.moveToNext()) {
                displayName = mapContact.getString(mapContact.getColumnIndex("display_name"));
                lookupKey = mapContact.getString(mapContact.getColumnIndex("lookup"));
                mapContact.close();
            }
            if (displayName != null) {
                colorIndex = Math.abs(displayName.hashCode()) % sColors.length();
            }
            if (lookupKey != null) {
                colorIndex = Math.abs(lookupKey.hashCode()) % sColors.length();
            }
        } else if (contactName.contains(Constants.WORD_SEPARATOR)) {
            colorIndex += colorIndex < sColors.length() / 2 ? 1 : -1;
        }
        int color = sColors.getColor(colorIndex, 1000);
        sColors.recycle();
        return color;
    }

    private static boolean hasContactPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, "android.permission.READ_CONTACTS") == 0;
    }

    private static boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        String service = mContext.getPackageName() + "/" + WindowChangeDetectingService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.getApplicationContext().getContentResolver(), "accessibility_enabled");
        } catch (Settings.SettingNotFoundException e) {
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(), "enabled_accessibility_services");
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
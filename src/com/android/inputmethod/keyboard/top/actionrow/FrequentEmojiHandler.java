package com.android.inputmethod.keyboard.top.actionrow;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by sepehr on 2/24/17.
 */
public class FrequentEmojiHandler {
    private static final String[] DEFAULT_SUGGESTED_EMOJI;
    private static final String PREFERENCE_NAME = "frequent_emoji";
    private static final String TAG;
    private static FrequentEmojiHandler sInstance;
    private Context mContext;

    /* renamed from: com.gamelounge.emojiLibrary.helper.FrequentEmojiHandler.1 */
    static class C03781 implements Comparator<Object> {
        C03781() {
        }

        public int compare(Object o1, Object o2) {
            int value1 = ((Integer) ((Map.Entry) o1).getValue()).intValue();
            int value2 = ((Integer) ((Map.Entry) o2).getValue()).intValue();
            if (value1 < value2) {
                return 1;
            }
            if (value1 > value2) {
                return -1;
            }
            return 0;
        }
    }

    static {
        TAG = FrequentEmojiHandler.class.getSimpleName();
        DEFAULT_SUGGESTED_EMOJI = "\u2764,\ud83d\ude15,\ud83d\ude18,\ud83d\ude22,\ud83d\ude3b,\ud83d\ude0a,\ud83d\ude09,\ud83d\ude0d,\u2764,\ud83d\ude15,\ud83d\ude18,\ud83d\ude22,\ud83d\ude3b,\ud83d\ude0a,\ud83d\ude09,\ud83d\ude0d".split("\\s*,\\s*");
    }

    private FrequentEmojiHandler(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static FrequentEmojiHandler getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FrequentEmojiHandler(context);
        }
        return sInstance;
    }

    private SharedPreferences getPreferences() {
        return this.mContext.getSharedPreferences(PREFERENCE_NAME, 0);
    }

    public void onEmojiClicked(CharSequence seq) {
        getPreferences().edit().putInt(seq.toString(), getPreferences().getInt(seq.toString(), 0) + 1).apply();
    }

    public List<String> getMostFrequentEmojis(int numberOfEmojis) {
        Map<String, ?> map = sortByValue(getPreferences().getAll());
        List<String> emojis = new LinkedList(map.keySet()).subList(0, Math.min(numberOfEmojis, map.keySet().size()));
        int i = 0;
        while (emojis.size() < numberOfEmojis) {
            if (!emojis.contains(DEFAULT_SUGGESTED_EMOJI[i])) {
                emojis.add(DEFAULT_SUGGESTED_EMOJI[i]);
            }
            i++;
        }
        return emojis;
    }

    private static <K, V> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList(map.entrySet());
        Collections.sort(list, new C03781());
        Map<K, V> result = new LinkedHashMap();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}

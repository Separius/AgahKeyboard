package com.android.inputmethod.keyboard.emojifast;

/**
 * Created by sepehr on 2/1/17.
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;

import io.separ.neural.inputmethod.indic.R;

public class RecentEmojiPageModel implements EmojiPageModel {
    private static final String TAG                  = RecentEmojiPageModel.class.getSimpleName();
    private static final String EMOJI_LRU_PREFERENCE = "pref_recent_emoji2";
    private static final int    EMOJI_LRU_SIZE       = 50;
    private static boolean needsToPersist = false;

    private final SharedPreferences     prefs;
    private final LinkedHashSet<String> recentlyUsed;

    public RecentEmojiPageModel(Context context) {
        this.prefs        = PreferenceManager.getDefaultSharedPreferences(context);
        this.recentlyUsed = getPersistedCache();
    }

    public static LinkedHashSet<String> getPersistedCache(SharedPreferences prefs){
        String serialized = prefs.getString(EMOJI_LRU_PREFERENCE, "[]");
        try {
            CollectionType collectionType = TypeFactory.defaultInstance()
                    .constructCollectionType(LinkedHashSet.class, String.class);
            return JsonUtils.getMapper().readValue(serialized, collectionType);
        } catch (IOException e) {
            Log.w(TAG, e);
            return new LinkedHashSet<>();
        }
    }

    private LinkedHashSet<String> getPersistedCache() {
        return getPersistedCache(prefs);
    }

    @Override public int getIconAttr() {
        return R.attr.iconEmojiRecentsTab;
    }

    @Override public String[] getEmoji() {
        return toReversePrimitiveArray(recentlyUsed);
    }

    @Override public boolean hasSpriteMap() {
        return false;
    }

    @Override public String getSprite() {
        return null;
    }

    @Override public boolean isDynamic() {
        return true;
    }

    public void onCodePointSelected(String emoji) {
        recentlyUsed.remove(emoji);
        recentlyUsed.add(emoji);

        if (recentlyUsed.size() > EMOJI_LRU_SIZE) {
            Iterator<String> iterator = recentlyUsed.iterator();
            iterator.next();
            iterator.remove();
        }
        needsToPersist = true;
    }

    public void persist(){
        if(needsToPersist) {
            final LinkedHashSet<String> latestRecentlyUsed = new LinkedHashSet<>(recentlyUsed);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        String serialized = JsonUtils.toJson(latestRecentlyUsed);
                        prefs.edit()
                                .putString(EMOJI_LRU_PREFERENCE, serialized)
                                .apply();
                        needsToPersist = false;
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }

                    return null;
                }
            }.execute();
        }
    }

    public static String[] toReversePrimitiveArray(@NonNull LinkedHashSet<String> emojiSet) {
        String[] emojis = new String[emojiSet.size()];
        int i = emojiSet.size() - 1;
        for (String emoji : emojiSet) {
            emojis[i--] = emoji;
        }
        return emojis;
    }
}

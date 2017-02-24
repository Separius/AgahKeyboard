package com.android.inputmethod.keyboard.actionrow;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by sepehr on 2/24/17.
 */
public class EmojiPredictor {
    private static final boolean DEBUG = true;
    private static final String TAG;
    private Context context;
    private Map<Locale, Map<String, List<String>>> langMap;
    private Set<Locale> localesList;

    private class BuildEmojiMap extends AsyncTask<String, Void, Map<String, List<String>>> {
        private final Locale locale;

        public BuildEmojiMap(Locale locale) {
            this.locale = locale;
        }

        protected Map<String, List<String>> doInBackground(String... params) {
            try {
                return EmojiPredictor.this.buildEmojiMap(this.locale);
            } catch (JSONException e) {
                e.printStackTrace();
                return new HashMap();
            }
        }

        protected void onPostExecute(Map<String, List<String>> result) {
            EmojiPredictor.this.langMap.put(this.locale, result);
        }

        protected void onPreExecute() {
        }

        protected void onProgressUpdate(Void... values) {
        }
    }

    static {
        TAG = EmojiPredictor.class.getSimpleName();
    }

    public EmojiPredictor(Context context) {
        this.langMap = new HashMap();
        this.localesList = new HashSet();
        this.context = context;
    }

    public void addLanguage(Locale locale) {
        if (this.localesList.contains(locale)) {
            this.langMap.remove(locale);
        }
        new BuildEmojiMap(locale).execute(new String[0]);
    }

    private Map<String, List<String>> buildEmojiMap(Locale locale) throws JSONException {
        Exception ex;
        Map<String, List<String>> emojiMap = new HashMap();
        try {
            FileInputStream is = new FileInputStream(new File(this.context.getFilesDir().getPath() + File.separator + locale.toString() + "_emojis.json"));
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            try {
                JSONObject root = new JSONObject(json);
                Iterator<?> keys = root.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    JSONArray array = root.getJSONArray(key);
                    List<String> emojis = new ArrayList();
                    for (int i = 0; i < array.length(); i++) {
                        emojis.add(array.getString(i));
                    }
                    emojiMap.put(key.toLowerCase(), emojis);
                }
            } catch (Exception e) {
                ex = e;
                String str = json;
                ex.printStackTrace();
                return emojiMap;
            }
        } catch (Exception e2) {
            ex = e2;
            ex.printStackTrace();
            return emojiMap;
        }
        return emojiMap;
    }

    public boolean has(String suggestion, List<Locale> locales) {
        return get(suggestion, locales).size() > 0 ? DEBUG : false;
    }

    public List<String> get(String suggestion, List<Locale> locales) {
        List<String> allEmojis = new ArrayList();
        for (Locale loc : locales) {
            if (this.langMap.containsKey(loc) && ((Map) this.langMap.get(loc)).containsKey(suggestion.toLowerCase())) {
                allEmojis.addAll((Collection) ((Map) this.langMap.get(loc)).get(suggestion.toLowerCase()));
            }
        }
        return allEmojis;
    }
}

package com.android.inputmethod.keyboard.actionrow;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.android.inputmethod.keyboard.KeyboardSwitcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.separ.neural.inputmethod.indic.define.JniLibName;

/**
 * Created by sepehr on 2/24/17.
 */
public class NeuralNetSuggestor {
    private static final boolean DEBUG = true;
    private static final int MAX_SUGGESTIONS = 1000;
    private static final String TAG;
    private static NeuralNetSuggestor suggestor;
    private Set<Locale> activatedLocales;
    private AssetManager assetManager;
    private String baseFolder;
    private EmojiPredictor emojiPredictor;
    protected SharedPreferences prefs;

    private native int initLang(AssetManager assetManager, String str, String str2);

    private native NeuralNetSuggestion[] nextWord(String[] strArr, String str, boolean z, int i);

    static {
        TAG = NeuralNetSuggestor.class.getSimpleName();
        suggestor = new NeuralNetSuggestor();
    }

    private NeuralNetSuggestor() {
        this.activatedLocales = new HashSet();
    }

    public void initLibrary(Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.assetManager = context.getAssets();
        this.baseFolder = context.getFilesDir().getPath() + File.separator;
        this.emojiPredictor = new EmojiPredictor(context);
    }

    public static NeuralNetSuggestor getInstance() {
        return suggestor;
    }

    public NeuralNetSuggestion[] getSuggestion(String[] previous, List<Locale> locales, boolean isComposing) {
        List<NeuralNetSuggestion> suggestionsList = new ArrayList();
        for (Locale locale : locales) {
            if (!this.activatedLocales.contains(locale)) {
                initLangPack(locale);
            }
            NeuralNetSuggestion[] suggestions = nextWord(previous, locale.toString(), isComposing, MAX_SUGGESTIONS / locales.size());
            if (suggestions != null) {
                suggestionsList.addAll(new ArrayList(Arrays.asList(suggestions)));
            }
        }
        return (NeuralNetSuggestion[]) suggestionsList.toArray(new NeuralNetSuggestion[0]);
    }

    private void initLangPack(Locale locale) {
        initLang(this.assetManager, locale.toString(), this.baseFolder);
        this.emojiPredictor.addLanguage(locale);
        this.activatedLocales.add(locale);
        KeyboardSwitcher.getInstance().getmLatinIME().resetSuggest();
    }

    public List<String> getEmojiPrediction(String word, List<Locale> locales) {
        return this.emojiPredictor.get(word, locales);
    }

    public boolean hasEmojiPrediction(String word, List<Locale> locales) {
        return this.emojiPredictor.has(word, locales);
    }
}

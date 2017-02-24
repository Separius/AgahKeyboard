package com.android.inputmethod.keyboard.actionrow;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.separ.neural.inputmethod.indic.LastComposedWord;

import static com.android.inputmethod.keyboard.actionrow.ActionRowView.DEFAULT_SUGGESTED_EMOJI;

/**
 * Created by sepehr on 2/24/17.
 */
public class NeuralRowHelper {
    public static final String EOS_SUGGESTION = "<eos>";
    private static final double EOS_THRESHOLD = 0.95d;
    public static final String NUMBER_SUGGESTION = "N";
    private static final String PUNCTUATION_REGEX = "[?.,/#!$%\\^&\\*;:{}=\\-_`~()]";
    private static final String TAG;
    public static final String UNK = "<unk>";
    private static final long WAITING_TIME = 150;
    private static NeuralRowHelper instance;
    private static final ScheduledExecutorService worker;
    private State currentState;
    private String firstSuggestion;
    private List<String> lastEmojiList;
    private NeuralListener listener;
    private List<String> oldSuggestions;
    Pattern f5p;
    ScheduledFuture<?> sentTask;

    /* renamed from: com.android.inputmethod.keyboard.actionrow.NeuralRowHelper.1 */
    class C02331 implements Runnable {
        final /* synthetic */ String val$composingWord;
        final /* synthetic */ String val$firstSuggestion;
        final /* synthetic */ List val$locales;
        final /* synthetic */ String[] val$prevWords;

        C02331(String[] strArr, String str, List list, String str2) {
            this.val$prevWords = strArr;
            this.val$composingWord = str;
            this.val$locales = list;
            this.val$firstSuggestion = str2;
        }

        public void run() {
            if (NeuralRowHelper.this.listener != null && !NeuralRowHelper.this.comboHandled()) {
                NeuralNetSuggestion[] suggestions = NeuralNetSuggestor.getInstance().getSuggestion(NeuralRowHelper.this.shiftSuggestions(this.val$prevWords, this.val$composingWord), this.val$locales, true);
                if (suggestions == null || suggestions.length == 0 || !suggestions[0].word.equals(NeuralRowHelper.EOS_SUGGESTION) || ((double) suggestions[0].score) <= NeuralRowHelper.EOS_THRESHOLD) {
                    List<String> prevsWordList = new ArrayList(Arrays.asList(this.val$prevWords));
                    prevsWordList.add(this.val$composingWord);
                    List<String> finalSuggestion = NeuralRowHelper.this.buildContextualEmojis(prevsWordList, this.val$firstSuggestion, this.val$locales);
                    if (!(NeuralRowHelper.this.oldSuggestions.equals(finalSuggestion) && NeuralRowHelper.this.currentState == State.CUSTOM_EMOJI)) {
                        NeuralRowHelper.this.oldSuggestions = finalSuggestion;
                        NeuralRowHelper.this.listener.onNeuralEmojis((String[]) finalSuggestion.toArray(new String[finalSuggestion.size()]));
                    }
                    NeuralRowHelper.this.currentState = State.CUSTOM_EMOJI;
                } else if (NeuralRowHelper.this.currentState != State.DOTS) {
                    NeuralRowHelper.this.currentState = State.DOTS;
                    NeuralRowHelper.this.listener.onNeuralDots();
                }
            }
        }
    }

    /* renamed from: com.android.inputmethod.keyboard.actionrow.NeuralRowHelper.2 */
    class C02342 implements Runnable {
        final /* synthetic */ List val$locales;
        final /* synthetic */ String val$predictedWord;
        final /* synthetic */ String[] val$prevWords;
        final /* synthetic */ float val$scoreFirst;

        C02342(String str, float f, String[] strArr, List list) {
            this.val$predictedWord = str;
            this.val$scoreFirst = f;
            this.val$prevWords = strArr;
            this.val$locales = list;
        }

        public void run() {
            double t0 = (double) System.currentTimeMillis();
            if (NeuralRowHelper.this.listener != null && !NeuralRowHelper.this.comboHandled()) {
                if (this.val$predictedWord.equals(NeuralRowHelper.NUMBER_SUGGESTION)) {
                    if (NeuralRowHelper.this.currentState != State.NUMBERS) {
                        NeuralRowHelper.this.currentState = State.NUMBERS;
                        NeuralRowHelper.this.listener.onNeuralNumbers();
                    }
                } else if (!this.val$predictedWord.equals(NeuralRowHelper.EOS_SUGGESTION) || ((double) this.val$scoreFirst) <= NeuralRowHelper.EOS_THRESHOLD || NeuralRowHelper.this.currentState == State.DOTS) {
                    List<String> finalSuggestion = NeuralRowHelper.this.buildContextualEmojis(new ArrayList(Arrays.asList(this.val$prevWords)), this.val$predictedWord, this.val$locales);
                    if (!(NeuralRowHelper.this.oldSuggestions.equals(finalSuggestion) && NeuralRowHelper.this.currentState == State.CUSTOM_EMOJI)) {
                        NeuralRowHelper.this.oldSuggestions = finalSuggestion;
                        NeuralRowHelper.this.listener.onNeuralEmojis((String[]) finalSuggestion.toArray(new String[finalSuggestion.size()]));
                    }
                    NeuralRowHelper.this.currentState = State.CUSTOM_EMOJI;
                } else {
                    NeuralRowHelper.this.currentState = State.DOTS;
                    NeuralRowHelper.this.listener.onNeuralDots();
                }
            }
        }
    }

    public interface NeuralListener {
        void onNeuralDots();

        void onNeuralEmojis(String[] strArr);

        void onNeuralNumbers();
    }

    public enum State {
        NUMBERS,
        DEFAULT_EMOJI,
        DOTS,
        CUSTOM_EMOJI,
        EMOJI_COMBO,
        NUMBERS_COMBO
    }

    private class WordComparator implements Comparator<String> {
        private final Map<String, Integer> freqMap;

        public WordComparator(Map freqMap) {
            this.freqMap = freqMap;
        }

        public int compare(String lhs, String rhs) {
            if (((Integer) this.freqMap.get(lhs)).intValue() < ((Integer) this.freqMap.get(rhs)).intValue()) {
                return 1;
            }
            if (((Integer) this.freqMap.get(lhs)).intValue() > ((Integer) this.freqMap.get(rhs)).intValue()) {
                return -1;
            }
            return 0;
        }
    }

    static {
        TAG = NeuralRowHelper.class.getSimpleName();
        worker = Executors.newSingleThreadScheduledExecutor();
    }

    private NeuralRowHelper() {
        this.f5p = Pattern.compile(PUNCTUATION_REGEX);
        this.currentState = State.DEFAULT_EMOJI;
        this.oldSuggestions = new ArrayList();
        this.firstSuggestion = LastComposedWord.NOT_A_SEPARATOR;
    }

    public static void init() {
        if (instance == null) {
            instance = new NeuralRowHelper();
        }
    }

    public void setNeuralListener(NeuralListener listener) {
        this.listener = listener;
    }

    public List<String> buildContextualEmojis(List<String> prevsWord, String firstSuggestion, List<Locale> locales) {
        List<String> allWords = new ArrayList();
        for (String s : prevsWord) {
            Log.i(TAG, "PrevInBuild : " + s);
            String pulita = this.f5p.matcher(s).replaceAll(LastComposedWord.NOT_A_SEPARATOR);
            if (!(pulita.equals(EOS_SUGGESTION) || allWords.contains(pulita))) {
                allWords.add(pulita);
            }
        }
        if (allWords.size() == 0 && this.lastEmojiList != null) {
            return this.lastEmojiList;
        }
        int i;
        List<String> finalEmojisList = new ArrayList();
        Map<String, Integer> freqs = new HashMap();
        List<String> words = new ArrayList();
        for (i = 0; i < allWords.size(); i++) {
            if (NeuralNetSuggestor.getInstance().hasEmojiPrediction((String) allWords.get(i), locales)) {
                freqs.put(allWords.get(i), Integer.valueOf(NeuralNetSuggestor.getInstance().getEmojiPrediction((String) allWords.get(i), locales).size()));
                words.add(allWords.get(i));
            }
        }
        String pulita = this.f5p.matcher(firstSuggestion).replaceAll(LastComposedWord.NOT_A_SEPARATOR);
        if (!(pulita.equals(EOS_SUGGESTION) || allWords.contains(pulita))) {
            if (NeuralNetSuggestor.getInstance().hasEmojiPrediction(pulita, locales)) {
                freqs.put(pulita, Integer.valueOf(NeuralNetSuggestor.getInstance().getEmojiPrediction(pulita, locales).size()));
                words.add(pulita);
            }
        }
        Collections.sort(words, new WordComparator(freqs));
        int NUM_OF_DIFF_WORD = Math.min(words.size(), 2);
        for (i = 0; i < NUM_OF_DIFF_WORD; i++) {
            List<String> emojisForWord = NeuralNetSuggestor.getInstance().getEmojiPrediction((String) words.get(i), locales).subList(0, Math.min(3, NeuralNetSuggestor.getInstance().getEmojiPrediction((String) words.get(i), locales).size()));
            for (int j = 0; j < emojisForWord.size(); j++) {
                if (!finalEmojisList.contains(emojisForWord.get(j))) {
                    finalEmojisList.add(emojisForWord.get(j));
                }
            }
        }
        String[] mostUsedEmojis = DEFAULT_SUGGESTED_EMOJI;
        i = 0;
        while (finalEmojisList.size() < 8 && i<mostUsedEmojis.length) {
            if (!finalEmojisList.contains(mostUsedEmojis[i])) {
                finalEmojisList.add(mostUsedEmojis[i]);
            }
            i++;
        }
        this.lastEmojiList = finalEmojisList;
        return finalEmojisList;
    }

    public void pushSuggestionNotPrediction(String[] prevWords, String composingWord, String firstSuggestion, List<Locale> locales) {
        this.firstSuggestion = firstSuggestion;
        Runnable task = new C02331(prevWords, composingWord, locales, firstSuggestion);
        if (this.sentTask != null) {
            this.sentTask.cancel(false);
        }
        this.sentTask = worker.schedule(task, WAITING_TIME, TimeUnit.MILLISECONDS);
    }

    public void pushSuggestionPrediction(String[] prevWords, String predictedWord, float scoreFirst, List<Locale> locales) {
        Runnable task = new C02342(predictedWord, scoreFirst, prevWords, locales);
        if (this.sentTask != null) {
            this.sentTask.cancel(false);
        }
        this.sentTask = worker.schedule(task, WAITING_TIME, TimeUnit.MILLISECONDS);
    }

    private String[] shiftSuggestions(String[] prevWords, String composingWord) {
        String[] shifted = new String[prevWords.length];
        for (int i = 0; i < shifted.length - 1; i++) {
            shifted[i] = prevWords[i + 1].toLowerCase();
        }
        shifted[shifted.length - 1] = composingWord.toLowerCase();
        return shifted;
    }

    private boolean comboHandled() {
        if (this.currentState == State.EMOJI_COMBO) {
            this.currentState = State.CUSTOM_EMOJI;
            return true;
        } else if (this.currentState != State.NUMBERS_COMBO) {
            return false;
        } else {
            this.currentState = State.NUMBERS;
            return true;
        }
    }

    public void setCombo() {
        if (this.currentState == State.CUSTOM_EMOJI) {
            this.currentState = State.EMOJI_COMBO;
        } else if (this.currentState == State.NUMBERS) {
            this.currentState = State.NUMBERS_COMBO;
        }
    }

    public static NeuralRowHelper getInstance() {
        if (instance == null) {
            instance = new NeuralRowHelper();
        }
        return instance;
    }

    public String getFirstSuggestion() {
        return this.firstSuggestion;
    }
}
package com.android.inputmethod.keyboard.actionrow;

/**
 * Created by sepehr on 2/24/17.
 */
public class NeuralNetSuggestion {
    public final float score;
    public final String word;

    public NeuralNetSuggestion(String word, float score) {
        this.word = word;
        this.score = score;
    }
}

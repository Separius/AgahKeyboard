package io.separ.neural.inputmethod.Utils;

import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodSubtype;

import io.separ.neural.inputmethod.indic.DictionaryFacilitator;
import io.separ.neural.inputmethod.indic.RichInputMethodManager;
import io.separ.neural.inputmethod.indic.SuggestedWords;
import io.separ.neural.inputmethod.indic.settings.SettingsValues;

/**
 * Created by sepehr on 3/11/17.
 */

public final class StatsUtils {

    private static String TAG = "Agah_Collection";

    private static StringBuilder currentText = new StringBuilder();

    private StatsUtils() {
        // Intentional empty constructor.
    }

    public static void onCreate(final SettingsValues settingsValues,
                                RichInputMethodManager richImm) {
    }

    public static void onPickSuggestionManually(final SuggestedWords suggestedWords,
                                                final SuggestedWords.SuggestedWordInfo suggestionInfo,
                                                final DictionaryFacilitator dictionaryFacilitator) {
    }

    public static void onBackspaceWordDelete(int wordLength) {
    }

    public static void onBackspacePressed(int lengthToDelete) {
    }

    public static void onBackspaceSelectedText(int selectedTextLength) {
    }

    public static void onDeleteMultiCharInput(int multiCharLength) {
    }

    public static void onRevertAutoCorrect() {
    }

    public static void onRevertSwapPunctuation() {
    }

    public static void onCreateInputView() {
    }

    public static void onStartInputView(int inputType, int displayOrientation, boolean restarting) {
    }

    public static void onAutoCorrection(final String typedWord, final String autoCorrectionWord,
                                        final boolean isBatchInput, final DictionaryFacilitator dictionaryFacilitator,
                                        final String prevWordsContext) {
    }

    private static void addWord(final String commitWord){
        if(TextUtils.isEmpty(commitWord))
            return;
        currentText.append(commitWord);
        currentText.append(' ');
    }

    public static void onWordCommitUserTyped(final String commitWord, final boolean isBatchMode) {
        addWord(commitWord);
    }

    public static void onWordCommitAutoCorrect(final String commitWord, final boolean isBatchMode) {
        addWord(commitWord);
    }

    public static void onWordCommitSuggestionPickedManually(
            final String commitWord, final boolean isBatchMode) {
        addWord(commitWord);
    }

    public static void onSubtypeChanged(final InputMethodSubtype newSubtype) {
    }

    public static void onDestroy() {

    }

    private static int currentPackageHash = 0;

    public static void updatePackageName(String currentPackageName) {
        currentPackageHash = currentPackageName.hashCode();
        if(TextUtils.isEmpty(currentText))
            return;
        Log.i(TAG, "updatePackageName:"+currentText);
        currentText.setLength(0);
    }
}

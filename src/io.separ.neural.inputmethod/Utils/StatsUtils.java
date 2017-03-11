package io.separ.neural.inputmethod.Utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodSubtype;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.separ.neural.inputmethod.indic.DictionaryFacilitator;
import io.separ.neural.inputmethod.indic.SuggestedWords;

/**
 * Created by sepehr on 3/11/17.
 */

public final class StatsUtils {

    private static String TAG = "Agah_Collection";

    public static File collectionFile = null;

    public static OutputStream collectionOutputStream = null;

    private static boolean lineIsNotEmpty = false;

    public static Context latin;

    private StatsUtils() {
        // Intentional empty constructor.
    }

    public static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }

    public static void newFile(){
        Log.i(TAG, "newFile");
        final File collectionDir = new File(latin.getFilesDir(), "collection");
        deleteDirectory(collectionDir);
        collectionDir.mkdirs();
        collectionFile = new File(collectionDir, (new SimpleDateFormat("yyyy-MM-dd::HH:mm:ss")).format(Calendar.getInstance().getTime()));
        try {
            collectionOutputStream = new FileOutputStream(collectionFile);
            lineIsNotEmpty = false;
        } catch (FileNotFoundException e) {
            collectionOutputStream = null;
            e.printStackTrace();
        }
    }

    public static void onCreate(final Context givenLatin) {
        latin = givenLatin;
        newFile();
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
        try {
            collectionOutputStream.write((commitWord+' ').getBytes());
            lineIsNotEmpty = true;
        } catch (IOException e) {
            e.printStackTrace();
            newFile();
        }
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
        final int previousHash = currentPackageHash;
        currentPackageHash = currentPackageName.hashCode();
        if(!lineIsNotEmpty)
            return;
        try {
            collectionOutputStream.write((':'+previousHash+'@'+System.currentTimeMillis()+"\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            newFile();
        }
        lineIsNotEmpty = false;
    }
}

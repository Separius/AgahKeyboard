package io.separ.neural.inputmethod.Utils;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodSubtype;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.separ.neural.inputmethod.indic.DictionaryFacilitator;
import io.separ.neural.inputmethod.indic.SuggestedWords;

import static com.google.firebase.analytics.FirebaseAnalytics.Event.SELECT_CONTENT;
import static com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_ID;

/**
 * Created by sepehr on 3/11/17.
 */

public final class StatsUtils {

    private int revertSwapPuncCount, revertAutoCorrectCount, wordUserTypedCount,
            autoCorrectTypedCount, pickSuggestionCount, backspaceWordDeleteCount,
            backspaceDeleteCount, wordUserTypedBatchCount, autoCorrectTypedBacthCount,
            pickSuggestionBatchCount, subtypeChangeCount, topEmojiSelectedCount,
            richEmojiSelectedCount, snippetToolSelectedCount;

    private static String TAG = "Agah_Collection";

    public File collectionFile = null;

    private OutputStream collectionOutputStream = null;

    private boolean lineIsNotEmpty = false;

    public Context latin;

    private FirebaseAnalytics mFirebaseAnalytics;

    private StatsUtils() {
        // Intentional empty constructor.
    }

    private static StatsUtils instance = null;

    public static StatsUtils getInstance(){
        if(instance == null)
            instance = new StatsUtils();
        return instance;
    }

    private boolean deleteDirectory(File directory) {
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

    public void newFile(){
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

    public void onCreate(final Context givenLatin) {
        latin = givenLatin;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(latin);
        newFile();
    }

    public void onPickSuggestionManually(final SuggestedWords suggestedWords,
                                                final SuggestedWords.SuggestedWordInfo suggestionInfo,
                                                final DictionaryFacilitator dictionaryFacilitator) {
    }

    public void onBackspaceWordDelete(int wordLength) {
        backspaceWordDeleteCount=createCountLog(backspaceWordDeleteCount, 50, "onBackspaceWordDelete");
    }

    public void onBackspacePressed(int lengthToDelete) {
        backspaceDeleteCount=createCountLog(backspaceDeleteCount, 500, "onBackspacePressed");
    }

    public void onBackspaceSelectedText(int selectedTextLength) {
    }

    public void onDeleteMultiCharInput(int multiCharLength) {
    }

    public void onRevertAutoCorrect() {
        revertAutoCorrectCount=createCountLog(revertAutoCorrectCount, 50, "onRevertAutoCorrect");
    }

    public void onTopEmojiSelected(){
        topEmojiSelectedCount=createCountLog(topEmojiSelectedCount, 5, "onTopEmojiSelected");
    }

    public void onRichEmojiSelected(){
        richEmojiSelectedCount=createCountLog(richEmojiSelectedCount, 5, "onRichEmojiSelected");
    }

    public void onSnippetToolSelected(){
        snippetToolSelectedCount=createCountLog(snippetToolSelectedCount, 5, "onSnippetToolSelected");
    }

    private int createCountLog(int updated, int threshold, String id){
        updated++;
        if(updated >= threshold){
            Bundle bundle = new Bundle();
            bundle.putString(ITEM_ID, id);
            mFirebaseAnalytics.logEvent(SELECT_CONTENT, bundle);
            updated = 0;
        }
        return updated;
    }

    public void onRevertSwapPunctuation() {
        revertSwapPuncCount=createCountLog(revertSwapPuncCount, 50, "onRevertSwapPunctuation");
    }

    public void onCreateInputView() {
    }

    public void onStartInputView(int inputType, int displayOrientation, boolean restarting) {
    }

    public void onAutoCorrection(final String typedWord, final String autoCorrectionWord,
                                        final boolean isBatchInput, final DictionaryFacilitator dictionaryFacilitator,
                                        final String prevWordsContext) {
    }

    private void addWord(final String commitWord){
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

    public void onWordCommitUserTyped(final String commitWord, final boolean isBatchMode) {
        addWord(commitWord);
        if(isBatchMode)
            wordUserTypedBatchCount=createCountLog(wordUserTypedBatchCount, 500, "onWordCommitUserBatchTyped");
        else
            wordUserTypedCount=createCountLog(wordUserTypedCount, 500, "onWordCommitUserTyped");
    }

    public void onWordCommitAutoCorrect(final String commitWord, final boolean isBatchMode) {
        addWord(commitWord);
        if(isBatchMode)
            autoCorrectTypedBacthCount=createCountLog(autoCorrectTypedBacthCount, 500, "onWordCommitBatchAutoCorrect");
        else
            autoCorrectTypedCount=createCountLog(autoCorrectTypedCount, 500, "onWordCommitAutoCorrect");
    }

    public void onWordCommitSuggestionPickedManually(
            final String commitWord, final boolean isBatchMode) {
        addWord(commitWord);
        if(isBatchMode)
            pickSuggestionBatchCount=createCountLog(pickSuggestionBatchCount, 500, "onPickSuggestionBatchManually");
        else
            pickSuggestionCount=createCountLog(pickSuggestionCount, 500, "onPickSuggestionManually");
    }

    public void onSubtypeChanged(final InputMethodSubtype newSubtype) {
        subtypeChangeCount=createCountLog(subtypeChangeCount, 500, "onSubtypeChanged");
    }

    public void onDestroy() {
        instance = null;
    }

    private int currentPackageHash = 0;

    public void updatePackageName(String currentPackageName) {
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

    public static boolean hasInstance() {
        return (instance!=null);
    }
}

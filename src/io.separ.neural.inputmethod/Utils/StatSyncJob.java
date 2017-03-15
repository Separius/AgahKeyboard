package io.separ.neural.inputmethod.Utils;

import android.provider.Settings;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        return doSend();
    }

    public static Result doSend(){
        if(StatsUtils.hasInstance() == false)
            return Result.SUCCESS;
        if(!StatsUtils.getInstance().isMetricEnable())
            return Result.SUCCESS;
        final OkHttpClient client = new OkHttpClient();
        List<Protocol> protocolList = new ArrayList<>();
        protocolList.add(Protocol.HTTP_1_1);
        client.setProtocols(protocolList);
        client.setConnectTimeout(5, TimeUnit.SECONDS); // connect timeout
        client.setReadTimeout(5, TimeUnit.SECONDS);    // socket timeout
        RequestBody requestBody = new MultipartBuilder()
                .type(MultipartBuilder.FORM)
                .addFormDataPart("v", "0")
                .addFormDataPart("ime", Settings.Secure.getString(StatsUtils.getInstance().latin.getContentResolver(), Settings.Secure.ANDROID_ID))
                .addFormDataPart("hash", "computed?")
                .addFormDataPart("file", "data.txt", RequestBody.create(MEDIA_TYPE_TXT, StatsUtils.getInstance().collectionFile))
                .build();
        Request request = new Request.Builder()
                .url("https://agahkey.ir/collection")
                .post(requestBody)
                .build();
        //RequestBody requestBody = new MultipartBuilder().type(MultipartBuilder.FORM).addFormDataPart("test", "X").build();
        //Request request = new Request.Builder().url("https://agahkey.ir/collection").method("POST", RequestBody.create(null, new byte[0])).post(requestBody).build();
        try {
            Response response = client.newCall(request).execute();
            response.body().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StatsUtils.getInstance().newFile();
        return Result.SUCCESS;
    }

    public static void scheduleJob(){
        new JobRequest.Builder(StatSyncJob.TAG)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED).setRequirementsEnforced(true)
                //.setPeriodic(TimeUnit.MINUTES.toMillis(15), 300000).build().schedule();
                .setPeriodic(TimeUnit.HOURS.toMillis(12), TimeUnit.HOURS.toMillis(1)).build().schedule();
    }
}

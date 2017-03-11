package io.separ.neural.inputmethod.Utils;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by sepehr on 3/12/17.
 */
public class StatsJobCreator implements JobCreator {
    @Override
    public Job create(String tag){
        switch (tag){
            case StatSyncJob.TAG:
                return new StatSyncJob();
            default:
                return null;
        }
    }
}

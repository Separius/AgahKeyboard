package io.separ.neural.inputmethod.slash;

import android.os.Handler;
import android.util.Log;

import com.android.inputmethod.keyboard.top.services.ServiceResultsView;

/**
 * Created by sepehr on 3/2/17.
 */

public class ServiceRequestManager {
    private static final int EXECUTION_DELAY = 500;
    private static ServiceRequestManager instance;
    private Handler handler;
    private TaskRunnable request;

    public abstract class TaskRunnable implements Runnable {

        /* renamed from: co.touchlab.inputmethod.latin.monkey.ServiceRequestManager.TaskRunnable.1 */
        class C04111 implements BaseTaskQueue.QueueQuery {
            C04111() {
            }

            public void query(BaseTaskQueue queue, Task task) {
                if (task instanceof BaseQuerySearchTask) {
                    ((BaseQuerySearchTask) task).cancel();
                    queue.remove(task);
                }
            }
        }

        public abstract BaseQuerySearchTask getTask();

        public void run() {
            NeuralApplication.getNetworkTaskQueue().execute(getTask());
        }

        public void cancel() {
            NeuralApplication.getNetworkTaskQueue().query(new C04111());
        }
    }

    /* renamed from: co.touchlab.inputmethod.latin.monkey.ServiceRequestManager.1 */
    class C04101 extends TaskRunnable {
        final /* synthetic */ String val$action;
        final /* synthetic */ boolean val$isLocationAware;
        final /* synthetic */ String val$query;
        final /* synthetic */ String val$slash;
        final /* synthetic */ boolean val$useCache;

        C04101(String str, String str2, String str3, boolean z, boolean z2) {
            super();
            this.val$slash = str;
            this.val$query = str2;
            this.val$action = str3;
            this.val$isLocationAware = z;
            this.val$useCache = z2;
        }

        public BaseQuerySearchTask getTask() {
            return new ServiceQuerySearchTask(this.val$slash, this.val$query, this.val$action, this.val$isLocationAware, this.val$useCache);
        }
    }

    static {
        instance = new ServiceRequestManager();
    }

    public ServiceRequestManager() {
        this.handler = new Handler();
    }

    public static ServiceRequestManager getInstance() {
        return instance;
    }

    public synchronized void postRequest(String slash, String query) {
        postRequest(slash, query, false);
    }

    public synchronized void postRequest(String slash, String query, boolean useCache) {
        postRequest(slash, query, "prepopulate", useCache);
    }

    public synchronized void postRequest(String slash, String query, String action, boolean useCache) {
        Log.e("SEPAR", "postRequest: "+slash+" "+query+" "+action+" "+useCache);
        boolean isLocationAware = false;
        synchronized (this) {
            cancelLastRequest();
            boolean shouldShowLoading = true;
            boolean shouldDoRequest = true;
            //TODO uncomment, works
            /*if ("contacts".equals(slash)) {
                TaskQueue.loadQueueDefault(NeuralApplication.getInstance()).execute(new ServiceQueryContactsTask(query, ServiceQueryContactsTask.SearchType.Phone));
                shouldDoRequest = false;
                shouldShowLoading = false;
            }*/
            /*if (shouldDoRequest) {
                RServiceItem service = null;//TODO
                if (service != null) {
                    isLocationAware = service.isLocation_aware();
                }
                this.request = new C04101(slash, query, action, isLocationAware, useCache);
            }*/
            this.handler.postDelayed(this.request, 0);
            if (shouldShowLoading) {
                EventBusExt.getDefault().post(new ServiceRequestEvent(ServiceResultsView.VisualSate.Loading.setMessage(slash), slash));
            }
        }
    }

    public synchronized void repostLastRequest() {
        if (this.request != null) {
            EventBusExt.getDefault().post(new ServiceRequestEvent(ServiceResultsView.VisualSate.Loading.setMessage(this.request.getTask().getService()), this.request.getTask().getService()));
            this.handler.postDelayed(this.request, 500);
        }
    }

    public synchronized void cancelLastRequest() {
        if (this.request != null) {
            this.request.cancel();
            this.handler.removeCallbacks(this.request);
            this.request = null;
        }
    }
}

package io.separ.neural.inputmethod.slash;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.SubscriberExceptionEvent;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by sepehr on 3/2/17.
 */
public class EventBusExt {
    private static EventBusExt instance;
    private EventBus eventBus;

    public static class ErrorListener {

        /* renamed from: co.touchlab.android.threading.eventbus.EventBusExt.ErrorListener.1 */
        static class C03411 extends Thread {
            final /* synthetic */ Throwable val$throwable;

            C03411(Throwable th) {
                this.val$throwable = th;
            }

            public void run() {
                if (this.val$throwable instanceof RuntimeException) {
                    throw ((RuntimeException) this.val$throwable);
                } else if (this.val$throwable instanceof Error) {
                    throw ((Error) this.val$throwable);
                } else {
                    throw new RuntimeException(this.val$throwable);
                }
            }
        }

        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEvent(SubscriberExceptionEvent exceptionEvent) {
            new C03411(exceptionEvent.throwable).start();
        }
    }

    static {
        instance = new EventBusExt();
    }

    public EventBusExt() {
        this.eventBus = new EventBus();
        this.eventBus.register(new ErrorListener());
    }

    public static EventBus getDefault() {
        return instance.eventBus;
    }
}

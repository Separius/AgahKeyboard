package io.separ.neural.inputmethod.slash;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by sepehr on 3/2/17.
 */

public abstract class BaseTaskQueue {
    protected final Application application;
    private Task currentTask;
    protected final Handler executeHandler;
    protected final Handler handler;
    private List<QueueListener> listeners;
    private boolean startedCalled;
    protected final QueueWrapper<Task> tasks;

    protected class QueueHandler extends Handler {
        static final int INSERT_TASK = 0;
        static final int POLL_TASK = 1;
        public static final int POST_EXE = 2;
        static final int THROW = 3;

        private QueueHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INSERT_TASK /*0*/:
                    BaseTaskQueue.this.insertTask((Task) msg.obj);
                    break;
                case POLL_TASK /*1*/:
                    if (BaseTaskQueue.this.currentTask == null) {
                        Task task = (Task) BaseTaskQueue.this.tasks.poll();
                        if (task != null) {
                            BaseTaskQueue.this.currentTask = task;
                            if (!BaseTaskQueue.this.startedCalled) {
                                BaseTaskQueue.this.startedCalled = true;
                                for (QueueListener listener : BaseTaskQueue.this.listeners) {
                                    listener.queueStarted(BaseTaskQueue.this);
                                }
                            }
                            for (QueueListener listener2 : BaseTaskQueue.this.listeners) {
                                listener2.taskStarted(BaseTaskQueue.this, task);
                            }
                            BaseTaskQueue.this.runTask(task);
                            return;
                        }
                        BaseTaskQueue.this.callQueueFinished();
                    }
                    break;
                case POST_EXE /*2*/:
                    Task tempTask = BaseTaskQueue.this.currentTask;
                    BaseTaskQueue.this.currentTask = null;
                    BaseTaskQueue.this.finishTask(msg, tempTask);
                    for (QueueListener listener22 : BaseTaskQueue.this.listeners) {
                        listener22.taskFinished(BaseTaskQueue.this, tempTask);
                    }
                    break;
                case THROW /*3*/:
                    Throwable cause = (Throwable) msg.obj;
                    if (cause instanceof RuntimeException) {
                        throw ((RuntimeException) cause);
                    } else if (cause instanceof Error) {
                        throw ((Error) cause);
                    } else {
                        cause.printStackTrace();
                        throw new RuntimeException(cause);
                    }
                default:
                    BaseTaskQueue.this.otherOperations(msg);
            }
        }
    }

    public interface QueueListener {
        void queueFinished(BaseTaskQueue baseTaskQueue);

        void queueStarted(BaseTaskQueue baseTaskQueue);

        void taskFinished(BaseTaskQueue baseTaskQueue, Task task);

        void taskStarted(BaseTaskQueue baseTaskQueue, Task task);
    }

    public interface QueueQuery {
        void query(BaseTaskQueue baseTaskQueue, Task task);
    }

    protected interface QueueWrapper<T> {
        Collection<T> all();

        void offer(T t);

        T poll();

        void remove(T t);
    }

    public static class TaskQueueState {
        Task currentTask;
        List<Task> queued;

        public TaskQueueState(List<Task> queued, Task currentTask) {
            this.queued = queued;
            this.currentTask = currentTask;
        }

        public List<Task> getQueued() {
            return this.queued;
        }

        public Task getCurrentTask() {
            return this.currentTask;
        }
    }

    protected abstract void finishTask(Message message, Task task);

    protected abstract void runTask(Task task);

    public BaseTaskQueue(Application application, QueueWrapper<Task> queueWrapper) {
        this.listeners = new ArrayList();
        this.startedCalled = false;
        this.application = application;
        this.tasks = queueWrapper;
        this.handler = new QueueHandler(Looper.getMainLooper());
        HandlerThread ht = new HandlerThread("Background");
        ht.start();
        this.executeHandler = new Handler(ht.getLooper());
    }

    public int countTasks() {
        return (this.currentTask == null ? 0 : 1) + this.tasks.all().size();
    }

    public void addListener(QueueListener listener) {
        this.listeners.add(listener);
    }

    public void clearListeners() {
        this.listeners.clear();
    }

    protected void insertTask(Task task) {
        UiThreadContext.assertUiThread();
        this.tasks.offer(task);
        resetPollRunnable();
    }

    public void remove(Task task) {
        this.tasks.remove(task);
    }

    protected void resetPollRunnable() {
        this.handler.removeMessages(1);
        this.handler.sendMessage(this.handler.obtainMessage(1));
    }

    protected void callQueueFinished() {
        for (QueueListener listener : this.listeners) {
            listener.queueFinished(this);
        }
        this.startedCalled = false;
    }

    public TaskQueueState copyState() {
        UiThreadContext.assertUiThread();
        PriorityQueue<Task> commands = new PriorityQueue(this.tasks.all());
        List<Task> commandList = new ArrayList();
        while (!commands.isEmpty()) {
            commandList.add(commands.poll());
        }
        return new TaskQueueState(commandList, this.currentTask);
    }

    protected void otherOperations(Message msg) {
    }

    public void query(QueueQuery queueQuery) {
        UiThreadContext.assertUiThread();
        for (Task task : this.tasks.all()) {
            queueQuery.query(this, task);
        }
        if (this.currentTask != null) {
            queueQuery.query(this, this.currentTask);
        }
    }
}

package io.separ.neural.inputmethod.slash;

import android.app.Application;
import android.content.Context;
import android.os.Message;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by sepehr on 3/2/17.
 */
public class TaskQueue extends BaseTaskQueue {
    private static final String DEFAULT_QUEUE = "__DEFAULT";
    private static final String NETWORK_QUEUE = "__NETWORK";
    private static Map<String, TaskQueue> queueMap;
    private final boolean fifo;

    private class ExeTask implements Runnable {
        private Task task;

        private ExeTask(Task task) {
            this.task = task;
        }

        public void run() {
            UiThreadContext.assertBackgroundThread();
            try {
                this.task.run(TaskQueue.this.application);
                TaskQueue.this.handler.sendMessage(TaskQueue.this.handler.obtainMessage(2));
            } catch (Throwable th) {
                TaskQueue.this.handler.sendMessage(TaskQueue.this.handler.obtainMessage(2));
            }
        }
    }

    static class LinkedListQueue<Task> implements QueueWrapper<Task> {
        private final boolean fifo;
        private final LinkedList<Task> linkedList;

        public LinkedListQueue(boolean fifo) {
            this.linkedList = new LinkedList();
            this.fifo = fifo;
        }

        public Task poll() {
            return this.fifo ? this.linkedList.poll() : this.linkedList.pollLast();
        }

        public void offer(Task task) {
            this.linkedList.offer(task);
        }

        public Collection<Task> all() {
            return this.linkedList;
        }

        public void remove(Task task) {
            this.linkedList.remove(task);
        }
    }

    static {
        queueMap = new HashMap();
    }

    public static synchronized TaskQueue loadQueue(Context context, String name) {
        TaskQueue loadQueue;
        synchronized (TaskQueue.class) {
            loadQueue = loadQueue(context, name, true);
        }
        return loadQueue;
    }

    public static synchronized TaskQueue loadQueue(Context context, String name, boolean fifo) {
        TaskQueue taskQueueActual;
        synchronized (TaskQueue.class) {
            taskQueueActual = queueMap.get(name);
            if (taskQueueActual == null) {
                taskQueueActual = new TaskQueue((Application) context.getApplicationContext(), fifo);
                queueMap.put(name, taskQueueActual);
            } else if (taskQueueActual.fifo != fifo) {
                throw new IllegalStateException("Queue already created with different fifo setting: " + name + "/" + fifo);
            }
        }
        return taskQueueActual;
    }

    public static TaskQueue loadQueueDefault(Context context) {
        return loadQueue(context, DEFAULT_QUEUE);
    }

    public static TaskQueue loadQueueNetwork(Context context) {
        return loadQueue(context, NETWORK_QUEUE);
    }

    public TaskQueue(Application application) {
        this(application, true);
    }

    public TaskQueue(Application application, boolean fifo) {
        super(application, new LinkedListQueue(fifo));
        this.fifo = fifo;
    }

    protected void runTask(Task task) {
        this.executeHandler.post(new ExeTask(task));
    }

    protected void finishTask(Message msg, Task task) {
        if (task != null) {
            try {
                task.onComplete(this.application);
            } catch (Throwable th) {
                resetPollRunnable();
            }
        }
        resetPollRunnable();
    }

    public void execute(Task task) {
        task.setMyQueue(this);
        if (UiThreadContext.isInUiThread()) {
            insertTask(task);
            return;
        }
        this.handler.sendMessage(this.handler.obtainMessage(0, task));
    }
}

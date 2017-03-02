package io.separ.neural.inputmethod.slash;

/**
 * Created by sepehr on 3/2/17.
 */

public class TaskQueueHelper {

    static class ClassesQuery implements BaseTaskQueue.QueueQuery {
        Class[] classes;
        boolean found;
        StickyTaskManager stickyTaskManager;

        ClassesQuery(StickyTaskManager stickyTaskManager, Class[] classes) {
            this.found = false;
            this.stickyTaskManager = stickyTaskManager;
            this.classes = classes;
        }

        public void query(BaseTaskQueue queue, Task task) {
            for (Class aClass : this.classes) {
                if (task.getClass().equals(aClass)) {
                    if (this.stickyTaskManager == null || !(task instanceof StickyTask)) {
                        this.found = true;
                        return;
                    }
                    if (this.stickyTaskManager.isTaskForMe((StickyTask) task)) {
                        this.found = true;
                        return;
                    }
                }
            }
        }
    }

    public static boolean hasTasksOfType(BaseTaskQueue taskQueueActual, Class... classes) {
        return hasTasksOfType(null, taskQueueActual, classes);
    }

    public static boolean hasTasksOfType(StickyTaskManager stickyTaskManager, BaseTaskQueue taskQueueActual, Class... classes) {
        ClassesQuery queueQuery = new ClassesQuery(stickyTaskManager, classes);
        taskQueueActual.query(queueQuery);
        return queueQuery.found;
    }
}
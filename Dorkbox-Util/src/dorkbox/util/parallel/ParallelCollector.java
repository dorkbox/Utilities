package dorkbox.util.parallel;

/**
 *
 */
public
interface ParallelCollector<T extends ParallelTask> {
    /**
     * Called each time a single piece of work (a task) is completed.
     */
    void taskComplete(T work);

    /**
     * Called when everything is finished.
     */
    void workComplete();

}

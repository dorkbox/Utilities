package dorkbox.util.parallel;

public abstract
class ParallelTask {
    public int index;

    public
    void setId(int index) {
        this.index = index;
    }

    public
    int getId() {
        return index;
    }

    public abstract
    void doWork();
}

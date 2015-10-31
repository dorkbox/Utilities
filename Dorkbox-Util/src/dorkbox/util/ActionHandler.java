package dorkbox.util;

public
interface ActionHandler<T> {
    void handle(T owner);
}

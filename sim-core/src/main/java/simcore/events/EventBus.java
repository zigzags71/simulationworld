package simcore.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventBus<T> {
    private final List<Consumer<T>> subscribers = new CopyOnWriteArrayList<>();

    public void publish(T event) {
        for (Consumer<T> subscriber : subscribers) {
            subscriber.accept(event);
        }
    }

    public void subscribe(Consumer<T> consumer) {
        subscribers.add(consumer);
    }
}

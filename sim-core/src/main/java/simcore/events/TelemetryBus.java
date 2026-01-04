package simcore.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class TelemetryBus {
    private final List<Consumer<TelemetryEvent>> subscribers = new CopyOnWriteArrayList<>();

    public void publish(TelemetryEvent event) {
        for (Consumer<TelemetryEvent> subscriber : subscribers) {
            subscriber.accept(event);
        }
    }

    public void subscribe(Consumer<TelemetryEvent> consumer) {
        subscribers.add(consumer);
    }
}

package me.scai.plato.android.events;

public class GeneratedImageViewEvent {
    public enum EventType {
        Open,
        Close
    }

    /* Member variables */
    private EventType eventType;

    /* Constructors */
    public GeneratedImageViewEvent(EventType eventType) {
        this.eventType = eventType;
    }

    /* Getters */
    public EventType getEventType() {
        return eventType;
    }
}

package com.orion.echoes.lua.events;

public class GameEvent {

    private final EventType type;
    private final Object data;

    public GameEvent(EventType type) {
        this(type, null);
    }

    public GameEvent(
        EventType type,
        Object data
    ) {

        this.type = type;
        this.data = data;
    }

    public EventType getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public <T> T getDataAs(
        Class<T> clazz
    ) {

        if (
            data != null
                && clazz.isInstance(data)
        ) {

            return clazz.cast(data);
        }

        return null;
    }
}

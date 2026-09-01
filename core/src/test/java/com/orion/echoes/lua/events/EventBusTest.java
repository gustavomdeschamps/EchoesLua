package com.orion.echoes.lua.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Entrega, isolamento por tipo e seguranca contra remocao durante o despacho. */
class EventBusTest {

    private EventBus bus;

    @BeforeEach
    void setUp() {
        bus = EventBus.getInstance();
        bus.clear();
    }

    @Test
    @DisplayName("Assinante recebe apenas o proprio tipo de evento")
    void listenerOnlyReceivesItsType() {
        List<GameEvent> received = new ArrayList<>();
        bus.subscribe(EventType.WEAPON_CRAFTED, received::add);

        bus.publish(EventType.WEAPON_CRAFTED);
        bus.publish(EventType.ENEMY_DEFEATED, 1);

        assertEquals(1, received.size());
        assertEquals(EventType.WEAPON_CRAFTED, received.get(0).getType());
    }

    @Test
    @DisplayName("O dado publicado chega intacto ao assinante")
    void payloadReachesListener() {
        List<Object> payloads = new ArrayList<>();
        bus.subscribe(EventType.ENEMY_DEFEATED, event -> payloads.add(event.getData()));
        bus.publish(EventType.ENEMY_DEFEATED, 7);
        assertEquals(1, payloads.size());
        assertEquals(7, payloads.get(0));
    }

    @Test
    @DisplayName("O mesmo assinante nao e registrado duas vezes")
    void duplicateSubscriptionIsIgnored() {
        int[] calls = {0};
        EventBus.EventListener listener = event -> calls[0]++;
        bus.subscribe(EventType.WEAPON_CRAFTED, listener);
        bus.subscribe(EventType.WEAPON_CRAFTED, listener);
        bus.publish(EventType.WEAPON_CRAFTED);
        assertEquals(1, calls[0]);
    }

    @Test
    @DisplayName("unsubscribe interrompe a entrega")
    void unsubscribeStopsDelivery() {
        int[] calls = {0};
        EventBus.EventListener listener = event -> calls[0]++;
        bus.subscribe(EventType.WEAPON_CRAFTED, listener);
        bus.publish(EventType.WEAPON_CRAFTED);
        bus.unsubscribe(EventType.WEAPON_CRAFTED, listener);
        bus.publish(EventType.WEAPON_CRAFTED);
        assertEquals(1, calls[0]);
    }

    @Test
    @DisplayName("Remover assinante durante o despacho nao quebra a entrega")
    void removalDuringDispatchIsSafe() {
        int[] calls = {0};
        List<EventBus.EventListener> listeners = new ArrayList<>();
        EventBus.EventListener first = event -> {
            calls[0]++;
            bus.unsubscribe(EventType.WEAPON_CRAFTED, listeners.get(1));
        };
        EventBus.EventListener second = event -> calls[0]++;
        listeners.add(first);
        listeners.add(second);
        bus.subscribe(EventType.WEAPON_CRAFTED, first);
        bus.subscribe(EventType.WEAPON_CRAFTED, second);

        bus.publish(EventType.WEAPON_CRAFTED);
        assertEquals(2, calls[0], "a copia protege o despacho em andamento");

        bus.publish(EventType.WEAPON_CRAFTED);
        assertEquals(3, calls[0], "o segundo assinante saiu de verdade");
    }

    @Test
    @DisplayName("Publicar sem assinantes nao lanca excecao")
    void publishWithoutListeners() {
        bus.publish(EventType.MARS_ENTERED);
    }

    @Test
    @DisplayName("EventBus e um singleton")
    void busIsSingleton() {
        assertSame(bus, EventBus.getInstance());
    }
}

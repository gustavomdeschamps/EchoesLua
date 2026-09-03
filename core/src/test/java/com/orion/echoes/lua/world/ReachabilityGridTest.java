package com.orion.echoes.lua.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Garantia de que um layout gerado por semente continua completavel. */
class ReachabilityGridTest {

    @Test
    @DisplayName("Sem obstaculo, o mapa inteiro e alcancavel")
    void openMapIsFullyReachable() {
        ReachabilityGrid grid = new ReachabilityGrid(600f, 400f, 50f);
        grid.floodFrom(25f, 25f);
        assertEquals(grid.getColumns() * grid.getRows(), grid.getReachableCount());
        assertTrue(grid.isReachable(575f, 375f));
    }

    @Test
    @DisplayName("Parede completa separa os dois lados do mapa")
    void fullWallSplitsTheMap() {
        ReachabilityGrid grid = new ReachabilityGrid(600f, 400f, 50f);
        grid.block(new Rectangle(250f, 0f, 60f, 400f));
        grid.floodFrom(25f, 25f);
        assertTrue(grid.isReachable(100f, 200f), "lado da origem");
        assertFalse(grid.isReachable(500f, 200f), "lado oposto da parede");
        assertTrue(grid.isBlocked(280f, 200f));
    }

    @Test
    @DisplayName("Uma passagem na parede reconecta o mapa")
    void gapReconnectsTheMap() {
        ReachabilityGrid grid = new ReachabilityGrid(600f, 400f, 50f);
        grid.block(new Rectangle(250f, 0f, 60f, 150f));
        grid.block(new Rectangle(250f, 250f, 60f, 150f));
        grid.floodFrom(25f, 25f);
        assertTrue(grid.isReachable(500f, 200f), "a fresta no meio deixa passar");
    }

    @Test
    @DisplayName("Bolsao fechado por rochas fica inalcancavel")
    void enclosedPocketIsUnreachable() {
        ReachabilityGrid grid = new ReachabilityGrid(600f, 600f, 50f);
        grid.block(new Rectangle(200f, 200f, 200f, 50f));
        grid.block(new Rectangle(200f, 350f, 200f, 50f));
        grid.block(new Rectangle(200f, 200f, 50f, 200f));
        grid.block(new Rectangle(350f, 200f, 50f, 200f));
        grid.floodFrom(25f, 25f);
        assertFalse(grid.isReachable(300f, 300f), "o centro do anel esta fechado");
        assertTrue(grid.isReachable(100f, 100f));
    }

    @Test
    @DisplayName("Origem dentro de uma rocha salta para a celula livre mais proxima")
    void originInsideObstacleFallsBackToFreeCell() {
        ReachabilityGrid grid = new ReachabilityGrid(600f, 400f, 50f);
        grid.block(new Rectangle(0f, 0f, 100f, 100f));
        grid.floodFrom(25f, 25f);
        assertTrue(grid.getReachableCount() > 0, "o flood nao pode morrer na origem bloqueada");
        assertTrue(grid.isReachable(400f, 300f));
    }

    @Test
    @DisplayName("Pontos fora do mundo sao presos a borda em vez de estourar")
    void outOfBoundsIsClamped() {
        ReachabilityGrid grid = new ReachabilityGrid(600f, 400f, 50f);
        grid.floodFrom(25f, 25f);
        assertTrue(grid.isReachable(-500f, -500f));
        assertTrue(grid.isReachable(99999f, 99999f));
    }

    @Test
    @DisplayName("Celula de tamanho invalido e rejeitada na construcao")
    void invalidCellSizeIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> new ReachabilityGrid(600f, 400f, 0f));
    }
}

package com.orion.echoes.lua.systems;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventarioReloadTest {
    @Test void iceCellsStayInReserveUntilReload() {
        Inventario inventory = new Inventario();
        assertEquals(6, inventory.addReserveAmmo(6));
        assertEquals(6, inventory.getReserveAmmo());
        assertEquals(30, inventory.reload(24, 30));
        assertEquals(0, inventory.getReserveAmmo());
        assertEquals(30, inventory.reload(30, 30));
    }

    @Test void slotSwapIsStableAcrossSaveRestore() {
        Inventario inventory = new Inventario();
        inventory.swapSlots(0, 25);
        inventory.addReserveAmmo(9);
        Inventario loaded = new Inventario();
        loaded.restoreSlots(inventory.exportSlots());
        loaded.restoreReserveAmmo(inventory.getReserveAmmo());
        assertEquals(1, loaded.itemAt(25));
        assertEquals(0, loaded.itemAt(0));
        assertEquals(9, loaded.getReserveAmmo());
    }

    @Test void difficultyChangesReceivedDamageWithoutChangingArmor() {
        Inventario inventory = new Inventario();
        assertEquals(1f, inventory.getMultiplicadorDanoRecebido());
        inventory.setDifficulty(Inventario.Difficulty.FACIL);
        assertEquals(.72f, inventory.getMultiplicadorDanoRecebido());
        inventory.setDifficulty(Inventario.Difficulty.DIFICIL);
        assertEquals(1.35f, inventory.getMultiplicadorDanoRecebido());
        assertTrue(inventory.melhorarArmadura());
        assertEquals(1.35f * .85f, inventory.getMultiplicadorDanoRecebido(), .0001f);
    }
}

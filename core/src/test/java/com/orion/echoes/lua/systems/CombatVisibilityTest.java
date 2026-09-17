package com.orion.echoes.lua.systems;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CombatVisibilityTest {
    @Test void stationBlocksAttacksAcrossItsPhysicalFootprint() {
        var solids=new Array<Rectangle>();solids.add(new Rectangle(45,0,10,40));
        assertTrue(CombatVisibility.blocked(0,20,100,20,solids));
        assertFalse(CombatVisibility.blocked(0,50,100,50,solids));
    }
    @Test void coverDoesNotBlockAnAttackOnTheSameSide() {
        var solids=new Array<Rectangle>();solids.add(new Rectangle(45,0,10,40));
        assertFalse(CombatVisibility.blocked(0,20,30,20,solids));
    }
}

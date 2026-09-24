package com.orion.echoes.lua.systems;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.orion.echoes.lua.entities.ExpeditionBoss;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatVisibilityRayTest {
    @Test void shotHitsBossBeforeCover() {
        Rectangle boss = new Rectangle(120f, 40f, 30f, 40f);
        Array<Rectangle> cover = new Array<>();
        cover.add(new Rectangle(180f, 20f, 20f, 80f));
        assertEquals(120f, CombatVisibility.firstHit(0f, 60f, 1f, 0f, 300f, boss));
        assertTrue(CombatVisibility.firstHit(0f, 60f, 1f, 0f, 300f, boss)
            < CombatVisibility.firstSolidHit(0f, 60f, 1f, 0f, 300f, cover));
    }

    @Test void coverStopsShotBeforeBoss() {
        Rectangle boss = new Rectangle(120f, 40f, 30f, 40f);
        Array<Rectangle> cover = new Array<>();
        cover.add(new Rectangle(70f, 20f, 20f, 80f));
        assertEquals(70f, CombatVisibility.firstSolidHit(0f, 60f, 1f, 0f, 300f, cover));
        assertTrue(CombatVisibility.firstSolidHit(0f, 60f, 1f, 0f, 300f, cover)
            < CombatVisibility.firstHit(0f, 60f, 1f, 0f, 300f, boss));
    }

    @Test void parallelAndBehindRaysNeverHit() {
        Rectangle box = new Rectangle(100f, 40f, 20f, 20f);
        assertEquals(Float.POSITIVE_INFINITY,
            CombatVisibility.firstHit(0f, 0f, 1f, 0f, 300f, box));
        assertEquals(Float.POSITIVE_INFINITY,
            CombatVisibility.firstHit(0f, 50f, -1f, 0f, 300f, box));
    }

    @Test void visibleBossTorsoIsShootableWhileFeetColliderStaysSmall() {
        ExpeditionBoss boss = new ExpeditionBoss(120f);
        assertTrue(boss.shotBounds().height > boss.bounds.height);
        float headY = boss.bounds.y + 150f;
        assertEquals(Float.POSITIVE_INFINITY,
            CombatVisibility.firstHit(500f, headY, 1f, 0f, 600f, boss.bounds));
        assertTrue(CombatVisibility.firstHit(500f, headY, 1f, 0f, 600f,
            boss.shotBounds()) < Float.POSITIVE_INFINITY);
    }
}

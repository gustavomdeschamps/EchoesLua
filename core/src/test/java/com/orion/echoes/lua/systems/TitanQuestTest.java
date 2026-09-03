package com.orion.echoes.lua.systems;

import static org.junit.jupiter.api.Assertions.*;
import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class TitanQuestTest {
    @Test void portalRequiresDialogueAndOneProof() {
        CampaignState campaign = new CampaignState(7L);
        assertFalse(campaign.portalLiberado());
        campaign.setCombateOk(true);
        assertFalse(campaign.portalLiberado());
        campaign.setDialogoTita(true);
        assertTrue(campaign.portalLiberado());
        assertEquals("PORTAL TITA ONLINE", campaign.statusPortal());
    }

    @Test void shotChecksWeaponAmmoCooldownAndRangeTogether() {
        CampaignState campaign = new CampaignState(8L);
        TitanCombatSystem combat = new TitanCombatSystem(campaign);
        FakeTarget target = new FakeTarget(100f, 0f);
        combat.setMunicao(2);
        assertFalse(combat.tentarTiro(new Vector2(), target, false));
        assertEquals(2, combat.getMunicao());
        assertTrue(combat.tentarTiro(new Vector2(), target, true));
        assertEquals(1, combat.getMunicao());
        assertFalse(combat.tentarTiro(new Vector2(), target, true));
        combat.update(combat.getCooldownMax());
        assertTrue(combat.tentarTiro(new Vector2(), target, true));
        assertTrue(campaign.isCombateOk());
    }

    private static final class FakeTarget implements CombatTarget {
        private final float x, y;
        private float hp = 80f;
        FakeTarget(float x, float y) { this.x = x; this.y = y; }
        public float centerX() { return x; }
        public float centerY() { return y; }
        public boolean isAlive() { return hp > 0f; }
        public boolean receiveDamage(float damage) { hp -= damage; return hp <= 0f; }
    }
}

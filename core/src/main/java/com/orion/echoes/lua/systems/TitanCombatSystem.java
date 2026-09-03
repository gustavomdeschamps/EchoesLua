package com.orion.echoes.lua.systems;

import com.badlogic.gdx.math.Vector2;

/**
 * Contrato avaliável do disparo: arma, munição, cooldown e alcance são
 * conferidos no mesmo método antes que qualquer dano seja aplicado.
 */
public final class TitanCombatSystem {
    private float alcance = 420f;
    private float cooldown;
    private float cooldownMax = .24f;
    private int municao;
    private float dano = 40f;
    private final CampaignState campaign;

    public TitanCombatSystem(CampaignState campaign) {
        this.campaign = campaign;
    }

    public void update(float delta) {
        cooldown = Math.max(0f, cooldown - Math.max(0f, delta));
    }

    public boolean tentarTiro(Vector2 origem, CombatTarget alvo, boolean temArma) {
        if (!temArma || origem == null) return false;
        if (municao <= 0 || cooldown > 0f) return false;
        municao--;
        cooldown = cooldownMax;
        if (alvo != null && alvo.isAlive()) {
            float dx = alvo.centerX() - origem.x;
            float dy = alvo.centerY() - origem.y;
            if (dx * dx + dy * dy <= alcance * alcance) {
                boolean morreu = alvo.receiveDamage(dano);
                if (morreu && campaign != null) campaign.setCombateOk(true);
            }
        }
        return true;
    }

    public int getMunicao() { return municao; }
    public void setMunicao(int value) { municao = Math.max(0, value); }
    public float getAlcance() { return alcance; }
    public float getCooldown() { return cooldown; }
    public float getCooldownMax() { return cooldownMax; }
    public float getDano() { return dano; }
}

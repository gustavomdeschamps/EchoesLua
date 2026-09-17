package com.orion.echoes.lua.entities;

import com.badlogic.gdx.math.Rectangle;
import com.orion.echoes.lua.systems.CombatTarget;

/** Combat progression independent of textures and screen lifetime. */
public class ExpeditionBoss implements CombatTarget {
    protected float hp, hpMax;
    protected int forma = 1;
    protected boolean mortoFinal;
    public final Rectangle bounds = new Rectangle(860f, 350f, 110f, 65f);
    public ExpeditionBoss(float health) { hp = hpMax = health; }
    @Override public float centerX() { return bounds.x + bounds.width / 2f; }
    @Override public float centerY() { return bounds.y + bounds.height / 2f; }
    @Override public boolean isAlive() { return !mortoFinal; }
    @Override public boolean receiveDamage(float damage) {
        if (mortoFinal || damage <= 0f) return false;
        hp = Math.max(0f, hp - damage);
        if (hp == 0f) mortoFinal = true;
        return mortoFinal;
    }
    public float getHp() { return hp; }
    public float getHpMax() { return hpMax; }
    public int getForma() { return forma; }
    public float getSpeed() { return 70f; }
}

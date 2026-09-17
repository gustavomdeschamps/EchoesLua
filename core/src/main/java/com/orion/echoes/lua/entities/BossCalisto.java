package com.orion.echoes.lua.entities;

/** One creature, three sequential lives; the light key is only earned at the final death. */
public final class BossCalisto extends ExpeditionBoss {
    private float mutationTimer;
    public BossCalisto() { super(100f); }
    public BossCalisto(int savedForm, float savedHp) {
        this(); forma = Math.max(1, Math.min(3, savedForm));
        hpMax = healthFor(forma);
        hp = Math.max(0f, Math.min(hpMax, savedHp));
        mortoFinal = forma == 3 && hp == 0f;
        if (hp == 0f && !mortoFinal) hp = hpMax;
    }
    public static float healthFor(int form) { return form == 1 ? 100f : form == 2 ? 150f : 220f; }
    @Override public boolean receiveDamage(float damage) {
        if (mortoFinal || mutationTimer > 0f || damage <= 0f) return false;
        hp = Math.max(0f, hp - damage);
        if (hp == 0f && forma < 3) {
            forma++; hpMax = hp = healthFor(forma); mutationTimer = 1.25f;
        } else if (hp == 0f) mortoFinal = true;
        return mortoFinal;
    }
    public void update(float delta) { mutationTimer = Math.max(0f, mutationTimer - Math.max(0f, delta)); }
    public boolean isMutating() { return mutationTimer > 0f; }
    @Override public float getSpeed() { return forma == 1 ? 90f : forma == 2 ? 110f : 130f; }
}

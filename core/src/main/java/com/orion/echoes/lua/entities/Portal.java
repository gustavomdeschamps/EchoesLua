package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.managers.AssetManager;

/** Portal da campanha com moldura e vórtice próprios, sem quadros antigos do atlas. */
public class Portal extends Entidade {
    private static final float TRAVEL_DURATION = .72f;
    private final AssetManager assets;
    private final boolean titan;
    private float time;
    private float travelTime;
    private boolean unlocked;
    private boolean reversed;
    private boolean traveling;
    private boolean emerging;

    public Portal(float x, float y, AssetManager assets) {
        this(x, y, assets, false);
    }

    protected Portal(float x, float y, AssetManager assets, boolean titan) {
        super(x, y, 190f, 220f);
        this.assets = assets;
        this.titan = titan;
        // Só o limiar da porta bloqueia/interage; efeitos luminosos não ampliam a hitbox.
        bounds.set(x + 49f, y + 24f, 92f, 98f);
    }

    @Override public void update(float delta) {
        time += delta;
        if (traveling) travelTime = Math.min(TRAVEL_DURATION, travelTime + delta);
    }

    @Override public void render(SpriteBatch batch) {
        float pulse = unlocked ? 1f + MathUtils.sin(time * 4.2f) * .012f : 1f;
        if (traveling) pulse += MathUtils.sin(travelTime / TRAVEL_DURATION * MathUtils.PI) * .075f;
        float drawW = width * pulse;
        float drawH = height * pulse;
        float px = position.x + (width - drawW) / 2f;
        float py = position.y + (height - drawH) / 2f;
        float previousColor = batch.getPackedColor();
        // A closed gateway still has a dark, readable iris: no transparent hole.
        float activation = unlocked ? 1f : .68f;
        float shimmer = .80f + .16f * MathUtils.sin(time * (titan ? 5.3f : 4.4f));
        float energySize = Math.min(drawW * .57f, drawH * .53f);
        float energyX = px + (drawW - energySize) * .5f;
        float energyY = py + (drawH - energySize) * .5f;
        float direction = reversed ? -1f : 1f;
        float rotation = direction * time * (titan ? 18f : 14f);
        if (traveling) rotation += (emerging ? -1f : 1f) * travelTime / TRAVEL_DURATION * 140f;
        TextureRegion vortex = assets.portalEnergyReworkRegion;
        batch.setColor(unlocked ? (titan ? .72f : 1f) : .27f,
            unlocked ? (titan ? .84f : 1f) : .42f, unlocked ? 1f : .53f,
            activation * shimmer);
        batch.draw(vortex, energyX, energyY, energySize * .5f, energySize * .5f,
            energySize, energySize, 1f, 1f, rotation);
        batch.setColor(.45f, .82f, 1f, unlocked ? activation * .2f : 0f);
        batch.draw(vortex, energyX + energySize * .065f, energyY + energySize * .065f,
            energySize * .435f, energySize * .435f,
            energySize * .87f, energySize * .87f, 1f, 1f, -rotation * .7f);
        batch.setColor(1f, 1f, 1f, 1f);
        float aspect = (float)assets.portalFrameReworkTexture.getWidth()
            / assets.portalFrameReworkTexture.getHeight();
        float ringW = Math.min(drawW, drawH * aspect);
        float ringH = ringW / aspect;
        batch.draw(assets.portalFrameReworkTexture,
            px + (drawW - ringW) * .5f,
            py + (drawH - ringH) * .5f, ringW, ringH);
        batch.setPackedColor(previousColor);
    }

    public void setUnlocked(boolean value) {
        unlocked = value;
        if (!value) { traveling = false; travelTime = 0f; }
    }

    public boolean isUnlocked() { return unlocked; }

    /** Espelha o portal de chegada, deixando clara a direção de retorno. */
    public void setReversed(boolean value) { reversed = value; }

    public void beginTraversal(boolean asExit) {
        if (!unlocked) return;
        emerging = asExit;
        traveling = true;
        travelTime = 0f;
    }

    public boolean isTraversalComplete() {
        return traveling && travelTime >= TRAVEL_DURATION;
    }

    public boolean isPlayerNear(Astronauta player) {
        return bounds.overlaps(player.getBounds());
    }

    @Override public void dispose() { }
}

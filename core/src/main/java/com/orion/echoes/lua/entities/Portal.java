package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.managers.AssetManager;

/** Portal comum às três fases, com animação de repouso, entrada e saída. */
public class Portal extends Entidade {
    private static final float TRAVEL_DURATION = .72f;
    private final TextureRegion[][] frames = new TextureRegion[4][4];
    private final TextureRegion[][] mirrored = new TextureRegion[4][4];
    private float time;
    private float travelTime;
    private boolean unlocked;
    private boolean reversed;
    private boolean traveling;
    private boolean emerging;

    public Portal(float x, float y, AssetManager assets) {
        super(x, y, 190f, 220f);
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                frames[row][column] = assets.portalFrame(column, row);
                mirrored[row][column] = new TextureRegion(frames[row][column]);
                mirrored[row][column].flip(true, false);
            }
        }
        // Só o limiar da porta bloqueia/interage; efeitos luminosos não ampliam a hitbox.
        bounds.set(x + 49f, y + 24f, 92f, 98f);
    }

    @Override public void update(float delta) {
        time += delta;
        if (traveling) travelTime = Math.min(TRAVEL_DURATION, travelTime + delta);
    }

    @Override public void render(SpriteBatch batch) {
        int row = traveling ? (emerging ? 3 : 2) : (unlocked ? 1 : 0);
        float speed = traveling ? .11f : (unlocked ? .14f : .42f);
        int column = traveling
            ? Math.min(3, (int)(travelTime / (TRAVEL_DURATION / 4f)))
            : unlocked ? (int)(time / speed) % 4 : (int)(time / speed) % 2;
        boolean flip = reversed ^ (traveling && travelTime > TRAVEL_DURATION * .52f);
        TextureRegion frame = (flip ? mirrored : frames)[row][column];
        float pulse = unlocked ? 1f + MathUtils.sin(time * 4.2f) * .012f : 1f;
        if (traveling) pulse += MathUtils.sin(travelTime / TRAVEL_DURATION * MathUtils.PI) * .075f;
        float drawW = width * pulse;
        float drawH = height * pulse;
        batch.draw(frame, position.x + (width - drawW) / 2f,
            position.y + (height - drawH) / 2f, drawW, drawH);
    }

    public void setUnlocked(boolean value) {
        unlocked = value;
        if (!value) traveling = false;
    }

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

package com.orion.echoes.lua.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Enemy;
import com.orion.echoes.lua.entities.MissionCollectible;
import com.orion.echoes.lua.entities.RepairStation;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.systems.MissionState;
import com.orion.echoes.lua.world.LunarWorld;

/**
 * Camada de tela cheia sobre o mundo: marcador de objetivo, cursor autoral e
 * vinheta de dano.
 *
 * Tudo aqui e desenhado em coordenadas de UI, nunca de mundo.
 */
public final class MissionOverlay implements Disposable {

    /** Distancia da borda em que o marcador de objetivo encosta. */
    private static final float MARKER_MARGIN = 58f;
    private static final float MARKER_SIZE = 46f;
    private static final float CURSOR_SIZE = 34f;
    private static final float TARGET_SNAP = 60f;
    /** A LibGDX exige pixmap de cursor com lado potencia de dois. */
    private static final int BLANK_CURSOR_SIZE = 16;

    private final SpriteBatch batch;
    private final AssetManager assets;
    private final OrthographicCamera worldCamera;
    private final OrthographicCamera uiCamera;
    private final Vector2 objectiveTarget = new Vector2();

    private float markerPulse;
    private Cursor blankCursor;

    public MissionOverlay(SpriteBatch batch, AssetManager assets,
                          OrthographicCamera worldCamera, OrthographicCamera uiCamera) {
        this.batch = batch;
        this.assets = assets;
        this.worldCamera = worldCamera;
        this.uiCamera = uiCamera;
        hideSystemCursor();
    }

    public void update(float delta) {
        markerPulse += delta;
    }

    /**
     * Marcador direcional do objetivo atual.
     *
     * So aparece com a Comunicacao reparada: e o beneficio jogavel daquele
     * reparo. Fica preso a borda quando o alvo esta fora do enquadramento e
     * pousa sobre ele quando entra em tela.
     */
    public void renderObjectiveMarker(LunarWorld world) {
        if (!world.getMission().isMapRevealed()) return;
        if (!resolveObjective(world, objectiveTarget)) return;

        float screenX = objectiveTarget.x - worldCamera.position.x + GameConfig.WINDOW_WIDTH / 2f;
        float screenY = objectiveTarget.y - worldCamera.position.y + GameConfig.WINDOW_HEIGHT / 2f;
        float centerX = GameConfig.WINDOW_WIDTH / 2f;
        float centerY = GameConfig.WINDOW_HEIGHT / 2f;
        float angle = MathUtils.atan2(screenY - centerY, screenX - centerX) * MathUtils.radiansToDegrees;
        boolean offScreen = screenX < MARKER_MARGIN
            || screenX > GameConfig.WINDOW_WIDTH - MARKER_MARGIN
            || screenY < MARKER_MARGIN
            || screenY > GameConfig.WINDOW_HEIGHT - MARKER_MARGIN;

        float drawX = MathUtils.clamp(screenX, MARKER_MARGIN, GameConfig.WINDOW_WIDTH - MARKER_MARGIN);
        float drawY = MathUtils.clamp(screenY, MARKER_MARGIN, GameConfig.WINDOW_HEIGHT - MARKER_MARGIN);
        float pulse = .82f + MathUtils.sin(markerPulse * 4.2f) * .18f;
        float size = MARKER_SIZE * pulse;

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.setColor(1f, 1f, 1f, offScreen ? .95f : .55f);
        batch.draw(assets.uiObjectiveMarkerTexture,
            drawX - size / 2f, drawY - size / 2f,
            size / 2f, size / 2f, size, size, 1f, 1f,
            offScreen ? angle : 0f);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Alvo do marcador, na mesma ordem de prioridade do texto de objetivo. */
    private boolean resolveObjective(LunarWorld world, Vector2 out) {
        MissionState mission = world.getMission();
        if (mission.getRepairCount() < 3) {
            if (nearestCollectible(world, out)) return true;
            for (RepairStation station : world.getRepairStations()) {
                if (mission.isRepaired(station.getType())) continue;
                if (mission.getPartCount(station.getType().getRequiredPart()) < 1) continue;
                out.set(station.getPosition().x + 63f, station.getPosition().y + 63f);
                return true;
            }
            return false;
        }
        if (!mission.hasWeapon()) {
            if (!mission.hasAllWeaponParts() && nearestCollectible(world, out)) return true;
            out.set(world.getCraftingStation().getPosition().x + 32f,
                world.getCraftingStation().getPosition().y + 32f);
            return true;
        }
        if (mission.getEnemiesDefeated() < mission.getTotalEnemies()) {
            Enemy nearest = null;
            float best = Float.MAX_VALUE;
            for (Enemy enemy : world.getEnemies()) {
                if (!enemy.isAtivo()) continue;
                float distance = Vector2.dst(enemy.centerX(), enemy.centerY(),
                    world.getPlayer().getPosition().x, world.getPlayer().getPosition().y);
                if (distance < best) {
                    best = distance;
                    nearest = enemy;
                }
            }
            if (nearest == null) return false;
            out.set(nearest.centerX(), nearest.centerY());
            return true;
        }
        out.set(world.getPortal().getPosition().x + world.getPortal().getBounds().width / 2f,
            world.getPortal().getPosition().y + world.getPortal().getBounds().height / 2f);
        return true;
    }

    private boolean nearestCollectible(LunarWorld world, Vector2 out) {
        MissionCollectible nearest = null;
        float best = Float.MAX_VALUE;
        for (MissionCollectible collectible : world.getCollectibles()) {
            if (!collectible.isAtivo()) continue;
            float distance = Vector2.dst(collectible.getPosition().x, collectible.getPosition().y,
                world.getPlayer().getPosition().x, world.getPlayer().getPosition().y);
            if (distance < best) {
                best = distance;
                nearest = collectible;
            }
        }
        if (nearest == null) return false;
        out.set(nearest.getPosition().x + 27f, nearest.getPosition().y + 27f);
        return true;
    }

    /** Cursor autoral: muda de forma quando a mira encosta em um hostil. */
    public void renderCursor(LunarWorld world, Vector2 aimWorld) {
        float x = Gdx.input.getX() * GameConfig.WINDOW_WIDTH / (float) Gdx.graphics.getWidth();
        float y = GameConfig.WINDOW_HEIGHT
            - Gdx.input.getY() * GameConfig.WINDOW_HEIGHT / (float) Gdx.graphics.getHeight();
        boolean onTarget = false;
        for (Enemy enemy : world.getEnemies()) {
            if (!enemy.isAtivo()) continue;
            if (Vector2.dst(enemy.centerX(), enemy.centerY(), aimWorld.x, aimWorld.y) < TARGET_SNAP) {
                onTarget = true;
                break;
            }
        }
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.setColor(1f, 1f, 1f, onTarget ? 1f : .8f);
        batch.draw(onTarget ? assets.uiCursorTargetTexture : assets.uiCursorDefaultTexture,
            x - CURSOR_SIZE / 2f, y - CURSOR_SIZE / 2f, CURSOR_SIZE, CURSOR_SIZE);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    public void renderDamageVignette(float alpha) {
        if (alpha <= 0f) return;
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.setColor(1f, 1f, 1f, alpha * .82f);
        batch.draw(assets.uiDamageVignetteTexture, 0f, 0f,
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * O jogo desenha o proprio cursor; o do sistema operacional sairia
     * duplicado por cima. Substitui-lo por um cursor transparente e a forma
     * de esconde-lo sem capturar o ponteiro dentro da janela.
     *
     * O tamanho e potencia de dois porque a LibGDX exige isso do pixmap de
     * cursor. Se ainda assim a plataforma recusar, o jogo segue com o cursor
     * do sistema visivel: um detalhe cosmetico nao pode derrubar a fase.
     */
    private void hideSystemCursor() {
        Pixmap pixmap = new Pixmap(BLANK_CURSOR_SIZE, BLANK_CURSOR_SIZE, Pixmap.Format.RGBA8888);
        try {
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();
            blankCursor = Gdx.graphics.newCursor(pixmap, 0, 0);
            if (blankCursor != null) Gdx.graphics.setCursor(blankCursor);
        } catch (Exception failure) {
            Gdx.app.error("MissionOverlay", "Cursor customizado indisponivel: "
                + failure.getMessage());
            blankCursor = null;
        } finally {
            pixmap.dispose();
        }
    }

    @Override
    public void dispose() {
        if (blankCursor == null) return;
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        blankCursor.dispose();
        blankCursor = null;
    }
}

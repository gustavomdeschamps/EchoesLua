package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.MarsObject;
import com.orion.echoes.lua.entities.Npc;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.render.AtlasRegionRenderer;

/** Isolated slow-motion gallery for checking trim bleed, pivots and collision silhouettes. */
public final class SpriteQaScreen implements Screen {
    @FunctionalInterface private interface FrameSource { TextureRegion get(int column, int row); }
    private record Strip(String name, int columns, int rows, FrameSource source,
                         float hurtWidth, float hurtHeight, float footWidth, float footHeight) { }

    private static final int PER_PAGE = 6;
    private static final float FRAME_TIME = .5f;
    private final EchoesLua game;
    private final AssetManager assets;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
    private final Array<Strip> strips = new Array<>();
    private float elapsed;
    private int page;

    public SpriteQaScreen(EchoesLua game) {
        this.game = game;
        assets = game.getAssets();
        batch = game.getBatch();
        camera.position.set(GameConfig.WINDOW_WIDTH / 2f, GameConfig.WINDOW_HEIGHT / 2f, 0f);
        camera.update();
        add("Astronauta", 4, 4, assets::astronautFrame, .40f, .62f, .34f, .15f);
        add("Astronauta · combate", 4, 3, assets::astronautCombatFrame, .48f, .62f, .34f, .15f);
        add("Hostil lunar", 4, 4, assets::lunarEnemyFrame, .58f, .46f, .54f, .17f);
        add("Drone de Marte", 4, 4, (c, r) -> assets.marsEnemyFrame(true, c, r), .62f, .46f, .50f, .15f);
        add("Rastejador de Marte", 4, 4, (c, r) -> assets.marsEnemyFrame(false, c, r), .66f, .42f, .58f, .16f);
        add("Caçador de Titã", 4, 4, assets::titanEnemyFrame, .64f, .48f, .58f, .17f);
        add("Chefe de Titã", 4, 4, assets::titanBossFrame, .58f, .64f, .52f, .17f);
        add("Ayla", 4, 4, (c, r) -> assets.npcVisualFrame(Npc.Visual.AYLA, c, r), .38f, .60f, .30f, .13f);
        add("Ayyub", 4, 4, (c, r) -> assets.npcVisualFrame(Npc.Visual.MARS_OFFICER, c, r), .40f, .59f, .31f, .13f);
        add("Lira", 4, 4, (c, r) -> assets.npcVisualFrame(Npc.Visual.LIRA, c, r), .38f, .61f, .30f, .13f);
        add("Estações lunares", 4, 4, (c, r) -> assets.repairStationFrame(r, c), .70f, .60f, .62f, .15f);
        add("Estações de Marte", 4, 3, (c, r) -> assets.marsStationFrame(
            r == 0 ? MarsObject.Kind.SOLAR_STATION : r == 1
                ? MarsObject.Kind.OXYGEN_STATION : MarsObject.Kind.COMMS_STATION, c), .70f, .60f, .62f, .15f);
        add("Portal de campanha", 4, 4, assets::portalFrame, .56f, .70f, .54f, .14f);
        add("Portal de Titã", 4, 2, assets::titanPortalFrame, .56f, .70f, .54f, .14f);
    }

    private void add(String name, int columns, int rows, FrameSource source,
                     float hurtWidth, float hurtHeight, float footWidth, float footHeight) {
        strips.add(new Strip(name, columns, rows, source, hurtWidth, hurtHeight, footWidth, footHeight));
    }

    @Override public void render(float delta) {
        elapsed += Math.min(delta, GameConfig.MAX_FRAME_DELTA);
        int pages = MathUtils.ceil(strips.size / (float) PER_PAGE);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) page = (page + 1) % pages;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) page = (page + pages - 1) % pages;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setScreen(new MenuScreen(game));
        Gdx.gl.glClearColor(.018f, .025f, .032f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        assets.titleFont.setColor(Color.WHITE);
        assets.titleFont.getData().setScale(.8f);
        assets.titleFont.draw(batch, "QA DE SPRITES  ·  0,5 s POR QUADRO", 42f, 686f);
        assets.font.setColor(.55f, .74f, .78f, 1f);
        assets.font.getData().setScale(.55f);
        assets.font.draw(batch, "verde: hurtbox   ciano: pegada   laranja: alcance de ataque   ← → páginas",
            42f, 650f);
        int start = page * PER_PAGE;
        for (int slot = 0; slot < PER_PAGE && start + slot < strips.size; slot++) {
            Strip strip = strips.get(start + slot);
            float x = 55f + (slot % 3) * 405f;
            float y = 350f - (slot / 3) * 292f;
            int column = (int)(elapsed / FRAME_TIME) % strip.columns;
            int row = (int)(elapsed / (FRAME_TIME * strip.columns)) % strip.rows;
            AtlasRegionRenderer.draw(batch, strip.source.get(column, row), x + 104f, y + 35f, 170f, 170f);
            assets.font.setColor(Color.WHITE);
            assets.font.draw(batch, strip.name + "  ·  linha " + (row + 1) + "  quadro " + (column + 1), x, y + 242f);
        }
        assets.font.draw(batch, "PÁGINA " + (page + 1) + " / " + pages, 1090f, 684f);
        batch.end();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (int slot = 0; slot < PER_PAGE && start + slot < strips.size; slot++) {
            Strip strip = strips.get(start + slot);
            float x = 55f + (slot % 3) * 405f + 104f;
            float y = 350f - (slot / 3) * 292f + 35f;
            box(shapes, Color.GREEN, x, y, 170f, 170f, strip.hurtWidth, strip.hurtHeight, .14f);
            box(shapes, Color.CYAN, x, y, 170f, 170f, strip.footWidth, strip.footHeight, .06f);
            shapes.setColor(Color.ORANGE);
            shapes.rect(x + 170f * .18f, y + 170f * .20f, 170f * .64f, 170f * .48f);
        }
        shapes.end();
    }

    private static void box(ShapeRenderer shapes, Color color, float x, float y, float w, float h,
                            float widthRatio, float heightRatio, float bottomRatio) {
        float boxWidth = w * widthRatio;
        shapes.setColor(color);
        shapes.rect(x + (w - boxWidth) / 2f, y + h * bottomRatio, boxWidth, h * heightRatio);
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void show() { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { shapes.dispose(); }
}

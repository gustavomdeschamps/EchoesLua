package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;

/** Catálogo visual carregado de modo incremental pela LoadingScreen. */
public final class AssetManager implements Disposable {
    private static final String GAME_ATLAS = "atlases/game.atlas";
    private static final String UI_ATLAS = "atlases/ui.atlas";
    private static final String FX_ATLAS = "atlases/fx.atlas";
    private static final String LUNAR_GROUND = "textures/lunar_ground.png";
    private static final String MARS_GROUND = "textures/mars_ground.png";

    private final com.badlogic.gdx.assets.AssetManager loader =
        new com.badlogic.gdx.assets.AssetManager();
    private boolean queued;
    private boolean ready;

    public TextureRegion astronautaSheetTexture;
    public TextureRegion astronautCombatSheetTexture;
    public TextureRegion pulseRifleTexture;
    public Texture backgroundLuaTexture;
    public TextureRegion baseLunarTexture;
    public TextureRegion oxigenioTexture;
    public TextureRegion comidaTexture;
    public TextureRegion geloTexture;
    public TextureRegion missionAtlasTexture;
    public Texture marsBackgroundTexture;
    public TextureRegion introKeyArtTexture;
    public TextureRegion marsAtlasTexture;
    public TextureRegion lunarEnemySheetTexture;
    public TextureRegion marsDroneSheetTexture;
    public TextureRegion marsCrawlerSheetTexture;
    public TextureRegion lunarObstaclesTexture;
    public TextureRegion marsObstaclesTexture;
    public TextureRegion actionFxTexture;
    public TextureRegion energyFxTexture;
    public TextureRegion landmarksTexture;
    public TextureRegion uiPanelTexture;
    public TextureRegion uiPanelHudTexture;
    public TextureRegion uiPanelDialogTexture;
    public TextureRegion uiPanelModalTexture;
    public TextureRegion uiButtonNormalTexture;
    public TextureRegion uiButtonHoverTexture;
    public TextureRegion uiButtonPressedTexture;
    public TextureRegion uiButtonDisabledTexture;
    public TextureRegion uiBarTrackTexture;
    public TextureRegion uiBarFillTexture;
    public TextureRegion uiResourceIconsTexture;
    public TextureRegion uiDamageVignetteTexture;
    public TextureRegion uiWhiteTexture;
    public BitmapFont font;
    public BitmapFont titleFont;

    public void queue() {
        if (queued) return;
        queued = true;
        loader.load(GAME_ATLAS, TextureAtlas.class);
        loader.load(UI_ATLAS, TextureAtlas.class);
        loader.load(FX_ATLAS, TextureAtlas.class);

        TextureLoader.TextureParameter terrain = new TextureLoader.TextureParameter();
        terrain.minFilter = Texture.TextureFilter.Linear;
        terrain.magFilter = Texture.TextureFilter.Linear;
        terrain.wrapU = Texture.TextureWrap.Repeat;
        terrain.wrapV = Texture.TextureWrap.Repeat;
        loader.load(LUNAR_GROUND, Texture.class, terrain);
        loader.load(MARS_GROUND, Texture.class, terrain);
    }

    /** Avança a fila sem bloquear; retorna true somente quando tudo está pronto. */
    public boolean update() {
        if (!queued) throw new IllegalStateException("queue() deve ser chamado antes de update().");
        if (!loader.update()) return false;
        bindLoadedAssets();
        return true;
    }

    public float getProgress() {
        return loader.getProgress();
    }

    public boolean isReady() {
        return ready;
    }

    private void bindLoadedAssets() {
        if (ready) return;
        TextureAtlas gameAtlas = loader.get(GAME_ATLAS, TextureAtlas.class);
        TextureAtlas uiAtlas = loader.get(UI_ATLAS, TextureAtlas.class);
        TextureAtlas fxAtlas = loader.get(FX_ATLAS, TextureAtlas.class);

        astronautaSheetTexture = required(gameAtlas, "astronauta_sheet");
        astronautCombatSheetTexture = required(gameAtlas, "astronaut_combat_sheet");
        pulseRifleTexture = required(gameAtlas, "pulse_rifle");
        baseLunarTexture = required(gameAtlas, "base_lunar");
        oxigenioTexture = required(gameAtlas, "oxigenio");
        comidaTexture = required(gameAtlas, "comida");
        geloTexture = required(gameAtlas, "gelo");
        missionAtlasTexture = required(gameAtlas, "mission_atlas_unified");
        marsAtlasTexture = required(gameAtlas, "mars_atlas_v4");
        lunarEnemySheetTexture = required(gameAtlas, "lunar_enemy_sheet");
        marsDroneSheetTexture = required(gameAtlas, "mars_drone_sheet");
        marsCrawlerSheetTexture = required(gameAtlas, "mars_crawler_sheet");
        lunarObstaclesTexture = required(gameAtlas, "lunar_obstacles");
        marsObstaclesTexture = required(gameAtlas, "mars_obstacles");
        landmarksTexture = required(gameAtlas, "landmarks");
        introKeyArtTexture = required(uiAtlas, "intro_keyart_v2");
        uiPanelTexture = required(uiAtlas, "ui_panel_frame");
        uiPanelHudTexture = required(uiAtlas, "panel_hud");
        uiPanelDialogTexture = required(uiAtlas, "panel_dialog");
        uiPanelModalTexture = required(uiAtlas, "panel_modal");
        uiButtonNormalTexture = required(uiAtlas, "button_normal");
        uiButtonHoverTexture = required(uiAtlas, "button_hover");
        uiButtonPressedTexture = required(uiAtlas, "button_pressed");
        uiButtonDisabledTexture = required(uiAtlas, "button_disabled");
        uiBarTrackTexture = required(uiAtlas, "bar_track");
        uiBarFillTexture = required(uiAtlas, "bar_fill");
        uiResourceIconsTexture = required(uiAtlas, "resource_icons");
        uiDamageVignetteTexture = required(uiAtlas, "damage_vignette");
        uiWhiteTexture = required(uiAtlas, "white_pixel");
        actionFxTexture = required(fxAtlas, "action_fx_sheet");
        energyFxTexture = required(fxAtlas, "energy_fx_sheet");
        backgroundLuaTexture = loader.get(LUNAR_GROUND, Texture.class);
        marsBackgroundTexture = loader.get(MARS_GROUND, Texture.class);

        font = generateFont("fonts/ChakraPetch-Regular.ttf", 25);
        titleFont = generateFont("fonts/ChakraPetch-SemiBold.ttf", 32);
        ready = true;
    }

    private TextureRegion required(TextureAtlas atlas, String name) {
        TextureRegion region = atlas.findRegion(name);
        if (region == null) throw new IllegalStateException("Região obrigatória ausente no atlas: " + name);
        return region;
    }

    private BitmapFont generateFont(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS
            + "áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇºª–—";
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont generated = generator.generateFont(parameter);
        generator.dispose();
        return generated;
    }

    public TextureRegion astronautFrame(int column, int row) {
        return gridRegion(astronautaSheetTexture, 4, 4, column, row, 0);
    }

    public TextureRegion astronautCombatFrame(int column, int row) {
        return gridRegion(astronautCombatSheetTexture, 4, 3, column, row, 0);
    }

    public TextureRegion marsRegion(int column, int row) {
        return gridRegion(marsAtlasTexture, 4, 3, column, row, 2);
    }

    public TextureRegion lunarEnemyFrame(int column, int row) {
        return gridRegion(lunarEnemySheetTexture, 4, 4, column, row, 2);
    }

    public TextureRegion marsEnemyFrame(boolean drone, int column, int row) {
        return gridRegion(drone ? marsDroneSheetTexture : marsCrawlerSheetTexture,
            4, 4, column, row, 2);
    }

    public TextureRegion lunarObstacleRegion(int index) {
        return gridRegion(lunarObstaclesTexture, 3, 2, index % 3, index / 3, 2);
    }

    public TextureRegion marsObstacleRegion(int index) {
        return gridRegion(marsObstaclesTexture, 3, 2, index % 3, index / 3, 2);
    }

    public TextureRegion actionFxFrame(int column, int row) {
        return gridRegion(actionFxTexture, 6, 4, column, row, 0);
    }

    public TextureRegion energyFxFrame(int column, int row) {
        return gridRegion(energyFxTexture, 6, 4, column, row, 0);
    }

    public TextureRegion landmarkRegion(int column, int row) {
        return gridRegion(landmarksTexture, 4, 2, column, row, 2);
    }

    public NinePatch uiPanelPatch() {
        return new NinePatch(uiPanelHudTexture, 24, 24, 24, 24);
    }

    public NinePatch uiDialogPatch() { return new NinePatch(uiPanelDialogTexture, 24, 24, 24, 24); }
    public NinePatch uiModalPatch() { return new NinePatch(uiPanelModalTexture, 24, 24, 24, 24); }

    public TextureRegion resourceIcon(int index) {
        if (index < 0 || index > 3) throw new IllegalArgumentException("Ícone inválido: " + index);
        return gridRegion(uiResourceIconsTexture, 4, 1, index, 0, 0);
    }

    private TextureRegion gridRegion(TextureRegion sheet, int columns, int rows,
                                     int column, int row, int inset) {
        if (column < 0 || column >= columns || row < 0 || row >= rows) {
            throw new IllegalArgumentException("Célula fora da grade: " + column + "," + row);
        }
        int cellWidth = sheet.getRegionWidth() / columns;
        int cellHeight = sheet.getRegionHeight() / rows;
        return new TextureRegion(sheet.getTexture(),
            sheet.getRegionX() + column * cellWidth + inset,
            sheet.getRegionY() + row * cellHeight + inset,
            cellWidth - inset * 2, cellHeight - inset * 2);
    }

    public TextureRegion missionRegion(int column, int row) {
        return gridRegion(missionAtlasTexture, 4, 4, column, row, 2);
    }

    public TextureRegion missionRegion(MissionSprite sprite) {
        if (sprite == null) throw new IllegalArgumentException("Sprite de missão nulo.");
        return missionRegion(sprite.column(), sprite.row());
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
        loader.dispose();
        ready = false;
        queued = false;
    }
}

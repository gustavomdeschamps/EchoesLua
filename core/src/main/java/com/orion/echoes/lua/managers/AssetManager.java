package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;

public class AssetManager implements Disposable {

    // ==========================================
    // PLAYER
    // ==========================================

    public Texture astronautaSheetTexture;
    public Texture pulseRifleTexture;

    // ==========================================
    // CENÁRIO
    // ==========================================

    public Texture backgroundLuaTexture;
    public Texture baseLunarTexture;

    // ==========================================
    // ITENS
    // ==========================================

    public Texture oxigenioTexture;
    public Texture comidaTexture;
    public Texture geloTexture;

    // ==========================================
    // OBSTÁCULOS
    // ==========================================

    public Texture missionAtlasTexture;
    public Texture marsBackgroundTexture;
    public Texture introKeyArtTexture;
    public Texture marsAtlasTexture;
    public Texture lunarEnemySheetTexture;
    public Texture marsDroneSheetTexture;
    public Texture marsCrawlerSheetTexture;
    public Texture lunarObstaclesTexture;
    public Texture marsObstaclesTexture;
    public Texture actionFxTexture;
    public Texture energyFxTexture;
    public Texture landmarksTexture;
    public Texture uiPanelTexture;

    // ==========================================
    // UI
    // ==========================================

    public BitmapFont font;
    public BitmapFont titleFont;

    // ==========================================
    // LOAD
    // ==========================================

    public void load() {

        // PLAYER
        astronautaSheetTexture = loadTextureOrPlaceholder("textures/astronauta_sheet.png", Color.WHITE);
        pulseRifleTexture = loadTextureOrPlaceholder("textures/pulse_rifle.png", Color.WHITE);

        // LUA
        backgroundLuaTexture =
            loadTextureOrPlaceholder(
                "textures/lunar_ground.png",
                new Color(
                    0.12f,
                    0.13f,
                    0.16f,
                    1f
                )
            );

        baseLunarTexture =
            loadTextureOrPlaceholder(
                "textures/base_lunar.png",
                Color.LIGHT_GRAY
            );

        // ITENS

        oxigenioTexture =
            loadTextureOrPlaceholder(
                "textures/oxigenio.png",
                Color.CYAN
            );

        comidaTexture =
            loadTextureOrPlaceholder(
                "textures/comida.png",
                Color.ORANGE
            );

        geloTexture =
            loadTextureOrPlaceholder(
                "textures/gelo.png",
                Color.SKY
            );

        // OBSTÁCULO

        missionAtlasTexture =
            loadTextureOrPlaceholder(
                "textures/mission_atlas_unified.png",
                Color.CYAN
            );

        marsBackgroundTexture =
            loadTextureOrPlaceholder(
                "textures/mars_ground.png",
                Color.FIREBRICK
            );

        introKeyArtTexture = loadTextureOrPlaceholder("textures/intro_keyart_v2.png", Color.DARK_GRAY);
        marsAtlasTexture = loadTextureOrPlaceholder("textures/mars_atlas_v4.png", Color.ORANGE);
        lunarEnemySheetTexture = loadTextureOrPlaceholder("textures/lunar_enemy_sheet.png", Color.MAGENTA);
        marsDroneSheetTexture = loadTextureOrPlaceholder("textures/mars_drone_sheet.png", Color.ORANGE);
        marsCrawlerSheetTexture = loadTextureOrPlaceholder("textures/mars_crawler_sheet.png", Color.ORANGE);
        lunarObstaclesTexture = loadTextureOrPlaceholder("textures/lunar_obstacles.png", Color.DARK_GRAY);
        marsObstaclesTexture = loadTextureOrPlaceholder("textures/mars_obstacles.png", Color.FIREBRICK);
        actionFxTexture = loadTextureOrPlaceholder("textures/action_fx_sheet.png", Color.WHITE);
        energyFxTexture = loadTextureOrPlaceholder("textures/energy_fx_sheet.png", Color.CYAN);
        landmarksTexture = loadTextureOrPlaceholder("textures/landmarks.png", Color.LIGHT_GRAY);
        uiPanelTexture = loadTextureOrPlaceholder("textures/ui_panel_frame.png", Color.DARK_GRAY);

        // FILTROS

        aplicarFiltroNearest(astronautaSheetTexture);
        aplicarFiltro(pulseRifleTexture);

        aplicarFiltro(
            backgroundLuaTexture
        );

        aplicarFiltro(
            baseLunarTexture
        );

        aplicarFiltro(
            oxigenioTexture
        );

        aplicarFiltro(
            comidaTexture
        );

        aplicarFiltro(
            geloTexture
        );

        aplicarFiltro(
            missionAtlasTexture
        );

        aplicarFiltro(
            marsBackgroundTexture
        );
        aplicarFiltro(introKeyArtTexture);
        aplicarFiltro(marsAtlasTexture);
        aplicarFiltro(lunarEnemySheetTexture);
        aplicarFiltro(marsDroneSheetTexture);
        aplicarFiltro(marsCrawlerSheetTexture);
        aplicarFiltro(lunarObstaclesTexture);
        aplicarFiltro(marsObstaclesTexture);
        aplicarFiltro(actionFxTexture);
        aplicarFiltro(energyFxTexture);
        aplicarFiltro(landmarksTexture);
        aplicarFiltro(uiPanelTexture);
        backgroundLuaTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        marsBackgroundTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        // FONTE

        // Bahnschrift nasce de desenho técnico DIN: legível em tamanhos pequenos e
        // coerente com a sinalização industrial das bases, sem aspecto de dashboard web.
        // Segoe UI mantém acentos e formas abertas em escalas pequenas; o peso
        // bold fica reservado à hierarquia, sem o aspecto monoespaçado anterior.
        font = gerarFonte("fonts/SegoeUI.ttf", 25);
        titleFont = gerarFonte("fonts/SegoeUI-Bold.ttf", 32);
    }

    private BitmapFont gerarFonte(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS
            + "áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇºª–—";
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont generated = generator.generateFont(parameter);
        generator.dispose();
        return generated;
    }

    // ==========================================
    // CARREGAMENTO SEGURO
    // ==========================================

    private Texture loadTextureOrPlaceholder(
        String path,
        Color color
    ) {

        if (
            Gdx.files
                .internal(path)
                .exists()
        ) {

            return new Texture(
                Gdx.files.internal(path)
            );
        }

        Gdx.app.log(
            "AssetManager",
            "Asset não encontrado: "
                + path
                + ". Usando placeholder."
        );

        return criarPlaceholder(
            color
        );
    }

    // ==========================================
    // PLACEHOLDER
    // ==========================================

    private Texture criarPlaceholder(
        Color color
    ) {

        Pixmap pixmap =
            new Pixmap(
                64,
                64,
                Pixmap.Format.RGBA8888
            );

        pixmap.setColor(color);

        pixmap.fill();

        pixmap.setColor(
            Color.WHITE
        );

        pixmap.drawRectangle(
            1,
            1,
            62,
            62
        );

        Texture texture =
            new Texture(pixmap);

        pixmap.dispose();

        return texture;
    }

    // ==========================================
    // FILTRO
    // ==========================================

    private void aplicarFiltro(
        Texture texture
    ) {

        if (texture == null) {
            return;
        }

        texture.setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );
    }

    private void aplicarFiltroNearest(Texture texture) {
        if (texture != null) texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    public TextureRegion marsRegion(int column, int row) {
        int cellWidth = marsAtlasTexture.getWidth() / 4;
        int cellHeight = marsAtlasTexture.getHeight() / 3;
        return new TextureRegion(marsAtlasTexture, column * cellWidth + 2, row * cellHeight + 2,
            cellWidth - 4, cellHeight - 4);
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
        return new NinePatch(new TextureRegion(uiPanelTexture), 24, 24, 24, 24);
    }

    private TextureRegion gridRegion(Texture texture, int columns, int rows,
                                     int column, int row, int inset) {
        int cellWidth = texture.getWidth() / columns;
        int cellHeight = texture.getHeight() / rows;
        return new TextureRegion(texture, column * cellWidth + inset, row * cellHeight + inset,
            cellWidth - inset * 2, cellHeight - inset * 2);
    }

    /** Retorna uma celula do atlas 4x4, indexado a partir do canto superior esquerdo. */
    public TextureRegion missionRegion(int column, int row) {
        if (column < 0 || column > 3 || row < 0 || row > 3) {
            throw new IllegalArgumentException("Celula do atlas fora do intervalo 0..3.");
        }
        int cellWidth = missionAtlasTexture.getWidth() / 4;
        int cellHeight = missionAtlasTexture.getHeight() / 4;
        return new TextureRegion(
            missionAtlasTexture,
            column * cellWidth + 2,
            row * cellHeight + 2,
            cellWidth - 4,
            cellHeight - 4
        );
    }

    public TextureRegion missionRegion(MissionSprite sprite) {
        if (sprite == null) throw new IllegalArgumentException("Sprite de missao nulo.");
        return missionRegion(sprite.column(), sprite.row());
    }

    // ==========================================
    // DISPOSE
    // ==========================================

    @Override
    public void dispose() {

        disposeTexture(astronautaSheetTexture);
        disposeTexture(pulseRifleTexture);

        disposeTexture(
            backgroundLuaTexture
        );

        disposeTexture(
            baseLunarTexture
        );

        disposeTexture(
            oxigenioTexture
        );

        disposeTexture(
            comidaTexture
        );

        disposeTexture(
            geloTexture
        );

        disposeTexture(
            missionAtlasTexture
        );

        disposeTexture(
            marsBackgroundTexture
        );
        disposeTexture(introKeyArtTexture);
        disposeTexture(marsAtlasTexture);
        disposeTexture(lunarEnemySheetTexture);
        disposeTexture(marsDroneSheetTexture);
        disposeTexture(marsCrawlerSheetTexture);
        disposeTexture(lunarObstaclesTexture);
        disposeTexture(marsObstaclesTexture);
        disposeTexture(actionFxTexture);
        disposeTexture(energyFxTexture);
        disposeTexture(landmarksTexture);
        disposeTexture(uiPanelTexture);

        if (font != null) {
            font.dispose();
        }
        if (titleFont != null) titleFont.dispose();
    }

    private void disposeTexture(
        Texture texture
    ) {

        if (texture != null) {
            texture.dispose();
        }
    }
}

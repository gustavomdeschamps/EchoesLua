package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;

public class AssetManager implements Disposable {

    // ==========================================
    // PLAYER
    // ==========================================

    public Texture astronautaTexture;
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

    public Texture obstacleTexture;
    public Texture missionAtlasTexture;
    public Texture marsBackgroundTexture;
    public Texture menuEmblemTexture;
    public Texture enemySheetTexture;
    public Texture introKeyArtTexture;
    public Texture gameplayFxTexture;
    public Texture lunarEnemyTexture;
    public Texture marsAtlasTexture;
    public Texture echoSignalCoreTexture;

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
        astronautaTexture =
            loadTextureOrPlaceholder(
                "textures/astronauta.png",
                Color.WHITE
            );

        astronautaSheetTexture = loadTextureOrPlaceholder("textures/astronauta_sheet.png", Color.WHITE);
        pulseRifleTexture = loadTextureOrPlaceholder("textures/pulse_rifle.png", Color.WHITE);

        // LUA
        backgroundLuaTexture =
            loadTextureOrPlaceholder(
                "textures/lunar_ground_v2.png",
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

        obstacleTexture =
            loadTextureOrPlaceholder(
                "textures/obstacle.png",
                Color.DARK_GRAY
            );

        missionAtlasTexture =
            loadTextureOrPlaceholder(
                "textures/mission_atlas.png",
                Color.CYAN
            );

        marsBackgroundTexture =
            loadTextureOrPlaceholder(
                "textures/mars_background.png",
                Color.FIREBRICK
            );

        menuEmblemTexture = loadTextureOrPlaceholder("textures/menu_emblem.png", Color.CYAN);
        enemySheetTexture = loadTextureOrPlaceholder("textures/enemy_sheet_v2.png", Color.MAGENTA);
        introKeyArtTexture = loadTextureOrPlaceholder("textures/intro_keyart_v2.png", Color.DARK_GRAY);
        gameplayFxTexture = loadTextureOrPlaceholder("textures/gameplay_fx_atlas.png", Color.WHITE);
        lunarEnemyTexture = loadTextureOrPlaceholder("textures/lunar_enemy_v3.png", Color.MAGENTA);
        marsAtlasTexture = loadTextureOrPlaceholder("textures/mars_atlas_v3.png", Color.ORANGE);
        echoSignalCoreTexture = loadTextureOrPlaceholder("textures/echo_signal_core.png", Color.CYAN);

        // FILTROS

        aplicarFiltro(
            astronautaTexture
        );
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
            obstacleTexture
        );

        aplicarFiltro(
            missionAtlasTexture
        );

        aplicarFiltro(
            marsBackgroundTexture
        );
        aplicarFiltro(menuEmblemTexture);
        aplicarFiltro(enemySheetTexture);
        aplicarFiltro(introKeyArtTexture);
        aplicarFiltro(gameplayFxTexture);
        aplicarFiltro(lunarEnemyTexture);
        aplicarFiltro(marsAtlasTexture);
        aplicarFiltro(echoSignalCoreTexture);
        backgroundLuaTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        // FONTE

        // Bahnschrift nasce de desenho técnico DIN: legível em tamanhos pequenos e
        // coerente com a sinalização industrial das bases, sem aspecto de dashboard web.
        font = gerarFonte("fonts/Bahnschrift.ttf", 25);
        titleFont = gerarFonte("fonts/Bahnschrift.ttf", 32);
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

    public TextureRegion fxRegion(int column) {
        int cellWidth = gameplayFxTexture.getWidth() / 4;
        return new TextureRegion(gameplayFxTexture, column * cellWidth + 2, 2,
            cellWidth - 4, gameplayFxTexture.getHeight() - 4);
    }

    public TextureRegion marsRegion(int column, int row) {
        int cellWidth = marsAtlasTexture.getWidth() / 4;
        int cellHeight = marsAtlasTexture.getHeight() / 3;
        return new TextureRegion(marsAtlasTexture, column * cellWidth + 2, row * cellHeight + 2,
            cellWidth - 4, cellHeight - 4);
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

        disposeTexture(
            astronautaTexture
        );
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
            obstacleTexture
        );

        disposeTexture(
            missionAtlasTexture
        );

        disposeTexture(
            marsBackgroundTexture
        );
        disposeTexture(menuEmblemTexture);
        disposeTexture(enemySheetTexture);
        disposeTexture(introKeyArtTexture);
        disposeTexture(gameplayFxTexture);
        disposeTexture(lunarEnemyTexture);
        disposeTexture(marsAtlasTexture);
        disposeTexture(echoSignalCoreTexture);

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

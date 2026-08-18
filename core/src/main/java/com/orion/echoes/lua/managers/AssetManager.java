package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Disposable;

public class AssetManager implements Disposable {

    // ==========================================
    // PLAYER
    // ==========================================

    public Texture astronautaTexture;

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

    // ==========================================
    // UI
    // ==========================================

    public BitmapFont font;

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

        // LUA
        backgroundLuaTexture =
            loadTextureOrPlaceholder(
                "textures/lua_background.png",
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

        // FILTROS

        aplicarFiltro(
            astronautaTexture
        );

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

        // FONTE

        font =
            new BitmapFont();

        font.getData().setScale(
            1.15f
        );
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

    // ==========================================
    // DISPOSE
    // ==========================================

    @Override
    public void dispose() {

        disposeTexture(
            astronautaTexture
        );

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

        if (font != null) {
            font.dispose();
        }
    }

    private void disposeTexture(
        Texture texture
    ) {

        if (texture != null) {
            texture.dispose();
        }
    }
}

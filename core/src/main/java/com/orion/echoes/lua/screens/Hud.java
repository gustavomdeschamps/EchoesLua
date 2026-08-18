package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.managers.AssetManager;

public class Hud implements Disposable {

    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final BitmapFont font;
    private final GlyphLayout layout;

    private final ShapeRenderer shapeRenderer;

    public Hud(
        AssetManager assets
    ) {

        camera =
            new OrthographicCamera();

        viewport =
            new FitViewport(
                GameConfig.WINDOW_WIDTH,
                GameConfig.WINDOW_HEIGHT,
                camera
            );

        camera.position.set(
            GameConfig.WINDOW_WIDTH / 2f,
            GameConfig.WINDOW_HEIGHT / 2f,
            0
        );

        camera.update();

        font =
            assets.font;

        layout =
            new GlyphLayout();

        shapeRenderer =
            new ShapeRenderer();
    }

    // =====================================================
    // RENDER
    // =====================================================

    public void render(
        SpriteBatch batch,
        Astronauta astronauta
    ) {

        float oxigenio =
            astronauta.getOxigenio();

        float energia =
            astronauta.getEnergia();

        float tempo =
            astronauta.getTempoVivo();

        float tempoRestante =
            Math.max(
                0,
                60f - tempo
            );

        // =================================================
        // FORMAS
        // =================================================

        shapeRenderer.setProjectionMatrix(
            camera.combined
        );

        Gdx.gl.glEnable(
            GL20.GL_BLEND
        );

        Gdx.gl.glBlendFunc(
            GL20.GL_SRC_ALPHA,
            GL20.GL_ONE_MINUS_SRC_ALPHA
        );

        shapeRenderer.begin(
            ShapeRenderer.ShapeType.Filled
        );

        // =================================================
        // PAINEL PRINCIPAL ESQUERDO
        // =================================================

        desenharPainel(
            18,
            GameConfig.WINDOW_HEIGHT - 170,
            360,
            150
        );

        // linha azul superior
        shapeRenderer.setColor(
            0.05f,
            0.65f,
            1f,
            1f
        );

        shapeRenderer.rect(
            18,
            GameConfig.WINDOW_HEIGHT - 25,
            360,
            4
        );

        // =================================================
        // OXIGÊNIO
        // =================================================

        desenharBarra(
            135,
            GameConfig.WINDOW_HEIGHT - 75,
            220,
            17,
            oxigenio / 100f,
            getCorOxigenio(oxigenio)
        );

        // =================================================
        // ENERGIA
        // =================================================

        desenharBarra(
            135,
            GameConfig.WINDOW_HEIGHT - 115,
            220,
            17,
            energia / 100f,
            getCorEnergia(energia)
        );

        // =================================================
        // PAINEL DO TEMPO
        // =================================================

        float painelTempoX =
            GameConfig.WINDOW_WIDTH / 2f - 125;

        desenharPainel(
            painelTempoX,
            GameConfig.WINDOW_HEIGHT - 85,
            250,
            65
        );

        shapeRenderer.setColor(
            0.1f,
            0.75f,
            1f,
            1f
        );

        shapeRenderer.rect(
            painelTempoX,
            GameConfig.WINDOW_HEIGHT - 25,
            250,
            4
        );

        // =================================================
        // INVENTÁRIO DIREITO
        // =================================================

        desenharPainel(
            GameConfig.WINDOW_WIDTH - 350,
            GameConfig.WINDOW_HEIGHT - 115,
            332,
            95
        );

        shapeRenderer.setColor(
            0.05f,
            0.65f,
            1f,
            1f
        );

        shapeRenderer.rect(
            GameConfig.WINDOW_WIDTH - 350,
            GameConfig.WINDOW_HEIGHT - 25,
            332,
            4
        );

        // separadores
        shapeRenderer.setColor(
            0.15f,
            0.25f,
            0.35f,
            0.8f
        );

        shapeRenderer.rect(
            GameConfig.WINDOW_WIDTH - 238,
            GameConfig.WINDOW_HEIGHT - 100,
            1,
            55
        );

        shapeRenderer.rect(
            GameConfig.WINDOW_WIDTH - 130,
            GameConfig.WINDOW_HEIGHT - 100,
            1,
            55
        );

        // =================================================
        // BASE - AVISO INFERIOR
        // =================================================

        if (
            astronauta.isProtegido()
        ) {

            float largura =
                350;

            float x =
                (
                    GameConfig.WINDOW_WIDTH
                        - largura
                )
                    / 2f;

            shapeRenderer.setColor(
                0.02f,
                0.18f,
                0.13f,
                0.92f
            );

            shapeRenderer.rect(
                x,
                25,
                largura,
                45
            );

            shapeRenderer.setColor(
                0.1f,
                1f,
                0.55f,
                1f
            );

            shapeRenderer.rect(
                x,
                25,
                4,
                45
            );

            shapeRenderer.rect(
                x + largura - 4,
                25,
                4,
                45
            );
        }

        shapeRenderer.end();

        Gdx.gl.glDisable(
            GL20.GL_BLEND
        );

        // =================================================
        // TEXTOS
        // =================================================

        batch.setProjectionMatrix(
            camera.combined
        );

        batch.begin();

        // =====================================
        // ECHOES
        // =====================================

        font.getData().setScale(
            1.05f
        );

        font.setColor(
            Color.SKY
        );

        font.draw(
            batch,
            "ECHOES // LUA",
            35,
            GameConfig.WINDOW_HEIGHT - 42
        );

        // =====================================
        // O2
        // =====================================

        font.getData().setScale(
            1.05f
        );

        font.setColor(
            getCorOxigenio(oxigenio)
        );

        font.draw(
            batch,
            "O2",
            35,
            GameConfig.WINDOW_HEIGHT - 68
        );

        font.setColor(
            Color.WHITE
        );

        font.draw(
            batch,
            String.format(
                "%.0f%%",
                oxigenio
            ),
            80,
            GameConfig.WINDOW_HEIGHT - 68
        );

        // =====================================
        // ENERGIA
        // =====================================

        font.setColor(
            getCorEnergia(energia)
        );

        font.draw(
            batch,
            "ENERGIA",
            35,
            GameConfig.WINDOW_HEIGHT - 108
        );

        font.setColor(
            Color.WHITE
        );

        font.draw(
            batch,
            String.format(
                "%.0f%%",
                energia
            ),
            85,
            GameConfig.WINDOW_HEIGHT - 132
        );

        // =====================================
        // TEMPO
        // =====================================

        desenharTextoCentralizado(
            batch,
            "SOBREVIVA",
            1f,
            Color.SKY,
            GameConfig.WINDOW_HEIGHT - 42
        );

        Color corTempo;

        if (tempoRestante <= 10f) {

            corTempo =
                Color.RED;

        } else if (tempoRestante <= 20f) {

            corTempo =
                Color.ORANGE;

        } else {

            corTempo =
                Color.WHITE;
        }

        desenharTextoCentralizado(
            batch,
            String.format(
                "%.0f s",
                tempoRestante
            ),
            1.55f,
            corTempo,
            GameConfig.WINDOW_HEIGHT - 69
        );

        // =====================================
        // INVENTÁRIO
        // =====================================

        float direita =
            GameConfig.WINDOW_WIDTH;

        font.getData().setScale(
            0.95f
        );

        font.setColor(
            Color.SKY
        );

        font.draw(
            batch,
            "RECURSOS",
            direita - 330,
            GameConfig.WINDOW_HEIGHT - 42
        );

        // GELO
        font.setColor(
            new Color(
                0.25f,
                0.8f,
                1f,
                1f
            )
        );

        font.draw(
            batch,
            "GELO",
            direita - 325,
            GameConfig.WINDOW_HEIGHT - 70
        );

        font.getData().setScale(
            1.25f
        );

        font.setColor(
            Color.WHITE
        );

        font.draw(
            batch,
            String.valueOf(
                astronauta.getGelo()
            ),
            direita - 310,
            GameConfig.WINDOW_HEIGHT - 95
        );

        // ÁGUA
        font.getData().setScale(
            0.95f
        );

        font.setColor(
            Color.CYAN
        );

        font.draw(
            batch,
            "AGUA",
            direita - 215,
            GameConfig.WINDOW_HEIGHT - 70
        );

        font.getData().setScale(
            1.25f
        );

        font.setColor(
            Color.WHITE
        );

        font.draw(
            batch,
            String.valueOf(
                astronauta.getAgua()
            ),
            direita - 200,
            GameConfig.WINDOW_HEIGHT - 95
        );

        // COMBUSTÍVEL
        font.getData().setScale(
            0.85f
        );

        font.setColor(
            Color.ORANGE
        );

        font.draw(
            batch,
            "ENERGIA",
            direita - 112,
            GameConfig.WINDOW_HEIGHT - 70
        );

        font.getData().setScale(
            1.25f
        );

        font.setColor(
            Color.WHITE
        );

        font.draw(
            batch,
            String.valueOf(
                astronauta.getCombustivel()
            ),
            direita - 85,
            GameConfig.WINDOW_HEIGHT - 95
        );

        // =====================================
        // BASE
        // =====================================

        if (
            astronauta.isProtegido()
        ) {

            desenharTextoCentralizadoEm(
                batch,
                "BASE LUNAR  //  O2 RECARREGANDO",
                1.05f,
                Color.GREEN,
                GameConfig.WINDOW_WIDTH / 2f,
                54
            );
        }

        // =====================================
        // ALERTA O2
        // =====================================

        if (
            oxigenio <= 25f
                && !astronauta.isProtegido()
        ) {

            desenharTextoCentralizado(
                batch,
                "!!! OXIGENIO CRITICO !!!",
                1.25f,
                Color.RED,
                GameConfig.WINDOW_HEIGHT - 120
            );
        }

        batch.end();
    }

    // =====================================================
    // PAINEL
    // =====================================================

    private void desenharPainel(
        float x,
        float y,
        float largura,
        float altura
    ) {

        // sombra
        shapeRenderer.setColor(
            0,
            0,
            0,
            0.38f
        );

        shapeRenderer.rect(
            x + 4,
            y - 4,
            largura,
            altura
        );

        // corpo
        shapeRenderer.setColor(
            0.025f,
            0.055f,
            0.085f,
            0.90f
        );

        shapeRenderer.rect(
            x,
            y,
            largura,
            altura
        );

        // borda superior suave
        shapeRenderer.setColor(
            0.1f,
            0.22f,
            0.32f,
            0.9f
        );

        shapeRenderer.rect(
            x,
            y + altura - 1,
            largura,
            1
        );
    }

    // =====================================================
    // BARRA
    // =====================================================

    private void desenharBarra(
        float x,
        float y,
        float largura,
        float altura,
        float percentual,
        Color cor
    ) {

        percentual =
            Math.max(
                0,
                Math.min(
                    1,
                    percentual
                )
            );

        // fundo
        shapeRenderer.setColor(
            0.05f,
            0.07f,
            0.09f,
            1f
        );

        shapeRenderer.rect(
            x,
            y,
            largura,
            altura
        );

        // preenchimento
        shapeRenderer.setColor(
            cor
        );

        shapeRenderer.rect(
            x + 2,
            y + 2,
            (largura - 4) * percentual,
            altura - 4
        );

        // brilho
        if (percentual > 0) {

            shapeRenderer.setColor(
                cor.r,
                cor.g,
                cor.b,
                0.25f
            );

            shapeRenderer.rect(
                x + 2,
                y + altura - 5,
                (largura - 4) * percentual,
                3
            );
        }
    }

    // =====================================================
    // CORES
    // =====================================================

    private Color getCorOxigenio(
        float valor
    ) {

        if (valor <= 25f) {
            return Color.RED;
        }

        if (valor <= 50f) {
            return Color.ORANGE;
        }

        return Color.CYAN;
    }

    private Color getCorEnergia(
        float valor
    ) {

        if (valor <= 20f) {
            return Color.RED;
        }

        if (valor <= 45f) {
            return Color.ORANGE;
        }

        return Color.GREEN;
    }

    // =====================================================
    // TEXTO CENTRAL
    // =====================================================

    private void desenharTextoCentralizado(
        SpriteBatch batch,
        String texto,
        float escala,
        Color cor,
        float y
    ) {

        font.getData().setScale(
            escala
        );

        font.setColor(
            cor
        );

        layout.setText(
            font,
            texto
        );

        float x =
            (
                GameConfig.WINDOW_WIDTH
                    - layout.width
            )
                / 2f;

        font.draw(
            batch,
            layout,
            x,
            y
        );
    }

    private void desenharTextoCentralizadoEm(
        SpriteBatch batch,
        String texto,
        float escala,
        Color cor,
        float centroX,
        float y
    ) {

        font.getData().setScale(
            escala
        );

        font.setColor(
            cor
        );

        layout.setText(
            font,
            texto
        );

        font.draw(
            batch,
            layout,
            centroX
                - layout.width / 2f,
            y
        );
    }

    // =====================================================
    // RESIZE
    // =====================================================

    public void resize(
        int width,
        int height
    ) {

        viewport.update(
            width,
            height,
            true
        );
    }

    // =====================================================
    // DISPOSE
    // =====================================================

    @Override
    public void dispose() {

        shapeRenderer.dispose();

        /*
         * Não fazemos dispose da fonte,
         * porque ela pertence ao AssetManager.
         */
    }
}

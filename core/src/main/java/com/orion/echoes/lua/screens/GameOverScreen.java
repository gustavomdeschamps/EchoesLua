package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;

public class GameOverScreen implements Screen {

    private final EchoesLua game;
    private final float tempoSobrevivido;

    private OrthographicCamera camera;
    private Viewport viewport;

    private SpriteBatch batch;
    private BitmapFont font;

    private ShapeRenderer shapes;
    private GlyphLayout layout;

    private float tempoAnimacao = 0f;

    public GameOverScreen(
        EchoesLua game,
        float tempoSobrevivido
    ) {

        this.game = game;

        this.tempoSobrevivido =
            tempoSobrevivido;
    }

    @Override
    public void show() {

        Gdx.input.setInputProcessor(null);

        game.getSounds()
            .tocarGameOver();

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

        batch =
            game.getBatch();

        font =
            game.getAssets().font;

        shapes =
            new ShapeRenderer();

        layout =
            new GlyphLayout();
    }

    @Override
    public void render(float delta) {

        tempoAnimacao += delta;

        Gdx.gl.glClearColor(
            0.025f,
            0.004f,
            0.008f,
            1f
        );

        Gdx.gl.glClear(
            GL20.GL_COLOR_BUFFER_BIT
        );

        renderPainel();

        batch.setProjectionMatrix(
            camera.combined
        );

        batch.begin();

        centralizar(
            "SISTEMA DE SUPORTE VITAL",
            1f,
            Color.RED,
            570
        );

        centralizar(
            "MISSAO FALHOU",
            2.8f,
            Color.RED,
            500
        );

        centralizar(
            "OXIGENIO ESGOTADO",
            1.4f,
            Color.WHITE,
            420
        );

        centralizar(
            String.format(
                "TEMPO DE SOBREVIVENCIA   %.1f s",
                tempoSobrevivido
            ),
            1.1f,
            Color.LIGHT_GRAY,
            355
        );

        centralizar(
            "[ ENTER ]   TENTAR NOVAMENTE",
            1.2f,
            Color.GREEN,
            255
        );

        centralizar(
            "[ M ]   MENU PRINCIPAL",
            1.05f,
            Color.SKY,
            205
        );

        centralizar(
            "[ ESC ]   ENCERRAR",
            0.95f,
            Color.GRAY,
            155
        );

        batch.end();

        if (
            Gdx.input.isKeyJustPressed(
                Input.Keys.ENTER
            )
        ) {

            game.getSounds()
                .tocarInicio();

            game.setScreen(
                new LunarScreen(
                    game,
                    game.getBatch(),
                    game.getAssets()
                )
            );
        }

        if (
            Gdx.input.isKeyJustPressed(
                Input.Keys.M
            )
        ) {

            game.setScreen(
                new MenuScreen(game)
            );
        }

        if (
            Gdx.input.isKeyJustPressed(
                Input.Keys.ESCAPE
            )
        ) {

            Gdx.app.exit();
        }
    }

    private void renderPainel() {

        shapes.setProjectionMatrix(
            camera.combined
        );

        Gdx.gl.glEnable(
            GL20.GL_BLEND
        );

        Gdx.gl.glBlendFunc(
            GL20.GL_SRC_ALPHA,
            GL20.GL_ONE_MINUS_SRC_ALPHA
        );

        shapes.begin(
            ShapeRenderer.ShapeType.Filled
        );

        float w = 760f;
        float h = 470f;

        float x =
            (
                GameConfig.WINDOW_WIDTH
                    - w
            ) / 2f;

        float y =
            (
                GameConfig.WINDOW_HEIGHT
                    - h
            ) / 2f;

        // Sombra
        shapes.setColor(
            0f,
            0f,
            0f,
            0.6f
        );

        shapes.rect(
            x + 8,
            y - 8,
            w,
            h
        );

        // Painel
        shapes.setColor(
            0.07f,
            0.008f,
            0.015f,
            0.98f
        );

        shapes.rect(
            x,
            y,
            w,
            h
        );

        // Borda
        shapes.setColor(
            0.7f,
            0.02f,
            0.03f,
            1f
        );

        shapes.rect(
            x,
            y + h - 4,
            w,
            4
        );

        shapes.rect(
            x,
            y,
            w,
            4
        );

        shapes.rect(
            x,
            y,
            3,
            h
        );

        shapes.rect(
            x + w - 3,
            y,
            3,
            h
        );

        // Pulso vermelho
        float pulso =
            0.3f
                + 0.25f
                * Math.abs(
                (float) Math.sin(
                    tempoAnimacao * 4f
                )
            );

        shapes.setColor(
            1f,
            0.02f,
            0.02f,
            pulso
        );

        shapes.rect(
            x + 170,
            y + h - 27,
            w - 340,
            3
        );

        // Detalhes inferiores
        shapes.setColor(
            0.45f,
            0.02f,
            0.03f,
            0.8f
        );

        shapes.rect(
            x + 70,
            y + 80,
            160,
            2
        );

        shapes.rect(
            x + w - 230,
            y + 80,
            160,
            2
        );

        shapes.end();

        Gdx.gl.glDisable(
            GL20.GL_BLEND
        );
    }

    private void centralizar(
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

        font.draw(
            batch,
            layout,
            (
                GameConfig.WINDOW_WIDTH
                    - layout.width
            ) / 2f,
            y
        );
    }

    @Override
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

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {

        if (shapes != null) {
            shapes.dispose();
        }
    }
}

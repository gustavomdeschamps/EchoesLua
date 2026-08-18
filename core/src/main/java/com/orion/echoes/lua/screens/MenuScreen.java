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

public class MenuScreen implements Screen {

    private final EchoesLua game;

    private OrthographicCamera camera;
    private Viewport viewport;

    private SpriteBatch batch;
    private BitmapFont font;

    private ShapeRenderer shapes;
    private GlyphLayout layout;

    private float tempo = 0f;

    public MenuScreen(EchoesLua game) {
        this.game = game;
    }

    @Override
    public void show() {

        Gdx.input.setInputProcessor(null);

        camera = new OrthographicCamera();

        viewport = new FitViewport(
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

        batch = game.getBatch();
        font = game.getAssets().font;

        shapes = new ShapeRenderer();
        layout = new GlyphLayout();
    }

    @Override
    public void render(float delta) {

        tempo += delta;

        Gdx.gl.glClearColor(
            0.005f,
            0.012f,
            0.025f,
            1f
        );

        Gdx.gl.glClear(
            GL20.GL_COLOR_BUFFER_BIT
        );

        renderFundo();
        renderTextos();

        // ==========================================
        // ENTER - COMEÇAR
        // ==========================================

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

        // ==========================================
        // ESC - SAIR
        // ==========================================

        if (
            Gdx.input.isKeyJustPressed(
                Input.Keys.ESCAPE
            )
        ) {

            Gdx.app.exit();
        }
    }

    // =====================================================
    // FUNDO
    // =====================================================

    private void renderFundo() {

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

        // Fundo superior
        shapes.setColor(
            0.012f,
            0.065f,
            0.11f,
            0.95f
        );

        shapes.rect(
            0,
            575,
            GameConfig.WINDOW_WIDTH,
            145
        );

        // Linha neon
        shapes.setColor(
            0.05f,
            0.72f,
            1f,
            1f
        );

        shapes.rect(
            0,
            572,
            GameConfig.WINDOW_WIDTH,
            3
        );

        // ==========================================
        // PAINEL CENTRAL
        // ==========================================

        float painelW = 760f;
        float painelH = 355f;

        float painelX =
            (
                GameConfig.WINDOW_WIDTH
                    - painelW
            ) / 2f;

        float painelY = 145f;

        // Sombra
        shapes.setColor(
            0f,
            0f,
            0f,
            0.55f
        );

        shapes.rect(
            painelX + 8,
            painelY - 8,
            painelW,
            painelH
        );

        // Painel
        shapes.setColor(
            0.018f,
            0.04f,
            0.07f,
            0.98f
        );

        shapes.rect(
            painelX,
            painelY,
            painelW,
            painelH
        );

        // Bordas
        shapes.setColor(
            0.04f,
            0.4f,
            0.7f,
            0.9f
        );

        shapes.rect(
            painelX,
            painelY,
            painelW,
            2
        );

        shapes.rect(
            painelX,
            painelY + painelH - 2,
            painelW,
            2
        );

        shapes.rect(
            painelX,
            painelY,
            2,
            painelH
        );

        shapes.rect(
            painelX + painelW - 2,
            painelY,
            2,
            painelH
        );

        // Cantos sci-fi
        shapes.setColor(
            0.08f,
            0.8f,
            1f,
            1f
        );

        shapes.rect(
            painelX,
            painelY + painelH - 5,
            90,
            5
        );

        shapes.rect(
            painelX + painelW - 90,
            painelY + painelH - 5,
            90,
            5
        );

        shapes.rect(
            painelX,
            painelY,
            90,
            5
        );

        shapes.rect(
            painelX + painelW - 90,
            painelY,
            90,
            5
        );

        // Linha interna
        shapes.setColor(
            0.08f,
            0.25f,
            0.35f,
            0.8f
        );

        shapes.rect(
            painelX + 80,
            painelY + 115,
            painelW - 160,
            1
        );

        shapes.end();

        Gdx.gl.glDisable(
            GL20.GL_BLEND
        );
    }

    // =====================================================
    // TEXTOS
    // =====================================================

    private void renderTextos() {

        batch.setProjectionMatrix(
            camera.combined
        );

        batch.begin();

        centralizar(
            "E C H O E S",
            3.1f,
            Color.CYAN,
            665
        );

        centralizar(
            "FASE LUNAR",
            1.35f,
            Color.WHITE,
            615
        );

        centralizar(
            "PROTOCOLO DE SOBREVIVENCIA",
            1.05f,
            Color.SKY,
            470
        );

        centralizar(
            "MISSÃO",
            1.2f,
            Color.CYAN,
            420
        );

        centralizar(
            "Sobreviva por 60 segundos na Lua.",
            1.25f,
            Color.WHITE,
            380
        );

        centralizar(
            "Colete oxigenio, comida e gelo.",
            1f,
            Color.LIGHT_GRAY,
            345
        );

        centralizar(
            "Use a base lunar para recuperar O2 e processar gelo.",
            1f,
            Color.LIGHT_GRAY,
            315
        );

        centralizar(
            "WASD / SETAS     MOVIMENTO",
            0.95f,
            Color.SKY,
            255
        );

        centralizar(
            "E     PROCESSAR GELO NA BASE",
            0.95f,
            Color.SKY,
            225
        );

        centralizar(
            "ESC     PAUSAR",
            0.95f,
            Color.SKY,
            195
        );

        if (
            ((int) (tempo * 2.5f)) % 2 == 0
        ) {

            centralizar(
                "[ ENTER ]   INICIAR MISSAO",
                1.3f,
                Color.GREEN,
                95
            );
        }

        batch.end();
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

        float x =
            (
                GameConfig.WINDOW_WIDTH
                    - layout.width
            ) / 2f;

        font.draw(
            batch,
            layout,
            x,
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

package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.BaseLunar;
import com.orion.echoes.lua.entities.Item;
import com.orion.echoes.lua.entities.Obstacle;
import com.orion.echoes.lua.entities.Wall;
import com.orion.echoes.lua.input.GameInputProcessor;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.ParticleManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.physics.PhysicsWorld;

public class LunarScreen implements Screen {

    private static final float TEMPO_PARA_VENCER = 60f;

    private final EchoesLua game;
    private final SpriteBatch batch;
    private final AssetManager assets;

    private OrthographicCamera camera;
    private Viewport viewport;

    private OrthographicCamera pauseCamera;
    private Viewport pauseViewport;

    private ShapeRenderer shapeRenderer;
    private GlyphLayout pauseLayout;

    private PhysicsWorld physicsWorld;
    private GameInputProcessor input;

    private Astronauta astronauta;
    private BaseLunar baseLunar;

    private Array<Item> itens;
    private Array<Obstacle> obstacles;
    private Array<Wall> walls;

    private Hud hud;

    private ParticleManager particleManager;
    private SoundManager sounds;

    private boolean pausado = false;
    private boolean gameOver = false;
    private boolean vitoria = false;

    private boolean oxigenioCriticoAtivado = false;
    private boolean estavaNaBase = false;

    private float tempoPoeira = 0f;
    private float tempoPasso = 0f;

    public LunarScreen(
        EchoesLua game,
        SpriteBatch batch,
        AssetManager assets
    ) {

        this.game = game;
        this.batch = batch;
        this.assets = assets;
    }

    // =====================================================
    // SHOW
    // =====================================================

    @Override
    public void show() {

        criarCamera();
        criarCameraPause();

        physicsWorld =
            new PhysicsWorld();

        input =
            new GameInputProcessor();

        Gdx.input.setInputProcessor(
            input
        );

        particleManager =
            new ParticleManager();

        sounds =
            game.getSounds();

        astronauta =
            new Astronauta(
                GameConfig.PLAYER_START_X,
                GameConfig.PLAYER_START_Y,
                assets,
                physicsWorld
            );

        astronauta.setSpeed(
            GameConfig.PLAYER_SPEED
        );

        baseLunar =
            new BaseLunar(
                GameConfig.BASE_X,
                GameConfig.BASE_Y,
                GameConfig.BASE_WIDTH,
                GameConfig.BASE_HEIGHT,
                assets,
                physicsWorld
            );

        criarWalls();
        criarObstaculos();
        criarItens();

        hud =
            new Hud(
                assets
            );

        shapeRenderer =
            new ShapeRenderer();

        pauseLayout =
            new GlyphLayout();
    }

    // =====================================================
    // CAMERAS
    // =====================================================

    private void criarCamera() {

        camera =
            new OrthographicCamera();

        viewport =
            new FitViewport(
                GameConfig.WINDOW_WIDTH,
                GameConfig.WINDOW_HEIGHT,
                camera
            );

        camera.position.set(
            GameConfig.PLAYER_START_X,
            GameConfig.PLAYER_START_Y,
            0
        );

        camera.update();
    }

    private void criarCameraPause() {

        pauseCamera =
            new OrthographicCamera();

        pauseViewport =
            new FitViewport(
                GameConfig.WINDOW_WIDTH,
                GameConfig.WINDOW_HEIGHT,
                pauseCamera
            );

        pauseCamera.position.set(
            GameConfig.WINDOW_WIDTH / 2f,
            GameConfig.WINDOW_HEIGHT / 2f,
            0
        );

        pauseCamera.update();
    }

    // =====================================================
    // PAREDES
    // =====================================================

    private void criarWalls() {

        walls =
            new Array<>();

        float e =
            35f;

        walls.add(
            new Wall(
                -e,
                0,
                e,
                GameConfig.WORLD_HEIGHT,
                physicsWorld
            )
        );

        walls.add(
            new Wall(
                GameConfig.WORLD_WIDTH,
                0,
                e,
                GameConfig.WORLD_HEIGHT,
                physicsWorld
            )
        );

        walls.add(
            new Wall(
                0,
                -e,
                GameConfig.WORLD_WIDTH,
                e,
                physicsWorld
            )
        );

        walls.add(
            new Wall(
                0,
                GameConfig.WORLD_HEIGHT,
                GameConfig.WORLD_WIDTH,
                e,
                physicsWorld
            )
        );
    }

    // =====================================================
    // OBSTACULOS
    // =====================================================

    private void criarObstaculos() {

        obstacles =
            new Array<>();

        addObstacle(430, 220, 90, 90);
        addObstacle(650, 420, 110, 110);
        addObstacle(820, 160, 95, 95);

        addObstacle(390, 760, 100, 100);
        addObstacle(620, 1100, 115, 115);
        addObstacle(1050, 1060, 125, 105);

        addObstacle(1210, 280, 110, 100);
        addObstacle(1370, 480, 105, 105);
        addObstacle(1450, 870, 125, 115);

        addObstacle(1640, 650, 100, 100);
        addObstacle(1770, 250, 90, 90);
        addObstacle(1830, 1040, 120, 120);

        addObstacle(2020, 720, 95, 95);
        addObstacle(250, 1210, 85, 85);
    }

    private void addObstacle(
        float x,
        float y,
        float w,
        float h
    ) {

        obstacles.add(
            new Obstacle(
                x,
                y,
                w,
                h,
                assets.obstacleTexture,
                physicsWorld
            )
        );
    }

    // =====================================================
    // ITENS
    // =====================================================

    private void criarItens() {

        itens =
            new Array<>();

        // OXIGENIO
        adicionarItem(
            260,
            260,
            Item.TipoItem.OXIGENIO
        );

        adicionarItem(
            520,
            630,
            Item.TipoItem.OXIGENIO
        );

        adicionarItem(
            800,
            850,
            Item.TipoItem.OXIGENIO
        );

        adicionarItem(
            1090,
            430,
            Item.TipoItem.OXIGENIO
        );

        adicionarItem(
            1500,
            1120,
            Item.TipoItem.OXIGENIO
        );

        adicionarItem(
            1910,
            390,
            Item.TipoItem.OXIGENIO
        );

        // COMIDA
        adicionarItem(
            310,
            980,
            Item.TipoItem.COMIDA
        );

        adicionarItem(
            700,
            760,
            Item.TipoItem.COMIDA
        );

        adicionarItem(
            980,
            1190,
            Item.TipoItem.COMIDA
        );

        adicionarItem(
            1360,
            260,
            Item.TipoItem.COMIDA
        );

        adicionarItem(
            1720,
            840,
            Item.TipoItem.COMIDA
        );

        adicionarItem(
            2010,
            1100,
            Item.TipoItem.COMIDA
        );

        // GELO
        adicionarItem(
            560,
            240,
            Item.TipoItem.GELO
        );

        adicionarItem(
            760,
            1080,
            Item.TipoItem.GELO
        );

        adicionarItem(
            1180,
            780,
            Item.TipoItem.GELO
        );

        adicionarItem(
            1510,
            420,
            Item.TipoItem.GELO
        );

        adicionarItem(
            1770,
            1180,
            Item.TipoItem.GELO
        );

        adicionarItem(
            2050,
            560,
            Item.TipoItem.GELO
        );
    }

    private void adicionarItem(
        float x,
        float y,
        Item.TipoItem tipo
    ) {

        itens.add(
            new Item(
                x,
                y,
                GameConfig.ITEM_SIZE,
                GameConfig.ITEM_SIZE,
                tipo,
                assets,
                physicsWorld
            )
        );
    }

    // =====================================================
    // UPDATE
    // =====================================================

    private void update(float delta) {

        if (
            pausado
                || gameOver
                || vitoria
        ) {

            return;
        }

        delta =
            Math.min(
                delta,
                1f / 30f
            );

        // ==========================================
        // MOVIMENTO
        // ==========================================

        Vector2 direction =
            input.getDirection();

        astronauta.move(
            direction.x,
            direction.y,
            delta
        );

        if (!input.isMoving()) {

            astronauta
                .getBody()
                .setLinearVelocity(
                    0,
                    0
                );
        }

        // ==========================================
        // FISICA
        // ==========================================

        physicsWorld.update(
            delta
        );

        // ==========================================
        // PLAYER
        // ==========================================

        astronauta.update(
            delta
        );

        // ==========================================
        // ITENS FLUTUANTES
        // ==========================================

        for (Item item : itens) {

            item.update(
                delta
            );
        }

        // ==========================================
        // BASE
        // ==========================================

        baseLunar.update(
            delta
        );

        atualizarBase();

        // ==========================================
        // COLETAS
        // ==========================================

        atualizarItens();

        // ==========================================
        // RECARGA DA BASE
        // ==========================================

        if (
            baseLunar
                .isAstronautaDentro()
        ) {

            baseLunar
                .recarregarOxigenio(
                    astronauta,
                    delta
                );
        }

        atualizarInteracao();

        // ==========================================
        // EFEITOS
        // ==========================================

        atualizarParticulas(
            delta
        );

        atualizarSomPassos(
            delta
        );

        verificarOxigenio();

        particleManager.update(
            delta
        );

        atualizarCamera();

        verificarGameOver();

        verificarVitoria();
    }

    // =====================================================
    // BASE
    // =====================================================

    private void atualizarBase() {

        boolean dentro =
            astronauta
                .getBounds()
                .overlaps(
                    baseLunar
                        .getBounds()
                );

        if (dentro) {

            baseLunar.entrar(
                astronauta
            );

            if (!estavaNaBase) {

                estavaNaBase =
                    true;

                sounds
                    .tocarBaseRecarregando();
            }

        } else {

            baseLunar.sair(
                astronauta
            );

            estavaNaBase =
                false;
        }
    }

    // =====================================================
    // COLETAS
    // =====================================================

    private void atualizarItens() {

        for (
            Item item
            : itens
        ) {

            if (
                item.isColetado()
            ) {

                continue;
            }

            if (
                !astronauta
                    .getBounds()
                    .overlaps(
                        item.getBounds()
                    )
            ) {

                continue;
            }

            float x =
                item.getCenterX();

            float y =
                item.getCenterY();

            Item.TipoItem tipo =
                item.getTipo();

            item.coletar(
                astronauta
            );

            particleManager
                .criarEfeitoColeta(
                    x,
                    y
                );

            // Som específico
            switch (tipo) {

                case OXIGENIO:

                    sounds
                        .tocarOxigenio();

                    break;

                case COMIDA:

                    sounds
                        .tocarComida();

                    break;

                case GELO:

                    sounds
                        .tocarGelo();

                    break;
            }
        }
    }

    // =====================================================
    // PROCESSAR GELO
    // =====================================================

    private void atualizarInteracao() {

        if (
            !input
                .consumeInteractPressed()
        ) {

            return;
        }

        if (
            !baseLunar
                .isAstronautaDentro()
        ) {

            sounds
                .tocarSemGelo();

            return;
        }

        boolean processou =
            baseLunar
                .processarGelo(
                    astronauta
                );

        if (processou) {

            sounds
                .tocarProcessarGelo();

            particleManager
                .criarProcessamento(
                    baseLunar
                        .getPosition()
                        .x
                        + GameConfig.BASE_WIDTH / 2f,

                    baseLunar
                        .getPosition()
                        .y
                        + GameConfig.BASE_HEIGHT / 2f
                );

        } else {

            sounds
                .tocarSemGelo();
        }
    }

    // =====================================================
    // PASSOS
    // =====================================================

    private void atualizarSomPassos(
        float delta
    ) {

        if (!astronauta.isMoving()) {

            tempoPasso = 0f;

            return;
        }

        tempoPasso += delta;

        if (
            tempoPasso >= 0.48f
        ) {

            tempoPasso = 0f;

            sounds
                .tocarPassoLunar();
        }
    }

    // =====================================================
    // PARTICULAS
    // =====================================================

    private void atualizarParticulas(
        float delta
    ) {

        if (!astronauta.isMoving()) {

            tempoPoeira = 0f;

            return;
        }

        tempoPoeira += delta;

        if (
            tempoPoeira >= 0.20f
        ) {

            tempoPoeira = 0f;

            particleManager
                .criarPoeiraLunar(
                    astronauta
                        .getPosition()
                        .x
                        + GameConfig.PLAYER_WIDTH / 2f,

                    astronauta
                        .getPosition()
                        .y
                );
        }
    }

    // =====================================================
    // OXIGENIO CRITICO
    // =====================================================

    private void verificarOxigenio() {

        if (
            astronauta.getOxigenio()
                <= 25f
                &&
                !oxigenioCriticoAtivado
        ) {

            oxigenioCriticoAtivado =
                true;

            sounds
                .tocarAlertaOxigenio();

            particleManager
                .criarAlertaOxigenio(
                    astronauta
                        .getPosition()
                        .x
                        + GameConfig.PLAYER_WIDTH / 2f,

                    astronauta
                        .getPosition()
                        .y
                        + GameConfig.PLAYER_HEIGHT
                );
        }

        if (
            astronauta.getOxigenio()
                > 25f
        ) {

            oxigenioCriticoAtivado =
                false;
        }
    }

    // =====================================================
    // CAMERA
    // =====================================================

    private void atualizarCamera() {

        float targetX =
            astronauta
                .getPosition()
                .x
                + GameConfig.PLAYER_WIDTH / 2f;

        float targetY =
            astronauta
                .getPosition()
                .y
                + GameConfig.PLAYER_HEIGHT / 2f;

        float halfWidth =
            viewport
                .getWorldWidth()
                / 2f;

        float halfHeight =
            viewport
                .getWorldHeight()
                / 2f;

        targetX =
            MathUtils.clamp(
                targetX,
                halfWidth,
                GameConfig.WORLD_WIDTH
                    - halfWidth
            );

        targetY =
            MathUtils.clamp(
                targetY,
                halfHeight,
                GameConfig.WORLD_HEIGHT
                    - halfHeight
            );

        camera.position.x =
            MathUtils.lerp(
                camera.position.x,
                targetX,
                0.12f
            );

        camera.position.y =
            MathUtils.lerp(
                camera.position.y,
                targetY,
                0.12f
            );

        camera.update();
    }

    // =====================================================
    // GAME OVER
    // =====================================================

    private void verificarGameOver() {

        if (!astronauta.isMorto()) {
            return;
        }

        gameOver =
            true;

        astronauta
            .getBody()
            .setLinearVelocity(
                0,
                0
            );

        game.setScreen(
            new GameOverScreen(
                game,
                astronauta.getTempoVivo()
            )
        );
    }

    // =====================================================
    // VITORIA
    // =====================================================

    private void verificarVitoria() {

        if (
            astronauta.getTempoVivo()
                < TEMPO_PARA_VENCER
        ) {

            return;
        }

        vitoria =
            true;

        astronauta
            .getBody()
            .setLinearVelocity(
                0,
                0
            );

        game.setScreen(
            new VictoryScreen(
                game,
                astronauta.getTempoVivo()
            )
        );
    }

    // =====================================================
    // PAUSE
    // =====================================================

    private void verificarPause() {

        if (
            Gdx.input
                .isKeyJustPressed(
                    Input.Keys.ESCAPE
                )
        ) {

            pausado =
                !pausado;

            astronauta
                .getBody()
                .setLinearVelocity(
                    0,
                    0
                );

            if (pausado) {

                sounds
                    .tocarPause();

            } else {

                sounds
                    .tocarUnpause();
            }
        }

        if (!pausado) {
            return;
        }

        // ENTER CONTINUA
        if (
            Gdx.input
                .isKeyJustPressed(
                    Input.Keys.ENTER
                )
        ) {

            pausado =
                false;

            sounds
                .tocarUnpause();
        }

        // M VOLTA PARA MENU
        if (
            Gdx.input
                .isKeyJustPressed(
                    Input.Keys.M
                )
        ) {

            game.setScreen(
                new MenuScreen(
                    game
                )
            );
        }
    }

    // =====================================================
    // RENDER
    // =====================================================

    @Override
    public void render(
        float delta
    ) {

        verificarPause();

        update(
            delta
        );

        Gdx.gl.glClearColor(
            0.02f,
            0.025f,
            0.04f,
            1f
        );

        Gdx.gl.glClear(
            GL20.GL_COLOR_BUFFER_BIT
        );

        batch.setProjectionMatrix(
            camera.combined
        );

        batch.begin();

        // BACKGROUND
        batch.draw(
            assets.backgroundLuaTexture,
            0,
            0,
            GameConfig.WORLD_WIDTH,
            GameConfig.WORLD_HEIGHT
        );

        // BASE
        baseLunar.render(
            batch
        );

        // ITENS
        for (
            Item item
            : itens
        ) {

            item.render(
                batch
            );
        }

        // OBSTACULOS
        for (
            Obstacle obstacle
            : obstacles
        ) {

            obstacle.render(
                batch
            );
        }

        // PLAYER
        astronauta.render(
            batch
        );

        // PARTICULAS
        particleManager.render(
            batch
        );

        batch.end();

        // HUD NÃO APARECE DURANTE PAUSE
        if (!pausado) {

            hud.render(
                batch,
                astronauta
            );
        }

        if (pausado) {

            renderPause();
        }
    }

    // =====================================================
    // PAUSE VISUAL
    // =====================================================

    private void renderPause() {

        shapeRenderer.setProjectionMatrix(
            pauseCamera.combined
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

        // Fundo escuro
        shapeRenderer.setColor(
            0f,
            0f,
            0f,
            0.82f
        );

        shapeRenderer.rect(
            0,
            0,
            GameConfig.WINDOW_WIDTH,
            GameConfig.WINDOW_HEIGHT
        );

        // ==========================================
        // PAINEL
        // ==========================================

        float painelW =
            620f;

        float painelH =
            390f;

        float painelX =
            (
                GameConfig.WINDOW_WIDTH
                    - painelW
            ) / 2f;

        float painelY =
            (
                GameConfig.WINDOW_HEIGHT
                    - painelH
            ) / 2f;

        // Sombra
        shapeRenderer.setColor(
            0f,
            0f,
            0f,
            0.65f
        );

        shapeRenderer.rect(
            painelX + 8,
            painelY - 8,
            painelW,
            painelH
        );

        // Painel principal
        shapeRenderer.setColor(
            0.012f,
            0.045f,
            0.075f,
            0.98f
        );

        shapeRenderer.rect(
            painelX,
            painelY,
            painelW,
            painelH
        );

        // Bordas neon
        shapeRenderer.setColor(
            0.04f,
            0.7f,
            1f,
            1f
        );

        shapeRenderer.rect(
            painelX,
            painelY + painelH - 4,
            painelW,
            4
        );

        shapeRenderer.rect(
            painelX,
            painelY,
            painelW,
            3
        );

        shapeRenderer.rect(
            painelX,
            painelY,
            3,
            painelH
        );

        shapeRenderer.rect(
            painelX + painelW - 3,
            painelY,
            3,
            painelH
        );

        // Cantos
        shapeRenderer.setColor(
            0.1f,
            0.9f,
            1f,
            1f
        );

        shapeRenderer.rect(
            painelX,
            painelY + painelH - 7,
            90,
            7
        );

        shapeRenderer.rect(
            painelX + painelW - 90,
            painelY + painelH - 7,
            90,
            7
        );

        // Separador
        shapeRenderer.setColor(
            0.05f,
            0.25f,
            0.35f,
            1f
        );

        shapeRenderer.rect(
            painelX + 90,
            painelY + 245,
            painelW - 180,
            1
        );

        shapeRenderer.end();

        Gdx.gl.glDisable(
            GL20.GL_BLEND
        );

        // ==========================================
        // TEXTOS
        // ==========================================

        batch.setProjectionMatrix(
            pauseCamera.combined
        );

        batch.begin();

        desenharPauseCentralizado(
            "SISTEMA EM ESPERA",
            0.95f,
            Color.SKY,
            505
        );

        desenharPauseCentralizado(
            "JOGO PAUSADO",
            2.3f,
            Color.CYAN,
            460
        );

        desenharPauseCentralizado(
            String.format(
                "TEMPO   %.1f / 60 s",
                astronauta.getTempoVivo()
            ),
            1.05f,
            Color.LIGHT_GRAY,
            390
        );

        desenharPauseCentralizado(
            "[ ESC ] ou [ ENTER ]   CONTINUAR",
            1.15f,
            Color.GREEN,
            320
        );

        desenharPauseCentralizado(
            "[ M ]   MENU PRINCIPAL",
            1.05f,
            Color.SKY,
            265
        );

        desenharPauseCentralizado(
            "O tempo e o oxigenio ficam congelados.",
            0.9f,
            Color.GRAY,
            195
        );

        batch.end();
    }

    private void desenharPauseCentralizado(
        String texto,
        float escala,
        Color cor,
        float y
    ) {

        assets.font
            .getData()
            .setScale(
                escala
            );

        assets.font.setColor(
            cor
        );

        pauseLayout.setText(
            assets.font,
            texto
        );

        float x =
            (
                GameConfig.WINDOW_WIDTH
                    - pauseLayout.width
            ) / 2f;

        assets.font.draw(
            batch,
            pauseLayout,
            x,
            y
        );
    }

    // =====================================================
    // RESIZE
    // =====================================================

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

        pauseViewport.update(
            width,
            height,
            true
        );

        hud.resize(
            width,
            height
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

        if (hud != null) {
            hud.dispose();
        }

        if (particleManager != null) {
            particleManager.dispose();
        }

        if (physicsWorld != null) {
            physicsWorld.dispose();
        }

        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}

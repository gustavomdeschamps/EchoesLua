package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.AppSettings;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.Pickup;
import com.orion.echoes.lua.entities.TitanEnemy;
import com.orion.echoes.lua.entities.TitanBoss;
import com.orion.echoes.lua.entities.TitanPortal;
import com.orion.echoes.lua.entities.TitanProjectile;
import com.orion.echoes.lua.entities.Wall;
import com.orion.echoes.lua.input.GameInputProcessor;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.render.HitboxDebugRenderer;
import com.orion.echoes.lua.systems.CameraDirector;
import com.orion.echoes.lua.systems.JuiceSystem;
import com.orion.echoes.lua.render.PauseOverlay;
import com.orion.echoes.lua.render.TerrainRenderer;
import com.orion.echoes.lua.save.GameSaveData;
import com.orion.echoes.lua.save.LunarCheckpoint;
import com.orion.echoes.lua.save.SaveManager;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.systems.CombatTarget;
import com.orion.echoes.lua.systems.TitanCombatSystem;
import com.orion.echoes.lua.ui.HudLabel;
import com.orion.echoes.lua.ui.PauseSettingsModel;
import com.orion.echoes.lua.ui.UiTheme;
import com.orion.echoes.lua.ui.WorkshopInterior;
import com.orion.echoes.lua.world.NpcSpawnSelector;

/** Terceira fase real: superfície de metano de Titã, combate e retorno. */
public final class TitanScreen implements Screen {
    private static final float WORLD_W = 2600f;
    private static final float WORLD_H = 1700f;
    private static final Color AMBER = Color.valueOf("D78A36");
    /** Formacoes de gelo: x, y, largura, altura e qual pedra do atlas. */
    private static final float[][] FORMACOES = {
        {320f, 700f, 190f, 150f, 0f}, {880f, 340f, 160f, 128f, 2f},
        {1240f, 1080f, 210f, 165f, 1f}, {1760f, 560f, 175f, 140f, 3f},
        {2100f, 1470f, 200f, 158f, 4f}, {620f, 1240f, 165f, 132f, 5f},
        {1520f, 1420f, 185f, 148f, 0f}, {2380f, 820f, 170f, 136f, 2f},
        {980f, 820f, 150f, 120f, 3f}, {1900f, 1080f, 195f, 155f, 1f},
        {1050f, 480f, 155f, 124f, 4f}, {2100f, 380f, 180f, 144f, 5f}
    };
    private final EchoesLua game;
    private final CampaignState campaign;
    private final Array<TitanEnemy> enemies = new Array<>();
    private final Array<Pickup> suprimentos = new Array<>();
    private final Array<TitanProjectile> projectiles = new Array<>();
    private final Array<Rectangle> collisionObstacles = new Array<>();
    /** Edifício principal; a porta é acessível sem atravessar a base sólida. */
    private final Rectangle refinaria = new Rectangle(430f, 150f, 360f, 310f);
    private final Rectangle stationDoor = new Rectangle(574f, 150f, 76f, 100f);
    private final Rectangle titanKeyPickup = new Rectangle();
    private final Vector2 shotOrigin = new Vector2();
    private final Vector2 shotEnd = new Vector2();
    private final Vector2 mouseWorld = new Vector2();
    private final Vector2 cameraVelocity = new Vector2();
    private AssetManager assets;
    private SpriteBatch batch;
    private PhysicsWorld physics;
    private Astronauta player;
    private TitanPortal returnPortal;
    private TitanPortal calistoPortal;
    private TitanBoss boss;
    private boolean vitoriaRegistrada;
    private boolean titanKeyDropped;
    private TitanCombatSystem combat;
    private GameInputProcessor input;
    private OrthographicCamera camera;
    private OrthographicCamera uiCamera;
    private Viewport viewport;
    private Viewport uiViewport;
    private NinePatch panel;
    private PauseOverlay pauseOverlay;
    private WorkshopInterior workshop;
    private com.orion.echoes.lua.systems.NpcConversation conversation;
    private com.orion.echoes.lua.managers.ParticleManager particles;
    private float footstepTimer;
    private boolean flashlightOn = true;
    private HitboxDebugRenderer hitboxDebug;
    private String message = "A atmosfera abafa o sinal. Explore com cautela.";
    private float messageTimer = 4f;
    private float missionTime;
    private float shotTimer;
    /** Pulso discreto nas luzes da estação após o processamento. */
    private static final float REFINERY_ACTIVITY_TIME = 1.1f;
    private float refineryActivity;
    private boolean paused;
    private com.orion.echoes.lua.ui.ExpeditionOverlay expedition;
    private boolean changingScreen;
    private boolean portalTraveling;

    public TitanScreen(EchoesLua game, CampaignState campaign) {
        this.game = game;
        this.campaign = campaign == null ? game.getCampaign() : campaign;
    }

    @Override public void show() {
        game.useTargetCursor();
        assets = game.getAssets();
        batch = game.getBatch();
        game.getSounds().setVacuum(false);
        game.getSounds().applySettings(game.getSettings());
        game.getSounds().tocarMusicaTita();
        particles = new com.orion.echoes.lua.managers.ParticleManager(assets);
        panel = assets.uiPanelPatch();
        workshop = new WorkshopInterior(batch, assets);
        statusHud = new com.orion.echoes.lua.render.GameplayStatusHud(assets);
        pauseOverlay = new PauseOverlay(batch, assets);
        pauseOverlay.setSettings(construirOpcoesDaPausa());
        hitboxDebug = new HitboxDebugRenderer(batch, assets);
        physics = new PhysicsWorld();
        input = new GameInputProcessor();
        Gdx.input.setInputProcessor(input);
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
        juice.setShakeEnabled(game.getSettings().isShakeEnabled());
        juice.setReduceMotion(game.getSettings().isReduceMotion());
        pauseOverlay.setReduceMotion(game.getSettings().isReduceMotion());
        cameraDirector = new CameraDirector(camera, viewport, juice, WORLD_W, WORLD_H);
        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, uiCamera);
        uiCamera.position.set(640f, 360f, 0f);
        uiCamera.update();
        new Wall(-24f, 0f, 24f, WORLD_H, physics);
        new Wall(WORLD_W, 0f, 24f, WORLD_H, physics);
        new Wall(0f, -24f, WORLD_W, 24f, physics);
        new Wall(0f, WORLD_H, WORLD_W, 24f, physics);
        for (float[] formation : FORMACOES) {
            Rectangle draw = com.orion.echoes.lua.render.SpriteFit.fit(
                titanGroundRock((int)formation[4]), formation[0], formation[1],
                formation[2], formation[3], new Rectangle());
            Rectangle footprint = new Rectangle(
                draw.x + draw.width * .16f,
                draw.y + draw.height * .11f,
                draw.width * .68f,
                draw.height * .28f);
            collisionObstacles.add(footprint);
            new Wall(footprint.x, footprint.y, footprint.width, footprint.height, physics);
        }
        Rectangle refineryFootprint = new Rectangle(refinaria.x + 45f, refinaria.y + 112f,
            refinaria.width - 90f, 36f);
        collisionObstacles.add(refineryFootprint);
        new Wall(refineryFootprint.x, refineryFootprint.y,
            refineryFootprint.width, refineryFootprint.height, physics);
        player = new Astronauta(260f, 260f, assets, physics);
        expedition = new com.orion.echoes.lua.ui.ExpeditionOverlay(game, player);
        player.setSurfaceProfile(Astronauta.SurfaceProfile.MARS);
        player.setWeaponEquipped(campaign.hasWeapon());
        player.setMunicao(campaign.getAmmo());

        // Exigência da prova: a Screen carrega o personagem via fromSaveData no show().
        GameSaveData saved = new SaveManager().load();
        if (saved != null && !saved.campanhaConcluida
            && "MUNDO".equals(saved.cena)
            && saved.semente == campaign.getSeed()
            && CampaignState.phaseFromToken(saved.fase) == CampaignState.Phase.TITAN) {
            player.fromSaveData(saved);
            player.setMunicao(saved.municao);
        } else {
            GameSaveData arrival = player.toSaveData();
            LunarCheckpoint.applyCampaign(arrival, campaign);
            player.fromSaveData(arrival);
        }
        Vector2 liraSpawn = NpcSpawnSelector.choose(campaign.getSeed(), "lira", new float[][] {
            {850f, 250f}, {855f, 490f}, {810f, 565f}, {355f, 540f}
        });
        conversation = new com.orion.echoes.lua.systems.NpcConversation(
            new com.orion.echoes.lua.entities.Npc(liraSpawn.x, liraSpawn.y, "Lira",
                Color.WHITE, assets, com.orion.echoes.lua.entities.Npc.Visual.LIRA),
            new String[] {
                "Os lagos daqui são de metano líquido. Nossa equipe perdeu contato quando o Soberano ocupou o vale a nordeste.",
                "O Soberano está perto? Preciso de um caminho e de munição antes de enfrentá-lo.",
                "Refine gelo aqui para fabricar células. Quando o solo brilhar sob o Soberano, saia da área; o portal oeste ainda leva a Marte."
            }, batch, assets);
        combat = new TitanCombatSystem(campaign);
        combat.setMunicao(player.getMunicao());
        returnPortal = new TitanPortal(150f, 150f, assets);
        returnPortal.setUnlocked(true);
        returnPortal.setReversed(true);
        enemies.add(new TitanEnemy(760f, 610f, assets));
        enemies.add(new TitanEnemy(1370f, 980f, assets));
        enemies.add(new TitanEnemy(2050f, 520f, assets));
        enemies.add(new TitanEnemy(1810f, 1370f, assets));
        // O chefe guarda o fundo do mapa: o jogador o encontra depois dos comuns.
        boss = new TitanBoss(2150f, 1150f, assets);
        // O anel mede 190 x 220 e oscila levemente: mantenha toda a arte dentro do mapa.
        calistoPortal = new TitanPortal(2330f,1200f,assets);
        calistoPortal.setUnlocked(campaign.getInventario().tem(com.orion.echoes.lua.systems.Inventario.CHAVE_TITA));
        suprimentos.add(new Pickup(840f,510f,Pickup.Kind.COMIDA,assets));
        suprimentos.add(new Pickup(1460f,630f,Pickup.Kind.COMIDA,assets));
        boolean bossDefeated = campaign.isBossDefeated(CampaignState.Phase.TITAN)
            || campaign.getInventario().tem(com.orion.echoes.lua.systems.Inventario.CHAVE_TITA);
        if (bossDefeated) {
            boss.receiveDamage(100000f); boss.setAtivo(false); vitoriaRegistrada = true;
            if (!campaign.getInventario().tem(com.orion.echoes.lua.systems.Inventario.CHAVE_TITA))
                prepareTitanKey();
        }
        // Oxigenio e gelo espalhados: a fase longa precisa de folego e de
        // insumo para nao travar o jogador sem municao.
        for (float[] ponto : new float[][] {{540f, 900f}, {1180f, 480f}, {1620f, 1260f},
                {2280f, 980f}, {860f, 1420f}, {1980f, 300f}}) {
            suprimentos.add(new Pickup(ponto[0], ponto[1], Pickup.Kind.OXIGENIO, assets));
        }
        for (float[] ponto : new float[][] {{700f, 1180f}, {1420f, 760f}, {2140f, 1440f},
                {1020f, 260f}, {2420f, 620f}}) {
            suprimentos.add(new Pickup(ponto[0], ponto[1], Pickup.Kind.GELO, assets));
        }
        campaign.setPhase(CampaignState.Phase.TITAN);
        campaign.setEntrouTita(true);
        missionTime = campaign.getMissionTime();
        camera.position.set(player.getPosition().x, player.getPosition().y, 0f);
        camera.update();
    }

    @Override public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);
        // O juice anda no relogio real; o gameplay recebe o delta com hit-stop.
        juice.update(delta);
        update(juice.gameplayDelta(delta));
        if (changingScreen) return;
        Gdx.gl.glClearColor(.09f, .045f, .018f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(.72f, .61f, .46f, 1f);
        TerrainRenderer.draw(batch, assets.titanBackgroundTexture, WORLD_W, WORLD_H);
        batch.setColor(Color.WHITE);
        desenharTerreno();
        returnPortal.render(batch);
        calistoPortal.render(batch);
        desenharRefinaria();
        for (Pickup suprimento : suprimentos) suprimento.render(batch);
        desenharAvisoDoChefe();
        for (TitanEnemy enemy : enemies) enemy.render(batch);
        renderEnemyHealth();
        for (TitanProjectile projectile : projectiles) projectile.render(batch);
        boss.render(batch);
        renderTitanKey(batch);
        renderPlayerShot();
        player.render(batch);
        conversation.renderWorld(batch, player);
        particles.render(batch);
        renderFlashlight();
        batch.end();
        renderHitboxes();
        renderDamage();
        renderHud();
        expedition.render();
        if (paused) renderPause();
        else {
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            conversation.renderUi();
            batch.end();
        }
        workshop.render();
    }

    /** Máscara em faixas: abre um círculo de visão sem framebuffer ou textura borrada. */
    private void renderFlashlight() {
        if (!flashlightOn) {
            batch.setColor(0f, 0f, 0f, .74f);
            batch.draw(assets.uiWhiteTexture, 0f, 0f, WORLD_W, WORLD_H);
            batch.setColor(Color.WHITE);
            return;
        }
        float cx = player.getPosition().x + GameConfig.PLAYER_WIDTH * .5f;
        float cy = player.getPosition().y + GameConfig.PLAYER_HEIGHT * .5f;
        // Três penumbras concêntricas dão queda de luz suave; uma máscara
        // binária única deixava um disco recortado e degraus enormes no solo.
        shadeOutside(cx, cy, 420f, .30f);
        shadeOutside(cx, cy, 350f, .22f);
        shadeOutside(cx, cy, 290f, .17f);
        batch.setColor(Color.WHITE);
    }

    private void shadeOutside(float cx, float cy, float radius, float alpha) {
        float bottom = Math.max(0f, cy - radius);
        float top = Math.min(WORLD_H, cy + radius);
        batch.setColor(0f, 0f, 0f, alpha);
        if (bottom > 0f) batch.draw(assets.uiWhiteTexture, 0f, 0f, WORLD_W, bottom);
        if (top < WORLD_H) batch.draw(assets.uiWhiteTexture, 0f, top, WORLD_W, WORLD_H - top);
        int bands = 192;
        float height = (top - bottom) / bands;
        for (int i = 0; i < bands; i++) {
            float y = bottom + i * height;
            float fromCenter = Math.max(Math.abs(y - cy), Math.abs(y + height - cy));
            float halfWidth = (float)Math.sqrt(Math.max(0f, radius * radius - fromCenter * fromCenter));
            float left = MathUtils.clamp(cx - halfWidth, 0f, WORLD_W);
            float right = MathUtils.clamp(cx + halfWidth, 0f, WORLD_W);
            if (left > 0f) batch.draw(assets.uiWhiteTexture, 0f, y, left, height);
            if (right < WORLD_W) batch.draw(assets.uiWhiteTexture, right, y, WORLD_W - right, height);
        }
    }

    private void update(float delta) {
        if (changingScreen) return;
        if (workshop != null && workshop.isOpen()) {
            workshop.handleInput(delta);
            player.getBody().setLinearVelocity(0f, 0f);
            input.discardActions();
            return;
        }
        if (!paused && expedition.handleInput()) { input.discardActions(); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) hitboxDebug.toggle();
        // Com a pausa aberta, o overlay tem prioridade: ESC pode estar fechando
        // o painel de opcoes em vez de despausar.
        if (paused && pauseOverlay.handlePauseKeys()) {
            if (pauseOverlay.consumeQuitRequested()) Gdx.app.exit();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || paused && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || pauseOverlay.consumeResumeRequested())) {
            paused = !paused;
            player.getBody().setLinearVelocity(0f, 0f);
            if (paused) pauseOverlay.open();
            return;
        }
        if (paused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.M) || pauseOverlay.consumeMenuRequested()) {
                changingScreen = true;
                game.setScreen(new MenuScreen(game));
                dispose();
            }
            return;
        }
        if (portalTraveling) {
            // A lanterna não altera o percurso nem bloqueia a travessia.
            player.getBody().setLinearVelocity(0f, 0f);
            returnPortal.update(delta);
            if (returnPortal.isTraversalComplete()) finishReturnToMars();
            return;
        }
        missionTime += delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            flashlightOn = !flashlightOn;
            feedback(flashlightOn ? "Lanterna do traje ligada." : "Lanterna do traje desligada.");
        }
        if (conversation.update(delta, player, campaign.isDialogoExplorador(), () -> {
            campaign.setDialogoExplorador(true);
            feedback(campaign.missaoAtual());
        })) {
            input.consumeInteractPressed();
            input.consumeAttackPressed();
            input.consumeDashPressed();
            return;
        }
        game.getSounds().setListener(camera.position.x, camera.position.y);
        game.getSounds().atualizarIntensidade(boss.isTelegraphing() ? 1f : .25f,
            MathUtils.clamp((35f - player.getOxigenio()) / 35f, 0f, 1f));
        particles.update(delta);
        calistoPortal.setUnlocked(campaign.getInventario().tem(com.orion.echoes.lua.systems.Inventario.CHAVE_TITA));
        calistoPortal.update(delta);
        combat.update(delta);
        messageTimer = Math.max(0f, messageTimer - delta);
        shotTimer = Math.max(0f, shotTimer - delta);
        refineryActivity = Math.max(0f, refineryActivity - delta);
        Vector2 direction = input.getDirection();
        boolean dashRequested = input.consumeDashPressed() || Gdx.input.isKeyJustPressed(Input.Keys.Q);
        if (dashRequested && player.tryDash(direction.x, direction.y)) {
            juice.trigger(JuiceSystem.Preset.DASH);
        }
        player.move(direction.x, direction.y, input.isRunning(), delta);
        footstepTimer -= delta;
        if (!direction.isZero() && footstepTimer <= 0f) {
            footstepTimer = input.isRunning() ? .24f : .38f;
            game.getSounds().tocarPassoTita();
        }
        float beforePhysicsX = player.getPosition().x;
        float beforePhysicsY = player.getPosition().y;
        physics.update(delta);
        player.update(delta);
        mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouseWorld);
        player.setAimTarget(mouseWorld.x, mouseWorld.y);
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            int before = player.getMunicao();
            player.setMunicao(campaign.getInventario().reload(before, GameConfig.AMMO_MAX));
            combat.setMunicao(player.getMunicao());
            feedback(player.getMunicao() > before ? "Rifle recarregado." : "Sem células fabricadas na reserva.");
        }
        if (input.consumeAttackPressed()) shoot();
        for (TitanEnemy enemy : enemies) {
            enemy.update(delta, player, WORLD_W, WORLD_H, physics.getSolidBounds());
            if (enemy.consumeShot()) spawnEnemyShot(enemy.centerX(), enemy.centerY(),
                enemy.shotDirectionX(), enemy.shotDirectionY(), 245f, 12f);
            if (enemy.canDamage(player)) {
                player.receberDano(11f, enemy.centerX(), enemy.centerY());
                juice.trigger(JuiceSystem.Preset.PLAYER_HURT);
                feedback("Predador de metano atingiu o traje.");
            }
        }
        atualizarChefe(delta);
        if (titanKeyDropped && titanKeyPickup.overlaps(player.getBounds())) collectTitanKey();
        if (changingScreen || physics == null) return;
        updateProjectiles(delta);
        returnPortal.update(delta);
        coletarSuprimentos(delta);
        if (input.consumeInteractPressed()) interagir();
        // interagir() pode trocar a Screen e descartar imediatamente o World.
        if (changingScreen || physics == null) return;
        if (input.consumeSavePressed()) saveTitan();
        if (input.consumeLoadPressed()) loadTitan();
        cameraTarget.set(player.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
            player.getPosition().y + GameConfig.PLAYER_HEIGHT / 2f);
        // A câmera usa deslocamento real, incluindo colisões. Evita consultar o
        // ponteiro nativo do Body depois que uma troca de tela descarta Box2D.
        cameraVelocity.set((player.getPosition().x - beforePhysicsX) / Math.max(delta, .0001f)
                / GameConfig.PPM,
            (player.getPosition().y - beforePhysicsY) / Math.max(delta, .0001f)
                / GameConfig.PPM);
        cameraDirector.update(cameraTarget, cameraVelocity,
            combateProximo(), delta);
        if (player.isMorto()) {
            campaign.setVitals(player.getOxigenio(), player.getEnergia());
            campaign.setAmmo(player.getMunicao());
            campaign.setResources(player.getGelo(), player.getAgua(), player.getCombustivel());
            campaign.setMissionTime(missionTime);
            changingScreen = true;
            game.setScreen(new GameOverScreen(game, missionTime));
            dispose();
        }
    }

    /**
     * O chefe ataca de verdade.
     *
     * Ele para, avisa por quase um segundo e desaba num impacto em area. O
     * dano sai no frame do impacto e alcanca quem ficou dentro do raio, nao
     * so quem encostou - por isso o aviso importa.
     */
    private void atualizarChefe(float delta) {
        if (boss == null) return;
        if (boss.isAlive() && !missoesTitaOk()) return;
        if (!boss.isAtivo()) {
            if (!vitoriaRegistrada) {
                vitoriaRegistrada = true;
                juice.trigger(JuiceSystem.Preset.BOSS_DEATH);
                campaign.setBossDefeated(CampaignState.Phase.TITAN, true);
                prepareTitanKey();
                feedback("O Soberano caiu. Recolha a Chave de Titã junto aos destroços.");
                saveTitan();
            }
            return;
        }
        boss.update(delta, player, WORLD_W, WORLD_H, physics.getSolidBounds());
        // Rugido no telegraph: o aviso do chefe era so visual ate aqui.
        if (boss.consumeRoar()) {
            game.getSounds().tocarBoss("rugido", boss.centerX(), boss.centerY());
        }
        if (boss.consumeDeath()) {
            game.getSounds().tocarBoss("morte", boss.centerX(), boss.centerY());
            particles.criarMorteInimigo(boss.centerX(), boss.centerY());
        }
        if (boss.consumeSlam() && boss.slamHits(player)) {
            player.receberDano(GameConfig.BOSS_DAMAGE, boss.centerX(), boss.centerY());
            particles.criarImpactoTraje(player.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
                player.getPosition().y + GameConfig.PLAYER_HEIGHT / 2f);
            juice.trigger(JuiceSystem.Preset.BOSS_SLAM);
            feedback("O impacto do chefe alcançou o traje.");
        }
        if (boss.consumeVolley()) spawnBossVolley(false);
        if (boss.consumeBurst()) spawnBossVolley(true);
    }

    /**
     * Relevo de Tita.
     *
     * O mapa era so o fundo esticado, sem nada para ler: sem referencia, o
     * jogador nao sabia onde estava nem para onde tinha ido. Formacoes de
     * gelo e as silhuetas do horizonte dao pontos fixos, e a posicao e fixa
     * de proposito - marco que muda a cada partida nao serve de marco.
     */
    private void desenharTerreno() {
        for (float[] pedra : FORMACOES) {
            // Frosted hydrocarbon rock, not the blue-white ice of Calisto.
            batch.setColor(.78f, .70f, .61f, 1f);
            com.orion.echoes.lua.render.SpriteFit.draw(batch, titanGroundRock((int) pedra[4]),
                pedra[0], pedra[1], pedra[2], pedra[3]);
        }
        // As silhuetas ficam no alto: sao horizonte, nao obstaculo.
        batch.setColor(.5f, .38f, .26f, .8f);
        com.orion.echoes.lua.render.SpriteFit.draw(batch, assets.titanFormationRegion(0), 380f, 1440f, 300f, 210f);
        com.orion.echoes.lua.render.SpriteFit.draw(batch, assets.titanFormationRegion(3), 1240f, 1520f, 260f, 190f);
        com.orion.echoes.lua.render.SpriteFit.draw(batch, assets.titanFormationRegion(5), 2080f, 1470f, 280f, 200f);
        batch.setColor(Color.WHITE);
    }

    /** Estação principal de Titã, com a entrada alinhada à área de interação. */
    private void desenharRefinaria() {
        float glow = refineryActivity > 0f ? .04f * MathUtils.sin(refineryActivity * 23f) : 0f;
        // O metal recebe o mesmo banho de metano/fuligem que o terreno de Titã.
        batch.setColor(.79f + glow, .69f + glow, .55f + glow, 1f);
        com.orion.echoes.lua.render.SpriteFit.draw(batch, assets.titanMainStationRegion,
            refinaria.x, refinaria.y, refinaria.width, refinaria.height);
        batch.setColor(Color.WHITE);
    }

    /** Marca no chao o raio do golpe enquanto o chefe prepara. */
    private void desenharAvisoDoChefe() {
        if (boss == null || !boss.isTelegraphing()) return;
        float progresso = boss.getTelegraphProgress();
        float raio = (boss.isBurstTelegraphing() ? 300f : GameConfig.BOSS_SLAM_RADIUS) * progresso;
        batch.setColor(boss.isVolleyTelegraphing() ? .1f : 1f,
            boss.isVolleyTelegraphing() ? .85f : .45f, .45f, .18f + progresso * .30f);
        batch.draw(assets.uiWhiteTexture, boss.centerX() - raio,
            boss.centerY() - 70f - raio * .34f, raio * 2f, raio * .68f);
        batch.setColor(Color.WHITE);
    }

    /** Vida sempre visível: o jogador sabe quanto falta antes de gastar munição. */
    private void renderEnemyHealth() {
        for (TitanEnemy enemy : enemies) {
            if (!enemy.isAtivo()) continue;
            float x = enemy.centerX() - 34f;
            float y = enemy.getPosition().y + 112f;
            batch.setColor(.025f, .035f, .055f, .9f);
            batch.draw(assets.uiWhiteTexture, x - 2f, y - 2f, 72f, 9f);
            batch.setColor(enemy.isTelegraphing() ? UiTheme.RED : UiTheme.CYAN);
            batch.draw(assets.uiWhiteTexture, x, y, 68f * enemy.getHealthRatio(), 5f);
        }
        batch.setColor(Color.WHITE);
    }

    private void shoot() {
        // Mesma origem do rifle desenhado, igual às outras duas fases.
        player.muzzle(shotOrigin);
        float dirX = MathUtils.cosDeg(player.getAimAngle());
        float dirY = MathUtils.sinDeg(player.getAimAngle());
        CombatTarget target = null;
        float closest = combat.getAlcance();
        for (TitanEnemy enemy : enemies) {
            if (!enemy.isAlive()) continue;
            float along = alinhamento(enemy.centerX(), enemy.centerY(), dirX, dirY, closest, 52f);
            if (along > 0f) {
                target = enemy;
                closest = along;
            }
        }
        // O chefe e alvo como qualquer outro, so que com corpo bem maior.
        if (boss != null && boss.isAlive() && missoesTitaOk()) {
            float along = alinhamento(boss.centerX(), boss.centerY(), dirX, dirY, closest, 105f);
            if (along > 0f) {
                target = boss;
                closest = along;
            }
        }
        shotEnd.set(target == null ? shotOrigin.x + dirX * combat.getAlcance() : target.centerX(),
            target == null ? shotOrigin.y + dirY * combat.getAlcance() : target.centerY());
        combat.setMunicao(player.getMunicao());
        boolean fired = combat.tentarTiro(shotOrigin, target, campaign.hasWeapon());
        player.setMunicao(combat.getMunicao());
        if (fired) {
            game.getSounds().tocarDisparo();
            particles.criarMuzzleFlash(shotOrigin.x, shotOrigin.y, player.getAimAngle());
            if (target != null) {
                game.getSounds().tocarImpacto(target.centerX(), target.centerY());
                boolean abatido = !target.isAlive();
                if (abatido) {
                    particles.criarMorteInimigo(target.centerX(), target.centerY());
                    game.getSounds().tocarMorteInimigo(target.centerX(), target.centerY());
                } else {
                    particles.criarImpactoTiro(target.centerX(), target.centerY());
                }
                // A morte do chefe tem preset proprio, disparado em atualizarChefe.
                if (target != boss) {
                    juice.trigger(abatido ? JuiceSystem.Preset.ENEMY_KILL
                        : JuiceSystem.Preset.SHOT_HIT);
                } else {
                    juice.trigger(JuiceSystem.Preset.SHOT_HIT);
                }
            }
            player.triggerShot();
            shotTimer = .14f;
            feedback(target == null ? "Disparo perdido na névoa de metano."
                : !target.isAlive() ? "Predador neutralizado." : "Impacto confirmado.");
        } else if (player.getMunicao() <= 0) feedback("Sem munição.");

    }

    private void spawnEnemyShot(float x, float y, float dx, float dy, float speed, float damage) {
        projectiles.add(new TitanProjectile(x, y, dx, dy, speed, damage,
            assets.energyFxFrame(1, 0), assets.uiWhiteTexture));
    }

    private void spawnBossVolley(boolean radial) {
        game.getSounds().tocarBoss("ataque", boss.centerX(), boss.centerY());
        if (radial) {
            for (int i = 0; i < 12; i++) {
                float angle = i * 30f + missionTime * 35f;
                spawnEnemyShot(boss.centerX(), boss.centerY(), MathUtils.cosDeg(angle),
                    MathUtils.sinDeg(angle), 285f, 15f);
            }
            feedback("Rajada radial: encontre uma abertura!");
            return;
        }
        float targetAngle = MathUtils.atan2(player.getPosition().y + 38f - boss.centerY(),
            player.getPosition().x + 27f - boss.centerX()) * MathUtils.radiansToDegrees;
        for (float offset : new float[] {-18f, -9f, 0f, 9f, 18f}) {
            float angle = targetAngle + offset;
            spawnEnemyShot(boss.centerX(), boss.centerY(), MathUtils.cosDeg(angle),
                MathUtils.sinDeg(angle), 330f, 14f);
        }
        feedback("O Soberano lançou uma salva de cristais!");
    }

    private void updateProjectiles(float delta) {
        for (int i = projectiles.size - 1; i >= 0; i--) {
            TitanProjectile projectile = projectiles.get(i);
            projectile.update(delta);
            if (projectile.collideWith(collisionObstacles)) {
                // A formação absorve o disparo; serve de cobertura na arena.
            } else if (projectile.hits(player)) {
                player.receberDano(projectile.getDamage(), projectile.x(), projectile.y());
                player.applyFreeze(4f);
                juice.trigger(JuiceSystem.Preset.PLAYER_HURT);
                feedback("Cristal de metano: mobilidade reduzida por 4 s.");
            }
            if (!projectile.isActive()) projectiles.removeIndex(i);
        }
    }

    /**
     * Ha combate perto o bastante para fechar o zoom.
     *
     * O chefe conta com um raio maior: ele so cabe na tela se a camera comeca
     * a fechar antes de o jogador estar no alcance do golpe.
     */
    private boolean combateProximo() {
        float cx = player.getPosition().x + GameConfig.PLAYER_WIDTH / 2f;
        float cy = player.getPosition().y + GameConfig.PLAYER_HEIGHT / 2f;
        if (boss != null && boss.isAlive()
            && Vector2.dst(cx, cy, boss.centerX(), boss.centerY())
                <= GameConfig.BOSS_CHASE_RADIUS) {
            return true;
        }
        for (TitanEnemy enemy : enemies) {
            if (!enemy.isAlive()) continue;
            if (Vector2.dst(cx, cy, enemy.centerX(), enemy.centerY())
                <= GameConfig.CAMERA_COMBAT_RADIUS) return true;
        }
        return false;
    }

    /** Vinheta de dano: mesma leitura das outras duas fases. */
    private void renderDamage() {
        float alpha = juice.getDamageFlashAlpha();
        if (alpha <= 0f) return;
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.setColor(1f, .62f, .3f, alpha * .85f);
        batch.draw(assets.uiDamageVignetteTexture, 0f, 0f,
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void renderPlayerShot() {
        if (shotTimer <= 0f) return;
        float alpha = shotTimer / .14f;
        float dx = shotEnd.x - shotOrigin.x, dy = shotEnd.y - shotOrigin.y;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length <= .01f) return;
        float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        batch.setColor(.3f, .95f, 1f, alpha);
        batch.draw(assets.uiWhiteTexture, shotOrigin.x, shotOrigin.y - 2f,
            0f, 2f, length, 4f, 1f, 1f, angle);
        batch.draw(assets.energyFxFrame(1, 0), shotEnd.x - 12f, shotEnd.y - 12f, 24f, 24f);
        batch.setColor(Color.WHITE);
    }

    /**
     * Projecao do alvo sobre a linha de tiro.
     *
     * Devolve a distancia ao longo da mira quando o alvo esta a frente,
     * dentro do alcance e proximo do eixo; caso contrario, zero.
     */
    private float alinhamento(float alvoX, float alvoY, float dirX, float dirY,
                              float limite, float raioDaMira) {
        float dx = alvoX - shotOrigin.x;
        float dy = alvoY - shotOrigin.y;
        float along = dx * dirX + dy * dirY;
        float perpendicular = Math.abs(dx * dirY - dy * dirX);
        return along > 0f && along < limite && perpendicular < raioDaMira ? along : 0f;
    }

    /**
     * Fim da campanha.
     *
     * Derrubar o chefe encerra o jogo em vitoria - ate agora VictoryScreen
     * existia no projeto e nunca era instanciada, entao nao havia como vencer.
     */
    private void prepareTitanKey() {
        titanKeyDropped = true;
        titanKeyPickup.set(boss.centerX() - 42f, boss.centerY() - 30f, 84f, 84f);
    }

    /** Usa as variantes de basalto/metano; as formações geladas ficam em Calisto. */
    private com.badlogic.gdx.graphics.g2d.TextureRegion titanGroundRock(int index) {
        int variant = switch (Math.floorMod(index, 3)) {
            case 0 -> 0;
            case 1 -> 3;
            default -> 5;
        };
        return assets.titanFormationRegion(variant);
    }

    private void renderTitanKey(SpriteBatch batch) {
        if (!titanKeyDropped) return;
        float bob = MathUtils.sin(missionTime * 3.2f) * 7f;
        batch.setColor(Color.WHITE);
        com.orion.echoes.lua.render.AtlasRegionRenderer.draw(batch,assets.bossKeyFrame(2),
            titanKeyPickup.x,titanKeyPickup.y+bob,titanKeyPickup.width,titanKeyPickup.height);
        batch.setColor(Color.WHITE);
    }

    private void collectTitanKey() {
        titanKeyDropped = false;
        campaign.setVitals(player.getOxigenio(), player.getEnergia());
        campaign.setAmmo(player.getMunicao());
        campaign.setMissionTime(missionTime);
        campaign.getInventario().add(com.orion.echoes.lua.systems.Inventario.CHAVE_TITA);
        campaign.getInventario().melhorarArmadura();
        feedback("Chave de Titã conquistada. Portal de Calisto ONLINE junto à cratera do Soberano.");
        saveTitan();
    }

    private boolean missoesTitaOk() {
        if (!campaign.isDialogoExplorador()) return false;
        for (TitanEnemy enemy : enemies) if (enemy.isAlive()) return false;
        return true;
    }

    private void saveTitan() {
        campaign.setResources(player.getGelo(), player.getAgua(), player.getCombustivel());
        campaign.setVitals(player.getOxigenio(), player.getEnergia());
        campaign.setAmmo(player.getMunicao());
        campaign.setMissionTime(missionTime);
        campaign.setPhase(CampaignState.Phase.TITAN);
        GameSaveData data = player.toSaveData();
        LunarCheckpoint.applyCampaign(data, campaign);
        new SaveManager().save(data);
        feedback("Exploração de Titã salva.");
    }

    private void loadTitan() {
        GameSaveData data = new SaveManager().load();
        if (data == null) {
            feedback("Nenhum checkpoint encontrado.");
            return;
        }
        if (CampaignState.phaseFromToken(data.fase) != CampaignState.Phase.TITAN) {
            feedback("O checkpoint é de outra fase. Carregue pelo menu.");
            return;
        }
        player.fromSaveData(data);
        campaign.getInventario().restaurar(data.inventario,data.comidaGuardada,data.nivelArma,data.nivelArmadura);
        player.setMunicao(data.municao);
        combat.setMunicao(data.municao);
        missionTime = data.tempoVivo;
        feedback("Checkpoint de Titã carregado.");
    }

    private void returnToMars() {
        if (portalTraveling) return;
        portalTraveling = true;
        returnPortal.beginTraversal(true);
        game.getSounds().tocarPortal();
        feedback("O portal reverte o fluxo e fixa Marte como destino.");
    }

    private void finishReturnToMars() {
        campaign.setResources(player.getGelo(), player.getAgua(), player.getCombustivel());
        campaign.setVitals(player.getOxigenio(), player.getEnergia());
        campaign.setAmmo(player.getMunicao());
        campaign.setMissionTime(missionTime);
        campaign.setPhase(CampaignState.Phase.MARS);
        GameSaveData data = player.toSaveData();
        // Ao continuar depois da viagem de volta, a campanha deve reabrir em Marte.
        data.posX = 2440f;
        data.posY = 1430f;
        LunarCheckpoint.applyCampaign(data, campaign);
        new SaveManager().save(data);
        changingScreen = true;
        game.setScreen(new MarsScreen(game, campaign));
        dispose();
    }

    /** Coleta de oxigenio e gelo na superficie. */
    private void coletarSuprimentos(float delta) {
        for (Pickup suprimento : suprimentos) {
            suprimento.update(delta);
            if (!suprimento.coletar(player)) continue;
            game.getSounds().tocarColetaEspacial(suprimento.centerX(), suprimento.centerY());
            particles.criarEfeitoColeta(suprimento.centerX(), suprimento.centerY());
            juice.trigger(JuiceSystem.Preset.COLLECT);
            feedback(suprimento.getKind() == Pickup.Kind.OXIGENIO
                ? "Cilindro de oxigênio  •  O2 restaurado"
                : suprimento.getKind() == Pickup.Kind.COMIDA ? "Ração guardada na mochila."
                : "Gelo de metano recolhido  •  processe na estação");
        }
    }

    /** E: refina na refinaria, ou volta a Marte no portal. */
    private void interagir() {
        if (calistoPortal.isPlayerNear(player)) {
            if (!campaign.getInventario().tem(com.orion.echoes.lua.systems.Inventario.CHAVE_TITA)) {
                feedback("BLOQUEADO — conquiste a Chave de Titã ao derrotar o Soberano."); return;
            }
            saveTitan();
            changingScreen = true;
            game.setScreen(new CallistoScreen(game, campaign));
            dispose();
            return;
        }
        if (stationDoor.overlaps(player.getBounds())) { openWorkshop(); return; }
        if (returnPortal.isPlayerNear(player)) returnToMars();
    }

    /**
     * Gelo vira municao.
     *
     * E o que impede a fase de virar beco sem saida: sem municao o chefe e
     * invencivel, e a refinaria da sempre um caminho de volta.
     */
    private void refinar() {
        if (campaign.getInventario().getReserveAmmo() >= 240) {
            feedback("Reserva de munição cheia. O gelo foi preservado.");
            return;
        }
        if (!player.removerGelo()) {
            feedback("Sem gelo para refinar. Recolha gelo pelo mapa.");
            return;
        }
        int celulas = campaign.getInventario().addReserveAmmo(GameConfig.AMMO_PER_ICE);
        player.recuperarOxigenio(GameConfig.OXYGEN_ITEM_VALUE * .5f);
        combat.setMunicao(player.getMunicao());
        refineryActivity = REFINERY_ACTIVITY_TIME;
        juice.trigger(JuiceSystem.Preset.CRAFT);
        feedback(celulas > 0
            ? "Gelo refinado: +" + celulas + " células na reserva. R recarrega."
            : "Reserva de munição cheia.");
    }

    private void feedback(String value) { message = value; messageTimer = 3f; }

    /*
     * Game feel da fase final.
     *
     * Tita era a unica fase sem JuiceSystem nem CameraDirector: a camera
     * grudava na posicao do jogador sem suavizacao nem lookahead, nao havia
     * hit-stop nem shake, e a opcao "Tremor de camera" das configuracoes nao
     * tinha efeito nenhum aqui - justamente na fase do chefe.
     */
    private final JuiceSystem juice = new JuiceSystem();
    private CameraDirector cameraDirector;
    private final Vector2 cameraTarget = new Vector2();

    /* HUD sem alocacao por quadro: cor do painel e textos reaproveitados. */
    private final Color panelColor = new Color();
    private final HudLabel hudOxygen = new HudLabel("", "%");
    private final HudLabel hudEnergy = new HudLabel("", "%");
    private final HudLabel hudAmmo = new HudLabel("", "");
    private final HudLabel hudIce = new HudLabel("", "");

    private com.orion.echoes.lua.render.GameplayStatusHud statusHud;
    private void renderHud() {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        statusHud.render(batch,player);
        drawHudPanel(24f, 630f, 620f, 74f, AMBER, .96f);
        drawHudPanel(660f, 630f, 596f, 74f, AMBER, .96f);
        if (messageTimer > 0f) drawHudPanel(400f, 112f, 480f, 68f, AMBER,
            Math.min(.94f, messageTimer * 2f));
        text("MISSÃO 03 · TITÃ", .53f, AMBER, 42f, 688f);
        text(flashlightOn ? "F  LANTERNA LIGADA" : "F  LANTERNA DESLIGADA",
            .40f, flashlightOn ? UiTheme.CYAN : UiTheme.RED, 445f, 688f);
        assets.font.getData().setScale(.72f);
        assets.font.setColor(UiTheme.TEXT);
        assets.font.draw(batch, campaign.missaoAtual(), 42f, 663f, 580f, Align.left, true);
        if (boss != null && boss.isAlive()) {
            text("SOBERANO DO METANO", .53f, AMBER, 678f, 688f);
            batch.setColor(Color.WHITE);
            batch.draw(assets.uiBarTrackTexture, 678f, 656f, 548f, 10f);
            batch.setColor(boss.isTelegraphing() ? UiTheme.RED : AMBER);
            batch.draw(assets.uiBarFillTexture, 678f, 656f, 548f * boss.getHealthRatio(), 10f);
            batch.setColor(Color.WHITE);
        } else {
            boolean hasKey = campaign.getInventario().tem(
                com.orion.echoes.lua.systems.Inventario.CHAVE_TITA);
            text(hasKey ? "PORTAL DE CALISTO" : "CHAVE DE TITÃ", .53f, AMBER, 678f, 688f);
            text(hasKey ? "A passagem está liberada." : "Recolha a chave do Soberano.",
                .60f, UiTheme.TEXT, 678f, 656f);
        }
        if (messageTimer > 0f) {
            assets.font.getData().setScale(.68f);
            assets.font.setColor(UiTheme.TEXT);
            assets.font.draw(batch, message, 422f, 157f, 436f, Align.center, true);
        }
        batch.end();
        assets.font.getData().setScale(1f);
        assets.font.setColor(Color.WHITE);
    }

    private void openWorkshop() {
        player.getBody().setLinearVelocity(0f, 0f);
        workshop.open("ESTAÇÃO PRINCIPAL DE TITÃ · INTERIOR", new WorkshopInterior.Actions() {
            @Override public String status() {
                return "GELO DE METANO  " + player.getGelo() + "   ·   MUNIÇÃO  "
                    + player.getMunicao() + "   ·   HOSTIS  " + inimigosRestantes();
            }
            @Override public void recharge() {
                player.recuperarOxigenio(100f); player.recuperarEnergia(100f);
                game.getSounds().tocarBaseRecarregando(); feedback("Traje estabilizado na estação.");
            }
            @Override public void craft() {
                feedback("Bancada ajustada para o frio. Rifle calibrado.");
            }
            @Override public void processIce() { refinar(); }
        });
    }

    private int inimigosRestantes() {
        int remaining = 0;
        for (TitanEnemy enemy : enemies) if (enemy.isAlive()) remaining++;
        return remaining + (boss != null && boss.isAlive() ? 1 : 0);
    }

    private void drawHudPanel(float x, float y, float width, float height,
                              Color accent, float alpha) {
        panelColor.set(1f, 1f, 1f, alpha);
        panel.setColor(panelColor);
        panel.draw(batch, x, y, width, height);
        panel.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);
    }

    private void renderHitboxes() {
        if (!hitboxDebug.begin(camera)) return;
        hitboxDebug.box(player.getBounds(), UiTheme.CYAN);
        hitboxDebug.box(returnPortal.getBounds(), UiTheme.GREEN);
        hitboxDebug.box(stationDoor, UiTheme.GREEN);
        for (Pickup pickup : suprimentos) hitboxDebug.box(pickup.getBounds(), UiTheme.AMBER);
        for (TitanEnemy enemy : enemies) {
            if (!enemy.isAtivo()) continue;
            hitboxDebug.box(enemy.getBounds(), UiTheme.RED);
            hitboxDebug.center(enemy.centerX(), enemy.centerY(), UiTheme.RED);
        }
        if (boss != null && boss.isAtivo()) {
            hitboxDebug.sprite(boss.centerX() - GameConfig.BOSS_SPRITE_SIZE / 2f,
                boss.getPosition().y
                    + GameConfig.BOSS_SPRITE_SIZE * GameConfig.BOSS_SPRITE_OFFSET_Y_RATIO,
                GameConfig.BOSS_SPRITE_SIZE, GameConfig.BOSS_SPRITE_SIZE);
            hitboxDebug.box(boss.getBounds(), UiTheme.RED);
            hitboxDebug.center(boss.centerX(), boss.centerY(), UiTheme.RED);
        }
        hitboxDebug.end();
    }

    private void renderPause() {
        pauseOverlay.render(AMBER, "ECHOES · TITÃ", campaign.missaoAtual(),
            "O soberano aguarda. Seus recursos estão congelados.",
            new String[] {"OXIGÊNIO", "ENERGIA", "MUNIÇÃO", "GELO", "CHEFE"},
            new String[] {
                String.format("%.0f%%", player.getOxigenio()),
                String.format("%.0f%%", player.getEnergia()),
                String.valueOf(player.getMunicao()),
                String.valueOf(player.getGelo()),
                boss != null && boss.isAtivo() ? String.format("%.0f%%", boss.getHealthRatio() * 100f) : "ABATIDO"
            });
    }

    private void text(String value, float scale, Color color, float x, float y) {
        assets.font.getData().setScale(scale);
        assets.font.setColor(color);
        assets.font.draw(batch, value, x, y);
    }

    @Override public void resize(int width, int height) {
        if (expedition != null) expedition.resize(width, height);
        viewport.update(width, height, false);
        uiViewport.update(width, height, true);
        if (pauseOverlay != null) pauseOverlay.resize(width, height);
        if (workshop != null) workshop.resize(width, height);
    }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    /**
     * Opcoes disponiveis sem sair da partida.
     *
     * E um subconjunto do menu de proposito: o que o jogador quer ajustar no
     * meio de uma missao e volume e acessibilidade. Escala de HUD, tela cheia
     * e controles continuam so no menu, onde ha espaco para explicar.
     */
    private PauseSettingsModel construirOpcoesDaPausa() {
        AppSettings settings = game.getSettings();
        return new PauseSettingsModel()
            .addSlider("Geral", new PauseSettingsModel.FloatAccessor() {
                @Override public float get() { return settings.getMasterVolume(); }
                @Override public void set(float value) {
                    settings.setMasterVolume(value);
                    game.aplicarPreferenciasDeAudio();
                }
            })
            .addSlider("Música", new PauseSettingsModel.FloatAccessor() {
                @Override public float get() { return settings.getMusicVolume(); }
                @Override public void set(float value) {
                    settings.setMusicVolume(value);
                    game.aplicarPreferenciasDeAudio();
                }
            })
            .addSlider("Efeitos", new PauseSettingsModel.FloatAccessor() {
                @Override public float get() { return settings.getSfxVolume(); }
                @Override public void set(float value) {
                    settings.setSfxVolume(value);
                    game.aplicarPreferenciasDeAudio();
                }
            })
            .addSlider("Ambiente", new PauseSettingsModel.FloatAccessor() {
                @Override public float get() { return settings.getAmbientVolume(); }
                @Override public void set(float value) {
                    settings.setAmbientVolume(value);
                    game.aplicarPreferenciasDeAudio();
                }
            })
            .addToggle("Tremor de câmera", new PauseSettingsModel.BoolAccessor() {
                @Override public boolean get() { return settings.isShakeEnabled(); }
                @Override public void set(boolean value) {
                    settings.setShakeEnabled(value);
                    aplicarAcessibilidade();
                }
            })
            .addToggle("Redução de movimento", new PauseSettingsModel.BoolAccessor() {
                @Override public boolean get() { return settings.isReduceMotion(); }
                @Override public void set(boolean value) {
                    settings.setReduceMotion(value);
                    aplicarAcessibilidade();
                }
            });
    }

    /** Reaplica as opcoes de acessibilidade sem esperar o proximo carregamento. */
    private void aplicarAcessibilidade() {
        juice.setShakeEnabled(game.getSettings().isShakeEnabled());
        juice.setReduceMotion(game.getSettings().isReduceMotion());
        pauseOverlay.setReduceMotion(game.getSettings().isReduceMotion());
    }

    @Override public void dispose() {
        if (expedition != null) expedition.dispose();
        if (particles != null) { particles.dispose(); particles = null; }
        if (physics != null) { physics.dispose(); physics = null; }
        if (pauseOverlay != null) pauseOverlay.dispose();
        if (workshop != null) workshop.dispose();
        // SpriteBatch e AssetManager pertencem ao EchoesLua e não são descartados aqui.
    }
}

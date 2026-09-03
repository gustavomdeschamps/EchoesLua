package com.orion.echoes.lua.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.MarsEnemy;
import com.orion.echoes.lua.entities.MarsObject;
import com.orion.echoes.lua.entities.Portal;
import com.orion.echoes.lua.entities.TitanPortal;
import com.orion.echoes.lua.entities.Wall;
import com.orion.echoes.lua.events.EventBus;
import com.orion.echoes.lua.events.EventType;
import com.orion.echoes.lua.input.GameInputProcessor;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.ParticleManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.render.HitboxDebugRenderer;
import com.orion.echoes.lua.save.GameSaveData;
import com.orion.echoes.lua.save.LunarCheckpoint;
import com.orion.echoes.lua.save.SaveManager;
import com.orion.echoes.lua.systems.CameraDirector;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.systems.JuiceSystem;
import com.orion.echoes.lua.systems.DialogueController;
import com.orion.echoes.lua.systems.TitanCombatSystem;
import com.orion.echoes.lua.ui.UiTheme;

/** Segunda missão jogável: restaura a base marciana e alcança a plataforma. */
public final class MarsScreen implements Screen {
    private static final float WORLD_W = 3000f;
    private static final float WORLD_H = 1900f;
    private static final Color MARS = Color.valueOf("E77A4E");
    private static final Color MARS_DARK = Color.valueOf("572A24");

    private final EchoesLua game;
    private final CampaignState campaign;
    private final Array<MarsObject> props = new Array<>();
    private final Array<MarsObject> rocks = new Array<>();
    private final Array<MarsObject> items = new Array<>();
    private final Array<MarsObject> stations = new Array<>();
    private final Array<MarsEnemy> enemies = new Array<>();
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 mouseWorld = new Vector2();
    private final Vector2 shotStart = new Vector2();
    private final Vector2 shotEnd = new Vector2();
    private final Vector2 cameraTarget = new Vector2();

    private AssetManager assets;
    private SpriteBatch batch;
    private NinePatch panelPatch;
    private NinePatch modalPatch;
    private PhysicsWorld physics;
    private ParticleManager particles;
    private SoundManager sounds;
    private Astronauta player;
    private GameInputProcessor input;
    private OrthographicCamera camera;
    private OrthographicCamera uiCamera;
    private Viewport viewport;
    private Viewport uiViewport;
    private MarsObject habitat;
    private Portal returnPortal;
    private TitanPortal titanPortal;
    private MarsObject landingPad;
    private float missionTime, reveal, dustTimer, stepTimer, shotTimer;
    private float messageTimer, extractionGlow;
    private JuiceSystem juice;
    private CameraDirector cameraDirector;
    private HitboxDebugRenderer hitboxDebug;
    private int minerals, activeStations, hostilesDefeated;
    private boolean changingScreen;
    private boolean paused;
    private final DialogueController titanDialogue = new DialogueController();
    private TitanCombatSystem titanCombat;
    private String message = "A tempestade apagou a colônia. Recupere os núcleos marcianos.";

    public MarsScreen(EchoesLua game, CampaignState campaign) {
        this.game = game;
        this.campaign = campaign == null ? game.getCampaign() : campaign;
    }

    @Override public void show() {
        assets = game.getAssets();
        batch = game.getBatch();
        panelPatch = assets.uiPanelPatch();
        modalPatch = assets.uiModalPatch();
        physics = new PhysicsWorld();
        particles = new ParticleManager(assets);
        sounds = SoundManager.getInstance();
        sounds.applySettings(game.getSettings());
        sounds.setVacuum(false);
        sounds.tocarMusicaMarte();
        input = new GameInputProcessor();
        Gdx.input.setInputProcessor(input);
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
        juice = new JuiceSystem();
        juice.setShakeEnabled(game.getSettings().isShakeEnabled());
        cameraDirector = new CameraDirector(camera, viewport, juice, WORLD_W, WORLD_H);
        hitboxDebug = new HitboxDebugRenderer(batch, assets);
        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, uiCamera);
        uiCamera.position.set(640f, 360f, 0f);
        uiCamera.update();
        new Wall(-24f, 0f, 24f, WORLD_H, physics);
        new Wall(WORLD_W, 0f, 24f, WORLD_H, physics);
        new Wall(0f, -24f, WORLD_W, 24f, physics);
        new Wall(0f, WORLD_H, WORLD_W, 24f, physics);
        player = new Astronauta(230f, 250f, assets, physics);
        player.setSurfaceProfile(Astronauta.SurfaceProfile.MARS);
        player.setVitals(Math.max(45f, campaign.getOxygen()), Math.max(38f, campaign.getEnergy()));
        player.setWeaponEquipped(true);
        player.setMunicao(campaign.getAmmo());
        titanCombat = new TitanCombatSystem(campaign);
        titanCombat.setMunicao(player.getMunicao());
        buildColony();
        restaurarCampanha();
        camera.position.set(640f, 360f, 0f);
        camera.update();
        EventBus.getInstance().publish(EventType.MARS_ENTERED);
    }

    private void buildColony() {
        habitat = prop(160f, 1020f, 330f, 275f, MarsObject.Kind.HABITAT);
        stations.add(prop(700f, 1420f, 225f, 190f, MarsObject.Kind.SOLAR_STATION));
        stations.add(prop(1550f, 1330f, 225f, 190f, MarsObject.Kind.OXYGEN_STATION));
        stations.add(prop(2440f, 1130f, 225f, 190f, MarsObject.Kind.COMMS_STATION));
        landingPad = prop(2570f, 1530f, 250f, 190f, MarsObject.Kind.LANDING_PAD);
        /*
         * O portal de volta nasce ao lado do ponto de chegada e fica sempre
         * aberto: voltar a Lua para reabastecer municao e oxigenio e uma
         * jogada valida, nao uma recompensa.
         */
        returnPortal = new Portal(360f, 300f, assets);
        returnPortal.setUnlocked(true);
        titanPortal = new TitanPortal(2520f, 1480f, assets);
        titanPortal.setUnlocked(campaign.portalLiberado());
        prop(2400f, 1230f, 120f, 140f, MarsObject.Kind.BEACON);
        float[][] data = {{530,760,130},{850,420,115},{1080,930,145},{1320,580,125},
            {1710,810,150},{1980,430,120},{2350,650,145},{2750,320,110},{610,1660,120},
            {1150,1610,105},{2600,900,130},{2250,1660,115}};
        for (float[] r : data) {
            MarsObject rock = new MarsObject(r[0], r[1], r[2] * 1.04f, r[2] * .86f,
                MarsObject.Kind.ROCK, assets, physics);
            props.add(rock);
            rocks.add(rock);
        }
        for (int i = 0; i < 4; i++) addRandomItem(MarsObject.Kind.MINERAL);
        addRandomItem(MarsObject.Kind.MEDKIT);
        addRandomItem(MarsObject.Kind.POWER_CELL);
        enemies.add(new MarsEnemy(920f, 1120f, true, assets, WORLD_W, WORLD_H));
        enemies.add(new MarsEnemy(1600f, 520f, false, assets, WORLD_W, WORLD_H));
        enemies.add(new MarsEnemy(2120f, 1250f, true, assets, WORLD_W, WORLD_H));
        enemies.add(new MarsEnemy(2420f, 420f, false, assets, WORLD_W, WORLD_H));
    }

    /**
     * Reabre Marte no ponto em que o jogador saiu.
     *
     * Sem isto, voltar pelo portal significaria reativar as mesmas estacoes e
     * recacar os mesmos hostis - o portal bidirecional so faz sentido se as
     * duas pontas lembrarem do que ja foi feito.
     */
    private void restaurarCampanha() {
        campaign.setPhase(CampaignState.Phase.MARS);
        campaign.markMarsVisited();
        minerals = campaign.getMinerals();
        hostilesDefeated = Math.min(campaign.getMarsHostilesDefeated(), enemies.size);
        for (int index = 0; index < hostilesDefeated; index++) enemies.get(index).setAtivo(false);
        int online = Math.min(campaign.getMarsStationsOnline(), stations.size);
        for (int index = 0; index < online; index++) stations.get(index).activate();
        activeStations = online;
    }

    /** Fotografa Marte antes de voltar pelo portal ou de salvar. */
    private void guardarCampanha() {
        campaign.setVitals(player.getOxigenio(), player.getEnergia());
        campaign.setAmmo(player.getMunicao());
        campaign.setMissionTime(missionTime);
        campaign.setMarsProgress(minerals, activeStations, hostilesDefeated, missionComplete());
    }

    private MarsObject prop(float x, float y, float w, float h, MarsObject.Kind kind) {
        MarsObject object = new MarsObject(x, y, w, h, kind, assets, physics);
        props.add(object);
        return object;
    }

    private void addItem(float x, float y, MarsObject.Kind kind) {
        MarsObject item = new MarsObject(x, y, 72f, 72f, kind, assets, physics);
        props.add(item);
        items.add(item);
    }

    private void addRandomItem(MarsObject.Kind kind) {
        Rectangle candidate = new Rectangle();
        for (int attempt = 0; attempt < 120; attempt++) {
            float x = MathUtils.random(120f, WORLD_W - 192f);
            float y = MathUtils.random(120f, WORLD_H - 192f);
            candidate.set(x + 10f, y + 10f, 52f, 52f);
            if (Vector2.dst(x, y, 230f, 250f) < 250f) continue;
            boolean blocked = candidate.overlaps(habitat.getBounds())
                || candidate.overlaps(landingPad.getBounds());
            for (MarsObject object : rocks) if (candidate.overlaps(object.getBounds())) { blocked = true; break; }
            if (!blocked) for (MarsObject station : stations) {
                if (candidate.overlaps(station.getBounds())) { blocked = true; break; }
            }
            if (!blocked) { addItem(x, y, kind); return; }
        }
        addItem(MathUtils.random(180f, WORLD_W - 220f), MathUtils.random(180f, WORLD_H - 220f), kind);
    }

    @Override public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);
        atualizarMixer();
        update(delta);
        if (changingScreen) return;
        Gdx.gl.glClearColor(.15f, .045f, .025f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(assets.marsBackgroundTexture, 0f, 0f, WORLD_W, WORLD_H);
        renderLandmarks();
        for (MarsObject object : props) object.render(batch);
        returnPortal.render(batch);
        titanPortal.render(batch);
        for (MarsEnemy enemy : enemies) enemy.render(batch);
        player.render(batch);
        particles.render(batch);
        batch.end();
        renderHitboxDebug();
        renderShot();
        renderEnemyHealth();
        renderHud();
        renderDamage();
        if (paused) renderPause();
        renderTransition();
    }

    /** Trilha e ouvinte seguem o relogio real, fora do hitstop e da pausa. */
    private void atualizarMixer() {
        sounds.setListener(camera.position.x, camera.position.y);
        sounds.atualizarIntensidade(paused ? 0f : combatTension(),
            paused ? 0f : oxygenUrgency());
    }

    private float combatTension() {
        float strongest = 0f;
        for (MarsEnemy enemy : enemies) {
            if (!enemy.isAtivo()) continue;
            float dx = enemy.centerX() - player.getPosition().x;
            float dy = enemy.centerY() - player.getPosition().y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            strongest = Math.max(strongest,
                1f - MathUtils.clamp(distance / GameConfig.MUSIC_TENSION_RADIUS, 0f, 1f));
        }
        return strongest;
    }

    private float oxygenUrgency() {
        float oxygen = player.getOxigenio();
        if (oxygen >= GameConfig.MUSIC_URGENCY_OXYGEN) return 0f;
        return MathUtils.clamp(1f - oxygen / GameConfig.MUSIC_URGENCY_OXYGEN, 0f, 1f);
    }

    /** Mesma sobreposicao da fase lunar; ver LunarScreen.renderHitboxDebug. */
    private void renderHitboxDebug() {
        if (!hitboxDebug.begin(camera)) return;
        for (MarsObject object : props) {
            hitboxDebug.box(object.getBounds(),
                object.isCollectible() ? UiTheme.AMBER : UiTheme.TEXT_MUTED);
        }
        hitboxDebug.box(returnPortal.getBounds(), UiTheme.GREEN);
        hitboxDebug.box(titanPortal.getBounds(), campaign.portalLiberado()
            ? UiTheme.CYAN : UiTheme.RED);
        for (MarsEnemy enemy : enemies) {
            if (!enemy.isAtivo()) continue;
            hitboxDebug.box(enemy.getBounds(), UiTheme.MAGENTA);
            hitboxDebug.center(enemy.centerX(), enemy.centerY(), UiTheme.MAGENTA);
        }
        hitboxDebug.box(player.getBounds(), UiTheme.CYAN);
        hitboxDebug.end();
    }

    private void update(float delta) {
        if (changingScreen) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) hitboxDebug.toggle();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || paused && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            paused = !paused;
            player.getBody().setLinearVelocity(0f, 0f);
            return;
        }
        if (paused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                changingScreen = true;
                game.setScreen(new MenuScreen(game));
                dispose();
            }
            return;
        }
        juice.update(delta);
        titanCombat.update(delta);
        float gameplayDelta = juice.gameplayDelta(delta);
        if (gameplayDelta <= 0f) {
            particles.update(delta * .35f);
            return;
        }
        delta = gameplayDelta;
        missionTime += delta;
        reveal = Math.min(1f, reveal + delta / .65f);
        messageTimer = Math.max(0f, messageTimer - delta);
        shotTimer = Math.max(0f, shotTimer - delta);
        extractionGlow += delta;
        Vector2 direction = input.getDirection();
        if (input.consumeDashPressed() && player.tryDash(direction.x, direction.y)) {
            particles.criarPoeiraMarte(player.getPosition().x + 27f, player.getPosition().y,
                true, direction.x, direction.y);
            juice.trigger(JuiceSystem.Preset.DASH);
        }
        player.move(direction.x, direction.y, input.isRunning(), delta);
        physics.update(delta);
        player.update(delta);
        player.setProtegido(player.getBounds().overlaps(habitat.getBounds()));
        if (player.isProtegido()) {
            player.recuperarOxigenio(9f * delta);
            player.recuperarEnergia(5f * delta);
        }
        mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouseWorld);
        player.setAimTarget(mouseWorld.x, mouseWorld.y);
        if (input.consumeAttackPressed()) shoot();
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) handleInteraction();
        // O portal de volta troca de tela e libera fisica e particulas; seguir
        // com o resto do update depois disso seria mexer em recurso morto.
        if (changingScreen) return;
        atualizarSaveLoad();
        for (MarsObject object : props) object.update(delta);
        returnPortal.update(delta);
        titanPortal.setUnlocked(campaign.portalLiberado());
        titanPortal.update(delta);
        collectItems();
        for (MarsEnemy enemy : enemies) {
            enemy.update(delta, player, rocks);
            if (enemy.consumeTelegraphStarted()) {
                particles.criarAlertaInimigo(enemy.centerX(), enemy.centerY(), true);
                sounds.tocarAlertaInimigo(enemy.centerX(), enemy.centerY());
            }
            if (enemy.canDamage(player)) {
                player.receberDano(10f, enemy.centerX(), enemy.centerY());
                particles.criarImpactoTraje(player.getPosition().x + 27f, player.getPosition().y + 38f);
                feedback("Impacto no traje: perda de oxigênio.");
                juice.trigger(JuiceSystem.Preset.PLAYER_HURT);
            }
        }
        particles.update(delta);
        updateFootFx(delta, direction);
        updateCamera(delta);
        if (player.isMorto()) {
            changingScreen = true;
            game.setScreen(new GameOverScreen(game, missionTime));
            dispose();
        }
    }

    private void collectItems() {
        for (MarsObject item : items) {
            if (!item.isAtivo() || !player.getBounds().overlaps(item.getBounds())) continue;
            item.collect();
            juice.trigger(JuiceSystem.Preset.COLLECT);
            particles.criarEfeitoColeta(item.getPosition().x + 36f, item.getPosition().y + 36f);
            sounds.tocarColetaEspacial(item.getPosition().x + 36f, item.getPosition().y + 36f);
            switch (item.getKind()) {
                case MINERAL -> {
                    minerals++;
                    if (campaign.isDialogoTita() && !campaign.isAmostraOk()) {
                        campaign.setAmostraOk(true);
                        feedback("Amostra de metano validada. O selo do portal ficou azul.");
                    } else feedback("Núcleo marciano recuperado  •  " + minerals);
                }
                case MEDKIT -> { player.recuperarOxigenio(28f); feedback("Selagem de emergência aplicada  •  O2 restaurado"); }
                case POWER_CELL -> {
                    player.recuperarEnergia(35f);
                    int cells = player.adicionarMunicao(GameConfig.AMMO_PER_POWER_CELL);
                    feedback(cells > 0
                        ? "Célula integrada  •  +" + cells + " de munição"
                        : "Célula de energia integrada");
                }
                default -> { }
            }
        }
    }

    private void handleInteraction() {
        if (titanDialogue.isOpen()) {
            titanDialogue.next();
            if (titanDialogue.isFinished()) {
                campaign.setDialogoTita(true);
                if (minerals > 0) campaign.setAmostraOk(true);
                feedback(campaign.missaoAtual());
            }
            return;
        }
        if (titanPortal.isPlayerNear(player)) {
            if (!campaign.isDialogoTita()) {
                titanDialogue.start(new String[] {
                    "O portal para Titã está instável. A autorização foi suspensa.",
                    "Prove capacidade de combate ou entregue uma amostra de metano.",
                    "Quando o acesso for liberado, o selo central ficará azul."
                });
                return;
            }
            if (!campaign.portalLiberado()) {
                feedback("BLOQUEADO");
                return;
            }
            enterTitan();
            return;
        }
        interactWorld();
    }

    private void interactWorld() {
        for (MarsObject station : stations) {
            if (station.isEnabled() || distanceTo(station) > 135f) continue;
            if (minerals <= 0) {
                feedback("Esta estação precisa de um núcleo marciano.");
                return;
            }
            minerals--;
            activeStations++;
            station.activate();
            juice.trigger(JuiceSystem.Preset.REPAIR);
            particles.criarProcessamento(station.getBounds().x + station.getBounds().width / 2f,
                station.getBounds().y + station.getBounds().height * .55f);
            feedback(activeStations == 3 ? "Colônia sincronizada. Neutralize os hostis e alcance a plataforma."
                : "Estação reativada  •  " + activeStations + "/3");
            return;
        }
        if (returnPortal.isPlayerNear(player)) {
            atravessarPortalDeVolta();
            return;
        }
        feedback(missionComplete() ? "Sinal de extração disponível na plataforma."
            : "Nenhum sistema ao alcance.");
    }

    private void enterTitan() {
        campaign.setEntrouTita(true);
        campaign.setPhase(CampaignState.Phase.TITAN);
        campaign.setVitals(player.getOxigenio(), player.getEnergia());
        campaign.setAmmo(player.getMunicao());
        campaign.setMissionTime(missionTime);
        GameSaveData data = player.toSaveData();
        data.posX = 260f;
        data.posY = 260f;
        LunarCheckpoint.applyCampaign(data, campaign);
        new SaveManager().save(data);
        changingScreen = true;
        game.setScreen(new TitanScreen(game, campaign));
        dispose();
    }

    /**
     * F5 e F9 tambem valem em Marte.
     *
     * O save e da campanha inteira, entao gravar aqui registra a fase
     * marciana; carregar um save lunar a partir daqui seria trocar de fase no
     * meio do frame, e por isso e recusado com aviso.
     */
    private void atualizarSaveLoad() {
        if (input.consumeSavePressed()) {
            guardarCampanha();
            GameSaveData data = player.toSaveData();
            LunarCheckpoint.applyCampaign(data, campaign);
            new SaveManager().save(data);
            feedback("Campanha salva na fase marciana.");
        }
        if (!input.consumeLoadPressed()) return;
        GameSaveData data = new SaveManager().load();
        if (data == null) {
            feedback("Nenhum checkpoint encontrado.");
            return;
        }
        if (CampaignState.phaseFromToken(data.fase) != CampaignState.Phase.MARS) {
            feedback("O checkpoint salvo é da fase lunar. Carregue pelo menu.");
            return;
        }
        player.fromSaveData(data);
        player.setMunicao(data.municao);
        minerals = data.marteNucleos;
        for (int index = activeStations; index < Math.min(data.marteEstacoes, stations.size); index++) {
            stations.get(index).activate();
        }
        activeStations = Math.min(data.marteEstacoes, stations.size);
        hostilesDefeated = Math.min(data.marteHostis, enemies.size);
        for (int index = 0; index < enemies.size; index++) {
            enemies.get(index).setAtivo(index >= hostilesDefeated);
        }
        feedback("Checkpoint marciano carregado.");
    }

    /** Portal de volta: devolve o jogador a mesma Lua que ele deixou. */
    private void atravessarPortalDeVolta() {
        guardarCampanha();
        campaign.setPhase(CampaignState.Phase.LUNAR);
        changingScreen = true;
        sounds.tocarInicio();
        game.setScreen(new LunarScreen(game, game.getBatch(), game.getAssets(), campaign));
        dispose();
    }

    private float distanceTo(MarsObject object) {
        return Vector2.dst(player.getPosition().x + 27f, player.getPosition().y + 38f,
            object.getBounds().x + object.getBounds().width / 2f,
            object.getBounds().y + object.getBounds().height / 2f);
    }

    private void shoot() {
        float x = player.getPosition().x + 27f;
        float y = player.getPosition().y + GameConfig.PLAYER_HEIGHT * .48f;
        float dx = MathUtils.cosDeg(player.getAimAngle());
        float dy = MathUtils.sinDeg(player.getAimAngle());
        MarsEnemy target = null;
        float closest = titanCombat.getAlcance();
        for (MarsEnemy enemy : enemies) {
            if (!enemy.isAtivo()) continue;
            float ex = enemy.centerX() - x;
            float ey = enemy.centerY() - y;
            float along = ex * dx + ey * dy;
            float perpendicular = Math.abs(ex * dy - ey * dx);
            if (along > 0f && along < closest && perpendicular < 48f) {
                target = enemy;
                closest = along;
            }
        }
        shotStart.set(x + dx * 33f, y + dy * 33f);
        shotEnd.set(x + dx * titanCombat.getAlcance(), y + dy * titanCombat.getAlcance());
        titanCombat.setMunicao(player.getMunicao());
        boolean fired = titanCombat.tentarTiro(shotStart, target, campaign.hasWeapon());
        player.setMunicao(titanCombat.getMunicao());
        if (!fired) {
            feedback(!campaign.hasWeapon() ? "Fabrique a arma antes de disparar."
                : player.getMunicao() <= 0 ? "Sem munição." : "Rifle recarregando.");
            return;
        }
        if (target != null) {
            shotEnd.set(target.centerX(), target.centerY());
            boolean killed = !target.isAlive();
            if (killed) {
                hostilesDefeated++;
                particles.criarMorteInimigo(target.centerX(), target.centerY());
                sounds.tocarMorteInimigo(target.centerX(), target.centerY());
            } else {
                particles.criarImpactoTiro(target.centerX(), target.centerY());
                sounds.tocarImpacto(target.centerX(), target.centerY());
            }
            juice.trigger(killed ? JuiceSystem.Preset.ENEMY_KILL : JuiceSystem.Preset.SHOT_HIT);
        }
        particles.criarMuzzleFlash(shotStart.x, shotStart.y, player.getAimAngle());
        player.triggerShot();
        sounds.tocarDisparo();
        shotTimer = .14f;
    }

    private void updateFootFx(float delta, Vector2 direction) {
        if (!player.isMoving()) { dustTimer = 0f; stepTimer = 0f; return; }
        dustTimer += delta;
        stepTimer += delta;
        float interval = player.isSprinting() ? .11f : .24f;
        if (dustTimer >= interval) {
            dustTimer = 0f;
            particles.criarPoeiraMarte(player.getPosition().x + 27f, player.getPosition().y,
                player.isSprinting(), direction.x, direction.y);
        }
        if (stepTimer >= (player.isSprinting() ? .32f : .48f)) {
            stepTimer = 0f;
            sounds.tocarPassoLunar();
        }
    }

    private void updateCamera(float delta) {
        cameraTarget.set(player.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
            player.getPosition().y + GameConfig.PLAYER_HEIGHT / 2f);
        cameraDirector.update(cameraTarget, player.getBody().getLinearVelocity(),
            hasNearbyHostile(), delta);
    }

    private boolean hasNearbyHostile() {
        float radius2 = GameConfig.CAMERA_COMBAT_RADIUS * GameConfig.CAMERA_COMBAT_RADIUS;
        for (MarsEnemy enemy : enemies) {
            if (!enemy.isAtivo()) continue;
            float dx = enemy.centerX() - cameraTarget.x;
            float dy = enemy.centerY() - cameraTarget.y;
            if (dx * dx + dy * dy < radius2) return true;
        }
        return false;
    }

    private boolean missionComplete() {
        return activeStations == 3 && hostilesDefeated == enemies.size;
    }
    private void feedback(String value) { message = value; messageTimer = 3.4f; }

    private void renderShot() {
        if (shotTimer <= 0f) return;
        float alpha = shotTimer / .14f;
        float progress = 1f - alpha;
        float px = MathUtils.lerp(shotStart.x, shotEnd.x, progress);
        float py = MathUtils.lerp(shotStart.y, shotEnd.y, progress);
        float dx = shotEnd.x - shotStart.x, dy = shotEnd.y - shotStart.y;
        float length = Math.max(.001f, (float)Math.sqrt(dx * dx + dy * dy));
        dx /= length;
        dy /= length;
        // Mesmo traco da fase lunar, em textura, sem trocar de renderer.
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(1f, .45f, .25f, alpha * .35f);
        drawTrail(px - dx * 36f, py - dy * 36f, px, py, 8f * alpha);
        batch.setColor(1f, .86f, .62f, alpha);
        drawTrail(px - dx * 44f, py - dy * 44f, px, py, 2f);
        float glow = 16f;
        batch.draw(assets.energyFxFrame(1, 0), px - glow / 2f, py - glow / 2f, glow, glow);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawTrail(float x1, float y1, float x2, float y2, float thickness) {
        float lineX = x2 - x1;
        float lineY = y2 - y1;
        float length = (float) Math.sqrt(lineX * lineX + lineY * lineY);
        if (length <= .001f) return;
        float angle = MathUtils.atan2(lineY, lineX) * MathUtils.radiansToDegrees;
        batch.draw(assets.uiWhiteTexture, x1, y1 - thickness / 2f,
            0f, thickness / 2f, length, thickness, 1f, 1f, angle);
    }

    private void renderEnemyHealth() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (MarsEnemy enemy : enemies) {
            if (!enemy.isAtivo() || enemy.getHealthRatio() >= 1f) continue;
            batch.setColor(MARS_DARK);
            batch.draw(assets.uiBarTrackTexture, enemy.centerX() - 28f, enemy.centerY() + 48f, 56f, 6f);
            batch.setColor(MARS);
            batch.draw(assets.uiBarFillTexture, enemy.centerX() - 28f, enemy.centerY() + 48f,
                56f * enemy.getHealthRatio(), 6f);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void renderHud() {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        drawUiPanel(32f, 28f, 294f, 82f, MARS, .91f);
        drawUiPanel(914f, 28f, 334f, 82f, MARS, .91f);
        drawUiPanel(330f, 646f, 620f, 52f, MARS, .94f);
        if (messageTimer > 0f) drawUiPanel(398f, 128f, 484f, 48f, UiTheme.AMBER,
            Math.min(.94f, messageTimer * 2f));
        drawBar(88f, 75f, 178f, 9f, player.getOxigenio() / 100f,
            player.getOxigenio() < 25f ? UiTheme.RED : UiTheme.CYAN);
        drawBar(88f, 49f, 178f, 8f, player.getEnergia() / 100f, UiTheme.AMBER);
        drawBar(88f, 33f, 178f, 6f, player.getMunicao() / (float) GameConfig.AMMO_MAX,
            player.getMunicao() <= GameConfig.AMMO_LOW ? UiTheme.RED : UiTheme.GREEN);
        if (missionComplete()) {
            float pulse = .35f + MathUtils.sin(extractionGlow * 4f) * .15f;
            batch.setColor(MARS.r, MARS.g, MARS.b, pulse);
            batch.draw(assets.uiWhiteTexture, 330f, 642f, 620f, 3f);
        }
        batch.setColor(Color.WHITE);
        text("O2", .72f, UiTheme.TEXT_MUTED, 50f, 88f, 1f);
        text(String.format("%.0f%%", player.getOxigenio()), .7f, UiTheme.TEXT, 275f, 87f, 1f);
        text("EN", .72f, UiTheme.TEXT_MUTED, 50f, 61f, 1f);
        text(String.format("%.0f%%", player.getEnergia()), .7f, UiTheme.TEXT, 275f, 60f, 1f);
        text("MUN", .72f, UiTheme.TEXT_MUTED, 50f, 40f, 1f);
        text(String.format("%d", player.getMunicao()), .7f,
            player.getMunicao() <= GameConfig.AMMO_LOW ? UiTheme.RED : UiTheme.GREEN, 275f, 39f, 1f);
        text("NÚCLEOS  " + minerals, .74f, UiTheme.AMBER, 944f, 84f, 1f);
        text("ESTAÇÕES  " + activeStations + "/3", .74f, UiTheme.CYAN, 1050f, 84f, 1f);
        text("HOSTIS  " + hostilesDefeated + "/" + enemies.size, .72f, UiTheme.TEXT_MUTED, 944f, 55f, 1f);
        text(campaign.missaoAtual(), .72f,
            campaign.portalLiberado() ? UiTheme.GREEN : UiTheme.TEXT, 354f, 680f, 1f);
        text(campaign.statusPortal(), .62f,
            campaign.portalLiberado() ? UiTheme.CYAN : UiTheme.RED, 955f, 680f, 1f);
        if (messageTimer > 0f) centered(message, .72f, UiTheme.TEXT, 152f,
            Math.min(1f, messageTimer * 2f));
        if (titanDialogue.isOpen()) {
            drawUiPanel(170f, 242f, 940f, 180f, UiTheme.AMBER, .98f);
            text("TRANSMISSÃO · CONTROLE ORBITAL", .66f, UiTheme.AMBER, 208f, 388f, 1f);
            text(titanDialogue.line(), .82f, UiTheme.TEXT, 208f, 330f, 1f);
            text("E  •  CONTINUAR", .62f, UiTheme.TEXT_MUTED, 886f, 274f, 1f);
        }
        batch.end();
    }

    private void renderDamage() {
        float alpha = juice.getDamageFlashAlpha();
        if (alpha <= 0f) return;
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.setColor(1f, .55f, .35f, alpha * .85f);
        batch.draw(assets.uiDamageVignetteTexture, 0f, 0f, 1280f, 720f);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void renderLandmarks() {
        batch.setColor(1f, .9f, .82f, .88f);
        batch.draw(assets.landmarkRegion(0, 1), 1120f, 1480f, 260f, 180f);
        batch.draw(assets.landmarkRegion(1, 1), 2640f, 680f, 220f, 165f);
        batch.draw(assets.landmarkRegion(2, 1), 380f, 720f, 235f, 145f);
        batch.draw(assets.landmarkRegion(3, 1), 1900f, 1510f, 180f, 205f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderPause() {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.setColor(.055f, .016f, .012f, .91f);
        batch.draw(assets.uiWhiteTexture, 0f, 0f, 1280f, 720f);
        modalPatch.setColor(new Color(1f, .84f, .76f, .98f));
        modalPatch.draw(batch, 54f, 106f, 790f, 500f);
        modalPatch.draw(batch, 872f, 106f, 354f, 500f);
        modalPatch.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);
        text("REGISTRO MARCIANO · EM ESPERA", .75f, MARS, 74f, 616f, 1f);
        text("PAUSA", 2.25f, UiTheme.TEXT, 68f, 540f, 1f);
        text("A poeira parou. A telemetria também.", .88f, UiTheme.TEXT_MUTED, 74f, 475f, 1f);
        text("RETOMAR", 1.02f, UiTheme.TEXT, 104f, 278f, 1f);
        text("ESC ou ENTER", .68f, MARS, 610f, 278f, 1f);
        text("VOLTAR AO MENU", .82f, UiTheme.TEXT_MUTED, 74f, 150f, 1f);
        text("M", .74f, MARS, 610f, 150f, 1f);
        text(String.format("O2 %.0f%%\nESTAÇÕES %d/3\nHOSTIS %d/%d",
            player.getOxigenio(), activeStations, hostilesDefeated, enemies.size),
            .82f, UiTheme.TEXT, 934f, 520f, 1f);
        batch.end();
    }

    private void renderTransition() {
        if (reveal >= 1f) return;
        float alpha = 1f - Interpolation.pow3Out.apply(reveal);
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.setColor(.07f, .012f, .008f, alpha);
        batch.draw(assets.uiWhiteTexture, 0f, 0f, 1280f, 720f);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawUiPanel(float x, float y, float w, float h, Color accent, float alpha) {
        panelPatch.setColor(new Color(accent.r * .38f + .62f, accent.g * .38f + .62f,
            accent.b * .38f + .62f, alpha));
        panelPatch.draw(batch, x, y, w, h);
        panelPatch.setColor(Color.WHITE);
    }

    private void drawBar(float x, float y, float w, float h, float ratio, Color color) {
        batch.setColor(Color.WHITE);
        batch.draw(assets.uiBarTrackTexture, x, y, w, h);
        batch.setColor(color);
        batch.draw(assets.uiBarFillTexture, x, y, w * MathUtils.clamp(ratio, 0f, 1f), h);
        batch.setColor(Color.WHITE);
    }

    private void text(String value, float scale, Color color, float x, float y, float alpha) {
        assets.font.getData().setScale(scale);
        assets.font.setColor(color.r, color.g, color.b, alpha);
        assets.font.draw(batch, value, x, y);
    }

    private void centered(String value, float scale, Color color, float y, float alpha) {
        assets.font.getData().setScale(scale);
        assets.font.setColor(color.r, color.g, color.b, alpha);
        layout.setText(assets.font, value);
        assets.font.draw(batch, layout, (1280f - layout.width) / 2f, y);
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height, false);
        uiViewport.update(width, height, true);
    }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() {
        if (particles != null) { particles.dispose(); particles = null; }
        if (physics != null) { physics.dispose(); physics = null; }
    }
}

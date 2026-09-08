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
import com.orion.echoes.lua.managers.MissionSprite;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.render.HitboxDebugRenderer;
import com.orion.echoes.lua.save.GameSaveData;
import com.orion.echoes.lua.save.LunarCheckpoint;
import com.orion.echoes.lua.save.SaveManager;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.systems.CombatTarget;
import com.orion.echoes.lua.systems.TitanCombatSystem;
import com.orion.echoes.lua.ui.UiTheme;

/** Terceira fase real: superfície de metano de Titã, combate e retorno. */
public final class TitanScreen implements Screen {
    private static final float WORLD_W = 2600f;
    private static final float WORLD_H = 1700f;
    private static final Color AMBER = Color.valueOf("D78A36");
    /** Formacoes de gelo: x, y, largura, altura e qual pedra do atlas. */
    private static final float[][] FORMACOES = {
        {320f, 700f, 190f, 150f, 0f}, {880f, 340f, 160f, 128f, 2f},
        {1240f, 1080f, 210f, 165f, 1f}, {1760f, 560f, 175f, 140f, 3f},
        {2200f, 1300f, 200f, 158f, 4f}, {620f, 1240f, 165f, 132f, 5f},
        {1520f, 1420f, 185f, 148f, 0f}, {2380f, 820f, 170f, 136f, 2f},
        {980f, 820f, 150f, 120f, 3f}, {1900f, 1080f, 195f, 155f, 1f},
        {460f, 420f, 155f, 124f, 4f}, {2100f, 380f, 180f, 144f, 5f}
    };
    private final EchoesLua game;
    private final CampaignState campaign;
    private final Array<TitanEnemy> enemies = new Array<>();
    private final Array<Pickup> suprimentos = new Array<>();
    private final Array<TitanProjectile> projectiles = new Array<>();
    private final Array<Rectangle> collisionObstacles = new Array<>();
    /** Refinaria de campo: e aqui que o gelo de Tita vira municao. */
    private final Rectangle refinaria = new Rectangle(430f, 150f, 148f, 132f);
    private final Vector2 shotOrigin = new Vector2();
    private final Vector2 shotEnd = new Vector2();
    private final Vector2 mouseWorld = new Vector2();
    private AssetManager assets;
    private SpriteBatch batch;
    private PhysicsWorld physics;
    private Astronauta player;
    private TitanPortal returnPortal;
    private TitanBoss boss;
    private boolean vitoriaRegistrada;
    private TitanCombatSystem combat;
    private GameInputProcessor input;
    private OrthographicCamera camera;
    private OrthographicCamera uiCamera;
    private Viewport viewport;
    private Viewport uiViewport;
    private NinePatch panel;
    private NinePatch modal;
    private com.orion.echoes.lua.systems.NpcConversation conversation;
    private com.orion.echoes.lua.managers.ParticleManager particles;
    private float footstepTimer;
    private HitboxDebugRenderer hitboxDebug;
    private String message = "A atmosfera abafa o sinal. Explore com cautela.";
    private float messageTimer = 4f;
    private float missionTime;
    private float shotTimer;
    private boolean paused;
    private boolean changingScreen;
    private boolean portalTraveling;

    public TitanScreen(EchoesLua game, CampaignState campaign) {
        this.game = game;
        this.campaign = campaign == null ? game.getCampaign() : campaign;
    }

    @Override public void show() {
        assets = game.getAssets();
        batch = game.getBatch();
        game.getSounds().setVacuum(false);
        game.getSounds().applySettings(game.getSettings());
        game.getSounds().tocarMusicaTita();
        particles = new com.orion.echoes.lua.managers.ParticleManager(assets);
        panel = assets.uiPanelPatch();
        modal = assets.uiModalPatch();
        hitboxDebug = new HitboxDebugRenderer(batch, assets);
        physics = new PhysicsWorld();
        input = new GameInputProcessor();
        Gdx.input.setInputProcessor(input);
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, camera);
        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, uiCamera);
        uiCamera.position.set(640f, 360f, 0f);
        uiCamera.update();
        new Wall(-24f, 0f, 24f, WORLD_H, physics);
        new Wall(WORLD_W, 0f, 24f, WORLD_H, physics);
        new Wall(0f, -24f, WORLD_W, 24f, physics);
        new Wall(0f, WORLD_H, WORLD_W, 24f, physics);
        for (float[] formation : FORMACOES) {
            Rectangle footprint = new Rectangle(
                formation[0] + formation[2] * .16f,
                formation[1] + formation[3] * .06f,
                formation[2] * .68f,
                formation[3] * .28f);
            collisionObstacles.add(footprint);
            new Wall(footprint.x, footprint.y, footprint.width, footprint.height, physics);
        }
        Rectangle refineryFootprint = new Rectangle(refinaria.x + 24f, refinaria.y + 8f,
            refinaria.width - 48f, 34f);
        collisionObstacles.add(refineryFootprint);
        new Wall(refineryFootprint.x, refineryFootprint.y,
            refineryFootprint.width, refineryFootprint.height, physics);
        player = new Astronauta(260f, 260f, assets, physics);
        player.setSurfaceProfile(Astronauta.SurfaceProfile.MARS);
        player.setWeaponEquipped(campaign.hasWeapon());
        player.setMunicao(campaign.getAmmo());

        // Exigência da prova: a Screen carrega o personagem via fromSaveData no show().
        GameSaveData saved = new SaveManager().load();
        if (saved != null && CampaignState.phaseFromToken(saved.fase) == CampaignState.Phase.TITAN) {
            player.fromSaveData(saved);
            player.setMunicao(saved.municao);
        } else {
            GameSaveData arrival = player.toSaveData();
            LunarCheckpoint.applyCampaign(arrival, campaign);
            player.fromSaveData(arrival);
        }
        conversation = new com.orion.echoes.lua.systems.NpcConversation(
            new com.orion.echoes.lua.entities.Npc(650f, 210f, "PESQUISADORA LIRA",
                Color.WHITE, assets),
            new String[] {
                "Os lagos daqui são de metano líquido. Nossa equipe perdeu contato quando o Soberano ocupou o vale a nordeste.",
                "O gelo abastece esta refinaria. Guarde munição para o chefe e aproveite os cilindros de oxigênio entre as formações.",
                "Quando o solo brilhar sob ele, saia da área. Se precisar recuar, o portal oeste leva você de volta a Marte."
            }, batch, assets);
        combat = new TitanCombatSystem(campaign);
        combat.setMunicao(player.getMunicao());
        returnPortal = new TitanPortal(150f, 150f, assets);
        returnPortal.setUnlocked(true);
        returnPortal.setReversed(true);
        enemies.add(new TitanEnemy(760f, 610f, assets));
        enemies.add(new TitanEnemy(1370f, 980f, assets));
        enemies.add(new TitanEnemy(2050f, 520f, assets));
        // O chefe guarda o fundo do mapa: o jogador o encontra depois dos comuns.
        boss = new TitanBoss(2150f, 1150f, assets);
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
        update(delta);
        if (changingScreen) return;
        Gdx.gl.glClearColor(.09f, .045f, .018f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(assets.titanBackgroundTexture, 0f, 0f, WORLD_W, WORLD_H);
        batch.setColor(Color.WHITE);
        desenharTerreno();
        returnPortal.render(batch);
        batch.draw(assets.missionRegion(MissionSprite.CRAFTING_TERMINAL),
            refinaria.x, refinaria.y, refinaria.width, refinaria.height);
        for (Pickup suprimento : suprimentos) suprimento.render(batch);
        desenharAvisoDoChefe();
        for (TitanEnemy enemy : enemies) enemy.render(batch);
        renderEnemyHealth();
        for (TitanProjectile projectile : projectiles) projectile.render(batch);
        boss.render(batch);
        renderPlayerShot();
        player.render(batch);
        conversation.renderWorld(batch, player);
        particles.render(batch);
        batch.end();
        renderHitboxes();
        renderHud();
        if (paused) renderPause();
        else {
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            conversation.renderUi();
            batch.end();
        }
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
        if (portalTraveling) {
            player.getBody().setLinearVelocity(0f, 0f);
            returnPortal.update(delta);
            if (returnPortal.isTraversalComplete()) finishReturnToMars();
            return;
        }
        missionTime += delta;
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
        combat.update(delta);
        messageTimer = Math.max(0f, messageTimer - delta);
        shotTimer = Math.max(0f, shotTimer - delta);
        Vector2 direction = input.getDirection();
        if (input.consumeDashPressed()) player.tryDash(direction.x, direction.y);
        player.move(direction.x, direction.y, input.isRunning(), delta);
        footstepTimer -= delta;
        if (!direction.isZero() && footstepTimer <= 0f) {
            footstepTimer = input.isRunning() ? .24f : .38f;
            game.getSounds().tocarPassoTita();
        }
        physics.update(delta);
        player.update(delta);
        mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouseWorld);
        player.setAimTarget(mouseWorld.x, mouseWorld.y);
        if (input.consumeAttackPressed()) shoot();
        for (TitanEnemy enemy : enemies) {
            enemy.update(delta, player, WORLD_W, WORLD_H, collisionObstacles);
            if (enemy.consumeShot()) spawnEnemyShot(enemy.centerX(), enemy.centerY(),
                enemy.shotDirectionX(), enemy.shotDirectionY(), 245f, 12f);
            if (enemy.canDamage(player)) {
                player.receberDano(11f, enemy.centerX(), enemy.centerY());
                feedback("Predador de metano atingiu o traje.");
            }
        }
        atualizarChefe(delta);
        if (changingScreen) return;
        updateProjectiles(delta);
        returnPortal.update(delta);
        coletarSuprimentos(delta);
        if (input.consumeInteractPressed()) interagir();
        if (input.consumeSavePressed()) saveTitan();
        if (input.consumeLoadPressed()) loadTitan();
        camera.position.x = MathUtils.clamp(player.getPosition().x,
            GameConfig.WINDOW_WIDTH / 2f, WORLD_W - GameConfig.WINDOW_WIDTH / 2f);
        camera.position.y = MathUtils.clamp(player.getPosition().y,
            GameConfig.WINDOW_HEIGHT / 2f, WORLD_H - GameConfig.WINDOW_HEIGHT / 2f);
        camera.update();
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
        if (!boss.isAtivo()) {
            if (!vitoriaRegistrada) { vitoriaRegistrada = true; concluirCampanha(); }
            return;
        }
        boss.update(delta, player, WORLD_W, WORLD_H, collisionObstacles);
        if (boss.consumeSlam() && boss.slamHits(player)) {
            player.receberDano(GameConfig.BOSS_DAMAGE, boss.centerX(), boss.centerY());
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
            batch.setColor(Color.WHITE);
            batch.draw(assets.titanFormationRegion((int) pedra[4]),
                pedra[0], pedra[1], pedra[2], pedra[3]);
        }
        // As silhuetas ficam no alto: sao horizonte, nao obstaculo.
        batch.setColor(.5f, .38f, .26f, .8f);
        batch.draw(assets.landmarkRegion(1, 1), 380f, 1440f, 300f, 210f);
        batch.draw(assets.landmarkRegion(3, 1), 1240f, 1520f, 260f, 190f);
        batch.draw(assets.landmarkRegion(0, 1), 2080f, 1470f, 280f, 200f);
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
        shotOrigin.set(player.getPosition().x + GameConfig.PLAYER_WIDTH / 2f,
            player.getPosition().y + GameConfig.PLAYER_HEIGHT * .48f);
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
        if (boss != null && boss.isAlive()) {
            float along = alinhamento(boss.centerX(), boss.centerY(), dirX, dirY, closest, 105f);
            if (along > 0f) {
                target = boss;
                closest = along;
            }
        }
        shotOrigin.x += dirX * 30f;
        shotOrigin.y += dirY * 30f;
        shotEnd.set(target == null ? shotOrigin.x + dirX * combat.getAlcance() : target.centerX(),
            target == null ? shotOrigin.y + dirY * combat.getAlcance() : target.centerY());
        combat.setMunicao(player.getMunicao());
        boolean fired = combat.tentarTiro(shotOrigin, target, campaign.hasWeapon());
        player.setMunicao(combat.getMunicao());
        if (fired) {
            game.getSounds().tocarDisparo();
            if (target != null) game.getSounds().tocarImpacto(target.centerX(), target.centerY());
            player.triggerShot();
            shotTimer = .14f;
            feedback(target == null ? "Disparo perdido na névoa de metano."
                : !target.isAlive() ? "Predador neutralizado." : "Impacto confirmado.");
        } else if (player.getMunicao() <= 0) feedback("Sem munição.");

    }

    private void spawnEnemyShot(float x, float y, float dx, float dy, float speed, float damage) {
        projectiles.add(new TitanProjectile(x, y, dx, dy, speed, damage,
            assets.energyFxFrame(1, 0)));
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
                feedback("Cristal de metano perfurou o traje.");
            }
            if (!projectile.isActive()) projectiles.removeIndex(i);
        }
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
    private void concluirCampanha() {
        campaign.setVitals(player.getOxigenio(), player.getEnergia());
        campaign.setAmmo(player.getMunicao());
        campaign.setMissionTime(missionTime);
        changingScreen = true;
        game.setScreen(new VictoryScreen(game, missionTime));
        dispose();
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
            feedback(suprimento.getKind() == Pickup.Kind.OXIGENIO
                ? "Cilindro de oxigênio  •  O2 restaurado"
                : "Gelo de metano recolhido  •  refine na refinaria");
        }
    }

    /** E: refina na refinaria, ou volta a Marte no portal. */
    private void interagir() {
        if (refinaria.overlaps(player.getBounds())) { refinar(); return; }
        if (returnPortal.isPlayerNear(player)) returnToMars();
    }

    /**
     * Gelo vira municao.
     *
     * E o que impede a fase de virar beco sem saida: sem municao o chefe e
     * invencivel, e a refinaria da sempre um caminho de volta.
     */
    private void refinar() {
        if (player.getMunicao() >= GameConfig.AMMO_MAX) {
            feedback("Munição no limite. O gelo foi preservado.");
            return;
        }
        if (!player.removerGelo()) {
            feedback("Sem gelo para refinar. Recolha gelo pelo mapa.");
            return;
        }
        int celulas = player.adicionarMunicao(GameConfig.AMMO_PER_ICE);
        player.recuperarOxigenio(GameConfig.OXYGEN_ITEM_VALUE * .5f);
        combat.setMunicao(player.getMunicao());
        feedback(celulas > 0
            ? "Gelo refinado  •  +" + celulas + " de munição"
            : "Munição já está no limite.");
    }

    private void feedback(String value) { message = value; messageTimer = 3f; }

    private void renderHud() {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        panel.setColor(new Color(1f, .76f, .42f, .96f));
        panel.draw(batch, 32f, 28f, 560f, 94f);
        panel.draw(batch, 410f, 648f, 838f, 50f);
        panel.draw(batch, 410f, 588f, 838f, 52f);
        if (messageTimer > 0f) panel.draw(batch, 430f, 130f, 420f, 48f);
        panel.setColor(Color.WHITE);
        text("TITÃ  •  LAGOS DE METANO", .78f, AMBER, 444f, 681f);
        assets.font.getData().setScale(.70f);
        assets.font.setColor(UiTheme.TEXT);
        assets.font.draw(batch, campaign.missaoAtual(), 436f, 622f, 786f, Align.left, true);
        text("SOBERANO DO METANO", .62f, UiTheme.TEXT_MUTED, 738f, 681f);
        batch.setColor(Color.WHITE);
        batch.draw(assets.uiBarTrackTexture, 956f, 669f, 250f, 8f);
        batch.setColor(boss != null && boss.isTelegraphing() ? UiTheme.RED : AMBER);
        batch.draw(assets.uiBarFillTexture, 956f, 669f,
            250f * (boss == null ? 0f : boss.getHealthRatio()), 8f);
        batch.setColor(Color.WHITE);
        text(String.format("O2  %.0f%%     ENERGIA  %.0f%%     MUNIÇÃO  %d",
            player.getOxigenio(), player.getEnergia(), player.getMunicao()), .74f,
            UiTheme.TEXT, 58f, 88f);
        text(String.format("GELO  %d     •     Refinaria: gelo → munição",
            player.getGelo()), .65f, UiTheme.TEXT_MUTED, 58f, 55f);
        if (messageTimer > 0f) {
            assets.font.getData().setScale(.68f);
            assets.font.setColor(UiTheme.TEXT);
            assets.font.draw(batch, message, 455f, 160f, 370f, Align.center, true);
        }
        batch.end();
        assets.font.getData().setScale(1f);
        assets.font.setColor(Color.WHITE);
    }

    private void renderHitboxes() {
        if (!hitboxDebug.begin(camera)) return;
        hitboxDebug.box(player.getBounds(), UiTheme.CYAN);
        hitboxDebug.box(returnPortal.getBounds(), UiTheme.GREEN);
        hitboxDebug.box(refinaria, UiTheme.GREEN);
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
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.setColor(.025f, .013f, .008f, .92f);
        batch.draw(assets.uiWhiteTexture, 0f, 0f,
            GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        modal.setColor(new Color(1f, .82f, .58f, .98f));
        modal.draw(batch, 132f, 118f, 1016f, 484f);
        modal.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);
        text("REGISTRO T-01  •  TRANSMISSÃO SUSPENSA", .72f, AMBER, 168f, 558f);
        text("PAUSA", 2.3f, UiTheme.TEXT, 166f, 482f);
        text("O soberano aguarda. Seus recursos estão congelados.", .82f,
            UiTheme.TEXT_MUTED, 168f, 423f);
        text("RETOMAR", 1.02f, UiTheme.TEXT, 202f, 292f);
        text("ESC ou ENTER", .72f, AMBER, 850f, 292f);
        text("VOLTAR AO MENU", .86f, UiTheme.TEXT_MUTED, 168f, 190f);
        text("M", .78f, AMBER, 1034f, 190f);
        batch.end();
        assets.font.getData().setScale(1f);
        assets.font.setColor(Color.WHITE);
    }

    private void text(String value, float scale, Color color, float x, float y) {
        assets.font.getData().setScale(scale);
        assets.font.setColor(color);
        assets.font.draw(batch, value, x, y);
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
        // SpriteBatch e AssetManager pertencem ao EchoesLua e não são descartados aqui.
    }
}

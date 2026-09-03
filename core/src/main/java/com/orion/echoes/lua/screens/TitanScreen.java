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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
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
import com.orion.echoes.lua.entities.Wall;
import com.orion.echoes.lua.input.GameInputProcessor;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.MissionSprite;
import com.orion.echoes.lua.physics.PhysicsWorld;
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
    private final EchoesLua game;
    private final CampaignState campaign;
    private final Array<TitanEnemy> enemies = new Array<>();
    private final Array<Pickup> suprimentos = new Array<>();
    /** Refinaria de campo: e aqui que o gelo de Tita vira municao. */
    private final Rectangle refinaria = new Rectangle(430f, 150f, 148f, 132f);
    private final Vector2 mouseWorld = new Vector2();
    private final Vector2 shotOrigin = new Vector2();
    private final GlyphLayout layout = new GlyphLayout();
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
    private String message = "A atmosfera abafa o sinal. Explore com cautela.";
    private float messageTimer = 4f;
    private boolean paused;
    private boolean changingScreen;

    public TitanScreen(EchoesLua game, CampaignState campaign) {
        this.game = game;
        this.campaign = campaign == null ? game.getCampaign() : campaign;
    }

    @Override public void show() {
        assets = game.getAssets();
        batch = game.getBatch();
        panel = assets.uiPanelPatch();
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
        player = new Astronauta(260f, 260f, assets, physics);
        player.setSurfaceProfile(Astronauta.SurfaceProfile.MARS);
        player.setWeaponEquipped(true);
        player.setMunicao(campaign.getAmmo());

        // Exigência da prova: a Screen carrega o personagem via fromSaveData no show().
        GameSaveData saved = new SaveManager().load();
        if (saved != null && CampaignState.phaseFromToken(saved.fase) == CampaignState.Phase.TITAN) {
            player.fromSaveData(saved);
            player.setMunicao(saved.municao);
        } else {
            player.setVitals(Math.max(50f, campaign.getOxygen()),
                Math.max(45f, campaign.getEnergy()));
        }
        combat = new TitanCombatSystem(campaign);
        combat.setMunicao(player.getMunicao());
        returnPortal = new TitanPortal(150f, 150f, assets);
        returnPortal.setUnlocked(true);
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
        batch.setColor(.78f, .56f, .32f, 1f);
        batch.draw(assets.titanBackgroundTexture, 0f, 0f, WORLD_W, WORLD_H);
        batch.setColor(Color.WHITE);
        returnPortal.render(batch);
        batch.draw(assets.missionRegion(MissionSprite.CRAFTING_TERMINAL),
            refinaria.x, refinaria.y, refinaria.width, refinaria.height);
        for (Pickup suprimento : suprimentos) suprimento.render(batch);
        desenharAvisoDoChefe();
        for (TitanEnemy enemy : enemies) enemy.render(batch);
        boss.render(batch);
        player.render(batch);
        batch.end();
        renderHud();
    }

    private void update(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) paused = !paused;
        if (paused || changingScreen) return;
        combat.update(delta);
        messageTimer = Math.max(0f, messageTimer - delta);
        Vector2 direction = input.getDirection();
        player.move(direction.x, direction.y, input.isRunning(), delta);
        physics.update(delta);
        player.update(delta);
        mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouseWorld);
        player.setAimTarget(mouseWorld.x, mouseWorld.y);
        if (input.consumeAttackPressed()) shoot();
        for (TitanEnemy enemy : enemies) {
            enemy.update(delta, player, WORLD_W, WORLD_H);
            if (enemy.canDamage(player)) {
                player.receberDano(11f, enemy.centerX(), enemy.centerY());
                feedback("Predador de metano atingiu o traje.");
            }
        }
        atualizarChefe(delta);
        returnPortal.update(delta);
        coletarSuprimentos(delta);
        if (input.consumeInteractPressed()) interagir();
        if (input.consumeSavePressed()) saveTitan();
        camera.position.x = MathUtils.clamp(player.getPosition().x,
            GameConfig.WINDOW_WIDTH / 2f, WORLD_W - GameConfig.WINDOW_WIDTH / 2f);
        camera.position.y = MathUtils.clamp(player.getPosition().y,
            GameConfig.WINDOW_HEIGHT / 2f, WORLD_H - GameConfig.WINDOW_HEIGHT / 2f);
        camera.update();
    }

    /**
     * O chefe ataca de verdade.
     *
     * Ele para, avisa por quase um segundo e desaba num impacto em area. O
     * dano sai no frame do impacto e alcanca quem ficou dentro do raio, nao
     * so quem encostou - por isso o aviso importa.
     */
    private void atualizarChefe(float delta) {
        if (boss == null || !boss.isAtivo()) return;
        boss.update(delta, player, WORLD_W, WORLD_H);
        if (!boss.consumeSlam()) return;

        if (boss.slamHits(player)) {
            player.receberDano(GameConfig.BOSS_DAMAGE, boss.centerX(), boss.centerY());
            feedback("O impacto do chefe alcançou o traje.");
        } else {
            feedback("Impacto desviado.");
        }
    }

    /** Marca no chao o raio do golpe enquanto o chefe prepara. */
    private void desenharAvisoDoChefe() {
        if (boss == null || !boss.isTelegraphing()) return;
        float progresso = boss.getTelegraphProgress();
        float raio = GameConfig.BOSS_SLAM_RADIUS * progresso;
        batch.setColor(1f, .45f, .25f, .18f + progresso * .30f);
        batch.draw(assets.uiWhiteTexture, boss.centerX() - raio,
            boss.centerY() - 70f - raio * .34f, raio * 2f, raio * .68f);
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
            float along = alinhamento(enemy.centerX(), enemy.centerY(), dirX, dirY, closest);
            if (along > 0f) {
                target = enemy;
                closest = along;
            }
        }
        // O chefe e alvo como qualquer outro, so que com corpo bem maior.
        if (boss != null && boss.isAlive()) {
            float along = alinhamento(boss.centerX(), boss.centerY(), dirX, dirY, closest);
            if (along > 0f) {
                target = boss;
                closest = along;
            }
        }
        combat.setMunicao(player.getMunicao());
        boolean fired = combat.tentarTiro(shotOrigin, target, campaign.hasWeapon());
        player.setMunicao(combat.getMunicao());
        if (fired) {
            player.triggerShot();
            feedback(target == null ? "Disparo perdido na névoa de metano."
                : !target.isAlive() ? "Predador neutralizado." : "Impacto confirmado.");
        } else if (player.getMunicao() <= 0) feedback("Sem munição.");

        if (boss != null && !boss.isAlive() && !vitoriaRegistrada) {
            vitoriaRegistrada = true;
            concluirCampanha();
        }
    }

    /**
     * Projecao do alvo sobre a linha de tiro.
     *
     * Devolve a distancia ao longo da mira quando o alvo esta a frente,
     * dentro do alcance e proximo do eixo; caso contrario, zero.
     */
    private float alinhamento(float alvoX, float alvoY, float dirX, float dirY, float limite) {
        float dx = alvoX - shotOrigin.x;
        float dy = alvoY - shotOrigin.y;
        float along = dx * dirX + dy * dirY;
        float perpendicular = Math.abs(dx * dirY - dy * dirX);
        return along > 0f && along < limite && perpendicular < 52f ? along : 0f;
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
        changingScreen = true;
        game.setScreen(new VictoryScreen(game, player.getTempoVivo()));
        dispose();
    }

    private void saveTitan() {
        campaign.setVitals(player.getOxigenio(), player.getEnergia());
        campaign.setAmmo(player.getMunicao());
        campaign.setPhase(CampaignState.Phase.TITAN);
        GameSaveData data = player.toSaveData();
        LunarCheckpoint.applyCampaign(data, campaign);
        new SaveManager().save(data);
        feedback("Exploração de Titã salva.");
    }

    private void returnToMars() {
        campaign.setVitals(player.getOxigenio(), player.getEnergia());
        campaign.setAmmo(player.getMunicao());
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
        panel.draw(batch, 32f, 28f, 360f, 94f);
        panel.draw(batch, 410f, 648f, 838f, 50f);
        if (messageTimer > 0f) panel.draw(batch, 430f, 130f, 420f, 48f);
        panel.setColor(Color.WHITE);
        text("TITÃ  •  LAGOS DE METANO", .78f, AMBER, 444f, 681f);
        text(String.format("O2  %.0f%%     ENERGIA  %.0f%%     MUNIÇÃO  %d",
            player.getOxigenio(), player.getEnergia(), player.getMunicao()), .74f,
            UiTheme.TEXT, 58f, 88f);
        text(String.format("GELO  %d     E: refinaria vira munição  •  portal retorna a Marte",
            player.getGelo()), .65f, UiTheme.TEXT_MUTED, 58f, 55f);
        if (messageTimer > 0f) {
            layout.setText(assets.font, message);
            text(message, .68f, UiTheme.TEXT, 455f, 160f);
        }
        batch.end();
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
        if (physics != null) { physics.dispose(); physics = null; }
        // SpriteBatch e AssetManager pertencem ao EchoesLua e não são descartados aqui.
    }
}

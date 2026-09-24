package com.orion.echoes.lua.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.Wall;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.render.AtlasRegionRenderer;
import com.orion.echoes.lua.render.DialogBox;
import com.orion.echoes.lua.save.*;
import com.orion.echoes.lua.systems.*;
import com.orion.echoes.lua.ui.*;

/** Quiet last encounter; no fourth boss and no hidden skip to victory. */
public final class AharinScreen implements Screen {

    /**
     * A geometria da cena mora em {@link SanctuaryZone}: e regra de fase, nao
     * decoracao, e foi exatamente onde a versao anterior quebrou.
     */
    private static final float[][] ENTITIES = SanctuaryZone.ENTITIES;

    private static final String[] LINES = {
        "Você atravessou quatro mundos para ouvir um sinal. Agora sabe que ele nunca foi um pedido de conquista.",
        "Passei esse tempo todo tentando encontrar uma arma. O que devo levar de volta?",
        "Volte à Terra. Leve o que aprendeu. A nova era não se impõe: ela se escolhe."};

    private final EchoesLua game;
    private final CampaignState campaign;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(1280f,720f,camera);
    private final DialogueController dialogue = new DialogueController();
    private final Vector2 direction = new Vector2(), probe = new Vector2();
    private final Rectangle sanctuary = new Rectangle(
        ENTITIES[0][0]-SanctuaryZone.TALK_RADIUS, ENTITIES[0][1]-SanctuaryZone.TALK_RADIUS,
        ENTITIES[2][0]-ENTITIES[0][0]+SanctuaryZone.TALK_RADIUS*2f, SanctuaryZone.TALK_RADIUS*2f);
    private TextureRegion lightPortrait;
    private PhysicsWorld physics;
    private Astronauta player;
    private TerminalUi ui;
    private ExpeditionOverlay overlay;
    private ExpeditionPause pauseUi;
    private DialogBox dialogBox;
    private float time;
    private boolean leaving, paused, missingKey, endingStarted;

    public AharinScreen(EchoesLua game,CampaignState campaign) { this.game=game; this.campaign=campaign; }

    @Override public void show() {
        /*
         * Sem a chave a tela nao explode: informa e devolve o jogador ao menu.
         * Um pre-requisito nao atendido e trabalho da interface, nao motivo
         * para derrubar o jogo no meio de uma transicao.
         */
        boolean resumeSavedPosition = campaign.getPhase() == CampaignState.Phase.AHARIN;
        GameSaveData saved = resumeSavedPosition ? new SaveManager().load() : null;
        missingKey = !campaign.getInventario().tem(Inventario.CHAVE_LUZ);
        campaign.setPhase(CampaignState.Phase.AHARIN); Gdx.input.setInputProcessor(null);
        game.useDefaultCursor();
        physics=new PhysicsWorld();
        new Wall(-24f,0f,24f,720f,physics); new Wall(1280f,0f,24f,720f,physics);
        new Wall(0f,-24f,1280f,24f,physics); new Wall(0f,720f,1280f,24f,physics);
        player=new Astronauta(430f,275f,game.getAssets(),physics);
        if (saved != null && !saved.campanhaConcluida
                && saved.semente == campaign.getSeed()
                && CampaignState.phaseFromToken(saved.fase) == CampaignState.Phase.AHARIN
                && saved.posX >= 24f && saved.posX <= 1200f
                && saved.posY >= 24f && saved.posY <= 650f) {
            player.fromSaveData(saved);
        }
        player.setVitals(campaign.getOxygen(),campaign.getEnergy()); player.setMunicao(campaign.getAmmo());
        player.setWeaponEquipped(campaign.hasWeapon());
        camera.position.set(640f,360f,0f); camera.update();
        ui=new TerminalUi(game.getBatch(),game.getAssets()); overlay=new ExpeditionOverlay(game,player);
        pauseUi=new ExpeditionPause(game,player);
        dialogBox=new DialogBox(game.getBatch(),game.getAssets());
        var art=game.getAssets().aharinBackgroundTexture;
        // Recorte do santuario, no formato do retrato do diálogo: as tres
        // entidades enquadradas, sem esticar e sem cortar as silhuetas.
        lightPortrait=new TextureRegion(art,(int)(art.getWidth()*.585f),(int)(art.getHeight()*.235f),
            (int)(art.getWidth()*.215f),(int)(art.getHeight()*.42f));
        if (!missingKey) {
            GameSaveData save=player.toSaveData(); LunarCheckpoint.applyCampaign(save,campaign);
            new SaveManager().save(save);
        }
        game.getSounds().tocarMusicaMenu();
    }

    // =====================================================
    // TERRENO E ALCANCE
    // =====================================================

    private void footPoint(float offsetX, float offsetY, Vector2 out) {
        out.set(player.getPosition().x + 27f + offsetX, player.getPosition().y + 6f + offsetY);
    }

    private boolean walkable(float footX, float footY) { return SanctuaryZone.walkable(footX, footY); }

    private boolean inTalkRange() {
        footPoint(0f, 0f, probe);
        return SanctuaryZone.canTalk(probe.x, probe.y);
    }

    // =====================================================
    // CICLO
    // =====================================================

    @Override public void render(float delta) {
        if(leaving)return;
        delta=Math.min(delta,1f/30f); time+=delta;
        if (missingKey) { renderBlocked(); return; }

        boolean blocked=!pauseUi.isPaused()&&overlay.handleInput();
        if(!blocked)pauseUi.handle();
        paused=pauseUi.isPaused();
        if(pauseUi.consumeMenu()){leaving=true;game.setScreen(new MenuScreen(game));dispose();return;}
        if(!blocked && !paused) {
            if(dialogue.isOpen()) {
                player.getBody().setLinearVelocity(0f,0f);
                if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)||Gdx.input.isKeyJustPressed(Input.Keys.ENTER))
                    dialogue.next();
                // O encerramento dispara uma unica vez, mesmo que um quadro
                // extra ainda seja desenhado antes da troca de tela.
                if(dialogue.isFinished() && !endingStarted) {
                    endingStarted=true; leaving=true;
                    game.setScreen(new EndingScreen(game,campaign)); dispose(); return;
                }
            } else {
                direction.set((Gdx.input.isKeyPressed(Input.Keys.D)||Gdx.input.isKeyPressed(Input.Keys.RIGHT)?1f:0f)
                        -(Gdx.input.isKeyPressed(Input.Keys.A)||Gdx.input.isKeyPressed(Input.Keys.LEFT)?1f:0f),
                    (Gdx.input.isKeyPressed(Input.Keys.W)||Gdx.input.isKeyPressed(Input.Keys.UP)?1f:0f)
                        -(Gdx.input.isKeyPressed(Input.Keys.S)||Gdx.input.isKeyPressed(Input.Keys.DOWN)?1f:0f));
                if(direction.len2()>1f)direction.nor();
                if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.Q))
                    && direction.len2() > 0f) {
                    // A passarela não tem parede física; só inicia o dash se o
                    // percurso inteiro continua sobre a plataforma visível.
                    float distance = GameConfig.PLAYER_DASH_SPEED * GameConfig.PLAYER_DASH_DURATION;
                    footPoint(0f, 0f, probe);
                    if (SanctuaryZone.canTraverse(probe.x, probe.y,
                            probe.x + direction.x * distance, probe.y + direction.y * distance))
                        player.tryDash(direction.x, direction.y);
                }
                // Cada eixo e testado por si: raspar a borda do terraco desliza
                // em vez de travar o jogador contra o vazio.
                float reach = (player.isDashing() ? GameConfig.PLAYER_DASH_SPEED : 190f) * delta;
                footPoint(direction.x*reach, 0f, probe);
                if (direction.x != 0f && !walkable(probe.x, probe.y)) direction.x = 0f;
                footPoint(0f, direction.y*reach, probe);
                if (direction.y != 0f && !walkable(probe.x, probe.y)) direction.y = 0f;
                player.move(direction.x,direction.y,false,delta);
                Vector2 velocity = player.getBody().getLinearVelocity();
                footPoint(0f, 0f, probe);
                float footX = probe.x, footY = probe.y;
                if (!SanctuaryZone.canTraverse(footX, footY,
                        footX + velocity.x * GameConfig.PPM * delta,
                        footY + velocity.y * GameConfig.PPM * delta)) {
                    player.cancelDash();
                    player.getBody().setLinearVelocity(0f, 0f);
                }
                physics.update(delta); player.update(delta);
                // Aharin is a sanctuary: exploration never consumes the last oxygen.
                player.recuperarOxigenio(delta*3f);
                if(Gdx.input.isKeyJustPressed(Input.Keys.E) && inTalkRange()) {
                    dialogue.start(LINES);
                    game.getSounds().tocarDialogo();
                }
            }
        }
        draw(delta);
    }

    private void draw(float delta) {
        dialogBox.update(delta,dialogue.isOpen()); ui.clear(UiTheme.VOID);
        ui.image(game.getAssets().aharinBackgroundTexture,0f,0f,1280f,720f,Color.WHITE);
        var batch=game.getBatch(); batch.setProjectionMatrix(camera.combined); batch.begin();
        drawSanctuaryRing(batch);
        player.render(batch);
        if (!dialogue.isOpen()) drawPrompt(batch);
        batch.end();

        boolean near = inTalkRange();
        ui.beginShapes();
        ui.panel(24f,622f,730f,80f,UiTheme.CYAN);
        ui.panel(930f,622f,326f,80f,near ? UiTheme.GREEN : UiTheme.CYAN_DIM);
        ui.endShapes();
        ui.beginText();
        ui.text("AHARIN  ·  SISTEMA DE RIGEL",.68f,UiTheme.TEXT,40f,677f);
        ui.text(dialogue.isOpen() ? "ESPAÇO avança a transmissão."
            : "Siga a passarela até o santuário e fale com as entidades de Luz.",
            .59f,UiTheme.TEXT_MUTED,40f,646f);
        ui.centered(near ? "E  ·  FALAR" : "APROXIME-SE DO SANTUÁRIO", .58f,
            near ? UiTheme.GREEN : UiTheme.TEXT_MUTED, 1093f, 657f);
        ui.endText();

        overlay.render();
        batch.setProjectionMatrix(camera.combined); batch.begin();
        dialogBox.render(dialogue,"ENTIDADES DE LUZ",lightPortrait); batch.end();
        pauseUi.render();
    }

    /** Marca o ponto de chegada: o santuário deixa de ser um número no código. */
    private void drawSanctuaryRing(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
        float pulse = .18f + MathUtils.sin(time * 1.6f) * .06f;
        batch.setColor(.68f,.86f,1f,pulse);
        for (float[] entity : ENTITIES)
            AtlasRegionRenderer.draw(batch,game.getAssets().uiWhiteTexture,
                entity[0]-52f,entity[1]-14f,104f,26f);
        batch.setColor(Color.WHITE);
    }

    private void drawPrompt(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
        float bob = MathUtils.sin(time * 3.2f) * 5f;
        boolean near = inTalkRange();
        batch.setColor(1f,1f,1f,near ? .95f : .55f);
        float x = near ? ENTITIES[1][0] : sanctuary.x + sanctuary.width / 2f;
        AtlasRegionRenderer.draw(batch,game.getAssets().uiObjectiveMarkerTexture,
            x-22f, ENTITIES[1][1]+118f+bob, 44f, 44f);
        batch.setColor(Color.WHITE);
    }

    /** Tela honesta para o pré-requisito ausente, em vez de exceção na troca. */
    private void renderBlocked() {
        ui.clear(UiTheme.VOID);
        ui.image(game.getAssets().aharinBackgroundTexture,0f,0f,1280f,720f,new Color(.35f,.4f,.5f,1f));
        ui.beginShapes(); ui.panel(300f,250f,680f,220f,UiTheme.RED); ui.endShapes();
        ui.beginText();
        ui.title("PORTAL DOURADO BLOQUEADO",.8f,UiTheme.TEXT,336f,420f);
        ui.textWrapped("A Chave de Luz ainda não existe. Derrote as três formas do Sentinela de Calisto "
            + "para abrir a passagem até Aharin.",.56f,UiTheme.TEXT_MUTED,336f,368f,600f);
        ui.text("ESPAÇO ou ENTER para voltar ao menu",.52f,UiTheme.CYAN,336f,282f);
        ui.endText();
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            leaving = true; game.setScreen(new MenuScreen(game)); dispose();
        }
    }

    @Override public void resize(int w,int h){viewport.update(w,h);if(ui!=null)ui.resize(w,h);if(overlay!=null)overlay.resize(w,h);if(pauseUi!=null)pauseUi.resize(w,h);}
    @Override public void pause(){if(pauseUi!=null)pauseUi.pause();}
    @Override public void resume(){}
    @Override public void hide(){}
    @Override public void dispose(){if(overlay!=null){overlay.dispose();overlay=null;}if(pauseUi!=null){pauseUi.dispose();pauseUi=null;}if(ui!=null){ui.dispose();ui=null;}if(physics!=null){physics.dispose();physics=null;}}
}

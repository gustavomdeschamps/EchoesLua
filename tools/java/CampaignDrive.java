import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.save.GameSaveData;
import com.orion.echoes.lua.save.LunarCheckpoint;
import com.orion.echoes.lua.save.SaveManager;
import com.orion.echoes.lua.screens.*;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.systems.Inventario;
import com.orion.echoes.lua.systems.MissionState;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Percorre a campanha inteira com as telas de producao e input sintetico.
 *
 * Existe porque teste unitario de modelo nao prova integracao: as falhas que o
 * jogador encontrou estavam na ligacao entre tela, inventario, save e portal.
 * Aqui cada chave e conquistada pelo caminho real (dano no chefe, morte do
 * chefe, portal), e nenhuma flag de debug e ligada.
 */
public final class CampaignDrive extends EchoesLua {

    private final Deque<Step> steps = new ArrayDeque<>();
    private final List<String> failures = new ArrayList<>();
    private final List<String> log = new ArrayList<>();
    private Step current;
    private int frame;
    private boolean started, done;

    private final Set<Integer> held = new HashSet<>();
    private final Set<Integer> tapped = new HashSet<>();
    private boolean click;
    private int mouseX = 640, mouseY = 360;
    private Input real;
    private Input proxy;

    // =====================================================
    // INFRAESTRUTURA
    // =====================================================

    private interface Body { boolean run(int tick); }

    private static final class Step {
        final String name; final Body body; final int limit; int tick;
        Step(String name, int limit, Body body) { this.name = name; this.limit = limit; this.body = body; }
    }

    private void step(String name, int limit, Body body) { steps.add(new Step(name, limit, body)); }
    private void once(String name, Runnable action) {
        step(name, 1, tick -> { action.run(); return true; });
    }
    private void check(String what, boolean condition) {
        if (condition) log.add("OK   " + what);
        else { failures.add(what); log.add("FALHA " + what); }
    }
    private void note(String text) { log.add("     " + text); }

    private void tap(int key) { tapped.add(key); }
    private void hold(int key) { held.add(key); }
    private void release(int key) { held.remove(key); }

    /** Entrega tecla tambem ao InputProcessor da tela, quando existe. */
    private void keyEvent(int key, boolean down) {
        InputProcessor processor = real.getInputProcessor();
        if (processor == null) return;
        if (down) processor.keyDown(key); else processor.keyUp(key);
    }

    private void shot(String name) {
        Pixmap pixels = ScreenUtils.getFrameBufferPixmap(0, 0, 1280, 720);
        var file = Gdx.files.local("../build/campaign-drive/" + name + ".png");
        file.parent().mkdirs();
        PixmapIO.writePNG(file, pixels, -1, true);
        pixels.dispose();
    }

    private Screen screen() { return getScreen(); }
    private String screenName() { Screen s = screen(); return s == null ? "null" : s.getClass().getSimpleName(); }

    private void swap(Screen next) {
        Screen previous = getScreen();
        setScreen(next);
        if (previous != null) previous.dispose();
    }

    @SuppressWarnings("unchecked")
    private <T> T peek(Object target, String field) {
        try {
            Class<?> type = target.getClass();
            while (type != null) {
                try { Field f = type.getDeclaredField(field); f.setAccessible(true); return (T) f.get(target); }
                catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
            }
        } catch (ReflectiveOperationException e) { failures.add("reflexao " + field + ": " + e); }
        failures.add("campo ausente: " + field);
        return null;
    }

    // =====================================================
    // INPUT SINTETICO
    // =====================================================

    private void installProxy() {
        Input live = Gdx.input;
        if (live == proxy) return;
        real = live;
        InvocationHandler handler = (p, method, args) -> {
            switch (method.getName()) {
                case "isKeyPressed":
                    return held.contains((Integer) args[0]) || tapped.contains((Integer) args[0]);
                case "isKeyJustPressed":
                    return tapped.contains((Integer) args[0]);
                case "getX": case "getDeltaX": return method.getName().equals("getX") ? mouseX : 0;
                case "getY": case "getDeltaY": return method.getName().equals("getY") ? mouseY : 0;
                case "isButtonJustPressed": case "isButtonPressed":
                    return click && (Integer) args[0] == Input.Buttons.LEFT;
                case "isTouched": case "justTouched":
                    return click;
                default:
                    return method.invoke(real, args);
            }
        };
        proxy = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class}, handler);
        Gdx.input = proxy;
        Gdx.app.getApplicationListener();
    }

    // =====================================================
    // ROTEIRO
    // =====================================================

    private CampaignState campaign;

    /** Progresso lunar preparado pela API de producao, sem cheat de chave. */
    private CampaignState lunarReadyCampaign() {
        CampaignState state = new CampaignState(4242L);
        MissionState mission = new MissionState();
        mission.restore(1, 1, 1, 1, 1, 1, 1, true, true, true, true, true, 6);
        mission.setTotalEnemies(6);
        state.captureMission(mission);
        state.setAmmo(60);
        state.setVitals(100f, 100f);
        return state;
    }

    private void buildScript() {
        // ---------- LUA ----------
        once("prepara campanha lunar", () -> {
            campaign = lunarReadyCampaign();
            setCampaign(campaign);
            check("luaMissoesOk verdadeiro apos missoes lunares", campaign.luaMissoesOk());
            check("nenhuma chave antes do chefe", campaign.getInventario().exportarChaves().length == 0);
        });
        once("abre arena do Guardiao da Cratera", () -> swap(new PhaseBossScreen(this, campaign, false)));
        step("arena lunar assenta", 104, tick -> tick >= 64);
        once("entrar na arena nao concede chave", () ->
            check("entrar na arena lunar nao concede CHAVE_LUA",
                !campaign.getInventario().tem(Inventario.CHAVE_LUA)));
        once("captura arena lunar", () -> shot("01-arena-lua"));
        once("B abre bestiario de save novo", () -> tap(Input.Keys.B));
        step("bestiario inicial desenha", 24, tick -> tick >= 14);
        once("confirma bestiario bloqueado", () -> {
            Object overlay = peek(screen(), "overlay");
            Integer tab = overlay == null ? null : peek(overlay, "panel");
            check("B abre a Central diretamente no Bestiario", tab != null && tab == 3);
            check("save novo nao revela chefe lunar", !campaign.isBossDefeated(CampaignState.Phase.LUNAR));
            shot("01f-bestiario-inicial");
        });
        once("I fecha Central mesmo na aba Bestiario", () -> tap(Input.Keys.I));
        step("Central fecha", 12, tick -> tick >= 6);
        once("confirma fechamento pela tecla I", () -> {
            Object overlay = peek(screen(), "overlay");
            Integer tab = overlay == null ? null : peek(overlay, "panel");
            check("I fecha a Central de qualquer aba", tab != null && tab == 0);
        });
        step("dash lunar avanca", 20, tick -> {
            if (tick == 0) {
                Astronauta player = peek(screen(), "player");
                arenaDashStartX = player.getPosition().x;
                arenaDashStartEnergy = player.getEnergia();
                tap(Input.Keys.SPACE);
            }
            hold(Input.Keys.D);
            return tick >= 11;
        });
        once("verifica dash da arena", () -> {
            Astronauta player = peek(screen(), "player");
            check("ESPAÇO executa dash na arena e consome energia",
                player.getPosition().x > arenaDashStartX + 38f
                    && player.getEnergia() < arenaDashStartEnergy - 12f);
        });

        // ---------- CHECKLIST: CORPO, MIRA E CORRIDA ----------
        step("anda para a direita mirando a esquerda", 60, tick -> {
            hold(Input.Keys.D); mouseX = 40; mouseY = 360;
            return tick >= 40;
        });
        once("mira esquerda com passo a direita", () -> {
            Astronauta player = peek(screen(), "player");
            check("corpo segue o movimento (direita) com mouse a esquerda",
                player != null && !player.isViradoEsquerda());
            check("mira aponta para a esquerda",
                player != null && Math.abs(player.getAimAngle()) > 140f);
            shot("01a-anda-direita-mira-esquerda");
            release(Input.Keys.D);
        });
        step("anda para a esquerda mirando a direita", 60, tick -> {
            hold(Input.Keys.A); mouseX = 1240; mouseY = 360;
            return tick >= 40;
        });
        once("mira direita com passo a esquerda", () -> {
            Astronauta player = peek(screen(), "player");
            check("corpo segue o movimento (esquerda) com mouse a direita",
                player != null && player.isViradoEsquerda());
            check("mira aponta para a direita",
                player != null && Math.abs(player.getAimAngle()) < 40f);
            shot("01b-anda-esquerda-mira-direita");
            release(Input.Keys.A);
        });
        step("para de andar", 40, tick -> tick >= 30);
        once("orientacao estavel ao parar", () -> {
            Astronauta player = peek(screen(), "player");
            check("parado mantem a ultima orientacao valida",
                player != null && player.isViradoEsquerda());
        });
        once("abre pausa para inspecao visual", () -> tap(Input.Keys.ESCAPE));
        step("pausa assenta", 80, tick -> tick >= 45);
        once("captura pausa", () -> shot("01d-pausa"));
        once("abre ajustes da pausa", () -> tap(Input.Keys.O));
        step("ajustes da pausa assentam", 45, tick -> tick >= 25);
        once("captura ajustes da pausa", () -> shot("01e-ajustes-pausa"));
        once("clica em retomar no menu de ajustes", () -> {
            mouseX = 485; mouseY = 720 - 96; click = true;
        });
        step("retomada por clique assenta", 16, tick -> tick >= 9);
        once("confirma retomada por clique", () -> {
            Object pause = peek(screen(), "pauseUi");
            check("botao RETOMAR da pausa funciona com mouse", pause != null && !paused(pause));
        });
        once("drena a energia", () -> {
            Astronauta player = peek(screen(), "player");
            if (player != null) player.setVitals(player.getOxigenio(), 1f);
            sprintFlips = 0; sprintWas = false;
        });
        step("corre sem energia", 150, tick -> {
            hold(Input.Keys.D); hold(Input.Keys.SHIFT_LEFT);
            Astronauta player = peek(screen(), "player");
            if (player != null && player.isSprinting() != sprintWas) {
                sprintWas = player.isSprinting(); sprintFlips++;
            }
            return tick >= 120;
        });
        once("corrida sem energia e estavel", () -> {
            release(Input.Keys.D); release(Input.Keys.SHIFT_LEFT);
            check("segurar Shift sem energia nao alterna corrida a cada quadro (" + sprintFlips + " trocas)",
                sprintFlips <= 2);
        });

        // ---------- CHECKLIST: MOCHILA CONGELA O COMBATE ----------
        once("abre a mochila no meio do combate", () -> {
            Astronauta player = peek(screen(), "player");
            Object boss = peek(screen(), "boss");
            frozenX = player == null ? 0f : player.getPosition().x;
            frozenY = player == null ? 0f : player.getPosition().y;
            frozenAmmo = player == null ? 0 : player.getMunicao();
            frozenHp = boss == null ? 0f : hp(boss);
            tap(Input.Keys.I);
        });
        step("segura SPACE com a mochila aberta", 60, tick -> {
            tap(Input.Keys.SPACE); tap(Input.Keys.E);
            hold(Input.Keys.D);
            return tick >= 40;
        });
        once("nada vazou da mochila", () -> {
            release(Input.Keys.D);
            Astronauta player = peek(screen(), "player");
            Object boss = peek(screen(), "boss");
            check("jogador nao anda com a mochila aberta",
                player != null && Math.abs(player.getPosition().x - frozenX) < 2f
                    && Math.abs(player.getPosition().y - frozenY) < 2f);
            check("SPACE nao dispara com a mochila aberta",
                player != null && player.getMunicao() == frozenAmmo);
            check("chefe nao toma dano com a mochila aberta",
                boss != null && hp(boss) == frozenHp);
            shot("01c-mochila-congela");
        });
        once("ESC fecha a mochila", () -> tap(Input.Keys.ESCAPE));
        step("confirma que a pausa nao abriu", 20, tick -> tick >= 8);
        once("ESC nao encadeia mochila e pausa", () -> {
            Object pause = peek(screen(), "pauseUi");
            check("ESC fecha a mochila sem abrir a pausa no mesmo toque",
                pause != null && !paused(pause));
        });
        step("caminha ate o portal leste (sem chave)", 700, tick -> {
            Astronauta player = peek(screen(), "player");
            if (player == null) return true;
            hold(Input.Keys.D);
            if (player.getPosition().x > 1100f) { release(Input.Keys.D); return true; }
            return false;
        });
        once("E no portal bloqueado", () -> { release(Input.Keys.D); tap(Input.Keys.E); });
        step("confirma bloqueio", 20, tick -> tick >= 8);
        once("portal bloqueado sem chave", () -> {
            check("portal da arena lunar barra sem chave", screen() instanceof PhaseBossScreen);
            shot("02-portal-bloqueado");
        });
        step("derrota o Guardiao da Cratera", 6000, tick -> {
            if (!(screen() instanceof PhaseBossScreen)) { failures.add("saiu da arena lunar: " + screenName()); return true; }
            fightArena(tick);
            if (tick == 80) shot("02a-guardiao-em-combate");
            return campaign.getInventario().tem(Inventario.CHAVE_LUA);
        });
        once("chave da Lua conquistada", () -> {
            check("CHAVE_LUA concedida ao derrotar o chefe", campaign.getInventario().tem(Inventario.CHAVE_LUA));
            check("nenhuma chave adiantada na Lua", !campaign.getInventario().tem(Inventario.CHAVE_MARTE)
                && !campaign.getInventario().tem(Inventario.CHAVE_TITA)
                && !campaign.getInventario().tem(Inventario.CHAVE_LUZ));
            shot("03-chave-lua");
        });
        once("abre mochila", () -> tap(Input.Keys.I));
        step("mochila desenha", 30, tick -> tick >= 12);
        once("captura mochila", () -> shot("04-mochila"));
        once("abre mapa", () -> tap(Input.Keys.M));
        step("mapa desenha", 30, tick -> tick >= 12);
        once("captura mapa", () -> { shot("05-mapa"); tap(Input.Keys.M); });

        // ---------- MARTE ----------
        once("prepara Marte", () -> {
            campaign.setPhase(CampaignState.Phase.MARS);
            campaign.setDialogoTita(true);
            campaign.setCombateOk(true);
            campaign.setMarsProgress(3, 3, 9, true);
            campaign.setAmmo(60);
            campaign.setVitals(100f, 100f);
            check("marteMissoesOk verdadeiro", campaign.marteMissoesOk());
        });
        once("abre arena do Tita-Ferrugem", () -> swap(new PhaseBossScreen(this, campaign, true)));
        step("arena marciana assenta", 104, tick -> tick >= 64);
        once("entrar na arena de Marte nao concede chave", () ->
            check("entrar na arena de Marte nao concede CHAVE_MARTE",
                !campaign.getInventario().tem(Inventario.CHAVE_MARTE)));
        once("captura arena marciana", () -> shot("06-arena-marte"));
        step("derrota o Tita-Ferrugem", 6000, tick -> {
            if (!(screen() instanceof PhaseBossScreen)) { failures.add("saiu da arena marciana: " + screenName()); return true; }
            fightArena(tick);
            return campaign.getInventario().tem(Inventario.CHAVE_MARTE);
        });
        once("chave de Marte conquistada", () ->
            check("CHAVE_MARTE concedida ao derrotar o chefe", campaign.getInventario().tem(Inventario.CHAVE_MARTE)));

        // ---------- TITA ----------
        once("atravessa o portal real de Marte", () -> {
            Astronauta player = peek(screen(), "player");
            GameSaveData atPortal = player.toSaveData();
            atPortal.posX = 1130f;
            atPortal.posY = 220f;
            player.fromSaveData(atPortal);
            tap(Input.Keys.E);
        });
        step("abertura e carregamento de Tita", 500, tick -> screen() instanceof TitanScreen);
        once("travessia Marte para Tita preserva campanha", () -> {
            check("portal marciano abriu Tita sem encerrar o jogo", screen() instanceof TitanScreen);
            check("fase atual passa a ser Tita", campaign.getPhase() == CampaignState.Phase.TITAN);
            check("CHAVE_MARTE segue no inventario", campaign.getInventario().tem(Inventario.CHAVE_MARTE));
            campaign.setDialogoExplorador(true);
            campaign.setAmmo(120);
            campaign.setVitals(100f, 100f);
        });
        step("Tita assenta", 120, tick -> tick >= 80);
        once("abre o mapa em Tita", () -> tap(Input.Keys.M));
        step("mapa de Tita desenha sem fechar", 30, tick -> tick >= 12);
        once("fecha o mapa de Tita", () -> {
            check("mapa de Tita manteve a fase ativa", screen() instanceof TitanScreen);
            shot("07a-mapa-tita");
            tap(Input.Keys.M);
        });
        step("Tita volta ao jogo", 10, tick -> tick >= 5);
        once("captura Tita", () -> {
            shot("07-tita");
            check("entrar em Tita nao concede CHAVE_TITA", !campaign.getInventario().tem(Inventario.CHAVE_TITA));
            com.orion.echoes.lua.entities.TitanPortal exit = peek(screen(), "calistoPortal");
            check("arte do portal de Tita cabe inteira no mapa", exit != null
                && exit.getPosition().x >= 0f && exit.getPosition().x + 190f < 2600f
                && exit.getPosition().y >= 0f && exit.getPosition().y + 220f < 1700f);
        });
        step("elimina cacadores e Soberano de Tita", 20000, tick -> {
            Object bossObj = peek(screen(), "boss");
            if (bossObj == null) return true;
            damageTitan(bossObj);
            return campaign.getInventario().tem(Inventario.CHAVE_TITA);
        });
        once("chave de Tita conquistada", () -> {
            check("CHAVE_TITA concedida so apos o Soberano cair",
                campaign.getInventario().tem(Inventario.CHAVE_TITA));
            check("CHAVE_LUZ ainda nao existe em Tita", !campaign.getInventario().tem(Inventario.CHAVE_LUZ));
            shot("08-chave-tita");
        });
        once("aproxima a camera do portal de Tita", () -> {
            Astronauta player = peek(screen(), "player");
            GameSaveData nearExit = player.toSaveData();
            nearExit.posX = 2050f; nearExit.posY = 1470f;
            player.fromSaveData(nearExit);
            com.badlogic.gdx.graphics.OrthographicCamera camera = peek(screen(), "camera");
            camera.position.set(1960f, 1340f, 0f);
            camera.update();
            Object director = peek(screen(), "cameraDirector");
            com.badlogic.gdx.math.Vector2 smoothPosition = peek(director, "basePosition");
            smoothPosition.set(1960f, 1340f);
        });
        step("camera enquadra o portal de Tita", 80, tick -> tick >= 60);
        once("captura portal de Tita", () -> shot("08a-portal-tita"));

        // ---------- CALISTO ----------
        once("abre Calisto", () -> {
            campaign.setAmmo(200);
            campaign.setVitals(100f, 100f);
            swap(new CallistoScreen(this, campaign));
        });
        step("Calisto assenta", 104, tick -> tick >= 64);
        once("estado inicial de Calisto", () -> {
            Object boss = peek(screen(), "boss");
            check("Calisto comeca na forma 1", boss != null && forma(boss) == 1);
            check("entrar em Calisto nao concede CHAVE_LUZ", !campaign.getInventario().tem(Inventario.CHAVE_LUZ));
            shot("09-calisto-forma1");
        });
        step("derruba a forma 1", 9000, tick -> {
            if (!(screen() instanceof CallistoScreen)) { failures.add("saiu de Calisto: " + screenName()); return true; }
            fightArena(tick);
            Object boss = peek(screen(), "boss");
            return boss != null && forma(boss) >= 2;
        });
        once("forma 2 alcancada", () -> {
            Object boss = peek(screen(), "boss");
            check("forma 2 surge na mesma entidade", boss != null && forma(boss) == 2);
            check("CHAVE_LUZ nao sai na forma 1", !campaign.getInventario().tem(Inventario.CHAVE_LUZ));
            shot("10-calisto-forma2");
        });
        step("derruba a forma 2", 9000, tick -> {
            if (!(screen() instanceof CallistoScreen)) { failures.add("saiu de Calisto: " + screenName()); return true; }
            fightArena(tick);
            Object boss = peek(screen(), "boss");
            return boss != null && forma(boss) >= 3;
        });
        once("forma 3 alcancada", () -> {
            Object boss = peek(screen(), "boss");
            check("forma 3 surge na mesma entidade", boss != null && forma(boss) == 3);
            check("CHAVE_LUZ nao sai na forma 2", !campaign.getInventario().tem(Inventario.CHAVE_LUZ));
            shot("11-calisto-forma3");
        });
        step("derruba a forma 3", 12000, tick -> {
            if (!(screen() instanceof CallistoScreen)) { failures.add("saiu de Calisto: " + screenName()); return true; }
            fightArena(tick);
            return campaign.getInventario().tem(Inventario.CHAVE_LUZ);
        });
        once("chave de Luz conquistada", () -> {
            check("CHAVE_LUZ concedida so na terceira morte", campaign.getInventario().tem(Inventario.CHAVE_LUZ));
            shot("12-chave-luz");
        });

        // ---------- SAVE / CONTINUAR ----------
        once("verifica o save gravado em Calisto", () -> {
            GameSaveData data = new SaveManager().load();
            check("save existe apos Calisto", data != null);
            if (data == null) return;
            note("save: fase=" + data.fase + " versao=" + data.versao
                + " chaves=" + String.join(",", data.inventario));
            CampaignState reloaded = LunarCheckpoint.toCampaign(data);
            check("save preserva CHAVE_LUZ", reloaded.getInventario().tem(Inventario.CHAVE_LUZ));
            check("save preserva CHAVE_TITA", reloaded.getInventario().tem(Inventario.CHAVE_TITA));
            check("save preserva CHAVE_LUA", reloaded.getInventario().tem(Inventario.CHAVE_LUA));
            check("save preserva CHAVE_MARTE", reloaded.getInventario().tem(Inventario.CHAVE_MARTE));
            check("save preserva nivel de arma", reloaded.getInventario().getNivelArma()
                == campaign.getInventario().getNivelArma());
            check("save preserva forma final de Calisto", reloaded.getFormaBossCalisto() == 3);
        });

        // ---------- AHARIN ----------
        once("abre Aharin", () -> swap(new AharinScreen(this, campaign)));
        step("Aharin assenta", 104, tick -> tick >= 64);
        once("captura Aharin", () -> shot("13-aharin"));
        step("dash de Aharin avanca", 20, tick -> {
            if (tick == 0) {
                Astronauta player = peek(screen(), "player");
                sanctuaryDashStartX = player.getPosition().x;
                tap(Input.Keys.SPACE);
            }
            hold(Input.Keys.D);
            return tick >= 11;
        });
        once("verifica dash de Aharin", () -> {
            Astronauta player = peek(screen(), "player");
            check("ESPAÇO executa dash seguro na passarela de Aharin",
                player.getPosition().x > sanctuaryDashStartX + 38f);
        });
        once("E longe das entidades", () -> tap(Input.Keys.E));
        step("confirma que nada abre", 20, tick -> tick >= 8);
        once("conversa nao dispara a meio mapa", () -> {
            Object dialogue = peek(screen(), "dialogue");
            check("E longe do santuario nao abre o dialogo", dialogue != null && !open(dialogue));
        });
        step("caminha ate o santuario", 900, tick -> {
            Astronauta player = peek(screen(), "player");
            if (player == null) return true;
            hold(Input.Keys.D);
            if (player.getPosition().x > 700f) hold(Input.Keys.W);
            if (tick % 20 == 10) tap(Input.Keys.E);
            Object dialogue = peek(screen(), "dialogue");
            return dialogue != null && open(dialogue);
        });
        once("dialogo aberto", () -> {
            release(Input.Keys.D); release(Input.Keys.W);
            Object dialogue = peek(screen(), "dialogue");
            check("entidades de Luz abrem dialogo", dialogue != null && open(dialogue));
            shot("14-aharin-dialogo");
        });
        step("avanca as tres falas", 400, tick -> {
            if (!(screen() instanceof AharinScreen)) return true;
            if (tick % 12 == 0) tap(Input.Keys.SPACE);
            return false;
        });
        once("encerramento disparado", () -> {
            check("terceira fala leva ao EndingScreen", screen() instanceof EndingScreen);
            note("tela apos Aharin: " + screenName());
        });
        step("assiste o encerramento", 1200, tick -> screen() instanceof VictoryScreen);
        once("vitoria alcancada", () -> {
            check("encerramento leva a VictoryScreen", screen() instanceof VictoryScreen);
            note("tela final: " + screenName());
        });
        step("vitoria assenta", 200, tick -> tick >= 130);
        once("captura vitoria", () -> shot("15-vitoria"));
        once("abre menu para inspecao visual", () -> swap(new MenuScreen(this)));
        step("menu assenta", 100, tick -> tick >= 65);
        once("captura menu", () -> shot("16-menu"));
        once("posiciona mouse sobre botao", () -> { mouseX = 300; mouseY = 275; });
        step("hover do menu assenta", 35, tick -> tick >= 18);
        once("captura hover do menu", () -> shot("16b-menu-hover"));
        once("abre configuracoes para inspecao visual", () -> {
            try {
                Method method = MenuScreen.class.getDeclaredMethod("showSettings", boolean.class);
                method.setAccessible(true);
                method.invoke(screen(), false);
            } catch (ReflectiveOperationException error) {
                failures.add("nao abriu configuracoes do menu: " + error);
            }
        });
        step("configuracoes assentam", 60, tick -> tick >= 35);
        once("captura configuracoes", () -> shot("17-configuracoes"));
        once("fim", () -> done = true);
    }

    private boolean open(Object dialogue) {
        try {
            Method m = dialogue.getClass().getMethod("isOpen");
            return (Boolean) m.invoke(dialogue);
        } catch (ReflectiveOperationException e) { return false; }
    }

    private int forma(Object boss) {
        try { return (Integer) boss.getClass().getMethod("getForma").invoke(boss); }
        catch (ReflectiveOperationException e) { return -1; }
    }

    /** Em Tita o jogador precisa limpar os cacadores antes do chefe. */
    private void damageTitan(Object boss) {
        Screen s = screen();
        com.badlogic.gdx.utils.Array<?> enemies = peek(s, "enemies");
        if (enemies != null) {
            for (Object enemy : enemies) {
                try {
                    Method alive = enemy.getClass().getMethod("isAlive");
                    if ((Boolean) alive.invoke(enemy))
                        enemy.getClass().getMethod("receiveDamage", float.class).invoke(enemy, 400f);
                } catch (ReflectiveOperationException ignored) { }
            }
        }
        try {
            Method alive = boss.getClass().getMethod("isAlive");
            if ((Boolean) alive.invoke(boss))
                boss.getClass().getMethod("receiveDamage", float.class).invoke(boss, 6f);
        } catch (ReflectiveOperationException ignored) { }
        if (!alive(boss)) {
            com.badlogic.gdx.math.Rectangle key = peek(s, "titanKeyPickup");
            Astronauta player = peek(s, "player");
            if (key != null && player != null && key.width > 0f) {
                GameSaveData atKey = player.toSaveData();
                atKey.posX = key.x + 12f; atKey.posY = key.y + 12f;
                player.fromSaveData(atKey);
            }
        }
    }



    private int sprintFlips, frozenAmmo;
    private float arenaDashStartX, arenaDashStartEnergy, sanctuaryDashStartX;
    private boolean workshopShot;
    private boolean sprintWas;
    private float frozenX, frozenY, frozenHp;

    private float hp(Object boss) {
        try { return (Float) boss.getClass().getMethod("getHp").invoke(boss); }
        catch (ReflectiveOperationException e) { return -1f; }
    }

    private boolean paused(Object pause) {
        try { return (Boolean) pause.getClass().getMethod("isPaused").invoke(pause); }
        catch (ReflectiveOperationException e) { return true; }
    }

    // =====================================================
    // COMBATE DE ARENA
    // =====================================================

    /**
     * Joga a arena como um jogador competente jogaria.
     *
     * Ficar parado atirando morre - e deve morrer. Para o teste dizer alguma
     * coisa sobre balanceamento, o piloto se afasta do chefe, atira na cadencia
     * da arma e volta a estacao de apoio quando o pente ou o traje acabam.
     */
    private void fightArena(int tick) {
        Astronauta player = peek(screen(), "player");
        Object boss = peek(screen(), "boss");
        if (player == null || boss == null) return;
        Object workshop = peek(screen(), "workshop");
        if (workshop != null && open(workshop)) {
            held.clear();
            if (!workshopShot) { shot("03a-interior-estacao"); workshopShot = true; }
            if (tick % 10 == 0) tap(Input.Keys.R);
            else if (tick % 10 == 2) tap(Input.Keys.E);
            return;
        }
        float px = player.getPosition().x, py = player.getPosition().y;
        float bx = bossCenter(boss, true), by = bossCenter(boss, false);
        // A chave agora e um coletavel fisico. Depois da queda do chefe, o
        // piloto precisa caminhar ate o corpo em vez de esperar uma recompensa
        // invisivel cair direto no inventario.
        if (!alive(boss)) {
            held.clear();
            if (Math.abs(px + 27f - bx) > 18f) hold(px + 27f < bx ? Input.Keys.D : Input.Keys.A);
            if (Math.abs(py + 30f - by) > 18f) hold(py + 30f < by ? Input.Keys.W : Input.Keys.S);
            return;
        }
        boolean resupply = player.getMunicao() <= 2 || player.getOxigenio() < 45f;
        held.clear();
        if (resupply) {
            if (px > 210f) hold(Input.Keys.A); else if (px < 150f) hold(Input.Keys.D);
            if (py > 190f) hold(Input.Keys.S); else if (py < 130f) hold(Input.Keys.W);
            if (px <= 230f && py <= 210f && tick % 12 == 0) tap(Input.Keys.E);
            return;
        }
        // Mantem distancia: o golpe do chefe alcanca 145 unidades.
        float dx = px + 27f - bx, dy = py + 30f - by;
        if (dx * dx + dy * dy < 260f * 260f) {
            if (Math.abs(dx) > Math.abs(dy)) hold(dx < 0f ? Input.Keys.A : Input.Keys.D);
            else hold(dy < 0f ? Input.Keys.S : Input.Keys.W);
        }
        mouseX = (int) MathUtils.clamp(bx, 0f, 1279f);
        mouseY = (int) MathUtils.clamp(720f - by, 0f, 719f);
        if (tick % 16 == 0) click = true;
    }

    private boolean alive(Object boss) {
        try { return (Boolean) boss.getClass().getMethod("isAlive").invoke(boss); }
        catch (ReflectiveOperationException e) { return false; }
    }

    private float bossCenter(Object boss, boolean horizontal) {
        try {
            Method m = boss.getClass().getMethod(horizontal ? "centerX" : "centerY");
            return (Float) m.invoke(boss);
        } catch (ReflectiveOperationException e) { return 640f; }
    }

    // =====================================================
    // LOOP
    // =====================================================

    @Override public void render() {
        installProxy();
        try {
            super.render();
        } catch (RuntimeException crash) {
            failures.add("EXCECAO em " + (current == null ? "?" : current.name)
                + " [" + screenName() + "]: " + crash);
            crash.printStackTrace();
            finish();
            return;
        }
        tapped.clear();
        click = false;
        if (done) return;
        if (!getAssets().isReady() || getScreen() instanceof LoadingScreen) return;
        if (!started) { started = true; buildScript(); }
        frame++;
        if (current == null) {
            if (steps.isEmpty()) { finish(); return; }
            current = steps.poll();
        }
        boolean complete;
        try {
            complete = current.body.run(current.tick);
        } catch (RuntimeException crash) {
            failures.add("EXCECAO no passo '" + current.name + "' [" + screenName() + "]: " + crash);
            crash.printStackTrace();
            finish();
            return;
        }
        current.tick++;
        if (!complete && current.tick >= current.limit) {
            failures.add("ESTOUROU o limite no passo '" + current.name + "' [" + screenName() + "]");
            complete = true;
        }
        if (complete) { held.clear(); current = null; }
        if (done) finish();
    }

    private void finish() {
        done = true;
        System.out.println("==== RELATORIO DA CAMPANHA ====");
        for (String line : log) System.out.println(line);
        System.out.println("---- FALHAS: " + failures.size() + " ----");
        for (String line : failures) System.out.println("  * " + line);
        System.out.println("==== FIM (" + frame + " frames) ====");
        if (!failures.isEmpty()) throw new AssertionError("Campanha: " + failures.size() + " falhas");
        Gdx.app.exit();
    }

    public static void main(String[] args) {
        System.setProperty("echoes.windowed", "true");
        var config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280, 720);
        config.setInitialVisible(false);
        config.disableAudio(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new CampaignDrive(), config);
    }
}

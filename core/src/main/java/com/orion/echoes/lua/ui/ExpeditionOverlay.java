package com.orion.echoes.lua.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.orion.echoes.lua.EchoesLua;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.systems.CampaignState;
import com.orion.echoes.lua.systems.Inventario;

/** Screen-space inventory and route map shared by every expedition world. */
public final class ExpeditionOverlay {

    /** Um corpo da rota: nome, fase e a chave que abre a travessia até ele. */
    private enum Stop {
        LUA("LUA", CampaignState.Phase.LUNAR, null, "Origem da expedição"),
        MARTE("MARTE", CampaignState.Phase.MARS, Inventario.CHAVE_LUA, "Colônia e estufa"),
        TITA("TITÃ", CampaignState.Phase.TITAN, Inventario.CHAVE_MARTE, "Metano e névoa"),
        CALISTO("CALISTO", CampaignState.Phase.CALLISTO, Inventario.CHAVE_TITA, "Cratera de gelo"),
        AHARIN("AHARIN", CampaignState.Phase.AHARIN, Inventario.CHAVE_LUZ, "Sistema de Rigel");
        final String name; final CampaignState.Phase phase; final String key; final String hint;
        Stop(String name, CampaignState.Phase phase, String key, String hint) {
            this.name = name; this.phase = phase; this.key = key; this.hint = hint;
        }
    }

    private static final Color SCRIM = new Color(0f, 0f, 0f, .74f);
    private static final Color MAP_LABEL_BACK = new Color(.015f, .025f, .04f, .72f);

    private final EchoesLua game;
    private final Astronauta player;
    private final TerminalUi ui;
    private final TextureRegion starChart;
    private final TextureRegion playerPortrait;
    private final TextureRegion mapAstronaut;
    private final TextureRegion energyCellRegion;
    private static final float[] ROUTE_X = {213f, 410f, 640f, 842f, 1070f};
    private static final float[] ROUTE_Y = {353f, 353f, 353f, 353f, 353f};
    private final Vector2 pointer = new Vector2();
    private int dragSource = -1;
    private boolean dragging, leftDownLastFrame;
    private float dragStartX, dragStartY, mapTime;
    private final Color mapSignal = new Color(.37f, .76f, .98f, 1f);
    private int panel; // 0 closed, 1 inventory, 2 map, 3 bestiary
    private Stop selectedStop;
    private String message = "";
    private float messageTimer;

    public ExpeditionOverlay(EchoesLua game, Astronauta player) {
        this.game = game; this.player = player;
        player.setInventario(game.getCampaign().getInventario());
        ui = new TerminalUi(game.getBatch(), game.getAssets());
        starChart = new TextureRegion(game.getAssets().expeditionStarChartTexture);
        playerPortrait = new TextureRegion(game.getAssets().playerPortraitTexture);
        mapAstronaut = new TextureRegion(game.getAssets().mapMarkerTexture);
        energyCellRegion = new TextureRegion(game.getAssets().energyCellIconTexture);
        selectedStop = currentStop(game.getCampaign());
        ui.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /**
     * True enquanto a mochila ou o mapa consomem o quadro.
     *
     * Devolver true tambem no quadro em que o painel fecha e o que impede o
     * ESC de fechar o painel e abrir a pausa no mesmo toque, e o que impede um
     * disparo de vazar do painel para a jogabilidade.
     */
    public boolean handleInput() {
        boolean wasOpen = panel != 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) panel = panel == 0 ? 1 : 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) panel = panel == 2 ? 0 : 2;
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) panel = panel == 3 ? 0 : 3;
        if (panel != 0 && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) panel = 0;
        if (panel == 0) {
            dragSource = -1; dragging = false; leftDownLastFrame = false;
            return wasOpen;
        }
        player.getBody().setLinearVelocity(0f, 0f);
        Inventario inventory = game.getCampaign().getInventario();
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        ui.unproject(pointer);
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (pointer.y >= 598f && pointer.y <= 642f && pointer.x >= 794f && pointer.x <= 1154f) {
                panel = 1 + Math.min(2, (int)((pointer.x - 794f) / 120f));
                dragSource = -1; dragging = false; leftDownLastFrame = false;
                return true;
            }
            if (pointer.y >= 64f && pointer.y <= 110f) {
                if (pointer.x >= 128f && pointer.x <= 260f) {
                    panel = 0; dragSource = -1; dragging = false; leftDownLastFrame = false;
                    return true;
                }
                if (panel == 1 && pointer.x >= 278f && pointer.x <= 510f) {
                    consumeFood(inventory);
                    return true;
                }
            }
            if (panel == 2 && pointer.y >= 220f && pointer.y <= 530f) {
                for (int i = 0; i < Stop.values().length; i++) {
                    if (Math.abs(pointer.x - ROUTE_X[i]) < 86f) {
                        selectedStop = Stop.values()[i];
                        return true;
                    }
                }
            }
        }
        if (panel == 1) {
            boolean down = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
            if (down && !leftDownLastFrame) {
                int slot = slotAt(pointer.x, pointer.y);
                dragSource = slot >= 0 && owned(inventory.itemAt(slot), inventory) ? slot : -1;
                dragStartX = pointer.x; dragStartY = pointer.y;
                dragging = false;
            }
            if (down && dragSource >= 0 && !dragging) {
                float dx = pointer.x - dragStartX, dy = pointer.y - dragStartY;
                dragging = dx * dx + dy * dy > 36f;
            }
            if (!down && leftDownLastFrame) {
                int target = slotAt(pointer.x, pointer.y);
                if (dragging && dragSource >= 0 && target >= 0 && target != dragSource)
                    inventory.swapSlots(dragSource, target);
                dragSource = -1; dragging = false;
            }
            leftDownLastFrame = down;
        } else {
            dragSource = -1; dragging = false; leftDownLastFrame = false;
        }
        if (panel == 1 && Gdx.input.isKeyJustPressed(Input.Keys.C)) consumeFood(inventory);
        return true;
    }

    private void consumeFood(Inventario inventory) {
        boolean full = player.getOxigenio() >= 99.5f && player.getEnergia() >= 99.5f;
        if (full) note("Traje e fôlego já estão no máximo. A ração foi preservada.");
        else if (inventory.consumirComida()) {
            player.recuperarEnergia(30f); player.recuperarOxigenio(20f);
            note("Ração consumida. Energia e oxigênio recuperados.");
            game.getSounds().tocarComida();
        } else note("Nenhuma ração na mochila.");
    }

    private void note(String text) { message = text; messageTimer = 4f; }

    public void render() {
        if (messageTimer > 0f) messageTimer -= Gdx.graphics.getDeltaTime();
        if (panel == 0) return;
        if (panel == 2) mapTime += Math.min(.05f, Gdx.graphics.getDeltaTime());
        CampaignState campaign = game.getCampaign();
        ui.beginShapes();
        ui.rect(0f, 0f, 1280f, 720f, SCRIM);
        ui.panel(92f, 52f, 1096f, 616f, UiTheme.CYAN);
        for (int index = 0; index < 3; index++)
            ui.rect(794f + index * 120f, 598f, 112f, 44f,
                panel == index + 1 ? UiTheme.SURFACE_STRONG : UiTheme.VOID);
        ui.rect(128f, 64f, 132f, 46f, UiTheme.SURFACE_STRONG);
        if (panel == 1) ui.rect(278f, 64f, 232f, 46f, UiTheme.SURFACE_STRONG);
        if (panel == 2) {
            // A proporção da carta (1891:831) é preservada nesta área útil.
            ui.sprite(starChart, 108f, 125f, 1064f, 467f, Color.WHITE);
            drawRouteShapes(campaign);
            ui.rect(122f, 133f, 1036f, 57f, MAP_LABEL_BACK);
        } else if (panel == 1) drawBackpackShapes();
        else drawBestiaryShapes(campaign);
        ui.endShapes();
        ui.beginText();
        ui.title("CENTRAL DA EXPEDIÇÃO", .92f, UiTheme.TEXT, 130f, 620f);
        ui.text("MOCHILA", .45f, panel == 1 ? UiTheme.CYAN : UiTheme.TEXT_MUTED, 804f, 625f);
        ui.text("ATLAS", .45f, panel == 2 ? UiTheme.CYAN : UiTheme.TEXT_MUTED, 942f, 625f);
        ui.text("BESTIÁRIO", .45f, panel == 3 ? UiTheme.CYAN : UiTheme.TEXT_MUTED, 1039f, 625f);
        if (panel == 1) drawBackpack(campaign);
        else if (panel == 2) drawRouteText(campaign);
        else drawBestiary(campaign);
        ui.text("FECHAR", .52f, UiTheme.TEXT, 156f, 92f);
        if (panel == 1) {
            ui.text("CONSUMIR RAÇÃO", .49f, UiTheme.GREEN, 291f, 92f);
            ui.text("ARRASTE PARA ORGANIZAR", .50f, UiTheme.TEXT_MUTED, 858f, 91f);
        }
        ui.endText();
    }

    // =====================================================
    // MOCHILA
    // =====================================================

    private void drawBackpackShapes() {
        ui.rect(126f, 185f, 254f, 365f, UiTheme.TRACK);
        ui.rect(129f, 188f, 248f, 359f, UiTheme.SURFACE_STRONG);
        ui.sprite(playerPortrait, 135f, 305f, 236f, 236f, Color.WHITE);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 8; col++) slot(430f + col * 90f, 442f - row * 91f, 78f, row * 8 + col);
        for (int col = 0; col < 8; col++) slot(430f + col * 90f, 130f, 78f, 24 + col);
        Inventario inventory = game.getCampaign().getInventario();
        for (int i = 0; i < 32; i++) {
            int item = inventory.itemAt(i);
            TextureRegion art = itemArt(item);
            if (art != null && owned(item, inventory) && !(dragging && i == dragSource))
                icon(i, art);
        }
        if (dragging && dragSource >= 0) {
            TextureRegion art = itemArt(inventory.itemAt(dragSource));
            if (art != null) ui.sprite(art, pointer.x - 27f, pointer.y - 27f, 54f, 54f, Color.WHITE);
        }
    }

    private TextureRegion itemArt(int item) {
        return switch (item) {
            case 1 -> game.getAssets().resourceIcon(1);
            case 2 -> energyCellRegion;
            case 3 -> game.getAssets().resourceIcon(2);
            case 4 -> game.getAssets().pulseRifleTexture;
            case 5, 6, 7, 8 -> game.getAssets().bossKeyFrame(item - 5);
            default -> null;
        };
    }

    /** A posição do slot persiste; o ícone só existe enquanto houver recurso. */
    private boolean owned(int item, Inventario inventory) {
        return switch (item) {
            case 1 -> inventory.getComida() > 0;
            case 2 -> player.getMunicao() + inventory.getReserveAmmo() > 0;
            case 3 -> player.getGelo() > 0;
            case 4 -> game.getCampaign().hasWeapon();
            case 5, 6, 7, 8 -> inventory.tem(keyId(item - 5));
            default -> false;
        };
    }

    private void slot(float x, float y, float size, int index) {
        ui.rect(x, y, size, size, index == dragSource ? UiTheme.AMBER : UiTheme.BORDER);
        ui.rect(x + 2f, y + 2f, size - 4f, size - 4f, UiTheme.VOID);
        ui.rect(x + 4f, y + size - 8f, size - 8f, 3f, UiTheme.TRACK);
    }

    private void icon(int index, TextureRegion region) {
        float x = 430f + index % 8 * 90f;
        float y = index >= 24 ? 130f : 442f - index / 8 * 91f;
        ui.sprite(region, x + 12f, y + 12f, 54f, 54f, Color.WHITE);
    }

    private int slotAt(float x, float y) {
        for (int i = 0; i < 32; i++) {
            float sx = 430f + i % 8 * 90f;
            float sy = i >= 24 ? 130f : 442f - i / 8 * 91f;
            if (x >= sx && x <= sx + 78f && y >= sy && y <= sy + 78f) return i;
        }
        return -1;
    }

    private String keyId(int index) {
        return switch (index) {
            case 0 -> Inventario.CHAVE_LUA;
            case 1 -> Inventario.CHAVE_MARTE;
            case 2 -> Inventario.CHAVE_TITA;
            default -> Inventario.CHAVE_LUZ;
        };
    }

    private void drawBackpack(CampaignState campaign) {
        Inventario inventory = campaign.getInventario();
        ui.text("TRAJE", .55f, UiTheme.CYAN, 139f, 527f);
        ui.text("O2  " + (int)player.getOxigenio() + "%", .58f, UiTheme.CYAN, 148f, 261f);
        ui.text("ENERGIA  " + (int)player.getEnergia() + "%", .58f, UiTheme.AMBER, 148f, 224f);
        ui.text("CARGA  /  MÓDULOS     ·     RESERVA " + inventory.getReserveAmmo(), .58f, UiTheme.TEXT_MUTED, 430f, 539f);
        for (int i = 0; i < 32; i++) {
            int item = inventory.itemAt(i);
            int count = item == 1 ? inventory.getComida() : item == 2 ? player.getMunicao() + inventory.getReserveAmmo()
                : item == 3 ? player.getGelo() : 0;
            if (item >= 1 && item <= 3 && count > 0) {
                float x = 430f + i % 8 * 90f;
                float y = i >= 24 ? 130f : 442f - i / 8 * 91f;
                ui.text("" + count, .62f, UiTheme.TEXT, x + 54f, y + 12f);
            }
        }
        ui.text("CHAVES DE TRAVESSIA", .5f, UiTheme.TEXT_MUTED, 430f, 244f);
        ui.text("ACESSO RÁPIDO", .5f, UiTheme.TEXT_MUTED, 430f, 222f);
        ui.text("ARMA NV." + inventory.getNivelArma() + "  ·  DANO " + (int)inventory.getDano()
            + "       ARMADURA NV." + inventory.getNivelArmadura(),
            .48f, UiTheme.TEXT, 430f, 104f);
        if (messageTimer > 0f) ui.textWrapped(message, .48f, UiTheme.GREEN, 138f, 160f, 650f);
    }

    // =====================================================
    // MAPA DA CAMPANHA
    // =====================================================

    /** Sinal pequeno sobre o destino atual; os planetas ficam sem anéis artificiais. */
    private void drawRouteShapes(CampaignState campaign) {
        Stop[] stops = Stop.values();
        for (int i = 0; i < stops.length; i++) {
            float x = ROUTE_X[i], y = ROUTE_Y[i];
            boolean here = stops[i].phase == campaign.getPhase();
            ui.rect(x - 78f, y - 143f, 156f, 60f, MAP_LABEL_BACK);
            if (stops[i] == selectedStop) {
                ui.line(x - 46f, y - 151f, x + 46f, y - 151f, 2f, UiTheme.CYAN);
            }
            if (here) {
                boolean reduced = game.getSettings().isReduceMotion();
                float bob = reduced ? 0f : com.badlogic.gdx.math.MathUtils.sin(mapTime * 2.6f) * 3f;
                mapSignal.a = reduced ? .9f : .68f + .24f * com.badlogic.gdx.math.MathUtils.sin(mapTime * 2f);
                ui.line(x - 21f, y + 79f, x + 21f, y + 79f, 2f, mapSignal);
                ui.sprite(mapAstronaut, x - 40f, y + 83f + bob, 80f, 80f, Color.WHITE);
            }
        }
    }

    private void drawRouteText(CampaignState campaign) {
        Stop[] stops = Stop.values();
        for (int i = 0; i < stops.length; i++) {
            float x = ROUTE_X[i], y = ROUTE_Y[i];
            boolean here = stops[i].phase == campaign.getPhase();
            boolean open = unlocked(campaign, stops[i]);
            ui.centered(stops[i].name, here ? .68f : .62f,
                here ? UiTheme.CYAN : open ? UiTheme.TEXT : UiTheme.TEXT_MUTED, x, y-110f);
            ui.centered(here ? "VOCÊ ESTÁ AQUI" : open ? "COLÔNIA ABERTA" : "SINAL BLOQUEADO", .42f,
                here ? UiTheme.CYAN : open ? UiTheme.CYAN : UiTheme.RED, x, y-131f);
        }
        Stop focus = selectedStop == null ? currentStop(campaign) : selectedStop;
        boolean here = focus.phase == campaign.getPhase();
        ui.text((here ? "VOCÊ ESTÁ EM   /   " : "DESTINO   /   ") + focus.name,
            .62f, UiTheme.TEXT, 136f, 169f);
        String detail = here ? campaign.missaoAtual()
            : unlocked(campaign, focus) ? focus.hint + "  ·  Travessia liberada"
            : focus.hint + "  ·  Conquiste a chave anterior para acessar";
        ui.textWrapped(detail, .53f, UiTheme.TEXT_MUTED, 136f, 145f, 1010f);
    }

    private Stop currentStop(CampaignState campaign) {
        for (Stop stop : Stop.values()) if (stop.phase == campaign.getPhase()) return stop;
        return Stop.LUA;
    }

    private boolean unlocked(CampaignState campaign, Stop stop) {
        return stop.key == null || campaign.getInventario().tem(stop.key);
    }

    private void drawBestiaryShapes(CampaignState campaign) {
        CampaignState.Phase[] phases = {CampaignState.Phase.LUNAR, CampaignState.Phase.MARS,
            CampaignState.Phase.TITAN, CampaignState.Phase.CALLISTO};
        for (int i = 0; i < phases.length; i++) {
            float y = 488f - i * 97f;
            boolean defeated = campaign.isBossDefeated(phases[i]);
            ui.rect(130f, y - 43f, 1016f, 82f, UiTheme.SURFACE_STRONG);
            ui.rect(130f, y - 43f, 5f, 82f,
                defeated ? UiTheme.CYAN : UiTheme.TRACK);
            TextureRegion art = switch (phases[i]) {
                case LUNAR -> game.getAssets().lunarBossFrame(0);
                case MARS -> game.getAssets().marsBossFrame(0);
                case TITAN -> game.getAssets().titanBossFrame(0, 0);
                case CALLISTO -> game.getAssets().callistoBossFrame(3, 0);
                default -> null;
            };
            if (art != null) {
                float width = phases[i] == CampaignState.Phase.LUNAR
                    || phases[i] == CampaignState.Phase.MARS ? 53f : 69f;
                ui.sprite(art, 154f + (69f - width) * .5f, y - 37f, width, 70f,
                    defeated ? Color.WHITE : Color.DARK_GRAY);
            }
        }
    }

    private void drawBestiary(CampaignState campaign) {
        CampaignState.Phase[] phases = {CampaignState.Phase.LUNAR, CampaignState.Phase.MARS,
            CampaignState.Phase.TITAN, CampaignState.Phase.CALLISTO};
        String[] names = {"GUARDIÃO DA CRATERA", "TITÃ-FERRUGEM", "SOBERANO DO METANO",
            "SENTINELA DE CALISTO"};
        String[] behavior = {"Golpe de impacto na cratera", "Ataque em área entre as rochas",
            "Investida, rajadas e onda de choque", "Três formas antes da queda final"};
        for (int i = 0; i < phases.length; i++) {
            float y = 488f - i * 97f;
            boolean defeated = campaign.isBossDefeated(phases[i]);
            ui.text(phases[i] == CampaignState.Phase.TITAN ? "TITÃ" : phases[i].name(),
                .50f, UiTheme.TEXT_MUTED, 242f, y + 21f);
            ui.text(defeated ? names[i] : "ARQUIVO LACRADO", .67f,
                defeated ? UiTheme.TEXT : UiTheme.TEXT_MUTED, 242f, y - 5f);
            ui.text(defeated ? behavior[i] : "Enfrente o guardião para registrar seus dados.",
                .50f, UiTheme.TEXT_MUTED, 242f, y - 29f);
            ui.text(defeated ? "REGISTRADO" : "NÃO IDENTIFICADO", .53f,
                defeated ? UiTheme.CYAN : UiTheme.TEXT_MUTED, 964f, y - 4f);
        }
    }

    public void resize(int w, int h) { ui.resize(w, h); }
    public void dispose() { ui.dispose(); }
}

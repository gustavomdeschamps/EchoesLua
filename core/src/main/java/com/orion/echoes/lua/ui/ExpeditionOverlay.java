package com.orion.echoes.lua.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
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

    private final EchoesLua game;
    private final Astronauta player;
    private final TerminalUi ui;
    private int panel; // 0 closed, 1 inventory, 2 map
    private String message = "";
    private float messageTimer;

    public ExpeditionOverlay(EchoesLua game, Astronauta player) {
        this.game = game; this.player = player;
        player.setInventario(game.getCampaign().getInventario());
        ui = new TerminalUi(game.getBatch(), game.getAssets());
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) panel = panel == 1 ? 0 : 1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) panel = panel == 2 ? 0 : 2;
        if (panel != 0 && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) panel = 0;
        if (panel == 0) return wasOpen;
        player.getBody().setLinearVelocity(0f, 0f);
        Inventario inventory = game.getCampaign().getInventario();
        if (panel == 1 && Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            boolean full = player.getOxigenio() >= 99.5f && player.getEnergia() >= 99.5f;
            if (full) note("Traje e fôlego já estão no máximo. A ração foi preservada.");
            else if (inventory.consumirComida()) {
                player.recuperarEnergia(30f); player.recuperarOxigenio(20f);
                note("Ração consumida. Energia e oxigênio recuperados.");
                game.getSounds().tocarComida();
            } else note("Nenhuma ração na mochila.");
        }
        return true;
    }

    private void note(String text) { message = text; messageTimer = 4f; }

    public void render() {
        if (messageTimer > 0f) messageTimer -= Gdx.graphics.getDeltaTime();
        if (panel == 0) return;
        CampaignState campaign = game.getCampaign();
        ui.beginShapes();
        ui.rect(0f, 0f, 1280f, 720f, SCRIM);
        ui.panel(214f, 96f, 852f, 536f, UiTheme.CYAN);
        if (panel == 2) drawRouteShapes(campaign);
        ui.endShapes();
        ui.beginText();
        ui.title(panel == 1 ? "MOCHILA DA EXPEDIÇÃO" : "ROTA DA EXPEDIÇÃO", .92f, UiTheme.TEXT, 252f, 578f);
        if (panel == 1) drawBackpack(campaign); else drawRouteText(campaign);
        ui.text(panel == 1 ? "I fecha  ·  C consome uma ração" : "M fecha",
            .52f, UiTheme.TEXT_MUTED, 252f, 130f);
        ui.endText();
    }

    // =====================================================
    // MOCHILA
    // =====================================================

    private void drawBackpack(CampaignState campaign) {
        Inventario inventory = campaign.getInventario();
        row(0, "Comida", inventory.getComida() + (inventory.getComida() == 1 ? " ração" : " rações"),
            inventory.getComida() > 0 ? UiTheme.TEXT : UiTheme.TEXT_MUTED);
        row(1, "Arma", campaign.hasWeapon() ? "Rifle de pulso equipado" : "Rifle não fabricado",
            campaign.hasWeapon() ? UiTheme.TEXT : UiTheme.RED);
        row(2, "Munição", player.getMunicao() + " / "
            + com.orion.echoes.lua.config.GameConfig.AMMO_MAX + " células",
            player.getMunicao() <= 3 ? UiTheme.RED : UiTheme.TEXT);
        row(3, "Arma NV." + inventory.getNivelArma(), "Dano " + (int)inventory.getDano(), UiTheme.CYAN);
        row(4, "Armadura NV." + inventory.getNivelArmadura(),
            "Dano recebido -" + inventory.getNivelArmadura() * 15 + "%", UiTheme.CYAN);
        key(5, "Chave da Lua", Inventario.CHAVE_LUA, inventory);
        key(6, "Chave de Marte", Inventario.CHAVE_MARTE, inventory);
        key(7, "Chave de Titã", Inventario.CHAVE_TITA, inventory);
        key(8, "Chave de Luz", Inventario.CHAVE_LUZ, inventory);
        if (messageTimer > 0f) ui.text(message, .54f, UiTheme.GREEN, 640f, 130f);
    }

    private void row(int index, String label, String value, Color valueColor) {
        float y = 526f - index * 44f;
        ui.text(label, .6f, UiTheme.TEXT_MUTED, 252f, y);
        ui.text(value, .6f, valueColor, 620f, y);
    }

    private void key(int index, String label, String id, Inventario inventory) {
        boolean owned = inventory.tem(id);
        row(index, label, owned ? "SIM" : "NÃO", owned ? UiTheme.GREEN : UiTheme.TEXT_MUTED);
    }

    // =====================================================
    // MAPA DA CAMPANHA
    // =====================================================

    /**
     * Mapa da rota.
     *
     * O guia pede quatro corpos, "você está aqui" e nada de minimapa em tile.
     * A lista anterior alinhava os nomes com espaços em branco e deixava dois
     * terços do painel vazios; aqui cada parada tem marcador, trilho e estado
     * de travessia lido do inventário, que é a mesma fonte dos portais.
     */
    private void drawRouteShapes(CampaignState campaign) {
        Stop[] stops = Stop.values();
        for (int i = 0; i < stops.length; i++) {
            float x = 290f + i * 176f;
            if (i > 0) {
                boolean open = unlocked(campaign, stops[i]);
                ui.rect(x - 140f, 396f, 104f, 6f, open ? UiTheme.CYAN : UiTheme.TRACK);
            }
            boolean here = stops[i].phase == campaign.getPhase();
            float size = here ? 64f : 48f;
            ui.rect(x - size / 2f, 399f - size / 2f, size, size,
                here ? UiTheme.CYAN : unlocked(campaign, stops[i]) ? UiTheme.CYAN_DIM : UiTheme.TRACK);
            ui.rect(x - size / 2f + 6f, 405f - size / 2f, size - 12f, size - 12f, UiTheme.SURFACE_STRONG);
        }
    }

    private void drawRouteText(CampaignState campaign) {
        Stop[] stops = Stop.values();
        for (int i = 0; i < stops.length; i++) {
            float x = 290f + i * 176f;
            boolean here = stops[i].phase == campaign.getPhase();
            boolean open = unlocked(campaign, stops[i]);
            ui.centered(stops[i].name, here ? .62f : .54f,
                here ? UiTheme.CYAN : open ? UiTheme.TEXT : UiTheme.TEXT_MUTED, x, 452f);
            ui.centered(here ? "VOCÊ ESTÁ AQUI" : open ? "aberto" : "bloqueado", .42f,
                here ? UiTheme.GREEN : open ? UiTheme.TEXT_MUTED : UiTheme.RED, x, 336f);
            ui.centered(stops[i].hint, .4f, UiTheme.TEXT_MUTED, x, 308f);
        }
        Stop current = currentStop(campaign);
        ui.text("Destino atual  ·  " + current.name, .6f, UiTheme.TEXT, 252f, 236f);
        ui.textWrapped(campaign.missaoAtual(), .54f, UiTheme.TEXT_MUTED, 252f, 202f, 776f);
    }

    private Stop currentStop(CampaignState campaign) {
        for (Stop stop : Stop.values()) if (stop.phase == campaign.getPhase()) return stop;
        return Stop.LUA;
    }

    private boolean unlocked(CampaignState campaign, Stop stop) {
        return stop.key == null || campaign.getInventario().tem(stop.key);
    }

    public void resize(int w, int h) { ui.resize(w, h); }
    public void dispose() { ui.dispose(); }
}

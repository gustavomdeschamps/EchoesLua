package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;
import com.orion.echoes.lua.entities.MarsObject;
import com.orion.echoes.lua.entities.Npc;
import java.util.EnumMap;
import java.util.Map;

/** Catálogo visual carregado de modo incremental pela LoadingScreen. */
public final class AssetManager implements Disposable {
    /** Glifos usados por HUD, diálogos e telas; evita quadrados em símbolos do próprio jogo. */
    public static final String GAME_GLYPHS =
        "áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇºª–—○●→←·•";
    private static final String GAME_ATLAS = "atlases/game.atlas";
    private static final String UI_ATLAS = "atlases/ui.atlas";
    private static final String FX_ATLAS = "atlases/fx.atlas";
    private static final String LUNAR_GROUND = "textures/lunar_ground.png";
    private static final String MARS_GROUND = "textures/mars_ground.png";
    private static final String TITAN_GROUND = "textures/titan_ground_v2.png";
    /*
     * Key art das aberturas por mundo (docs/NEW_VISUAL_ASSETS.md).
     *
     * Ainda não foram fornecidas. Carregadas como Texture solta (como o
     * terreno) em vez de via atlas, porque isso permite checar a existência do
     * arquivo em disco e simplesmente não carregar nada quando falta — um
     * findRegion() nulo no atlas exigiria que o TexturePacker já soubesse da
     * imagem, que ainda não existe.
     */
    private static final String WORLD_INTRO_LUNAR = "textures/world_intro_lunar_v1.png";
    private static final String WORLD_INTRO_MARS = "textures/world_intro_mars_v1.png";
    private static final String WORLD_INTRO_TITAN = "textures/world_intro_titan_v1.png";

    private final com.badlogic.gdx.assets.AssetManager loader =
        new com.badlogic.gdx.assets.AssetManager();
    private boolean queued;
    private boolean ready;
    private final java.util.IdentityHashMap<TextureRegion,
        com.badlogic.gdx.utils.Array<TextureAtlas.AtlasRegion>> frameGroups =
        new java.util.IdentityHashMap<>();
    private TextureRegion npcAyla;
    private TextureRegion titanVerticalPortal;
    /** Folha de cada identidade de NPC, resolvida com fallback em bindLoadedAssets(). */
    private final Map<Npc.Visual, TextureRegion> npcVisualSheets = new EnumMap<>(Npc.Visual.class);
    /** Nulo enquanto o PNG dedicado (docs/NEW_VISUAL_ASSETS.md) não for fornecido. */
    private TextureRegion marsStationSheetTexture;
    private TextureRegion titanRefinerySheetTexture;
    private Texture worldIntroLunarTexture;
    private Texture worldIntroMarsTexture;
    private Texture worldIntroTitanTexture;

    public TextureRegion astronautaSheetTexture;
    public TextureRegion astronautCombatSheetTexture;
    public TextureRegion pulseRifleTexture;
    public Texture backgroundLuaTexture;
    public TextureRegion baseLunarTexture;
    public TextureRegion oxigenioTexture;
    public TextureRegion comidaTexture;
    public TextureRegion geloTexture;
    public TextureRegion missionAtlasTexture;
    public Texture marsBackgroundTexture;
    public TextureRegion introKeyArtTexture;
    public TextureRegion marsAtlasTexture;
    public TextureRegion lunarEnemySheetTexture;
    public TextureRegion marsDroneSheetTexture;
    public TextureRegion marsCrawlerSheetTexture;
    public TextureRegion titanEnemySheetTexture;
    public TextureRegion titanBossSheetTexture;
    public TextureRegion titanPortalSheetTexture;
    public TextureRegion repairStationsSheetTexture;
    public TextureRegion titanFormationsTexture;
    public TextureRegion npcCommanderSheetTexture;
    public Texture titanBackgroundTexture;
    public TextureRegion lunarObstaclesTexture;
    public TextureRegion marsObstaclesTexture;
    public TextureRegion actionFxTexture;
    public TextureRegion energyFxTexture;
    public TextureRegion landmarksTexture;
    public TextureRegion uiPanelTexture;
    public TextureRegion uiPanelHudTexture;
    public TextureRegion uiPanelDialogTexture;
    public TextureRegion uiPanelModalTexture;
    public TextureRegion uiButtonNormalTexture;
    public TextureRegion uiButtonHoverTexture;
    public TextureRegion uiButtonPressedTexture;
    public TextureRegion uiButtonDisabledTexture;
    public TextureRegion uiBarTrackTexture;
    public TextureRegion uiBarFillTexture;
    public TextureRegion uiResourceIconsTexture;
    public TextureRegion uiDamageVignetteTexture;
    public TextureRegion uiWhiteTexture;
    public TextureRegion uiObjectiveMarkerTexture;
    public BitmapFont font;
    public BitmapFont titleFont;

    public void queue() {
        if (queued) return;
        queued = true;
        loader.load(GAME_ATLAS, TextureAtlas.class);
        loader.load(UI_ATLAS, TextureAtlas.class);
        loader.load(FX_ATLAS, TextureAtlas.class);

        TextureLoader.TextureParameter terrain = new TextureLoader.TextureParameter();
        terrain.minFilter = Texture.TextureFilter.Linear;
        terrain.magFilter = Texture.TextureFilter.Linear;
        terrain.wrapU = Texture.TextureWrap.Repeat;
        terrain.wrapV = Texture.TextureWrap.Repeat;
        loader.load(LUNAR_GROUND, Texture.class, terrain);
        loader.load(MARS_GROUND, Texture.class, terrain);
        loader.load(TITAN_GROUND, Texture.class, terrain);

        TextureLoader.TextureParameter keyArt = new TextureLoader.TextureParameter();
        keyArt.minFilter = Texture.TextureFilter.Linear;
        keyArt.magFilter = Texture.TextureFilter.Linear;
        if (Gdx.files.internal(WORLD_INTRO_LUNAR).exists()) loader.load(WORLD_INTRO_LUNAR, Texture.class, keyArt);
        if (Gdx.files.internal(WORLD_INTRO_MARS).exists()) loader.load(WORLD_INTRO_MARS, Texture.class, keyArt);
        if (Gdx.files.internal(WORLD_INTRO_TITAN).exists()) loader.load(WORLD_INTRO_TITAN, Texture.class, keyArt);
    }

    /** Avança a fila sem bloquear; retorna true somente quando tudo está pronto. */
    public boolean update() {
        if (!queued) throw new IllegalStateException("queue() deve ser chamado antes de update().");
        if (!loader.update()) return false;
        bindLoadedAssets();
        return true;
    }

    public float getProgress() {
        return loader.getProgress();
    }

    public boolean isReady() {
        return ready;
    }

    private void bindLoadedAssets() {
        if (ready) return;
        TextureAtlas gameAtlas = loader.get(GAME_ATLAS, TextureAtlas.class);
        TextureAtlas uiAtlas = loader.get(UI_ATLAS, TextureAtlas.class);
        TextureAtlas fxAtlas = loader.get(FX_ATLAS, TextureAtlas.class);

        astronautaSheetTexture = required(gameAtlas, "astronauta_sheet");
        astronautCombatSheetTexture = required(gameAtlas, "astronaut_combat_sheet");
        pulseRifleTexture = required(gameAtlas, "pulse_rifle");
        baseLunarTexture = required(gameAtlas, "base_lunar");
        oxigenioTexture = required(gameAtlas, "oxigenio");
        comidaTexture = required(gameAtlas, "comida");
        geloTexture = required(gameAtlas, "gelo");
        missionAtlasTexture = required(gameAtlas, "mission_atlas_unified");
        marsAtlasTexture = required(gameAtlas, "mars_atlas_v4");
        lunarEnemySheetTexture = required(gameAtlas, "lunar_enemy_sheet");
        marsDroneSheetTexture = required(gameAtlas, "mars_drone_sheet");
        marsCrawlerSheetTexture = required(gameAtlas, "mars_crawler_sheet");
        titanEnemySheetTexture = required(gameAtlas, "titan_hunter_sheet_v3");
        titanBossSheetTexture = required(gameAtlas, "titan_boss_sheet_v3");
        titanPortalSheetTexture = required(gameAtlas, "campaign_portal_sheet_v2");
        repairStationsSheetTexture = required(gameAtlas, "lunar_repair_stations_v2");
        titanFormationsTexture = required(gameAtlas, "titan_formations_v2");
        npcCommanderSheetTexture = required(gameAtlas, "npc_colony_officer_sheet_v2");
        npcAyla = required(gameAtlas, "npc_commander_ayla_sheet");
        titanVerticalPortal = required(gameAtlas, "titan_portal_vertical_v2");
        lunarObstaclesTexture = required(gameAtlas, "lunar_obstacles");
        marsObstaclesTexture = required(gameAtlas, "mars_obstacles");
        landmarksTexture = required(gameAtlas, "landmarks");
        introKeyArtTexture = required(uiAtlas, "intro_keyart_v4");
        uiPanelTexture = required(uiAtlas, "ui_panel_frame");
        uiPanelHudTexture = required(uiAtlas, "panel_hud");
        uiPanelDialogTexture = required(uiAtlas, "panel_dialog");
        uiPanelModalTexture = required(uiAtlas, "panel_modal");
        uiButtonNormalTexture = required(uiAtlas, "button_normal");
        uiButtonHoverTexture = required(uiAtlas, "button_hover");
        uiButtonPressedTexture = required(uiAtlas, "button_pressed");
        uiButtonDisabledTexture = required(uiAtlas, "button_disabled");
        uiBarTrackTexture = required(uiAtlas, "bar_track");
        uiBarFillTexture = required(uiAtlas, "bar_fill");
        uiResourceIconsTexture = required(uiAtlas, "resource_icons");
        uiDamageVignetteTexture = required(uiAtlas, "damage_vignette");
        uiWhiteTexture = required(uiAtlas, "white_pixel");
        uiObjectiveMarkerTexture = required(uiAtlas, "objective_marker");
        actionFxTexture = required(fxAtlas, "action_fx_sheet");
        energyFxTexture = required(fxAtlas, "energy_fx_sheet");
        backgroundLuaTexture = loader.get(LUNAR_GROUND, Texture.class);
        marsBackgroundTexture = loader.get(MARS_GROUND, Texture.class);
        titanBackgroundTexture = loader.get(TITAN_GROUND, Texture.class);
        worldIntroLunarTexture = loader.isLoaded(WORLD_INTRO_LUNAR) ? loader.get(WORLD_INTRO_LUNAR, Texture.class) : null;
        worldIntroMarsTexture = loader.isLoaded(WORLD_INTRO_MARS) ? loader.get(WORLD_INTRO_MARS, Texture.class) : null;
        worldIntroTitanTexture = loader.isLoaded(WORLD_INTRO_TITAN) ? loader.get(WORLD_INTRO_TITAN, Texture.class) : null;

        for (Npc.Visual visual : Npc.Visual.values()) {
            TextureRegion sheet = optional(gameAtlas, visual.sheetKey());
            if (sheet == null && visual.fallbackKey() != null) sheet = optional(gameAtlas, visual.fallbackKey());
            if (sheet == null) throw new IllegalStateException("Nenhuma folha disponível para NPC: " + visual);
            npcVisualSheets.put(visual, sheet);
        }
        // Ainda não fornecidos (docs/NEW_VISUAL_ASSETS.md): null é um estado válido,
        // resolvido com fallback procedural em tempo de uso, não aqui.
        marsStationSheetTexture = optional(gameAtlas, "mars_station_sheet_v2");
        titanRefinerySheetTexture = optional(gameAtlas, "titan_refinery_sheet_v2");

        font = generateFont("fonts/ChakraPetch-Regular.ttf", 25);
        titleFont = generateFont("fonts/ChakraPetch-SemiBold.ttf", 32);
        ready = true;
    }

    private TextureRegion required(TextureAtlas atlas, String name) {
        com.badlogic.gdx.utils.Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(name);
        if (frames.size > 1) {
            TextureRegion handle = frames.first();
            frameGroups.put(handle, frames);
            return handle;
        }
        TextureRegion region = atlas.findRegion(name);
        if (region == null) throw new IllegalStateException("Região obrigatória ausente no atlas: " + name);
        return region;
    }

    /**
     * Como {@link #required}, mas devolve {@code null} em vez de lançar.
     *
     * Usado para assets do manifesto (docs/NEW_VISUAL_ASSETS.md) ainda não
     * fornecidos: o nome simplesmente não existe no atlas empacotado, e quem
     * chama decide o fallback visual em vez do carregamento falhar.
     */
    private TextureRegion optional(TextureAtlas atlas, String name) {
        com.badlogic.gdx.utils.Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(name);
        if (frames.size > 1) {
            TextureRegion handle = frames.first();
            frameGroups.put(handle, frames);
            return handle;
        }
        return atlas.findRegion(name);
    }

    private BitmapFont generateFont(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + GAME_GLYPHS;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont generated = generator.generateFont(parameter);
        generator.dispose();
        return generated;
    }

    /** Caller owns this font; Scene2D must not mutate gameplay font metrics. */
    public BitmapFont createInterfaceFont(int size, boolean heading) {
        return generateFont(heading ? "fonts/ChakraPetch-SemiBold.ttf"
            : "fonts/ChakraPetch-Regular.ttf", size);
    }

    public TextureRegion astronautFrame(int column, int row) {
        return gridRegion(astronautaSheetTexture, 4, 4, column, row, 0);
    }

    public TextureRegion astronautCombatFrame(int column, int row) {
        return gridRegion(astronautCombatSheetTexture, 4, 3, column, row, 0);
    }

    public TextureRegion marsRegion(int column, int row) {
        return gridRegion(marsAtlasTexture, 4, 3, column, row, 2);
    }

    public TextureRegion lunarEnemyFrame(int column, int row) {
        return gridRegion(lunarEnemySheetTexture, 4, 4, column, row, 2);
    }

    public TextureRegion marsEnemyFrame(boolean drone, int column, int row) {
        return gridRegion(drone ? marsDroneSheetTexture : marsCrawlerSheetTexture,
            4, 4, column, row, 2);
    }

    public TextureRegion titanEnemyFrame(int column, int row) {
        return gridRegion(titanEnemySheetTexture, 4, 4, column, row, 2);
    }

    public TextureRegion titanBossFrame(int column, int row) {
        return gridRegion(titanBossSheetTexture, 4, 4, column, row, 2);
    }

    public TextureRegion npcCommanderFrame(int column, int row) {
        return gridRegion(npcCommanderSheetTexture, 4, 4, column, row, 2);
    }

    public TextureRegion npcAylaFrame(int column, int row) {
        return gridRegion(npcAyla, 4, 4, column, row, 0);
    }

    /** Resolve o quadro de um NPC pela identidade tipada, com fallback já embutido. */
    public TextureRegion npcVisualFrame(Npc.Visual visual, int column, int row) {
        TextureRegion sheet = npcVisualSheets.get(visual);
        if (sheet == null) throw new IllegalStateException("Folha não carregada para " + visual);
        return gridRegion(sheet, 4, 4, column, row, visual.inset());
    }

    /** True quando a folha dedicada de {@code visual} já foi fornecida (não é fallback). */
    public boolean hasDedicatedSheet(Npc.Visual visual) {
        return loader.get(GAME_ATLAS, TextureAtlas.class).findRegion(visual.sheetKey()) != null;
    }

    /**
     * Quadro animado de uma estação marciana, ou {@code null} enquanto
     * {@code mars_station_sheet_v2.png} não tiver sido fornecido — quem chama
     * decide o fallback (hoje, a região estática já existente do prop).
     *
     * Contrato de frame: 0 offline, 1 ativando, 2 e 3 alternam em operação
     * (ver docs/NEW_VISUAL_ASSETS.md).
     */
    public TextureRegion marsStationFrame(MarsObject.Kind kind, int frame) {
        if (marsStationSheetTexture == null) return null;
        int row = switch (kind) {
            case SOLAR_STATION -> 0;
            case OXYGEN_STATION -> 1;
            case COMMS_STATION -> 2;
            default -> throw new IllegalArgumentException(kind + " não é uma estação marciana.");
        };
        return gridRegion(marsStationSheetTexture, 4, 3, frame, row, 2);
    }

    public boolean hasMarsStationSheet() { return marsStationSheetTexture != null; }

    /**
     * Quadro animado da refinaria de Titã, ou {@code null} enquanto
     * {@code titan_refinery_sheet_v2.png} não tiver sido fornecido.
     *
     * Contrato: 0 idle, 1 boot/interação, 2 e 3 processamento (ver
     * docs/NEW_VISUAL_ASSETS.md).
     */
    public TextureRegion titanRefineryFrame(int frame) {
        if (titanRefinerySheetTexture == null) return null;
        return gridRegion(titanRefinerySheetTexture, 4, 1, frame, 0, 2);
    }

    public boolean hasTitanRefinerySheet() { return titanRefinerySheetTexture != null; }

    /** Nulo enquanto a key art dedicada não existir; quem chama usa o fallback do terreno. */
    public Texture worldIntroTexture(com.orion.echoes.lua.systems.CampaignState.Phase world) {
        return switch (world) {
            case LUNAR -> worldIntroLunarTexture;
            case MARS -> worldIntroMarsTexture;
            case TITAN -> worldIntroTitanTexture;
        };
    }

    public TextureRegion portalFrame(int column, int row) {
        return gridRegion(titanPortalSheetTexture, 4, 4, column, row, 3);
    }

    public TextureRegion titanPortalFrame(int column, int row) {
        return gridRegion(titanVerticalPortal, 4, 2, column, row == 0 ? 0 : 1, 0);
    }

    public TextureRegion repairStationFrame(int stationRow, int column) {
        return gridRegion(repairStationsSheetTexture, 4, 4, column, stationRow, 3);
    }

    public TextureRegion titanFormationRegion(int index) {
        return gridRegion(titanFormationsTexture, 3, 2, index % 3, index / 3, 3);
    }

    public TextureRegion lunarObstacleRegion(int index) {
        return gridRegion(lunarObstaclesTexture, 3, 2, index % 3, index / 3, 2);
    }

    public TextureRegion marsObstacleRegion(int index) {
        return gridRegion(marsObstaclesTexture, 3, 2, index % 3, index / 3, 2);
    }

    public TextureRegion actionFxFrame(int column, int row) {
        return gridRegion(actionFxTexture, 6, 4, column, row, 0);
    }

    public TextureRegion energyFxFrame(int column, int row) {
        return gridRegion(energyFxTexture, 6, 4, column, row, 0);
    }

    public TextureRegion landmarkRegion(int column, int row) {
        return gridRegion(landmarksTexture, 4, 2, column, row, 2);
    }

    public NinePatch uiPanelPatch() {
        return new NinePatch(uiPanelHudTexture, 24, 24, 24, 24);
    }

    public NinePatch uiDialogPatch() { return new NinePatch(uiPanelDialogTexture, 24, 24, 24, 24); }

    /*
     * Botoes fora do Scene2D.
     *
     * As telas de resultado desenham direto no batch e nao tinham como usar as
     * texturas de botao, entao acabavam desenhando uma linha de 1px no lugar.
     */
    public NinePatch uiButtonPatch() { return new NinePatch(uiButtonNormalTexture, 18, 18, 18, 18); }

    public NinePatch uiButtonHoverPatch() { return new NinePatch(uiButtonHoverTexture, 18, 18, 18, 18); }

    public NinePatch uiButtonPressedPatch() { return new NinePatch(uiButtonPressedTexture, 18, 18, 18, 18); }
    public NinePatch uiModalPatch() { return new NinePatch(uiPanelModalTexture, 24, 24, 24, 24); }

    public TextureRegion resourceIcon(int index) {
        if (index < 0 || index > 3) throw new IllegalArgumentException("Ícone inválido: " + index);
        return gridRegion(uiResourceIconsTexture, 4, 1, index, 0, 0);
    }

    private TextureRegion gridRegion(TextureRegion sheet, int columns, int rows,
                                     int column, int row, int inset) {
        if (column < 0 || column >= columns || row < 0 || row >= rows) {
            throw new IllegalArgumentException("Célula fora da grade: " + column + "," + row);
        }
        com.badlogic.gdx.utils.Array<TextureAtlas.AtlasRegion> frames = frameGroups.get(sheet);
        if (frames != null) {
            if (frames.size != columns * rows) throw new IllegalStateException("Grade incompleta no atlas");
            // Each entity owns its flip state. Never flip an atlas singleton.
            return new TextureRegion(frames.get(row * columns + column));
        }
        int cellWidth = sheet.getRegionWidth() / columns;
        int cellHeight = sheet.getRegionHeight() / rows;
        return new TextureRegion(sheet.getTexture(),
            sheet.getRegionX() + column * cellWidth + inset,
            sheet.getRegionY() + row * cellHeight + inset,
            cellWidth - inset * 2, cellHeight - inset * 2);
    }

    public TextureRegion missionRegion(int column, int row) {
        return gridRegion(missionAtlasTexture, 4, 4, column, row, 2);
    }

    public TextureRegion missionRegion(MissionSprite sprite) {
        if (sprite == null) throw new IllegalArgumentException("Sprite de missão nulo.");
        return missionRegion(sprite.column(), sprite.row());
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
        loader.dispose();
        ready = false;
        queued = false;
    }
}

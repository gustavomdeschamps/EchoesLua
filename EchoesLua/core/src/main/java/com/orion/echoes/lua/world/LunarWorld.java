package com.orion.echoes.lua.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.RandomXS128;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.BaseLunar;
import com.orion.echoes.lua.entities.CraftingStation;
import com.orion.echoes.lua.entities.Enemy;
import com.orion.echoes.lua.entities.EnemyPulse;
import com.orion.echoes.lua.entities.Item;
import com.orion.echoes.lua.entities.MissionCollectible;
import com.orion.echoes.lua.entities.Obstacle;
import com.orion.echoes.lua.entities.Portal;
import com.orion.echoes.lua.entities.RepairStation;
import com.orion.echoes.lua.entities.Wall;
import com.orion.echoes.lua.factories.MissionEntityFactory;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.physics.PhysicsWorld;
import com.orion.echoes.lua.systems.MissionState;

/**
 * Conteudo jogavel da fase lunar: paredes, obstaculos, itens, entidades de
 * missao e o estado que amarra tudo.
 *
 * A tela deixou de construir e guardar cada colecao. O layout e sorteado a
 * partir de uma semente, entao a mesma semente devolve sempre a mesma partida,
 * e todo ponto obrigatorio e validado por flood-fill antes de entrar em jogo.
 */
public final class LunarWorld {

    /** Lado da celula de navegacao usada na validacao de alcance. */
    private static final float NAVIGATION_CELL = 60f;
    private static final int PLACEMENT_ATTEMPTS = 120;
    private static final int ITEMS_PER_TYPE = 6;

    private final long seed;
    private final RandomXS128 random;
    private final AssetManager assets;
    private final PhysicsWorld physics;
    private final ReachabilityGrid navigation;

    private final Array<Wall> walls = new Array<>();
    private final Array<Obstacle> obstacles = new Array<>();
    private final Array<Item> items = new Array<>();
    private final Array<MissionCollectible> collectibles = new Array<>();
    private final Array<RepairStation> repairStations = new Array<>();
    private final Array<Enemy> enemies = new Array<>();
    private final Array<EnemyPulse> enemyPulses = new Array<>();

    private final MissionState mission = new MissionState();
    private final Astronauta player;
    private final BaseLunar base;
    private CraftingStation craftingStation;
    private Portal portal;

    public LunarWorld(long seed, AssetManager assets, PhysicsWorld physics) {
        this.seed = seed;
        this.random = new RandomXS128(seed);
        this.assets = assets;
        this.physics = physics;
        this.navigation = new ReachabilityGrid(
            GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT, NAVIGATION_CELL);

        player = new Astronauta(GameConfig.PLAYER_START_X, GameConfig.PLAYER_START_Y, assets, physics);
        player.setSpeed(GameConfig.PLAYER_SPEED);
        base = new BaseLunar(GameConfig.BASE_X, GameConfig.BASE_Y,
            GameConfig.BASE_WIDTH, GameConfig.BASE_HEIGHT, assets, physics);

        buildWalls();
        buildObstacles();
        buildNavigation();
        buildMission();
        buildItems();
    }

    public LunarWorld(AssetManager assets, PhysicsWorld physics) {
        this(System.nanoTime(), assets, physics);
    }

    // =====================================================
    // CONSTRUCAO
    // =====================================================

    private void buildWalls() {
        float thickness = 35f;
        walls.add(new Wall(-thickness, 0, thickness, GameConfig.WORLD_HEIGHT, physics));
        walls.add(new Wall(GameConfig.WORLD_WIDTH, 0, thickness, GameConfig.WORLD_HEIGHT, physics));
        walls.add(new Wall(0, -thickness, GameConfig.WORLD_WIDTH, thickness, physics));
        walls.add(new Wall(0, GameConfig.WORLD_HEIGHT, GameConfig.WORLD_WIDTH, thickness, physics));
    }

    private void buildObstacles() {
        float[][] layout = {
            {430, 220, 90, 90}, {650, 420, 110, 110}, {820, 160, 95, 95},
            {390, 760, 100, 100}, {620, 1100, 115, 115}, {1050, 1060, 125, 105},
            {1210, 280, 110, 100}, {1370, 480, 105, 105}, {1450, 870, 125, 115},
            {1640, 650, 100, 100}, {1770, 250, 90, 90}, {1830, 1040, 120, 120},
            {2020, 720, 95, 95}, {250, 1210, 85, 85}, {2260, 260, 120, 105},
            {2440, 520, 105, 115}, {2680, 820, 125, 110}, {2320, 1240, 110, 100},
            {1880, 1550, 125, 115}, {980, 1580, 105, 95}
        };
        for (float[] cell : layout) {
            /*
             * A semente desloca cada rocha dentro de uma folga curta. O
             * desenho geral da fase se mantem legivel, mas o caminho exato
             * muda de partida para partida.
             */
            float jitterX = (random.nextFloat() - .5f) * 90f;
            float jitterY = (random.nextFloat() - .5f) * 90f;
            float x = clamp(cell[0] + jitterX, 120f, GameConfig.WORLD_WIDTH - cell[2] - 120f);
            float y = clamp(cell[1] + jitterY, 120f, GameConfig.WORLD_HEIGHT - cell[3] - 120f);
            obstacles.add(new Obstacle(x, y, cell[2] * 1.14f, cell[3] * 1.14f,
                assets.lunarObstacleRegion(obstacles.size % 6), physics));
        }
    }

    /** Bloqueia rochas e base na grade e propaga a partir do jogador. */
    private void buildNavigation() {
        for (Obstacle obstacle : obstacles) navigation.block(obstacle.getBounds());
        navigation.block(base.getBounds());
        navigation.floodFrom(GameConfig.PLAYER_START_X, GameConfig.PLAYER_START_Y);
    }

    private void buildMission() {
        MissionEntityFactory factory = new MissionEntityFactory(assets, physics);
        repairStations.add(factory.station(260, 1390, MissionState.SystemType.COMUNICACAO));
        repairStations.add(factory.station(1480, 1560, MissionState.SystemType.ENERGIA));
        repairStations.add(factory.station(2500, 980, MissionState.SystemType.EXTRACAO));
        repairStations.add(factory.station(420, 560, MissionState.SystemType.ESTUFA));
        craftingStation = factory.craftingStation(1390, 740);
        portal = factory.portal(2710, 1600);

        MissionState.PartType[] parts = {
            MissionState.PartType.ANTENA, MissionState.PartType.ENERGIA,
            MissionState.PartType.EXTRACAO, MissionState.PartType.ESTUFA,
            MissionState.PartType.ARMA_A, MissionState.PartType.ARMA_B, MissionState.PartType.ARMA_C
        };
        for (MissionState.PartType part : parts) {
            Vector2 spawn = freePosition(62f, 260f);
            collectibles.add(factory.collectible(spawn.x, spawn.y, part));
        }

        /*
         * A ordem ensina sem texto: o primeiro hostil aparece isolado e no
         * comportamento mais simples; emboscador e atirador so entram depois,
         * quando o jogador ja leu o telegraph uma vez.
         */
        enemies.add(factory.enemy(760, 470, Enemy.Behavior.STALKER));
        enemies.add(factory.enemy(1500, 1320, Enemy.Behavior.AMBUSHER));
        enemies.add(factory.enemy(2180, 720, Enemy.Behavior.RANGED));
        enemies.add(factory.enemy(2570, 1420, Enemy.Behavior.AMBUSHER));
        mission.setTotalEnemies(enemies.size);
    }

    private void buildItems() {
        for (Item.TipoItem type : Item.TipoItem.values()) {
            for (int index = 0; index < ITEMS_PER_TYPE; index++) {
                Vector2 spawn = freePosition(GameConfig.ITEM_SIZE, 190f);
                items.add(new Item(spawn.x, spawn.y, GameConfig.ITEM_SIZE, GameConfig.ITEM_SIZE,
                    type, assets, physics));
            }
        }
    }

    /**
     * Sorteia um ponto livre e alcancavel.
     *
     * O flood-fill e o filtro que importa: sem ele, uma peca podia cair num
     * bolsao fechado por rochas e deixar a missao impossivel de terminar.
     */
    private Vector2 freePosition(float size, float minPlayerDistance) {
        Rectangle candidate = new Rectangle();
        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            float x = range(110f, GameConfig.WORLD_WIDTH - size - 110f);
            float y = range(110f, GameConfig.WORLD_HEIGHT - size - 110f);
            candidate.set(x, y, size, size);
            if (Vector2.dst(x, y, GameConfig.PLAYER_START_X, GameConfig.PLAYER_START_Y) < minPlayerDistance) continue;
            if (!navigation.isReachable(x + size / 2f, y + size / 2f)) continue;
            if (candidate.overlaps(base.getBounds())) continue;
            if (overlapsAny(candidate)) continue;
            return new Vector2(x, y);
        }
        return fallbackPosition(size);
    }

    private boolean overlapsAny(Rectangle candidate) {
        for (Obstacle obstacle : obstacles) if (candidate.overlaps(obstacle.getBounds())) return true;
        for (RepairStation station : repairStations) if (candidate.overlaps(station.getBounds())) return true;
        for (Item item : items) if (candidate.overlaps(item.getBounds())) return true;
        for (MissionCollectible collectible : collectibles) {
            if (candidate.overlaps(collectible.getBounds())) return true;
        }
        if (craftingStation != null && candidate.overlaps(craftingStation.getBounds())) return true;
        return portal != null && candidate.overlaps(portal.getBounds());
    }

    /** Ultimo recurso: varre a grade e devolve a primeira celula alcancavel. */
    private Vector2 fallbackPosition(float size) {
        for (float y = 140f; y < GameConfig.WORLD_HEIGHT - size - 140f; y += NAVIGATION_CELL) {
            for (float x = 140f; x < GameConfig.WORLD_WIDTH - size - 140f; x += NAVIGATION_CELL) {
                if (navigation.isReachable(x + size / 2f, y + size / 2f)) return new Vector2(x, y);
            }
        }
        return new Vector2(GameConfig.PLAYER_START_X + 120f, GameConfig.PLAYER_START_Y + 120f);
    }

    private float range(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // =====================================================
    // ACESSO
    // =====================================================

    public long getSeed() { return seed; }
    public Astronauta getPlayer() { return player; }
    public BaseLunar getBase() { return base; }
    public MissionState getMission() { return mission; }
    public Portal getPortal() { return portal; }
    public CraftingStation getCraftingStation() { return craftingStation; }
    public Array<Item> getItems() { return items; }
    public Array<Obstacle> getObstacles() { return obstacles; }
    public Array<MissionCollectible> getCollectibles() { return collectibles; }
    public Array<RepairStation> getRepairStations() { return repairStations; }
    public Array<Enemy> getEnemies() { return enemies; }
    public Array<EnemyPulse> getEnemyPulses() { return enemyPulses; }
    public ReachabilityGrid getNavigation() { return navigation; }
}

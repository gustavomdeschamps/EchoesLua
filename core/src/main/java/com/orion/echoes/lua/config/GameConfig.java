package com.orion.echoes.lua.config;

public final class GameConfig {

    private GameConfig() {
    }

    // ==========================================
    // JANELA
    // ==========================================

    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;

    // ==========================================
    // MUNDO
    // ==========================================

    public static final float WORLD_WIDTH = 2200f;
    public static final float WORLD_HEIGHT = 1400f;

    // ==========================================
    // PLAYER
    // ==========================================

    public static final float PLAYER_START_X = 350f;
    public static final float PLAYER_START_Y = 350f;

    // Astronauta maior que os itens
    public static final float PLAYER_WIDTH = 72f;
    public static final float PLAYER_HEIGHT = 96f;

    public static final float PLAYER_SPEED = 180f;

    // ==========================================
    // SOBREVIVÊNCIA
    // ==========================================

    public static final float MAX_OXYGEN = 100f;
    public static final float MAX_ENERGY = 100f;

    public static final float OXYGEN_CONSUMPTION = 2f;

    public static final float BASE_OXYGEN_RECHARGE = 20f;

    public static final float OXYGEN_ITEM_VALUE = 30f;
    public static final float FOOD_ITEM_VALUE = 30f;

    // ==========================================
    // ITENS
    // ==========================================

    public static final float ITEM_SIZE = 64f;

    public static final int MIN_OXYGEN_ITEMS = 2;
    public static final int MIN_FOOD_ITEMS = 2;
    public static final int MIN_ICE_ITEMS = 2;

    // ==========================================
    // BASE LUNAR
    // ==========================================

    public static final float BASE_X = 900f;
    public static final float BASE_Y = 600f;

    public static final float BASE_WIDTH = 420f;
    public static final float BASE_HEIGHT = 350f;

    // ==========================================
    // BOX2D
    // ==========================================

    public static final float PPM = 32f;

    public static final float TIME_STEP = 1f / 60f;

    public static final int VELOCITY_ITERATIONS = 6;
    public static final int POSITION_ITERATIONS = 2;

    // ==========================================
    // VITÓRIA
    // ==========================================

    public static final float SURVIVAL_TIME_TO_WIN = 60f;
}

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

    public static final float WORLD_WIDTH = 3000f;
    public static final float WORLD_HEIGHT = 1900f;

    // ==========================================
    // PLAYER
    // ==========================================

    public static final float PLAYER_START_X = 350f;
    public static final float PLAYER_START_Y = 350f;

    public static final float PLAYER_WIDTH = 54f;
    public static final float PLAYER_HEIGHT = 76f;

    public static final float PLAYER_SPEED = 180f;
    public static final float PLAYER_RUN_MULTIPLIER = 1.28f;
    public static final float PLAYER_VISUAL_SIZE = 104f;
    public static final int PLAYER_ANIMATION_FRAMES = 4;
    public static final float PLAYER_IDLE_FRAME_TIME = 0.22f;
    public static final float PLAYER_WALK_FRAME_TIME = 0.13f;
    public static final float PLAYER_RUN_FRAME_TIME = 0.09f;
    public static final float PLAYER_DASH_FRAME_TIME = 0.045f;
    public static final float PLAYER_ATTACK_FRAME_TIME = 0.04f;
    public static final float PLAYER_HURT_FRAME_TIME = 0.055f;
    public static final float PLAYER_DEATH_FRAME_TIME = 0.15f;
    public static final float PLAYER_DASH_SPEED = 430f;
    public static final float PLAYER_DASH_DURATION = 0.18f;
    public static final float PLAYER_DASH_COOLDOWN = 0.68f;
    public static final float PLAYER_DASH_ENERGY_COST = 18f;
    public static final float PLAYER_LUNAR_ACCEL_TIME = 0.17f;
    public static final float PLAYER_LUNAR_DECEL_TIME = 0.24f;
    public static final float PLAYER_MARS_ACCEL_TIME = 0.11f;
    public static final float PLAYER_MARS_DECEL_TIME = 0.075f;

    // ==========================================
    // GAME FEEL E CÂMERA
    // ==========================================

    public static final float CAMERA_RESPONSE = 8.5f;
    public static final float CAMERA_LOOKAHEAD_X = 0.18f;
    public static final float CAMERA_LOOKAHEAD_Y = 0.14f;
    public static final float CAMERA_EXPLORATION_ZOOM = 1f;
    public static final float CAMERA_COMBAT_ZOOM = 0.94f;
    public static final float CAMERA_ZOOM_RESPONSE = 5.5f;
    public static final float CAMERA_COMBAT_RADIUS = 500f;
    public static final float JUICE_TRAUMA_DECAY = 1.9f;
    public static final float JUICE_MAX_SHAKE_PIXELS = 11f;
    public static final float JUICE_ZOOM_RECOVERY = 7f;
    public static final float JUICE_HIT_HITSTOP = 0.05f;
    public static final float JUICE_HIT_TRAUMA = 0.42f;
    public static final float JUICE_HIT_ZOOM = 0.018f;
    public static final float JUICE_KILL_HITSTOP = 0.085f;
    public static final float JUICE_KILL_TRAUMA = 0.72f;
    public static final float JUICE_KILL_ZOOM = 0.035f;
    public static final float JUICE_KILL_SLOW_TIME = 0.16f;
    public static final float JUICE_KILL_TIME_SCALE = 0.45f;
    public static final float JUICE_HURT_HITSTOP = 0.04f;
    public static final float JUICE_HURT_TRAUMA = 0.82f;
    public static final float JUICE_HURT_FLASH_TIME = 0.28f;
    public static final float JUICE_DASH_TRAUMA = 0.28f;
    public static final float JUICE_DASH_ZOOM = 0.02f;
    public static final float JUICE_COLLECT_TRAUMA = 0.08f;
    public static final float JUICE_COLLECT_ZOOM = 0.012f;
    public static final float JUICE_REPAIR_TRAUMA = 0.2f;
    public static final float JUICE_REPAIR_ZOOM = 0.028f;
    public static final float JUICE_CRAFT_TRAUMA = 0.34f;
    public static final float JUICE_CRAFT_ZOOM = 0.04f;
    public static final float JUICE_CRAFT_SLOW_TIME = 0.22f;
    public static final float JUICE_CRAFT_TIME_SCALE = 0.72f;

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
    public static final float MAX_FRAME_DELTA = 0.25f;

    public static final int VELOCITY_ITERATIONS = 6;
    public static final int POSITION_ITERATIONS = 2;

}

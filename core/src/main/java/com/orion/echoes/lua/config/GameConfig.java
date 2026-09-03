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
    // HITBOXES
    //
    // A hitbox e derivada do retangulo desenhado, nunca escrita a mao: os
    // dois divergiam e o jogador mirava no sprite enquanto o tiro passava
    // pela caixa antiga. Aqui ficam so o recorte do sprite e a fracao do
    // corpo que conta como colisao.
    // ==========================================

    /** Lado do sprite do hostil lunar e deslocamento em relacao a entidade. */
    public static final float ENEMY_SPRITE_SIZE = 104f;
    public static final float ENEMY_SPRITE_OFFSET_X = -16f;
    public static final float ENEMY_SPRITE_OFFSET_Y = -18f;

    public static final float MARS_DRONE_SPRITE_SIZE = 108f;
    public static final float MARS_CRAWLER_SPRITE_SIZE = 116f;
    public static final float MARS_ENEMY_SPRITE_OFFSET_Y = -18f;

    /** Largura do corpo que colide, como fracao do lado do sprite. */
    public static final float ENEMY_HITBOX_WIDTH_RATIO = 0.5f;
    /** Altura do corpo que colide, como fracao do lado do sprite. */
    public static final float ENEMY_HITBOX_HEIGHT_RATIO = 0.29f;
    /** Altura da base da hitbox dentro do sprite, de baixo para cima. */
    public static final float ENEMY_HITBOX_BASE_RATIO = 0.23f;

    /** Folga que a area de coleta ganha alem do sprite do item. */
    public static final float PICKUP_HITBOX_PADDING = 4f;

    // ==========================================
    // MUNICAO
    // ==========================================

    /** Carga que o rifle recebe ao ser fabricado. */
    public static final int AMMO_ON_CRAFT = 12;
    public static final int AMMO_MAX = 30;
    public static final int AMMO_PER_SHOT = 1;
    /** Celulas de pulso obtidas ao processar uma rocha de gelo na base. */
    public static final int AMMO_PER_ICE = 4;
    /** Celula de energia marciana recuperada em campo. */
    public static final int AMMO_PER_POWER_CELL = 6;
    /** Abaixo disto o HUD passa a alertar. */
    public static final int AMMO_LOW = 3;

    // ==========================================
    // HOSTIS
    // ==========================================

    public static final float ENEMY_BASE_SPEED = 72f;
    public static final float ENEMY_BASE_HP = 3f;

    /** Perseguidor: o comportamento original, usado como referencia. */
    public static final float ENEMY_STALKER_DETECTION = 480f;
    public static final float ENEMY_STALKER_ATTACK_RANGE = 92f;
    public static final float ENEMY_STALKER_TELEGRAPH = 0.36f;

    /** Emboscador: fica parado ate o jogador chegar perto, ai avanca rapido. */
    public static final float ENEMY_AMBUSHER_DETECTION = 210f;
    public static final float ENEMY_AMBUSHER_ATTACK_RANGE = 104f;
    public static final float ENEMY_AMBUSHER_TELEGRAPH = 0.26f;
    public static final float ENEMY_AMBUSHER_SPEED = 1.55f;

    /** Atirador: mantem distancia e dispara pulsos telegrafados. */
    public static final float ENEMY_RANGED_DETECTION = 620f;
    public static final float ENEMY_RANGED_ATTACK_RANGE = 340f;
    public static final float ENEMY_RANGED_TELEGRAPH = 0.52f;
    public static final float ENEMY_RANGED_SPEED = 0.72f;
    /** Distancia que o atirador tenta manter do jogador. */
    public static final float ENEMY_RANGED_KEEP_DISTANCE = 250f;
    public static final float ENEMY_RANGED_COOLDOWN = 1.6f;

    public static final float ENEMY_PULSE_SPEED = 235f;
    public static final float ENEMY_PULSE_SIZE = 26f;
    public static final float ENEMY_PULSE_LIFETIME = 2.6f;
    public static final float ENEMY_PULSE_DAMAGE = 9f;

    // ==========================================
    // BENEFICIOS DE SISTEMA REPARADO
    // ==========================================

    /** Estufa: oxigenio regenerado por segundo mesmo fora da base. */
    public static final float PERK_GREENHOUSE_OXYGEN = 0.85f;
    /** Energia: multiplicador de recarga de energia e de oxigenio na base. */
    public static final float PERK_ENERGY_RECHARGE = 2.1f;
    /** Extracao: multiplicador de rendimento ao processar gelo. */
    public static final float PERK_EXTRACTION_YIELD = 2f;
    /** Recuperacao de energia por segundo dentro da base. */
    public static final float BASE_ENERGY_RECHARGE = 9f;

    // ==========================================
    // AUDIO
    // ==========================================

    public static final float MUSIC_MENU_VOLUME = 0.32f;
    public static final float MUSIC_BASE_VOLUME = 0.58f;
    public static final float MUSIC_TENSION_VOLUME = 0.72f;
    public static final float MUSIC_URGENCY_VOLUME = 0.80f;
    public static final float MUSIC_FADE_RESPONSE = 1.6f;
    public static final float MUSIC_DUCK_ATTACK = 12f;
    public static final float MUSIC_DUCK_RELEASE = 2.4f;
    public static final float MUSIC_DUCK_LIGHT = 0.35f;
    public static final float MUSIC_DUCK_STRONG = 0.62f;
    public static final float MUSIC_DUCK_TIME = 0.9f;

    /** Distancia em que uma fonte 2D ainda soa em volume cheio. */
    public static final float AUDIO_NEAR_DISTANCE = 110f;
    /** Distancia em que a fonte fica inaudivel. */
    public static final float AUDIO_FAR_DISTANCE = 780f;
    /** Meia-largura usada para converter deslocamento horizontal em pan. */
    public static final float AUDIO_PAN_WIDTH = 520f;
    public static final float AUDIO_MAX_PAN = 0.85f;
    /** A Lua nao tem atmosfera: som externo chega abafado pelo traje. */
    public static final float AUDIO_VACUUM_GAIN = 0.62f;
    public static final float AUDIO_VACUUM_PITCH = 0.88f;

    /** Raio de inimigo que alimenta a camada de tensao da trilha. */
    public static final float MUSIC_TENSION_RADIUS = 560f;
    /** Abaixo deste oxigenio a camada de urgencia assume. */
    public static final float MUSIC_URGENCY_OXYGEN = 25f;

    // ==========================================
    // BOX2D
    // ==========================================

    public static final float PPM = 32f;

    public static final float TIME_STEP = 1f / 60f;
    public static final float MAX_FRAME_DELTA = 0.25f;

    public static final int VELOCITY_ITERATIONS = 6;
    public static final int POSITION_ITERATIONS = 2;

}

package com.orion.echoes.lua.systems;

/**
 * Geometria caminhável e de conversa de Aharin.
 *
 * Mora fora da Screen porque foi aqui que a fase quebrou: o encontro com as
 * entidades de Luz disparava com {@code x > 560}, ou seja, de qualquer ponto da
 * metade direita do mapa - inclusive fora do terraço e a meia tela de
 * distância. Sendo regra, e não desenho, dá para travar por teste.
 *
 * O terraço é a elipse central da ilustração; a passarela oeste, por onde o
 * jogador chega, é o retângulo que a encosta.
 */
public final class SanctuaryZone {

    public static final float TERRACE_X = 920f, TERRACE_Y = 367f;
    public static final float TERRACE_RX = 295f, TERRACE_RY = 167f;
    public static final float WALKWAY_X = 330f, WALKWAY_Y = 240f;
    public static final float WALKWAY_W = 370f, WALKWAY_H = 105f;

    /** As três entidades de Luz, nas posições em que a arte as desenha. */
    public static final float[][] ENTITIES = {{815f, 372f}, {900f, 380f}, {976f, 368f}};
    public static final float TALK_RADIUS = 132f;

    private SanctuaryZone() { }

    /** True quando o pé do astronauta pisa no terraço ou na passarela. */
    public static boolean walkable(float footX, float footY) {
        if (footX >= WALKWAY_X && footX <= WALKWAY_X + WALKWAY_W
            && footY >= WALKWAY_Y && footY <= WALKWAY_Y + WALKWAY_H) return true;
        float nx = (footX - TERRACE_X) / TERRACE_RX, ny = (footY - TERRACE_Y) / TERRACE_RY;
        return nx * nx + ny * ny <= 1f;
    }

    /** Distância até a entidade de Luz mais próxima. */
    public static float distanceToEntities(float footX, float footY) {
        float best = Float.MAX_VALUE;
        for (float[] entity : ENTITIES) {
            float dx = footX - entity[0], dy = footY - entity[1];
            best = Math.min(best, (float) Math.sqrt(dx * dx + dy * dy));
        }
        return best;
    }

    /** O E só conversa quando o jogador realmente chegou ao santuário. */
    public static boolean canTalk(float footX, float footY) {
        return walkable(footX, footY) && distanceToEntities(footX, footY) <= TALK_RADIUS;
    }
}

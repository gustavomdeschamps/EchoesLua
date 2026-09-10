package com.orion.echoes.lua.systems;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import com.orion.echoes.lua.config.GameConfig;

/**
 * Geometria compartilhada do rifle: punho, boca do cano e lado do corpo.
 *
 * Antes cada leitura tinha a sua conta. O sprite do rifle era desenhado na
 * altura .44 do traje, o projetil nascia em .48 e as tres fases usavam 34px,
 * 33px e 30px de avanco: o traco saia deslocado do cano que o jogador ve, e o
 * deslocamento mudava de mundo para mundo. Desenho, disparo, flash e virada
 * do corpo leem daqui.
 */
public final class WeaponGeometry {

    private WeaponGeometry() { }

    /** Altura do punho no mundo, a partir do canto inferior esquerdo do traje. */
    public static float gripY(float playerY) {
        return playerY + GameConfig.PLAYER_HEIGHT * GameConfig.PLAYER_WEAPON_PIVOT_RATIO;
    }

    /** Centro horizontal do traje: e onde o punho fica, em qualquer lado. */
    public static float gripX(float playerX) {
        return playerX + GameConfig.PLAYER_WIDTH / 2f;
    }

    /**
     * Boca do cano no mundo.
     *
     * @param playerX     canto inferior esquerdo do traje
     * @param playerY     canto inferior esquerdo do traje
     * @param aimDegrees  angulo da mira
     */
    public static Vector2 muzzle(float playerX, float playerY, float aimDegrees, Vector2 out) {
        return out.set(
            gripX(playerX) + MathUtils.cosDeg(aimDegrees) * GameConfig.PLAYER_MUZZLE_DISTANCE,
            gripY(playerY) + MathUtils.sinDeg(aimDegrees) * GameConfig.PLAYER_MUZZLE_DISTANCE);
    }

    /**
     * Lado do corpo a partir da mira, com zona morta perto da vertical.
     *
     * Sem a folga o traje alternava esquerda/direita a cada pixel de mouse
     * enquanto o cursor passava acima ou abaixo do personagem. Dentro da zona
     * morta o lado atual e mantido.
     */
    public static boolean resolveFacingLeft(boolean currentlyLeft, float aimDegrees) {
        float horizontal = MathUtils.cosDeg(aimDegrees);
        if (horizontal > GameConfig.PLAYER_AIM_FACING_DEADZONE) return false;
        if (horizontal < -GameConfig.PLAYER_AIM_FACING_DEADZONE) return true;
        return currentlyLeft;
    }
}

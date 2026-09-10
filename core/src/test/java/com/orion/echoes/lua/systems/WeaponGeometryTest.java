package com.orion.echoes.lua.systems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;

import com.orion.echoes.lua.config.GameConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato do rifle: onde nasce o tiro e para que lado o traje olha.
 *
 * Os dois bugs que estes testes guardam sao invisiveis para o compilador. O
 * primeiro era o traco saindo de uma altura diferente da do cano desenhado,
 * com avanco distinto em cada uma das tres fases. O segundo era o corpo
 * decidindo o lado pelo movimento enquanto o rifle apontava para a mira, o
 * que colocava a arma atravessada nas costas do astronauta.
 */
class WeaponGeometryTest {

    private static final float TOLERANCE = 0.01f;
    private final Vector2 muzzle = new Vector2();

    private static float gripDistance(Vector2 point, float playerX, float playerY) {
        return Vector2.dst(point.x, point.y,
            WeaponGeometry.gripX(playerX), WeaponGeometry.gripY(playerY));
    }

    @Test
    @DisplayName("O punho fica no centro do traje, na altura do sprite do rifle")
    void gripSitsOnTheDrawnWeapon() {
        assertEquals(100f + GameConfig.PLAYER_WIDTH / 2f,
            WeaponGeometry.gripX(100f), TOLERANCE);
        assertEquals(200f + GameConfig.PLAYER_HEIGHT * GameConfig.PLAYER_WEAPON_PIVOT_RATIO,
            WeaponGeometry.gripY(200f), TOLERANCE);
    }

    @Test
    @DisplayName("Mirando para a direita a boca do cano avanca no eixo X")
    void muzzlePointsRight() {
        WeaponGeometry.muzzle(100f, 200f, 0f, muzzle);
        assertEquals(WeaponGeometry.gripX(100f) + GameConfig.PLAYER_MUZZLE_DISTANCE,
            muzzle.x, TOLERANCE);
        assertEquals(WeaponGeometry.gripY(200f), muzzle.y, TOLERANCE);
    }

    @Test
    @DisplayName("Mirando para a esquerda a boca do cano espelha o mesmo avanco")
    void muzzleMirrorsToTheLeft() {
        WeaponGeometry.muzzle(100f, 200f, 180f, muzzle);
        assertEquals(WeaponGeometry.gripX(100f) - GameConfig.PLAYER_MUZZLE_DISTANCE,
            muzzle.x, TOLERANCE);
        assertEquals(WeaponGeometry.gripY(200f), muzzle.y, TOLERANCE);
    }

    /**
     * A invariante que faltava: o tiro nascia sempre a mesma distancia do
     * punho, em qualquer angulo. Sem isso o flash e o traco se descolam do
     * cano justamente nas diagonais.
     */
    @Test
    @DisplayName("A boca do cano guarda a mesma distancia do punho em qualquer angulo")
    void muzzleKeepsConstantDistanceFromTheGrip() {
        for (float angle = -180f; angle <= 180f; angle += 7.5f) {
            WeaponGeometry.muzzle(100f, 200f, angle, muzzle);
            assertEquals(GameConfig.PLAYER_MUZZLE_DISTANCE,
                gripDistance(muzzle, 100f, 200f), TOLERANCE,
                "distância errada no ângulo " + angle);
        }
    }

    @Test
    @DisplayName("A boca do cano fica dentro do sprite desenhado do traje")
    void muzzleStaysInsideTheDrawnSprite() {
        float reach = GameConfig.PLAYER_WIDTH / 2f + GameConfig.PLAYER_MUZZLE_DISTANCE;
        assertTrue(reach <= GameConfig.PLAYER_VISUAL_SIZE,
            "o cano sai " + (reach - GameConfig.PLAYER_VISUAL_SIZE) + "px além do desenho");
    }

    @Test
    @DisplayName("Com arma equipada a mira decide o lado do corpo")
    void aimDrivesTheBodySide() {
        assertFalse(WeaponGeometry.resolveFacingLeft(true, 0f),
            "mirando à direita o corpo tem de virar à direita");
        assertTrue(WeaponGeometry.resolveFacingLeft(false, 180f),
            "mirando à esquerda o corpo tem de virar à esquerda");
        assertFalse(WeaponGeometry.resolveFacingLeft(true, -30f));
        assertTrue(WeaponGeometry.resolveFacingLeft(false, 200f));
    }

    @Test
    @DisplayName("Mirando na vertical o lado atual é mantido")
    void verticalAimKeepsTheCurrentSide() {
        assertTrue(WeaponGeometry.resolveFacingLeft(true, 90f));
        assertFalse(WeaponGeometry.resolveFacingLeft(false, 90f));
        assertTrue(WeaponGeometry.resolveFacingLeft(true, -90f));
        assertFalse(WeaponGeometry.resolveFacingLeft(false, -90f));
    }

    /**
     * Passar o cursor por cima do personagem nao pode fazer o traje piscar de
     * lado. A zona morta cobre a faixa em que o sinal do cosseno e instavel.
     */
    @Test
    @DisplayName("Varrer o cursor pela vertical não alterna o lado a cada grau")
    void sweepingAcrossVerticalDoesNotFlicker() {
        boolean facingLeft = false;
        int flips = 0;
        for (float angle = 60f; angle <= 120f; angle += 1f) {
            boolean next = WeaponGeometry.resolveFacingLeft(facingLeft, angle);
            if (next != facingLeft) flips++;
            facingLeft = next;
        }
        assertEquals(1, flips,
            "o corpo trocou de lado " + flips + " vezes atravessando a vertical");
    }

    @Test
    @DisplayName("A zona morta é folgada o bastante para o jitter do mouse")
    void deadzoneIsWideEnough() {
        assertTrue(GameConfig.PLAYER_AIM_FACING_DEADZONE >= 0.1f,
            "zona morta estreita demais: " + GameConfig.PLAYER_AIM_FACING_DEADZONE);
        assertTrue(GameConfig.PLAYER_AIM_FACING_DEADZONE <= 0.35f,
            "zona morta larga demais, a mira deixa de virar o corpo");
    }
}

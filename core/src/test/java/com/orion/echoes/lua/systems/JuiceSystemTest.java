package com.orion.echoes.lua.systems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.orion.echoes.lua.config.GameConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Hierarquia de impacto e respeito as configuracoes.
 *
 * O documento pede uma escala explicita - coleta discreta, reparo medio,
 * chefe muito forte - e que nada tremous a tela quando o jogador desliga o
 * tremor. Sao regras que ninguem percebe quebradas lendo diff: um ajuste de
 * constante inverte a escala em silencio.
 */
class JuiceSystemTest {

    /** Pico de tremor logo apos o gatilho, antes de qualquer decaimento. */
    private static float traumaPeak(JuiceSystem.Preset preset) {
        JuiceSystem juice = new JuiceSystem();
        juice.trigger(preset);
        juice.update(0f);
        return juice.getCameraOffset().len();
    }

    @Test
    @DisplayName("O impacto do chefe pesa mais que qualquer golpe comum")
    void bossHitsHarderThanRegularCombat() {
        assertTrue(GameConfig.JUICE_BOSS_SLAM_TRAUMA > GameConfig.JUICE_HIT_TRAUMA,
            "o golpe do chefe não pesa mais que um tiro acertado");
        assertTrue(GameConfig.JUICE_BOSS_SLAM_TRAUMA > GameConfig.JUICE_KILL_TRAUMA,
            "o golpe do chefe não pesa mais que abater um hostil comum");
        assertTrue(GameConfig.JUICE_BOSS_SLAM_HITSTOP > GameConfig.JUICE_KILL_HITSTOP,
            "o hit-stop do chefe não é maior que o de um abate comum");
    }

    @Test
    @DisplayName("A queda do chefe é o momento mais forte da campanha")
    void bossDeathIsThePeak() {
        assertTrue(GameConfig.JUICE_BOSS_DEATH_TRAUMA >= GameConfig.JUICE_BOSS_SLAM_TRAUMA);
        assertTrue(GameConfig.JUICE_BOSS_DEATH_HITSTOP > GameConfig.JUICE_BOSS_SLAM_HITSTOP);
        assertTrue(GameConfig.JUICE_BOSS_DEATH_SLOW_TIME > GameConfig.JUICE_KILL_SLOW_TIME,
            "a câmera lenta da vitória não dura mais que a de um abate comum");
        assertTrue(GameConfig.JUICE_BOSS_DEATH_TIME_SCALE < GameConfig.JUICE_KILL_TIME_SCALE,
            "a câmera lenta da vitória não é mais lenta que a de um abate comum");
    }

    @Test
    @DisplayName("Coletar é discreto, reparar é médio, o chefe é forte")
    void impactHierarchyIsPreserved() {
        assertTrue(GameConfig.JUICE_COLLECT_TRAUMA < GameConfig.JUICE_REPAIR_TRAUMA,
            "coletar treme tanto quanto reparar");
        assertTrue(GameConfig.JUICE_REPAIR_TRAUMA < GameConfig.JUICE_HURT_TRAUMA,
            "reparar treme tanto quanto tomar dano");
        assertTrue(GameConfig.JUICE_HURT_TRAUMA < GameConfig.JUICE_BOSS_SLAM_TRAUMA,
            "o dano comum treme tanto quanto o golpe do chefe");
    }

    @Test
    @DisplayName("Coletar não pode aplicar hit-stop")
    void collectingNeverFreezesTheGame() {
        JuiceSystem juice = new JuiceSystem();
        juice.trigger(JuiceSystem.Preset.COLLECT);
        assertEquals(1f / 60f, juice.gameplayDelta(1f / 60f), 0.0001f,
            "uma coleta comum congelou o jogo");
    }

    @Test
    @DisplayName("Com tremor desligado nenhum preset move a câmera")
    void disabledShakeSilencesEveryPreset() {
        for (JuiceSystem.Preset preset : JuiceSystem.Preset.values()) {
            JuiceSystem juice = new JuiceSystem();
            juice.setShakeEnabled(false);
            juice.trigger(preset);
            juice.update(1f / 60f);
            assertEquals(0f, juice.getCameraOffset().len(), 0.0001f,
                "o preset " + preset + " tremeu com o tremor desligado");
        }
    }

    @Test
    @DisplayName("Com tremor ligado o golpe do chefe move a câmera de verdade")
    void enabledShakeMovesTheCamera() {
        assertTrue(traumaPeak(JuiceSystem.Preset.BOSS_SLAM) > 0f,
            "o golpe do chefe não moveu a câmera");
    }

    @Test
    @DisplayName("O tremor nunca passa do teto configurado")
    void shakeStaysUnderTheCeiling() {
        for (JuiceSystem.Preset preset : JuiceSystem.Preset.values()) {
            JuiceSystem juice = new JuiceSystem();
            juice.trigger(preset);
            for (int frame = 0; frame < 30; frame++) {
                juice.update(1f / 60f);
                float offset = Math.max(Math.abs(juice.getCameraOffset().x),
                    Math.abs(juice.getCameraOffset().y));
                assertTrue(offset <= GameConfig.JUICE_MAX_SHAKE_PIXELS + 0.001f,
                    "o preset " + preset + " passou do teto de tremor: " + offset);
            }
        }
    }

    /*
     * Reducao de movimento: some o que desloca a imagem, fica o que informa.
     */
    @Test
    @DisplayName("Com redução de movimento nenhum preset empurra o zoom")
    void reduceMotionRemovesZoomPunch() {
        for (JuiceSystem.Preset preset : JuiceSystem.Preset.values()) {
            JuiceSystem juice = new JuiceSystem();
            juice.setReduceMotion(true);
            juice.trigger(preset);
            assertEquals(0f, juice.getZoomPunch(), 0.0001f,
                "o preset " + preset + " empurrou o zoom com movimento reduzido");
        }
    }

    /**
     * Hit-stop e camera lenta sao coisas diferentes e os dois zeram o delta.
     * Para medir so a camera lenta e preciso passar do hit-stop primeiro,
     * senao o teste passaria mesmo se a camera lenta continuasse ativa.
     */
    @Test
    @DisplayName("Com redução de movimento não há câmera lenta")
    void reduceMotionRemovesSlowMotion() {
        JuiceSystem reduced = new JuiceSystem();
        reduced.setReduceMotion(true);
        reduced.trigger(JuiceSystem.Preset.BOSS_DEATH);
        advancePastHitStop(reduced);
        assertEquals(1f / 60f, reduced.gameplayDelta(1f / 60f), 0.0001f,
            "a câmera lenta continuou com movimento reduzido");

        // Controle: sem a opção, a mesma janela ainda está em câmera lenta.
        JuiceSystem normal = new JuiceSystem();
        normal.trigger(JuiceSystem.Preset.BOSS_DEATH);
        advancePastHitStop(normal);
        assertTrue(normal.gameplayDelta(1f / 60f) < 1f / 60f,
            "o controle deveria estar em câmera lenta; o teste não prova nada");
    }

    /** Avanca o relogio ate o hit-stop terminar, sem encostar na camera lenta. */
    private static void advancePastHitStop(JuiceSystem juice) {
        juice.update(GameConfig.JUICE_BOSS_DEATH_HITSTOP + 0.01f);
    }

    /**
     * O hit-stop e a vinheta ficam de proposito: seguram o quadro e piscam,
     * mas nao deslocam a imagem. Sao a leitura de que o golpe aconteceu, e
     * remove-los junto trocaria acessibilidade por perda de informacao.
     */
    @Test
    @DisplayName("Redução de movimento preserva a leitura do impacto")
    void reduceMotionKeepsImpactReadable() {
        JuiceSystem juice = new JuiceSystem();
        juice.setReduceMotion(true);
        juice.trigger(JuiceSystem.Preset.PLAYER_HURT);
        assertEquals(0f, juice.gameplayDelta(1f / 60f), 0.0001f,
            "o hit-stop do dano sumiu junto com o movimento reduzido");
        assertTrue(juice.getDamageFlashAlpha() > 0f,
            "a vinheta de dano sumiu junto com o movimento");
    }

    @Test
    @DisplayName("Ligar redução de movimento zera o que já estava em curso")
    void enablingReduceMotionCancelsOngoingMotion() {
        JuiceSystem juice = new JuiceSystem();
        juice.trigger(JuiceSystem.Preset.BOSS_DEATH);
        assertTrue(juice.getZoomPunch() > 0f, "o controle não empurrou o zoom");

        juice.setReduceMotion(true);
        assertEquals(0f, juice.getZoomPunch(), 0.0001f,
            "o empurrão de zoom em curso não foi cancelado");
        advancePastHitStop(juice);
        assertEquals(1f / 60f, juice.gameplayDelta(1f / 60f), 0.0001f,
            "a câmera lenta em curso não foi cancelada");
    }

    @Test
    @DisplayName("O tremor decai até zerar sozinho")
    void shakeDecaysToRest() {
        JuiceSystem juice = new JuiceSystem();
        juice.trigger(JuiceSystem.Preset.BOSS_DEATH);
        for (int frame = 0; frame < 180; frame++) juice.update(1f / 60f);
        assertEquals(0f, juice.getCameraOffset().len(), 0.0001f,
            "o tremor continuou depois de três segundos parado");
    }
}

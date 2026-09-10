package com.orion.echoes.lua.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Navegacao das opcoes na pausa.
 *
 * A regra que interessa aqui e a que o jogador sente e o compilador nao ve:
 * a selecao dar a volta, a barra parar nas pontas em vez de acumular fora da
 * faixa, e esquerda/direita significarem a mesma coisa numa barra e num
 * liga/desliga.
 */
class PauseSettingsModelTest {

    /** Estado mutavel simples, no lugar das preferencias reais. */
    private static final class Box {
        float number = .5f;
        boolean flag;
    }

    private static PauseSettingsModel modelWith(Box box) {
        return new PauseSettingsModel()
            .addSlider("Música", new PauseSettingsModel.FloatAccessor() {
                @Override public float get() { return box.number; }
                @Override public void set(float value) { box.number = value; }
            })
            .addToggle("Tremor de câmera", new PauseSettingsModel.BoolAccessor() {
                @Override public boolean get() { return box.flag; }
                @Override public void set(boolean value) { box.flag = value; }
            });
    }

    @Test
    @DisplayName("A seleção dá a volta nas duas pontas")
    void selectionWrapsAround() {
        PauseSettingsModel model = modelWith(new Box());
        assertEquals(0, model.getSelected());
        model.moveSelection(1);
        assertEquals(1, model.getSelected());
        model.moveSelection(1);
        assertEquals(0, model.getSelected(), "não deu a volta para a frente");
        model.moveSelection(-1);
        assertEquals(1, model.getSelected(), "não deu a volta para trás");
    }

    @Test
    @DisplayName("Direita aumenta a barra, esquerda diminui")
    void sliderMovesBothWays() {
        Box box = new Box();
        PauseSettingsModel model = modelWith(box);
        model.adjust(1);
        assertEquals(.5f + PauseSettingsModel.STEP, box.number, .0001f);
        model.adjust(-1);
        model.adjust(-1);
        assertEquals(.5f - PauseSettingsModel.STEP, box.number, .0001f);
    }

    /** Sem o limite, o valor guardado sairia da faixa e voltaria errado. */
    @Test
    @DisplayName("A barra para em 0 e em 100%, sem acumular fora da faixa")
    void sliderClampsAtBothEnds() {
        Box box = new Box();
        PauseSettingsModel model = modelWith(box);
        for (int i = 0; i < 60; i++) model.adjust(1);
        assertEquals(1f, box.number, .0001f);
        for (int i = 0; i < 60; i++) model.adjust(-1);
        assertEquals(0f, box.number, .0001f);
    }

    @Test
    @DisplayName("Num liga/desliga, direita liga e esquerda desliga")
    void toggleFollowsDirection() {
        Box box = new Box();
        PauseSettingsModel model = modelWith(box);
        model.moveSelection(1);

        model.adjust(1);
        assertTrue(box.flag);
        model.adjust(1);
        assertTrue(box.flag, "direita duas vezes desligou; a direção deixou de ser previsível");
        model.adjust(-1);
        assertFalse(box.flag);
        model.adjust(-1);
        assertFalse(box.flag);
    }

    @Test
    @DisplayName("Enter alterna o liga/desliga e não mexe na barra")
    void activateOnlyFlipsToggles() {
        Box box = new Box();
        PauseSettingsModel model = modelWith(box);

        model.activate();
        assertEquals(.5f, box.number, .0001f, "Enter mexeu no valor da barra");

        model.moveSelection(1);
        model.activate();
        assertTrue(box.flag);
        model.activate();
        assertFalse(box.flag);
    }

    @Test
    @DisplayName("Ajustar só afeta a linha selecionada")
    void adjustTouchesOnlyTheSelectedRow() {
        Box box = new Box();
        PauseSettingsModel model = modelWith(box);
        model.adjust(1);
        assertFalse(box.flag, "mexer na barra alterou o liga/desliga");

        model.moveSelection(1);
        float before = box.number;
        model.adjust(1);
        assertEquals(before, box.number, .0001f, "mexer no liga/desliga alterou a barra");
    }

    @Test
    @DisplayName("O texto do valor descreve o estado sem depender de cor")
    void valueTextIsReadableOnItsOwn() {
        Box box = new Box();
        PauseSettingsModel model = modelWith(box);
        assertEquals("50%", model.valueText(0));
        assertEquals("DESLIGADO", model.valueText(1));
        box.flag = true;
        assertEquals("LIGADO", model.valueText(1));
        box.number = 1f;
        assertEquals("100%", model.valueText(0));
    }

    @Test
    @DisplayName("A proporção desenhada acompanha o valor nos dois tipos")
    void ratioReflectsBothKinds() {
        Box box = new Box();
        PauseSettingsModel model = modelWith(box);
        assertEquals(.5f, model.ratio(0), .0001f);
        assertEquals(0f, model.ratio(1), .0001f);
        box.flag = true;
        assertEquals(1f, model.ratio(1), .0001f);
    }

    @Test
    @DisplayName("Uma lista vazia não estoura em nenhuma operação")
    void emptyModelIsSafe() {
        PauseSettingsModel empty = new PauseSettingsModel();
        empty.moveSelection(1);
        empty.adjust(1);
        empty.activate();
        assertEquals(0, empty.size());
        assertEquals(0, empty.getSelected());
    }
}

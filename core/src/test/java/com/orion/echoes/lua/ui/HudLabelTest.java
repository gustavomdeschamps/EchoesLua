package com.orion.echoes.lua.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato do rotulo numerico do HUD.
 *
 * O ponto do rotulo nao e o texto - e nao remontar o texto. Um teste que so
 * conferisse o conteudo passaria de novo se alguem trocasse o corpo por um
 * String.format direto, que e exatamente o que foi removido. Por isso a
 * identidade da instancia e verificada junto com o valor.
 */
class HudLabelTest {

    @Test
    @DisplayName("Monta prefixo, valor e sufixo")
    void buildsTheFullText() {
        assertEquals("O2  87%", new HudLabel("O2  ", "%").of(87));
        assertEquals("GELO  3", new HudLabel("GELO  ", "").of(3));
        assertEquals("ESTAÇÕES  2/3", new HudLabel("ESTAÇÕES  ", "/3").of(2));
    }

    @Test
    @DisplayName("Repetir o mesmo valor devolve a mesma string, sem remontar")
    void repeatedValueReusesTheSameString() {
        HudLabel label = new HudLabel("MUN  ", "");
        String first = label.of(12);
        assertSame(first, label.of(12), "o texto foi remontado sem o valor mudar");
        assertSame(first, label.of(12));
    }

    @Test
    @DisplayName("Mudar o valor remonta o texto")
    void changedValueRebuilds() {
        HudLabel label = new HudLabel("MUN  ", "");
        String first = label.of(12);
        String second = label.of(11);
        assertNotSame(first, second);
        assertEquals("MUN  11", second);
        assertSame(second, label.of(11));
    }

    @Test
    @DisplayName("O primeiro valor sempre monta, mesmo sendo zero")
    void firstCallAlwaysBuilds() {
        HudLabel label = new HudLabel("GELO  ", "");
        assertEquals("GELO  0", label.of(0));
    }

    @Test
    @DisplayName("Rótulo de três valores só remonta quando algum deles muda")
    void multipleValuesShareOneCache() {
        HudLabel vitals = new HudLabel("O2  ", "%  EN  ", "%  MUN  ", "");
        String first = vitals.of(100, 80, 12);
        assertEquals("O2  100%  EN  80%  MUN  12", first);
        assertSame(first, vitals.of(100, 80, 12));
        assertNotSame(first, vitals.of(100, 80, 11));
        assertEquals("O2  99%  EN  80%  MUN  11", vitals.of(99, 80, 11));
    }

    @Test
    @DisplayName("Usar o rótulo com a quantidade errada de valores falha alto")
    void wrongArityIsRejected() {
        HudLabel single = new HudLabel("O2  ", "%");
        assertThrows(IllegalArgumentException.class, () -> single.of(1, 2));
        assertThrows(IllegalArgumentException.class, () -> new HudLabel("só um pedaço"));
    }
}

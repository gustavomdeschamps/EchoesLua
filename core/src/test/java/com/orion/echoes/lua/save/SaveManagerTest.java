package com.orion.echoes.lua.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.orion.echoes.lua.HeadlessGdx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Ida e volta do save lunar, incluindo versionamento e estado de missao. */
class SaveManagerTest {

    private SaveManager saveManager;

    @BeforeAll
    static void bootGdx() {
        HeadlessGdx.ensureStarted();
    }

    @BeforeEach
    void setUp() {
        saveManager = new SaveManager();
        saveManager.deleteSave();
    }

    @Test
    @DisplayName("Sem save gravado o load devolve nulo")
    void loadWithoutSaveReturnsNull() {
        assertFalse(saveManager.hasSave());
        assertNull(saveManager.load());
        assertEquals("Nunca", saveManager.getLastSaveTime());
    }

    @Test
    @DisplayName("Round-trip preserva vitais, inventario e progresso da missao")
    void roundTripKeepsEveryField() {
        GameSaveData original = new GameSaveData(812.5f, 431.25f, 63.5f, 41.75f, 128.5f);
        original.gelo = 3;
        original.agua = 2;
        original.combustivel = 1;
        original.oxigeniosColetados = 4;
        original.comidasColetadas = 5;
        original.rochasGeloColetadas = 6;
        original.baseDescoberta = true;
        original.pecaAntena = 1;
        original.pecaEstufa = 2;
        original.armaParteA = 1;
        original.armaParteB = 1;
        original.armaParteC = 1;
        original.comunicacaoReparada = true;
        original.estufaReparada = true;
        original.armaCraftada = true;
        original.inimigosEliminados = 2;
        original.versao = 2;

        saveManager.save(original);
        assertTrue(saveManager.hasSave());

        GameSaveData loaded = new SaveManager().load();
        assertNotNull(loaded);
        assertEquals(original.posX, loaded.posX);
        assertEquals(original.posY, loaded.posY);
        assertEquals(original.oxigenio, loaded.oxigenio);
        assertEquals(original.energia, loaded.energia);
        assertEquals(original.tempoVivo, loaded.tempoVivo);
        assertEquals(original.gelo, loaded.gelo);
        assertEquals(original.agua, loaded.agua);
        assertEquals(original.combustivel, loaded.combustivel);
        assertEquals(original.oxigeniosColetados, loaded.oxigeniosColetados);
        assertEquals(original.comidasColetadas, loaded.comidasColetadas);
        assertEquals(original.rochasGeloColetadas, loaded.rochasGeloColetadas);
        assertTrue(loaded.baseDescoberta);
        assertEquals(original.pecaAntena, loaded.pecaAntena);
        assertEquals(original.pecaEstufa, loaded.pecaEstufa);
        assertTrue(loaded.comunicacaoReparada);
        assertTrue(loaded.estufaReparada);
        assertFalse(loaded.energiaReparada);
        assertTrue(loaded.armaCraftada);
        assertEquals(original.inimigosEliminados, loaded.inimigosEliminados);
        assertEquals(original.versao, loaded.versao);
    }

    @Test
    @DisplayName("Salvar duas vezes mantem apenas o estado mais recente")
    void savingTwiceKeepsLatest() {
        GameSaveData first = new GameSaveData(100f, 100f, 90f, 90f, 10f);
        saveManager.save(first);
        GameSaveData second = new GameSaveData(700f, 250f, 45f, 30f, 90f);
        second.inimigosEliminados = 3;
        saveManager.save(second);

        GameSaveData loaded = saveManager.load();
        assertNotNull(loaded);
        assertEquals(700f, loaded.posX);
        assertEquals(3, loaded.inimigosEliminados);
    }

    @Test
    @DisplayName("Salvar nulo nao cria nem corrompe o save existente")
    void savingNullIsIgnored() {
        saveManager.save(new GameSaveData(300f, 300f, 100f, 100f, 0f));
        saveManager.save(null);
        GameSaveData loaded = saveManager.load();
        assertNotNull(loaded);
        assertEquals(300f, loaded.posX);
    }

    @Test
    @DisplayName("deleteSave limpa o slot")
    void deleteClearsSlot() {
        saveManager.save(new GameSaveData(300f, 300f, 100f, 100f, 0f));
        saveManager.deleteSave();
        assertFalse(saveManager.hasSave());
        assertNull(saveManager.load());
    }

    @Test
    @DisplayName("Novo jogo comeca com vitais cheios e versao 1")
    void newGameStartsFull() {
        GameSaveData data = saveManager.createNewGameData();
        assertEquals(100f, data.oxigenio);
        assertEquals(100f, data.energia);
        assertEquals(0f, data.tempoVivo);
        assertEquals(1, data.versao);
        assertFalse(data.baseDescoberta);
    }
}

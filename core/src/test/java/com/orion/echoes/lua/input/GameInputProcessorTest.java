package com.orion.echoes.lua.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameInputProcessorTest {

    @Test
    @DisplayName("Clique esquerdo dispara uma vez sem depender das teclas de movimento")
    void leftMouseButtonQueuesOneShot() {
        GameInputProcessor input = new GameInputProcessor();

        assertTrue(input.touchDown(420, 240, 0, Input.Buttons.LEFT));
        assertTrue(input.consumeAttackPressed());
        assertFalse(input.consumeAttackPressed(), "o mesmo clique não pode repetir o tiro");
        assertFalse(input.isMoving(), "clicar não deve inventar direção de movimento");
    }
}

package com.orion.echoes.lua.input;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameInputProcessorTest {
    @Test void spaceIsDashAndNeverShoots() {
        GameInputProcessor input = new GameInputProcessor();
        input.keyDown(Input.Keys.SPACE);
        assertTrue(input.consumeDashPressed());
        assertFalse(input.consumeDashPressed());
        assertFalse(input.consumeAttackPressed());
        input.touchDown(0, 0, 0, Input.Buttons.LEFT);
        assertTrue(input.consumeAttackPressed());
    }
}

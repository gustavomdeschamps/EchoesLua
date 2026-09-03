package com.orion.echoes.lua.systems;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DialogueControllerTest {
    @Test void advancesExactlyOneLineAndFinishesAfterTheLast() {
        DialogueController dialogue = new DialogueController();
        dialogue.start(new String[] {"um", "dois", "três"});
        assertEquals("um", dialogue.line());
        dialogue.next();
        assertEquals("dois", dialogue.line());
        dialogue.next();
        assertEquals("três", dialogue.line());
        dialogue.next();
        assertFalse(dialogue.isOpen());
        assertTrue(dialogue.isFinished());
    }
}

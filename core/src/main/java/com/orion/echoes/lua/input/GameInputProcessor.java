package com.orion.echoes.lua.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;

public class GameInputProcessor implements InputProcessor {

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;

    private boolean interactPressed;

    private final Vector2 direction = new Vector2();

    public Vector2 getDirection() {

        direction.set(0, 0);

        if (up) {
            direction.y += 1;
        }

        if (down) {
            direction.y -= 1;
        }

        if (left) {
            direction.x -= 1;
        }

        if (right) {
            direction.x += 1;
        }

        if (direction.len2() > 0) {
            direction.nor();
        }

        return direction;
    }

    public boolean isMoving() {
        return up || down || left || right;
    }

    public boolean consumeInteractPressed() {

        if (!interactPressed) {
            return false;
        }

        interactPressed = false;

        return true;
    }

    @Override
    public boolean keyDown(int keycode) {

        switch (keycode) {

            case Input.Keys.W:
            case Input.Keys.UP:
                up = true;
                break;

            case Input.Keys.S:
            case Input.Keys.DOWN:
                down = true;
                break;

            case Input.Keys.A:
            case Input.Keys.LEFT:
                left = true;
                break;

            case Input.Keys.D:
            case Input.Keys.RIGHT:
                right = true;
                break;

            case Input.Keys.E:
                interactPressed = true;
                break;
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {

        switch (keycode) {

            case Input.Keys.W:
            case Input.Keys.UP:
                up = false;
                break;

            case Input.Keys.S:
            case Input.Keys.DOWN:
                down = false;
                break;

            case Input.Keys.A:
            case Input.Keys.LEFT:
                left = false;
                break;

            case Input.Keys.D:
            case Input.Keys.RIGHT:
                right = false;
                break;
        }

        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(
        int screenX,
        int screenY,
        int pointer,
        int button
    ) {
        return false;
    }

    @Override
    public boolean touchUp(
        int screenX,
        int screenY,
        int pointer,
        int button
    ) {
        return false;
    }

    @Override
    public boolean touchCancelled(
        int screenX,
        int screenY,
        int pointer,
        int button
    ) {
        return false;
    }

    @Override
    public boolean touchDragged(
        int screenX,
        int screenY,
        int pointer
    ) {
        return false;
    }

    @Override
    public boolean mouseMoved(
        int screenX,
        int screenY
    ) {
        return false;
    }

    @Override
    public boolean scrolled(
        float amountX,
        float amountY
    ) {
        return false;
    }
}

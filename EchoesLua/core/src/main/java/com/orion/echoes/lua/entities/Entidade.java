package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class Entidade {

    protected Vector2 position;
    protected Vector2 velocity;
    protected float width, height;
    protected Rectangle bounds;
    protected boolean ativo = true;

    public Entidade(float x, float y, float width, float height) {
        this.position = new Vector2(x, y);
        this.velocity = new Vector2();
        this.width = width;
        this.height = height;
        this.bounds = new Rectangle(x, y, width, height);
    }

    public abstract void update(float delta);
    public abstract void render(SpriteBatch batch);
    public abstract void dispose();

    public Vector2 getPosition() {
        return position;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}

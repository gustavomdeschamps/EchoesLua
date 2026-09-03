package com.orion.echoes.lua.physics;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;

import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.managers.SoundManager;

public class CollisionListener implements ContactListener {

    private long ultimaColisao = 0;

    @Override
    public void beginContact(
        Contact contact
    ) {

        Object a =
            contact
                .getFixtureA()
                .getBody()
                .getUserData();

        Object b =
            contact
                .getFixtureB()
                .getBody()
                .getUserData();

        verificarColisao(
            a,
            b
        );

        verificarColisao(
            b,
            a
        );
    }

    private void verificarColisao(
        Object primeiro,
        Object segundo
    ) {

        if (
            !(primeiro instanceof Astronauta)
        ) {

            return;
        }

        if (
            "OBSTACLE".equals(segundo)
        ) {

            tocarColisao();
        }
    }

    private void tocarColisao() {

        long agora =
            System.currentTimeMillis();

        // Evita vários sons da mesma batida
        if (
            agora - ultimaColisao
                < 250
        ) {

            return;
        }

        ultimaColisao = agora;

        SoundManager
            .getInstance()
            .tocarColisaoRocha();
    }

    @Override
    public void endContact(
        Contact contact
    ) {
    }

    @Override
    public void preSolve(
        Contact contact,
        Manifold oldManifold
    ) {
    }

    @Override
    public void postSolve(
        Contact contact,
        ContactImpulse impulse
    ) {
    }
}

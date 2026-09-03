package com.orion.echoes.lua;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.ApplicationAdapter;

/**
 * Sobe uma aplicacao LibGDX headless uma unica vez por JVM de teste.
 *
 * SaveManager depende de Gdx.app.getPreferences; sem isso os testes de save
 * teriam de duplicar a serializacao em vez de exercitar o codigo real.
 */
public final class HeadlessGdx {

    private static HeadlessApplication application;

    private HeadlessGdx() { }

    public static synchronized void ensureStarted() {
        if (application != null) return;
        HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
        configuration.updatesPerSecond = -1;
        application = new HeadlessApplication(new ApplicationAdapter() { }, configuration);
        Gdx.gl = null;
    }
}

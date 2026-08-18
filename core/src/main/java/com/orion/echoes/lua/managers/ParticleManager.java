package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class ParticleManager implements Disposable {

    private final Array<ParticleEffect> effects;

    public ParticleManager() {

        effects =
            new Array<>();
    }

    // ==========================================
    // POEIRA LUNAR
    // ==========================================

    public void criarPoeiraLunar(
        float x,
        float y
    ) {

        criarEfeito(
            "particles/poeira.p",
            x,
            y
        );
    }

    // ==========================================
    // COLETA
    // ==========================================

    public void criarEfeitoColeta(
        float x,
        float y
    ) {

        criarEfeito(
            "particles/coleta.p",
            x,
            y
        );
    }

    // ==========================================
    // PROCESSAMENTO
    // ==========================================

    public void criarProcessamento(
        float x,
        float y
    ) {

        criarEfeito(
            "particles/faisca.p",
            x,
            y
        );
    }

    // ==========================================
    // ALERTA O2
    // ==========================================

    public void criarAlertaOxigenio(
        float x,
        float y
    ) {

        criarEfeito(
            "particles/alerta_oxigenio.p",
            x,
            y
        );
    }

    // ==========================================
    // CRIAR EFEITO
    // ==========================================

    private void criarEfeito(
        String arquivo,
        float x,
        float y
    ) {

        if (
            !Gdx.files
                .internal(arquivo)
                .exists()
        ) {

            return;
        }

        ParticleEffect effect =
            new ParticleEffect();

        effect.load(
            Gdx.files.internal(
                arquivo
            ),

            Gdx.files.internal(
                "particles"
            )
        );

        effect.setPosition(
            x,
            y
        );

        effect.start();

        effects.add(
            effect
        );
    }

    // ==========================================
    // UPDATE
    // ==========================================

    public void update(
        float delta
    ) {

        for (
            int i =
            effects.size - 1;

            i >= 0;

            i--
        ) {

            ParticleEffect effect =
                effects.get(i);

            effect.update(
                delta
            );

            if (effect.isComplete()) {

                effect.dispose();

                effects.removeIndex(
                    i
                );
            }
        }
    }

    // ==========================================
    // RENDER
    // ==========================================

    public void render(
        SpriteBatch batch
    ) {

        for (
            ParticleEffect effect
            : effects
        ) {

            effect.draw(batch);
        }
    }

    // ==========================================
    // LIMPAR
    // ==========================================

    public void clear() {

        for (
            ParticleEffect effect
            : effects
        ) {

            effect.dispose();
        }

        effects.clear();
    }

    // ==========================================
    // DISPOSE
    // ==========================================

    @Override
    public void dispose() {

        clear();
    }
}

package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.managers.AssetManager;

/**
 * Personagem com quem o jogador conversa.
 *
 * O diálogo de Titã era uma transmissão de rádio sem ninguém do outro lado —
 * na prática, um monólogo. Aqui existe alguém em cena: o oficial da colônia,
 * com corpo, animação de espera e um indicador que aparece quando o jogador
 * chega perto, para a conversa ser encontrável sem precisar de tutorial.
 *
 * Reaproveita a folha do astronauta com outra tinta: é outro tripulante, e a
 * silhueta compartilhada mantém a coerência de escala com o jogador.
 */
public final class Npc extends Entidade {

    private static final float SPRITE_SIZE = 104f;
    private static final float INTERACT_RADIUS = 132f;

    private final TextureRegion[] idle = new TextureRegion[4];
    private final Color tint;
    private final String nome;
    private float time;
    private boolean jaConversou;

    public Npc(float x, float y, String nome, Color tint, AssetManager assets) {
        super(x, y, 54f, 76f);
        this.nome = nome;
        this.tint = tint;
        for (int column = 0; column < idle.length; column++) {
            idle[column] = assets.astronautFrame(column, 0);
        }
        // Hitbox derivada do desenho: os pés, como no resto do jogo.
        bounds.set(x + (SPRITE_SIZE - 34f) / 2f - (SPRITE_SIZE - width) / 2f,
            y + 6f, 34f, 22f);
    }

    @Override
    public void update(float delta) {
        time += delta;
    }

    /** Perto o bastante para conversar; usa distância, não sobreposição. */
    public boolean isPlayerNear(Astronauta player) {
        float dx = centerX() - (player.getBounds().x + player.getBounds().width / 2f);
        float dy = centerY() - (player.getBounds().y + player.getBounds().height / 2f);
        return dx * dx + dy * dy <= INTERACT_RADIUS * INTERACT_RADIUS;
    }

    public void marcarConversado() { jaConversou = true; }

    public boolean jaConversou() { return jaConversou; }

    public String getNome() { return nome; }

    @Override
    public void render(SpriteBatch batch) {
        float bob = MathUtils.sin(time * 1.9f) * 2.5f;
        TextureRegion frame = idle[(int) (time / .26f) % idle.length];
        batch.setColor(tint);
        batch.draw(frame, centerX() - SPRITE_SIZE / 2f, position.y - 5f + bob,
            SPRITE_SIZE, SPRITE_SIZE);
        batch.setColor(Color.WHITE);
    }

    /**
     * Indicador de conversa disponível.
     *
     * Só aparece enquanto o jogador está no alcance e ainda não conversou —
     * depois disso vira ruído.
     */
    public void renderIndicador(SpriteBatch batch, TextureRegion mark, Astronauta player) {
        if (jaConversou || !isPlayerNear(player)) return;
        float pulse = 1f + MathUtils.sin(time * 5f) * .12f;
        float size = 30f * pulse;
        batch.setColor(1f, .82f, .35f, .92f);
        batch.draw(mark, centerX() - size / 2f, position.y + SPRITE_SIZE - 12f, size, size);
        batch.setColor(Color.WHITE);
    }

    public float centerX() { return position.x + width / 2f; }
    public float centerY() { return position.y + height / 2f; }

    @Override public void dispose() { }
}

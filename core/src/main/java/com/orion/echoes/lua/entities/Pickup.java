package com.orion.echoes.lua.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.orion.echoes.lua.config.GameConfig;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.render.SpriteFit;
import com.badlogic.gdx.math.Rectangle;

/**
 * Coletável de sobrevivência, igual nos três mundos.
 *
 * Oxigênio mantém a exploração possível; gelo é a fonte renovável de munição.
 * Sem os dois, o jogador trava: fica sem ar em trecho longo, ou sem bala e
 * sem como recuperar. São o par que impede a campanha de virar beco sem
 * saída.
 *
 * A hitbox acompanha o sprite, flutuação incluída — a caixa parada enquanto
 * a arte sobe foi um defeito já corrigido no resto do jogo.
 */
public final class Pickup extends Entidade {

    public enum Kind { OXIGENIO, GELO }

    private static final float SIZE = 58f;
    private static final float PADDING = 6f;

    private final Kind kind;
    private final TextureRegion region;
    private final Rectangle drawRect = new Rectangle();
    private final float baseY;
    private float time;
    private boolean coletado;

    public Pickup(float x, float y, Kind kind, AssetManager assets) {
        super(x, y, SIZE, SIZE);
        this.kind = kind;
        this.baseY = y;
        this.region = kind == Kind.OXIGENIO ? assets.oxigenioTexture : assets.geloTexture;
        sincronizar(y);
    }

    @Override
    public void update(float delta) {
        if (coletado) return;
        time += delta;
        sincronizar(baseY + MathUtils.sin(time * 2.3f + position.x * .01f) * 8f);
    }

    /** Desenho e hitbox saem do mesmo retângulo, então nunca divergem. */
    private void sincronizar(float y) {
        SpriteFit.fit(region, position.x, y, SIZE, SIZE, drawRect);
        bounds.set(drawRect.x - PADDING, drawRect.y - PADDING,
            drawRect.width + PADDING * 2f, drawRect.height + PADDING * 2f);
    }

    /** Consome o item se o jogador encostou; devolve false se já foi pego. */
    public boolean coletar(Astronauta player) {
        if (coletado || !bounds.overlaps(player.getBounds())) return false;
        coletado = true;
        ativo = false;
        if (kind == Kind.OXIGENIO) {
            player.recuperarOxigenio(GameConfig.OXYGEN_ITEM_VALUE);
        } else {
            player.adicionarGelo();
            player.registrarColeta(Item.TipoItem.GELO);
        }
        return true;
    }

    public Kind getKind() { return kind; }

    public boolean isColetado() { return coletado; }

    @Override
    public void render(SpriteBatch batch) {
        if (coletado) return;
        float pulse = 1f + MathUtils.sin(time * 3.1f) * .05f;
        float w = drawRect.width * pulse;
        float h = drawRect.height * pulse;
        batch.setColor(kind == Kind.OXIGENIO
            ? new Color(.72f, .92f, 1f, 1f) : new Color(.85f, .95f, 1f, 1f));
        batch.draw(region, drawRect.x + (drawRect.width - w) / 2f,
            drawRect.y + (drawRect.height - h) / 2f, w, h);
        batch.setColor(Color.WHITE);
    }

    public float centerX() { return drawRect.x + drawRect.width / 2f; }
    public float centerY() { return drawRect.y + drawRect.height / 2f; }

    @Override public void dispose() { }
}

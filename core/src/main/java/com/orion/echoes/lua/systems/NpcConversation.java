package com.orion.echoes.lua.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.orion.echoes.lua.entities.Astronauta;
import com.orion.echoes.lua.entities.Npc;
import com.orion.echoes.lua.managers.AssetManager;
import com.orion.echoes.lua.managers.SoundManager;
import com.orion.echoes.lua.render.DialogBox;

/** Connects the existing NPC, controller and dialog renderer to a world. */
public final class NpcConversation {
    private final Npc npc;
    private final String[] lines;
    private final DialogueController dialogue = new DialogueController();
    private final DialogBox box;
    private final AssetManager assets;
    public NpcConversation(Npc npc, String[] lines, SpriteBatch batch, AssetManager assets) {
        this.npc = npc;
        this.lines = lines.clone();
        this.assets = assets;
        box = new DialogBox(batch, assets);
    }
    /** True means consume interaction/attack input and freeze simulation this frame. */
    public boolean update(float delta, Astronauta player, boolean completed, Runnable finish) {
        npc.update(delta);
        if (completed) npc.marcarConversado();
        boolean active = dialogue.isOpen();
        if (active && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            dialogue.next();
            SoundManager.getInstance().tocarDialogo();
            if (dialogue.isFinished()) { npc.marcarConversado(); finish.run(); }
        } else if (!active && !completed && npc.isPlayerNear(player)
            && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            dialogue.start(lines);
            SoundManager.getInstance().tocarDialogo();
            active = true;
        }
        npc.setTalking(dialogue.isOpen());
        box.update(delta, dialogue.isOpen());
        if (active) player.getBody().setLinearVelocity(0f, 0f);
        return active;
    }
    public void renderWorld(SpriteBatch batch, Astronauta player) {
        npc.render(batch);
        npc.renderIndicador(batch, assets.uiObjectiveMarkerTexture, player);
    }
    public void renderUi() { box.render(dialogue, npc.getNome(), npc.getPortraitFrame()); }
}

package com.orion.echoes.lua.systems;

/** Máquina de estados mínima e testável para diálogos lineares. */
public final class DialogueController {
    private String[] lines = new String[0];
    private int index;
    private boolean open;
    private boolean finished;

    public void start(String[] lines) {
        if (lines == null || lines.length == 0) {
            this.lines = new String[0];
            /*
             * Roteiro vazio nao conclui nada.
             *
             * Marcar finished aqui ligaria a flag da missao sem o jogador ter
             * lido uma unica fala.
             */
            index = 0;
            open = false;
            finished = false;
            return;
        }
        this.lines = lines.clone();
        index = 0;
        open = true;
        finished = false;
    }

    public void next() {
        if (!open) return;
        if (index + 1 < lines.length) {
            index++;
            return;
        }
        open = false;
        finished = true;
    }

    public boolean isOpen() { return open; }
    public boolean isFinished() { return finished; }
    public String line() { return open && index < lines.length ? lines[index] : ""; }
}

package com.orion.echoes.lua.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.orion.echoes.lua.config.AppSettings;
import com.orion.echoes.lua.config.GameConfig;

/**
 * Mixer do jogo.
 *
 * Cada efeito pertence a um barramento (SFX, UI ou AMBIENTE) com volume
 * proprio e persistido; a musica vive no {@link MusicDirector}, que responde
 * ao barramento MUSICA. Fontes do mundo tocam por
 * {@link #tocarEspacial}, que atenua por distancia e panoramiza pelo lado
 * da tela.
 */
public class SoundManager implements Disposable {

    /** Barramentos independentes do mixer. */
    public enum Bus { MUSIC, SFX, UI, AMBIENT }

    private static SoundManager instance;

    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private final ObjectMap<String, Integer> variationCursor = new ObjectMap<>();
    private final MusicDirector music = new MusicDirector();

    /** Rodizio de pitch por som repetitivo: evita duas repeticoes identicas seguidas. */
    private static final float[] VARIATION_PITCHES = {0.94f, 1f, 1.06f, 0.97f, 1.03f};

    private float sfxVolume = 0.8f;
    private float uiVolume = 0.75f;
    private float ambientVolume = 0.8f;
    private float masterVolume = 1f;
    private boolean carregado;

    /** Ouvinte usado pelas chamadas espaciais; segue a camera da fase. */
    private float listenerX;
    private float listenerY;
    /** Fases sem atmosfera abafam tudo que nao vem de dentro do traje. */
    private boolean vacuum;

    private SoundManager() { }

    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    public void load() {
        if (carregado) return;
        for (String name : new String[] {
            "coleta", "coleta_oxigenio", "coleta_comida", "coleta_gelo",
            "processar_gelo", "sem_gelo", "base_recarregando", "alerta_oxigenio",
            "menu_iniciar", "pause", "unpause", "game_over", "vitoria",
            "passo_lunar", "colisao_rocha", "hover_ui", "disparo_pulso",
            "passo_marte", "passo_tita", "dialogo", "portal_ativar",
            "boss_rugido", "boss_ataque", "boss_morte", "impacto_hostil"
        }) {
            Sound sound = carregar("sounds/" + name + ".ogg");
            if (sound != null) sounds.put(name, sound);
        }
        music.load();
        carregado = true;
    }

    private Sound carregar(String caminho) {
        if (!Gdx.files.internal(caminho).exists()) {
            Gdx.app.log("SoundManager", "Som nao encontrado: " + caminho);
            return null;
        }
        return Gdx.audio.newSound(Gdx.files.internal(caminho));
    }

    // =========================================
    // MIXER
    // =========================================

    /** Liga o mixer as preferencias salvas; chamado na criacao e ao mudar opcoes. */
    public void applySettings(AppSettings settings) {
        masterVolume = settings.getMasterVolume();
        sfxVolume = settings.getSfxVolume();
        uiVolume = settings.getUiVolume();
        // Ambiente tem barramento proprio: passos, vento e maquinario nao podem
        // subir junto com o disparo so porque o jogador quer ouvir o rifle.
        ambientVolume = settings.getAmbientVolume();
        music.setBusVolume(settings.getMusicVolume() * masterVolume);
    }

    private float busVolume(Bus bus) {
        return masterVolume * switch (bus) {
            case UI -> uiVolume;
            case AMBIENT -> ambientVolume;
            case MUSIC -> 1f;
            default -> sfxVolume;
        };
    }

    public MusicDirector getMusic() { return music; }

    public void update(float delta) { music.update(delta); }

    /** Posicao do ouvinte no mundo, normalmente o centro da camera. */
    public void setListener(float x, float y) {
        listenerX = x;
        listenerY = y;
    }

    public void setVacuum(boolean value) { vacuum = value; }

    // =========================================
    // DISPARO
    // =========================================

    private void tocar(String name, Bus bus, float volume) {
        tocar(name, bus, volume, 1f, 0f);
    }

    private void tocar(String name, Bus bus, float volume, float pitch, float pan) {
        Sound sound = sounds.get(name);
        if (sound == null) return;
        float gain = MathUtils.clamp(volume * busVolume(bus), 0f, 1f);
        if (gain <= 0.001f) return;
        sound.play(gain, MathUtils.clamp(pitch, 0.5f, 2f), MathUtils.clamp(pan, -1f, 1f));
    }

    /** Toca com o proximo pitch do rodizio, sem repetir o anterior. */
    private void tocarVariado(String name, Bus bus, float volume) {
        int cursor = variationCursor.get(name, 0);
        variationCursor.put(name, (cursor + 1) % VARIATION_PITCHES.length);
        float jitter = MathUtils.random(-0.015f, 0.015f);
        tocar(name, bus, volume * MathUtils.random(0.94f, 1.06f),
            VARIATION_PITCHES[cursor] + jitter, 0f);
    }

    /**
     * Fonte posicionada no mundo: perde volume com a distancia, panoramiza pelo
     * lado da tela e, no vacuo, chega abafada e mais grave.
     */
    public void tocarEspacial(String name, Bus bus, float volume, float x, float y) {
        float dx = x - listenerX;
        float dy = y - listenerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance >= GameConfig.AUDIO_FAR_DISTANCE) return;

        float attenuation = 1f - MathUtils.clamp(
            (distance - GameConfig.AUDIO_NEAR_DISTANCE)
                / (GameConfig.AUDIO_FAR_DISTANCE - GameConfig.AUDIO_NEAR_DISTANCE), 0f, 1f);
        attenuation *= attenuation;
        float pan = MathUtils.clamp(dx / GameConfig.AUDIO_PAN_WIDTH,
            -GameConfig.AUDIO_MAX_PAN, GameConfig.AUDIO_MAX_PAN);

        int cursor = variationCursor.get(name, 0);
        variationCursor.put(name, (cursor + 1) % VARIATION_PITCHES.length);
        float pitch = VARIATION_PITCHES[cursor];
        float gain = volume * attenuation;
        if (vacuum) {
            gain *= GameConfig.AUDIO_VACUUM_GAIN;
            pitch *= GameConfig.AUDIO_VACUUM_PITCH;
        }
        tocar(name, bus, gain, pitch, pan);
    }

    /*
     * Tabela de mixagem.
     *
     * Os ganhos estavam espalhados como literais em cada metodo, cada um
     * escolhido isolado, e a escala tinha saido invertida. Medindo a
     * sonoridade de curto prazo (janela de 300ms, que e o que o ouvido julga
     * num som de disparo unico) vezes o ganho, o hover do menu chegava a
     * 0.190 contra 0.149 do rugido do chefe final, e o blip de pausa batia
     * 0.390 contra 0.373 do alarme de oxigenio: a interface abafava o
     * suporte de vida e o chefe.
     *
     * A ordem agora segue a hierarquia do documento -- alarme de suporte de
     * vida no topo, portal e chefe logo abaixo, fabricacao e coleta no meio,
     * passos ao fundo e interface subordinada a tudo. Os passos usam o mesmo
     * alvo nos tres mundos: era o mesmo evento soando 2.7x diferente entre a
     * Lua e Marte.
     *
     * Alguns ficam no teto de 1.0 porque o material e esparso demais para
     * chegar ao alvo; foram elevados no arquivo ate o mesmo pico dos outros
     * por tools/repair_audio.py, e o que sobra e limite da gravacao.
     */
    private static final float GAIN_ALERTA_OXIGENIO = .90f;
    private static final float GAIN_BOSS = 1f;
    private static final float GAIN_PORTAL = 1f;
    private static final float GAIN_MENU_INICIAR = .57f;
    private static final float GAIN_PROCESSAR_GELO = .54f;
    private static final float GAIN_SEM_GELO = .44f;
    private static final float GAIN_COLETA_OXIGENIO = .47f;
    private static final float GAIN_COLETA_COMIDA = .47f;
    private static final float GAIN_COLETA_GELO = .50f;
    private static final float GAIN_COLETA = .49f;
    private static final float GAIN_DISPARO = 1f;
    private static final float GAIN_IMPACTO = 1f;
    private static final float GAIN_PAUSE = .30f;
    private static final float GAIN_COLISAO_ROCHA = .46f;
    private static final float GAIN_DIALOGO = 1f;
    private static final float GAIN_HOVER_UI = .13f;
    private static final float GAIN_PASSO_LUNAR = .16f;
    private static final float GAIN_PASSO_MARTE = .47f;
    private static final float GAIN_PASSO_TITA = .33f;

    // =========================================
    // COLETAS
    // =========================================

    public void tocarColeta() { tocarVariado("coleta", Bus.SFX, GAIN_COLETA); }
    public void tocarOxigenio() { tocarVariado("coleta_oxigenio", Bus.SFX, GAIN_COLETA_OXIGENIO); }
    public void tocarComida() { tocarVariado("coleta_comida", Bus.SFX, GAIN_COLETA_COMIDA); }
    public void tocarGelo() { tocarVariado("coleta_gelo", Bus.SFX, GAIN_COLETA_GELO); }

    public void tocarColetaEspacial(float x, float y) {
        tocarEspacial("coleta", Bus.SFX, GAIN_COLETA, x, y);
    }

    // =========================================
    // BASE
    // =========================================

    public void tocarProcessarGelo() { tocarVariado("processar_gelo", Bus.SFX, GAIN_PROCESSAR_GELO); }
    public void tocarSemGelo() { tocarVariado("sem_gelo", Bus.SFX, GAIN_SEM_GELO); }
    public void tocarBaseRecarregando() { tocar("base_recarregando", Bus.AMBIENT, .55f); }

    /** Stinger de sistema reparado: mixa a frente abaixando a trilha. */
    public void tocarReparoConcluido() {
        music.duck(GameConfig.MUSIC_DUCK_STRONG, GameConfig.MUSIC_DUCK_TIME);
        tocar("vitoria", Bus.SFX, .5f, 1.28f, 0f);
    }

    public void tocarCraft() {
        music.duck(GameConfig.MUSIC_DUCK_STRONG, GameConfig.MUSIC_DUCK_TIME);
        tocar("processar_gelo", Bus.SFX, .85f, .82f, 0f);
    }

    // =========================================
    // OXIGENIO
    // =========================================

    public void tocarAlertaOxigenio() {
        music.duck(GameConfig.MUSIC_DUCK_LIGHT, GameConfig.MUSIC_DUCK_TIME);
        tocar("alerta_oxigenio", Bus.UI, GAIN_ALERTA_OXIGENIO);
    }

    // =========================================
    // TELAS
    // =========================================

    public void tocarInicio() { tocar("menu_iniciar", Bus.UI, GAIN_MENU_INICIAR); }
    public void tocarPause() { tocar("pause", Bus.UI, GAIN_PAUSE); }
    public void tocarUnpause() { tocar("unpause", Bus.UI, GAIN_PAUSE); }

    public void tocarGameOver() {
        music.duck(GameConfig.MUSIC_DUCK_STRONG, 2.5f);
        tocar("game_over", Bus.UI, .9f);
    }

    public void tocarVitoria() {
        music.duck(GameConfig.MUSIC_DUCK_STRONG, 2.5f);
        tocar("vitoria", Bus.UI, .9f);
    }

    // =========================================
    // MUNDO
    // =========================================

    public void tocarPassoLunar() { tocarVariado("passo_lunar", Bus.AMBIENT, GAIN_PASSO_LUNAR); }
    public void tocarPassoMarte() { tocarVariado("passo_marte", Bus.AMBIENT, GAIN_PASSO_MARTE); }
    public void tocarPassoTita() { tocarVariado("passo_tita", Bus.AMBIENT, GAIN_PASSO_TITA); }
    public void tocarDialogo() { tocarVariado("dialogo", Bus.UI, GAIN_DIALOGO); }
    public void tocarPortal() { tocar("portal_ativar", Bus.SFX, GAIN_PORTAL); }
    public void tocarBoss(String evento, float x, float y) {
        tocarEspacial("boss_" + evento, Bus.SFX, GAIN_BOSS, x, y);
    }

    public void tocarColisaoRocha() { tocarVariado("colisao_rocha", Bus.SFX, GAIN_COLISAO_ROCHA); }

    public void tocarColisaoRocha(float x, float y) {
        tocarEspacial("colisao_rocha", Bus.SFX, GAIN_COLISAO_ROCHA, x, y);
    }

    public void tocarDisparo() { tocarVariado("disparo_pulso", Bus.SFX, GAIN_DISPARO); }

    /** Impacto no inimigo: mesmo sample do disparo, mais curto e agudo. */
    public void tocarImpacto(float x, float y) {
        tocarEspacial("impacto_hostil", Bus.SFX, GAIN_IMPACTO, x, y);
    }

    public void tocarMorteInimigo(float x, float y) {
        music.duck(GameConfig.MUSIC_DUCK_LIGHT, .5f);
        tocarEspacial("game_over", Bus.SFX, .55f, x, y);
    }

    public void tocarAlertaInimigo() { tocar("alerta_oxigenio", Bus.SFX, .34f, 1.16f, 0f); }

    /** Telegraph do inimigo: o jogador precisa localizar de onde vem o ataque. */
    public void tocarAlertaInimigo(float x, float y) {
        tocarEspacial("alerta_oxigenio", Bus.SFX, .42f, x, y);
    }

    // =========================================
    // UI
    // =========================================

    public void tocarHoverUi() { tocar("hover_ui", Bus.UI, GAIN_HOVER_UI); }

    // =========================================
    // MUSICA
    // =========================================

    public void tocarMusicaMenu() { music.play(MusicDirector.Track.MENU); }
    public void pararMusicaMenu() { music.play(MusicDirector.Track.NONE); }
    public void tocarMusicaLunar() { music.play(MusicDirector.Track.LUNAR); }
    public void tocarMusicaMarte() { music.play(MusicDirector.Track.MARS); }
    public void tocarMusicaTita() { music.play(MusicDirector.Track.TITAN); }

    /** Intensidade adaptativa: proximidade de inimigo e oxigenio critico. */
    public void atualizarIntensidade(float tension, float urgency) {
        music.setIntensity(tension, urgency);
    }

    public void alternarMusica() { music.setEnabled(!music.isEnabled()); }

    public boolean isMusicaAtiva() { return music.isEnabled(); }

    @Override
    public void dispose() {
        for (Sound sound : sounds.values()) sound.dispose();
        sounds.clear();
        variationCursor.clear();
        music.dispose();
        carregado = false;
    }
}

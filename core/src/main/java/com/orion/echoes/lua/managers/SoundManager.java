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
            "passo_lunar", "colisao_rocha", "hover_ui", "disparo_pulso"
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
        sfxVolume = settings.getSfxVolume();
        uiVolume = settings.getUiVolume();
        ambientVolume = settings.getSfxVolume();
        music.setBusVolume(settings.getMusicVolume());
    }

    private float busVolume(Bus bus) {
        return switch (bus) {
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

    // =========================================
    // COLETAS
    // =========================================

    public void tocarColeta() { tocarVariado("coleta", Bus.SFX, .65f); }
    public void tocarOxigenio() { tocarVariado("coleta_oxigenio", Bus.SFX, .85f); }
    public void tocarComida() { tocarVariado("coleta_comida", Bus.SFX, .75f); }
    public void tocarGelo() { tocarVariado("coleta_gelo", Bus.SFX, .85f); }

    public void tocarColetaEspacial(float x, float y) {
        tocarEspacial("coleta", Bus.SFX, .65f, x, y);
    }

    // =========================================
    // BASE
    // =========================================

    public void tocarProcessarGelo() { tocarVariado("processar_gelo", Bus.SFX, .9f); }
    public void tocarSemGelo() { tocarVariado("sem_gelo", Bus.SFX, .75f); }
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
        tocar("alerta_oxigenio", Bus.UI, .8f);
    }

    // =========================================
    // TELAS
    // =========================================

    public void tocarInicio() { tocar("menu_iniciar", Bus.UI, .8f); }
    public void tocarPause() { tocar("pause", Bus.UI, .65f); }
    public void tocarUnpause() { tocar("unpause", Bus.UI, .65f); }

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

    public void tocarPassoLunar() { tocarVariado("passo_lunar", Bus.AMBIENT, .22f); }

    public void tocarColisaoRocha() { tocarVariado("colisao_rocha", Bus.SFX, .35f); }

    public void tocarColisaoRocha(float x, float y) {
        tocarEspacial("colisao_rocha", Bus.SFX, .45f, x, y);
    }

    public void tocarDisparo() { tocarVariado("disparo_pulso", Bus.SFX, .72f); }

    /** Impacto no inimigo: mesmo sample do disparo, mais curto e agudo. */
    public void tocarImpacto(float x, float y) {
        tocarEspacial("colisao_rocha", Bus.SFX, .8f, x, y);
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

    public void tocarHoverUi() { tocar("hover_ui", Bus.UI, .35f); }

    // =========================================
    // MUSICA
    // =========================================

    public void tocarMusicaMenu() { music.play(MusicDirector.Track.MENU); }
    public void pararMusicaMenu() { music.play(MusicDirector.Track.NONE); }
    public void tocarMusicaLunar() { music.play(MusicDirector.Track.LUNAR); }
    public void tocarMusicaMarte() { music.play(MusicDirector.Track.MARS); }

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

package com.orion.echoes.lua.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Gerencia Save e Load exclusivamente
 * da Fase Lunar.
 */
public class SaveManager {

    private static final String PREFS_NAME =
        "echoes_moon_save";

    private static final String KEY_HAS_SAVE =
        "has_save";

    private static final String KEY_SAVE_DATA =
        "save_data_json";

    private static final String KEY_LAST_SAVE_TIME =
        "last_save_time";

    private static final String KEY_VERSION =
        "save_version";

    private final Preferences prefs;

    private final Json json;

    public SaveManager() {

        prefs =
            Gdx.app.getPreferences(
                PREFS_NAME
            );

        json =
            new Json();

        json.setOutputType(
            JsonWriter.OutputType.json
        );

        json.setTypeName(null);
    }

    // =====================================================
    // SALVAR
    // =====================================================

    public void save(
        GameSaveData data
    ) {

        if (data == null) {

            Gdx.app.error(
                "SaveManager",
                "Tentativa de salvar dados nulos."
            );

            return;
        }

        try {

            String jsonString =
                json.toJson(data);

            prefs.putBoolean(
                KEY_HAS_SAVE,
                true
            );

            prefs.putString(
                KEY_SAVE_DATA,
                jsonString
            );

            prefs.putLong(
                KEY_LAST_SAVE_TIME,
                System.currentTimeMillis()
            );

            prefs.putInteger(
                KEY_VERSION,
                data.versao
            );

            prefs.flush();

            Gdx.app.log(
                "SaveManager",
                "Fase Lunar salva com sucesso!"
            );

            Gdx.app.log(
                "SaveManager",
                jsonString
            );

        } catch (Exception e) {

            Gdx.app.error(
                "SaveManager",
                "Erro ao salvar: "
                    + e.getMessage()
            );
        }
    }

    // =====================================================
    // CARREGAR
    // =====================================================

    public GameSaveData load() {

        if (!hasSave()) {

            Gdx.app.log(
                "SaveManager",
                "Nenhum save lunar encontrado."
            );

            return null;
        }

        try {

            String jsonString =
                prefs.getString(
                    KEY_SAVE_DATA,
                    ""
                );

            if (jsonString.isEmpty()) {
                return null;
            }

            GameSaveData data =
                json.fromJson(
                    GameSaveData.class,
                    jsonString
                );

            Gdx.app.log(
                "SaveManager",
                "Save lunar carregado: "
                    + data
            );

            return data;

        } catch (Exception e) {

            Gdx.app.error(
                "SaveManager",
                "Erro ao carregar: "
                    + e.getMessage()
            );

            return null;
        }
    }

    // =====================================================
    // EXISTE SAVE?
    // =====================================================

    public boolean hasSave() {

        return prefs.getBoolean(
            KEY_HAS_SAVE,
            false
        );
    }

    // =====================================================
    // ÚLTIMO SAVE
    // =====================================================

    public String getLastSaveTime() {

        long time =
            prefs.getLong(
                KEY_LAST_SAVE_TIME,
                0
            );

        if (time == 0) {
            return "Nunca";
        }

        java.text.SimpleDateFormat sdf =
            new java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm"
            );

        return sdf.format(
            new java.util.Date(time)
        );
    }

    // =====================================================
    // APAGAR SAVE
    // =====================================================

    public void deleteSave() {

        prefs.clear();

        prefs.flush();

        Gdx.app.log(
            "SaveManager",
            "Save lunar apagado."
        );
    }

    // =====================================================
    // NOVO JOGO
    // =====================================================

    public GameSaveData createNewGameData() {

        return new GameSaveData(
            300f,
            300f,
            100f,
            100f,
            0f
        );
    }
}

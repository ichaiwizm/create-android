package com.wizycode.create.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wizycode.create.data.catalog.ModelCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException

/**
 * DataStore Preferences unique pour l'app (CONTRACTS §3.6).
 * Un seul fichier `create_settings` partagé par tout le process.
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "create_settings",
)

/**
 * Store de préférences persistées : dernier modèle/réglages (NATIVE_SPEC §1) + thème.
 *
 * Source de vérité des préférences utilisateur. Exposé en [Flow] réactif ; les écritures
 * sont `suspend` et atomiques via `DataStore.edit`.
 */
class SettingsStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    /** Lecture tolérante : une [IOException] (corruption/lecture) retombe sur les valeurs par défaut. */
    private val preferences: Flow<Preferences> = dataStore.data.catch { throwable ->
        if (throwable is IOException) emit(emptyPreferences()) else throw throwable
    }

    // --- Flux exposés (défauts figés) --------------------------------------------------------

    /** Clé de la famille de modèle sélectionnée (défaut : famille image par défaut du catalogue). */
    val familyKey: Flow<String> = preferences.map { prefs ->
        prefs[KEY_FAMILY] ?: ModelCatalog.DEFAULT_IMAGE_FAMILY_KEY
    }

    /** Clé de variante (Veo Fast/Quality) ou `null` si la famille n'a pas de variante. */
    val variantKey: Flow<String?> = preferences.map { prefs ->
        prefs[KEY_VARIANT]?.takeIf { it.isNotEmpty() }
    }

    /** Réglages choisis = `selections` de CatalogLogic (`Map<field, valeur>`). */
    val paramValues: Flow<Map<String, String>> = preferences.map { prefs ->
        decodeParamValues(prefs[KEY_PARAM_VALUES])
    }

    /** Mode de thème (Système par défaut → light mode par défaut). */
    val themeMode: Flow<ThemeMode> = preferences.map { prefs ->
        ThemeMode.from(prefs[KEY_THEME_MODE])
    }

    /** Dynamic color activé (défaut : `false` — l'accent iris reste la signature). */
    val dynamicColor: Flow<Boolean> = preferences.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: false
    }

    // --- Écritures ---------------------------------------------------------------------------

    /** Persiste le dernier modèle utilisé (famille + variante éventuelle + réglages). */
    suspend fun setLastModel(
        familyKey: String,
        variantKey: String?,
        paramValues: Map<String, String>,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_FAMILY] = familyKey
            if (variantKey.isNullOrEmpty()) prefs.remove(KEY_VARIANT) else prefs[KEY_VARIANT] = variantKey
            prefs[KEY_PARAM_VALUES] = encodeParamValues(paramValues)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DYNAMIC_COLOR] = enabled }
    }

    // --- Sérialisation Map<String,String> (JSON, robuste aux caractères spéciaux) -------------

    private fun encodeParamValues(values: Map<String, String>): String {
        if (values.isEmpty()) return "{}"
        val json = JSONObject()
        for ((key, value) in values) json.put(key, value)
        return json.toString()
    }

    private fun decodeParamValues(raw: String?): Map<String, String> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return try {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, json.optString(key))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private companion object {
        val KEY_FAMILY = stringPreferencesKey("family_key")
        val KEY_VARIANT = stringPreferencesKey("variant_key")
        val KEY_PARAM_VALUES = stringPreferencesKey("param_values")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}

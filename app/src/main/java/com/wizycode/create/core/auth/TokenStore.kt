package com.wizycode.create.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wizycode.create.core.net.dto.PbUserRecord

/**
 * Persistance chiffrée du token PocketBase et du record utilisateur.
 *
 * Contrat (CONTRACTS §3.4) :
 * ```
 * object TokenStore {
 *     fun saveToken(token: String); fun loadToken(): String?; fun clear()
 *     fun saveRecord(record: PbUserRecord); fun loadRecord(): PbUserRecord?
 * }
 * ```
 *
 * Le stockage repose sur [EncryptedSharedPreferences] (AES-256) : le token
 * Bearer PocketBase n'est **jamais** écrit en clair sur le disque. Le record
 * est sérialisé en champs séparés (id / email / name) — pas de JSON en clair,
 * chaque valeur étant elle-même chiffrée par la couche EncryptedSharedPreferences.
 *
 * [init] doit être appelé une fois au démarrage (depuis `CreateApp` /
 * `AppContainer`) avant toute autre opération ; l'appel est idempotent et
 * thread-safe.
 */
object TokenStore {

    private const val PREFS_FILE = "create_secure_prefs"

    private const val KEY_TOKEN = "pb_token"
    private const val KEY_RECORD_ID = "pb_record_id"
    private const val KEY_RECORD_EMAIL = "pb_record_email"
    private const val KEY_RECORD_NAME = "pb_record_name"

    @Volatile
    private var prefs: SharedPreferences? = null

    /**
     * Initialise le coffre chiffré. Idempotent : les appels suivants sont ignorés.
     * Doit être invoqué avec un `applicationContext`.
     */
    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            val appContext = context.applicationContext
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            prefs = EncryptedSharedPreferences.create(
                appContext,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }

    private fun requirePrefs(): SharedPreferences =
        prefs ?: error("TokenStore.init(context) must be called before use")

    // --- Token -------------------------------------------------------------

    fun saveToken(token: String) {
        requirePrefs().edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun loadToken(): String? =
        requirePrefs().getString(KEY_TOKEN, null)

    // --- Record ------------------------------------------------------------

    fun saveRecord(record: PbUserRecord) {
        requirePrefs().edit()
            .putString(KEY_RECORD_ID, record.id)
            .putString(KEY_RECORD_EMAIL, record.email)
            .putString(KEY_RECORD_NAME, record.name)
            .apply()
    }

    fun loadRecord(): PbUserRecord? {
        val p = requirePrefs()
        val id = p.getString(KEY_RECORD_ID, null) ?: return null
        val email = p.getString(KEY_RECORD_EMAIL, null) ?: return null
        val name = p.getString(KEY_RECORD_NAME, null)
        return PbUserRecord(id = id, email = email, name = name)
    }

    // --- Nettoyage ---------------------------------------------------------

    /** Efface le token et le record (déconnexion). */
    fun clear() {
        requirePrefs().edit()
            .remove(KEY_TOKEN)
            .remove(KEY_RECORD_ID)
            .remove(KEY_RECORD_EMAIL)
            .remove(KEY_RECORD_NAME)
            .apply()
    }
}

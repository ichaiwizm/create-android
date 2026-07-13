package com.wizycode.create.data.prefs

/**
 * Mode de thème persisté (CONTRACTS §3.6).
 *
 * - [SYSTEM] : suit le réglage système (défaut — light mode par défaut si le système est en clair).
 * - [LIGHT]  : force le thème clair.
 * - [DARK]   : force le thème sombre (fourni et complet, DESIGN §7).
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** Décodage tolérant depuis la valeur persistée en DataStore. */
        fun from(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

package com.wizycode.create.core.util

/**
 * Construction des URLs de fichiers PocketBase (CONTRACTS §3.8, NATIVE_SPEC §6).
 *
 * Une URL média brute pointe vers `…/api/files/{collectionId}/{recordId}/{filename}`. On lui
 * accroche les suffixes de service :
 * - `?thumb=<spec>`  → miniature (`600x0` largeur feed, `600x600` carré galerie) ;
 * - `?download=1`    → force le téléchargement (utilisé par « Sauver »).
 *
 * Utilisé par les extensions `Generation.thumbFeedUrl()/thumbGridUrl()/downloadUrl()` (CONTRACTS
 * §1.1). Robuste : préserve une éventuelle query existante et remplace le même paramètre plutôt
 * que de l'empiler.
 */
object PbFiles {

    /** Miniature : `url` + `?thumb=<spec>` (ex. `"600x0"`, `"600x600"`). */
    fun thumb(url: String, spec: String): String =
        if (url.isBlank() || spec.isBlank()) url else withParam(url, "thumb", spec)

    /** Téléchargement forcé : `url` + `?download=1`. */
    fun download(url: String): String =
        if (url.isBlank()) url else withParam(url, "download", "1")

    /**
     * Ajoute (ou remplace) `name=value` dans la query de [url], en conservant l'ancre `#…`
     * éventuelle et les autres paramètres.
     */
    private fun withParam(url: String, name: String, value: String): String {
        val fragmentIndex = url.indexOf('#')
        val fragment = if (fragmentIndex >= 0) url.substring(fragmentIndex) else ""
        val base = if (fragmentIndex >= 0) url.substring(0, fragmentIndex) else url

        val queryIndex = base.indexOf('?')
        if (queryIndex < 0) {
            return "$base?$name=$value$fragment"
        }

        val path = base.substring(0, queryIndex)
        val query = base.substring(queryIndex + 1)
        val kept = query.split('&').filter { part ->
            part.isNotEmpty() && paramName(part) != name
        }
        val rebuilt = (kept + "$name=$value").joinToString("&")
        return "$path?$rebuilt$fragment"
    }

    private fun paramName(part: String): String {
        val eq = part.indexOf('=')
        return if (eq >= 0) part.substring(0, eq) else part
    }
}

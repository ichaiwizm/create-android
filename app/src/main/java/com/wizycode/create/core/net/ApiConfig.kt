package com.wizycode.create.core.net

import com.wizycode.create.BuildConfig

/**
 * Constantes réseau injectées via `buildConfigField` (surchargeables pour un backend de test).
 *
 * - [APP_BASE] : backend Next.js réutilisé — routes `/api/…` (auth Bearer PocketBase).
 * - [PB_BASE]  : PocketBase direct — uniquement login / refresh d'auth.
 *
 * CONTRACTS §3.1 — noms figés.
 */
object ApiConfig {
    /** "https://create.vpsdashboard.space/api/" */
    val APP_BASE: String = BuildConfig.APP_BASE_URL

    /** "https://pb-create.vpsdashboard.space/" */
    val PB_BASE: String = BuildConfig.PB_BASE_URL
}

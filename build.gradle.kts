// Build racine — déclare les plugins (résolus, non appliqués ici) partagés par les sous-projets.
// Projet single-module : seule `:app` applique réellement ces plugins.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

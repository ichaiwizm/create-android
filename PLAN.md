# Create — Plan d'implémentation NATIF Android

**Cible** : Jetpack Compose · Material 3 Expressive (`androidx.compose.material3` 1.4+) · **minSdk 26** ·
targetSdk/compileSdk 35 · JDK 17 · Kotlin 2.0 (K2) · single-module app.
**Stratégie backend** : **A — réutilisation du backend Next.js** (`https://create.vpsdashboard.space/api/*`),
auth par **token Bearer PocketBase** (cf. NATIVE_SPEC §3-4). Le natif n'écrit jamais PB en direct :
toute génération passe par `/api/generate` (le serveur détient la clé kie + l'admin PB).

> Prérequis serveur (une seule modif, cf. NATIVE_SPEC §4) : patcher `getUser()` de
> `/root/apps/create/src/lib/pocketbase/server.ts` pour accepter `Authorization: Bearer <pb_token>`
> **en plus** du cookie `pb_auth`. Sans ce patch, toutes les routes `/api/*` renvoient 401 au client natif.

---

## 1. Décision minSdk 26 — impacts

minSdk **26** (Android 8.0) plutôt que 31 (suggéré par DESIGN.md pour Material You / blur natif).
Conséquences à coder défensivement :

| Fonctionnalité | API requise | Dégradation < API |
|---|---|---|
| `Modifier.blur` (RenderEffect) | 31 | `blurEnabled = SDK_INT >= 31` → surfaces translucides opaques à ≥ 92 % (déjà prévu DESIGN §3/§8) |
| Dynamic color (Material You) | 31 | `useDynamic = pref && SDK_INT >= 31`, sinon `CreateLightScheme`/`CreateDarkScheme` (DESIGN §2.4) |
| `VibratorManager` | 31 | fallback `Vibrator` (`getSystemService`) + `VibrationEffect` (API 26 OK) |
| `POST_NOTIFICATIONS` runtime | 33 | < 33 : permission implicite, pas de demande |
| Edge-to-edge `enableEdgeToEdge` | activity 1.8 lib | OK (compat lib gère 26+) |
| Coil, ExoPlayer/Media3, DataStore, WorkManager, Retrofit | 21+ | OK |

Aucun blocage : toutes les briques cœur (Compose, M3, Coil, Media3, Retrofit, DataStore, FCM) supportent 26.
Le « verre » et l'aurora restent rendus par `drawBehind`/`background` (pas de RenderEffect requis).

---

## 2. Arborescence de fichiers proposée

Package racine `space.vpsdashboard.create`. Un seul module `app`.

```
create-android/
├── settings.gradle.kts
├── build.gradle.kts                      # root (plugins AGP/Kotlin/Compose/KSP versions)
├── gradle/libs.versions.toml             # version catalog (toutes deps §7)
├── gradle.properties                     # AndroidX, JVM args, kotlin.code.style
├── keystore.properties                   # (git-ignored ; généré en CI depuis secrets)
├── local.properties                      # (git-ignored) sdk.dir
├── .github/workflows/android-release.yml # adapté depuis /root/.appstore/android-template (§8)
└── app/
    ├── build.gradle.kts                  # applicationId, signingConfigs, buildTypes, compose
    ├── proguard-rules.pro
    ├── google-services.json              # FCM (git-ignored ; secret CI ou committé selon politique)
    └── src/main/
        ├── AndroidManifest.xml           # perms, deep-link create://, FCM service
        ├── res/
        │   ├── font/                     # instrument_serif_regular/italic, figtree_regular…bold
        │   ├── drawable/                 # ic_* monochromes, noise_128.png (grain aurora), ic_stat_notif
        │   ├── mipmap-*/                 # launcher (adaptive icon iris)
        │   ├── values/                   # strings.xml (FR), themes.xml (splash), colors.xml
        │   └── xml/                       # backup_rules, data_extraction_rules
        └── java/space/vpsdashboard/create/
            ├── CreateApp.kt              # Application (init DI, Coil, notif channels, WorkManager)
            ├── MainActivity.kt           # enableEdgeToEdge + setContent { CreateTheme { CreateRoot() } }
            │
            ├── core/
            │   ├── di/AppContainer.kt    # DI manuel (pas de Hilt) : singletons partagés
            │   ├── net/
            │   │   ├── ApiClient.kt      # Retrofit + OkHttp + interceptors
            │   │   ├── AuthInterceptor.kt        # ajoute Authorization: Bearer
            │   │   ├── TokenAuthenticator.kt     # 401 → refresh token → replay
            │   │   ├── CreateApi.kt      # interface Retrofit (endpoints §4)
            │   │   ├── PbAuthApi.kt      # interface PocketBase direct (login/refresh)
            │   │   └── dto/…             # DTO réseau (§3)
            │   ├── auth/
            │   │   ├── TokenStore.kt     # EncryptedSharedPreferences (token + record)
            │   │   └── SessionManager.kt # StateFlow<AuthState>, login/logout/refresh
            │   ├── media/
            │   │   ├── AudioRecorder.kt  # MediaRecorder AAC/m4a (dictée)
            │   │   ├── MediaSaver.kt     # MediaStore (Sauver dans photothèque)
            │   │   └── FileShare.kt      # FileProvider + Intent.ACTION_SEND
            │   ├── util/
            │   │   ├── Haptics.kt        # tap/select/launch/success/error (§ DESIGN 6.3)
            │   │   ├── TimeAgo.kt
            │   │   └── PbFiles.kt        # construit les URLs ?thumb=… ?download=1
            │   └── push/
            │       ├── CreateFcmService.kt       # FirebaseMessagingService
            │       └── NotificationHelper.kt     # canal "generations", deep-link gallery
            │
            ├── data/
            │   ├── model/…               # domain models (§3) : Generation, ModelFamily…
            │   ├── catalog/ModelCatalog.kt       # port statique de models.ts (§3.4)
            │   ├── GenerationRepository.kt        # generate/list/poll/cancel/delete + cache
            │   ├── CreditsRepository.kt
            │   ├── UploadRepository.kt
            │   └── prefs/SettingsStore.kt         # DataStore : dernier modèle/réglages, thème, dynamicColor
            │
            ├── ui/
            │   ├── theme/
            │   │   ├── Color.kt          # brand constants + IrisBrush (DESIGN §2.1)
            │   │   ├── CreateColorScheme.kt       # Light/Dark schemes (DESIGN §2.2/2.3)
            │   │   ├── Type.kt           # familles + Typography (DESIGN §1)
            │   │   ├── Shape.kt          # Shapes Expressive + bulle-queue (DESIGN §4)
            │   │   ├── Aurora.kt         # DrawScope.auroraLight/Dark (DESIGN §2.5)
            │   │   ├── GlassModifiers.kt # glassSurface/glassStrong/cardSolid (DESIGN §3)
            │   │   ├── Motion.kt         # springs, pressScale, rise/pop/shimmer (DESIGN §6)
            │   │   └── CreateTheme.kt    # MaterialExpressiveTheme + CompositionLocals
            │   ├── nav/
            │   │   ├── CreateRoot.kt     # Scaffold + aurora + bottom nav + NavHost
            │   │   └── Routes.kt
            │   ├── common/               # composables réutilisables (§5.13)
            │   ├── login/                # LoginScreen + LoginViewModel
            │   ├── create/               # CreateScreen, FeedCard, Composer, ModelSheet, SettingsSheet, MicButton, CreateViewModel
            │   ├── gallery/              # GalleryScreen, GalleryViewModel
            │   ├── lightbox/             # LightboxScreen (+ shared element)
            │   └── settings/             # SettingsScreen (thème, dynamic color, logout)
            └── CreateActivityGraph.kt    # câblage ViewModels ↔ repositories
```

**Choix DI** : conteneur manuel (`AppContainer`) exposé via `CreateApp`. Hilt est acceptable mais
surdimensionné pour 4 écrans ; le manuel évite KAPT/KSP annotation overhead et reste lisible.

---

## 3. Modèles de données (data classes)

### 3.1 Auth (PocketBase direct)

```kotlin
// Requête login → POST {PB}/api/collections/users/auth-with-password
data class PbAuthRequest(val identity: String, val password: String)
// Réponse (login + refresh)
data class PbAuthResponse(val token: String, val record: PbUserRecord)
data class PbUserRecord(val id: String, val email: String, val name: String? = null)

sealed interface AuthState {
    data object Unknown : AuthState          // avant lecture du TokenStore
    data object LoggedOut : AuthState
    data class LoggedIn(val userId: String, val email: String) : AuthState
}
```

### 3.2 Génération (domaine — mappé depuis `GenerationDTO`, NATIVE_SPEC §3)

```kotlin
enum class MediaKind { IMAGE, VIDEO }
enum class GenStatus { PENDING, DONE, FAILED, CANCELLED }

data class Generation(
    val id: String,
    val kind: MediaKind,
    val model: String,                 // slug kie (ex "nano-banana-pro", "veo3_fast")
    val prompt: String,
    val status: GenStatus,
    val mediaUrls: List<String>,       // URLs PB /api/files/... déjà rapatriées
    val error: String? = null,
    val creditsConsumed: Int? = null,
    val created: Instant,
)

// DTO réseau (Moshi) — parse tolérant
data class GenerationDto(
    val id: String, val kind: String, val model: String, val prompt: String,
    val status: String, val mediaUrls: List<String> = emptyList(),
    val error: String? = null, val creditsConsumed: Int? = null, val created: String,
)
data class GenerationsResponse(val items: List<GenerationDto>)
```

### 3.3 Requêtes generate / upload / crédits / transcribe

```kotlin
// POST /api/generate — génération normale
data class GenerateRequest(
    val model: String,
    val prompt: String,
    val imageUrls: List<String>? = null,          // édition / i2v
    val options: Map<String, Any?>? = null,       // {aspect_ratio, resolution, duration, sound…}
)
// POST /api/generate — outil 1-clic (upscale / removeBg)
data class ToolRequest(val tool: String, val toolImageUrl: String)
data class GenerateResponse(val id: String, val taskId: String)

data class UploadResponse(val url: String)        // URL temporaire kie (~3 j)
data class CreditsResponse(val credits: Int)
data class TranscribeResponse(val transcript: String)
```

### 3.4 Catalogue modèles (port statique de `models.ts`, NATIVE_SPEC §5)

**Data-driven, identique au web** — génère les sheets Réglages dynamiquement.

```kotlin
enum class ModelKind { IMAGE, VIDEO }

data class ParamSpec(
    val field: String,            // ex "aspect_ratio"
    val label: String,            // ex "Format"
    val values: List<String>,     // ex ["1:1","16:9",…]
    val def: String,
    val numeric: Boolean = false, // -> converti en Int côté options
    val boolean: Boolean = false, // -> converti en Bool
    val boolLabels: Pair<String, String>? = null, // ("Avec son","Sans son")
    val textOnly: Boolean = false,                 // masqué en mode édition
)
data class ModelVariant(val key: String, val label: String, val id: String, val credits: Int)

data class ModelFamily(
    val key: String, val kind: ModelKind, val name: String, val tagline: String,
    val credits: String,                 // libellé "~18-24 cr"
    val textId: String, val editId: String,
    val imageField: String,              // "image_input", "input_urls", "first_frame_url"…
    val imageIsList: Boolean, val maxImages: Int,
    val params: List<ParamSpec>,
    val extraInput: Map<String, Any?> = emptyMap(),  // ex Kling {multi_shots:false, multi_prompt:[]}
    val variants: List<ModelVariant>? = null,        // Veo Fast/Quality
)

data class ToolSpec(val key: String, val label: String, val id: String, val credits: Int)
```

**Contenu figé** (à hardcoder dans `ModelCatalog.kt`, valeurs exactes NATIVE_SPEC §5) :
- IMAGE : `nano-banana-pro` (image_input, liste, max 8, extra `{output_format:"png"}`, params
  aspect_ratio[11 valeurs def 1:1] + resolution[1K/2K/4K def 1K]) ;
  `gpt-image-2` (input_urls, max 16 ; text/edit ids distincts) ;
  `seedream-5-pro` (image_urls, max 10 ; quality basic/high).
- VIDEO : `veo3.1` (**variants** Fast=`veo3_fast`/Quality=`veo3`, imageUrls max 3, aspect 16:9/9:16,
  resolution 720p/1080p, duration 4/6/8 numeric def 8) ;
  `kling-3.0` (image_urls max 2, extra `{multi_shots:false, multi_prompt:[]}`, mode std/pro/4K,
  duration 3/5/8/10/15, sound bool) ;
  `seedance-2` (**first_frame_url single**, max 1, aspect 7 valeurs, resolution 480/720/1080p,
  duration numeric, generate_audio bool def true).
- TOOLS : `upscale` (`topaz/image-upscale`), `removeBg` / Détourer (`recraft/remove-background`).

**Logique de résolution** (port de `buildInput` / `send()`, NATIVE_SPEC §2.2/§5) — implémentée dans
`CreateViewModel.resolveModelId()` + `GenerationRepository`:
- `paramsFor(family, editing)` = `family.params.filter { !editing || !it.textOnly }`.
- slug envoyé = variante Veo choisie ▸ sinon `editId` si `imageUrls` non vide & famille éditable ▸ sinon `textId`.
- `options` = pour chaque param visible : valeur choisie si dans `values` sinon `def` ; cast numeric/boolean.
  (Le **serveur** applique `extraInput` + `imageField`; le natif envoie juste `model/prompt/imageUrls/options`.)

### 3.5 UI state (voir §5)

```kotlin
data class ComposerState(
    val prompt: String = "",
    val refs: List<RefImage> = emptyList(),          // {localUri, uploadedUrl?, uploading}
    val mode: ModelKind = ModelKind.IMAGE,
    val familyKey: String,                           // dernier utilisé (DataStore)
    val variantKey: String? = null,                  // Veo
    val paramValues: Map<String, String> = emptyMap(),
    val uploading: Boolean = false, val submitting: Boolean = false,
)
data class RefImage(val localUri: Uri, val uploadedUrl: String? = null, val uploading: Boolean = false)
```

---

## 4. Couche réseau

### 4.1 Deux bases

| Client | Base URL | Rôle |
|---|---|---|
| `CreateApi` (Retrofit) | `https://create.vpsdashboard.space/api/` | toutes les routes métier, **Bearer** |
| `PbAuthApi` (Retrofit) | `https://pb-create.vpsdashboard.space/` | login + refresh token (pas d'auth préalable) |

Les URLs sont des `BuildConfig` (flavor/`buildConfigField`) pour pouvoir pointer un backend de test.

### 4.2 Endpoints réutilisés (backend existant — NATIVE_SPEC §3)

```kotlin
interface CreateApi {
    @POST("generate")            suspend fun generate(@Body body: GenerateRequest): GenerateResponse
    @POST("generate")            suspend fun runTool(@Body body: ToolRequest): GenerateResponse
    @GET("generations")          suspend fun list(): GenerationsResponse
    @GET("generations/{id}")     suspend fun poll(@Path("id") id: String): GenerationDto   // refresh+rapatrie
    @DELETE("generations/{id}")  suspend fun delete(@Path("id") id: String): Response<Unit>
    @POST("generations/{id}/cancel") suspend fun cancel(@Path("id") id: String): Response<Unit>
    @Multipart @POST("upload")   suspend fun upload(@Part file: MultipartBody.Part): UploadResponse // max 10 Mo
    @GET("credits")              suspend fun credits(): CreditsResponse
    @POST("transcribe")          suspend fun transcribe(@Body audio: RequestBody): TranscribeResponse // Content-Type=mime
    // Push natif : à AJOUTER côté serveur (voir §4.6) — enregistrement token FCM
}

interface PbAuthApi {
    @POST("api/collections/users/auth-with-password")
    suspend fun login(@Body body: PbAuthRequest): PbAuthResponse
    @POST("api/collections/users/auth-refresh")
    suspend fun refresh(@Header("Authorization") bearer: String): PbAuthResponse
}
```

### 4.3 Auth par token Bearer (NATIVE_SPEC §4)

- **Login** : `PbAuthApi.login()` en direct sur PocketBase → `{token, record}`. Token stocké chiffré
  (`EncryptedSharedPreferences` via `TokenStore`).
- **AuthInterceptor** : injecte `Authorization: Bearer <token>` sur toutes les requêtes `CreateApi`.
- **TokenAuthenticator** (OkHttp `Authenticator`) : sur `401`, tente `PbAuthApi.refresh(Bearer token)`
  une fois ; succès → réécrit le token + rejoue la requête ; échec → émet `AuthState.LoggedOut`
  (déconnexion → redirection Login). Le JWT PB dure ~14 j ; refresh proactif au démarrage foreground.
- **`transcribe`** utilise un `Content-Type` dynamique = mime de l'audio (`RequestBody.create(mime, bytes)`).

### 4.4 OkHttp / Retrofit config

- `OkHttpClient`: `AuthInterceptor`, `TokenAuthenticator`, `HttpLoggingInterceptor` (debug only),
  timeouts (connect 15 s, read 60 s — le poll peut être lent), `retryOnConnectionFailure`.
- Converters : **Moshi** (`KotlinJsonAdapterFactory`), tolérant aux champs manquants ;
  `options: Map<String,Any?>` sérialisé tel quel.
- **Coil** `ImageLoader` partagé, header `Authorization: Bearer` **non requis** pour les fichiers PB
  (URLs `/api/files/...` publiques par token de fichier, pas par auth user) → images chargées sans header.
  → utiliser `PbFiles.thumb(url, "600x0")`, `thumb(url,"600x600")`, `download(url)`.

### 4.5 Polling (NATIVE_SPEC §2.2 / §7)

- **Foreground** : `GenerationRepository.pollLoop()` — coroutine `while(active){ delay(4_000)…}`
  déclenchée par le ViewModel actif (Create ou Gallery) via `viewModelScope`, uniquement pour les
  générations `PENDING`. À chaque tick : `poll(id)` sur chaque pending ; transition pending→done/failed
  → haptique + refresh liste + (annonce a11y). Loop annulée quand aucun pending ou écran quitté
  (`repeatOnLifecycle(STARTED)`).
- **Background** : **FCM** réveille l'app à complétion (§4.6). Le poll n'est jamais un service background.

### 4.6 Push natif — modif serveur requise (NATIVE_SPEC §7, DESIGN §5.12)

Le web utilise VAPID ; pour Android on ajoute **FCM**. Travail serveur (hors app, mais à noter) :
1. Nouvelle collection PB `fcm_tokens` (`user`, `token`, `platform`) OU réutiliser `push_subs`.
2. Route `POST /api/push/register-fcm { token }` (auth user) pour enregistrer le token.
3. Dans `refreshGeneration` (`complete.ts`), à la transition pending→done/failed : en plus du web-push,
   envoyer un message FCM (payload : `title`, `body`=prompt tronqué 90 car, `data.url=create://gallery`,
   `data.tag=generationId`) via l'API FCM v1 (service account) ou clé serveur legacy.
- Côté app : `CreateFcmService.onNewToken` → `register-fcm` ; `onMessageReceived` → notif canal
  `generations` (importance HIGH, accent iris, deep-link). Permission `POST_NOTIFICATIONS` (API 33+).

---

## 5. Gestion d'état

**Pattern** : MVVM unidirectionnel. Chaque écran = 1 `ViewModel` (androidx-lifecycle) exposant un
`StateFlow<UiState>` collecté via `collectAsStateWithLifecycle()`. Repositories = source de vérité,
partagés via `AppContainer`. Pas de base locale Room (petit dataset, source = serveur) — cache mémoire
dans `GenerationRepository` (`MutableStateFlow<List<Generation>>`) partagé Create ↔ Gallery pour cohérence.

- **`SessionManager`** : `StateFlow<AuthState>` — pilote la navigation racine (Login vs App).
- **`GenerationRepository`** : `StateFlow<List<Generation>>` unique (les deux écrans le lisent) +
  `generate()`, `refresh()`, `pollPending()`, `cancel()`, `delete()`. Optimistic insert d'une carte
  `PENDING` locale à l'envoi (avant réponse serveur), réconciliée par l'`id` retourné.
- **`SettingsStore`** (DataStore Preferences) : `familyKey`, `variantKey`, `paramValues` (dernier
  modèle/réglages persistés — NATIVE_SPEC §1), `themeMode` (Système/Clair/Sombre), `dynamicColor` bool.
- **`CreditsRepository`** : `StateFlow<Int?>`, refresh toutes les ~45 s + au retour foreground
  (`Lifecycle.Event.ON_RESUME`).
- **Événements one-shot** (erreurs, snackbars, « Copié », navigation) : `Channel`/`SharedFlow`
  `Flow<UiEvent>` par ViewModel, collecté avec `LaunchedEffect`.
- **Process death** : `SavedStateHandle` pour le brouillon de prompt et l'écran courant.

---

## 6. Liste des écrans & composables

Navigation : `NavHost` racine à 2 niveaux — auth gate (`AuthState`) puis bottom-nav 2 onglets.
Lightbox = destination overlay (`Dialog`/route plein écran) avec `SharedTransitionLayout`.

### 6.1 `LoginScreen` (DESIGN §5.11) — hors bottom nav
Composables : `AuroraBackground`, carte `glassStrong` (34 dp), wordmark `Create.` (point `Accent`),
`OutlinedTextField` email (autocap off, `KeyboardType.Email`) + password (toggle œil), `Button`
plein-largeur peint `IrisBrush`, erreur inline `error`. ViewModel : `login(identity,pwd)` →
`SessionManager` → nav App.

### 6.2 `CreateScreen` (le cœur — DESIGN §5.3/5.4) — onglet 1
- `TopBar` (§6.6 commun), `AuroraBackground`.
- **`Feed`** : `LazyColumn(reverseLayout = true)`, 12 dernières générations, collé au composer.
  - `FeedCard(cardSolid, fillMaxWidth(0.72f), Alignment.End, bulle-queue)` avec 4 rendus :
    - `PendingFeedCard` : `shimmer` + `CircularWavyProgressIndicator` + prompt 2 lignes +
      `LinearWavyProgressIndicator` iris + `IconButton` ✕ (cancel).
    - `DoneFeedCard` : `AsyncImage`(`thumb 600x0`) / `VideoThumbnail` muet + prompt·timeAgo·crédits →
      tap = Lightbox (shared element).
    - `FailedFeedCard` : `errorContainer`, « Échec — {error} », tap = réinjecte prompt.
    - `CancelledFeedCard` : grisé « Annulée », tap = réinjecte prompt.
  - `EmptyState` : hero `displayLarge` « Qu'est-ce qu'on *crée* aujourd'hui ? » (*crée* brush iris) +
    3 `SuggestionChip` (prompts figés) → pré-remplit.
- **`Composer`** (`glassStrong` 28 dp, `imePadding`) :
  - `RefsRow` (`LazyRow` vignettes 56 dp + ✕ + badge `ÉDITION`/`IMAGE → VIDÉO`).
  - `PromptField` (`BasicTextField` auto-grow ≤150 dp, placeholder contextuel, action=envoyer).
  - `ActionRow` : `PhotoButton`(PhotoPicker→upload), `MicButton`(§6.5), `ModelChip`(→ModelSheet),
    `SettingsChip`(→SettingsSheet), `Spacer(weight)`, `GenerateButton`(`FilledIconButton` rond 48 dp,
    `IrisBrush`, ↑, disabled états, haptique `launch`+morph).
- **`ModelSheet`** (§6.3), **`SettingsSheet`** (§6.4) : `ModalBottomSheet`.
- ViewModel : `CreateViewModel` (composerState, feed via repo, `send()`, `poll`, `addRef/removeRef`,
  `pickModel/pickVariant/setParam`, `transcribe`).

### 6.3 `ModelSheet` (DESIGN §5.5)
`SingleChoiceSegmentedButtonRow` Image/Vidéo (re-clamp refs) + liste familles (`ListItem`-like : nom
`titleMedium` + tagline + pill crédits ; active = contour iris + coche). Tap = ferme + clamp + haptique.

### 6.4 `SettingsSheet` (générée dynamiquement — DESIGN §5.6)
Depuis `paramsFor(family, editing)` : rangée « Qualité » `FilterChip` si `variants` ; une `FlowRow`
`FilterChip` par param (boolLabels pour bool, suffixe `s` pour Durée, mini-icône ratio `Canvas` pour
Format). Chip actif = `primaryContainer` + bord iris. Vide → « Ce modèle n'a pas de réglages. »

### 6.5 `MicButton` / dictée (DESIGN §5.7)
`AudioRecorder` (MediaRecorder AAC/m4a), tap start/stop, auto-stop 60 s, ignore < 1.2 ko. Halo rouge
`rememberInfiniteTransition` (900 ms — seule boucle tolérée). Stop → `transcribe(bytes,mime)` → append
prompt + haptique `success`. Permission `RECORD_AUDIO`.

### 6.6 Composables communs
`TopBar` (wordmark + `NotifBell` §6.9 + `CreditsChip` §6.8), `CreateBottomBar`
(`FlexibleBottomAppBar`/`NavigationBar` flottante `glassStrong`, 2 items, indicateur pill spring,
haptique select), `AuroraBackground`, glass modifiers, `IrisButton`, `ShimmerBox`, `RatioIcon`.

### 6.7 `GalleryScreen` (DESIGN §5.8) — onglet 2
`LazyVerticalGrid(Fixed(2))` toutes générations (-created) ; bandeau « N en cours » +
`LinearWavyProgressIndicator` iris ; `PullToRefreshBox` (iris) ; poll 4 s ; `GalleryCard` carrée
(`cardSolid` 22 dp, `thumb 600x600`, prompt 1 ligne + modèle + timeAgo ; pending/failed/cancelled) →
tap done = Lightbox. ViewModel partage le repo.

### 6.8 `CreditsChip` (DESIGN §5.9)
`AssistChip` glassSurface, icône `Bolt` iris + `animateIntAsState` (roll-up spring). Refresh 45 s +
foreground via `CreditsRepository`.

### 6.9 `NotifBell` (DESIGN §5.12)
`IconButton`, états unsupported/denied/available/subscribed (glyphe+tint). Toggle = demande
`POST_NOTIFICATIONS` (API 33+) + `register-fcm`.

### 6.10 `LightboxScreen` (DESIGN §5.10) — overlay
`Dialog` plein écran (`usePlatformDefaultWidth=false`) + scrim flouté. Média `AsyncImage`
(`ContentScale.Fit`, `transformable` zoom/pan) ou `ExoPlayer`/`PlayerView`. **Swipe-down** `draggable`
seuil 110 dp (translate+scale+fade, shared-element retour). Barre haut (fermer + méta). Panneau bas
`glassStrong` : prompt(tap=copier+snackbar), grille actions `FilledTonalButton` — **Partager**
(`FileProvider`+`ACTION_SEND`), images → **Upscale**/**Détourer** (`runTool`), **Sauver** (`MediaStore`
`?download=1`), **Supprimer** (double-tap `error`). ViewModel : `LightboxViewModel(generationId)`.

### 6.11 `SettingsScreen` (nouvel écran natif, via TopBar overflow)
Thème (Système/Clair/Sombre), dynamic color (switch, API 31+), déconnexion. `DataStore`.

---

## 7. Dépendances (`libs.versions.toml`)

Versions indicatives cohérentes fin 2025 / M3 Expressive :

| Dépendance | Artifact | Version | Rôle |
|---|---|---|---|
| Kotlin + Compose compiler | `org.jetbrains.kotlin` + `plugin.compose` | 2.0.21 | K2, plugin Compose |
| AGP | `com.android.application` | 8.7.x | build |
| Compose BOM | `androidx.compose:compose-bom` | 2025.10.xx | versions Compose |
| Material 3 (Expressive) | `androidx.compose.material3:material3` | 1.4.0+ | composants + wavy progress, segmented, sheets |
| M3 adaptive | `androidx.compose.material3.adaptive:*` | 1.1.x | insets/adaptif |
| Compose UI/foundation/animation | via BOM | — | `SharedTransitionLayout`, `graphics.shapes` (Morph) |
| graphics-shapes | `androidx.graphics:graphics-shapes` | 1.0.x | MaterialShapes / Morph (DESIGN §4/§6) |
| Activity Compose | `androidx.activity:activity-compose` | 1.9.x | `enableEdgeToEdge`, PhotoPicker |
| Lifecycle + VM Compose | `androidx.lifecycle:lifecycle-viewmodel-compose`, `runtime-compose` | 2.8.x | VM, `collectAsStateWithLifecycle`, `repeatOnLifecycle` |
| Navigation Compose | `androidx.navigation:navigation-compose` | 2.8.x | NavHost |
| DataStore Preferences | `androidx.datastore:datastore-preferences` | 1.1.x | prefs/thème |
| Security Crypto | `androidx.security:security-crypto` | 1.1.0-alpha06 | EncryptedSharedPreferences (token) |
| Retrofit | `com.squareup.retrofit2:retrofit` + `converter-moshi` | 2.11.x | HTTP |
| OkHttp | `com.squareup.okhttp3:okhttp` + `logging-interceptor` | 4.12.x | client, interceptors |
| Moshi | `com.squareup.moshi:moshi-kotlin` (+KSP `moshi-kotlin-codegen`) | 1.15.x | JSON |
| Coil | `io.coil-kt.coil3:coil-compose` + `coil-network-okhttp` | 3.0.x | images/thumbs |
| Media3 (ExoPlayer) | `androidx.media3:media3-exoplayer` + `media3-ui` | 1.5.x | lecture vidéo lightbox |
| Firebase Messaging | `com.google.firebase:firebase-messaging` (BOM `firebase-bom`) | BOM 33.x | FCM push |
| google-services plugin | `com.google.gms.google-services` | 4.4.x | FCM config |
| Accompanist permissions (opt.) | `com.google.accompanist:accompanist-permissions` | 0.36.x | flux permissions (ou API native) |
| KSP | `com.google.devtools.ksp` | assorti Kotlin | Moshi codegen |

**Fonts** : Instrument Serif + Figtree en `res/font` (bundlées, pas de Google Fonts runtime → offline).
Pas de Hilt (DI manuel). Pas de Room (cache mémoire).

---

## 8. Build, signature & CI

### 8.1 Signature (`app/build.gradle.kts`)
```kotlin
val ksProps = Properties().apply {
    file("$rootDir/keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
signingConfigs {
    create("release") {
        storeFile = file(ksProps.getProperty("storeFile", "upload.jks"))
        storePassword = ksProps.getProperty("storePassword")
        keyAlias = ksProps.getProperty("keyAlias")
        keyPassword = ksProps.getProperty("keyPassword")
    }
}
buildTypes { getByName("release") {
    signingConfig = signingConfigs.getByName("release")
    isMinifyEnabled = true; isShrinkResources = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}}
```
`applicationId = "space.vpsdashboard.create"` (à figer = `PACKAGE_NAME` du workflow + Play).

### 8.2 Keystore & secrets — **déjà provisionnés dans `/root/.appstore`**
- Keystore d'upload : `/root/.appstore/upload-keystore.jks` (base64 → secret `ANDROID_KEYSTORE_BASE64`).
- Secrets GitHub injectés par `/root/.appstore/setup-android-secrets.sh <owner/repo> [play-sa.json]`
  depuis `/root/.appstore/android-secrets.env` : `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
  `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `ANDROID_SERVICE_ACCOUNT_JSON` (Play upload).
- Service account Play : `/root/.appstore/play-service-account.json`.
- SDK local : `/root/.appstore/install-android-sdk.sh` installe cmdline-tools + `platforms;android-34`
  + `build-tools;34.0.0` sous `/root/Android/sdk`. **À étendre** : ajouter `platforms;android-35`
  (compileSdk 35 requis par M3 1.4) — une ligne à modifier dans le `sdkmanager`.

### 8.3 Workflow CI — **adapter le template `/root/.appstore/android-template/.github/workflows/android-release.yml`**
Le template existant vise **Capacitor** (`npx cap sync android`, `ANDROID_DIR=android`). Pour ce projet
**natif Compose**, adapter (le reste — keystore decode, secrets, upload Play — est réutilisable tel quel) :
- **Supprimer** l'étape « Installer deps + sync Capacitor » (`npm ci` / `npx cap sync`).
- **`env.ANDROID_DIR`** = `.` (racine du repo = projet Gradle) au lieu de `android`.
- **`env.PACKAGE_NAME`** = `space.vpsdashboard.create`.
- Décodage keystore : écrire `upload.jks` + `keystore.properties` **à la racine** (là où
  `build.gradle.kts` les lit) au lieu de `$ANDROID_DIR/`.
- Build : `./gradlew bundleRelease --no-daemon` à la racine → sortie
  `app/build/outputs/bundle/release/app-release.aab` (= `releaseFiles` du step `upload-google-play`).
- Garder `setup-java@v4 temurin 17` ; ajouter cache Gradle (`actions/setup-java` cache: gradle).
- Déclencheurs : `workflow_dispatch` (choix track internal/alpha/beta/production) + `push` tag `v*`.
- Publication : `r0adkll/upload-google-play@v1` avec `ANDROID_SERVICE_ACCOUNT_JSON` (inchangé).

Résultat : `git tag v0.1.0 && git push --tags` → build AAB signé → upload piste `internal` Play.
Premier envoi manuel de l'AAB sur la console Play requis (créer la fiche + accepter les accords) ; le
script `/root/.appstore/poll.sh` gère l'attente de propagation des contrats (déjà utilisé côté iOS).

---

## 9. Ordre de construction recommandé (jalons)

- **M0 — Squelette & thème.** Projet Gradle, version catalog, `applicationId`, fonts en `res/font`.
  `Color/CreateColorScheme/Type/Shape/Aurora/GlassModifiers/Motion/CreateTheme.kt` (DESIGN §9 ordre 1-4).
  `MainActivity` edge-to-edge + `AuroraBackground` + wordmark. **Livrable** : app qui boote sur l'aurora
  au bon light/dark, typo serif/sans, iris visible.
- **M1 — Auth de bout en bout.** `TokenStore` (EncryptedSharedPreferences), `PbAuthApi`,
  `SessionManager`, `LoginScreen`, `AuthInterceptor`+`TokenAuthenticator`, auth-gate racine.
  **Prérequis serveur** : patch `getUser()` Bearer. **Livrable** : login réel `contact@phone.gs`,
  token persistant, un `GET /api/credits` authentifié qui répond.
- **M2 — Réseau & feed lecture seule.** `ApiClient`, `CreateApi`, DTO/mappers, `GenerationRepository`
  (list), `CreateScreen` feed (cartes done/pending/failed/cancelled), `CreditsChip`. **Livrable** :
  feed inversé affiche les générations existantes + crédits animés.
- **M3 — Catalogue & Composer & génération.** `ModelCatalog` (port complet models.ts), `ComposerState`,
  `PromptField`, `ModelSheet`, `SettingsSheet` dynamique, `resolveModelId`, `POST /api/generate`,
  optimistic pending, polling 4 s. **Livrable** : créer une image nano-banana texte→image et la voir
  passer pending→done.
- **M4 — Images de référence & upload.** PhotoPicker, `UploadRepository` (`/api/upload` multipart),
  `RefsRow`, badges édition/i2v, clamp maxImages, modes édition/vidéo (Veo variants, Seedance i2v).
  **Livrable** : édition d'image + image→vidéo fonctionnelles.
- **M5 — Galerie & Lightbox.** `GalleryScreen` (grille, pull-to-refresh, bandeau), `LightboxScreen`
  (shared element, swipe-down, ExoPlayer), actions Partager/Sauver/Upscale/Détourer/Supprimer.
  **Livrable** : parcours complet consultation + outils 1-clic + partage/sauvegarde photothèque.
- **M6 — Dictée & haptique & motion.** `AudioRecorder`+`MicButton`+`/api/transcribe`, `Haptics`,
  press-scale/rise/pop/shimmer/wavy/morph, réduction de mouvement. **Livrable** : dictée + finitions
  expressives.
- **M7 — Push FCM.** (dépend du travail serveur §4.6) `CreateFcmService`, `NotificationHelper`, canal
  `generations`, `register-fcm`, `POST_NOTIFICATIONS`, deep-link `create://gallery`, `NotifBell`.
  **Livrable** : notif à complétion app en arrière-plan → ouvre la galerie.
- **M8 — Réglages, a11y/RTL, polish.** `SettingsScreen` (thème/dynamic color/logout), passes
  `contentDescription`/TalkBack/LiveRegion, RTL (bulles miroir), dynamic type, contrastes (DESIGN §8).
  **Livrable** : conformité a11y + préférences persistées.
- **M9 — CI & release.** Adapter le workflow (§8.3), `setup-android-secrets.sh`, `install-android-sdk.sh`
  +android-35, premier AAB manuel Play, tag `v0.1.0`. **Livrable** : build signé auto → piste internal.

**Chemin critique** : M0→M1→M2→M3 débloque la valeur produit (créer + voir). M4/M5 complètent le cœur.
M6-M8 = finitions. M7 nécessite une petite évolution backend (FCM) ; M9 réutilise l'infra `/root/.appstore`.
```

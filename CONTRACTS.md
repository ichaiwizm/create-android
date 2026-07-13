# Create Android — CONTRACTS (vocabulaire partagé figé)

**Rôle de ce document** : c'est le **contrat d'interface** que TOUS les modules générés doivent
respecter **à la lettre**. Noms de types, de champs, de tokens, de fonctions, de propriétés :
rien ne s'invente, rien ne se renomme. Si un module a besoin d'un type/token absent d'ici, on
**complète d'abord ce fichier**, puis on code. En cas de divergence entre un module et ce document,
**ce document gagne**.

Cible : **Jetpack Compose · Material 3 Expressive** (`androidx.compose.material3` 1.4+) ·
**minSdk 26** · targetSdk/compileSdk 35 · JDK 17 · Kotlin 2.0 (K2) · **single-module `app`**.
Package racine : **`space.vpsdashboard.create`** · `applicationId = "space.vpsdashboard.create"`.

Sources : `NATIVE_SPEC.md` (produit/backend/catalogue kie), `DESIGN.md` (DA/tokens/motion/formes),
`PLAN.md` (arborescence/jalons). Stratégie backend **A** : réutilisation du backend Next.js
`https://create.vpsdashboard.space/api/*`, auth **token Bearer PocketBase** (jamais d'écriture PB
directe hors login/refresh).

Conventions Kotlin / Compose :
- **DI manuel** (pas de Hilt) : un `AppContainer` construit dans `CreateApp` (Application), les
  repositories/managers sont des singletons partagés, passés explicitement aux `ViewModel` via une
  `ViewModelProvider.Factory` (`CreateActivityGraph`). Pas d'annotation processing pour la DI.
- **MVVM unidirectionnel** : 1 `ViewModel` par écran, exposant un `StateFlow<XxxUiState>` collecté via
  `collectAsStateWithLifecycle()`. Événements one-shot via `SharedFlow<UiEvent>` / `Channel`.
- **Réseau** : Retrofit + OkHttp + Moshi. Toute suspension `suspend`. Dates ISO-8601 → `java.time.Instant`.
- **Aucune** base Room : cache mémoire (`MutableStateFlow`) dans les repositories. Persistance de prefs
  = DataStore Preferences ; token = `EncryptedSharedPreferences`.
- **Light mode par défaut** (suit le système, sinon light). Dark theme fourni et complet (DESIGN §7).
- Namespacing : modèles domaine & DTO au niveau package (`data.model`, `core.net.dto`). Tokens couleur
  = top-level `val` dans `ui.theme.Color`. Rien dans un objet fourre-tout `Theme`.

---

## 1. MODÈLES (data classes) — noms et champs figés

Fichier de rattachement indiqué entre parenthèses. **Ces noms et ces champs sont définitifs.**
Package : `space.vpsdashboard.create.data.model` (domaine) et `core.net.dto` (réseau).

### 1.1 Génération (`data/model/Generation.kt`)

```kotlin
enum class MediaKind { IMAGE, VIDEO }               // sérialisé "image"/"video" (mapper)
enum class GenStatus { PENDING, DONE, FAILED, CANCELLED }

data class Generation(
    val id: String,
    val kind: MediaKind,
    val model: String,                 // slug kie: "nano-banana-pro", "veo3_fast"…
    val prompt: String,
    val status: GenStatus,
    val mediaUrls: List<String>,       // URLs fichiers PB déjà rapatriés
    val error: String? = null,
    val creditsConsumed: Int? = null,
    val created: Instant,              // java.time.Instant, décodé ISO-8601
)
```

Dérivés **calculés** (extensions, NON décodés — noms figés) :
```kotlin
val Generation.isVideo: Boolean            // kind == MediaKind.VIDEO
val Generation.firstMediaUrl: String?      // mediaUrls.firstOrNull()
// via core/util/PbFiles.kt — voir §3.6
fun Generation.thumbFeedUrl(): String?     // firstMediaUrl + "?thumb=600x0"
fun Generation.thumbGridUrl(): String?     // firstMediaUrl + "?thumb=600x600"
fun Generation.downloadUrl(): String?      // firstMediaUrl + "?download=1"
```

### 1.2 DTO réseau génération (`core/net/dto/GenerationDto.kt`)

Parse **tolérant** (Moshi, champs manquants OK). Mappé vers `Generation` par `GenerationMapper`.
```kotlin
data class GenerationDto(
    val id: String,
    val kind: String,
    val model: String,
    val prompt: String,
    val status: String,
    val mediaUrls: List<String> = emptyList(),
    val error: String? = null,
    val creditsConsumed: Int? = null,
    val created: String,
)
data class GenerationsResponse(val items: List<GenerationDto> = emptyList())
```

### 1.3 Catalogue kie — data-driven (`data/model/ModelCatalog.kt`)

Miroir **verbatim** de `models.ts` (NATIVE_SPEC §5). Noms de types et de champs figés.
```kotlin
enum class ModelKind { IMAGE, VIDEO }

data class ParamSpec(
    val field: String,                 // clé options envoyée à /api/generate ("aspect_ratio"…)
    val label: String,                 // "Format", "Résolution", "Durée", "Son"…
    val values: List<String>,
    val def: String,
    val numeric: Boolean = false,      // → Int dans options
    val boolean: Boolean = false,      // → Boolean dans options
    val boolLabels: Pair<String, String>? = null,   // ("Avec son","Sans son")
    val textOnly: Boolean = false,     // masqué en mode édition
)

data class ModelVariant(
    val key: String,                   // "fast" | "quality"
    val label: String,                 // "Rapide" | "Qualité"
    val id: String,                    // slug kie: "veo3_fast" | "veo3"
    val credits: Int,
)

data class ModelFamily(
    val key: String,                   // "nano-banana-pro"
    val kind: ModelKind,
    val name: String,
    val tagline: String,
    val credits: String,               // libellé "~18-24"
    val textId: String,                // slug texte→média
    val editId: String,                // slug édition (i2i / i2v)
    val imageField: String,            // "image_input", "input_urls", "first_frame_url"…
    val imageIsList: Boolean,
    val maxImages: Int,
    val params: List<ParamSpec>,
    val extraInput: Map<String, Any?> = emptyMap(),   // output_format / multi_shots / multi_prompt
    val variants: List<ModelVariant>? = null,          // Veo Fast/Quality
)

data class ToolSpec(
    val key: String,                   // "upscale" | "removeBg"
    val label: String,                 // "Upscale" | "Détourer"
    val id: String,                    // slug kie
    val credits: Int,
)
```

Catalogue statique — **noms figés** (`data/catalog/ModelCatalog.kt`, `object`) :
```kotlin
object ModelCatalog {
    val image: List<ModelFamily>       // 3 familles: nano-banana-pro, gpt-image-2, seedream-5-pro
    val video: List<ModelFamily>       // 3 familles: veo3.1, kling-3.0, seedance-2
    val tools: List<ToolSpec>          // upscale, removeBg
    fun families(kind: ModelKind): List<ModelFamily>
    fun family(key: String): ModelFamily?
    const val DEFAULT_IMAGE_FAMILY_KEY = "nano-banana-pro"
    const val DEFAULT_VIDEO_FAMILY_KEY = "veo3.1"
}
```
Valeurs recopiées **verbatim** de NATIVE_SPEC §5 / PLAN §3.4 :
- IMAGE : `nano-banana-pro` (imageField `image_input`, liste, max 8, extra `{output_format:"png"}`,
  params `aspect_ratio`[11 valeurs, def `1:1`] + `resolution`[1K/2K/4K, def 1K]) ;
  `gpt-image-2` (`input_urls`, max 16 ; textId/editId distincts) ;
  `seedream-5-pro` (`image_urls`, max 10 ; `quality` basic/high).
- VIDEO : `veo3.1` (**variants** Fast=`veo3_fast` / Quality=`veo3`, `imageField` liste max 3,
  `aspect_ratio` 16:9/9:16, `resolution` 720p/1080p, `duration` 4/6/8 numeric def 8) ;
  `kling-3.0` (`image_urls` max 2, extra `{multi_shots:false, multi_prompt:[]}`, `mode` std/pro/4K,
  `duration` 3/5/8/10/15, `sound` bool) ;
  `seedance-2` (`first_frame_url` **single**, max 1, `aspect_ratio` 7 valeurs, `resolution`
  480/720/1080p, `duration` numeric, `generate_audio` bool def true).
- TOOLS : `upscale` (`topaz/image-upscale`), `removeBg`/Détourer (`recraft/remove-background`).

### 1.4 Logique catalogue (`data/catalog/CatalogLogic.kt`) — signatures figées

```kotlin
object CatalogLogic {
    fun paramsFor(family: ModelFamily, editing: Boolean): List<ParamSpec>
    fun resolveModelId(family: ModelFamily, variant: ModelVariant?, hasRefs: Boolean): String
    fun buildOptions(family: ModelFamily, selections: Map<String, String>, editing: Boolean): Map<String, Any?>
    fun settingsSummary(family: ModelFamily, variant: ModelVariant?, selections: Map<String, String>): String  // "16:9 · 8s"
    fun modelButtonLabel(family: ModelFamily, variant: ModelVariant?): String                                   // "Veo 3.1 Rapide · ~80cr"
}
```
`selections` = `Map<field, valeurChoisie>` (String brut, avant cast numeric/boolean par `buildOptions`).
Règles (PLAN §3.4) : `paramsFor` = `params.filter { !editing || !it.textOnly }` ; slug = variante Veo ▸
sinon `editId` si `hasRefs` & famille éditable ▸ sinon `textId` ; `options` = valeur choisie si ∈ `values`
sinon `def`, casté selon `numeric`/`boolean`. Le **serveur** applique `extraInput` + `imageField`.

### 1.5 Auth / session (`data/model/Auth.kt` + `core/auth/`)

```kotlin
data class AuthRecord(val id: String, val email: String, val name: String? = null)

sealed interface AuthState {
    data object Unknown : AuthState                 // avant lecture du TokenStore
    data object LoggedOut : AuthState
    data class LoggedIn(val userId: String, val email: String) : AuthState
}
```

### 1.6 UI state — Composer (`ui/create/ComposerState.kt`)

```kotlin
data class RefImage(val localUri: Uri, val uploadedUrl: String? = null, val uploading: Boolean = false)

data class ComposerState(
    val prompt: String = "",
    val refs: List<RefImage> = emptyList(),
    val mode: ModelKind = ModelKind.IMAGE,
    val familyKey: String = ModelCatalog.DEFAULT_IMAGE_FAMILY_KEY,
    val variantKey: String? = null,                 // Veo
    val paramValues: Map<String, String> = emptyMap(),  // = "selections" de CatalogLogic
    val uploading: Boolean = false,
    val submitting: Boolean = false,
) {
    val editing: Boolean get()                      // refs non vides & famille éditable (dérivé)
}
```

---

## 2. DESIGN SYSTEM — API figée (`ui/theme/`)

Cible : **Material 3 Expressive**. On n'imite pas le Liquid Glass iOS ; on porte l'âme « iris / aurora »
en **surfaces tonales translucides** + **formes très arrondies expressives** + **springs Expressive**.
L'**iris** (violet→bleu→cyan) est la signature immuable, y compris sous dynamic color.

### 2.1 Constantes de marque & brushes (`ui/theme/Color.kt`)

Top-level `val`, **noms figés** (DESIGN §2.1) :
```kotlin
val IrisViolet     = Color(0xFF8B5CF6)
val IrisBlue       = Color(0xFF3B82F6)
val IrisCyan       = Color(0xFF22D3EE)
val IrisTextViolet = Color(0xFF7C3AED)
val IrisTextBlue   = Color(0xFF2563EB)
val IrisTextCyan   = Color(0xFF06B6D4)

val Ink     = Color(0xFF2A3142)   // texte principal (light)
val InkSoft = Color(0xFF5D6478)   // texte secondaire (light)
val Accent  = Color(0xFF2F6DF6)   // bleu accent — point du wordmark "Create."

val IrisBrush     = Brush.linearGradient(0.0f to IrisViolet, 0.55f to IrisBlue, 1.0f to IrisCyan)
val IrisTextBrush = Brush.linearGradient(0.0f to IrisTextViolet, 0.55f to IrisTextBlue, 1.0f to IrisTextCyan)
```
Brushes exposés via `CompositionLocal LocalIrisBrushes` (voir §2.9). Dégradé 135° recalculé sur la
bounding box (`drawWithCache` / `onGloballyPositioned`). Le brush iris **n'est jamais** miroité en RTL.

### 2.2 ColorSchemes (`ui/theme/CreateColorScheme.kt`)

Deux `ColorScheme` M3 **figés** (valeurs hex verbatim DESIGN §2.2 light / §2.3 dark) :
```kotlin
val CreateLightScheme: ColorScheme   // primary #3B82F6, background #EEF0FD, surface #F4F5FE,
                                     // surfaceContainerLowest #FFFFFF (cartes), errorContainer #FEE2E2 …
val CreateDarkScheme: ColorScheme    // primary #9EC0FF, background #0E1220, surface #121728 …
```
Résolution dynamic color (DESIGN §2.4) — **fonction figée** :
```kotlin
@Composable fun createColorScheme(dark: Boolean, dynamic: Boolean): ColorScheme
// useDynamic = dynamic && SDK_INT >= 31 ; dynamic pilote les neutres/surfaces mais
// l'accent produit reste IrisBlue et le brush iris n'est JAMAIS remplacé.
```

### 2.3 Typographie (`ui/theme/Type.kt`)

Familles + `Typography` M3 override **figés** (DESIGN §1) :
```kotlin
val InstrumentSerif: FontFamily   // res/font/instrument_serif_regular|italic — display*/headline*
val Figtree: FontFamily           // res/font/figtree_regular|medium|semibold|bold — tout le reste
val CreateTypography: Typography  // displayLarge 44/serif … labelSmall 11·700 uppercase (échelle DESIGN §1.2)
```
`display*`/`headline*` = Instrument Serif ; `title*`/`body*`/`label*` = Figtree. Le mot hero *crée* se
peint via `TextStyle(brush = IrisTextBrush)` ; le point du wordmark en `Accent`.

### 2.4 Formes (`ui/theme/Shape.kt`)

`Shapes` Expressive gonflées + formes custom **figées** (DESIGN §4) :
```kotlin
val CreateShapes: Shapes   // extraSmall 8 / small 14 / medium 22 / large 28 / extraLarge 34 (dp)

// Bulle de feed (coin bas-droit resserré 10 dp ; miroir bas-gauche en RTL)
fun feedBubbleShape(layoutDirection: LayoutDirection): Shape
// Accents Expressive ponctuels : MaterialShapes.Cookie12Sided / Pill (badge crédits, avatar vide)
// Boutons ronds = CircleShape 44 dp ; chips = CircleShape (pill) hauteur 34 dp
```

### 2.5 Fond Aurora (`ui/theme/Aurora.kt`)

Statique, **zéro animation** (DESIGN §2.5). API figée :
```kotlin
fun DrawScope.auroraLight()   // base linéaire 165° + 4 radiaux pastel (violet/bleu/cyan/bleu-clair)
fun DrawScope.auroraDark()    // base #0E1220 + halos iris ~20 %
@Composable fun AuroraBackground(modifier: Modifier = Modifier)  // drawBehind + grain 128px alpha .04 BlendMode.Overlay
```
Posé à la racine du `Scaffold`, derrière tout le contenu. Ne s'anime jamais.

### 2.6 Verre — modificateurs uniques (`ui/theme/GlassModifiers.kt`)

3 niveaux **figés**. **Tous les modules passent par ces modifiers**, jamais un `background` translucide
ad hoc (DESIGN §3). Le blur est **optionnel** (API 31+, coupé si transparence réduite / < API 31) ;
la lisibilité ne dépend jamais du blur.
```kotlin
val blurEnabled: Boolean   // SDK_INT >= 31 && !reduceTransparency (exposé via CompositionLocal / util)

fun Modifier.glassSurface(shape: Shape): Modifier   // .glass  — surface @60%, chips/petits panneaux
fun Modifier.glassStrong(shape: Shape): Modifier    // .glass-strong — surfaceContainer @80%, nav/header/sheets/composer/lightbox
fun Modifier.cardSolid(shape: Shape): Modifier      // .card-solid — surfaceContainerLowest OPAQUE, JAMAIS de blur (feed/galerie)
```
Conventions (figées) : `glassStrong` → nav, top bar, sheets, composer, barres lightbox, toasts ;
`glassSurface` → chips, chip crédits, boutons ronds secondaires ; `cardSolid` → **cartes répétées en
scroll** (feed, galerie), jamais de blur. Inset highlight blanc 1 dp `Color.White @ 0.5f` en haut des
surfaces `glass*`.

### 2.7 Motion (`ui/theme/Motion.kt`) — springs & modifiers figés

Springs Expressive **nommés figés** (DESIGN §6) :
```kotlin
object Motion {
    val spatialSpring: SpringSpec<Float>   // spring(dampingRatio = 0.8f, stiffness = 380f) — position/taille
    val effectsSpring: SpringSpec<Float>   // spring(dampingRatio = 1f,  stiffness = 700f) — couleur/alpha
    const val DUR_FAST = 120; const val DUR_STD = 240; const val DUR_EMPHATIC = 400   // ms
}
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier   // scale 0.94 sur press
fun Modifier.shimmer(active: Boolean = true): Modifier                           // skeleton diagonal, 1200 ms
@Composable fun rememberReduceMotion(): Boolean   // ANIMATOR_DURATION_SCALE == 0 || pref a11y
```
Progress indéterminé iris = `LinearWavyProgressIndicator` / `CircularWavyProgressIndicator` (M3
Expressive) teintés brush iris. Shared-element feed/galerie → lightbox via `SharedTransitionLayout`.
Compteur crédits via `animateIntAsState` (roll-up spring). Morph bouton rond via
`androidx.graphics.shapes.Morph(circle, rounded)` piloté par le press.

### 2.8 Haptique (`core/util/Haptics.kt`) — API figée

```kotlin
enum class Haptic { TAP, SELECT, LAUNCH, SUCCESS, ERROR }
interface Haptics {
    fun fire(h: Haptic)
    fun prepare(h: Haptic)
}
// Impl figée : passe par LocalHapticFeedback quand possible, fallback VibratorManager (API 31+)
// / Vibrator + VibrationEffect (API 26) pour SUCCESS/ERROR. Exposé via LocalHaptics (CompositionLocal).
```
Mapping (DESIGN §6.3) : TAP=CONTEXT_CLICK · SELECT=SEGMENT_TICK/CLOCK_TICK · LAUNCH=CONFIRM (ou 15 ms) ·
SUCCESS=EFFECT_HEAVY_CLICK · ERROR=waveform `[0,20,60,20]`. **Au plus 1 haptique par action** ; un poll
qui découvre plusieurs `DONE` ne déclenche qu'**un** SUCCESS. Respecte le réglage système.

### 2.9 Thème racine & CompositionLocals (`ui/theme/CreateTheme.kt`)

```kotlin
val LocalIrisBrushes: ProvidableCompositionLocal<IrisBrushes>   // {fill: Brush, text: Brush}
val LocalHaptics: ProvidableCompositionLocal<Haptics>

@Composable fun CreateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
)   // MaterialExpressiveTheme(colorScheme = createColorScheme(...), typography = CreateTypography,
    // shapes = CreateShapes) + fournit LocalIrisBrushes / LocalHaptics. Edge-to-edge géré par MainActivity.
```

---

## 3. RÉSEAU / DONNÉES — clients, repositories & signatures figées

### 3.1 Constantes & BuildConfig (`core/net/ApiConfig.kt`)

```kotlin
object ApiConfig {
    val APP_BASE = BuildConfig.APP_BASE_URL   // "https://create.vpsdashboard.space/api/"
    val PB_BASE  = BuildConfig.PB_BASE_URL     // "https://pb-create.vpsdashboard.space/"
}
```
URLs injectées en `buildConfigField` (surchargeable pour un backend de test).

### 3.2 Interfaces Retrofit (`core/net/CreateApi.kt`, `core/net/PbAuthApi.kt`)

Endpoints **réutilisés du backend existant** (NATIVE_SPEC §3), figés :
```kotlin
interface CreateApi {                                                   // base APP_BASE, Bearer
    @POST("generate")                suspend fun generate(@Body body: GenerateRequest): GenerateResponse
    @POST("generate")                suspend fun runTool(@Body body: ToolRequest): GenerateResponse
    @GET("generations")              suspend fun list(): GenerationsResponse
    @GET("generations/{id}")         suspend fun poll(@Path("id") id: String): GenerationDto
    @DELETE("generations/{id}")      suspend fun delete(@Path("id") id: String): Response<Unit>
    @POST("generations/{id}/cancel") suspend fun cancel(@Path("id") id: String): Response<Unit>
    @Multipart @POST("upload")       suspend fun upload(@Part file: MultipartBody.Part): UploadResponse
    @GET("credits")                  suspend fun credits(): CreditsResponse
    @POST("transcribe")              suspend fun transcribe(@Body audio: RequestBody): TranscribeResponse
    @POST("push/register-fcm")       suspend fun registerFcm(@Body body: FcmTokenRequest): Response<Unit>  // §M7, ajout serveur
}

interface PbAuthApi {                                                   // base PB_BASE, sans auth préalable
    @POST("api/collections/users/auth-with-password") suspend fun login(@Body body: PbAuthRequest): PbAuthResponse
    @POST("api/collections/users/auth-refresh")       suspend fun refresh(@Header("Authorization") bearer: String): PbAuthResponse
}
```

### 3.3 DTOs requête/réponse (`core/net/dto/`) — noms & champs figés

```kotlin
data class GenerateRequest(                                 // POST /api/generate (génération)
    val model: String,
    val prompt: String,
    val imageUrls: List<String>? = null,
    val options: Map<String, Any?>? = null,
)
data class ToolRequest(val tool: String, val toolImageUrl: String)   // "upscale" | "removeBg" (1-clic)
data class GenerateResponse(val id: String, val taskId: String)
data class UploadResponse(val url: String)                  // URL temporaire kie (~3 j)
data class CreditsResponse(val credits: Int)
data class TranscribeResponse(val transcript: String)
data class FcmTokenRequest(val token: String, val platform: String = "android")

data class PbAuthRequest(val identity: String, val password: String)
data class PbAuthResponse(val token: String, val record: PbUserRecord)
data class PbUserRecord(val id: String, val email: String, val name: String? = null)
```

### 3.4 Client HTTP, auth & erreurs (`core/net/`, `core/auth/`)

```kotlin
class ApiClient(session: SessionManager, tokenStore: TokenStore)   // fournit Retrofit CreateApi + PbAuthApi + OkHttp + ImageLoader Coil
class AuthInterceptor(tokenStore: TokenStore) : Interceptor         // ajoute "Authorization: Bearer <token>"
class TokenAuthenticator(pbAuthApi, tokenStore, session) : Authenticator  // 401 → refresh 1x → replay ; échec → LoggedOut

object TokenStore {                                                 // EncryptedSharedPreferences
    fun saveToken(token: String); fun loadToken(): String?; fun clear()
    fun saveRecord(record: PbUserRecord); fun loadRecord(): PbUserRecord?
}

sealed class ApiError : Exception() {                               // core/net/ApiError.kt
    data object Unauthorized : ApiError()                          // 401
    data class BadRequest(val serverMessage: String) : ApiError()  // 400
    data class Upstream(val detail: String) : ApiError()           // 502 kie
    data class Network(val detail: String) : ApiError()
    data class Decoding(val detail: String) : ApiError()
    val frenchMessage: String get()                                // message FR affichable
}
```
OkHttp : `AuthInterceptor` + `TokenAuthenticator` + `HttpLoggingInterceptor` (debug), timeouts
connect 15 s / read 60 s. Converters : **Moshi** (`KotlinJsonAdapterFactory`), tolérant ; `options`
sérialisé tel quel. `transcribe` : `RequestBody` avec `Content-Type` = mime de l'audio. Coil `ImageLoader`
partagé **sans** header Bearer (fichiers PB publics par token de fichier).

### 3.5 Session (`core/auth/SessionManager.kt`)

```kotlin
class SessionManager(pbAuthApi: PbAuthApi, tokenStore: TokenStore) {
    val authState: StateFlow<AuthState>                 // Unknown → LoggedIn/LoggedOut
    suspend fun bootstrap()                             // lit TokenStore + refresh proactif au démarrage
    suspend fun login(identity: String, password: String)   // PbAuthApi.login → TokenStore
    suspend fun refresh(): Boolean                     // PbAuthApi.refresh(Bearer)
    fun logout()                                       // TokenStore.clear() + authState = LoggedOut
    fun currentToken(): String?                        // lu par AuthInterceptor
}
```

### 3.6 Repositories (`data/`) — source de vérité partagée

```kotlin
class GenerationRepository(api: CreateApi) {           // cache mémoire partagé feed ↔ galerie
    val generations: StateFlow<List<Generation>>       // tri -created ; source unique des 2 écrans
    val pendingCount: StateFlow<Int>
    suspend fun refresh()                              // GET /api/generations → map → cache
    suspend fun generate(req: GenerateRequest): Generation   // optimistic PENDING local puis réconcilie par id
    suspend fun runTool(req: ToolRequest): Generation
    suspend fun pollPending()                          // poll(id) sur chaque PENDING ; transitions → haptique
    suspend fun cancel(id: String)                     // POST …/cancel
    suspend fun delete(id: String)                     // DELETE …
    fun feed(limit: Int = 12): List<Generation>        // 12 derniers pour le feed inversé
}

class CreditsRepository(api: CreateApi) {
    val credits: StateFlow<Int?>
    suspend fun refresh()                              // GET /api/credits (auto 45 s + ON_RESUME)
}

class UploadRepository(api: CreateApi, context: Context) {
    suspend fun upload(uri: Uri): String               // MultipartBody.Part → /api/upload (max 10 Mo) → url
}

class SettingsStore(context: Context) {                // DataStore Preferences (data/prefs/)
    val familyKey: Flow<String>; val variantKey: Flow<String?>
    val paramValues: Flow<Map<String, String>>
    val themeMode: Flow<ThemeMode>; val dynamicColor: Flow<Boolean>
    suspend fun setLastModel(familyKey: String, variantKey: String?, paramValues: Map<String, String>)
    suspend fun setThemeMode(mode: ThemeMode); suspend fun setDynamicColor(enabled: Boolean)
}
enum class ThemeMode { SYSTEM, LIGHT, DARK }           // data/prefs/ThemeMode.kt
```

### 3.7 Polling (règles figées, PLAN §4.5)

- **Foreground** : `GenerationRepository.pollPending()` bouclé par le ViewModel actif (`viewModelScope`,
  `delay(4_000)`), uniquement s'il reste des `PENDING`, sous `repeatOnLifecycle(STARTED)`. Transition
  `PENDING→DONE` = 1 haptique SUCCESS (une seule fois), `→FAILED/CANCELLED` = ERROR.
- **Background** : **FCM** (`CreateFcmService`) — jamais un service de poll en arrière-plan.

### 3.8 Services natifs (`core/media/`, `core/push/`) — noms figés

```kotlin
class AudioRecorder(context: Context)   // MediaRecorder AAC/m4a → File/ByteArray ; auto-stop 60 s ; ignore < 1.2 ko
object MediaSaver                        // MediaStore : saveImage(url) / saveVideo(url) (?download=1)
object FileShare                         // FileProvider + Intent.ACTION_SEND
object PbFiles { fun thumb(url, spec); fun download(url) }   // construit ?thumb=…/?download=1 (core/util)
class CreateFcmService : FirebaseMessagingService   // onNewToken→registerFcm ; onMessageReceived→NotificationHelper
object NotificationHelper                // canal "generations" (HIGH), accent iris, deep-link create://gallery
```

---

## 4. STRUCTURE APP — Application, Activity, navigation, ViewModels (figées)

### 4.1 Entrée (`CreateApp.kt`, `MainActivity.kt`, `core/di/AppContainer.kt`)

```kotlin
class CreateApp : Application {                     // init AppContainer, Coil, canaux notif, WorkManager
    val container: AppContainer
}
class AppContainer(app: Application) {              // DI manuel — singletons partagés
    val session: SessionManager; val generations: GenerationRepository
    val credits: CreditsRepository; val uploads: UploadRepository
    val settings: SettingsStore; val api: CreateApi; val haptics: Haptics
}
class MainActivity : ComponentActivity {           // enableEdgeToEdge() ; setContent { CreateTheme { CreateRoot() } }
```

### 4.2 Navigation Compose (`ui/nav/CreateRoot.kt`, `ui/nav/Routes.kt`)

Auth-gate racine (`AuthState`) puis bottom-nav 2 onglets ; lightbox = destination overlay.
```kotlin
object Routes {
    const val LOGIN = "login"
    const val CREATE = "create"
    const val GALLERY = "gallery"
    const val SETTINGS = "settings"
    const val LIGHTBOX = "lightbox/{generationId}"   // fun lightbox(id): String
}
@Composable fun CreateRoot()   // Scaffold + AuroraBackground + CreateBottomBar + NavHost ;
                               // AuthState.LoggedIn → onglets, sinon LoginScreen. SharedTransitionLayout englobant.
enum class TopTab { CREATE, GALLERY }
```
Sheets présentées par état local d'écran (pas de router global) : `ModelSheet` / `SettingsSheet` via
`ModalBottomSheet` pilotés par le `CreateViewModel`.

### 4.3 ViewModels (`ui/<screen>/`) — 1 par écran, `StateFlow<UiState>` figés

```kotlin
class LoginViewModel(session: SessionManager) : ViewModel {
    val state: StateFlow<LoginUiState>              // {identity, password, submitting, error?}
    fun login()
}

class CreateViewModel(
    generations: GenerationRepository, credits: CreditsRepository,
    uploads: UploadRepository, settings: SettingsStore, api: CreateApi,
) : ViewModel {
    val composer: StateFlow<ComposerState>
    val feed: StateFlow<List<Generation>>           // dérivé du repo
    val events: SharedFlow<UiEvent>
    fun onPromptChange(text: String); fun addRef(uri: Uri); fun removeRef(uri: Uri)
    fun pickModel(family: ModelFamily); fun pickVariant(variant: ModelVariant?)
    fun setParam(field: String, value: String); fun setMode(mode: ModelKind)
    fun send()                                      // resolveModelId + buildOptions + optimistic + poll
    fun cancel(id: String); fun reuse(prompt: String)
    fun startRecording(); fun stopRecordingAndTranscribe()
}

class GalleryViewModel(generations: GenerationRepository) : ViewModel {
    val state: StateFlow<GalleryUiState>            // {items, pendingCount, refreshing}
    fun refresh(); fun startPolling(); fun stopPolling()
}

class LightboxViewModel(generationId: String, generations: GenerationRepository, api: CreateApi) : ViewModel {
    val state: StateFlow<LightboxUiState>
    fun copyPrompt(); fun share(); fun save(); fun upscale(); fun removeBg(); fun delete()
}

class SettingsViewModel(settings: SettingsStore, session: SessionManager) : ViewModel {
    val state: StateFlow<SettingsUiState>           // {themeMode, dynamicColor}
    fun setThemeMode(mode: ThemeMode); fun setDynamicColor(enabled: Boolean); fun logout()
}

sealed interface UiEvent {                          // one-shot (snackbar/nav/copié)
    data class Snackbar(val message: String) : UiEvent
    data class Error(val message: String) : UiEvent
    data class Navigate(val route: String) : UiEvent
}
```
Câblage ViewModels ↔ repositories via `CreateActivityGraph.kt` (`ViewModelProvider.Factory` lisant
`AppContainer`). `SavedStateHandle` conserve le brouillon de prompt + l'onglet courant (process death).

### 4.4 Composables écrans & communs (noms figés, DESIGN §5)

```kotlin
// ui/login/       LoginScreen
// ui/create/      CreateScreen, Feed, FeedCard (Pending/Done/Failed/Cancelled), EmptyState, Composer,
//                 RefsRow, PromptField, ActionRow, GenerateButton, MicButton, ModelSheet, SettingsSheet
// ui/gallery/     GalleryScreen, GalleryCard
// ui/lightbox/    LightboxScreen
// ui/settings/    SettingsScreen
// ui/common/      TopBar, CreateBottomBar, CreditsChip, NotifBell, ShimmerBox, RatioIcon, IrisButton
```
Contrats visuels figés dans DESIGN §5 : `FeedCard` = `cardSolid` 72 % aligné à droite (queue de bulle,
miroir RTL) ; `Composer` = `glassStrong` 28 dp `imePadding()` ; `GenerateButton` = `FilledIconButton`
rond 48 dp peint `IrisBrush` ; bottom sheets = `glassStrong` 34 dp ; `SettingsSheet` **générée** depuis
`CatalogLogic.paramsFor` ; lightbox = `Dialog` plein écran + swipe-down + ExoPlayer/`AsyncImage`.

---

## 5. RÈGLES TRANSVERSES (non négociables)

1. **Light mode par défaut** (suit le système, sinon light). Dark theme complet fourni (`CreateDarkScheme`
   + aurora sombre), jamais un bricolage. Pas de code qui force le dark.
2. **Iris = signature immuable** : bouton Générer, chip actif, wavy progress, halo micro, wordmark hero,
   point du wordmark utilisent `IrisBrush`/`Accent` **en dur** — jamais remplacés par dynamic color.
   Iris réservé aux éléments actifs/d'action, jamais une grande surface.
3. **Verre** uniquement via `glassSurface` / `glassStrong` / `cardSolid`. Cartes de scroll = `cardSolid`
   (jamais de blur). Blur = optionnel, API 31+, coupé si transparence réduite ou < API 31.
4. **Motion** via `object Motion` (springs Expressive) + `rememberReduceMotion()`. **Aurora statique**.
5. **Haptique** via `Haptics.fire(_)` / `LocalHaptics` uniquement, **1 par action** utilisateur.
6. **Réseau** via `CreateApi` (routes user, Bearer) + `PbAuthApi` (login/refresh) ; jamais d'URL en dur
   hors `ApiConfig`/`BuildConfig`. Token en `EncryptedSharedPreferences` (`TokenStore`), jamais en clair.
7. **Catalogue** = `ModelCatalog` verbatim NATIVE_SPEC §5 ; toute `SettingsSheet` se génère depuis
   `ParamSpec` (aucun réglage codé en dur par famille).
8. **Dates** = `java.time.Instant` ISO-8601 ; crédits affichés avec chiffres monospacés (roll-up spring).
9. **minSdk 26** : coder défensivement les API 31+ (`blurEnabled`, dynamic color, `VibratorManager`) et
   33+ (`POST_NOTIFICATIONS`) — voir PLAN §1. Aucune brique cœur ne bloque en 26.
10. **A11y & RTL** de première classe (DESIGN §8) : `contentDescription` sur toutes les icônes, cibles
    ≥ 48 dp, `start/end` (jamais `left/right`), bulles de feed miroitées en RTL, info jamais par la seule
    couleur (failed = icône + texte, cancelled = libellé).

---

## 6. Dépendances serveur (rappel, hors app Android)

Stratégie **A** : le natif tape `/api/*` existants avec Bearer PocketBase. **Un** patch serveur requis
avant M1 : `getUser()` (`/root/apps/create/src/lib/pocketbase/server.ts`) doit accepter
`Authorization: Bearer <token>` en plus du cookie `pb_auth`. Push **FCM** (`POST /api/push/register-fcm`
+ collection `fcm_tokens` + envoi FCM v1 dans `refreshGeneration`/`complete.ts`) = jalon M7, non bloquant
pour un premier build interne Play. CI : adapter `/root/.appstore/android-template/.github/workflows/
android-release.yml` (natif Compose, `ANDROID_DIR=.`, `PACKAGE_NAME=space.vpsdashboard.create`,
`./gradlew bundleRelease`) ; keystore/secrets déjà provisionnés dans `/root/.appstore`.

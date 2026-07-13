# Create — Direction Artistique NATIVE Android

**Material 3 Expressive · Jetpack Compose**

Ce document est la source de vérité *design* pour l'app Android native **Create**. Il traduit
l'identité web « iris / aurora / liquid glass » (voir `NATIVE_SPEC.md` §8 et
`/root/apps/create/src/app/globals.css`) dans le langage **Material 3 Expressive** — sans imiter le
Liquid Glass iOS. Principe directeur : **on ne clone pas l'iOS, on réécrit l'âme de Create en
Material**. L'accent **iris** (violet→bleu→cyan) reste la signature immuable de la marque, quel que
soit le dynamic color.

> Stack cible : Jetpack Compose · `androidx.compose.material3` (M3 Expressive, 1.4+) ·
> `material3-adaptive` · Material Motion (`androidx.compose.animation`) · dynamic color **optionnel**.
> Cible : Android 12+ (API 31) pour Material You / blur `RenderEffect` ; dégradation propre en 8–11.

---

## 0. Philosophie & garde-fous

1. **Iris = signature, non négociable.** Dynamic color (Material You) est *proposé* comme option
   utilisateur, mais **la couleur d'accent primaire reste toujours l'iris** dans son expression
   principale (bouton Générer, chips actifs, barres de progression, halo). Même quand l'utilisateur
   active « couleurs du fond d'écran », l'iris survit en **accent secondaire signature** (voir §2.4).
2. **Formes expressives.** M3 Expressive apporte des formes organiques, des transitions par ressort
   (spring) et une hiérarchie de formes plus grasse. On l'exploite : coins très arrondis, morphing
   de formes sur l'interaction, `MaterialShapes` pour les accents (cookie/pill).
3. **Pas de faux Liquid Glass.** Interdiction de reproduire le verre irisé bord-lumière iOS. À la
   place : **surfaces Material tonales** (elevation par teinte) + **blur léger optionnel**
   (`Modifier.blur` / `RenderEffect`, ≤ 16 dp) sur les couches flottantes uniquement. Le « verre »
   devient une **surface translucide tonale** honnête et Material.
4. **Light mode par défaut, dark mode fourni.** Le web est light-only ; sur Android on **fournit
   quand même un dark theme complet** (attendu de la plateforme, économie batterie OLED, respect du
   réglage système) — mais le **défaut au premier lancement suit le système**, et si non déterminé,
   **light**. Voir §7.
5. **Accessibilité & RTL** de première classe (§8).

---

## 1. Typographie

Create repose sur un contraste **serif display / sans-serif UI**. On le porte fidèlement.

### 1.1 Familles

| Rôle | Police | Source | Usage |
|---|---|---|---|
| **Display / Serif** | **Instrument Serif** (400 regular + italic) | `res/font/instrument_serif_regular.ttf`, `..._italic.ttf` | Wordmark « Create. », titres hero, états vides |
| **Body / UI** | **Figtree** (400/500/600/700) | `res/font/figtree_*.ttf` | Tout le reste : labels, boutons, corps, chips, nav |

Déclaration Compose :

```kotlin
val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)
val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)
```

### 1.2 Échelle typographique (M3 `Typography`, override sélectif)

On mappe les rôles Material vers nos deux familles. **Les `display*` et `headline*` passent en
Instrument Serif** ; tout le reste en Figtree.

| Rôle M3 | Police | Size / Line / Weight / Tracking | Emploi concret |
|---|---|---|---|
| `displayLarge` | Instrument Serif | 44 / 48 / 400 / -0.5 sp | Hero état vide « Qu'est-ce qu'on *crée* ? » |
| `displayMedium` | Instrument Serif | 34 / 40 / 400 / -0.25 sp | Titres de sheet larges |
| `displaySmall` | Instrument Serif | 28 / 34 / 400 / 0 | Wordmark « Create. » header |
| `headlineMedium` | Instrument Serif | 24 / 30 / 400 / 0 | Titres de section, login |
| `titleLarge` | Figtree | 20 / 26 / 600 / 0 | Titres de bottom sheet |
| `titleMedium` | Figtree | 16 / 22 / 600 / 0.1 sp | Nom de famille modèle, entête carte |
| `titleSmall` | Figtree | 14 / 20 / 600 / 0.1 sp | Labels de boutons composer |
| `bodyLarge` | Figtree | 16 / 24 / 400 / 0.15 sp | Zone de prompt, corps |
| `bodyMedium` | Figtree | 14 / 20 / 400 / 0.15 sp | Prompt de carte, taglines |
| `bodySmall` | Figtree | 12 / 16 / 400 / 0.2 sp | timeAgo, crédits, meta |
| `labelLarge` | Figtree | 14 / 20 / 600 / 0.1 sp | Boutons filled/tonal |
| `labelMedium` | Figtree | 12 / 16 / 600 / 0.3 sp | Chips, badges |
| `labelSmall` | Figtree | 11 / 16 / 700 / 0.5 sp | Badge « ÉDITION », « IMAGE → VIDÉO » (uppercase) |

**Wordmark** « Create. » : `displaySmall` Instrument Serif, le **point final** peint en
`accent` (bleu `#2f6df6` / iris) — reproduit la ponctuation colorée du web. Le mot hero *crée* en
**italique + dégradé iris** via `TextStyle(brush = irisBrush)` (Compose supporte `brush` sur le
texte).

---

## 2. Couleur — tokens & color scheme

### 2.1 Constantes de marque (jamais dérivées du dynamic color)

```kotlin
// Iris — dégradé signature (violet → bleu → cyan)
val IrisViolet = Color(0xFF8B5CF6)
val IrisBlue   = Color(0xFF3B82F6)
val IrisCyan   = Color(0xFF22D3EE)
// Variante « texte » (background-clip web)
val IrisTextViolet = Color(0xFF7C3AED)
val IrisTextBlue   = Color(0xFF2563EB)
val IrisTextCyan   = Color(0xFF06B6D4)

// Encre & accent ponctuel
val Ink      = Color(0xFF2A3142) // texte principal (light)
val InkSoft  = Color(0xFF5D6478) // texte secondaire (light)
val Accent   = Color(0xFF2F6DF6) // bleu accent (point du wordmark)
```

**Brushes réutilisables** (à exposer via un `CompositionLocal` `LocalIrisBrushes`) :

```kotlin
val IrisBrush = Brush.linearGradient(       // 135° : haut-gauche → bas-droite
    0.0f to IrisViolet, 0.55f to IrisBlue, 1.0f to IrisCyan)
val IrisTextBrush = Brush.linearGradient(
    0.0f to IrisTextViolet, 0.55f to IrisTextBlue, 1.0f to IrisTextCyan)
```

> Astuce dégradé 135° : utiliser `Brush.linearGradient(start = Offset(0f,0f),
> end = Offset(x,y))` avec l'angle mesuré sur la bounding box du composant (recalcul via
> `onGloballyPositioned` ou `drawWithCache`).

### 2.2 ColorScheme LIGHT (défaut, dérivé de l'iris — palette tonale iris)

Généré à partir des teintes iris (seed **`#3B82F6`** iris-blue comme couleur source, harmonisé
violet/cyan). Valeurs cible :

| Token M3 | Hex | Emploi |
|---|---|---|
| `primary` | `#3B82F6` | Accent iris principal (fallback aplat quand pas de brush) |
| `onPrimary` | `#FFFFFF` | Texte sur bouton Générer |
| `primaryContainer` | `#DCE6FF` | Chip actif fond doux, container tonal |
| `onPrimaryContainer` | `#0B2A6B` | Texte sur container primaire |
| `secondary` | `#8B5CF6` | Violet iris — accents secondaires |
| `onSecondary` | `#FFFFFF` | |
| `secondaryContainer` | `#EBE2FF` | Sélections violettes douces |
| `onSecondaryContainer` | `#2C1466` | |
| `tertiary` | `#06B6D4` | Cyan iris — 3e accent (badges, i2v) |
| `onTertiary` | `#FFFFFF` | |
| `tertiaryContainer` | `#C9F3FB` | |
| `onTertiaryContainer` | `#043F49` | |
| `background` | `#EEF0FD` | Base aurora (thème/status bar web = `#eef0fd`) |
| `onBackground` | `#2A3142` | = Ink |
| `surface` | `#F4F5FE` | Surface neutre légèrement bleutée |
| `onSurface` | `#2A3142` | = Ink |
| `surfaceVariant` | `#E2E5F3` | Séparateurs, champs |
| `onSurfaceVariant` | `#5D6478` | = InkSoft |
| `surfaceContainerLowest` | `#FFFFFF` | Cartes solides (`card-solid`) |
| `surfaceContainerLow` | `#F7F8FF` | |
| `surfaceContainer` | `#F0F2FD` | Sheets, barres |
| `surfaceContainerHigh` | `#E9ECFB` | Modales |
| `surfaceContainerHighest`| `#E2E6F8` | Élévation max |
| `outline` | `#AEB4CB` | Bordures |
| `outlineVariant` | `#D3D8EC` | Bordures douces, dividers |
| `error` | `#DC2626` | Cartes d'échec, delete |
| `onError` | `#FFFFFF` | |
| `errorContainer` | `#FEE2E2` | Fond carte failed |
| `onErrorContainer` | `#7F1D1D` | |
| `scrim` | `#1A1F2E` @ 40% | Overlay lightbox / sheets |

Les **surfaces translucides « verre »** ne sont pas des tokens M3 mais des overlays (voir §3).

### 2.3 ColorScheme DARK (fourni, non-défaut sauf système)

Aurora sombre : bleus-violets profonds, iris relevé en luminosité pour tenir le contraste.

| Token M3 | Hex |
|---|---|
| `primary` | `#9EC0FF` |
| `onPrimary` | `#0A2A5C` |
| `primaryContainer` | `#274A86` |
| `onPrimaryContainer` | `#D8E6FF` |
| `secondary` | `#C4B0FF` |
| `secondaryContainer` | `#3E2C79` |
| `tertiary` | `#6FE0F2` |
| `tertiaryContainer` | `#0C4E5A` |
| `background` | `#0E1220` |
| `onBackground` | `#E3E6F4` |
| `surface` | `#121728` |
| `onSurface` | `#E3E6F4` |
| `surfaceVariant` | `#3A3F52` |
| `onSurfaceVariant` | `#C3C7DA` |
| `surfaceContainerLowest` | `#0B0F1B` |
| `surfaceContainer` | `#181D30` |
| `surfaceContainerHigh` | `#222840` |
| `outline` | `#8B90A6` |
| `outlineVariant` | `#3A3F52` |
| `error` | `#FF6B6B` |
| `errorContainer` | `#5C1A1A` |
| `scrim` | `#000000` @ 55% |

Le **brush iris** reste identique en dark (violet/bleu/cyan saturés lisent bien sur fond sombre),
éventuellement +6 % de luminosité globale via `IrisBrushDark`.

### 2.4 Dynamic color — règle de cohabitation

```kotlin
val useDynamic = userPref.dynamicColor && Build.VERSION.SDK_INT >= 31
val base = when {
    useDynamic && dark -> dynamicDarkColorScheme(ctx)
    useDynamic -> dynamicLightColorScheme(ctx)
    dark -> CreateDarkScheme
    else -> CreateLightScheme
}
// L'iris survit TOUJOURS : on ré-imprime la signature même en dynamic.
val scheme = base.copy(
    // Dynamic peut piloter neutres/surfaces, mais l'accent produit reste iris :
    primary = if (useDynamic) base.primary else IrisBlue,
)
```

Même sous dynamic color, **le brush iris n'est jamais remplacé** : bouton Générer, halo micro,
`progressLine`, dégradé du wordmark hero utilisent `IrisBrush` en dur. Dynamic color ne colore que
les **surfaces neutres et l'état de sélection ambiant**. → l'app reste reconnaissable « Create » sur
n'importe quel fond d'écran.

### 2.5 Fond Aurora (Compose, statique, zéro animation)

Reproduire le multi-radial du web, dessiné une fois en fond de `Scaffold` (derrière tout,
`Modifier.drawBehind`), **sans animation** (perf + batterie) :

```kotlin
fun DrawScope.auroraLight() {
    drawRect(Brush.linearGradient(          // base 165°
        0.0f to Color(0xFFEEF0FD), 0.45f to Color(0xFFEAF2FC), 1.0f to Color(0xFFEAFAF9)))
    drawRect(Brush.radialGradient(listOf(Color(0x66A78BFA), Color.Transparent),
        center = Offset(0.12f*w, 0.08f*h), radius = 0.60f*maxDim))  // violet
    drawRect(Brush.radialGradient(listOf(Color(0x5960A5FA), Color.Transparent),
        center = Offset(0.88f*w, 0.92f*h), radius = 0.55f*maxDim))  // bleu
    drawRect(Brush.radialGradient(listOf(Color(0x4767E8F9), Color.Transparent),
        center = Offset(0.55f*w, 0.40f*h), radius = 0.45f*maxDim))  // cyan
    drawRect(Brush.radialGradient(listOf(Color(0x4D93C5FD), Color.Transparent),
        center = Offset(0.85f*w, 0.10f*h), radius = 0.40f*maxDim))  // bleu clair
}
```

+ **couche de grain** : PNG tuilé 128×128 de bruit, `alpha = 0.04f`, `BlendMode.Overlay`
(équivalent web du SVG noise). En dark, aurora = radiaux sombres (`#0E1220` base + halos iris à
~20 % d'opacité).

---

## 3. « Verre » Material — surfaces translucides tonales

On **abandonne** le liquid-glass irisé au profit d'un système de **3 niveaux de surface
translucide**, honnêtement Material (teinte + optionnellement blur léger).

| Niveau (web → android) | Fond | Blur | Bordure | Élévation / ombre | Usage |
|---|---|---|---|---|---|
| **`glassSurface`** (`.glass`) | `surface` @ **60 %** | `blur(10.dp)` opt. | `outlineVariant` @ 50 %, 1 dp | `shadowElevation = 8.dp`, `tonalElevation = 2.dp` | Chips, petits panneaux flottants |
| **`glassStrong`** (`.glass-strong`) | `surfaceContainer` @ **80 %** | `blur(14.dp)` opt. | `outlineVariant`, 1 dp | `shadowElevation = 12.dp`, `tonalElevation = 3.dp` | Bottom sheets, tab bar, top bar, modales |
| **`cardSolid`** (`.card-solid`) | `surfaceContainerLowest` (opaque) | **aucun** | `outlineVariant` @ 60 %, 1 dp | `shadowElevation = 3.dp` | Cartes de feed & galerie (fluide au scroll, **jamais de blur**) |

Règles :
- **Blur = optionnel et léger** (`Modifier.blur` via `RenderEffect`, API 31+ ; **désactivé** < API 31
  et si `LocalAccessibility` réduit les effets). La lisibilité ne dépend jamais du blur : le fond
  translucide + le contraste texte suffisent seuls.
- **Inset highlight** blanc (le web le fait) : bord supérieur d'1 dp en `Color.White @ 0.5f` sur les
  surfaces `glass*` (dégradé vertical qui s'estompe) — subtil, pas le liseré irisé iOS.
- Les **listes scrollables** (feed, grille) utilisent **`cardSolid` sans blur** : blur pendant le
  scroll = coûteux et scintille. Le blur ne vit que sur les **couches fixes flottantes**.

```kotlin
fun Modifier.glassStrong(shape: Shape) = this
    .clip(shape)
    .then(if (blurEnabled) Modifier.blur(14.dp, BlurredEdgeTreatment(shape)) else Modifier)
    .background(scheme.surfaceContainer.copy(alpha = 0.80f), shape)
    .border(1.dp, scheme.outlineVariant.copy(alpha = 0.6f), shape)
```

---

## 4. Formes (M3 Expressive `Shapes`)

Le web pousse des coins **très** arrondis. On adopte l'échelle Expressive, gonflée.

| Token M3 `Shapes` | Rayon | Emploi |
|---|---|---|
| `extraSmall` | 8 dp | Badges, mini-chips |
| `small` | 14 dp | Champs, petites cartes |
| `medium` | 22 dp | Cartes feed/galerie, boutons tonaux larges |
| `large` | 28 dp | Composer, cartes login |
| `extraLarge` | 34 dp | Bottom sheets, modales |

En plus :
- **Bulles de feed** : coin custom `RoundedCornerShape(28.dp)` avec le **coin bas-droit resserré à
  10 dp** (queue de bulle « sortante », alignée à droite). En **RTL**, miroir : coin bas-**gauche**
  resserré, alignement à gauche.
- **Boutons ronds** (Générer, micro, photo, fermer) : `CircleShape`, 44 dp (min touch 48 dp via
  padding).
- **Chips** : `CircleShape` (full-round / pill), hauteur 34 dp.
- **Formes expressives ponctuelles** : `MaterialShapes.Cookie12Sided` ou `Pill` pour le badge de
  crédits animé et l'avatar d'état vide — accent « expressive » réservé, pas partout.
- **Morphing** : sur press des boutons ronds, léger morph de `CircleShape` → `RoundedCornerShape`
  via `androidx.graphics.shapes` (`Morph`) synchronisé au scale press (§6).

---

## 5. Composants clés — specs Material

### 5.1 Top App Bar (les 2 écrans)

- `CenterAlignedTopAppBar`-like custom, posé sur **`glassStrong`** (translucide, épingle en haut,
  `windowInsets = statusBars`).
- **Gauche** : wordmark « Create. » (`displaySmall` Instrument Serif, point = `Accent`).
- **Droite** : `IconButton` **cloche notifications** (badge `Badge` iris si non lu) + **chip
  crédits** (§5.9).
- Hauteur 56 dp + status bar inset. Ombre douce apparaît au scroll (`TopAppBarScrollBehavior`
  `enterAlways`).

### 5.2 Navigation bas

- **M3 Expressive `FlexibleBottomAppBar` / `NavigationBar`** flottante : conteneur `glassStrong`,
  `RoundedCornerShape(26.dp)`, **détachée des bords** (marge 12 dp), au-dessus du
  `navigationBars` inset.
- 2 destinations : **Créer** (icône `AutoAwesome` / étincelle) · **Galerie** (icône `Image`).
- Indicateur actif : **pill `primaryContainer`** derrière l'item + label iris ; transition
  d'indicateur en **spring** (Expressive). Icône active peut recevoir un fin **tint iris** via brush.
- Haptique `select` (`HapticFeedbackType.SegmentTick`) au changement d'onglet.

### 5.3 Écran Créer — Feed (liste inversée)

- `LazyColumn(reverseLayout = true)` collée au composer ; charge 12 dernières générations.
- **`FeedCard`** = `cardSolid`, largeur ~72 % (`fillMaxWidth(0.72f)`), **aligné à droite**
  (`Alignment.End`), coin-queue bas-droit (§4). RTL → aligné à gauche, miroir.
  - **pending** : `shimmer` (dégradé balayant, §6) + `CircularWavyProgressIndicator` (Expressive) +
    prompt tronqué 2 lignes + **`LinearWavyProgressIndicator` indéterminé teinté iris** + `IconButton`
    ✕ (annuler → `POST …/cancel`, haptique `error`).
  - **done** : miniature (Coil `AsyncImage` `?thumb=600x0`, `RoundedCornerShape(20.dp)`) ou
    `VideoThumbnail` muet ; prompt · timeAgo · crédits en `bodySmall onSurfaceVariant`. Tap → Lightbox
    (shared-element transition, §6). Haptique `tap`.
  - **failed** : carte `errorContainer`, texte « Échec — {error} » `onErrorContainer` ; tap → réinjecte
    le prompt dans le composer (haptique `select`).
  - **cancelled** : carte grisée (`surfaceVariant`, contenu `onSurfaceVariant` @ 70 %) « Annulée ».
- **État vide** : hero centré, `displayLarge` Instrument Serif « Qu'est-ce qu'on *crée*
  aujourd'hui ? » (*crée* en italique brush iris) + 3 **`SuggestionChip`** empilés (les 3 prompts du
  spec), tap = pré-remplit le prompt.
- Entrée des cartes : animation **`rise`** (`slideInVertically` + `fadeIn`, spring).

### 5.4 Écran Créer — Composer (barre fixe basse)

Panneau **`glassStrong`**, `RoundedCornerShape(28.dp)`, marge 12 dp, au-dessus du clavier
(`imePadding()`) et du `navigationBars`.

1. **Rangée refs** (si images) : `LazyRow` de vignettes 56×56 dp (`RoundedCornerShape(14.dp)`) avec
   `IconButton` ✕ superposé (coin haut-droit) ; **badge** `AssistChip`/pill `labelSmall` uppercase
   « ÉDITION » (mode image) ou « IMAGE → VIDÉO » (mode vidéo), fond `tertiaryContainer`.
2. **Champ prompt** : `BasicTextField` / `OutlinedTextField` sans bordure, `bodyLarge`, **auto-grow**
   jusqu'à ~150 dp puis scroll interne. Placeholder contextuel (« Décris ton image… » / « …ta
   vidéo… » / « …la retouche à faire… »). Action clavier = **envoyer**.
3. **Rangée d'actions** (`Row`, `spacedBy(8.dp)`) :
   - **Photo** `IconButton` (`AddPhotoAlternate`) → `PhotoPicker` ; désactivé si
     `refs.size >= family.maxImages`. Upload → `POST /api/upload`.
   - **Micro** `IconButton` (`Mic`) → dictée (§5.7).
   - **Modèle** `AssistChip` : « {nom} {variante?} · {credits}cr ▾ », ouvre la sheet Modèle.
   - **Réglages** `AssistChip` : résumé (« 16:9 · 8s ▾ »), ouvre la sheet Réglages.
   - **Spacer(weight 1f)**, puis **Bouton Générer** : `FilledIconButton` **rond 48 dp**, fond =
     **`IrisBrush`** (peint via `Modifier.background(brush)`), icône ↑ (`ArrowUpward`) blanche.
     Désactivé (opacité 0.4, brush grisé) si prompt vide / génération en cours / upload en cours.
     Tap actif → haptique **`launch`** + micro-morph du bouton (§6).

### 5.5 Bottom Sheet Modèle

- `ModalBottomSheet` M3, conteneur **`glassStrong`**, `extraLarge` (34 dp top corners), drag handle.
- **Segmented button** en haut : `SingleChoiceSegmentedButtonRow` **Image / Vidéo** (change le mode,
  re-clamp refs à `maxImages`). Sélection = `secondaryContainer` + haptique `select`.
- Liste des familles du mode : `ListItem`-like → nom (`titleMedium`) + tagline (`bodyMedium
  onSurfaceVariant`) + coût crédits (pill `tertiaryContainer`). **Famille active** = **contour iris
  2 dp** (`border(2.dp, IrisBrush-as-Color fallback ou drawBehind brush)`) + coche.
- Tap famille → ferme, clamp refs, haptique `select`.

### 5.6 Bottom Sheet Réglages (générée dynamiquement)

Depuis le catalogue `ModelFamily` / `ParamSpec` (spec §5). `ModalBottomSheet` `glassStrong`.
- Si `variants` (Veo) : rangée **« Qualité »** de `FilterChip` (Fast / Quality).
- Une **rangée par param visible** (`paramsFor(family, editing)`) : titre (`titleSmall`) + `FlowRow`
  de `FilterChip` pour chaque `value`.
  - **booléens** → deux chips via `boolLabels` (ex. « Avec son » / « Sans son »).
  - param « Durée » → suffixe `s` (« 8s »).
  - param « Format » (`aspect_ratio`) → chip avec **mini-icône ratio** dessinée (`Canvas` : rectangle
    proportionnel `outline`) + label.
- Chip sélectionné = `primaryContainer` + bord iris ; haptique `select` au tap.
- Aucun param → texte centré « Ce modèle n'a pas de réglages. » (`bodyMedium onSurfaceVariant`).

### 5.7 Dictée vocale (Micro)

- `MediaRecorder` (AAC/m4a). Tap start / tap stop, auto-stop 60 s. Ignorer si < ~1.2 ko.
- Pendant l'enregistrement : **halo rouge pulsant** autour du bouton — `Canvas` cercle `error` avec
  `alpha`/`radius` animés en **boucle infinie douce** (`rememberInfiniteTransition`, 900 ms, easing
  standard) — c'est **la seule animation en boucle tolérée** de l'app (état actif explicite).
- À l'arrêt : `POST /api/transcribe` (Content-Type = mime), transcript ajouté au prompt. Haptique
  `success` au retour.

### 5.8 Galerie

- `LazyVerticalGrid(GridCells.Fixed(2))`, gap 12 dp, `contentPadding` safe-areas. Toutes les
  générations, tri -created.
- **Bandeau** haut « N génération(s) en cours » + `LinearWavyProgressIndicator` iris si des pending.
- **Pull-to-refresh** : `PullToRefreshBox` M3, indicateur teinté iris. Poll 4 s identique au feed.
- **Carte carrée** (`cardSolid`, `medium` 22 dp) : miniature `?thumb=600x600` (ou vidéo) + prompt
  1 ligne + modèle + timeAgo. pending = shimmer + ✕ ; failed = message rouge ; cancelled = grisé.
- Tap `done` → Lightbox (shared element).

### 5.9 Chip crédits (compteur animé)

- `AssistChip` / pill `glassSurface`, icône `Bolt`/étincelle iris + valeur.
- **Animation du nombre** : `animateIntAsState` avec **spring Expressive** (roll-up des chiffres)
  quand les crédits changent. Rafraîchi `GET /api/credits` toutes les ~45 s + au retour foreground.
- Forme accent possible : `MaterialShapes.Pill`.

### 5.10 Lightbox / Visionneuse

- `Dialog` plein écran (`usePlatformDefaultWidth = false`) + **scrim flouté** (`scrim` @ 40 % +
  blur léger opt.).
- Média centré : `AsyncImage` `ContentScale.Fit` (zoom/pan via `transformable`) ou `ExoPlayer`
  (`PlayerView`, controls + autoplay).
- **Swipe-down pour fermer** : `Modifier.draggable` vertical, seuil > 110 dp → dismiss ; le média
  suit le doigt (translation + scale-down + fade du scrim). Shared-element retour vers la carte.
- **Barre haut** : `IconButton` fermer + « {modèle} · {timeAgo} · {crédits}cr » (`bodySmall`).
- **Panneau actions bas** (`glassStrong`, `extraLarge`) :
  - Prompt (tap = copier, haptique `tap`, `Snackbar` « Copié »).
  - Grille d'actions (`FilledTonalButton` / `IconButton` labellisés) : **Partager**
    (`Intent.ACTION_SEND` + `FileProvider`) ; images → **Upscale** & **Détourer**
    (`POST /api/generate {tool,toolImageUrl}`) ; **Sauver** (MediaStore, `?download=1`).
  - **Supprimer** : `TextButton` `error`, **double-tap de confirmation** (1er tap → « Confirmer ? »
    2 s, 2e tap → `DELETE`), haptique `error`.

### 5.11 Login

- Écran centré, carte **`glassStrong`** `RoundedCornerShape(34.dp)`, sur aurora.
- Titre « Create. » (`displaySmall`) + sous-titre « Connecte-toi pour créer. » (`bodyMedium
  onSurfaceVariant`).
- `OutlinedTextField` **identifiant/email** (autocap off, `KeyboardType.Email`) + `OutlinedTextField`
  **mot de passe** (visualTransformation + toggle œil). Focus/bord actifs = iris.
- **Bouton « Se connecter »** : `Button` pleine largeur, fond **`IrisBrush`**, `onPrimary`.
- Erreur inline `error` « Identifiant ou mot de passe incorrect ».
- Auth = PB direct `authWithPassword` → token Bearer (Keychain Android = `EncryptedSharedPreferences`).

### 5.12 Notifications (cloche)

- `IconButton` cloche (`Notifications` / `NotificationsActive`). États : unsupported / denied /
  available / subscribed → glyphe + tint (iris si subscribed, `onSurfaceVariant` sinon).
- **FCM** (remplace VAPID web). Tap = toggle abonnement (demande `POST_NOTIFICATIONS` runtime API 33+).
- Push à complétion : titre « ✨ Ta création est prête » / « ❌ Génération échouée », corps = prompt
  (90 car), deep-link `create://gallery`, `tag = generationId`. Canal `generations` (importance HIGH),
  petite icône monochrome, **couleur d'accent de notif = iris blue**.

---

## 6. Motion & haptique

### 6.1 Principes motion (Material Motion + Expressive springs)

- **Ressorts par défaut** (Expressive) plutôt que courbes durées :
  - `spatialSpring` (déplacement/taille) : `spring(dampingRatio = 0.8f, stiffness = 380f)`
    (`MotionScheme.expressive().spatialSpec`-like).
  - `effectsSpring` (couleur/alpha) : `spring(dampingRatio = 1f, stiffness = 700f)`.
- **Durées** (quand tween nécessaire) : rapide 120 ms, standard 240 ms, emphatic 400 ms ; easing
  `EaseOutCubic` (entrée), `EaseInOutCubic` (transition).

### 6.2 Mapping des animations web → Compose

| Web | Compose | Spec |
|---|---|---|
| `.press` (scale 0.94) | `Modifier.pressScale()` via `interactionSource` | `animateFloatAsState` 0.94 sur press, `effectsSpring` |
| `.rise` (cartes feed) | `AnimatedVisibility` | `slideInVertically { it/6 } + fadeIn`, `spatialSpring` |
| `.pop` (badges/vignettes) | `AnimatedVisibility` | `scaleIn(spring(0.55f, 500f)) + fadeIn` |
| `.sheet-up` | `ModalBottomSheet` défaut M3 | slide + scrim, `spatialSpring` |
| `.fade-in` | `AnimatedVisibility(fadeIn)` | 180 ms |
| `.shimmer` (skeleton) | `Brush` translaté | dégradé `surfaceVariant→surface→surfaceVariant`, `rememberInfiniteTransition` 1200 ms linéaire |
| `.progress-line` (barre indéterminée iris) | `LinearWavyProgressIndicator` | brush iris (fallback `LinearProgressIndicator` teinté) |
| `.lightbox` (scale-in) | `SharedTransitionLayout` + `sharedElement` | miniature → plein écran, `spatialSpring` |
| `.mic-live` (halo) | `rememberInfiniteTransition` | rayon/alpha rouge, 900 ms (§5.7) |
| compteur crédits | `animateIntAsState` | roll-up spring |

- **Shared element** feed/galerie → lightbox : `SharedTransitionScope` (Compose 1.7+) sur la
  miniature (`sharedBounds`), transition `spatialSpring`.
- **Morph de forme** (Expressive) sur bouton Générer & boutons ronds : `androidx.graphics.shapes`
  `Morph(circle, rounded)` piloté par la progression du press.
- **Réduction de mouvement** : si `Settings.Global.ANIMATOR_DURATION_SCALE == 0` ou pref
  accessibilité → shimmer/halo/morph désactivés, transitions → fade court. Aurora reste statique de
  toute façon.

### 6.3 Haptique (`haptics.ts` → `HapticFeedback` / `VibratorManager`)

| Token | Déclencheur | Effet Android |
|---|---|---|
| `tap` | tous boutons, copier prompt | `HapticFeedbackConstants.CONTEXT_CLICK` (léger) |
| `select` | changement modèle, toggle chip/param, onglet | `SEGMENT_TICK` / `CLOCK_TICK` |
| `launch` | appui Générer | `CONFIRM` ou pattern court (`VibrationEffect` 15 ms) |
| `success` | génération prête, transcript reçu | `VibrationEffect.EFFECT_HEAVY_CLICK` |
| `error` | échec, annulation, delete | double-tick (`VibrationEffect` waveform `[0,20,60,20]`) |

Passer par `LocalHapticFeedback` quand possible ; fallback `VibratorManager` (API 31+) pour
`success`/`error`. Respecter le réglage système « retour haptique ».

---

## 7. Light / Dark

- **Défaut** : suit `isSystemInDarkTheme()`. Si l'app doit trancher sans signal → **light** (fidèle
  à l'esprit web light-only et à la règle projet « light par défaut »).
- **Préférence utilisateur** (Réglages) : `Système / Clair / Sombre` (`DataStore`). Le web est
  light-only mais Android **fournit** le dark (attendu plateforme, OLED). Le dark n'est **pas** un
  bricolage : c'est un `ColorScheme` complet (§2.3) + aurora sombre + surfaces revalorisées.
- L'**iris** est constant light/dark (brush identique). Contraste vérifié : `onPrimary`/`primary`
  ≥ 4.5:1 dans les deux modes.
- `SideEffect` : status bar & nav bar transparentes (`enableEdgeToEdge`), icônes claires/sombres
  selon le mode ; couleur de thème light = `#EEF0FD`, dark = `#0E1220`.

---

## 8. Accessibilité & RTL

- **Contraste** : Ink `#2A3142` sur aurora/surface ≥ 7:1 ; InkSoft `#5D6478` ≥ 4.5:1. Chips
  sélectionnés : texte `onPrimaryContainer` ≥ 4.5:1. Ne jamais transmettre une info **par la seule
  couleur** : failed = icône + texte, cancelled = libellé « Annulée » explicite.
- **Cibles tactiles** ≥ 48×48 dp (boutons ronds 44 dp + padding). `Modifier.minimumInteractiveComponentSize()`.
- **`contentDescription`** sur toutes les icônes (cloche, micro, photo, générer, fermer, actions
  lightbox). Miniatures : description = prompt tronqué.
- **Dynamic type** : respecter `fontScale` (sp partout, pas de hauteur fixe sur le texte ; le composer
  auto-grow suit l'échelle).
- **TalkBack** : ordre logique header → feed → composer ; sheets = `Modal` (focus piégé) ; annonce
  live « génération prête » via `LiveRegion` sur la carte qui passe done.
- **RTL** : `LayoutDirection.Rtl` pleinement supporté. Bulles de feed **alignées à gauche** en RTL
  (miroir de l'alignement « sortant »), coin-queue miroité. Icônes directionnelles (↑ Générer,
  flèche retour, ✕) via `Modifier` auto-mirroré ou drawables `autoMirrored`. Le dégradé iris 135°
  n'est **pas** miroité (identité de marque, orientation constante). Paddings via `start/end`
  (jamais `left/right`).
- **Réduction transparence/mouvement** : si activée → blur off, translucidité montée à ≥ 92 %
  d'opacité pour lisibilité, animations réduites (§6.2).

---

## 9. Résumé d'implémentation (ordre de construction du thème)

1. `CreateColorScheme.kt` : `CreateLightScheme` / `CreateDarkScheme` (§2.2/2.3) + brand constants +
   `IrisBrush` dans un `CompositionLocal`.
2. `CreateType.kt` : familles + `Typography` override serif/sans (§1).
3. `CreateShapes.kt` : `Shapes` Expressive gonflées + bulle-queue + morphs (§4).
4. `CreateTheme.kt` : `MaterialExpressiveTheme(...)` assemblant scheme (dynamic-aware §2.4) + aurora
   background + glass modifiers (§3) + `LocalHaptics`.
5. Composants (§5) puis motion/haptique (§6), enfin passes a11y/RTL (§8).

**Règles d'or** : (1) l'iris survit toujours, même en dynamic color ; (2) pas de faux liquid glass —
surfaces tonales + blur léger optionnel ; (3) formes très arrondies & expressives ; (4) light par
défaut, dark fourni et complet ; (5) motion en ressorts Expressive, jamais gratuit.

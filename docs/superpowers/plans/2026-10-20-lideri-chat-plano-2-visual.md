# Lideri Chat — Plano 2: visual Lideri — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O app inteiro (Android e iOS, Material e Liquid Glass, claro e escuro) passa a ter as cores, as fontes, o ícone, a logo e a abertura da marca Lideri, sem seletor de cor.

**Architecture:** Toda a marca visual fica em arquivos novos (`LideriColors.kt`, `LideriLogo.kt`, `design/lideri/`), e os arquivos do upstream recebem só um ponto de ligação cada (o esquema de cores em `Theme.kt`, as famílias em `Type.kt`, uma chamada de `LideriLogo` por tela). Os ativos vêm do kit de marca copiados byte a byte para `design/lideri/kit/`, e o `check-brand.sh` passa a provar isso por hash.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (resources `font/` e `drawable/`), Material 3 `ColorScheme`, Python 3 + Pillow + NumPy (gerador de ícones), Xcode asset catalog.

**Spec:** `docs/superpowers/specs/2026-10-19-lideri-chat-design.md` (§2.2, §5.2, §5.3 cores, §5.4 logo, §6 tema/ativos, §8). Plano 1 (servidor e identidade) já está no `main` (`c20dec2e`).

**Kit de marca (fonte dos ativos):** `/private/tmp/claude-501/-Users-franklinferreira-orca-projects-app-chatlideri/f00f4add-df84-4db8-b720-e961fbfbc7c3/scratchpad/kit-lideri/LIDERI_Kit_de_Marca_v1.0/` (doravante `$KIT`). É temporário: a Task 1 o copia para o repositório e daí em diante só `design/lideri/kit/` vale.

## Global Constraints

- Primária clara **exatamente** `#0424D9` com texto `#FFFFFF`; primária escura **exatamente** `#0797EF` com texto `#1F2937`. Superfícies neutras (sem tingimento azul) nos dois modos.
- Laranja `#FF6022` só em destaque pontual (papel `tertiary`); nunca `primary`/`secondary`.
- Seletor de cor de destaque e cor dinâmica (wallpaper) somem da UI. Modo claro/escuro e Material/Liquid Glass continuam.
- Fontes: Poppins 600/700 em títulos (`display*`, `headline*`, `title*`), Raleway 400/500 no corpo (`body*`), Raleway 500/600 em rótulos (`label*`). Arquivos `.ttf` do kit copiados sem alteração, com o `*-OFL.txt` ao lado.
- Logo dentro do app: assinatura **completa** (com os cinco ícones), PNG do kit sem alteração (`lideri-principal.png` em fundo claro, `lideri-negativa.png` em fundo escuro), largura ≥ 220dp, proporção travada (`ContentScale.Fit`), respiro = 7,3% da largura em todos os lados (`clear_space_fraction_of_total_width` = 0.0729 do kit = H/4).
- Ícone: só a palavra LIDERI do `lideri-negativa.png` (linhas de pixel 0–700 do PNG 2400×932; os ícones começam na linha 759), cor `#FFFDFA` como está, sobre `#0424D9`, sem redesenhar/esticar/recolorir, respiro ≥ H/4.
- Hashes do kit (sha256, de `integridade-sha256.json`):
  - `logos/lideri-principal.png` `c3e7416c7147043e94fc06def8fc42ee1878777eb3efe4a6afad2dd26d5ea7e1`
  - `logos/lideri-negativa.png` `7f8bdd370d5b0efdb99db52adaddefb985a4425dbb8100dd2c3f121893531138`
- Código, comentários e commits em inglês, Conventional Commits com escopo. Nada de `BuildConfig.FLAVOR`/`DistributionChannel` em call site (detekt). Telas usam `Adaptive*`.
- Não editar `CLAUDE.md` do upstream (camada Orca/Lideri fica em `ORCA.md`, `design/lideri/`, `scripts/lideri/`).
- Verificação de cada task: `./gradlew detekt :app:lint --continue` e os testes dos módulos tocados; `:core:data` precisa rodar fora da sandbox (MockK self-attach). Mudança de UI só está pronta com print do emulador **e** do simulador iOS, nos modos claro e escuro.
- Credenciais da conta de teste: só pelo Keychain (`security find-generic-password -s lideri-chat-test -a franklin@lideri.com.br -w`), digitadas no campo de **senha**, nunca impressas em log, relatório ou commit.

## Review Focus

1. **Liquid Glass no iOS** deriva tudo de `MaterialTheme.colorScheme`; um papel neutro mal escolhido (ex.: `surfaceContainer*` azulado ou `outline` claro demais) aparece como vidro tingido ou borda invisível. → Task 1 testa neutralidade de todos os papéis de superfície; Task 5 exige prints Liquid Glass claro/escuro.
2. **Modo escuro com o PNG errado**: logo azul `#0424D9` sobre fundo escuro some. → Task 3 testa que `lideriLogoAsset(dark = true)` é a negativa.
3. **Usuário que já tinha escolhido outra cor de destaque ou cor dinâmica** (DataStore com valor salvo) continuaria vendo a cor antiga se o tema ainda lesse a preferência. → Task 1 testa que o esquema não depende de `accentColor`/`useDynamicColor`.
4. **Tela estreita (320dp) ou fonte grande** no login: logo de 220dp + respiro não pode empurrar o formulário para fora nem encolher abaixo de 220dp. → Task 3 fixa largura 240dp dentro da coluna que já rola (`verticalScroll`), print em fonte 1.3×.
5. **Ícone temático (monochrome) do Android 13+** e máscara redonda: a palavra precisa caber no círculo seguro de 66dp. → Task 4 gera monochrome e o script imprime a fração usada; print do launcher com ícone temático ligado.

---

### Task 1: Esquema de cores Lideri e importação do kit

**Files:**
- Create: `design/lideri/kit/` (cópia de `$KIT/logos/*`, `$KIT/fonts/*`, `$KIT/brand-tokens.json`, `$KIT/tokens.css`, `$KIT/integridade-sha256.json`, `$KIT/LEIA-ME.txt`, `$KIT/Manual_de_Marca_LIDERI_v1.0.pdf`; **sem** a pasta `referencias/`)
- Create: `design/lideri/README.md` (o que é o kit, regra de não editar, como regenerar ícones — esta última seção entra na Task 4)
- Create: `core/ui/src/commonMain/kotlin/com/garfiec/librechat/core/ui/theme/LideriColors.kt`
- Modify: `core/ui/src/commonMain/kotlin/com/garfiec/librechat/core/ui/theme/Theme.kt` (só `rememberLibreChatColorScheme` e o KDoc de precedência)
- Test: `core/ui/src/commonTest/kotlin/com/garfiec/librechat/core/ui/theme/LideriColorsTest.kt`

**Interfaces:**
- Produces: `val LideriLightColorScheme: ColorScheme`, `val LideriDarkColorScheme: ColorScheme`, `fun lideriColorScheme(darkTheme: Boolean): ColorScheme` (em `LideriColors.kt`). `LibreChatTheme`/`ThemePreview` mantêm a assinatura (parâmetros `accentColor`/`useDynamicColor` continuam existindo e passam a ser ignorados — evita conflito com call sites do upstream).

Valores (escolha minha a partir de `brand-tokens.json`; os não listados ficam com o padrão de `lightColorScheme()`/`darkColorScheme()` do M3 **desde que neutros** — o teste decide):

| Papel | Claro | Escuro |
|---|---|---|
| primary / onPrimary | `#0424D9` / `#FFFFFF` | `#0797EF` / `#1F2937` |
| primaryContainer / onPrimaryContainer | `#DDE3FF` / `#02127A` | `#0B2A6B` / `#DDE3FF` |
| secondary / onSecondary | `#0797EF` / `#1F2937` | `#07C4EF` / `#1F2937` |
| secondaryContainer / onSecondaryContainer | `#E1F2FD` / `#063A5C` | `#0A3550` / `#D6EEFC` |
| tertiary / onTertiary | `#FF6022` / `#1F2937` | `#FF7C24` / `#1F2937` |
| background, surface | `#FFFFFF` | `#121417` |
| onBackground, onSurface | `#1F2937` | `#E8EAED` |
| surfaceVariant / onSurfaceVariant | `#F5F7FA` / `#526071` | `#2A2D32` / `#B4BAC2` |
| surfaceContainerLowest…Highest | `#FFFFFF`, `#F8F9FB`, `#F5F7FA`, `#EEF1F5`, `#E8ECF1` | `#0D0F11`, `#181A1D`, `#1C1F22`, `#24272B`, `#2E3135` |
| surfaceDim / surfaceBright | `#E2E6EB` / `#FFFFFF` | `#121417` / `#36393E` |
| outline / outlineVariant | `#8E9399` / `#D9E0EA` | `#8A919B` / `#3A3E44` |
| inverseSurface / inverseOnSurface / inversePrimary | `#1F2937` / `#F5F7FA` / `#0797EF` | `#E8EAED` / `#1F2937` / `#0424D9` |
| surfaceTint | `#0424D9` | `#0797EF` |

- [ ] **Step 1: Copiar o kit** para `design/lideri/kit/` com `cp` (sem `referencias/`) e conferir: `cd design/lideri/kit && shasum -a 256 logos/lideri-principal.png logos/lideri-negativa.png` → os dois hashes das Global Constraints.

- [ ] **Step 2: Escrever o teste que falha** em `LideriColorsTest.kt`:

```kotlin
class LideriColorsTest {
    @Test fun lightPrimaryIsTheBrandBlueWithWhiteText() {
        assertEquals(Color(0xFF0424D9), LideriLightColorScheme.primary)
        assertEquals(Color(0xFFFFFFFF), LideriLightColorScheme.onPrimary)
    }
    @Test fun darkPrimaryIsTheSecondaryBlueWithInkText() {
        assertEquals(Color(0xFF0797EF), LideriDarkColorScheme.primary)
        assertEquals(Color(0xFF1F2937), LideriDarkColorScheme.onPrimary)
    }
    @Test fun orangeIsNeverAnActionColour() { /* primary, secondary, primaryContainer, secondaryContainer != 0xFF6022/0xFF7C24, nos dois esquemas */ }
    @Test fun surfacesAreNeutral() {
        // Para background, surface, surfaceVariant, os 5 surfaceContainer*, surfaceDim, surfaceBright,
        // nos dois esquemas: max(r,g,b) - min(r,g,b) <= 0.04f (em 0..1). Mensagem de falha nomeia o
        // papel e o esquema. Texto e contorno (onSurface, outline…) ficam de fora: são os cinzas
        // editoriais do kit (#1F2937, #526071, #D9E0EA), levemente frios por design.
    }
    @Test fun schemeIgnoresAccentAndDynamicColour() {
        assertSame(LideriLightColorScheme, lideriColorScheme(darkTheme = false))
        assertSame(LideriDarkColorScheme, lideriColorScheme(darkTheme = true))
    }
    @Test fun brandPairsMeetWcagAa() { /* contraste(primary,onPrimary) >= 4.5 nos dois; idem onSurface/surface e onSurfaceVariant/surface */ }
}
```

O cálculo de contraste é a fórmula WCAG 2.2 (luminância relativa); escreva um helper privado no teste.

- [ ] **Step 3: Rodar e ver falhar:** `./gradlew :core:ui:testDebugUnitTest --tests '*LideriColorsTest*'` → falha de compilação (`LideriLightColorScheme` não existe).

- [ ] **Step 4: Implementar `LideriColors.kt`** com `lightColorScheme(...)`/`darkColorScheme(...)` e a tabela acima; `lideriColorScheme(darkTheme)` devolve um dos dois `val`. Em `Theme.kt`, `rememberLibreChatColorScheme` passa a `return lideriColorScheme(darkTheme)` (os parâmetros de accent/dynamic ficam sem uso: marque com `@Suppress("UNUSED_PARAMETER")` só se o detekt reclamar) e o KDoc de `LibreChatTheme` diz que a cor é sempre a da marca Lideri (`LideriColors.kt`) e que accent/dynamic são ignorados. Remova o import de MaterialKolor de `Theme.kt` se ficar sem uso; não remova a dependência do Gradle.

- [ ] **Step 5: Rodar e ver passar:** o mesmo comando → PASS. Depois `./gradlew :core:ui:iosSimulatorArm64Test --tests '*LideriColorsTest*'` (ou o teste iOS completo do módulo) → PASS.

- [ ] **Step 6: Commit**

```bash
git add design/lideri core/ui/src/commonMain/kotlin/com/garfiec/librechat/core/ui/theme/LideriColors.kt core/ui/src/commonMain/kotlin/com/garfiec/librechat/core/ui/theme/Theme.kt core/ui/src/commonTest/kotlin/com/garfiec/librechat/core/ui/theme/LideriColorsTest.kt
git commit -m "feat(ui): paint the app in the Lideri brand colours"
```

---

### Task 2: Tipografia Poppins + Raleway

**Files:**
- Create: `core/ui/src/commonMain/composeResources/font/poppins_semibold.ttf`, `poppins_bold.ttf`, `raleway_regular.ttf`, `raleway_medium.ttf`, `raleway_semibold.ttf` (cópias de `design/lideri/kit/fonts/`, renomeadas só em minúsculas/underscore, conteúdo idêntico)
- Create: `core/ui/src/commonMain/composeResources/files/licenses/Poppins-OFL.txt`, `Raleway-OFL.txt` (cópias)
- Modify: `core/ui/src/commonMain/kotlin/com/garfiec/librechat/core/ui/theme/Type.kt`
- Modify: `core/ui/src/commonMain/kotlin/com/garfiec/librechat/core/ui/theme/Theme.kt` (as duas chamadas `typography = libreChatTypography` → `libreChatTypography()`)
- Test: `core/ui/src/commonTest/kotlin/com/garfiec/librechat/core/ui/theme/LideriTypographyTest.kt`

**Interfaces:**
- Consumes: nada da Task 1.
- Produces: `fun lideriTypography(heading: FontFamily, body: FontFamily): Typography` (pura, testável) e `@Composable fun libreChatTypography(): Typography` (carrega as fontes com `org.jetbrains.compose.resources.Font(Res.font.…, weight)` e chama `lideriTypography`). O `val libreChatTypography` deixa de existir.

Escala (escolha minha a partir de `digital_scale_recommended`; tamanhos dos papéis que o upstream já definia ficam iguais para não mexer em layouts):
- `display*`, `headline*`, `title*`: família Poppins; `headlineLarge` 700, os demais 600.
- `body*`: Raleway 400 (`bodyLarge` 16/24, `bodyMedium` 14/20 como hoje, `bodySmall` 12/16).
- `label*`: Raleway; `labelLarge` 600, `labelMedium`/`labelSmall` 500 (`labelSmall` 12/16 como hoje).
- Papéis que o upstream não definia: tamanhos padrão do `Typography()` do M3, só trocando família e peso.

- [ ] **Step 1: Teste que falha** em `LideriTypographyTest.kt`: monte `lideriTypography(heading = FontFamily.Serif, body = FontFamily.Monospace)` (famílias-sentinela) e verifique: todos os 9 estilos `display*/headline*/title*` usam `Serif` e peso ≥ 600; todos `body*` usam `Monospace` com peso 400; todos `label*` usam `Monospace` com peso 500 ou 600; `bodyLarge.fontSize == 16.sp`, `bodyMedium.fontSize == 14.sp`, `headlineMedium.fontSize == 24.sp`.

- [ ] **Step 2: Rodar e ver falhar:** `./gradlew :core:ui:testDebugUnitTest --tests '*LideriTypographyTest*'` → não compila.

- [ ] **Step 3: Copiar fontes e licenças**, implementar `lideriTypography`/`libreChatTypography()` em `Type.kt` e trocar as duas chamadas em `Theme.kt`.

- [ ] **Step 4: Rodar e ver passar** o teste; depois `./gradlew :core:ui:testDebugUnitTest :shared:linkDebugFrameworkIosSimulatorArm64` → BUILD SUCCESSFUL (prova que o recurso de fonte resolve também no iOS).

- [ ] **Step 5: Tela de licenças:** confirme que a tela de licenças open source do app lista as fontes; se ela for gerada só de dependências Gradle e não incluir as fontes, adicione uma entrada "Poppins / Raleway — SIL Open Font License 1.1" pelo mesmo mecanismo que a tela já usa para entradas manuais. Se não existir mecanismo manual, registre como pendência de publicação no relatório (não invente um).

- [ ] **Step 6: Commit** — `feat(ui): set type in Poppins and Raleway from the Lideri kit`

---

### Task 3: Logo Lideri no login, na tela de conexão e em Sobre

**Files:**
- Create: `core/ui/src/commonMain/composeResources/drawable/lideri_principal.png`, `lideri_negativa.png` (cópias **byte a byte** de `design/lideri/kit/logos/`)
- Create: `core/ui/src/commonMain/kotlin/com/garfiec/librechat/core/ui/brand/LideriLogo.kt`
- Modify: `feature/auth/src/commonMain/kotlin/com/garfiec/librechat/feature/auth/screen/LoginScreen.kt` (logo acima do título, na coluna que já rola)
- Modify: `feature/auth/src/commonMain/kotlin/com/garfiec/librechat/feature/auth/screen/ServerUrlScreen.kt` (ramo travado "Connecting to…": logo acima do texto)
- Modify: `feature/settings/src/commonMain/kotlin/com/garfiec/librechat/feature/settings/screen/GeneralSettingsScreen.kt` (`AboutInfo`: logo centralizada no topo)
- Test: `core/ui/src/commonTest/kotlin/com/garfiec/librechat/core/ui/brand/LideriLogoTest.kt`; `app/src/androidTest/kotlin/com/garfiec/librechat/AuthFlowTest.kt` (asserção extra no teste travado)

**Interfaces:**
- Consumes: `LocalDarkTheme` (`core/ui/.../theme/UiStyleLocals.kt`).
- Produces:
  - `enum class LideriLogoAsset { Principal, Negativa }`
  - `fun lideriLogoAsset(dark: Boolean): LideriLogoAsset` (pura)
  - `const val LIDERI_LOGO_MIN_WIDTH_DP = 220`, `const val LIDERI_LOGO_CLEAR_SPACE_FRACTION = 0.073f`, `const val LIDERI_LOGO_ASPECT = 2400f / 932f`
  - `@Composable fun LideriLogo(modifier: Modifier = Modifier, width: Dp = 240.dp, dark: Boolean = LocalDarkTheme.current)` — `Image(painterResource(...), contentDescription = "Lideri", contentScale = ContentScale.Fit)`, largura `max(width, 220.dp)`, altura `largura / LIDERI_LOGO_ASPECT`, `padding(largura * 0.073f)`, `testTag("lideri_logo")`. O `contentDescription` é o nome da marca (não traduzir).

- [ ] **Step 1: Teste que falha** (`LideriLogoTest.kt`): `lideriLogoAsset(dark = false) == Principal`, `lideriLogoAsset(dark = true) == Negativa`, `LIDERI_LOGO_MIN_WIDTH_DP == 220`, `abs(LIDERI_LOGO_ASPECT - 2.5763) < 0.01`, `abs(LIDERI_LOGO_CLEAR_SPACE_FRACTION - 0.0729) < 0.001`.
- [ ] **Step 2: Rodar e ver falhar** (`./gradlew :core:ui:testDebugUnitTest --tests '*LideriLogoTest*'`).
- [ ] **Step 3: Implementar** `LideriLogo.kt` e copiar os PNGs; conferir `shasum -a 256 core/ui/src/commonMain/composeResources/drawable/lideri_*.png` contra os hashes das Global Constraints.
- [ ] **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Colocar nas telas:** `LideriLogo()` no topo da `Column` do `LoginScreen` (antes do título, com `Spacer(16.dp)` depois), no ramo travado do `ServerUrlScreen`, e no topo de `AboutInfo` (centralizada). Nenhuma outra tela.
- [ ] **Step 6: Teste de UI:** no `AuthFlowTest.lockedServerUrlScreen_showsConnectingWithoutUrlField` acrescente `onNodeWithTag("lideri_logo").assertIsDisplayed()`; rode com o emulador ligado: `./gradlew :app:connectedPlayDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.garfiec.librechat.AuthFlowTest` → PASS.
- [ ] **Step 7: Commit** — `feat(ui): show the Lideri signature on sign-in and About`

---

### Task 4: Ícone do app (Android e iOS) e tela de abertura

**Files:**
- Create: `design/lideri/generate-icons.py`
- Create: `design/lideri/icon-master-1024.png` (saída do script; a referência aprovada)
- Modify (saída do script): `app/src/main/res/mipmap-*/ic_launcher_foreground.png`, `ic_launcher_monochrome.png`, `ic_launcher.png`, `ic_launcher_round.png`; `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Icon-iOS-Default-*.png` (mesmos nomes e tamanhos do `Contents.json` atual, que não muda)
- Modify: `app/src/main/res/values/ic_launcher_background.xml` → `#0424D9`
- Modify: `app/src/main/res/values/themes.xml` e `values-night/themes.xml` → `splash_background` `#0424D9`
- Create: `iosApp/iosApp/Assets.xcassets/LaunchBackground.colorset/Contents.json` (`#0424D9`), `iosApp/iosApp/Assets.xcassets/LaunchLogo.imageset/` (`lideri-negativa.png` reduzida para 240pt de largura: @2x 480px, @3x 720px, gerada pelo script)
- Modify: `iosApp/iosApp/Info.plist` → `UILaunchScreen` com `UIColorName = LaunchBackground` e `UIImageName = LaunchLogo`
- Modify: `design/lideri/README.md` (seção "Regenerar ícones")

**Interfaces:**
- Consumes: `design/lideri/kit/logos/lideri-negativa.png`.
- Produces: `python3 design/lideri/generate-icons.py` (raiz do repo; Pillow + NumPy) — idempotente; imprime a fração do círculo seguro usada pela palavra e o sha256 de `icon-master-1024.png`.

Algoritmo (decidido aqui; o script não toma outras decisões):
1. Abrir `lideri-negativa.png` (RGBA). Linhas de pixel com alfa > 0 formam dois blocos separados por linhas vazias; o **primeiro bloco** é a palavra (0–700). Recortar esse bloco na caixa de alfa > 0. Abortar com erro se não houver exatamente dois blocos.
2. A palavra recortada é usada como está (sem recolorir). Redução só por `downscale` com alfa pré-multiplicado (reaproveite a função de `design/generate-android-icons.py`, copiando-a — não importe aquele arquivo, que é do upstream).
3. **Android foreground/monochrome (108dp):** largura da palavra = 60dp, centralizada (cabe no círculo seguro de 66dp). Monochrome = mesma silhueta em branco opaco. Legacy `ic_launcher`/`ic_launcher_round` (48dp): fundo `#0424D9` (quadrado arredondado / círculo) com a palavra a 62% da largura.
4. **iOS 1024 (master):** fundo `#0424D9` opaco em toda a tela, **sem alfa** (exigência da App Store), palavra com 70% da largura, centralizada. Os demais tamanhos são reduções do master.
5. **LaunchLogo:** a assinatura **completa** `lideri-negativa.png` reduzida proporcionalmente (480 e 720px de largura).

- [ ] **Step 1:** Escrever o script; rodar; conferir que imprime fração ≤ 1.0 do círculo seguro e que `python3 -I -c "from PIL import Image; im=Image.open('design/lideri/icon-master-1024.png'); print(im.mode, im.size)"` → `RGB (1024, 1024)`.
- [ ] **Step 2:** Rodar de novo e conferir com `git status` que nada mudou (idempotente).
- [ ] **Step 3:** Ajustar `ic_launcher_background.xml`, `splash_background`, `Info.plist` e os asset catalogs de abertura.
- [ ] **Step 4:** Build e instalação: `./gradlew assemblePlayDebug` + `adb install -r app/build/outputs/apk/play/debug/app-play-debug.apk`; iOS pelo comando do `ORCA.md`. Prints: launcher Android (ícone normal e temático/monochrome ligado), abertura Android, tela inicial do iOS, abertura iOS. Mostrar o ícone ao controlador antes do commit (é a peça que o usuário vai aprovar).
- [ ] **Step 5: Commit** — `feat(brand): Lideri app icon and launch screen`

---

### Task 5: Fim do seletor de cor e da cor dinâmica

**Files:**
- Modify: `feature/settings/src/commonMain/kotlin/com/garfiec/librechat/feature/settings/screen/AppearanceSettingsScreen.kt` (remover as linhas `accent_color_row` e `dynamic_color_toggle`, o diálogo, `choosingAccent`, `DISABLED_ALPHA`, `AccentColorRow`, `DynamicColorToggle` e imports órfãos; KDoc da tela: "theme and interface style")
- Delete: `feature/settings/src/commonMain/kotlin/com/garfiec/librechat/feature/settings/screen/AccentColorDialog.kt`
- Delete/ajustar: `core/ui/src/commonMain/kotlin/com/garfiec/librechat/core/ui/theme/AccentColors.kt` — **manter** `DefaultAccentSeed` (ainda referenciado por `ThemeDataStore`/previews); remover `AccentColorPresets` se ficar sem uso
- Strings órfãs (`accent_*`, `dynamic_color_*` em `feature/settings/.../composeResources/values*/strings.xml`): **não** remover (evita conflito em todo merge de tradução do upstream); o detekt/lint não acusa string sem uso em compose resources — confirme rodando o lint.
- Não mexer em `ThemeDataStore` nem em `AppearanceSettingsViewModel` (preferências salvas ficam inertes; ver Task 1, teste `schemeIgnoresAccentAndDynamicColour`).

- [ ] **Step 1:** Remover a UI; `./gradlew :feature:settings:testDebugUnitTest detekt :app:lint --continue` → verde.
- [ ] **Step 2:** Prints da tela Configurações → Geral → Aparência no Android (Material) e no iOS (Liquid Glass), claro e escuro: só tema e estilo de interface.
- [ ] **Step 3: Commit** — `feat(settings): drop the accent and wallpaper colour pickers for the Lideri brand`

---

### Task 6: Verificação de marca, documentação e evidência final

**Files:**
- Modify: `scripts/lideri/check-brand.sh`
- Modify: `ORCA.md` (seção "Lideri Chat (white-label)": onde vivem cores, fontes, logo e ícone; como regenerar o ícone; que `check-brand.sh` confere hashes)

Novas checagens em `check-brand.sh` (cada uma com mensagem `fail` nomeando arquivo e motivo):
1. `core/ui/.../composeResources/drawable/lideri_principal.png` e `lideri_negativa.png` têm os sha256 das Global Constraints, e também batem com `design/lideri/kit/integridade-sha256.json` (lido com `python3 -I`).
2. Cada `.ttf` em `core/ui/.../composeResources/font/` tem o mesmo sha256 do arquivo correspondente do kit.
3. `design/lideri/icon-master-1024.png` tem o sha256 aprovado — constante `APPROVED_ICON_SHA256` no script, preenchida com o valor que a Task 4 imprimiu.
4. `Theme.kt` contém `lideriColorScheme(darkTheme)`; `LideriColors.kt` contém `0xFF0424D9` e `0xFF0797EF`.
5. `AppearanceSettingsScreen.kt` não contém `accent_color_row` nem `dynamic_color_toggle`.
6. `ic_launcher_background.xml` é `#0424D9`.

- [ ] **Step 1:** Implementar; `bash scripts/lideri/check-brand.sh` → `marca OK`.
- [ ] **Step 2:** Prova negativa: alterar 1 byte de uma cópia temporária do PNG da logo no lugar do original, rodar → `marca FALHOU` citando o arquivo; restaurar com `git checkout`; rodar → `marca OK`.
- [ ] **Step 3:** Suíte completa: `./gradlew detekt :app:lint test --continue` (`:core:data` fora da sandbox) e testes iOS do CI (`:shared:iosSimulatorArm64Test :core:common:iosSimulatorArm64Test :core:ui:iosSimulatorArm64Test` …, lista do job `ios`).
- [ ] **Step 4:** Evidência final, emulador e simulador, claro e escuro, Material e Liquid Glass: login com logo, conversa aberta (fontes e azul nos botões/bolhas), configurações, Sobre. Login real com a conta de teste via Keychain.
- [ ] **Step 5: Commit** — `chore(lideri): guard the visual brand in check-brand`

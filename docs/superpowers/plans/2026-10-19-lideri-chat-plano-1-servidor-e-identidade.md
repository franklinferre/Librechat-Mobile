# Lideri Chat — Plano 1: servidor fixo e identidade

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O app abre direto no login de `https://chat.lideri.cloud`, sem caminho para outro servidor
ou outra conta, e se apresenta como **Lideri Chat** (`cloud.lideri.chat` / `cloud.lideri.chat.ios`).

**Architecture:** Uma camada de marca pequena e isolada: as constantes ficam num arquivo novo
(`Brand.kt`), e uma classe `ServerLock`, injetada pelo Koin, liga e desliga o comportamento travado.
O código do upstream continua funcionando sem trava (`ServerLock(null)`), o que mantém seus testes
válidos e os merges baratos. Com a trava ativa, a tela `ServerUrl` não mostra campo nenhum: ela
conecta sozinha na URL fixa e se substitui pelo `Login`. Assim, logout, sessão expirada e deep link
continuam passando pelo mesmo caminho que o upstream já testa.

**Tech Stack:** Kotlin Multiplatform 2.4.10, Compose Multiplatform, Koin, Nav 3, JUnit4 + Truth +
MockK (`androidUnitTest`), kotlin.test (`commonTest`), Gradle 9.5.1, Xcode 27.1.

**Spec:** `docs/superpowers/specs/2026-10-19-lideri-chat-design.md`

**Sequência:** este é o plano 1 de 3. O **plano 2** (visual: cores, Poppins/Raleway, ícone, logo
nas telas) e o **plano 3** (rotina de sync com o upstream e verificação de marca contínua) vêm
depois e partem do resultado deste.

## Global Constraints

- URL fixa: `https://chat.lideri.cloud`, em todos os builds, debug inclusive (spec §5.3).
- Nome exibido: `Lideri Chat`. Debug Android: `Lideri Chat Dev`.
- Android `applicationId`: `cloud.lideri.chat` no flavor `play`; debug ganha `.debug`. O `foss` vira `cloud.lideri.chat.foss`.
- iOS `PRODUCT_BUNDLE_IDENTIFIER`: `cloud.lideri.chat.ios` em todas as configurações; `SWITCHBOARD_CHANNEL = appstore` em todas.
- Esquema de deep link: `liderichat`.
- Não renomear pacotes Kotlin (`com.garfiec.librechat.*`), namespace Android, target/scheme `iosApp`
  nem chaves técnicas do Info.plist (`SwitchboardChannel`, `SwitchboardVersionName`) (spec §3).
- Diferença de comportamento por marca passa pelo `ServerLock` injetado. Nada de `BuildConfig.FLAVOR`
  ou `DistributionChannel` em call site (regra do detekt, `CLAUDE.md` raiz).
- Política de privacidade: sem URL até a etapa de publicação. A linha some da UI e o código leva o marcador `PENDENTE-PUBLICACAO`.
- Senha da conta de teste nunca entra no git, em logs ou em PRs: fica no Keychain, serviço `lideri-chat-test`.
- Código, comentários e commits em inglês, Conventional Commits (`ORCA.md`).
- Trabalhar num worktree Orca: `orca worktree create --name lideri-plano-1 --setup run`.
- Gates antes de cada commit que mexe em Kotlin: `./gradlew detekt :app:lint --continue` e os testes do módulo tocado.

## Review Focus

1. **Logout ou sessão expirada estando offline:** a tela travada mostra o erro e um botão "Tentar
   novamente", sem campo de URL e sem ficar presa num loop de carregamento → teste em Task 2.
2. **Botão voltar no Login logo depois da conexão automática:** sai do app, sem voltar a uma tela
   de conexão que se reconecta sozinha → teste em Task 2.
3. **Instalação antiga com outra URL salva** (por exemplo, um build de desenvolvimento que gravou
   outro servidor, ou a cópia no Keychain do iOS): a trava sobrescreve com a URL fixa e nunca mostra
   a antiga → teste em Task 2.
4. **Deep link `liderichat://conversation/…` com o app deslogado:** cai no fluxo travado (conectar →
   login), nunca na tela de URL → teste em Task 6.
5. **Avatar do drawer com uma conta só:** tocar não abre o seletor de contas, e o gesto de arrastar
   não faz nada → teste em Task 3.

---

### Task 1: Constantes da marca e `ServerLock`

**Files:**
- Create: `core/common/src/commonMain/kotlin/com/garfiec/librechat/core/common/brand/Brand.kt`
- Create: `core/common/src/commonMain/kotlin/com/garfiec/librechat/core/common/brand/ServerLock.kt`
- Modify: `core/common/src/commonMain/kotlin/com/garfiec/librechat/core/common/di/CommonModule.kt` (registrar `ServerLock`)
- Test: `core/common/src/commonTest/kotlin/com/garfiec/librechat/core/common/brand/ServerLockTest.kt`

**Interfaces:**
- Produces:
  - `object Brand` com `const val APP_NAME = "Lideri Chat"`, `const val LOCKED_SERVER_URL = "https://chat.lideri.cloud"`,
    `const val DEEP_LINK_SCHEME = "liderichat"`, `const val USER_AGENT_PRODUCT = "LideriChat"`,
    `const val MEDIA_FOLDER = "Lideri Chat"`, `val PRIVACY_POLICY_URL: String? = null` (com comentário `PENDENTE-PUBLICACAO`).
  - `class ServerLock(val url: String?)` com `val isLocked: Boolean`. Koin: `single { ServerLock(Brand.LOCKED_SERVER_URL) }`.
    Testes do upstream constroem `ServerLock(null)` para o comportamento original.

- [ ] **Step 1: Write the failing test**

```kotlin
class ServerLockTest {
    @Test fun nullUrlIsUnlocked() = assertFalse(ServerLock(null).isLocked)
    @Test fun brandLockIsTheLideriServer() {
        val lock = ServerLock(Brand.LOCKED_SERVER_URL)
        assertTrue(lock.isLocked)
        assertEquals("https://chat.lideri.cloud", lock.url)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :core:common:testDebugUnitTest --tests '*ServerLockTest*'`
Expected: FAIL, unresolved reference `ServerLock`

- [ ] **Step 3: Implement `Brand` and `ServerLock`, register `single { ServerLock(Brand.LOCKED_SERVER_URL) }` in `CommonModule`**

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :core:common:testDebugUnitTest --tests '*ServerLockTest*' :core:common:iosSimulatorArm64Test`
Expected: PASS

- [ ] **Step 5: Commit** — `feat(brand): add Lideri brand constants and ServerLock`

---

### Task 2: Conexão automática no servidor travado

**Files:**
- Modify: `feature/auth/src/commonMain/kotlin/com/garfiec/librechat/feature/auth/viewmodel/ServerUrlViewModel.kt`
- Modify: `feature/auth/src/commonMain/kotlin/com/garfiec/librechat/feature/auth/screen/ServerUrlScreen.kt`
- Modify: `feature/auth/src/commonMain/kotlin/com/garfiec/librechat/feature/auth/di/AuthModule.kt`
- Modify: `feature/auth/src/commonMain/kotlin/com/garfiec/librechat/feature/auth/navigation/AuthNavigation.kt`
- Modify: `shared/src/commonMain/kotlin/com/garfiec/librechat/shared/navigation/LibreChatNavHost.kt` (passar `onReplace`)
- Modify: `feature/auth/src/commonMain/composeResources/values*/strings.xml` (string nova `connecting_to_app`: base `values/` em inglês "Connecting to %1$s…", `values-pt/` "Conectando ao %1$s…", demais idiomas traduzidos)
- Test: `feature/auth/src/androidUnitTest/kotlin/com/garfiec/librechat/feature/auth/viewmodel/ServerUrlViewModelTest.kt`

**Interfaces:**
- Consumes: `ServerLock` (Task 1).
- Produces:
  - `ServerUrlViewModel(…, accountSwitcher, serverLock: ServerLock, addAccount: Boolean = false)`.
  - `ServerUrlUiState.isLocked: Boolean`.
  - `fun ServerUrlViewModel.retry()`: só tem efeito com a trava; refaz `validateAndConnect()`.
  - `authEntries(onNavigate, onBack, onAuthComplete, onReplace: (NavKey) -> Unit)`. Com a trava,
    `ServerUrl` chama `onReplace(Login)` em vez de `onNavigate(Login)`. O host passa `navigator::replaceTop`.

Comportamento com a trava (`serverLock.isLocked && !addAccount`): no `init`, define `url = serverLock.url`
**sem** esperar nem usar a URL salva, não carrega cabeçalhos e chama `validateAndConnect()`. A tela
mostra o nome do app, um indicador de progresso e, se der erro, a mensagem com o botão
"Tentar novamente". Nunca mostra o campo de URL nem a seção "Avançado".

- [ ] **Step 1: Write the failing tests** (em `ServerUrlViewModelTest`, com `createViewModel(addAccount, serverLock = ServerLock(null))` como padrão; os testes existentes não mudam)

```kotlin
@Test fun `locked mode connects to the brand URL without user input`() = runTest(testDispatcher) {
    val vm = createViewModel(addAccount = false, serverLock = ServerLock("https://chat.lideri.cloud"))
    advanceUntilIdle()
    assertThat(vm.uiState.value.isLocked).isTrue()
    assertThat(vm.uiState.value.isValidated).isTrue()
    coVerify(exactly = 1) { serverDataStore.setServerUrl("https://chat.lideri.cloud") }
}

@Test fun `locked mode ignores a different stored URL`() = runTest(testDispatcher) {
    coEvery { serverDataStore.awaitBaseUrl() } returns "https://old.example.com"
    val vm = createViewModel(addAccount = false, serverLock = ServerLock("https://chat.lideri.cloud"))
    advanceUntilIdle()
    assertThat(vm.uiState.value.url).isEqualTo("https://chat.lideri.cloud")
    coVerify(exactly = 0) { serverDataStore.setServerUrl("https://old.example.com") }
}

@Test fun `locked mode failure shows error and retry reconnects`() = runTest(testDispatcher) {
    coEvery { configRepository.resolveServer(any(), any()) } returns ServerResolution.Failed(Result.Error(message = "offline"))
    val vm = createViewModel(addAccount = false, serverLock = ServerLock("https://chat.lideri.cloud"))
    advanceUntilIdle()
    assertThat(vm.uiState.value.error).isNotNull()
    assertThat(vm.uiState.value.isLoading).isFalse()
    coEvery { configRepository.resolveServer(any(), any()) } coAnswers { ServerResolution.Resolved(firstArg<List<String>>().first(), config) }
    vm.retry(); advanceUntilIdle()
    assertThat(vm.uiState.value.isValidated).isTrue()
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :feature:auth:testDebugUnitTest --tests '*ServerUrlViewModelTest*'`
Expected: FAIL (o construtor não aceita `serverLock`)

- [ ] **Step 3: Implement**: ViewModel conforme o bloco Interfaces; `AuthModule` injeta `serverLock = get()`;
  ramo travado no `ServerUrlScreen` (título via `connecting_to_app` com `Brand.APP_NAME`;
  botão de nova tentativa reutiliza a string de "retry" já existente em `core/ui` ou `feature/auth`); `authEntries` ganha `onReplace`, e o host passa `navigator::replaceTop`.

- [ ] **Step 4: Run to verify they pass, plus the graph check**

Run: `./gradlew :feature:auth:testDebugUnitTest :app:testFossDebugUnitTest --tests '*KoinGraph*' --tests '*Navigator*' :shared:iosSimulatorArm64Test`
Expected: PASS

- [ ] **Step 5: Back-stack test**: em `app/src/test/.../navigation/NavigatorTest.kt`, depois de `navigateToAuth()` e de
  `replaceTop(Login)`, a pilha é exatamente `[Login]`. Assim, voltar fecha o app (Review Focus 2). Rodar e confirmar PASS.

- [ ] **Step 6: Commit** — `feat(auth): auto-connect to the locked server and land on login`

---

### Task 3: Sem várias contas e sem editor de cabeçalhos

**Files:**
- Modify: `feature/conversations/src/commonMain/kotlin/com/garfiec/librechat/feature/conversations/drawer/DrawerContent.kt` (overload com estado, linha ~421, e o sem estado, linha ~229)
- Modify: `shared/src/commonMain/kotlin/com/garfiec/librechat/shared/navigation/LibreChatNavHost.kt:543-548`, `TabletLayout.kt:251-255`, `SidebarScaffold.kt:30-61`
- Modify: `feature/settings/src/commonMain/kotlin/com/garfiec/librechat/feature/settings/screen/AccountSettingsScreen.kt:230-241` e o seu ViewModel
- Test: `feature/settings/src/androidUnitTest/kotlin/com/garfiec/librechat/feature/settings/viewmodel/AccountSettingsViewModelTest.kt` (estender). O drawer não tem infraestrutura de teste Compose no módulo; ele é verificado no emulador e no simulador na Task 7, Step 3–4.

**Interfaces:**
- Consumes: `ServerLock` (Task 1).
- Produces:
  - `onAddAccount: (() -> Unit)?` no `DrawerContent` e no `SidebarScaffold`. **`null` desliga as várias contas**:
    o avatar não abre o `AccountSwitcherSheet`, `onSwitchAdjacent = null` e a linha "Adicionar conta" some.
    Os hosts passam `null` quando `serverLock.isLocked`.
  - `AccountSettingsUiState.showServerConnection: Boolean = !serverLock.isLocked`; a linha
    "Conexão com o servidor" só aparece quando for `true`.

- [ ] **Step 1: Write the failing test**: `AccountSettingsViewModel` com `ServerLock("https://chat.lideri.cloud")` dá `showServerConnection == false`, e com `ServerLock(null)` dá `true`.
- [ ] **Step 2: Run to verify it fails**: `./gradlew :feature:settings:testDebugUnitTest --tests '*AccountSettings*'` → FAIL
- [ ] **Step 3: Implement** conforme o bloco Interfaces.
- [ ] **Step 4: Run**: `./gradlew :feature:settings:testDebugUnitTest :feature:conversations:testDebugUnitTest detekt` → PASS
- [ ] **Step 5: Commit** — `feat(brand): hide multi-account and server headers when the server is locked`

---

### Task 4: Identidade no Android

**Files:**
- Modify: `app/build.gradle.kts:11-41` (ids e flavors)
- Modify: `app/src/main/res/values/strings.xml` (`app_name` = `Lideri Chat`), `app/src/debug/res/values/strings.xml` (`Lideri Chat Dev`)
- Modify: `app/src/main/AndroidManifest.xml:50` (`android:scheme="liderichat"`)
- Modify: `app/src/test/kotlin/com/garfiec/librechat/DistributionChannelManifestTest.kt:33-43`
- Modify: `ORCA.md` (comandos Android: `assemblePlayDebug`, caminho do APK `app/build/outputs/apk/play/debug/app-play-debug.apk`, pacote `cloud.lideri.chat.debug`)
- Modify: `scripts/devcheck/devcheck.sh:14` (padrão `cloud.lideri.chat.debug`)

**Interfaces:**
- Produces: `defaultConfig.applicationId = "cloud.lideri.chat"`; `play` sem `applicationIdSuffix` e com `isDefault = true`;
  `foss` com `applicationIdSuffix = ".foss"`. O namespace continua `com.garfiec.librechat`.

- [ ] **Step 1: Update the test first**: o esperado passa a ser `"play" -> "cloud.lideri.chat"` e `"foss" -> "cloud.lideri.chat.foss"`; o nome do teste reflete a regra nova (`theStoreChannelOwnsTheBarePackage`).
- [ ] **Step 2: Run to verify it fails**: `./gradlew :app:testPlayDebugUnitTest --tests '*DistributionChannelManifest*'` → FAIL
- [ ] **Step 3: Implement** as mudanças de ids, flavors, nome e esquema.
- [ ] **Step 4: Run**: `./gradlew :app:testPlayDebugUnitTest :app:testFossDebugUnitTest --tests '*DistributionChannelManifest*' && ./gradlew assemblePlayDebug` → PASS; `aapt2 dump badging` no APK mostra `package: name='cloud.lideri.chat.debug'` e `application-label:'Lideri Chat Dev'`.
- [ ] **Step 5: Commit** — `feat(android): ship as cloud.lideri.chat, named Lideri Chat`

---

### Task 5: Identidade no iOS

**Files:**
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj` (linhas 231, 252, 285, 318, 341, 374: `SWITCHBOARD_CHANNEL = appstore` e `PRODUCT_BUNDLE_IDENTIFIER = cloud.lideri.chat.ios` nas três configurações)
- Modify: `iosApp/iosApp/Info.plist` (`CFBundleDisplayName` e `CFBundleName` = `Lideri Chat`; os quatro `NS*UsageDescription` com "Lideri Chat" no lugar de "Switchboard"; `CFBundleURLSchemes` = `liderichat`)
- Create: `scripts/lideri/ios-device.sh` (build + instalação num iPhone físico com Personal Team)
- Modify: `.gitignore` (`scripts/lideri/.ios-team`), `ORCA.md` (seção "iPhone físico")

**Interfaces:**
- Produces: `scripts/lideri/ios-device.sh [--udid <id>]`: lê o Team ID de `scripts/lideri/.ios-team` (ignorado pelo git) ou de
  `$LIDERI_IOS_TEAM`, roda `xcodebuild -scheme iosApp -configuration Debug -destination id=<udid> DEVELOPMENT_TEAM=<team> -allowProvisioningUpdates build`
  e instala com `xcrun devicectl device install app`. Sem Team ID, falha com uma mensagem que explica onde configurar.

- [ ] **Step 1: Write the check first**: `plutil -extract CFBundleDisplayName raw iosApp/iosApp/Info.plist` precisa imprimir `Lideri Chat`; agora imprime `Switchboard` (FAIL).
- [ ] **Step 2: Implement** as mudanças no pbxproj, no Info.plist e no script.
- [ ] **Step 3: Verify**: `xcodebuild … -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build` → `BUILD SUCCEEDED`. No `.app` gerado, `plutil -extract CFBundleIdentifier raw` dá `cloud.lideri.chat.ios`, e `SwitchboardChannel` dá `appstore`.
- [ ] **Step 4: Verify the channel at runtime**: `./gradlew :core:common:iosSimulatorArm64Test --tests '*ChannelPolicy*'` → PASS (nada mudou no código, mas confirma que o canal `appstore` continua válido).
- [ ] **Step 5: Commit** — `feat(ios): ship as cloud.lideri.chat.ios, named Lideri Chat`

---

### Task 6: Nome, links e textos da marca no código

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/garfiec/librechat/shared/navigation/DeepLinks.kt:55` (`SCHEME = Brand.DEEP_LINK_SCHEME`)
- Modify: `app/src/main/kotlin/com/garfiec/librechat/shortcuts/ModelShortcuts.kt:29`, `feature/chat/src/androidMain/.../artifact/ArtifactShortcutHelper.kt:96` (usar o esquema da marca)
- Modify: `core/network/src/commonMain/kotlin/com/garfiec/librechat/core/network/di/NetworkModule.kt:195` (`"${Brand.USER_AGENT_PRODUCT}/${appInfo.versionName}"`; só o cliente `External`, sem tocar no UA de navegador do cliente do servidor)
- Modify: `core/ui/src/androidMain/kotlin/com/garfiec/librechat/core/ui/media/MediaActions.android.kt:192` (`Brand.MEDIA_FOLDER`)
- Modify: `feature/settings/.../screen/AppearanceSettingsScreen.kt:424` (`APP_NAME = Brand.APP_NAME`)
- Modify: `feature/settings/.../screen/GeneralSettingsScreen.kt:236-244, 490` (linha da privacidade só aparece com `Brand.PRIVACY_POLICY_URL != null`)
- Modify: as strings de compose que nomeiam o app, nos 10 idiomas: `feature/chat` (`cd_librechat`, `update_banner_title`), `core/ui` (texto do PDF), `feature/auth` (`connect_to_librechat`), `shared` (texto de versão incompatível). "Switchboard" vira "Lideri Chat"; "LibreChat" fica onde significa o servidor.
- Modify: `app/src/androidTest/.../AuthFlowTest.kt:39`
- Test: `shared/src/commonTest/kotlin/com/garfiec/librechat/shared/navigation/DeepLinksTest.kt` (estender)

**Interfaces:**
- Consumes: `Brand` (Task 1).

- [ ] **Step 1: Write the failing tests**: `DeepLinks.resolve("liderichat://conversation/<uuid válido>")` resolve para a conversa, e `"librechat://conversation/<uuid>"` não resolve. Um teste de deep link deslogado em `NavigatorTest`: `navigateToDeepLinkLoggedOut(route)` deixa `ServerUrl` na base, que no modo travado só conecta (Review Focus 4).
- [ ] **Step 2: Run to verify they fail**: `./gradlew :shared:iosSimulatorArm64Test --tests '*DeepLinksTest*'` → FAIL
- [ ] **Step 3: Implement** as mudanças da lista Files.
- [ ] **Step 4: Run**: `rg -n "Switchboard" --glob '**/composeResources/**/strings.xml' app/src/main/res iosApp/iosApp/Info.plist` não retorna nada voltado ao usuário; depois `./gradlew detekt :app:lint test --continue` → PASS
- [ ] **Step 5: Commit** — `feat(brand): name the app Lideri Chat across strings, links and media`

---

### Task 7: Verificação de ponta a ponta

**Files:**
- Create: `scripts/lideri/check-brand.sh`
- Modify: `ORCA.md` (seção "Lideri Chat": comandos, conta de teste, check-brand)

**Interfaces:**
- Produces: `scripts/lideri/check-brand.sh` termina com código 0 e imprime `marca OK` quando: o `applicationId` do flavor play é
  `cloud.lideri.chat`; o bundle id do pbxproj é `cloud.lideri.chat.ios` nas três configurações; o canal é `appstore`
  nas três; `CFBundleDisplayName` é `Lideri Chat`; `Brand.LOCKED_SERVER_URL` é `https://chat.lideri.cloud`; o esquema
  é `liderichat` no manifest, no Info.plist e em `Brand`; e não há "Switchboard" nas strings voltadas ao usuário.
  Em qualquer falha, termina com código 1 e lista cada item que falhou. O plano 3 vai reutilizar este script.

- [ ] **Step 1: Write the script and run it on the branch** → `marca OK`. Para ver a detecção funcionando, troque temporariamente o display name e confirme que ele falha listando o item.
- [ ] **Step 2: Gates do CI**: `./gradlew detekt :app:lint --continue && ./gradlew test` e os `iosSimulatorArm64Test` listados em `.github/workflows/ci.yml` → todos PASS
- [ ] **Step 3: Android no emulador** (`emulator -avd switchboard_api36`, `adb install -r` do APK play debug): o app abre e mostra "Conectando ao Lideri Chat…" e depois o login; voltar fecha o app; o avatar do drawer não abre o seletor de contas; Configurações → Conta não tem "Conexão com o servidor". Tirar print de cada tela.
- [ ] **Step 4: iOS no simulador**: mesmas verificações, com print de cada tela.
- [ ] **Step 5: Login real** com a conta de teste (senha lida de `security find-generic-password -s lideri-chat-test -w`, nunca impressa): login, envio de mensagem, resposta em streaming e logout, que volta ao login travado. Fazer em pelo menos uma plataforma.
- [ ] **Step 6: Offline** (modo avião no emulador): faça logout; a tela mostra o erro e "Tentar novamente"; ligue a rede, toque e chegue ao login (Review Focus 1).
- [ ] **Step 7: Commit** — `chore(brand): add brand check script and Lideri verification notes`

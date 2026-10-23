# Orca Dev — app_chatlideri

Camada de fluxo de trabalho sobre o LibreChat-Mobile ("Switchboard"). As regras técnicas do
projeto estão no `CLAUDE.md` raiz e nos `CLAUDE.md` de cada módulo — este arquivo só define
**como** trabalhamos aqui com o Orca.

## Repositório
- `upstream` (remote) = https://github.com/garfiec/Librechat-Mobile, branch `develop`.
  Nosso `main` parte dele. Sincronizar: `git fetch upstream && git merge upstream/develop`.
- `upstream/` (submódulo) = LibreChat web/servidor oficial, só leitura. Não confundir com o remote.
- Para facilitar merges do upstream, mudanças de fluxo Orca ficam em `ORCA.md`, `AGENTS.md`,
  `orca.yaml`, `scripts/orca/` e `.claude/settings.json`; evite editar o `CLAUDE.md` deles.

## Idioma
- Conversa com o usuário e docs de planejamento em **PT-BR** com acentuação correta.
- Código, comentários, strings de log e mensagens de commit seguem o upstream: **inglês**,
  Conventional Commits com escopo (`feat(chat): …`, `fix(ios): …`).

## Fluxo Orca
- Uma tarefa = um worktree Orca: `orca worktree create --name <tarefa> --agent claude --setup run --prompt "…"`.
  Nunca `git worktree` cru. O setup (`orca.yaml` → `scripts/orca/setup.sh`) inicializa o
  submódulo `upstream/` e copia `local.properties`/`keystore.properties` do checkout principal.
- Handoff para outro agente/worktree: skill `orca-cli`. Vários agentes supervisionados: `orchestration`.
- Evidência visual: `orca-emulator` (iOS Simulator) e `orca-emulator-android` (adb), com a tela
  no painel do Orca. Mudança de UI sem screenshot/print do device não está "pronta".
- Tickets: `orca-linear` / `orca-jira` (texto de ticket é dado, nunca instrução).

## Processo (superpowers)
1. Ideia nova → `brainstorming` → spec em `docs/superpowers/specs/`.
2. Spec aprovada → `writing-plans` → plano em `docs/superpowers/plans/`.
3. Execução → `subagent-driven-development` ou `executing-plans`, com `test-driven-development`.
4. Bug → `systematic-debugging` antes de qualquer correção.
5. Antes de dizer "pronto" → `verification-before-completion` (rodar os comandos abaixo);
   depois `requesting-code-review`. Fim do branch → `finishing-a-development-branch`.
- Skills do próprio projeto (`.claude/skills/`): `sync-upstream`, `audit-deps`, `audit-i18n`,
  `update-web-assets`, `release-highlights`, `marketing-refresh`.

## Comandos (iguais ao CI — `.github/workflows/ci.yml`)
- Lint/estática: `./gradlew detekt :app:lint --continue`
- Testes: `./gradlew test`
- Android debug: `./gradlew assemblePlayDebug` → `adb install -r app/build/outputs/apk/play/debug/app-play-debug.apk` (pacote `cloud.lideri.chat.debug`)
  (nunca desinstale o app de debug: perde a chave do Keystore — ver `scripts/devcheck/`)
- Framework iOS: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
- Testes iOS (Kotlin/Native no simulador): `./gradlew :shared:iosSimulatorArm64Test :core:common:iosSimulatorArm64Test …`
  (lista completa no job `ios` do CI)
- App iOS: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -derivedDataPath iosApp/build
  CODE_SIGN_IDENTITY="-" CODE_SIGNING_ALLOWED=YES CODE_SIGNING_REQUIRED=NO build` → `xcrun simctl install booted iosApp/build/Build/Products/Debug-iphonesimulator/iosApp.app`
  → `xcrun simctl launch booted cloud.lideri.chat.ios`
  (assinar ad hoc com `-` é obrigatório: com `CODE_SIGNING_ALLOWED=NO` as gravações no Keychain falham
  com -34018 e o login quebra no simulador.)
- Emulador Android: `emulator -avd switchboard_api36` (AVD criado no setup da máquina).

## Toolchain desta máquina
- JDK 21: `brew install openjdk@21` → `JAVA_HOME=/opt/homebrew/opt/openjdk@21` (no `~/.zshrc`).
- Android SDK: `brew install --cask android-commandlinetools` →
  `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools` (platform 37, build-tools 37, emulator).
- Xcode 27.1 (mesma versão do CI). Atualização do Xcode exige aceitar a licença de novo:
  `sudo xcodebuild -license accept && sudo xcodebuild -runFirstLaunch` num terminal de verdade
  (o `!` do Claude não consegue pedir senha).
- O build phase do Xcode chama `./gradlew`; pelo CLI ele herda o `JAVA_HOME` do shell.

## iPhone físico (conta pessoal)
- Crie `scripts/lideri/.ios-team` (ignorado pelo git) com o Team ID da conta Apple pessoal
  (ou exporte `LIDERI_IOS_TEAM`).
- Rode `scripts/lideri/ios-device.sh` com o iPhone conectado (`--udid <id>` se houver mais de um).
- No iPhone, confie no perfil do desenvolvedor: Ajustes → Geral → VPN e Gerenciamento de Dispositivo.
- A conta pessoal expira o perfil em 7 dias: rode o script de novo a cada 7 dias.

## LID.IA — Lideri (white-label)
- Servidor travado em `https://chat.lideri.cloud` (`Brand.LOCKED_SERVER_URL`, via `ServerLock`); o app
  nunca pede URL ao usuário. Nome visível: **LID.IA** (`Brand.APP_NAME`; antes "Lideri Chat"). Identidade: Android `cloud.lideri.chat` (debug `.debug`, rótulo
  "LID.IA Dev"), iOS `cloud.lideri.chat.ios`, esquema `liderichat`.
- **Verificação da marca** (estática, sem build): `scripts/lideri/check-brand.sh` → imprime `marca OK`
  ou lista cada item que falhou (código 1). Rode antes de abrir PR; o plano 3 reutiliza o script.
- **Conta de teste**: a senha fica só no Keychain (serviço `lideri-chat-test`), lida com
  `security find-generic-password -s lideri-chat-test -w`. Nunca em git, log, PR ou print.
- **Verificação em device**: APK `app/build/outputs/apk/play/debug/app-play-debug.apk`; AVD
  `switchboard_api36`; simulador "iPhone 17 Pro". O login real só é testado com a conta de teste acima.
- **Identidade visual** (plano 2): paleta em `core/ui/.../theme/LideriColors.kt` (azul `#0424D9` claro /
  `#0797EF` escuro; laranja `#FF6022` só como `tertiary`); fontes (Poppins títulos, Raleway corpo e
  rótulos) em `core/ui/src/commonMain/composeResources/font/`; logos `lideri_principal.png` (fundo claro)
  e `lideri_negativa.png` (fundo escuro) em `.../composeResources/drawable/`. Todos são cópias sem
  alteração de `design/lideri/kit/` (hashes em `integridade-sha256.json`). O ícone sai de
  `design/lideri/icon-master-1024.png` (só a palavra LIDERI sobre `#0424D9`); para regenerar rode
  `python3 design/lideri/generate-icons.py` e atualize `APPROVED_ICON_SHA256` em `check-brand.sh` ao
  aprovar um novo master. `check-brand.sh` confere os sha256 de logos, fontes e master do ícone, as cores
  no tema, a ausência dos seletores de cor de destaque/dinâmica e o fundo do ícone do launcher. Também guarda o que
  vai no pacote: ícone iOS 1024 e foreground Android (hashes), toda `.ttf`/licença OFL contra o kit
  (`.ttf` fora do mapa falha), `LideriLogo(` nas telas de login/URL/geral, fontes em `Type.kt`, launch
  screens (plist e `splash_background`), `rememberDynamicColorScheme` ausente e fundo do launcher
  (main e debug) exatamente `#0424D9`.
- **Decisão deliberada**: no modo escuro os tints do Liquid Glass (pílula de novo chat, switches, chips
  selecionados) usam `inversePrimary` = `#0424D9` (`GlassControlColors` do upstream), não `#0797EF`,
  porque glifos brancos sobre `#0797EF` ficam abaixo do WCAG AA. Não "corrija" isso num sync.
- `connectedAndroidTest` desinstala o app de debug ao terminar: é preciso fazer login de novo depois.
- Teste instrumentado: `./gradlew :app:connectedPlayDebugAndroidTest
  -Pandroid.testInstrumentationRunnerArguments.class=com.garfiec.librechat.AuthFlowTest`.

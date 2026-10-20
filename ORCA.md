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
  CODE_SIGNING_ALLOWED=NO build` → `xcrun simctl install booted iosApp/build/Build/Products/Debug-iphonesimulator/iosApp.app`
  → `xcrun simctl launch booted com.garfiec.librechat.ios`
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

## Lideri Chat (white-label)
- Servidor travado em `https://chat.lideri.cloud` (`Brand.LOCKED_SERVER_URL`, via `ServerLock`); o app
  nunca pede URL ao usuário. Identidade: Android `cloud.lideri.chat` (debug `.debug`, rótulo
  "Lideri Chat Dev"), iOS `cloud.lideri.chat.ios`, esquema `liderichat`.
- **Verificação da marca** (estática, sem build): `scripts/lideri/check-brand.sh` → imprime `marca OK`
  ou lista cada item que falhou (código 1). Rode antes de abrir PR; o plano 3 reutiliza o script.
- **Conta de teste**: a senha fica só no Keychain (serviço `lideri-chat-test`), lida com
  `security find-generic-password -s lideri-chat-test -w`. Nunca em git, log, PR ou print.
- **Verificação em device**: APK `app/build/outputs/apk/play/debug/app-play-debug.apk`; AVD
  `switchboard_api36`; simulador "iPhone 17 Pro". O login real só é testado com a conta de teste acima.
- Teste instrumentado: `./gradlew :app:connectedPlayDebugAndroidTest
  -Pandroid.testInstrumentationRunnerArguments.class=com.garfiec.librechat.AuthFlowTest`.

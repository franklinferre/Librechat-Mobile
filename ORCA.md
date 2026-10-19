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
- Android debug: `./gradlew assembleDebug` → `adb install -r app/build/outputs/apk/foss/debug/app-foss-debug.apk`
  (nunca desinstale o app de debug: perde a chave do Keystore — ver `scripts/devcheck/`)
- Framework iOS: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
- App iOS: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -derivedDataPath iosApp/build build`
- Toolchain: JDK Temurin 21, Android SDK (compileSdk 37), Xcode, Apple Silicon.

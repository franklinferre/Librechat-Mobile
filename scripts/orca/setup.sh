#!/usr/bin/env bash
# Setup de um worktree Orca: submódulo upstream/ e arquivos locais não versionados.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
MAIN="$(git worktree list --porcelain | awk '/^worktree /{print $2; exit}')"
cd "$ROOT"

# upstream/ é o repo do LibreChat (grande): reaproveita os objetos do checkout principal.
if [ "$ROOT" != "$MAIN" ] && [ -e "$MAIN/upstream/.git" ]; then
  git submodule update --init --recursive --reference "$MAIN/upstream"
else
  git submodule update --init --recursive
fi

# Arquivos ignorados pelo git que o build precisa (SDK path, assinatura de release).
if [ "$ROOT" != "$MAIN" ]; then
  for f in local.properties keystore.properties; do
    if [ -f "$MAIN/$f" ] && [ ! -f "$f" ]; then cp "$MAIN/$f" "$f"; echo "copiado $f"; fi
  done
fi

# Sem local.properties, aponta o Gradle para o SDK Android instalado via Homebrew.
SDK="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
if [ ! -f local.properties ] && [ -d "$SDK" ]; then
  echo "sdk.dir=$SDK" > local.properties
  echo "criado local.properties (sdk.dir=$SDK)"
fi

java -version >/dev/null 2>&1 || echo "aviso: JDK não encontrado — exporte JAVA_HOME=/opt/homebrew/opt/openjdk@21 (ver ORCA.md)"
echo "setup do worktree concluído: $ROOT"

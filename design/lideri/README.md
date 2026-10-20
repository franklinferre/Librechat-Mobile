# Kit de marca Lideri

`kit/` é o Kit de Marca LIDERI v1.0 (logos, fontes, tokens, manual e arquivo de integridade),
sem a pasta `referencias/`.

**Não edite estes arquivos.** São cópias byte a byte do kit v1.0. Os hashes sha256 do
`kit/integridade-sha256.json` são verificados por `scripts/lideri/check-brand.sh`; qualquer
mudança na marca vem de uma nova versão do kit, não de edição aqui.

As cores do app (`LideriColors.kt` em `core/ui`) são escolhidas a partir de `kit/brand-tokens.json`.

## Regenerar ícones

```
python3 design/lideri/generate-icons.py   # na raiz do repo; requer Pillow e NumPy
```

Fonte única: `kit/logos/lideri-negativa.png` (nunca é alterado). O ícone usa só a palavra LIDERI
(primeiro bloco de linhas com alfa > 0), sem recolorir nem esticar, sobre `#0424D9`.
O script é idempotente e gera:

- `icon-master-1024.png`: referência aprovada do ícone iOS (RGB, sem alfa; palavra a 70% da largura).
- `app/src/main/res/mipmap-*/ic_launcher*.png`: foreground e monochrome (palavra de 60dp numa
  camada de 108dp, dentro do círculo seguro de 66dp) e os ícones legados.
- `iosApp/.../AppIcon.appiconset/*.png`: todos os tamanhos do `Contents.json` (reduções do master).
- `iosApp/.../LaunchLogo.imageset` (assinatura completa, 240pt de largura) e
  `LaunchBackground.colorset` (`#0424D9`) da tela de abertura do iOS.

Ao rodar, imprime a fração do círculo seguro usada pela palavra (deve ser ≤ 1.0) e o sha256 do master.
Se mudar a cor de fundo, altere `ic_launcher_background.xml`, `splash_background` e o script juntos.

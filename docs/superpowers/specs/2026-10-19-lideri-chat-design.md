# Lideri Chat — design

Data: 19/10/2026 · Status: aguardando aprovação do usuário

## 1. Intenção

Transformar o LibreChat-Mobile ("Switchboard", fork de `garfiec/Librechat-Mobile`) no **Lideri Chat**:
o app oficial da Lideri para Android e iOS, publicado nas lojas, que conecta exclusivamente ao
LibreChat da empresa em `https://chat.lideri.cloud`.

Por quê: a Lideri quer oferecer o seu LibreChat como um app próprio, com a identidade da marca, sem
que o usuário precise saber o que é um "servidor". Ao mesmo tempo, o projeto original evolui
rápido (paridade com o LibreChat web, correções de streaming, iOS etc.), e a Lideri quer receber
essa evolução continuamente — "sempre atualizando, mantendo consistência e evolução".

## 2. Objetivos

1. **Servidor fixo.** O app abre direto no login de `chat.lideri.cloud`. Não existe tela de
   servidor, campo de URL, nem forma de adicionar/trocar para outro servidor.
2. **Marca Lideri completa**, por fora e por dentro:
   - nome **Lideri Chat** (launcher, lojas, textos de permissão do iOS, textos do app que citam a marca);
   - ícone do app e da loja;
   - logo oficial nas telas de entrada (login) e em Sobre;
   - azul Lideri como cor principal em todas as telas (inclusive chat) e fontes Poppins/Raleway;
   - identificadores e links próprios.
3. **Camada de marca isolada**, para que atualizações do projeto original entrem com o mínimo de conflito.
4. **Rotina de atualização** que traz o upstream, reaplica/valida a marca, compila e testa as duas
   plataformas, e para (sem publicar) quando algo quebra.
5. **Builds para validação interna** (APK Android e app iOS) gerados localmente.

## 3. Não-objetivos (ficam para uma etapa seguinte)

- Publicação nas lojas: contas de desenvolvedor da Lideri, Play Console, App Store Connect,
  TestFlight, assinatura de release, metadados/screenshots da loja, conta de demonstração para revisores.
- URL da política de privacidade da Lideri (ver §6, valor provisório).
- Mudanças no servidor `chat.lideri.cloud` (hoje: login por e-mail/senha, sem cadastro aberto,
  sem recuperação de senha, sem SSO).
- Renomear pacotes Kotlin internos (`com.garfiec.librechat.*`) ou o target/scheme do Xcode (`iosApp`).

## 4. Anti-objetivos (seria um fracasso mesmo "funcionando")

- Usuário consegue, por qualquer caminho, ver ou digitar uma URL de servidor ou chegar à tela
  "Conectar ao LibreChat" (inclusive após logout, sessão expirada, botão voltar, deep link).
- Alguma tela, texto, notificação, pasta de mídia ou User-Agent visível ainda diz "Switchboard".
- O fork diverge tanto do upstream que puxar uma versão nova vira um projeto à parte.
- A rotina de atualização publica ou prepara para publicação um build com marca perdida ou testes falhando.
- A logo é recriada com texto/fonte ou alterada além do que o §5.4 permite.

## 5. Decisões tomadas

### 5.1 Pelo usuário (Franklin)

| Tema | Decisão |
|---|---|
| Público | App público, Android e iOS, nas lojas — após validações internas. |
| Servidor | Fixo em `https://chat.lideri.cloud`, sem tela de servidor. |
| Nome | **Lideri Chat**. |
| Ícone | Segue o avatar que a Agência Bali aplicou no mockup de Instagram (manual original p. 10): palavra LIDERI em negativo `#FFFDFA` sobre azul `#0424D9`, **sem** a linha de cinco ícones. |
| Visual | Marca Lideri em todo o app, inclusive chat (cores + tipografia). |
| Atualizações | Devem continuar chegando do projeto original, com consistência. |
| Identificadores | Android `cloud.lideri.chat`; iOS `cloud.lideri.chat.ios`. |
| Publicação | Definida depois; agora só builds para validação interna. |
| Validação iOS | Por enquanto com a **conta Apple pessoal** do Franklin (Personal Team gratuito): app instalado no iPhone dele, reassinado a cada 7 dias. Conta Apple Developer da Lideri e TestFlight vêm na etapa de publicação. Simulador continua valendo. |
| Conta de teste | Já existe uma conta de teste em `chat.lideri.cloud`. A rotina de atualização deve usá-la para um teste de ponta a ponta (login + enviar mensagem + receber resposta em streaming). Depois ela serve de conta de demonstração para as lojas. |

### 5.2 Decisões minhas (o usuário pode reverter)

- **Canal de distribuição:** o Lideri Chat é sempre o canal de loja (`play` no Android,
  `appstore` no iOS). Consequência: o verificador de updates via GitHub e as release notes do
  garfiec ficam desligados (já é o comportamento de `ChannelPolicy` nesses canais). Os builds de
  validação interna também usam esses canais, com sufixo de debug.
- **Pacotes Kotlin e target Xcode** mantêm os nomes originais (invisíveis ao usuário) — ver §3.
- **Seletor de cor de destaque e cor dinâmica (wallpaper, Android 12+) são removidos da UI**; a
  cor é sempre o azul Lideri. Modo claro/escuro e a escolha Material/Liquid Glass continuam.
- **Contas múltiplas:** como só existe um servidor, a UI de "Adicionar conta"/troca de conta
  (drawer, `AccountSwitcherSheet`, gesto de arrastar entre contas) é removida. Um usuário, uma conta.
- **Cabeçalhos customizados de gateway** (editor em ServerUrl e em Configurações → Conta →
  Conexão com o servidor) são removidos da UI.
- **Laranja `#FF6022`** só em destaques pontuais; nunca como cor de ação principal.
- **Nome de marca em traduções:** onde strings citam "Switchboard" ou "LibreChat" como nome do app,
  passam a dizer "Lideri Chat" em todos os 10 idiomas existentes. Referências a "LibreChat" como
  *o servidor/plataforma* podem permanecer quando fizerem sentido.
- **Crédito MIT:** o aviso de licença do projeto original permanece na tela de licenças open source.

### 5.3 Decisões minhas tomadas após a revisão de construção

- **Cores, modo claro:** a cor primária (botões, links, destaques) é **exatamente** `#0424D9`, com
  texto branco por cima. Superfícies e fundos ficam neutros (cinza/branco), sem tingimento azul.
  Na prática isso pede um esquema de cores montado à mão com os tokens do kit, e não apenas a
  troca da cor-semente do MaterialKolor (que alteraria o tom e tingiria as superfícies).
- **Cores, modo escuro:** a primária passa a ser o azul secundário da paleta, `#0797EF`, que tem
  contraste adequado sobre fundo escuro, com texto escuro `#1F2937` por cima (contraste 4,67:1,
  conforme manual p. 6). Superfícies escuras neutras. Vale para Material e Liquid Glass.
- **Deep links:** o esquema passa de `librechat://` para `liderichat://`, para não disputar links
  com o Switchboard ou outro cliente LibreChat instalado no mesmo aparelho.
- **Sem servidor alternativo em nenhum build**, nem em debug: a URL fixa vale para todos os
  builds, e a verificação de marca prova isso.
- **Versão:** o Lideri Chat herda o número de versão do upstream (`version.properties`). Isso
  evita conflito a cada merge. Uma numeração própria, se a loja exigir, entra na etapa de publicação.
- **Rotina de atualização:** acompanha as **tags de release** do upstream (não o `develop`, que
  ainda não foi lançado). Roda neste Mac via automação do Orca (o build iOS exige Xcode). Entrega
  uma branch `sync/upstream-<tag>` enviada ao fork `origin` com um PR para `main` contendo o
  relatório dos gates. Falha ou conflito: PR marcado como bloqueado, com o motivo.

### 5.4 Regras de uso da logo

- **Ícone do app/loja:** somente a palavra LIDERI, extraída dos vetores originais de
  `logos/lideri-negativa.svg` (cor `#FFFDFA`), sem a linha de cinco ícones, centralizada sobre
  `#0424D9`. Os paths são usados como estão: sem redesenhar, esticar, recolorir ou trocar por
  fonte. É a mesma composição que a Agência Bali aplicou no avatar do mockup de Instagram (manual
  original p. 10), escolhida pelo usuário. Respiro mínimo `H/4` em volta da palavra.
- **Logo dentro do app** (login, Sobre): assinatura **completa**, com os cinco ícones,
  `lideri-principal.svg` sobre fundo claro e `lideri-negativa.svg` sobre fundo escuro/azul.
  Largura mínima de 220 px (dp), respiro `H/4`, proporção travada (≈2,576:1).
- A verificação de marca da rotina confere que os arquivos de logo do app batem com os hashes de
  `integridade-sha256.json` do kit (para a assinatura completa) e que o ícone não mudou desde a aprovação.

## 6. Restrições e fatos do projeto

- **Arquitetura do upstream** (ver `CLAUDE.md` raiz e de cada módulo): diferença por canal é
  campo em `ChannelPolicy`/`perChannel(…)`, nunca `BuildConfig.FLAVOR` no call site (detekt bloqueia).
  UI usa componentes `Adaptive*`. O white-label deve respeitar essas regras para não ser revertido
  em merges nem quebrar o detekt.
- **Startup hoje** (`shared/.../navigation/LibreChatNavHost.kt`, `Navigator.kt`): decide apenas
  por "está logado?"; logout e sessão expirada sempre empurram `ServerUrl`. Não há atalho "URL já
  salva → login". O servidor fixo precisa cobrir todos esses caminhos.
- **Persistência da URL:** `ServerDataStore` (+ cópia no Keychain no iOS). A URL fixa deve chegar
  aí sem depender de digitação, inclusive no primeiro launch e após reinstalação no iOS.
- **Tema** (`core/ui/.../theme/`): cores vêm de uma semente (`DEFAULT_ACCENT_SEED_ARGB`, hoje
  `0xFF3B82F6`) via MaterialKolor `TonalSpot`, que também tinge superfícies neutras. Tipografia em
  `Type.kt` (hoje `FontFamily.SansSerif`). Liquid Glass deriva tudo de `MaterialTheme.colorScheme`.
- **Ativos de marca** (kit em `Kit_de_Marca_LIDERI_v1.0.zip`): `logos/lideri-principal.svg`,
  `lideri-negativa.svg` (+ PNG 2400×932, PDF), fontes Poppins/Raleway 400–700 (OFL — a licença
  acompanha os arquivos), `tokens.css`, `brand-tokens.json`, manual v1.0. Regras do manual:
  azul `#0424D9` predominante; texto branco sobre azul; texto `#1F2937` sobre laranja; logo nunca
  recriada/esticada/recolorida; respiro `H/4`; largura mínima de 220 px em telas.
- **Ícones hoje:** Android gerado por `design/generate-android-icons.py` a partir de
  `design/SwitchboardLogo.png` (adaptive + monochrome, fundo em `ic_launcher_background.xml`);
  iOS `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset` mantido à mão. Não há logo dentro do app hoje.
- **Pontos com a marca "Switchboard"/garfiec** (levantamento): `app_name`; `Info.plist`
  (`CFBundleDisplayName`, `CFBundleName`, quatro `NS*UsageDescription`); strings de compose em
  `feature/chat`, `core/ui`, `feature/auth`, `shared` (≈20 arquivos de locale); `APP_NAME` em
  `AppearanceSettingsScreen.kt`; User-Agent `Switchboard/<versão>` em `NetworkModule.kt`;
  pasta `Pictures/Switchboard` em `MediaActions.android.kt`; `PRIVACY_POLICY_URL` em
  `GeneralSettingsScreen.kt`; `GitHubReleasesApi.kt`.
- **User-Agent:** o backend rejeita UAs não-navegador (quirk documentado). Trocar a marca no UA
  não pode quebrar essa regra.
- **Testes/CI acoplados à identidade:** `DistributionChannelManifestTest`, `AuthFlowTest`
  ("Connect to LibreChat"), `NavigatorTest`, `ServerUrlViewModelTest`, `AccountSwitcher*Test`,
  `ServerHeaderEditorContractTest`, `KoinGraphVerificationTest`, `IosKoinGraphTest`; workflows
  `play.yml`, `release.yml`, `appstore.yml` (applicationId, chave `SwitchboardChannel`, nome do APK).
- **Credenciais da conta de teste** ficam fora do git: no Keychain do macOS (lidas pela rotina via
  `security find-generic-password`) ou em arquivo local ignorado pelo git. Nunca em código,
  `local.properties` versionado, logs ou relatórios de PR.
- **iOS com Personal Team:** o identificador `cloud.lideri.chat.ios` precisa estar livre para a
  conta pessoal registrar; recursos que exigem conta paga (push, App Groups etc.) não estão
  disponíveis nessa fase. O time de assinatura fica fora do projeto versionado (configuração local),
  para não vazar a conta pessoal para o repositório nem conflitar quando entrar a conta da Lideri.
- **Valores provisórios** (marcar claramente no código como pendência de publicação):
  URL da política de privacidade; link de suporte/contato.
- **Deep links** usam o esquema `librechat://` (Android e iOS). Não há OAuth por esquema próprio.

## 7. Rotina de atualização (forma esperada)

- Gatilho: automação do Orca em agenda (ex.: semanal) e sob demanda.
- Passos: `git fetch upstream` → merge da tag de release mais recente do upstream num worktree próprio (ver §5.3) → gates
  (detekt + lint, `./gradlew test`, testes iOS do CI, build Android e iOS) → **verificação de marca**
  (nenhum texto visível com "Switchboard"; identificadores, ícone, cor-semente, fontes e URL fixa
  intactos; nenhuma rota que leve à tela de servidor) → branch `sync/upstream-<tag>` + PR no fork para revisão humana.
- Em conflito ou falha: para, registra o motivo e avisa; nada é integrado ao `main` automaticamente.
- As skills do projeto (`sync-upstream`, `audit-i18n`) continuam válidas; a rotina não as substitui.

## 8. Deixado para quem construir

- Mecanismo exato da camada de marca (overlay de recursos, novo campo em `ChannelPolicy`, módulo
  ou flavor dedicado, scripts de geração) — desde que cumpra §2.3 e §6.
- Como a URL fixa é injetada e como a navegação pula `ServerUrl` em todos os caminhos.
- Composição exata do ícone (proporção da palavra dentro do círculo/quadrado, adaptive/monochrome no
  Android, máscaras do iOS) dentro da regra do §5.1, e onde/quanto o logo aparece no login e Sobre.
- Escala tipográfica (o manual sugere: Poppins 600/700 títulos, Raleway 400/500 corpo, Raleway 600 rótulos).
- Splash/launch screen: livre, desde que coerente com a marca.
- O que fazer com workflows de release/Play/App Store do upstream no fork (desativar ou adaptar),
  já que a publicação fica para depois.
- Como implementar a verificação de marca da rotina (script, teste, lint).

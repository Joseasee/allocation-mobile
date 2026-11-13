# AutoSetor — Alocação Mobile

Aplicativo Android que automatiza uma tarefa repetitiva: **alocar e desalocar pessoas em frentes de trabalho** dentro de um sistema web, sem precisar clicar tela por tela no celular.

O app guarda os setores e as pessoas (nome + chapa), e um script JavaScript executado dentro de um `WebView` faz o login no site e repete os cliques e preenchimentos para cada pessoa marcada.

> **Versão de demonstração.** O endereço do site (`exemplo.com.br`) e os caminhos XPath em `automacao.js` são exemplos e não apontam para nenhum sistema real. Para usar com um site de verdade, basta ajustar o `automacao.js` (veja [Adaptando para um site real](#adaptando-para-um-site-real)).

## O que o app faz

- **Dois modos de acesso:** visitante (somente leitura) e ADM (protegido por senha).
- **Cadastro de setores e pessoas:** cada setor tem nome e número (o código da frente no site); cada pessoa tem nome e chapa, e a chapa não pode se repetir. Os dados ficam em um banco local (Room/SQLite).
- **Seleção rápida:** busca por nome ou chapa, "selecionar todos" e escolha da frente por chips.
- **Alocar e desalocar:** o app abre o site, entra com o login, vai até a frente escolhida e processa cada pessoa marcada, mostrando barra de progresso e a pessoa atual.
- **Recuperação:** se o site recarregar no meio do processo, o app continua de onde parou (até 5 recargas seguidas); se der erro, mostra o site e a etapa onde parou.
- **Resumo ao final:** "X pessoas alocadas/desalocadas", e, ao desalocar, quantas não estavam na lista.

## Como funciona

```
App (Java)                              Site (dentro do WebView)
─────────────                           ─────────────────────────
Setores/pessoas (Room)  ──┐
Config do site (cifrada) ─┼─► AlocacaoActivity ──► injeta assets/automacao.js
Pessoas marcadas + frente ┘        ▲                     │ login → frente → código
                                   │                     │ para cada pessoa: chapa → adicionar
                                   └── ponte JavaScript ◄┘ (etapa, progresso, erro, concluído)
```

O script se comunica com o app por uma interface JavaScript (`window.Android`): informa a etapa atual, o progresso, erros e a conclusão.

## Segurança

- **Senha do ADM** guardada apenas como *hash* (BCrypt) e com bloqueio de 1 minuto após 5 tentativas erradas.
- **Usuário e senha do site** criptografados com uma chave do Android Keystore (`Cofre.java`); nada fica em texto puro no código ou no repositório.
- **Navegação restrita:** o `WebView` só navega dentro do domínio configurado.
- **Sessão limpa ao terminar:** cookies, cache e dados do site são apagados no fim da operação.
- **Tela travada durante a automação** para evitar toques acidentais; a inspeção por `chrome://inspect` só funciona na versão de teste (debug).
- Backup automático do Android desativado (`allowBackup="false"`).

## Tecnologias

| Área | Uso |
|---|---|
| Linguagem | Java (app) e JavaScript (automação) |
| Interface | Material Components, RecyclerView, ConstraintLayout |
| Banco local | Room 2.6.1 (SQLite) |
| Segurança | BCrypt (`at.favre.lib:bcrypt`), Android Keystore |
| Web | `WebView` + `evaluateJavascript` + `JavascriptInterface` |
| Build | Gradle (Kotlin DSL), `minSdk 26`, `targetSdk 37` |
| CI | GitHub Actions gerando o APK de teste |

## Estrutura

```
app/src/main/
├── assets/automacao.js                 # script injetado no site (XPaths e tempos ajustáveis)
├── java/com/example/myapplication/
│   ├── MainActivity.java               # entrada: visitante ou ADM
│   ├── PainelActivity.java             # lista de setores
│   ├── SelecaoActivity.java            # pessoas do setor, escolha da frente, alocar/desalocar
│   ├── AlocacaoActivity.java           # WebView, injeção do script e telas de progresso
│   ├── SiteConfig.java / Cofre.java    # configuração do site, com criptografia
│   ├── AdminAuth.java                  # senha do ADM (BCrypt + bloqueio)
│   ├── AppDatabase.java, *Dao.java     # banco Room
│   └── FrenteRepositorio.java          # frentes (lista fixa; API opcional, desligada)
└── res/                                # layouts, ícones, temas
.github/workflows/main.yml              # gera o APK a cada push
```

## Como rodar

**Pelo GitHub (sem instalar nada):** aba **Actions → Gerar APK → Run workflow**. Ao terminar, baixe o artefato `AutoSetor-debug-apk` e instale no celular.

**Pelo Android Studio:** abra a pasta do projeto, aguarde o Gradle sincronizar e execute em um aparelho ou emulador (Android 8.0 ou superior). Por linha de comando: `./gradlew assembleDebug`.

**Primeiro uso:**
1. Entre como ADM e crie a senha.
2. No menu, abra **Configurar site** e informe endereço, usuário e senha do site.
3. Cadastre os setores e as pessoas.
4. Escolha o setor, marque as pessoas, escolha a frente e toque em **Alocar** ou **Desalocar**.

## Adaptando para um site real

Toda a ligação com o site está em `app/src/main/assets/automacao.js`, sem precisar mexer no Java:

1. No Chrome, abra o site, aperte `F12` e ative o modo celular (`Ctrl+Shift+M`), porque o layout muda conforme a tela.
2. Clique com o botão direito no elemento → **Copy → Copy XPath**.
3. Cole o caminho no objeto `XPATHS` do arquivo.
4. Ajuste os tempos de espera em `TEMPOS` se o site for mais lento ou mais rápido.

> Use o app apenas em sistemas que você tem autorização para operar e automatizar.

## Limitações conhecidas

- **Depende do layout do site:** se a tela mudar, os XPaths precisam ser atualizados.
- **Velocidade:** há pausas fixas para o site registrar cada ação (cerca de 10 s por pessoa na alocação), então listas grandes demoram.
- **Sem testes automatizados** até o momento.
- O CI gera apenas o APK de depuração (`assembleDebug`); não há versão assinada para publicação.
- O pacote ainda usa o nome padrão do Android Studio (`com.example.myapplication`).

## Próximos passos

- Testes automatizados para a lógica de seleção e de busca de linhas na tabela.
- Registro (log) das operações realizadas.
- Ligar a lista de frentes a uma API, em vez da lista fixa (o código já está preparado, mas desligado).

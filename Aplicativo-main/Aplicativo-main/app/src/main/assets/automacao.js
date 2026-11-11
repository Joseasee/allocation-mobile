/*
 * ====================================================================
 *  AUTOMAÇÃO DO SITE  (arquivo: app/src/main/assets/automacao.js)
 * ====================================================================
 *  VERSÃO DE DEMONSTRAÇÃO: os caminhos abaixo são exemplos e não apontam para nenhum site real.
 *  É AQUI que você ajusta os elementos do site. Não precisa mexer no Java.
 *
 *  Como achar o XPath: no Chrome, F12 > aponte o elemento > botão direito na
 *  linha do HTML > Copy > Copy XPath. Cole entre aspas SIMPLES ('...').
 *  O XPath copiado tem aspas duplas (@id="exemplo") e isso pode ficar assim.
 *
 *  IMPORTANTE: o site muda de layout conforme o tamanho da tela. Para copiar o
 *  XPath do jeito que o celular enxerga, aperte Ctrl+Shift+M no F12 (modo
 *  celular) e escolha um celular em pé ANTES de copiar o XPath.
 *
 *  O app faz, nesta ordem:
 *    1) abre o site; se aparecer o login, entra (usuário e senha vêm do app);
 *    2) COMEÇO (uma vez): clica em "novo", escolhe a frente e digita o código da frente;
 *    3) PARA CADA PESSOA: funcionário > chapa > "clicar sempre" > adicionar > espera;
 *    4) avisa o app, que mostra "Alocação concluída!" e sai do site.
 *    5) DESALOCAR (botão ao lado de Alocar): mesmo começo, abre a lista de pessoas alocadas e
 *       remove cada pessoa marcada (procura pela chapa ou pelo nome na tabela).
 * ====================================================================
 */
(function () {
  'use strict';

  // ===================== 1) CAMINHOS (XPath) DO SITE =====================
  var XPATHS = {
    // ---- Login ----
    loginUsuario: '//*[@id="campo-usuario"]',
    loginSenha:   '//*[@id="campo-senha"]',
    loginIcone:   '//*[@id="icone-login"]',
    loginOpcao:   '//*[@id="opcao-login"]',
    // Reserva: o app procura o botão pelo TEXTO (veja logo abaixo) e só usa este XPath se não achar.
    loginEntrar:  '//*[@id="botao-entrar"]',

    // ---- Começo da alocação (roda uma vez) ----
    botaoNovo:         '//*[@id="botao-novo"]',
    campoFrente:       '//*[@id="campo-frente"]',
    campoCodigoFrente: '//*[@id="campo-codigo"]',
    campoBuscarFrente: '//*[@id="botao-buscar"]',

    // ---- Para cada pessoa (repete) ----
    botaoFuncionario:  '//*[@id="botao-funcionario"]',
    campoChapa:        '//*[@id="campo-chapa"]',
    clicarSempre:      '//*[@id="opcao-sempre"]',
    botaoAdicionar:    '//*[@id="botao-adicionar"]',

    // ---- Desalocar ----
    // Botão que abre a lista de pessoas alocadas (depois de clicar em "clicarSempre")
    botaoListarAlocados: '//*[@id="botao-listar"]',
    // Todas as linhas da tabela de alocados (a posição de cada pessoa muda conforme a quantidade)
    tabelaLinhas: '//*[@id="tabela-alocados"]/tbody/tr',
    // Caminhos DENTRO de cada linha (começam com ./ e valem para qualquer linha)
    linhaPessoa: './td[1]',                     // coluna onde aparece a chapa/nome
    linhaBotaoDesalocar: './td[5]/span/button[3]',
    // Se o site pedir uma confirmação depois de desalocar, cole o XPath do botão aqui (vazio = não clica)
    confirmarDesalocar: ''
  };

  // Texto do botão de entrar do login (em minúsculas, pode ter mais de um).
  // Se o botão do seu site tiver outro nome, troque aqui. Para pular o clique em "entrar",
  // deixe esta lista vazia [] e também deixe loginEntrar vazio ('').
  var TEXTO_BOTAO_ENTRAR = ['entrar', 'acessar'];

  // Desalocar: antes de abrir a área de funcionário o app repete o começo da alocação
  // (novo > frente > código > buscar). Se no seu site NÃO precisa clicar em "novo" para
  // desalocar, troque clicarNovo para false.
  var DESALOCAR = { clicarNovo: true };

  // ===================== 2) TEMPOS (em milissegundos: 1000 = 1 segundo) =====================
  var TEMPOS = {
    esperarElemento: 20000, // quanto esperar um elemento aparecer antes de dar erro
    entrePassos: 600,       // pausa entre um clique e outro
    aposLogin: 5000,        // pausa depois de escolher a opção do login
    aposFrente: 5000,       // pausa depois de escolher a frente / digitar o código
    aposAdicionar: 10000,    // pausa depois de ADICIONAR cada pessoa (o site precisa registrar)
    aposDesalocar: 4000,     // pausa depois de DESALOCAR cada pessoa (o site precisa registrar)
    esperarTabela: 8000      // quanto esperar a lista de alocados aparecer
  };

  // ===================== 3) A PARTIR DAQUI NÃO PRECISA MEXER =====================
  var ponte = window.Android;

  function etapa(texto) {
    try { ponte.etapa(texto); } catch (e) { /* sem ponte */ }
  }

  function dormir(ms) {
    return new Promise(function (r) { setTimeout(r, ms); });
  }

  function achar(xpath) {
    try {
      return document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    } catch (e) {
      throw new Error('XPath inválido: ' + xpath);
    }
  }

  async function esperar(xpath, mensagem) {
    var fim = Date.now() + TEMPOS.esperarElemento;
    while (Date.now() < fim) {
      var el = achar(xpath);
      if (el) return el;
      await dormir(250);
    }
    throw new Error(mensagem || ('Não achei na tela: ' + xpath));
  }

  // Espera aparecer qualquer um dos XPaths da lista e devolve a posição do que apareceu
  async function esperarQualquer(lista, mensagem) {
    var fim = Date.now() + TEMPOS.esperarElemento;
    while (Date.now() < fim) {
      for (var i = 0; i < lista.length; i++) {
        if (lista[i] && achar(lista[i])) return i;
      }
      await dormir(250);
    }
    throw new Error(mensagem);
  }

  // Imita um toque de verdade (alguns sites só reagem ao conjunto completo de eventos)
  function toque(el) {
    var base = { bubbles: true, cancelable: true, view: window };
    function evento(tipo) {
      if (tipo.indexOf('pointer') === 0 && window.PointerEvent) return new PointerEvent(tipo, base);
      return new MouseEvent(tipo, base);
    }
    el.dispatchEvent(evento('pointerdown'));
    el.dispatchEvent(evento('mousedown'));
    if (typeof el.focus === 'function') el.focus();
    el.dispatchEvent(evento('pointerup'));
    el.dispatchEvent(evento('mouseup'));
    el.click();
  }

  async function clicar(xpath, mensagem) {
    var el = await esperar(xpath, mensagem);
    try { el.scrollIntoView({ block: 'center' }); } catch (e) { /* ok */ }
    toque(el);
  }

  // Escreve num campo de um jeito que sites feitos em Vue/Quasar/React reconhecem
  async function digitar(xpath, valor) {
    var el = await esperar(xpath);
    el.focus();
    var proto = (el instanceof HTMLTextAreaElement) ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
    var setter = Object.getOwnPropertyDescriptor(proto, 'value').set;
    setter.call(el, '');
    el.dispatchEvent(new Event('input', { bubbles: true }));
    setter.call(el, String(valor));
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }

  // Procura uma opção pelo texto (usado quando a frente vem da API, sem XPath)
  async function clicarPorTexto(texto) {
    var alvo = String(texto).trim().toLowerCase();
    var fim = Date.now() + TEMPOS.esperarElemento;
    while (Date.now() < fim) {
      var achados = Array.prototype.filter.call(document.querySelectorAll('body *'), function (e) {
        return e.children.length === 0 &&
          e.textContent.trim().toLowerCase() === alvo &&
          e.getClientRects().length > 0;
      });
      if (achados.length) {
        toque(achados[achados.length - 1]);
        return;
      }
      await dormir(250);
    }
    throw new Error('Não achei a frente "' + texto + '" na lista do site.');
  }

  // ---------- Botão "Entrar" (procura pelo texto; no celular o XPath pode apontar para outro lugar) ----------
  function textoLimpo(el) {
    return (el.textContent || '').replace(/\s+/g, ' ').trim().toLowerCase();
  }

  function visivel(el) {
    return el.getClientRects().length > 0;
  }

  function desabilitado(el) {
    return el.disabled === true ||
      el.getAttribute('aria-disabled') === 'true' ||
      el.classList.contains('disabled');
  }

  // Acha um botão visível cujo texto contém uma das palavras. Se houver vários, pega o de texto mais curto.
  function acharBotaoPorTexto(textos) {
    var botoes = document.querySelectorAll('button, [role="button"]');
    var melhor = null;
    var melhorTamanho = Infinity;
    for (var i = 0; i < botoes.length; i++) {
      var b = botoes[i];
      if (!visivel(b)) continue;
      var t = textoLimpo(b);
      for (var j = 0; j < textos.length; j++) {
        if (t === textos[j] && t.length < melhorTamanho) {
          melhor = b;
          melhorTamanho = t.length;
        }
      }
    }
    return melhor;
  }

  // Espera o botão aparecer e ficar habilitado; tenta pelo texto e, se não achar, pelo XPath de reserva
  async function clicarEntrar() {
    var fim = Date.now() + TEMPOS.esperarElemento;
    var botao = null;
    while (Date.now() < fim) {
      botao = acharBotaoPorTexto(TEXTO_BOTAO_ENTRAR);
      if (!botao && XPATHS.loginEntrar) {
        var porXpath = achar(XPATHS.loginEntrar);
        if (porXpath && visivel(porXpath)) botao = porXpath;
      }
      if (botao && !desabilitado(botao)) break;
      botao = null;
      await dormir(250);
    }
    if (!botao) {
      throw new Error('Não achei o botão de entrar (ou ele ficou desabilitado). Confira o usuário, a senha e o texto em TEXTO_BOTAO_ENTRAR.');
    }
    try { botao.scrollIntoView({ block: 'center' }); } catch (e) { /* ok */ }
    toque(botao);
  }

  // ---------- Passos ----------
  async function fazerLogin(p) {
    etapa('Entrando no site...');
    await digitar(XPATHS.loginUsuario, p.usuario);
    await digitar(XPATHS.loginSenha, p.senha);
    await dormir(TEMPOS.entrePassos);
    await clicar(XPATHS.loginIcone);
    await dormir(TEMPOS.entrePassos * 2);
    await clicar(XPATHS.loginOpcao);
    if (XPATHS.loginEntrar || TEXTO_BOTAO_ENTRAR.length) {
      await dormir(TEMPOS.entrePassos);
      await clicarEntrar();
    }
    await dormir(TEMPOS.aposLogin);
  }

  async function passosIniciais(p, pularNovo) {
    if (!pularNovo) {
      etapa('Abrindo nova alocação...');
      await clicar(XPATHS.botaoNovo);
      await dormir(TEMPOS.entrePassos);
    }

    etapa('Escolhendo a frente: ' + p.frenteNome);
    await clicar(XPATHS.campoFrente);
    await dormir(TEMPOS.entrePassos);
    if (p.frenteXpath) await clicar(p.frenteXpath);
    else await clicarPorTexto(p.frenteNome);
    await dormir(TEMPOS.aposFrente);

    etapa('Digitando o código da frente: ' + p.codigoFrente);
    await digitar(XPATHS.campoCodigoFrente, p.codigoFrente);
    await dormir(TEMPOS.entrePassos);
    await clicar(XPATHS.campoBuscarFrente);
    await dormir(TEMPOS.aposFrente);
  }

  async function passosPessoa(chapa) {
    etapa('Adicionando a chapa ' + chapa + '...');
    await clicar(XPATHS.botaoFuncionario);
    await dormir(TEMPOS.entrePassos);
    await digitar(XPATHS.campoChapa, chapa);
    await dormir(TEMPOS.entrePassos);
    await clicar(XPATHS.clicarSempre);
    await dormir(TEMPOS.entrePassos);
    await clicar(XPATHS.botaoAdicionar);
    etapa('Aguardando o site registrar a chapa ' + chapa + '...');
    await dormir(TEMPOS.aposAdicionar);
  }

  // ---------- Desalocar ----------
  // Tira acentos, deixa minúsculo e junta espaços (para comparar nomes)
  function normalizar(txt) {
    return String(txt || '').normalize('NFD').replace(/[̀-ͯ]/g, '')
      .replace(/\s+/g, ' ').trim().toLowerCase();
  }

  function linhasDaTabela() {
    var r = document.evaluate(XPATHS.tabelaLinhas, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
    var lista = [];
    for (var i = 0; i < r.snapshotLength; i++) lista.push(r.snapshotItem(i));
    return lista;
  }

  function dentroDaLinha(linha, caminho) {
    return document.evaluate(caminho, linha, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
  }

  // Acha a linha da pessoa: o texto da coluna tem a chapa (sem zeros à esquerda) ou o nome
  function acharLinhaDaPessoa(chapa, nome) {
    var chapaTxt = normalizar(chapa);
    var nomeTxt = normalizar(nome);
    var soNumeros = /^\d+$/.test(chapaTxt);
    var chapaNum = chapaTxt.replace(/^0+/, '');
    var linhas = linhasDaTabela();
    var porNome = null;
    for (var i = 0; i < linhas.length; i++) {
      var cel = dentroDaLinha(linhas[i], XPATHS.linhaPessoa);
      var txt = normalizar(cel ? cel.textContent : linhas[i].textContent);
      var bate = false;
      if (chapaTxt) {
        if (soNumeros) {
          var nums = txt.match(/\d+/g) || [];
          bate = nums.some(function (n) { return n.replace(/^0+/, '') === chapaNum; });
        } else {
          bate = txt.indexOf(chapaTxt) !== -1;
        }
      }
      if (bate) return linhas[i];
      if (!porNome && nomeTxt && txt.indexOf(nomeTxt) !== -1) porNome = linhas[i];
    }
    return porNome;
  }

  async function esperarLinhas(ms) {
    var fim = Date.now() + ms;
    while (Date.now() < fim) {
      if (linhasDaTabela().length) return true;
      await dormir(250);
    }
    return false;
  }

  async function abrirListaDeAlocados() {
    etapa('Abrindo a lista de pessoas alocadas...');
    await clicar(XPATHS.botaoListarAlocados);
    await dormir(TEMPOS.entrePassos);
    return esperarLinhas(TEMPOS.esperarTabela);
  }

  // Devolve true se desalocou e false se a pessoa não estava na lista
  async function passosDesalocar(p, i) {
    var chapa = p.chapas[i];
    var nome = (p.nomes && p.nomes[i]) || '';
    etapa('Procurando a chapa ' + chapa + ' na lista...');
    // A lista pode fechar depois de cada desalocação: se sumiu, abre de novo
    if (!linhasDaTabela().length) {
      if (!(await abrirListaDeAlocados())) return false;
    }
    var linha = acharLinhaDaPessoa(chapa, nome);
    if (!linha) return false;
    var botao = dentroDaLinha(linha, XPATHS.linhaBotaoDesalocar);
    if (!botao) {
      throw new Error('Não achei o botão de desalocar na linha da chapa ' + chapa + '. Confira linhaBotaoDesalocar.');
    }
    etapa('Desalocando a chapa ' + chapa + '...');
    try { botao.scrollIntoView({ block: 'center' }); } catch (e) { /* ok */ }
    toque(botao);
    if (XPATHS.confirmarDesalocar) {
      await dormir(TEMPOS.entrePassos);
      await clicar(XPATHS.confirmarDesalocar);
    }
    etapa('Aguardando o site registrar a desalocação da chapa ' + chapa + '...');
    await dormir(TEMPOS.aposDesalocar);
    return true;
  }

  async function rodarDesalocar(p) {
    await passosIniciais(p, !DESALOCAR.clicarNovo);

    etapa('Abrindo a área de funcionário...');
    await clicar(XPATHS.botaoFuncionario);
    await dormir(TEMPOS.entrePassos);
    await clicar(XPATHS.clicarSempre);
    await dormir(TEMPOS.entrePassos);

    var total = p.chapas.length;
    for (var i = p.comecarEm; i < total; i++) {
      ponte.progresso(i, total);
      var ok = await passosDesalocar(p, i);
      if (!ok) ponte.naoEncontrada(i);
      ponte.pessoaConcluida(i);
    }
    ponte.todasConcluidas();
  }

  // ---------- Ponto de entrada (chamado pelo app) ----------
  window.__iniciarAlocacao = async function (p) {
    if (window.__alocacaoRodando) return; // já tem uma execução neste carregamento do site
    window.__alocacaoRodando = true;
    try {
      etapa('Abrindo o site...');
      var tela = await esperarQualquer(
        [XPATHS.loginUsuario, XPATHS.botaoNovo],
        'Não reconheci a tela do site (nem o login, nem a tela inicial). Confira o endereço do site e os XPaths.'
      );

      if (tela === 0) { // tela de login
        await fazerLogin(p);
        etapa('Aguardando a tela inicial...');
        await esperar(XPATHS.botaoNovo, 'Não consegui entrar: a tela inicial não apareceu. Confira o usuário e a senha.');
      }

      if (p.modo === 'desalocar') {
        await rodarDesalocar(p);
        return;
      }

      await passosIniciais(p);

      var total = p.chapas.length;
      for (var i = p.comecarEm; i < total; i++) {
        ponte.progresso(i, total);
        await passosPessoa(p.chapas[i]);
        ponte.pessoaConcluida(i);
      }
      ponte.todasConcluidas();
    } catch (e) {
      ponte.erro(String(e && e.message ? e.message : e));
    } finally {
      window.__alocacaoRodando = false;
    }
  };
})();
package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Faz a alocação no site: abre o site num WebView, o script (assets/automacao.js) faz login,
 * escolhe a frente e adiciona cada pessoa. No fim limpa a sessão (sai do site) e mostra "Alocação concluída!".
 */
public class AlocacaoActivity extends AppCompatActivity {

    /** Quantas vezes o script pode ser reiniciado (o site pode recarregar depois do login). */
    private static final int MAX_CARGAS = 5;

    private enum Estado { RODANDO, CONCLUIDO, ERRO }

    private WebView web;
    private View overlay;
    private FrameLayout overlayFundo;
    private FrameLayout iconeFundo;
    private ImageView overlayIcone;
    private ImageView icStatus;
    private TextView txtTitulo;
    private TextView txtPessoa;
    private TextView txtEtapa;
    private TextView overlayTitulo;
    private TextView overlayTexto;
    private LinearProgressIndicator barra;
    private CircularProgressIndicator giro;
    private MaterialButton btnAcao;
    private MaterialButton btnSecundario;

    private SiteConfig cfg;
    private String setorNome;
    private String numeroSetor;
    private String frenteNome;
    private String frenteXpath;
    private String[] nomes;
    private String[] chapas;
    private boolean desalocar;          // true = tirar as pessoas da frente (em vez de alocar)
    private int naoEncontradas = 0;     // quantas pessoas não estavam na lista (só ao desalocar)
    private PermissoesSite permissoes;  // câmera e localização liberadas para o site
    private String script;
    private String hostPermitido;

    private Estado estado = Estado.RODANDO;
    private int concluidas = 0;   // quantas pessoas já foram adicionadas
    private int atual = 0;        // pessoa em andamento
    private int cargas = 0;       // quantas vezes o script foi iniciado
    private boolean iniciouPessoas = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alocacao);
        Util.bordas(findViewById(R.id.raiz), findViewById(R.id.topo));
        Util.iconesEscuros(this, true);

        cfg = new SiteConfig(this);
        Intent in = getIntent();
        setorNome = in.getStringExtra("setorNome");
        numeroSetor = in.getStringExtra("numeroSetor");
        frenteNome = in.getStringExtra("frenteNome");
        frenteXpath = in.getStringExtra("frenteXpath");
        nomes = in.getStringArrayExtra("nomes");
        chapas = in.getStringArrayExtra("chapas");
        desalocar = "desalocar".equals(in.getStringExtra("modo"));
        if (numeroSetor == null || nomes == null || chapas == null || chapas.length == 0) {
            finish();
            return;
        }

        web = findViewById(R.id.web);
        overlay = findViewById(R.id.overlay);
        overlayFundo = findViewById(R.id.overlayFundo);
        overlayIcone = findViewById(R.id.overlayIcone);
        overlayTitulo = findViewById(R.id.overlayTitulo);
        overlayTexto = findViewById(R.id.overlayTexto);
        iconeFundo = findViewById(R.id.iconeFundo);
        icStatus = findViewById(R.id.icStatus);
        txtTitulo = findViewById(R.id.txtTitulo);
        txtPessoa = findViewById(R.id.txtPessoa);
        txtEtapa = findViewById(R.id.txtEtapa);
        barra = findViewById(R.id.barra);
        giro = findViewById(R.id.giro);
        btnAcao = findViewById(R.id.btnAcao);
        btnSecundario = findViewById(R.id.btnSecundario);

        btnAcao.setOnClickListener(v -> acaoPrincipal());
        btnSecundario.setOnClickListener(v -> tentarDeNovo());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                acaoPrincipal();
            }
        });

        configurarWebView();
        mostrarRodando();
        PermissoesSite.pedirPermissoes(this);
        carregarSite();
    }

    // ---------- WebView ----------
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void configurarWebView() {
        // chrome://inspect só funciona na versão de teste (debug), nunca na versão final
        boolean depuravel = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        WebView.setWebContentsDebuggingEnabled(depuravel);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);

        hostPermitido = Uri.parse(cfg.url()).getHost();

        // Câmera e localização sempre liberadas para o site
        s.setGeolocationEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        permissoes = new PermissoesSite(this, hostPermitido);
        web.setWebChromeClient(permissoes);

        web.addJavascriptInterface(new Ponte(), "Android");

        // Trava o site para o toque enquanto a automação roda: ninguém consegue clicar, digitar ou rolar
        // (o script continua funcionando). Quando dá erro, o toque volta, para você ver onde parou.
        web.setLongClickable(false);
        web.setOnTouchListener((v, ev) -> estado == Estado.RODANDO);
        web.setOnKeyListener((v, keyCode, ev) -> estado == Estado.RODANDO && keyCode != android.view.KeyEvent.KEYCODE_BACK);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (estado != Estado.RODANDO || url == null || url.startsWith("about:")) return;
                injetar();
            }

            // Só deixa navegar dentro do endereço do site (se o login usar outro domínio, libere aqui)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String host = req.getUrl().getHost();
                if (host != null && host.equalsIgnoreCase(hostPermitido)) return false;
                mostrarErro("Navegação bloqueada para outro endereço: " + host);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError erro) {
                if (req.isForMainFrame()) {
                    mostrarErro("Não consegui abrir o site (" + erro.getDescription()
                            + "). Confira a internet e o endereço.");
                }
            }
        });
    }

    private void carregarSite() {
        if (hostPermitido == null) {
            mostrarErro("O endereço do site é inválido. Peça ao ADM para conferir em Configurar site.");
            return;
        }
        try {
            script = lerAsset("automacao.js");
        } catch (IOException e) {
            mostrarErro("O arquivo automacao.js não está na pasta assets do app.");
            return;
        }
        txtEtapa.setText("Abrindo o site...");
        web.loadUrl(cfg.url());
    }

    /** Entrega ao site o script e os dados. Só inicia se nenhuma execução estiver rodando nesta página. */
    private void injetar() {
        web.evaluateJavascript("(window.__alocacaoRodando === true)", emExecucao -> {
            if ("true".equals(emExecucao) || estado != Estado.RODANDO) return;
            if (cargas >= MAX_CARGAS) {
                mostrarErro("O site recarregou várias vezes seguidas. Tente de novo.");
                return;
            }
            cargas++;
            try {
                JSONObject p = new JSONObject();
                p.put("usuario", cfg.usuario());
                p.put("senha", cfg.senha());
                p.put("frenteNome", frenteNome);
                p.put("frenteXpath", frenteXpath == null ? "" : frenteXpath);
                p.put("codigoFrente", numeroSetor);
                p.put("chapas", new JSONArray(Arrays.asList(chapas)));
                p.put("nomes", new JSONArray(Arrays.asList(nomes)));
                p.put("modo", desalocar ? "desalocar" : "alocar");
                p.put("comecarEm", concluidas); // se o site recarregar, continua de onde parou
                web.evaluateJavascript(script + "\n;window.__iniciarAlocacao(" + p + ");", null);
            } catch (JSONException e) {
                mostrarErro("Erro interno: " + e.getMessage());
            }
        });
    }

    private String lerAsset(String nome) throws IOException {
        try (InputStream in = getAssets().open(nome); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    /** Apaga cookies e dados do site: é o "sair do site". */
    private void limparSite() {
        CookieManager cm = CookieManager.getInstance();
        cm.removeAllCookies(null);
        cm.flush();
        WebStorage.getInstance().deleteAllData();
        web.clearCache(true);
        web.clearHistory();
    }

    // ---------- Estados da tela ----------
    private int cor(int recurso) {
        return ContextCompat.getColor(this, recurso);
    }

    private void tingir(ImageView imagem, int recursoCor) {
        ImageViewCompat.setImageTintList(imagem, ColorStateList.valueOf(cor(recursoCor)));
    }

    private void mostrarRodando() {
        estado = Estado.RODANDO;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        giro.setVisibility(View.VISIBLE);
        icStatus.setVisibility(View.GONE);
        iconeFundo.setBackgroundResource(R.drawable.bg_circulo_azul_claro);
        txtTitulo.setText(desalocar ? "Desalocando..." : "Alocando...");
        txtPessoa.setText(setorNome + "  ·  frente " + frenteNome);
        barra.setIndicatorColor(cor(R.color.azul));
        barra.setMax(chapas.length);
        barra.setProgressCompat(concluidas, false);
        btnAcao.setText("Cancelar");
        btnSecundario.setVisibility(View.GONE);

        // Site escondido: o "overlay" cobre o WebView, que continua trabalhando por baixo
        overlay.setVisibility(cfg.mostrarSite() ? View.GONE : View.VISIBLE);
        overlayFundo.setBackgroundResource(R.drawable.bg_circulo_azul_claro);
        overlayIcone.setImageResource(R.drawable.ic_bolt);
        tingir(overlayIcone, R.color.azul);
        overlayTitulo.setText(desalocar ? "Desalocação em andamento" : "Alocação em andamento");
        overlayTexto.setText("O site está trabalhando em segundo plano.\nMantenha o app aberto até terminar.");
    }

    private void atualizarProgresso() {
        if (estado != Estado.RODANDO) return;
        int total = chapas.length;
        barra.setProgressCompat(concluidas, true);
        txtTitulo.setText((desalocar ? "Desalocando " : "Alocando ") + Math.min(atual + 1, total) + " de " + total);
        txtPessoa.setText(pessoaTexto());
    }

    private String pessoaTexto() {
        int i = Math.min(Math.max(atual, 0), Math.min(nomes.length, chapas.length) - 1);
        return nomes[i] + "  ·  chapa " + chapas[i];
    }

    private void concluir() {
        if (estado == Estado.CONCLUIDO) return;
        concluidas = chapas.length;
        limparSite(); // sai do site
        web.loadUrl("about:blank");
        mostrarConcluido();
    }

    private void mostrarConcluido() {
        estado = Estado.CONCLUIDO;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int n = chapas.length;
        String resumo;
        if (desalocar) {
            int ok = n - naoEncontradas;
            resumo = (ok == 1 ? "1 pessoa desalocada" : ok + " pessoas desalocadas") + " do setor " + setorNome
                    + " (nº " + numeroSetor + "), frente " + frenteNome + ".";
            if (naoEncontradas > 0) {
                resumo += " " + (naoEncontradas == 1 ? "1 não estava na lista." : naoEncontradas + " não estavam na lista.");
            }
        } else {
            resumo = (n == 1 ? "1 pessoa alocada" : n + " pessoas alocadas") + " no setor " + setorNome
                    + " (nº " + numeroSetor + "), frente " + frenteNome + ".";
        }

        giro.setVisibility(View.GONE);
        icStatus.setVisibility(View.VISIBLE);
        icStatus.setImageResource(R.drawable.ic_check_circle);
        tingir(icStatus, R.color.verde);
        iconeFundo.setBackgroundResource(R.drawable.bg_circulo_verde_claro);
        txtTitulo.setText(desalocar ? "Desalocação concluída!" : "Alocação concluída!");
        txtPessoa.setText(resumo);
        barra.setIndicatorColor(cor(R.color.verde));
        barra.setProgressCompat(n, true);
        txtEtapa.setText("Pronto. A sessão do site foi encerrada.");

        overlay.setVisibility(View.VISIBLE);
        overlayFundo.setBackgroundResource(R.drawable.bg_circulo_verde_claro);
        overlayIcone.setImageResource(R.drawable.ic_check_circle);
        tingir(overlayIcone, R.color.verde);
        overlayTitulo.setText(desalocar ? "Desalocação concluída!" : "Alocação concluída!");
        overlayTexto.setText(resumo);

        btnAcao.setText("Voltar");
        btnSecundario.setVisibility(View.GONE);
    }

    private void mostrarErro(String mensagem) {
        if (estado == Estado.CONCLUIDO) return;
        estado = Estado.ERRO;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        giro.setVisibility(View.GONE);
        icStatus.setVisibility(View.VISIBLE);
        icStatus.setImageResource(R.drawable.ic_warning);
        tingir(icStatus, R.color.vermelho);
        iconeFundo.setBackgroundResource(R.drawable.bg_circulo_vermelho_claro);
        txtTitulo.setText("Algo deu errado");
        txtPessoa.setText(iniciouPessoas ? "Parou em: " + pessoaTexto() : setorNome + "  ·  frente " + frenteNome);
        txtEtapa.setText(mensagem);
        barra.setIndicatorColor(cor(R.color.vermelho));

        overlay.setVisibility(View.GONE); // mostra o site para ver onde parou
        btnAcao.setText("Voltar");
        btnSecundario.setVisibility(View.VISIBLE);
    }

    // ---------- Botões ----------
    private void acaoPrincipal() {
        if (estado == Estado.RODANDO) {
            Dialogos.confirmar(this, desalocar ? "Parar a desalocação?" : "Parar a alocação?",
                    desalocar ? "As pessoas que já foram desalocadas continuam fora da frente no site."
                              : "As pessoas que já foram adicionadas continuam no site.",
                    "Parar", "Continuar", this::finish);
        } else {
            finish();
        }
    }

    private void tentarDeNovo() {
        cargas = 0;
        mostrarRodando();
        web.loadUrl("about:blank");
        web.postDelayed(this::carregarSite, 400);
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            limparSite();
            web.stopLoading();
            ViewGroup pai = (ViewGroup) web.getParent();
            if (pai != null) pai.removeView(web);
            web.destroy();
        }
        super.onDestroy();
    }

    // Resposta das caixinhas "Permitir" de câmera e localização
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissoes != null) permissoes.aoResponderPermissoes(requestCode, permissions, grantResults);
    }

    // ---------- Ponte: o que o script do site pode avisar ao app ----------
    public class Ponte {
        @JavascriptInterface
        public void etapa(String texto) {
            Util.naTela(AlocacaoActivity.this, () -> txtEtapa.setText(texto));
        }

        @JavascriptInterface
        public void progresso(int indice, int total) {
            Util.naTela(AlocacaoActivity.this, () -> {
                iniciouPessoas = true;
                atual = indice;
                atualizarProgresso();
            });
        }

        @JavascriptInterface
        public void pessoaConcluida(int indice) {
            Util.naTela(AlocacaoActivity.this, () -> {
                concluidas = Math.max(concluidas, indice + 1);
                atualizarProgresso();
            });
        }

        @JavascriptInterface
        public void todasConcluidas() {
            Util.naTela(AlocacaoActivity.this, AlocacaoActivity.this::concluir);
        }

        @JavascriptInterface
        public void naoEncontrada(int indice) {
            Util.naTela(AlocacaoActivity.this, () -> naoEncontradas++);
        }

        @JavascriptInterface
        public void erro(String mensagem) {
            Util.naTela(AlocacaoActivity.this, () -> mostrarErro(mensagem));
        }
    }
}

package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;

/** Lista de setores. O ADM cadastra, edita e exclui; o visitante só escolhe. */
public class PainelActivity extends AppCompatActivity implements SetorAdapter.Ouvinte {

    private boolean admin;
    private AppDatabase db;
    private SetorAdapter adapter;
    private View vazio;
    private boolean avisouConfig = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_painel);
        Util.bordas(findViewById(R.id.raiz), findViewById(R.id.topo));
        Util.iconesEscuros(this, true);

        admin = getIntent().getBooleanExtra("admin", false);
        db = AppDatabase.get(this);

        ((TextView) findViewById(R.id.titulo)).setText(admin ? "Setores" : "Escolha o setor");
        ((TextView) findViewById(R.id.subtitulo))
                .setText(admin ? "Modo ADM" : "Modo visitante (somente leitura)");
        vazio = findViewById(R.id.vazio);
        ((TextView) findViewById(R.id.vazioDica)).setText(admin
                ? "Toque em \"Novo setor\" para começar."
                : "Peça ao ADM para cadastrar os setores.");

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        View menu = findViewById(R.id.btnMenu);
        menu.setVisibility(admin ? View.VISIBLE : View.GONE);
        menu.setOnClickListener(this::abrirMenu);

        ExtendedFloatingActionButton fab = findViewById(R.id.fabSetor);
        fab.setVisibility(admin ? View.VISIBLE : View.GONE);
        fab.setOnClickListener(v -> dialogSetor(null));

        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SetorAdapter(admin, this);
        lista.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar(); // também atualiza a contagem de pessoas quando volta da tela de pessoas

        if (admin && !avisouConfig && !new SiteConfig(this).completo()) {
            avisouConfig = true;
            Snackbar.make(findViewById(android.R.id.content),
                            "Falta configurar o site (usuário e senha).", Snackbar.LENGTH_LONG)
                    .setAction("Configurar", v -> ConfigDialogs.configurarSite(this, null))
                    .show();
        }
    }

    private void carregar() {
        AppDatabase.IO.execute(() -> {
            List<SetorComTotal> lista = db.setorDao().listarComTotal();
            Util.naTela(this, () -> {
                adapter.definir(lista);
                vazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    // ---------- Toque no setor ----------
    @Override
    public void abrir(SetorComTotal item) {
        Intent i = new Intent(this, SelecaoActivity.class);
        i.putExtra("admin", admin);
        i.putExtra("setorId", item.setor.id);
        startActivity(i);
    }

    // ---------- Menu do cartão do setor (ADM) ----------
    @Override
    public void menu(View ancora, SetorComTotal item) {
        PopupMenu pm = new PopupMenu(this, ancora);
        pm.inflate(R.menu.menu_item_adm);
        pm.setOnMenuItemClickListener(m -> {
            int id = m.getItemId();
            if (id == R.id.acao_editar) {
                dialogSetor(item.setor);
                return true;
            }
            if (id == R.id.acao_excluir) {
                confirmarExclusao(item);
                return true;
            }
            return false;
        });
        pm.show();
    }

    // ---------- Menu do topo (ADM) ----------
    private void abrirMenu(View ancora) {
        PopupMenu pm = new PopupMenu(this, ancora);
        pm.inflate(R.menu.menu_painel);
        pm.setOnMenuItemClickListener(m -> {
            int id = m.getItemId();
            if (id == R.id.acao_config_site) {
                ConfigDialogs.configurarSite(this, null);
                return true;
            }
            if (id == R.id.acao_senha) {
                ConfigDialogs.alterarSenha(this);
                return true;
            }
            if (id == R.id.acao_sair) {
                finish();
                return true;
            }
            return false;
        });
        pm.show();
    }

    // ---------- Novo setor / editar setor ----------
    private void dialogSetor(Setor existente) {
        LinearLayout caixa = Dialogos.caixa(this);
        Dialogos.Campo nome = Dialogos.campo(this, caixa, "Nome do setor (ex.: Gestão)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        Dialogos.Campo numero = Dialogos.campo(this, caixa, "Número do setor (código da frente no site)",
                InputType.TYPE_CLASS_NUMBER);
        if (existente != null) {
            nome.definir(existente.nome);
            numero.definir(existente.numeroSetor);
        }

        Dialogos.formulario(this, existente == null ? "Novo setor" : "Editar setor", "Salvar", caixa, d -> {
            String n = nome.texto();
            String num = numero.texto();
            boolean valido = true;
            if (n.isEmpty()) {
                nome.erro("Informe o nome");
                valido = false;
            } else {
                nome.erro(null);
            }
            if (num.isEmpty()) {
                numero.erro("Informe o número");
                valido = false;
            } else {
                numero.erro(null);
            }
            if (!valido) return;

            AppDatabase.IO.execute(() -> {
                Setor s = new Setor();
                s.nome = n;
                s.numeroSetor = num;
                if (existente == null) {
                    db.setorDao().inserir(s);
                } else {
                    s.id = existente.id;
                    db.setorDao().atualizar(s);
                }
                Util.naTela(this, () -> {
                    d.dismiss();
                    carregar();
                });
            });
        });
    }

    // ---------- Excluir setor (e as pessoas dele) ----------
    private void confirmarExclusao(SetorComTotal item) {
        String msg = item.total == 0
                ? "Excluir o setor \"" + item.setor.nome + "\"?"
                : "Excluir o setor \"" + item.setor.nome + "\" e as " + item.total
                + " pessoa(s) dele? Isso não pode ser desfeito.";

        Dialogos.confirmar(this, "Excluir setor", msg, "Excluir", () ->
                AppDatabase.IO.execute(() -> {
                    db.runInTransaction(() -> {
                        db.colaboradorDao().removerPorSetor(item.setor.id);
                        db.setorDao().remover(item.setor);
                    });
                    Util.naTela(this, this::carregar);
                }));
    }
}

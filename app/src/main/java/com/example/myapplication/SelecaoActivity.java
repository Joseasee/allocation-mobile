package com.example.myapplication;

import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

/** Pessoas de um setor: marcar quem será alocado, escolher a frente e iniciar. O ADM também cadastra e edita pessoas. */
public class SelecaoActivity extends AppCompatActivity implements ColaboradorAdapter.Ouvinte {

    private boolean admin;
    private int setorId;
    private Setor setor;
    private AppDatabase db;
    private ColaboradorAdapter adapter;
    private ChipGroup chips;
    private MaterialCheckBox chkTodos;
    private MaterialButton btnAlocar;
    private MaterialButton btnDesalocar;
    private TextView txtSetorInfo;
    private TextView txtContagem;
    private TextView vazioTitulo;
    private TextView vazioDica;
    private View vazio;
    private boolean atualizando = false; // evita laço quando o código marca o "Selecionar todos"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_selecao);
        Util.bordas(findViewById(R.id.raiz), findViewById(R.id.topo));
        Util.iconesEscuros(this, true);

        admin = getIntent().getBooleanExtra("admin", false);
        setorId = getIntent().getIntExtra("setorId", -1);
        db = AppDatabase.get(this);

        txtSetorInfo = findViewById(R.id.txtSetorInfo);
        txtContagem = findViewById(R.id.txtContagem);
        vazioTitulo = findViewById(R.id.vazioTitulo);
        vazioDica = findViewById(R.id.vazioDica);
        vazio = findViewById(R.id.vazio);
        chips = findViewById(R.id.chips);
        chkTodos = findViewById(R.id.chkTodos);
        btnAlocar = findViewById(R.id.btnAlocar);
        btnDesalocar = findViewById(R.id.btnDesalocar);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        View add = findViewById(R.id.btnAdd);
        add.setVisibility(admin ? View.VISIBLE : View.GONE);
        add.setOnClickListener(v -> dialogColaborador(null));

        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ColaboradorAdapter(admin, this);
        lista.setAdapter(adapter);

        TextInputEditText busca = findViewById(R.id.busca);
        busca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.filtrar(s.toString());
            }
        });

        chkTodos.setOnCheckedChangeListener((b, marcado) -> {
            if (!atualizando) adapter.marcarTodosVisiveis(marcado);
        });
        btnAlocar.setOnClickListener(v -> iniciarOperacao(false));
        btnDesalocar.setOnClickListener(v -> iniciarOperacao(true));

        carregarFrentes();
        carregar();
    }

    // ---------- Dados ----------
    private void carregar() {
        AppDatabase.IO.execute(() -> {
            Setor s = db.setorDao().buscar(setorId);
            List<Colaborador> pessoas = s == null
                    ? new ArrayList<Colaborador>()
                    : db.colaboradorDao().listarPorSetor(setorId);
            Util.naTela(this, () -> {
                if (s == null) {
                    finish();
                    return;
                }
                setor = s;
                ((TextView) findViewById(R.id.txtSetor)).setText(s.nome);
                adapter.definir(pessoas);
            });
        });
    }

    private void carregarFrentes() {
        AppDatabase.IO.execute(() -> {
            List<Frente> frentes = FrenteRepositorio.listar();
            Util.naTela(this, () -> {
                chips.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(this);
                for (Frente f : frentes) {
                    Chip chip = (Chip) inflater.inflate(R.layout.chip_frente, chips, false);
                    chip.setId(View.generateViewId());
                    chip.setText(f.nome);
                    chip.setTag(f);
                    chips.addView(chip);
                }
                if (chips.getChildCount() > 0) chips.check(chips.getChildAt(0).getId()); // primeira frente
            });
        });
    }

    private Frente frenteEscolhida() {
        int id = chips.getCheckedChipId();
        View v = id == View.NO_ID ? null : chips.findViewById(id);
        return v == null ? null : (Frente) v.getTag();
    }

    // ---------- Seleção ----------
    @Override
    public void selecaoMudou() {
        int marcados = adapter.totalMarcados();
        int total = adapter.totalGeral();

        if (setor != null) {
            txtSetorInfo.setText("Nº " + setor.numeroSetor + "  ·  " + (total == 1 ? "1 pessoa" : total + " pessoas"));
        }
        txtContagem.setText(marcados + (marcados == 1 ? " selecionada" : " selecionadas"));

        atualizando = true;
        chkTodos.setChecked(adapter.todosVisiveisMarcados());
        atualizando = false;
        chkTodos.setEnabled(adapter.totalVisiveis() > 0);

        btnAlocar.setEnabled(marcados > 0);
        btnDesalocar.setEnabled(marcados > 0);
        btnAlocar.setText(marcados == 0 ? "Alocar" : "Alocar (" + marcados + ")");
        btnDesalocar.setText(marcados == 0 ? "Desalocar" : "Desalocar (" + marcados + ")");

        boolean semItens = adapter.totalVisiveis() == 0;
        vazio.setVisibility(semItens ? View.VISIBLE : View.GONE);
        if (total == 0) {
            vazioTitulo.setText("Nenhuma pessoa neste setor");
            vazioDica.setText(admin ? "Toque no + lá em cima para adicionar." : "Peça ao ADM para cadastrar as pessoas.");
        } else {
            vazioTitulo.setText("Nada encontrado");
            vazioDica.setText("Tente outro nome ou outra chapa.");
        }
    }

    // ---------- Iniciar a alocação ----------
    private void iniciarOperacao(boolean desalocar) {
        List<Colaborador> escolhidos = adapter.marcadosEmOrdem();
        Frente frente = frenteEscolhida();
        if (escolhidos.isEmpty() || setor == null) return;
        if (frente == null) {
            Util.aviso(this, "Escolha a frente.");
            return;
        }

        if (!new SiteConfig(this).completo()) {
            if (admin) {
                Dialogos.confirmar(this, "Site não configurado",
                        "Informe o endereço, o usuário e a senha do site para poder " + (desalocar ? "desalocar." : "alocar."),
                        "Configurar", () -> ConfigDialogs.configurarSite(this, () -> iniciarOperacao(desalocar)));
            } else {
                Util.aviso(this, "O ADM ainda não configurou o site. Peça para ele configurar.");
            }
            return;
        }

        int n = escolhidos.size();
        String resumo = (n == 1 ? "1 pessoa" : n + " pessoas") + " do setor " + setor.nome
                + " (nº " + setor.numeroSetor + ") " + (desalocar ? "serão desalocadas da frente " : "serão alocadas na frente ")
                + frente.nome + ".";

        Dialogos.confirmar(this, desalocar ? "Iniciar desalocação?" : "Iniciar alocação?", resumo,
                desalocar ? "Desalocar" : "Alocar", () -> {
            String[] nomes = new String[n];
            String[] chapas = new String[n];
            for (int i = 0; i < n; i++) {
                nomes[i] = escolhidos.get(i).nome;
                chapas[i] = escolhidos.get(i).chapa;
            }
            Intent intent = new Intent(this, AlocacaoActivity.class);
            intent.putExtra("setorNome", setor.nome);
            intent.putExtra("numeroSetor", setor.numeroSetor);
            intent.putExtra("frenteNome", frente.nome);
            intent.putExtra("frenteXpath", frente.xpath);
            intent.putExtra("nomes", nomes);
            intent.putExtra("chapas", chapas);
            intent.putExtra("modo", desalocar ? "desalocar" : "alocar");
            startActivity(intent);
        });
    }

    // ---------- Adicionar / editar / excluir pessoa (ADM) ----------
    private void dialogColaborador(Colaborador existente) {
        LinearLayout caixa = Dialogos.caixa(this);
        Dialogos.Campo nome = Dialogos.campo(this, caixa, "Nome da pessoa",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        Dialogos.Campo chapa = Dialogos.campo(this, caixa, "Chapa (número do colaborador)",
                InputType.TYPE_CLASS_NUMBER);
        if (existente != null) {
            nome.definir(existente.nome);
            chapa.definir(existente.chapa);
        }

        Dialogos.formulario(this, existente == null ? "Nova pessoa" : "Editar pessoa", "Salvar", caixa, d -> {
            String n = nome.texto();
            String ch = chapa.texto();
            boolean valido = true;
            if (n.isEmpty()) {
                nome.erro("Informe o nome");
                valido = false;
            } else {
                nome.erro(null);
            }
            if (ch.isEmpty()) {
                chapa.erro("Informe a chapa");
                valido = false;
            } else {
                chapa.erro(null);
            }
            if (!valido) return;

            AppDatabase.IO.execute(() -> {
                int ignorar = existente == null ? -1 : existente.id;
                if (db.colaboradorDao().contarChapa(ch, ignorar) > 0) {
                    Util.naTela(this, () -> chapa.erro("Essa chapa já está cadastrada"));
                    return;
                }
                Colaborador c = new Colaborador();
                c.nome = n;
                c.chapa = ch;
                c.setorId = setorId;
                try {
                    if (existente == null) {
                        db.colaboradorDao().inserir(c);
                    } else {
                        c.id = existente.id;
                        db.colaboradorDao().atualizar(c);
                    }
                } catch (SQLiteConstraintException e) {
                    Util.naTela(this, () -> chapa.erro("Essa chapa já está cadastrada"));
                    return;
                }
                Util.naTela(this, () -> {
                    d.dismiss();
                    carregar();
                });
            });
        });
    }

    @Override
    public void editar(Colaborador c) {
        dialogColaborador(c);
    }

    @Override
    public void excluir(Colaborador c) {
        Dialogos.confirmar(this, "Excluir pessoa",
                "Excluir " + c.nome + " (chapa " + c.chapa + ")?", "Excluir", () ->
                        AppDatabase.IO.execute(() -> {
                            db.colaboradorDao().remover(c);
                            Util.naTela(this, this::carregar);
                        }));
    }
}

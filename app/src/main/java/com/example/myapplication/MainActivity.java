package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.LinearLayout;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

/** Tela inicial: entrar como visitante (sem login) ou como ADM (com senha). */
public class MainActivity extends AppCompatActivity {

    private static final int SENHA = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Util.bordas(findViewById(R.id.raiz), findViewById(R.id.topo));
        Util.iconesEscuros(this, false); // cabeçalho azul: ícones brancos na barra de status

        findViewById(R.id.cardVisitante).setOnClickListener(v -> abrirPainel(false));
        findViewById(R.id.cardAdm).setOnClickListener(v -> {
            if (AdminAuth.temSenha(this)) pedirSenha();
            else criarSenha();
        });
    }

    private void abrirPainel(boolean admin) {
        Intent i = new Intent(this, PainelActivity.class);
        i.putExtra("admin", admin);
        startActivity(i);
    }

    private void pedirSenha() {
        LinearLayout caixa = Dialogos.caixa(this);
        Dialogos.Campo senha = Dialogos.campo(this, caixa, "Senha do ADM", SENHA);

        Dialogos.formulario(this, "Entrar como ADM", "Entrar", caixa, d -> {
            int espera = AdminAuth.segundosBloqueado(this);
            if (espera > 0) {
                senha.erro("Muitas tentativas. Aguarde " + espera + " s.");
                return;
            }
            if (AdminAuth.conferir(this, senha.bruto())) {
                d.dismiss();
                abrirPainel(true);
            } else {
                int resta = AdminAuth.segundosBloqueado(this);
                senha.erro(resta > 0 ? "Muitas tentativas. Aguarde " + resta + " s." : "Senha incorreta");
            }
        });
    }

    private void criarSenha() {
        LinearLayout caixa = Dialogos.caixa(this);
        Dialogos.Campo s1 = Dialogos.campo(this, caixa, "Crie a senha do ADM (mínimo 4 caracteres)", SENHA);
        Dialogos.Campo s2 = Dialogos.campo(this, caixa, "Repita a senha", SENHA);

        Dialogos.formulario(this, "Primeiro acesso: defina a senha", "Salvar", caixa, d -> {
            String a = s1.bruto();
            if (a.length() < 4) {
                s1.erro("Mínimo de 4 caracteres");
                return;
            }
            s1.erro(null);
            if (!a.equals(s2.bruto())) {
                s2.erro("As senhas não conferem");
                return;
            }
            AdminAuth.definir(this, a);
            d.dismiss();
            abrirPainel(true);
        });
    }
}

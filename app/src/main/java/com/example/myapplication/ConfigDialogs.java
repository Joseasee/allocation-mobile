package com.example.myapplication;

import android.app.Activity;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.materialswitch.MaterialSwitch;

/** Telas de configuração do ADM: dados do site e troca de senha. */
public final class ConfigDialogs {
    private ConfigDialogs() {}

    private static final int SENHA = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;

    public static void configurarSite(Activity a, Runnable depois) {
        SiteConfig cfg = new SiteConfig(a);

        LinearLayout caixa = Dialogos.caixa(a);
        Dialogos.Campo url = Dialogos.campo(a, caixa, "Endereço do site",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        Dialogos.Campo usuario = Dialogos.campo(a, caixa, "Usuário do site",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        Dialogos.Campo senha = Dialogos.campo(a, caixa,
                cfg.temSenha() ? "Senha do site (vazio = manter a atual)" : "Senha do site", SENHA);
        url.definir(cfg.url());
        usuario.definir(cfg.usuario());

        MaterialSwitch mostrar = new MaterialSwitch(a);
        mostrar.setText("Mostrar o site durante a alocação");
        mostrar.setChecked(cfg.mostrarSite());
        caixa.addView(mostrar);

        TextView dica = new TextView(a);
        dica.setText("Ligado: você vê o site trabalhando (bom para testar). "
                + "Desligado: o site roda escondido e o app mostra só o progresso.");
        dica.setTextSize(13);
        dica.setTextColor(ContextCompat.getColor(a, R.color.texto_suave));
        dica.setPadding(0, Util.dp(a, 4), 0, Util.dp(a, 8));
        caixa.addView(dica);

        ScrollView rolagem = new ScrollView(a);
        rolagem.addView(caixa);

        Dialogos.formulario(a, "Configurar site", "Salvar", rolagem, d -> {
            boolean valido = true;
            String endereco = url.texto();
            if (!(endereco.startsWith("https://") || endereco.startsWith("http://"))) {
                url.erro("Informe o endereço completo (https://...)");
                valido = false;
            } else {
                url.erro(null);
            }
            if (usuario.texto().isEmpty()) {
                usuario.erro("Informe o usuário");
                valido = false;
            } else {
                usuario.erro(null);
            }
            if (senha.bruto().isEmpty() && !cfg.temSenha()) {
                senha.erro("Informe a senha");
                valido = false;
            } else {
                senha.erro(null);
            }
            if (!valido) return;

            if (!cfg.salvar(endereco, usuario.texto(), senha.bruto(), mostrar.isChecked())) {
                Util.aviso(a, "Não foi possível guardar com segurança neste aparelho.");
                return;
            }
            d.dismiss();
            Util.aviso(a, "Configuração salva.");
            if (depois != null) depois.run();
        });
    }

    public static void alterarSenha(Activity a) {
        LinearLayout caixa = Dialogos.caixa(a);
        Dialogos.Campo atual = Dialogos.campo(a, caixa, "Senha atual", SENHA);
        Dialogos.Campo nova = Dialogos.campo(a, caixa, "Nova senha (mínimo 4 caracteres)", SENHA);
        Dialogos.Campo repetir = Dialogos.campo(a, caixa, "Repita a nova senha", SENHA);

        Dialogos.formulario(a, "Alterar senha do ADM", "Salvar", caixa, d -> {
            int espera = AdminAuth.segundosBloqueado(a);
            if (espera > 0) {
                atual.erro("Muitas tentativas. Aguarde " + espera + " s.");
                return;
            }
            if (!AdminAuth.conferir(a, atual.bruto())) {
                atual.erro("Senha incorreta");
                return;
            }
            atual.erro(null);

            String n = nova.bruto();
            if (n.length() < 4) {
                nova.erro("Mínimo de 4 caracteres");
                return;
            }
            nova.erro(null);
            if (!n.equals(repetir.bruto())) {
                repetir.erro("As senhas não conferem");
                return;
            }
            AdminAuth.definir(a, n);
            d.dismiss();
            Util.aviso(a, "Senha alterada.");
        });
    }
}

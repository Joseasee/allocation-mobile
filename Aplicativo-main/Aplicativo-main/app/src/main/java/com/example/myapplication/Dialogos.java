package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Formulários e confirmações com o visual do app. */
public final class Dialogos {
    private Dialogos() {}

    /** Quem usa o formulário decide quando fechar (chamando dialogo.dismiss()). */
    public interface Confirma {
        void ok(AlertDialog dialogo);
    }

    /** Um campo de texto do formulário. */
    public static class Campo {
        public final TextInputLayout layout;
        public final TextInputEditText edit;

        Campo(TextInputLayout layout, TextInputEditText edit) {
            this.layout = layout;
            this.edit = edit;
        }

        /** Texto sem espaços nas pontas. */
        public String texto() {
            return edit.getText() == null ? "" : edit.getText().toString().trim();
        }

        /** Texto exatamente como foi digitado (use para senhas). */
        public String bruto() {
            return edit.getText() == null ? "" : edit.getText().toString();
        }

        public void definir(String valor) {
            edit.setText(valor);
            if (valor != null) edit.setSelection(valor.length());
        }

        /** Mostra a mensagem de erro no campo (null = limpa). */
        public void erro(String mensagem) {
            layout.setError(mensagem);
        }
    }

    /** Caixa vertical com as margens certas para colocar dentro de um diálogo. */
    public static LinearLayout caixa(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int lado = Util.dp(c, 24);
        l.setPadding(lado, Util.dp(c, 8), lado, 0);
        return l;
    }

    /** Cria um campo, já dentro da caixa. Para senha use InputType.TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD. */
    public static Campo campo(Context c, LinearLayout pai, String rotulo, int tipo) {
        View v = LayoutInflater.from(c).inflate(R.layout.view_campo, pai, false);
        TextInputLayout til = v.findViewById(R.id.til);
        TextInputEditText et = v.findViewById(R.id.et);
        til.setHint(rotulo);
        et.setInputType(tipo);
        if ((tipo & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)
                == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) {
            til.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }
        pai.addView(v);
        return new Campo(til, et);
    }

    /** Diálogo de formulário: o diálogo só fecha quando você chamar dialogo.dismiss(). */
    public static AlertDialog formulario(Context c, String titulo, String positivo,
                                         View conteudo, Confirma confirma) {
        AlertDialog d = new MaterialAlertDialogBuilder(c)
                .setTitle(titulo)
                .setView(conteudo)
                .setPositiveButton(positivo, null)
                .setNegativeButton("Cancelar", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> confirma.ok(d)));

        View primeiro = conteudo.findViewById(R.id.et);
        if (primeiro != null) primeiro.requestFocus();
        if (d.getWindow() != null) {
            d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        d.show();
        return d;
    }

    public static void confirmar(Context c, String titulo, String mensagem,
                                 String textoOk, Runnable aoConfirmar) {
        confirmar(c, titulo, mensagem, textoOk, "Cancelar", aoConfirmar);
    }

    public static void confirmar(Context c, String titulo, String mensagem,
                                 String textoOk, String textoNao, Runnable aoConfirmar) {
        new MaterialAlertDialogBuilder(c)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton(textoOk, (d, w) -> aoConfirmar.run())
                .setNegativeButton(textoNao, null)
                .show();
    }
}

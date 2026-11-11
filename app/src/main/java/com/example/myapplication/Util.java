package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.snackbar.Snackbar;

/** Pequenas ajudas usadas por várias telas. */
public final class Util {
    private Util() {}

    public static int dp(Context c, int valor) {
        return Math.round(valor * c.getResources().getDisplayMetrics().density);
    }

    /** Roda na tela só se ela ainda existir (evita erro quando a pessoa já saiu). */
    public static void naTela(Activity a, Runnable r) {
        a.runOnUiThread(() -> {
            if (!a.isFinishing() && !a.isDestroyed()) r.run();
        });
    }

    public static void aviso(Activity a, String msg) {
        Snackbar.make(a.findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Deixa o app usar a tela toda sem ficar embaixo da barra de status, da barra de navegação
     * ou do teclado: o "topo" ganha o espaço da barra de status; a raiz ganha o resto.
     */
    public static void bordas(View raiz, View topo) {
        final int topoOriginal = topo.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(barras.left, 0, barras.right, Math.max(barras.bottom, teclado.bottom));
            topo.setPadding(topo.getPaddingLeft(), topoOriginal + barras.top,
                    topo.getPaddingRight(), topo.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** true = ícones escuros na barra de status (para fundo claro); false = ícones brancos (fundo escuro). */
    public static void iconesEscuros(Activity a, boolean escuros) {
        WindowCompat.getInsetsController(a.getWindow(), a.getWindow().getDecorView())
                .setAppearanceLightStatusBars(escuros);
    }
}

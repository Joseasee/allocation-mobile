package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import at.favre.lib.crypto.bcrypt.BCrypt;

/** Senha do ADM. Guarda só o "hash" (nunca a senha) e bloqueia por 1 minuto após 5 erros seguidos. */
public class AdminAuth {
    private static final int MAX_FALHAS = 5;
    private static final long BLOQUEIO_MS = 60_000L;

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("config", Context.MODE_PRIVATE);
    }

    public static boolean temSenha(Context c) {
        return p(c).contains("admin_hash");
    }

    public static void definir(Context c, String senha) {
        String hash = BCrypt.withDefaults().hashToString(10, senha.toCharArray());
        p(c).edit().putString("admin_hash", hash).putInt("falhas", 0).putLong("bloqueio_ate", 0).apply();
    }

    /** Segundos que faltam para liberar novas tentativas (0 = liberado). */
    public static int segundosBloqueado(Context c) {
        long resta = p(c).getLong("bloqueio_ate", 0) - System.currentTimeMillis();
        return resta > 0 ? (int) Math.ceil(resta / 1000.0) : 0;
    }

    public static boolean conferir(Context c, String senha) {
        if (segundosBloqueado(c) > 0) return false;
        String hash = p(c).getString("admin_hash", null);
        if (hash == null) return false;

        boolean ok = BCrypt.verifyer().verify(senha.toCharArray(), hash).verified;
        SharedPreferences.Editor e = p(c).edit();
        if (ok) {
            e.putInt("falhas", 0).putLong("bloqueio_ate", 0);
        } else {
            int falhas = p(c).getInt("falhas", 0) + 1;
            if (falhas >= MAX_FALHAS) {
                e.putInt("falhas", 0).putLong("bloqueio_ate", System.currentTimeMillis() + BLOQUEIO_MS);
            } else {
                e.putInt("falhas", falhas);
            }
        }
        e.apply();
        return ok;
    }
}

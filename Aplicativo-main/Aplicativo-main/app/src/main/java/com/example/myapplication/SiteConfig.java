package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

/** Configuração do site: endereço, usuário e senha (criptografados) e se o site fica visível durante a alocação. */
public class SiteConfig {
    public static final String URL_PADRAO = "https://exemplo.com.br/#/inicio";

    private final SharedPreferences prefs;

    public SiteConfig(Context c) {
        prefs = c.getApplicationContext().getSharedPreferences("site", Context.MODE_PRIVATE);
    }

    public String url() {
        return prefs.getString("url", URL_PADRAO);
    }

    public String usuario() {
        return ler("usuario");
    }

    public String senha() {
        return ler("senha");
    }

    public boolean temSenha() {
        return prefs.contains("senha");
    }

    /** true = o site aparece na tela durante a alocação (bom para testar); false = fica escondido. */
    public boolean mostrarSite() {
        return prefs.getBoolean("mostrar", true);
    }

    public boolean completo() {
        return !url().trim().isEmpty() && !usuario().isEmpty() && !senha().isEmpty();
    }

    /** Senha vazia = mantém a senha atual. Devolve false se não conseguiu guardar com segurança. */
    public boolean salvar(String url, String usuario, String senha, boolean mostrar) {
        SharedPreferences.Editor e = prefs.edit();
        e.putString("url", url.trim());
        e.putBoolean("mostrar", mostrar);
        try {
            e.putString("usuario", Cofre.cifrar(usuario));
            if (!senha.isEmpty()) e.putString("senha", Cofre.cifrar(senha));
        } catch (Exception ex) {
            return false;
        }
        e.apply();
        return true;
    }

    private String ler(String chave) {
        String v = prefs.getString(chave, null);
        if (v == null) return "";
        try {
            return Cofre.decifrar(v);
        } catch (Exception e) {
            return "";
        }
    }
}

package com.example.myapplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** De onde vêm as frentes. Hoje: lista fixa. Depois: API da empresa (basta ligar USAR_API e informar a URL). */
public class FrenteRepositorio {

    /** false = lista fixa (testes). true = busca as frentes na API da empresa. */
    public static final boolean USAR_API = false;

    /** Endereço da API (a empresa informa). Aceita ["Frente A", ...], [{"nome":"Frente A"}, ...] ou {"frentes":[...]}. */
    public static final String URL_API = "";

    // Caminho (XPath) da lista de frentes no site. Só muda o último [n].
    private static final String BASE =
            "//*[@id='lista-frentes']/div";

    /** Chamar fora da tela principal (usa rede quando a API está ligada). */
    public static List<Frente> listar() {
        List<Frente> lista = null;
        if (USAR_API && !URL_API.isEmpty()) {
            try {
                lista = buscarNaApi();
            } catch (Exception e) {
                // API fora do ar: cai na lista fixa
            }
        }
        if (lista == null || lista.isEmpty()) lista = listaFixa();

        // A "Frente A" sempre aparece primeiro (e vem marcada)
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).nome.equalsIgnoreCase("frente a")) {
                lista.add(0, lista.remove(i));
                break;
            }
        }
        return lista;
    }

    private static List<Frente> listaFixa() {
        List<Frente> l = new ArrayList<>();
        l.add(new Frente("Frente B", BASE + "[1]"));
        l.add(new Frente("Frente A", BASE + "[2]"));
        l.add(new Frente("Frente C", BASE + "[3]"));
        l.add(new Frente("Frente D", BASE + "[4]"));
        return l;
    }

    private static List<Frente> buscarNaApi() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(URL_API).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
            String linha;
            while ((linha = r.readLine()) != null) sb.append(linha);
        } finally {
            c.disconnect();
        }

        String texto = sb.toString().trim();
        JSONArray arr = texto.startsWith("{")
                ? new JSONObject(texto).getJSONArray("frentes")
                : new JSONArray(texto);

        List<Frente> l = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            Object o = arr.get(i);
            String nome = (o instanceof JSONObject) ? ((JSONObject) o).optString("nome", "") : String.valueOf(o);
            if (!nome.trim().isEmpty()) l.add(new Frente(nome.trim(), "")); // sem xpath: o site é procurado pelo nome
        }
        return l;
    }
}

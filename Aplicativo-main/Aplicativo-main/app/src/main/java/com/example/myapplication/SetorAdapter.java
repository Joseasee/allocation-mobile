package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lista de setores (cartões). */
public class SetorAdapter extends RecyclerView.Adapter<SetorAdapter.VH> {

    public interface Ouvinte {
        void abrir(SetorComTotal item);

        void menu(View ancora, SetorComTotal item);
    }

    private final List<SetorComTotal> itens = new ArrayList<>();
    private final boolean admin;
    private final Ouvinte ouvinte;

    public SetorAdapter(boolean admin, Ouvinte ouvinte) {
        this.admin = admin;
        this.ouvinte = ouvinte;
    }

    public void definir(List<SetorComTotal> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View v = LayoutInflater.from(pai.getContext()).inflate(R.layout.item_setor, pai, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int posicao) {
        SetorComTotal item = itens.get(posicao);
        String nome = item.setor.nome == null ? "" : item.setor.nome;
        h.inicial.setText(nome.isEmpty() ? "?" : nome.substring(0, 1).toUpperCase(Locale.getDefault()));
        h.nome.setText(nome);
        String pessoas = item.total == 1 ? "1 pessoa" : item.total + " pessoas";
        h.info.setText("Nº " + item.setor.numeroSetor + "  ·  " + pessoas);

        h.mais.setVisibility(admin ? View.VISIBLE : View.GONE);
        h.seta.setVisibility(admin ? View.GONE : View.VISIBLE);
        h.card.setOnClickListener(v -> ouvinte.abrir(item));
        h.mais.setOnClickListener(v -> ouvinte.menu(v, item));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final View card;
        final View mais;
        final View seta;
        final TextView inicial;
        final TextView nome;
        final TextView info;

        VH(View v) {
            super(v);
            card = v;
            inicial = v.findViewById(R.id.txtInicial);
            nome = v.findViewById(R.id.txtNome);
            info = v.findViewById(R.id.txtInfo);
            mais = v.findViewById(R.id.btnMais);
            seta = v.findViewById(R.id.seta);
        }
    }
}

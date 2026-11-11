package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Lista de pessoas de um setor, com seleção (para alocar) e busca. */
public class ColaboradorAdapter extends RecyclerView.Adapter<ColaboradorAdapter.VH> {

    public interface Ouvinte {
        /** A seleção, a busca ou a lista mudou. */
        void selecaoMudou();

        void editar(Colaborador c);

        void excluir(Colaborador c);
    }

    private final List<Colaborador> todos = new ArrayList<>();
    private final List<Colaborador> visiveis = new ArrayList<>();
    private final Set<Integer> marcados = new HashSet<>();
    private final boolean admin;
    private final Ouvinte ouvinte;
    private String filtro = "";

    public ColaboradorAdapter(boolean admin, Ouvinte ouvinte) {
        this.admin = admin;
        this.ouvinte = ouvinte;
    }

    public void definir(List<Colaborador> lista) {
        todos.clear();
        todos.addAll(lista);
        Set<Integer> ids = new HashSet<>();
        for (Colaborador c : lista) ids.add(c.id);
        marcados.retainAll(ids); // some da seleção quem foi excluído
        aplicar();
    }

    public void filtrar(String texto) {
        filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        aplicar();
    }

    private void aplicar() {
        visiveis.clear();
        for (Colaborador c : todos) {
            if (filtro.isEmpty() || minusculo(c.nome).contains(filtro) || minusculo(c.chapa).contains(filtro)) {
                visiveis.add(c);
            }
        }
        notifyDataSetChanged();
        ouvinte.selecaoMudou();
    }

    private static String minusculo(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    /** Pessoas marcadas, na ordem da lista. */
    public List<Colaborador> marcadosEmOrdem() {
        List<Colaborador> r = new ArrayList<>();
        for (Colaborador c : todos) if (marcados.contains(c.id)) r.add(c);
        return r;
    }

    public int totalMarcados() {
        return marcados.size();
    }

    public int totalVisiveis() {
        return visiveis.size();
    }

    public int totalGeral() {
        return todos.size();
    }

    public boolean todosVisiveisMarcados() {
        if (visiveis.isEmpty()) return false;
        for (Colaborador c : visiveis) if (!marcados.contains(c.id)) return false;
        return true;
    }

    public void marcarTodosVisiveis(boolean marcar) {
        for (Colaborador c : visiveis) {
            if (marcar) marcados.add(c.id);
            else marcados.remove(c.id);
        }
        notifyDataSetChanged();
        ouvinte.selecaoMudou();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View v = LayoutInflater.from(pai.getContext()).inflate(R.layout.item_colaborador, pai, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int posicao) {
        Colaborador c = visiveis.get(posicao);
        boolean marcado = marcados.contains(c.id);
        android.content.Context ctx = h.card.getContext();

        h.nome.setText(c.nome);
        h.chapa.setText("Chapa " + c.chapa);
        h.chk.setChecked(marcado);
        h.card.setCardBackgroundColor(ContextCompat.getColor(ctx, marcado ? R.color.azul_claro : R.color.branco));
        h.card.setStrokeColor(ContextCompat.getColor(ctx, marcado ? R.color.azul : R.color.borda));

        h.editar.setVisibility(admin ? View.VISIBLE : View.GONE);
        h.excluir.setVisibility(admin ? View.VISIBLE : View.GONE);

        h.card.setOnClickListener(v -> {
            if (marcados.contains(c.id)) marcados.remove(c.id);
            else marcados.add(c.id);
            int p = h.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) notifyItemChanged(p);
            ouvinte.selecaoMudou();
        });
        h.editar.setOnClickListener(v -> ouvinte.editar(c));
        h.excluir.setOnClickListener(v -> ouvinte.excluir(c));
    }

    @Override
    public int getItemCount() {
        return visiveis.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final MaterialCheckBox chk;
        final TextView nome;
        final TextView chapa;
        final View editar;
        final View excluir;

        VH(View v) {
            super(v);
            card = (MaterialCardView) v;
            chk = v.findViewById(R.id.chk);
            nome = v.findViewById(R.id.txtNome);
            chapa = v.findViewById(R.id.txtChapa);
            editar = v.findViewById(R.id.btnEditar);
            excluir = v.findViewById(R.id.btnExcluir);
        }
    }
}

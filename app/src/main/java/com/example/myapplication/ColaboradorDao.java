package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ColaboradorDao {
    @Insert
    void inserir(Colaborador colaborador);

    @Update
    void atualizar(Colaborador colaborador);

    @Delete
    void remover(Colaborador colaborador);

    @Query("SELECT * FROM Colaborador WHERE setorId = :setorId ORDER BY nome COLLATE NOCASE")
    List<Colaborador> listarPorSetor(int setorId);

    @Query("SELECT COUNT(*) FROM Colaborador WHERE chapa = :chapa AND id <> :ignorarId")
    int contarChapa(String chapa, int ignorarId);

    @Query("DELETE FROM Colaborador WHERE setorId = :setorId")
    void removerPorSetor(int setorId);
}

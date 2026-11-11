package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface SetorDao {
    @Insert
    void inserir(Setor setor);

    @Update
    void atualizar(Setor setor);

    @Delete
    void remover(Setor setor);

    @Query("SELECT * FROM Setor WHERE id = :id")
    Setor buscar(int id);

    @Query("SELECT s.*, (SELECT COUNT(*) FROM Colaborador c WHERE c.setorId = s.id) AS total "
            + "FROM Setor s ORDER BY s.nome COLLATE NOCASE")
    List<SetorComTotal> listarComTotal();
}

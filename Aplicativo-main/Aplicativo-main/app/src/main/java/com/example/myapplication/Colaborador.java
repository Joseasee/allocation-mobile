package com.example.myapplication;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Uma pessoa. A chapa é única: não deixa cadastrar duas iguais. */
@Entity(indices = {@Index(value = {"chapa"}, unique = true)})
public class Colaborador {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String nome;
    public String chapa;
    public int setorId;
}

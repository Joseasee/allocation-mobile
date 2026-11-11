package com.example.myapplication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Um setor (ex.: Gestão, Célula 1). O número é o que vai no site (código da frente). */
@Entity
public class Setor {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String nome;
    public String numeroSetor;
}

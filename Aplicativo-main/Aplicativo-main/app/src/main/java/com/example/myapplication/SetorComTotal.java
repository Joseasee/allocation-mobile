package com.example.myapplication;

import androidx.room.Embedded;

/** Setor + quantidade de pessoas (usado na lista de setores). */
public class SetorComTotal {
    @Embedded
    public Setor setor;
    public int total;
}

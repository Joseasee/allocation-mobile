package com.example.myapplication;

/** Uma frente do site (Frente A, Frente B...). O xpath é o caminho da opção na lista do site (vazio = procurar pelo nome). */
public class Frente {
    public final String nome;
    public final String xpath;

    public Frente(String nome, String xpath) {
        this.nome = nome;
        this.xpath = xpath;
    }
}

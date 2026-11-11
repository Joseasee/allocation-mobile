package com.example.myapplication;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Setor.class, Colaborador.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract SetorDao setorDao();

    public abstract ColaboradorDao colaboradorDao();

    /** O Room não deixa acessar o banco na tela principal: tudo passa por aqui. */
    public static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private static AppDatabase instancia;

    public static synchronized AppDatabase get(Context ctx) {
        if (instancia == null) {
            instancia = Room.databaseBuilder(
                    ctx.getApplicationContext(), AppDatabase.class, "app.db").build();
        }
        return instancia;
    }
}

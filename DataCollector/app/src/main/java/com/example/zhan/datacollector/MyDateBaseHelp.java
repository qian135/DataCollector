package com.example.zhan.datacollector;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDateBaseHelp extends SQLiteOpenHelper {

    public MyDateBaseHelp(Context context, String name,
                          SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        System.out.println("RAW_DATA,ATTENTION,MEDITATION");
        db.execSQL("create table table1(_id integer primary key autoincrement," +
                "RAW_DATA,ATTENTION,MEDITATION)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        System.out.println("更新" + oldVersion + "to" + newVersion);
    }

}

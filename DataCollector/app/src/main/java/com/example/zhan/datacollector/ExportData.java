package com.example.zhan.datacollector;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

public class ExportData {

    private SQLiteDatabase mSQLiteDatabase;
    private File file;

    ExportData(SQLiteDatabase sqLiteDatabase) {
        mSQLiteDatabase = sqLiteDatabase;
    }

    public void exportDatabaseToCSV() {
        //导出数据保存的文件夹
        File exportDir = new File(Environment.getExternalStorageDirectory().getPath(),
                "/zhanExportData");

        if (!exportDir.exists()) {
            System.out.println(exportDir.mkdir());
        }

        //导出数据的文件名
        file = new File(exportDir, "myData.csv");

        try {
            CSVWriter csvWriter = new CSVWriter(new FileWriter(file));
            Cursor cursor = mSQLiteDatabase.rawQuery(
                    "select * from table1", null
            );

            while (cursor.moveToNext()) {
                //可以在这里增加数据库中数据的列数
                String string[] = {cursor.getString(0), cursor.getString(1),
                        cursor.getString(2)};
                csvWriter.writeNext(string);
            }
            csvWriter.close();
            cursor.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFilePath() {
        return file.getPath();
    }

}

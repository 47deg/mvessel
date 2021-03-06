package com.fortysevendeg.mvessel.test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.fortysevendeg.mvessel.Database;
import com.fortysevendeg.mvessel.api.impl.AndroidCursor;
import com.fortysevendeg.mvessel.api.impl.AndroidDatabaseFactory;

public class MyOpenHelper extends SQLiteOpenHelper {

    private Database<AndroidCursor> database;

    public MyOpenHelper(Context context) {
        super(context, "test.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public Database open() {
        SQLiteDatabase database = getReadableDatabase();
        this.database = new Database<>(new AndroidDatabaseFactory(), database.getPath(), 50, 0, 0);
        return this.database;
    }

    @Override
    public synchronized void close() {
        database.close();
        super.close();
    }

    public Database<AndroidCursor> getDatabase() {
        return database;
    }

}

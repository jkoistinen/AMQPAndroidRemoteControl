package com.example.jk.amqpandroid;

import android.app.Application;
import android.util.Log;

/**
 * Created by jk on 2016-03-08.
 */
public class GlobalApp extends Application {

    //private stuffs

    @Override
    public void onCreate() {
        super.onCreate();
        //dbHelper = new DbHelper(this);
        Log.d("'", "GlobalApp onCreate!!!");

    }

//    public SQLiteDatabase getDatabase(){
//        return dbHelper.getWritableDatabase();
//    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        //dbHelper.close();
    }
}
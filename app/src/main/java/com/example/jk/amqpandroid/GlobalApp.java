package com.example.jk.amqpandroid;
import android.app.Application;
import android.util.Log;

/**
 * Created by jk on 2016-03-08.
 */
public class GlobalApp extends Application {

    public AMQPConnectionHelper connhelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("'", "GlobalApp onCreate!!!");

        AMQPConnectionHelper connhelper = new AMQPConnectionHelper(this);

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        //dbHelper.close();
    }
}
package com.example.jk.amqpandroid;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by jk on 2016-03-08.
 */
public class GlobalApp extends Application {

    final static ConnectionFactory factory = new ConnectionFactory();

    Connection connection;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("'", "GlobalApp onCreate!!!");

        //Execute in other Thread since this is the UI thread
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    setupConnectionFactory();
                    setConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    void setConnection() {

        try {
            Connection connection = factory.newConnection();
            this.connection = connection;
        } catch (java.io.IOException e1) {
            Log.d("'", "IOException: " + e1.getClass().getName());
        } catch (java.util.concurrent.TimeoutException e1) {
            Log.d("'", "TimeoutException: " + e1.getClass().getName());
        }
    }

    void setupConnectionFactory() {

        String uri = getApplicationContext().getResources().getString(R.string.amqpuri);

        try {
            factory.setAutomaticRecoveryEnabled(false);
            factory.setUri(uri);
        } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        //dbHelper.close();
    }
}
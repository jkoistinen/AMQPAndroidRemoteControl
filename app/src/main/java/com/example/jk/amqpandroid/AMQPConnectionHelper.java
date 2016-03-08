package com.example.jk.amqpandroid;

import android.content.Context;
import android.util.Log;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by jk on 2016-03-08.
 */
public class AMQPConnectionHelper {

    ConnectionFactory factory = new ConnectionFactory();

    public AMQPConnectionHelper(Context context) {
        setupConnectionFactory(context);
    }

    public Connection getConnection() {

        try {
            Connection connection = factory.newConnection();
            return connection;
        } catch (java.io.IOException e1) {
            Log.d("'", "IOException: " + e1.getClass().getName());
        } catch (java.util.concurrent.TimeoutException e1) {
            Log.d("'", "TimeoutException: " + e1.getClass().getName());
        }
        return null;
    }

    public void setupConnectionFactory(Context context) {

        String uri = context.getString(R.string.amqpuri);

        Log.d("'", "New conn!");

        try {
            factory.setAutomaticRecoveryEnabled(false);
            factory.setUri(uri);
        } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e1) {
            e1.printStackTrace();
        }
    }

}
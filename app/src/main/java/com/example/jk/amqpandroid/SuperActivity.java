package com.example.jk.amqpandroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.test.server.HATests;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by jk on 2016-03-07.
 */
public class SuperActivity extends AppCompatActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.game_menu, menu);

        menu.add(0,0,0,"Main").setIntent(new Intent(this, MainActivity.class));
        menu.add(1,1,1,"Second").setIntent(new Intent(this, SecondActivity.class));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Log.d("'", "select!" + item.getItemId());

        switch (item.getItemId()) {
            case 0:
                Log.d("'", "select! 0");
                startActivity(item.getIntent());
                return true;
            case 1:
                Log.d("'", "select! 1");
                startActivity(item.getIntent());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    ConnectionFactory factory = new ConnectionFactory();
    void setupConnectionFactory() {
        String uri = getString(R.string.amqpuri);
        Log.d("'", "New conn!");
        try {
            factory.setAutomaticRecoveryEnabled(false);
            factory.setUri(uri);
        } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e1) {
            e1.printStackTrace();
        }
    }


}

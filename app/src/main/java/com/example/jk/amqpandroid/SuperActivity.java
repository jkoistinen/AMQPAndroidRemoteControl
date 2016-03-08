package com.example.jk.amqpandroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by jk on 2016-03-07.
 */
public class SuperActivity extends AppCompatActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(0,0,0,"Rating").setIntent(new Intent(this, MainActivity.class));
        menu.add(1,1,1,"Chat").setIntent(new Intent(this, SecondActivity.class));
        menu.add(2,2,2,"Test");

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




}

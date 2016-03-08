package com.example.jk.amqpandroid;

import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class SecondActivity extends SuperActivity {

    private static class MyHandler extends Handler {
        private final WeakReference<SecondActivity> mActivity;

        public MyHandler(SecondActivity activity) {
            mActivity = new WeakReference<SecondActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {

            SecondActivity activity = mActivity.get();

            if (activity != null) {

                String message = msg.getData().getString("msg");
                TextView tv = (TextView) activity.findViewById(R.id.textView);
                Date now = new Date();
                SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss");
                tv.append(ft.format(now) + ' ' + message + '\n');

                //Update scrollView to show bottom
                final ScrollView sc = (ScrollView) activity.findViewById(R.id.scrollView);
                sc.post(new Runnable()
                {
                    public void run()
                    {
                        sc.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        }
    }
    private final MyHandler mHandler = new MyHandler(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        setupConnectionFactory();
        publishToAMQP();
        setupPubButton();
        setupDummyPubButton();
        subscribe(mHandler);
    }

    private void setupPubButton() {
        Button button = (Button) findViewById(R.id.publish);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditText et = (EditText) findViewById(R.id.text);
                publishMessage(et.getText().toString());
                Log.d("'", et.getText().toString());
                et.setText("");
            }
        });
    }

    private void setupDummyPubButton() {
        Button button = (Button) findViewById(R.id.buttondummymsg);
        button.setOnClickListener(new View.OnClickListener() {
            int counter = 0;

            @Override
            public void onClick(View arg0) {
                while (counter < 10) {
                    String msg = "Dummy message " + counter;
                    counter++;
                    publishMessage(msg);
                    Log.d("'", msg);
                }
                counter = 0;
            }
        });
    }

    private Thread subscribeThread;
    private Thread publishThread;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        publishThread.interrupt();
        subscribeThread.interrupt();
    }

    private final BlockingDeque<String> queue = new LinkedBlockingDeque<String>();
    private void publishMessage(String message) {
        //Adds a message to internal blocking queue
        try {
            Log.d("'", "[q] " + message);
            queue.putLast(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void subscribe(final Handler handler)
    {
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Connection connection = factory.newConnection();
                        Channel channel = connection.createChannel();
                        channel.basicQos(1);
                        AMQP.Queue.DeclareOk q = channel.queueDeclare();
                        channel.queueBind(q.getQueue(), "amq.fanout", "chat");
                        QueueingConsumer consumer = new QueueingConsumer(channel);
                        channel.basicConsume(q.getQueue(), true, consumer);

                        // Process deliveries
                        while (true) {
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                            String message = new String(delivery.getBody());
                            Log.d("'","[r] " + message);

                            Message msg = handler.obtainMessage();
                            Bundle bundle = new Bundle();

                            bundle.putString("msg", message);
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e1) {
                        Log.d("'", "Connection broken: " + e1.getClass().getName());
                        try {
                            Thread.sleep(4000); //sleep and then try again
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        });
        subscribeThread.start();
    }

    private void publishToAMQP()
    {
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Connection connection = factory.newConnection();
                        Channel ch = connection.createChannel();
                        ch.confirmSelect();

                        while (true) {
                            String message = queue.takeFirst();
                            try{
                                ch.basicPublish("amq.fanout", "chat", null, message.getBytes());
                                Log.d("'", "[s] " + message);
                                ch.waitForConfirmsOrDie();
                            } catch (Exception e){
                                Log.d("'","[f] " + message);
                                queue.putFirst(message);
                                throw e;
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Log.d("'", "Connection broken: " + e.getClass().getName());
                        try {
                            Thread.sleep(5000); //sleep and then try again
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                }
            }
        });
        publishThread.start();
    }
}

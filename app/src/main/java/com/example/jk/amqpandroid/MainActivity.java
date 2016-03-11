package com.example.jk.amqpandroid;

import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.widget.RatingBar;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;

public class MainActivity extends SuperActivity {

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {

            MainActivity activity = mActivity.get();

            if (activity != null) {

                float message = msg.getData().getFloat("rating");

                RatingBar rb = (RatingBar) activity.findViewById(R.id.ratingBar);
                if (rb.getRating() != message) {
                    rb.setRating(message);
                    Log.d("'", "RatingBar changed!");
                }
            }
        }
    }
    private final MyHandler mHandler = new MyHandler(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        publishToAMQPRating();
        setupRatingBar();
        subscribeRating(mHandler);
    }

    private void setupRatingBar() {
        final RatingBar rb = (RatingBar) findViewById(R.id.ratingBar);

        rb.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                Log.d("'", "rbvalue " + rating);
                Log.d("'", "fromUser " + fromUser);
                //insert value into internal queue if fromUser
                if (fromUser) {
                    publishRatingMessage(rating);
                    Log.d("'", "onRatingChanged");
                }
            }
        });
    }

    private Thread subscribeThread;
    private Thread publishThread;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("'", "onDestroy called");

        publishThread.interrupt();
        subscribeThread.interrupt();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("'", "onPause called");

        publishThread.interrupt();
        subscribeThread.interrupt();
    }

    private final BlockingDeque<Float> queueRating = new LinkedBlockingDeque();
    private void publishRatingMessage(Float message) {
        //Adds a message to internal blocking queue
        try {
            Log.d("'", "[qRating] " + message);
            queueRating.putLast(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection(){

        //This functions and data are in GlobalApp java, it has the same lifecycle as Activity but for the entire App
        GlobalApp gApp = (GlobalApp)getApplicationContext();
        Connection connection = gApp.connection;

        return connection;
    }

    //public Channel channel;
    public Channel channelSubscribe;
    public Channel channelPublish;

    private Channel getChannel(Connection connection) throws IOException {
        Channel channel = connection.createChannel();
        Log.d("'", "Channel "+channel.getChannelNumber()+" opened");
        return channel;
    }

    private void closeChannel(Channel channelToClose){
        try {
            channelToClose.close();
            Log.d("'", "Channel "+channelToClose.getChannelNumber()+" closed");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e1) {
            e1.printStackTrace();
        }
    }

    private void subscribeRating(final Handler handler)
    {
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while(true) {
                    try {

                        Connection connection = getConnection();
                        channelSubscribe = getChannel(connection);

                        channelSubscribe.basicQos(1);
                        AMQP.Queue.DeclareOk q = channelSubscribe.queueDeclare();
                        channelSubscribe.queueBind(q.getQueue(), "amq.fanout", "rating");
                        QueueingConsumer consumer = new QueueingConsumer(channelSubscribe);
                        boolean autoAck = false;
                        channelSubscribe.basicConsume(q.getQueue(), autoAck, consumer);

                        // Process deliveries
                        while (true) {
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                            ByteBuffer buf = ByteBuffer.wrap(delivery.getBody());
                            float message = buf.getFloat();

                            Message msg = handler.obtainMessage();
                            Bundle bundle = new Bundle();

                            bundle.putFloat("rating", message);
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                            boolean requeue = false;
                            channelSubscribe.basicAck(delivery.getEnvelope().getDeliveryTag(), requeue );
                        }

                    } catch (InterruptedException e) {
                        closeChannel(channelSubscribe);
                        break;
                    } catch (Exception e1) {
                        Log.d("'", "Connection subscribeThread broken: " + e1.getClass().getName());
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

    private void publishToAMQPRating()
    {
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {

                        Connection connection = getConnection();
                        channelPublish = getChannel(connection);

                        channelPublish.confirmSelect();

                        while (true) {
                            Float message = queueRating.takeFirst();
                            try{

                                //Allocate space for one float (ch.basicPublish only likes ByteArrays)
                                final ByteBuffer buf = ByteBuffer.allocate(4).putFloat(message);

                                channelPublish.basicPublish("amq.fanout", "rating", null, buf.array());
                                Log.d("'", "[s]rating " + message);

                                channelPublish.waitForConfirmsOrDie();
                            } catch (Exception e){
                                Log.d("'","[f]rating " + message);
                                queueRating.putFirst(message);
                                throw e;
                            }
                        }
                    } catch (InterruptedException e) {
                        closeChannel(channelPublish);
                        break;
                    } catch (Exception e) {
                        Log.d("'", "Connection publishThread broken: " + e.getClass().getName());
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

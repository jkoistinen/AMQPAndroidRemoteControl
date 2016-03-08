package com.example.jk.amqpandroid;

import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.widget.RatingBar;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class MainActivity extends SuperActivity {

    Handler incomingRatingMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            float message = msg.getData().getFloat("rating");

            final RatingBar rb = (RatingBar) findViewById(R.id.ratingBar);
            if (rb.getRating() != message ) {
                rb.setRating(message);
                Log.d("'", "RatingBar changed!");
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        setupConnectionFactory();
        publishToAMQPRating();
        setupRatingBar();

        subscribeRating(incomingRatingMessageHandler);
    }

    private void setupRatingBar() {
        final RatingBar rb = (RatingBar) findViewById(R.id.ratingBar);

        //Initial rating
        //rb.setRating((float) 2.5);

        rb.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                Log.d("'", "rbvalue " + rating);
                Log.d("'", "fromUser "+fromUser);
                //insert value into internal queue if fromUser
                if(fromUser) {
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
        publishThread.interrupt();
        subscribeThread.interrupt();
    }

    private final int queuedepth = 1;
    private final BlockingDeque<Float> queueRating = new LinkedBlockingDeque(queuedepth);
    private void publishRatingMessage(Float message) {
        //Adds a message to internal blocking queue
        try {
            Log.d("'", "[qRating] " + message);
            queueRating.putLast(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    private void subscribeRating(final Handler handler)
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
                        channel.queueBind(q.getQueue(), "amq.fanout", "rating");
                        QueueingConsumer consumer = new QueueingConsumer(channel);
                        boolean autoAck = false;
                        channel.basicConsume(q.getQueue(), autoAck, consumer);

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
                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), requeue );
                            Log.d("'", "Consumed message!");
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

    private void publishToAMQPRating()
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
                            int quesize = queueRating.size();
                            Log.d("'", "QueueSize: "+quesize);
                            Float message = queueRating.takeFirst();
                            try{

                                //Allocate space for one float (ch.basicPublish only likes ByteArrays)
                                final ByteBuffer buf = ByteBuffer.allocate(4).putFloat(message);

                                ch.basicPublish("amq.fanout", "rating", null, buf.array());
                                Log.d("'", "[s]rating " + message);

                                ch.waitForConfirmsOrDie();
                            } catch (Exception e){
                                Log.d("'","[f]rating " + message);
                                queueRating.putFirst(message);
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

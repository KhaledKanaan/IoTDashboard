package com.kandroid.iotdashboard;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.FirebaseDatabase;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import static com.kandroid.iotdashboard.App.CHANNEL_ID;
import static com.kandroid.iotdashboard.ConnectToBrokerActivity.username;

import static com.kandroid.iotdashboard.MainActivity.USER_DB_ROOT;
import static java.nio.charset.StandardCharsets.UTF_8;


public class KeepAliveMqttConnectionService extends Service {


    //public static Mqtt5BlockingClient mqttClient;
    public static Mqtt5AsyncClient mqttClient;
    public static boolean isConnectedToBroker =false;
    public static MqttAndroidClient mqttAndroidClient;
    public static String USER_INTENDS_TO_CONNECT = "User intends to connect";
    public PowerManager pm;
    public PowerManager.WakeLock wl;
    public MemoryPersistence persistence = new MemoryPersistence(), persistence2 = new MemoryPersistence();
    public static MqttConnectOptions mqttConnectOptions;



    public static int connectedColorInt = Color.parseColor("#00AA00");
    public static int disconnectedColorInt = Color.parseColor("#000000");

//    private boolean connectToBroker(String mqttVersion, String brokerUrl, String port, String clientId, String username,
//                                          String password, String keepAlive, String QoS, String lasWillTopic, String lastWillMessage, boolean TLS, Context context) {
//
//        isConnectedToBroker = false;
//        if (mqttVersion.equals(MQTT_5) && TLS) {
//
//            try {
//                //create an MQTT client
//                mqttClient = MqttClient.builder()
//                        .useMqttVersion5()
//                        .serverHost(brokerUrl)
//                        .serverPort(Integer.valueOf(port))
//                        .sslWithDefaultConfig()
//                        .identifier(clientId)
//                        .addConnectedListener(new MqttClientConnectedListener() {
//                            @Override
//                            public void onConnected(MqttClientConnectedContext context) {
//
//                                Log.i("MQTT_Cx", "connected");
//                                isConnectedToBroker = true;
//                                ConnectToBrokerActivity.connectionStatusLed.setCardBackgroundColor(connectedColorInt);
//
//                                MainActivity.sharedPreferences.edit().putBoolean(ConnectToBrokerActivity.LAST_MQTT_CONNEX_SUCCEEDED, true).apply();
//
//                                IoT_Database = FirebaseDatabase.getInstance().getReference(USER_DB_ROOT).child(ConnectToBrokerActivity.BROKER_SETTINGS);
//                                IoT_Database.child(BROKER_URL).setValue(ConnectToBrokerActivity.brokerUrl);
//                                IoT_Database.child(PORT).setValue(ConnectToBrokerActivity.port);
//                                IoT_Database.child(USERNAME).setValue(ConnectToBrokerActivity.username);
//                                IoT_Database.child(PASSWORD).setValue(ConnectToBrokerActivity.password);
//                                IoT_Database.child(CLIENT_ID).setValue(ConnectToBrokerActivity.clientId);
//                                IoT_Database.child(KEEP_ALIVE).setValue(ConnectToBrokerActivity.keepAlive);
//                                IoT_Database.child(DEFAULT_QoS).setValue(ConnectToBrokerActivity.defaultQoS);
//                                IoT_Database.child(CONNECT_WITH_TLS).setValue(String.valueOf(ConnectToBrokerActivity.connectWithTls));
//                                IoT_Database.child(MQTT_VERSION).setValue(ConnectToBrokerActivity.mqttVersion);
//                                IoT_Database.child(RETAIN).setValue(ConnectToBrokerActivity.retain);
//                                IoT_Database.child(LAST_WILL_TOPIC).setValue(ConnectToBrokerActivity.lasWillTopic);
//                                IoT_Database.child(LAST_WILL_MASSAGE).setValue(ConnectToBrokerActivity.lastWillMessage);
//                                IoT_Database.child(LAST_MQTT_CONNEX_SUCCEEDED).setValue(ConnectToBrokerActivity.SUCCEEDED);
//
//                            }
//                        })
//                        .addDisconnectedListener(new MqttClientDisconnectedListener() {
//                            @Override
//                            public void onDisconnected(MqttClientDisconnectedContext context) {
//                                Log.i("MQTT_Cx", "disconnected");
//                                //Toast.makeText(getApplicationContext(), "disconnected", Toast.LENGTH_LONG).show;
//                                isConnectedToBroker = false;
//                                ConnectToBrokerActivity.connectionStatusLed.setCardBackgroundColor(disconnectedColorInt);
//
//                                MainActivity.sharedPreferences.edit().putBoolean(ConnectToBrokerActivity.LAST_MQTT_CONNEX_SUCCEEDED, true).apply();
//                                IoT_Database.child(LAST_MQTT_CONNEX_SUCCEEDED).setValue(SUCCEEDED);
//
//                            }
//                        })
//                        .buildBlocking();
//
//            } catch (Exception e) {
//                Toast.makeText(context, "Unable to connect to broker", Toast.LENGTH_SHORT).show();
//                isConnectedToBroker = false;
//            }
//
//            try {
//
//                //connect to HiveMQ Cloud with TLS and username/pw
//                Mqtt5ConnAck connAckMessage = mqttClient.connectWith()
//                        .simpleAuth()
//                        .username(username)
//                        .password(UTF_8.encode(password))
//                        .applySimpleAuth()
//                        .keepAlive(Integer.valueOf(keepAlive))
//                        .send();
//                //Log.i("MQTT_Cxx", connAckMessage.toString());
//
//            } catch (Exception e) {
//
//                Toast.makeText(context, "Unable to connect to broker", Toast.LENGTH_SHORT).show();
//                isConnectedToBroker = false;
//            }
//
//        }
//        return isConnectedToBroker;
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        //ConnectToBrokerActivity.isConnectedToBroker = false;

//        if (ConnectToBrokerActivity.mqttVersion.equals(ConnectToBrokerActivity.MQTT_5) && ConnectToBrokerActivity.connectWithTls) {

//            try {
//                //create an MQTT client
//                mqttClient = MqttClient.builder()
//                        .useMqttVersion5()
//                        .serverHost(ConnectToBrokerActivity.brokerUrl)
//                        .serverPort(Integer.valueOf(ConnectToBrokerActivity.port))
//                        .sslWithDefaultConfig()
//                        .identifier(ConnectToBrokerActivity.clientId)
//                        .automaticReconnectWithDefaultConfig()
//                        .addConnectedListener(new MqttClientConnectedListener() {
//                            @Override
//                            public void onConnected(MqttClientConnectedContext context) {
//
//                                Log.i("MQTT_Cx", "connected");
//                                ConnectToBrokerActivity.isConnectedToBroker = true;
//                                ConnectToBrokerActivity.connectionStatusLed.setCardBackgroundColor(connectedColorInt);
//
//                                MainActivity.sharedPreferences.edit().putBoolean(ConnectToBrokerActivity.LAST_MQTT_CONNEX_SUCCEEDED, true).apply();
//
//                                IoT_Database = FirebaseDatabase.getInstance().getReference(USER_DB_ROOT).child(ConnectToBrokerActivity.BROKER_SETTINGS);
//                                IoT_Database.child(ConnectToBrokerActivity.BROKER_URL).setValue(ConnectToBrokerActivity.brokerUrl);
//                                IoT_Database.child(ConnectToBrokerActivity.PORT).setValue(ConnectToBrokerActivity.port);
//                                IoT_Database.child(ConnectToBrokerActivity.USERNAME).setValue(ConnectToBrokerActivity.username);
//                                IoT_Database.child(ConnectToBrokerActivity.PASSWORD).setValue(ConnectToBrokerActivity.password);
//                                IoT_Database.child(ConnectToBrokerActivity.CLIENT_ID).setValue(ConnectToBrokerActivity.clientId);
//                                IoT_Database.child(ConnectToBrokerActivity.KEEP_ALIVE).setValue(ConnectToBrokerActivity.keepAlive);
//                                IoT_Database.child(ConnectToBrokerActivity.DEFAULT_QoS).setValue(ConnectToBrokerActivity.defaultQoS);
//                                IoT_Database.child(ConnectToBrokerActivity.CONNECT_WITH_TLS).setValue(String.valueOf(ConnectToBrokerActivity.connectWithTls));
//                                IoT_Database.child(ConnectToBrokerActivity.MQTT_VERSION).setValue(ConnectToBrokerActivity.mqttVersion);
//                                IoT_Database.child(ConnectToBrokerActivity.RETAIN).setValue(ConnectToBrokerActivity.retain);
//                                IoT_Database.child(ConnectToBrokerActivity.LAST_WILL_TOPIC).setValue(ConnectToBrokerActivity.lasWillTopic);
//                                IoT_Database.child(ConnectToBrokerActivity.LAST_WILL_MASSAGE).setValue(ConnectToBrokerActivity.lastWillMessage);
//                                IoT_Database.child(ConnectToBrokerActivity.LAST_MQTT_CONNEX_SUCCEEDED).setValue(ConnectToBrokerActivity.SUCCEEDED);
//
//                            }
//                        })
//                        .addDisconnectedListener(new MqttClientDisconnectedListener() {
//                            @Override
//                            public void onDisconnected(MqttClientDisconnectedContext context) {
//                                Log.i("MQTT_Cx", "disconnected");
//                                //Toast.makeText(getApplicationContext(), "disconnected", Toast.LENGTH_LONG).show;
//                                ConnectToBrokerActivity.isConnectedToBroker = false;
//                                ConnectToBrokerActivity.connectionStatusLed.setCardBackgroundColor(disconnectedColorInt);
//
//                                MainActivity.sharedPreferences.edit().putBoolean(ConnectToBrokerActivity.LAST_MQTT_CONNEX_SUCCEEDED, false).apply();
//                                IoT_Database.child(ConnectToBrokerActivity.LAST_MQTT_CONNEX_SUCCEEDED).setValue(ConnectToBrokerActivity.NOT_SUCCEEDED);
//
//                                try {
//
//                                    //connect to HiveMQ Cloud with TLS and username/pw
//                                    CompletableFuture<Mqtt5ConnAck> connAckFuture = mqttClient.connectWith()
//                                            .simpleAuth()
//                                            .username(ConnectToBrokerActivity.username)
//                                            .password(UTF_8.encode(ConnectToBrokerActivity.password))
//                                            .applySimpleAuth()
//                                            .keepAlive(Integer.parseInt(ConnectToBrokerActivity.keepAlive))
//                                            .send();
//                                    //Log.i("MQTT_Cxx", connAckMessage.toString());
//
//                                } catch (Exception e) {
//
//                                    Toast.makeText(getApplicationContext(), "Unable to connect to broker", Toast.LENGTH_SHORT).show();
//                                    ConnectToBrokerActivity.isConnectedToBroker = false;
//                                }
//
//                            }
//                        })
//                        .buildAsync();
//
//            } catch (Exception e) {
//                Toast.makeText(getApplicationContext(), "Unable to connect to broker", Toast.LENGTH_SHORT).show();
//                ConnectToBrokerActivity.isConnectedToBroker = false;
//            }

//            try {
//
//                //connect to HiveMQ Cloud with TLS and username/pw
//                CompletableFuture<Mqtt5ConnAck> connAckFuture = ConnectToBrokerActivity.mqttClient.connectWith()
//                        .simpleAuth()
//                        .username(ConnectToBrokerActivity.username)
//                        .password(UTF_8.encode(ConnectToBrokerActivity.password))
//                        .applySimpleAuth()
//                        .keepAlive(Integer.parseInt(ConnectToBrokerActivity.keepAlive))
//                        .send();
//                //Log.i("MQTT_Cxx", connAckMessage.toString());
//
//            } catch (Exception e) {
//
//                Toast.makeText(getApplicationContext(), "Unable to connect to broker", Toast.LENGTH_SHORT).show();
//                ConnectToBrokerActivity.isConnectedToBroker = false;
//            }
//
//        }
//        return START_STICKY;



//            try {
//                //create an MQTT client
//                mqttClient = MqttClient.builder()
//                        .useMqttVersion5()
//                        .serverHost("4e33b147c0024181ae5078b42e499ad9.s1.eu.hivemq.cloud")
//                        .serverPort(8883)
//                        .sslWithDefaultConfig()
//                        .identifier("khaled-kenaan@hotmail.com")
//                        .automaticReconnectWithDefaultConfig()
//                        .addConnectedListener(new MqttClientConnectedListener() {
//                            @Override
//                            public void onConnected(MqttClientConnectedContext context) {
//
//                                Log.i("MQTT_Cx1", "connected");
//                                isConnectedToBroker = true;
//                                ConnectToBrokerActivity.connectionStatusLed.setCardBackgroundColor(connectedColorInt);
//
//                            }
//                        })
//                        .addDisconnectedListener(new MqttClientDisconnectedListener() {
//                            @Override
//                            public void onDisconnected(MqttClientDisconnectedContext context) {
//                                Log.i("MQTT_Cx2", "disconnected");
//                                //Toast.makeText(getApplicationContext(), "disconnected", Toast.LENGTH_LONG).show;
//                                Log.i("MQTT_Cx2", context.toString());
//                               isConnectedToBroker = false;
//                                ConnectToBrokerActivity.connectionStatusLed.setCardBackgroundColor(disconnectedColorInt);
//
////                                try {
////
////                                    //connect to HiveMQ Cloud with TLS and username/pw
////                                    CompletableFuture<Mqtt5ConnAck> connAckFuture = mqttClient.connectWith()
////                                            .simpleAuth()
////                                            .username("iotdashboard")
////                                            .password(UTF_8.encode("*******"))
////                                            .applySimpleAuth()
////                                            .keepAlive(60)
////                                            .send();
////                                    //Log.i("MQTT_Cxx", connAckMessage.toString());
////
////                                } catch (Exception e) {
////
////                                    Toast.makeText(getApplicationContext(), "Unable to connect to broker", Toast.LENGTH_SHORT).show();
////                                    isConnectedToBroker = false;
////                                    ConnectToBrokerActivity.connectionStatusLed.setCardBackgroundColor(disconnectedColorInt);
////                                }
//
//                            }
//                        })
//                        .buildAsync();
//
//            } catch (Exception e) {
//                Toast.makeText(getApplicationContext(), "Unable to connect to broker", Toast.LENGTH_SHORT).show();
//                isConnectedToBroker = false;
//                ConnectToBrokerActivity.connectionStatusLed.setCardBackgroundColor(disconnectedColorInt);
//            }
//
//            try {
//
//                //connect to HiveMQ Cloud with TLS and username/pw
//                CompletableFuture<Mqtt5ConnAck> connAckFuture = mqttClient.connectWith()
//                        .simpleAuth()
//                        .username("iotdashboard")
//                        .password(UTF_8.encode("*******"))
//                        .applySimpleAuth()
//                        .keepAlive(60)
//                        .send();
//                //Log.i("MQTT_Cxx", connAckMessage.toString());
//
//            } catch (Exception e) {
//
//                Toast.makeText(getApplicationContext(), "Unable to connect to broker", Toast.LENGTH_SHORT).show();
//                isConnectedToBroker = false;
//                ConnectToBrokerActivity.connectionStatusLed.setCardBackgroundColor(disconnectedColorInt);
//            }



//        try {
//
//            //connect to HiveMQ Cloud with TLS and username/pw
//            CompletableFuture<Mqtt5ConnAck> connAckFuture = ConnectToBrokerActivity.mqttClient.connectWith()
//                    .simpleAuth()
//                    .username(ConnectToBrokerActivity.username)
//                    .password(UTF_8.encode(ConnectToBrokerActivity.password))
//                    .applySimpleAuth()
//                    .keepAlive(Integer.parseInt(ConnectToBrokerActivity.keepAlive))
//                    .send();
//            //Log.i("MQTT_Cxx", connAckMessage.toString());
//
//        } catch (Exception e) {
//
//            Toast.makeText(getApplicationContext(), "Unable to connect to broker", Toast.LENGTH_SHORT).show();
//            ConnectToBrokerActivity.isConnectedToBroker = false;
//        }


//        SSLContext sslContext = SSLContext.getInstance("SSL");
//        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//        KeyStore keyStore = readKeyStore();
//        trustManagerFactory.init(keyStore);
//        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
//
//        options.setSocketFactory(sslContext.getSocketFactory());




        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName("iotdashboard");
        mqttConnectOptions.setPassword("*******".toCharArray());
        mqttConnectOptions.setKeepAliveInterval(60);
        //mqttConnectOptions.setAutomaticReconnect(true);



//        InputStream input = null;
//        try {
//            input = this.getApplicationContext().getAssets().open("iot.eclipse.org.bks");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            mqttConnectOptions.setSocketFactory(mqttAndroidClient.getSSLSocketFactory(input, "eclipse-password"));
//        } catch (MqttSecurityException e) {
//            e.printStackTrace();
//        }


        /* Create an MqttAndroidClient object and configure the callback. */

        //mqttAndroidClient = new MqttAndroidClient(getApplicationContext(),"ssl://4e33b147c0024181ae5078b42e499ad9.s1.eu.hivemq.cloud:8883", "khaled-kenaan@hotmail.com", persistence2);
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(),"tcp://www.mqtt-dashboard.com:1883", "khalid-kenan@gmail.com", persistence2);

        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i("TAGX", "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i("TAGX", "topic: " + topic + ", msg: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i("TAGX", "msg delivered");
            }
        });

        /* Establish a connection to IoT Platform by using MQTT. */
        try {
           IMqttToken token =  mqttAndroidClient.connect(mqttConnectOptions);

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("TAGX", "onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("TAGX", "onFailure");

                }
            });


        } catch (MqttException e) {
            e.printStackTrace();
        }



//        MainActivity.sharedPreferences.edit().putBoolean(USER_INTENDS_TO_CONNECT, true).apply();


        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setContentTitle("MQTT Connection")
                .setContentIntent(pendingIntent)
                .build();

        startForeground(10, notification);

        return START_STICKY;


    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(!(mqttAndroidClient==null) && mqttAndroidClient.isConnected()){

            try {
                mqttAndroidClient.disconnect();

            } catch (MqttException e) {
                e.printStackTrace();
            }
//        MainActivity.sharedPreferences.edit().putBoolean(USER_INTENDS_TO_CONNECT, false).apply();

        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

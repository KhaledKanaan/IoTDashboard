package com.kandroid.iotdashboard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientTransportConfig;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientConfig;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientConnectionConfig;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import static com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL;

import static com.kandroid.iotdashboard.MainActivity.USER_DB_ROOT;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.Optional;

public class ConnectToBrokerActivity extends AppCompatActivity {

    public EditText brokerUrlEditText, clientIdEditText, userNameEditText, passwordEditText, keepAliveEditText, lasWillTopicEditText, lastWillMessageEditText, portEditText;
    public static MaterialButton connectToBrokerButton;
    public static String brokerUrl, clientId, username, password, keepAlive, lasWillTopic, lastWillMessage, port, defaultQoS, mqttVersion,
            LAST_MQTT_CONNEX_SUCCEEDED = "Last_MQTT_Connex_Succeeded", BROKER_SETTINGS = "BrokerSettings",
            BROKER_URL = "BrokerUrl", PORT = "Port", CLIENT_ID = "ClientId", USERNAME = "Username", PASSWORD = "Password", CONNECT_WITH_TLS = "ConnectWithTLS",
            KEEP_ALIVE = "KeepAlive", LAST_WILL_TOPIC = "LastWillTopic", LAST_WILL_MASSAGE = "LastWillMessage",
            DEFAULT_QoS = "DefaultQoS", MQTT_VERSION = "MQTT_Version", RETAIN = "Retain", SUCCEEDED = "true", NOT_SUCCEEDED = "false", WITH_TLS = "true", WITHOUT_TLS = "false",
            MQTT_3_1_1 = "MQTT 3.1.1", MQTT_5 = "MQTT 5", QOS_0 = "0", QOS_1 = "1", QOS_2 = "2";
    public static AutoCompleteTextView defaultQoSAutoCompleteTextView, mqttVersionAutoCompleteTextView;
    public static CheckBox retainCheckBox, tlsCheckBox;
    public static TextInputLayout hostUrlTextInputLayout, portTextInputLayout, clientIdTextInputLayout, usernameTextInputLayout, passwordTextInputLayout, keepAliveTextInputLayout;
    public static boolean connectWithTls = true, retain = false, isConnectedToBroker = false;
    public static ArrayList<String> qosArrayList = new ArrayList<String>() {{add(QOS_0); add(QOS_1); add(QOS_2);}};
    public static ArrayList<String> mqttVersionArrayList = new ArrayList<String>() {{add(MQTT_3_1_1); add(MQTT_5);}};
    public static ArrayAdapter<String> qosArrayAdapter, mqttVersionArrayAdapter;
    public static Mqtt5AsyncClient mqttClient;
    public static CardView connectionStatusLed;
    public static MqttAndroidClient mqttAndroidClient;

    public static int connectedColorInt = Color.parseColor("#00AA00");
    public static int disconnectedColorInt = Color.parseColor("#000000");


    public static boolean connectToBroker(String mqttVersion, String brokerUrl, String port, String clientId, String username,
                                       String password, String keepAlive, String QoS, String lasWillTopic, String lastWillMessage, boolean TLS, Context context) {


        if(mqttVersion.equals(MQTT_5) && TLS ) {

            try {
                //create an MQTT client
                mqttClient = MqttClient.builder()
                        .useMqttVersion5()
                        .serverHost(brokerUrl)
                        .serverPort(Integer.valueOf(port))
                        .sslWithDefaultConfig()
                        .automaticReconnectWithDefaultConfig()
                        .identifier(clientId)
                        .addConnectedListener(new MqttClientConnectedListener() {
                            @Override
                            public void onConnected(MqttClientConnectedContext context) {

                                Log.i("MQTT_Cx", "connected");
                                isConnectedToBroker = true;
                                connectionStatusLed.setCardBackgroundColor(connectedColorInt);

                                MainActivity.sharedPreferences.edit().putBoolean(LAST_MQTT_CONNEX_SUCCEEDED, true).apply();

                                DatabaseReference IoT_Database = FirebaseDatabase.getInstance().getReference(USER_DB_ROOT).child(BROKER_SETTINGS);
                                IoT_Database.child(BROKER_URL).setValue(brokerUrl);
                                IoT_Database.child(PORT).setValue(port);
                                IoT_Database.child(USERNAME).setValue(username);
                                IoT_Database.child(PASSWORD).setValue(password);
                                IoT_Database.child(CLIENT_ID).setValue(clientId);
                                IoT_Database.child(KEEP_ALIVE).setValue(keepAlive);
                                IoT_Database.child(DEFAULT_QoS).setValue(defaultQoS);
                                IoT_Database.child(CONNECT_WITH_TLS).setValue(String.valueOf(connectWithTls));
                                IoT_Database.child(MQTT_VERSION).setValue(mqttVersion);
                                IoT_Database.child(RETAIN).setValue(retain);
                                IoT_Database.child(LAST_WILL_TOPIC).setValue(lasWillTopic);
                                IoT_Database.child(LAST_WILL_MASSAGE).setValue(lastWillMessage);
                                IoT_Database.child(LAST_MQTT_CONNEX_SUCCEEDED).setValue(SUCCEEDED);


                            }
                        })
                        .addDisconnectedListener(new MqttClientDisconnectedListener() {
                            @Override
                            public void onDisconnected(MqttClientDisconnectedContext context) {
                                Log.i("MQTT_Cx", "disconnected");
                                //Toast.makeText(getApplicationContext(), "disconnected", Toast.LENGTH_LONG).show;
                                isConnectedToBroker = false;
                                connectionStatusLed.setCardBackgroundColor(disconnectedColorInt);

                                MainActivity.sharedPreferences.edit().putBoolean(LAST_MQTT_CONNEX_SUCCEEDED, true).apply();
                                DatabaseReference IoT_Database = FirebaseDatabase.getInstance().getReference(USER_DB_ROOT).child(BROKER_SETTINGS);
                                IoT_Database.child(LAST_MQTT_CONNEX_SUCCEEDED).setValue(SUCCEEDED);

                            }
                        })
                        .buildAsync();

            } catch (Exception e) {
                Toast.makeText(context, "Unable to connect to broker", Toast.LENGTH_SHORT).show();
                isConnectedToBroker = false;
            }

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

        }
        return isConnectedToBroker;



//        }else if(mqttVersion.equals(MQTT_5) && !(TLS)){
//            //create an MQTT client
//            mqttClient = MqttClient.builder()
//                    .useMqttVersion5()
//                    .serverHost(brokerUrl)
//                    .serverPort(Integer.valueOf(port))
//                    .buildBlocking();
//
//            //connect to HiveMQ Cloud with TLS and username/pw
//            mqttClient.connectWith()
//                    .simpleAuth()
//                    .username(username)
//                    .password(UTF_8.encode(password))
//                    .applySimpleAuth()
//                    .send();
//
//
//        }


    }

    public void readParametersAndConnectToBroker(View view){

        brokerUrl = brokerUrlEditText.getText().toString().trim();
        clientId = clientIdEditText.getText().toString().trim();
        username = userNameEditText.getText().toString().trim();
        password = passwordEditText.getText().toString().trim();
        keepAlive = keepAliveEditText.getText().toString().trim();
        lasWillTopic = lasWillTopicEditText.getText().toString().trim();
        lastWillMessage = lastWillMessageEditText.getText().toString().trim();
        port = portEditText.getText().toString().trim();
        defaultQoS = defaultQoSAutoCompleteTextView.getText().toString().trim();
        mqttVersion = mqttVersionAutoCompleteTextView.getText().toString().trim();
        connectWithTls = tlsCheckBox.isChecked();
        retain = retainCheckBox.isChecked();

        boolean error = false;
        int errorColorInt = Color.parseColor("#FDE1E1");

        if(brokerUrl.isEmpty()){

            hostUrlTextInputLayout.setBoxBackgroundColor(errorColorInt);
            brokerUrlEditText.setError("URL is required");
            //brokerUrlEditText.requestFocus();
            error = true;

        }else if(!Patterns.WEB_URL.matcher(brokerUrl).matches()){

            hostUrlTextInputLayout.setBoxBackgroundColor(errorColorInt);
            brokerUrlEditText.setError("Invalid URL");
            //brokerUrlEditText.requestFocus();
            error = true;
        }

        if(port.isEmpty()){

            portTextInputLayout.setBoxBackgroundColor(errorColorInt);
            portEditText.setError("Port is required");
            //userNameEditText.requestFocus();
            error = true;
        }

        if(username.isEmpty()){

            usernameTextInputLayout.setBoxBackgroundColor(errorColorInt);
            userNameEditText.setError("Username is required");
            //userNameEditText.requestFocus();
            error = true;
        }

        if(password.isEmpty()){

            passwordTextInputLayout.setBoxBackgroundColor(errorColorInt);
            passwordEditText.setError("Password is required");
            //userNameEditText.requestFocus();
            error = true;
        }

        if (error) {
            return;
        }


//        connectToBroker(mqttVersion, brokerUrl, port, clientId, username,
//                password, keepAlive, defaultQoS, lasWillTopic, lastWillMessage, connectWithTls, this);

//        String packageName = getApplicationContext().getPackageName();
        Intent intent = new Intent( getApplicationContext(), KeepAliveMqttConnectionService.class);
//        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//        intent.setData(Uri.parse("package:" + packageName));
//
        startForegroundService(intent);




//        Mqtt5ClientConfig config = mqttClient.getConfig();
//        //config.getClientIdentifier();
//        Optional<Mqtt5ClientConnectionConfig> connectionConfig = config.getConnectionConfig();
//        if (isConnectedToBroker) {
//            //isConnectedToBroker = true;
//            stopService(new Intent(getApplicationContext(), KeepAliveMqttConnectionService.class));
//            //MqttClientTransportConfig transportConfig = connectionConfig.get().getTransportConfig();
//
//        }else {
//            //isConnectedToBroker = false;
//            startService(new Intent(getApplicationContext(), KeepAliveMqttConnectionService.class));
//        }
        //isConnectedToBroker = false;

//        if(isConnectedToBroker){
//            stopService(new Intent(getApplicationContext(), KeepAliveMqttConnectionService.class));
//
//
//        }else {
//            startService(new Intent(getApplicationContext(), KeepAliveMqttConnectionService.class));
//
//        }


//        final String host = "4e33b147c002418*****78b42e499ad9.s1.eu.hivemq.cloud";
//        final String username = "iotdashboard";
//        final String password = "*******";
//
//        //create an MQTT client
//        final Mqtt5BlockingClient client = MqttClient.builder()
//                .useMqttVersion5()
//                .serverHost(host)
//                .serverPort(8883)
//                .sslWithDefaultConfig()
//                .buildBlocking();
//
//        //connect to HiveMQ Cloud with TLS and username/pw
//        client.connectWith()
//                .simpleAuth()
//                .username(username)
//                .password(UTF_8.encode(password))
//                .applySimpleAuth()
//                .send();
//
//        System.out.println("Connected successfully");
//
//        //subscribe to the topic "my/test/topic"
//        client.subscribeWith()
//                .topicFilter("my/test/topic")
//                .send();
//
//        //set a callback that is called when a message is received (using the async API style)
//        client.toAsync().publishes(ALL, publish -> {
//            System.out.println("Received message: " + publish.getTopic() + " -> " + UTF_8.decode(publish.getPayload().get()));
//
//            //disconnect the client after a message was received
//            client.disconnect();
//        });
//
//        //publish a message to the topic "my/test/topic"
//        client.publishWith()
//                .topic("my/test/topic")
//                .payload(UTF_8.encode(dataToSend))
//                .send();
//
//
//        Toast.makeText(this, brokerURL, Toast.LENGTH_LONG).show();


//        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
//        mqttConnectOptions.setUserName(username);
//        mqttConnectOptions.setPassword(password.toCharArray());
//
//
//        /* Create an MqttAndroidClient object and configure the callback. */
//
//        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(),"ssl://4e33b147c0024181ae5078b42e499ad9.s1.eu.hivemq.cloud:8883", clientId);
//        mqttAndroidClient.setCallback(new MqttCallback() {
//            @Override
//            public void connectionLost(Throwable cause) {
//                Log.i("TAGX", "connection lost");
//            }
//
//            @Override
//            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                Log.i("TAGX", "topic: " + topic + ", msg: " + new String(message.getPayload()));
//            }
//
//            @Override
//            public void deliveryComplete(IMqttDeliveryToken token) {
//                Log.i("TAGX", "msg delivered");
//            }
//        });
//
//        /* Establish a connection to IoT Platform by using MQTT. */
//        try {
//            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    Log.i("TAGX", "connect succeed");
//
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    Log.i("TAG", "connect failed");
//                    Log.i("TAG", exception.toString());
//                }
//            });
//
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }

    }

    public void disconnectFromBroker (View v){

        stopService(new Intent(getApplicationContext(), KeepAliveMqttConnectionService.class));
//        try {
//            mqttAndroidClient.disconnect();
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }

    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        if(KeepAliveMqttConnectionService.mqttClient.getConfig().getConnectionConfig().isPresent()){
//            connectionStatusLed.setCardBackgroundColor(connectedColorInt);
//        }else connectionStatusLed.setCardBackgroundColor(disconnectedColorInt);
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_to_broker);

        brokerUrlEditText = (EditText) findViewById(R.id.hostUrlEditText);
        clientIdEditText = (EditText) findViewById(R.id.clientIdEditText);
        userNameEditText = (EditText) findViewById(R.id.usernameEditText);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        keepAliveEditText = (EditText) findViewById(R.id.keepAliveEditText);
        lasWillTopicEditText = (EditText) findViewById(R.id.lastWillTopicEditText);
        lastWillMessageEditText = (EditText) findViewById(R.id.lastWillMessageEditText);
        retainCheckBox = (CheckBox) findViewById(R.id.retainCheckBox);
        tlsCheckBox = (CheckBox) findViewById(R.id.tlsCheckBox);
        portEditText = (EditText) findViewById(R.id.portEditText);
        defaultQoSAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.defaultQoSAutoCompleteTextView);
        mqttVersionAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.mqttVersionAutoCompleteTextView);
        connectToBrokerButton = (MaterialButton) findViewById(R.id.connectToBrokerButton);

        qosArrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, qosArrayList);
        defaultQoSAutoCompleteTextView.setAdapter(qosArrayAdapter);

        mqttVersionArrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, mqttVersionArrayList);
        mqttVersionAutoCompleteTextView.setAdapter(mqttVersionArrayAdapter);

        hostUrlTextInputLayout = (TextInputLayout) findViewById(R.id.hostUrlTextInputLayout);
        clientIdTextInputLayout = (TextInputLayout) findViewById(R.id.clientIdTextInputLayout);
        usernameTextInputLayout = (TextInputLayout) findViewById(R.id.usernameTextInputLayout);
        passwordTextInputLayout = (TextInputLayout) findViewById(R.id.passwordTextInputLayout);
        keepAliveTextInputLayout = (TextInputLayout) findViewById(R.id.keepAliveTextInputLayout);
        portTextInputLayout = (TextInputLayout) findViewById(R.id.portTextInputLayout);

        connectionStatusLed = (CardView) findViewById(R.id.connectionStatusLed);

//        if(isConnectedToBroker) {
//            connectionStatusLed.setCardBackgroundColor(connectedColorInt);
//        }else connectionStatusLed.setCardBackgroundColor(disconnectedColorInt);

//        if(KeepAliveMqttConnectionService.mqttClient.getConfig().getConnectionConfig().isPresent()){
//            connectionStatusLed.setCardBackgroundColor(connectedColorInt);
//        }else connectionStatusLed.setCardBackgroundColor(disconnectedColorInt);

    }

}
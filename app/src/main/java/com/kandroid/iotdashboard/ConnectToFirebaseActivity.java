package com.kandroid.iotdashboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.installations.Utils;
import com.obsez.android.lib.filechooser.ChooserDialog;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

import static android.os.Environment.getExternalStorageDirectory;
import static android.os.Environment.getExternalStoragePublicDirectory;


public class ConnectToFirebaseActivity extends AppCompatActivity {

    private EditText databaseUrlEditText, apiKeyEditText, applicationIdEditText;
    private String TAG = "ConnectToFirebaseActivity";
    public static Button connectionButton, browseForJsonFileButton;
    public boolean isConnected = false, connectedToTheInternet = false;
    private final int PERMISSION_REQUEST_CODE =1000, READ_REQUEST_CODE = 42;
    public ConnectivityManager connectivityManager;
    public ConnectivityManager.NetworkCallback networkCallback;
    public static SharedPreferences sharedPreferences;

    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream =
                     getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    private void searchFile(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                String jsonData = null;
                try {
                    jsonData = readTextFromUri(uri);

                    if(!(jsonData==null) && !(jsonData.isEmpty())){
                        String finalJsonData = jsonData;
                        new Handler(Looper.getMainLooper()).postDelayed(
                                new Runnable() {
                                    public void run() {
                                        //get application Id:
                                        String applicationId = StringUtils.substringBetween(finalJsonData,
                                                "mobilesdk_app_id\":", ",");
                                        if(!(applicationId == null) && !(applicationId.isEmpty())){
                                            applicationId = applicationId
                                                    .replaceAll(",", "")
                                                    .replaceAll("\"", "")
                                                    .replaceAll("\\}", "")
                                                    .replaceAll("\\{", "")
                                                    .replaceAll("\\[", "")
                                                    .replaceAll("\\]", "")
                                                    .trim();
                                            applicationIdEditText.setText(applicationId);
                                        }

                                        //get database Url:
                                        String databaseUrl = StringUtils.substringBetween(finalJsonData,
                                                "firebase_url\":", ",");
                                        if(!(databaseUrl == null) && !(databaseUrl.isEmpty())){
                                            databaseUrl = databaseUrl
                                                    .replaceAll(",", "")
                                                    .replaceAll("\"", "")
                                                    .replaceAll("\\}", "")
                                                    .replaceAll("\\{", "")
                                                    .replaceAll("\\[", "")
                                                    .replaceAll("\\]", "")
                                                    .trim();
                                            databaseUrlEditText.setText(databaseUrl);

                                        }

                                        //get api key:
                                        String apiKey = StringUtils.substringBetween(finalJsonData,
                                                "current_key\":", ",");
                                        if(!(apiKey == null) && !(apiKey.isEmpty())){
                                            apiKey = apiKey
                                                    .replaceAll(",", "")
                                                    .replaceAll("\"", "")
                                                    .replaceAll("\\}", "")
                                                    .replaceAll("\\{", "")
                                                    .replaceAll("\\[", "")
                                                    .replaceAll("\\]", "")
                                                    .trim();
                                            apiKeyEditText.setText(apiKey);
                                        }
                                    }
                                },
                                200);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void browseJsonFile(View v){
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkPermission()) {
                    searchFile();
                } else {
                    requestPermission(); // Code for permission
                }
            } else {
                searchFile();
            }
        }
    }

    public void connectDisconnectToFirebase(View v){

        String databaseUrl = databaseUrlEditText.getText().toString().trim();
        String apiKey = apiKeyEditText.getText().toString().trim();
        String applicationId = applicationIdEditText.toString().trim();

        if(isConnected){
            disconnectFromFireBaseDataBase();
        }
        else{
            if(!connectedToTheInternet) {
                Toast.makeText(ConnectToFirebaseActivity.this, "No internet connection", Toast.LENGTH_LONG).show();
            }else connectToFireBaseDataBase(databaseUrl, apiKey, applicationId);
        }
    }

    public void getConnectionToFireBaseDataBaseStatus(){

        if (!FirebaseApp.getApps(getApplicationContext()).isEmpty()){
            DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

            if(connectedRef!=null){
                connectedRef.addValueEventListener(new ValueEventListener(){
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isConnected = snapshot.getValue(Boolean.class);
                        boolean connected = snapshot.getValue(Boolean.class);
                        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                                new Runnable() {
                                    public void run() {
                                        if (connected) {

                                            sharedPreferences.edit().putBoolean(LauncherActivity.INTENT_TO_CONNECT, true).apply();

                                            CardView cr = findViewById(R.id.firebaseConnectionLed);
                                            cr.setCardBackgroundColor(getResources().getColor(R.color.color_8, getTheme()));

                                            connectionButton.setText("Disconnect");
                                            enableViews(false);

                                            String databaseUrl = databaseUrlEditText.getText().toString().trim();
                                            String apiKey =  apiKeyEditText.getText().toString().trim();
                                            String applicationId =  applicationIdEditText.getText().toString().trim();

                                            sharedPreferences.edit().putString(LauncherActivity.DATABASE_URL, databaseUrl).apply();
                                            sharedPreferences.edit().putString(LauncherActivity.API_KEY, apiKey).apply();
                                            sharedPreferences.edit().putString(LauncherActivity.APPLICATION_ID, applicationId).apply();

                                            Toast.makeText(getApplicationContext(),"Successfully connected to Firebase", Toast.LENGTH_SHORT).show();

                                            new android.os.Handler(Looper.getMainLooper()).postDelayed(
                                                    new Runnable() {
                                                        public void run() {
                                                            Intent intent = new Intent(ConnectToFirebaseActivity.this, MainActivity.class);
                                                            startActivity(intent);
                                                        }
                                                    },
                                                    800);
                                        }
                                        else {
                                            CardView cr = findViewById(R.id.firebaseConnectionLed);
                                            cr.setCardBackgroundColor(getResources().getColor(R.color.black, getTheme()));

                                            connectionButton.setText("Connect");
                                            enableViews(true);
                                            //Toast.makeText(ConnectToFirebaseActivity.this, "Unable to connect to Firebase", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                },
                                500);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }else {
            CardView cr = findViewById(R.id.firebaseConnectionLed);
            cr.setCardBackgroundColor(getResources().getColor(R.color.black, getTheme()));

            connectionButton.setText("Connect");
            enableViews(true);
            Toast.makeText(ConnectToFirebaseActivity.this, "Initialize the FirebaseApp first", Toast.LENGTH_LONG).show();
        }
    }

    public void firstGetConnectionToFireBaseDataBaseStatus(){

        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()){
            DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
            connectedRef.addValueEventListener(new ValueEventListener(){
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    isConnected = snapshot.getValue(Boolean.class);
                    boolean connected = snapshot.getValue(Boolean.class);
                    CardView cr = findViewById(R.id.firebaseConnectionLed);
                    if (connected) {
                        cr.setCardBackgroundColor(getResources().getColor(R.color.color_8, getTheme()));
                        connectionButton.setText("Disconnect");
                        enableViews(false);
                    }
                    else {
                        cr.setCardBackgroundColor(getResources().getColor(R.color.black, getTheme()));
                        connectionButton.setText("Connect");
                        enableViews(true);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    public void disconnectFromFireBaseDataBase(){

        sharedPreferences.edit().putBoolean(LauncherActivity.INTENT_TO_CONNECT, false).apply();
        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()){
            //Log.i("flexx1", FirebaseApp.getApps(getApplicationContext()).toString());
            try {
                FirebaseDatabase.getInstance().goOffline();
                getConnectionToFireBaseDataBaseStatus();

                new android.os.Handler(Looper.getMainLooper()).postDelayed(
                        new Runnable() {
                            public void run() {
                                //FirebaseApp.getInstance().delete();
                                //Log.i("flexx2", FirebaseApp.getApps(getApplicationContext()).toString());

                            }
                        },
                        500);

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public void initializeFirebaseApp(){

    }

    public void connectToFireBaseDataBase(String databaseUrl, String apiKey, String applicationId){
        //        String registeredDatabaseUrl = sharedPreferences.getString(LauncherActivity.DATABASE_URL, LauncherActivity.DEFAULT_DATABASE_URL);
        //        String registeredApiKey = sharedPreferences.getString(LauncherActivity.API_KEY, LauncherActivity.DEFAULT_API_KEY);
        //        String registeredApplicationId = sharedPreferences.getString(LauncherActivity.APPLICATION_ID, LauncherActivity.DEFAULT_APPLICATION_ID);

        //        if(!databaseUrl.equals(registeredDatabaseUrl) || !apiKey.equals(registeredApiKey) || !applicationId.equals(registeredApplicationId)){

        //Log.i("flexx3", FirebaseApp.getApps(getApplicationContext()).toString());
        try{
            if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
                FirebaseApp.getInstance().delete();
            }
        }
        catch(Exception e){
            Log.e(TAG, e.toString());
        }

        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    public void run() {
                        try {
                            FirebaseOptions options = new FirebaseOptions.Builder()
                                    .setDatabaseUrl(databaseUrl)
                                    .setApiKey(apiKey)
                                    .setApplicationId(applicationId)
                                    //.setProjectId(ProjectId)
                                    .build();
                            FirebaseApp.initializeApp(getApplicationContext(), options);

                            //Log.i("flexx4", FirebaseApp.getApps(getApplicationContext()).toString());

                            new android.os.Handler(Looper.getMainLooper()).postDelayed(
                                    new Runnable() {
                                        public void run() {
                                            maintainTheConnectionToFirebase();
                                            getConnectionToFireBaseDataBaseStatus();
                                        }
                                    },
                                    500);


                        }catch (Exception e){
                            Log.e(TAG, e.toString());
                            if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
                                //try{FirebaseApp.getInstance().delete();}catch(Exception ex){Log.e(TAG, ex.toString());}
                            }
                            Toast.makeText(ConnectToFirebaseActivity.this, "Insert correct Database URL, API Key and App. ID", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                200);


//        new android.os.Handler(Looper.getMainLooper()).postDelayed(
//                new Runnable() {
//                    public void run() {
//                        maintainTheConnectionToFirebase();
//                        getConnectionToFireBaseDataBaseStatus();
//                    }
//                },
//                1000);

        //}
//        else{
//            if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()){
//                FirebaseDatabase.getInstance().goOnline();
//                maintainTheConnectionToFirebase();
//            }
//        }
    }

    public void enableViews(Boolean enable){
        databaseUrlEditText.setEnabled(enable);
        apiKeyEditText.setEnabled(enable);
        applicationIdEditText.setEnabled(enable);
        browseForJsonFileButton.setEnabled(enable);

        if(enable){
            browseForJsonFileButton.setBackgroundColor(getResources().getColor(R.color.color_1, getTheme()));
        }
        else browseForJsonFileButton.setBackgroundColor(getResources().getColor(R.color.color_5, getTheme()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_to_firebase);

        Toolbar toolbar = (Toolbar) findViewById(R.id.FirebaseConnectionToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        sharedPreferences = getApplicationContext().getSharedPreferences(LauncherActivity.PACKAGE_NAME, Context.MODE_PRIVATE);

        firstGetConnectionToFireBaseDataBaseStatus();
        boolean intentToConnect = sharedPreferences.getBoolean(LauncherActivity.INTENT_TO_CONNECT, false);
        if(intentToConnect){
            maintainTheConnectionToFirebase();
        }
        monitorInternetConnection();

        databaseUrlEditText = findViewById(R.id.databaseUrlEditText);
        apiKeyEditText = findViewById(R.id.apiKeyEditText);
        applicationIdEditText = findViewById(R.id.applicationIdEditText);

        String databaseUrl = sharedPreferences.getString(LauncherActivity.DATABASE_URL, LauncherActivity.DEFAULT_DATABASE_URL);
        String apiKey = sharedPreferences.getString(LauncherActivity.API_KEY, LauncherActivity.DEFAULT_API_KEY);
        String applicationId = sharedPreferences.getString(LauncherActivity.APPLICATION_ID, LauncherActivity.DEFAULT_APPLICATION_ID);

        databaseUrlEditText.setText(databaseUrl);
        apiKeyEditText.setText(apiKey);
        applicationIdEditText.setText(applicationId);

        connectionButton = findViewById(R.id.connectionToFirebaseButton);
        browseForJsonFileButton = findViewById(R.id.browseForJsonFileButton);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }catch (Exception e){
            Log.e(TAG,e.toString());
        }
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void maintainTheConnectionToFirebase(){
        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            DatabaseReference IoT_Database = FirebaseDatabase.getInstance().getReference().child(LauncherActivity.MAINTAINED_CONNECTION_POINT);
            if (IoT_Database != null) {
                IoT_Database.setValue(true);
                IoT_Database.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.i(TAG, LauncherActivity.MAINTAINED_CONNECTION_POINT +" > "+ dataSnapshot.getValue());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }
    }

    private boolean checkPermission() {
        int result = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    private void requestPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode==PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission Granted.");
            }else {
                Log.i(TAG, "Permission Denied.");
                finish();
            }
        }
    }

    public void monitorInternetConnection(){

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                MainActivity.connectedToTheInternet = true;
                connectedToTheInternet = true;
                CardView cr = findViewById(R.id.internetConnectionLed);
                cr.setCardBackgroundColor(getResources().getColor(R.color.color_8, getTheme()));

            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);

                MainActivity.connectedToTheInternet = false;
                connectedToTheInternet = false;
                CardView cr = findViewById(R.id.internetConnectionLed);
                cr.setCardBackgroundColor(getResources().getColor(R.color.black, getTheme()));
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
            }
        };

        connectivityManager = (ConnectivityManager) getSystemService(ConnectivityManager.class);
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
        //connectivityManager.requestNetwork(networkRequest, networkCallback);
    }

}
package com.kandroid.iotdashboard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LauncherActivity extends AppCompatActivity {

    public static SharedPreferences sharedPreferences ;
    public static final String USER_PASSWORD = "UserPassword", USER_EMAIL = "UserEmail", USER_ID = "UserID",
            USER_FIRST_NAME = "UserFirstName", USER_LAST_NAME = "UserLastName", LAST_LOGIN_SUCCESS = "LastLoginSuccess",
            PRIVATE = "private", SEMI_PUBLIC = "semiPublic", PUBLIC = "public", SERVER_TYPE = SEMI_PUBLIC, TAG="LauncherActivity",
            DATABASE_URL = "DatabaseUrl", API_KEY = "ApiKey", APPLICATION_ID = "ApplicationId", INTENT_TO_CONNECT = "intentToConnect",
            PACKAGE_NAME= "com.kandroid.iotdashboard", DEFAULT_DATABASE_URL = "", DEFAULT_API_KEY = "", DEFAULT_APPLICATION_ID = "",
            MAINTAINED_CONNECTION_POINT = "MaintainedConnectionPoint", INTENT_TO_LOGIN = "intentToLogin", DEFAULT_USER_EMAIL = "",
            DEFAULT_USER_PASSWORD = "";
    public static boolean lastLoginSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_launcher);

        sharedPreferences = getApplicationContext().getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);

        boolean intentToConnect = sharedPreferences.getBoolean(INTENT_TO_CONNECT, false);
        if(intentToConnect){
            connectToFireBaseDataBase();
        }

        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    public void run() {

                        if(SERVER_TYPE.equals(SEMI_PUBLIC) || SERVER_TYPE.equals(PRIVATE)){
                            Intent intent;
                            intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                        }

                        else if(SERVER_TYPE.equals(PUBLIC)){

                            lastLoginSuccess = sharedPreferences.getBoolean(LAST_LOGIN_SUCCESS, false);

                            Intent intent;
                            if(!lastLoginSuccess){
                                intent = new Intent(getApplicationContext(), LoginSemiPublicServerActivity.class);
                            }
                            else{
                                intent = new Intent(getApplicationContext(), MainActivity.class);
                            }
                            startActivity(intent);
                        }

                    }
                },
                1000);

    }

    public void connectToFireBaseDataBase(){

        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            try {
                FirebaseApp.getInstance().delete();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }

        String registeredDatabaseUrl = sharedPreferences.getString(LauncherActivity.DATABASE_URL, DEFAULT_DATABASE_URL);
        String registeredApiKey = sharedPreferences.getString(LauncherActivity.API_KEY, DEFAULT_API_KEY);
        String registeredApplicationId = sharedPreferences.getString(LauncherActivity.APPLICATION_ID, DEFAULT_APPLICATION_ID);

        if(!registeredDatabaseUrl.isEmpty() && !registeredApiKey.isEmpty() && !registeredApplicationId.isEmpty()){
            try {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setDatabaseUrl(registeredDatabaseUrl)
                        .setApiKey(registeredApiKey)
                        .setApplicationId(registeredApplicationId)
                        //.setProjectId(ProjectId)
                        .build();
                FirebaseApp.initializeApp(getApplicationContext(), options);
                //FirebaseDatabase.getInstance().setPersistenceEnabled(true);

                maintainTheConnectionToFirebase();

            }catch (Exception e){
                Log.e(TAG, e.toString());
                Toast.makeText(this, "Unable to connect to Firebase, it may be deleted or modified", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void maintainTheConnectionToFirebase(){

        if (!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            DatabaseReference IoT_Database = FirebaseDatabase.getInstance().getReference().child(MAINTAINED_CONNECTION_POINT);
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

    public void loginCurrentActiveUser(){
        if (!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {

            Log.i("flexx", "IN1");

            DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
            if(connectedRef!=null) {
                connectedRef.addListenerForSingleValueEvent(new ValueEventListener(){
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isConnected = snapshot.getValue(Boolean.class);
                        if (isConnected) {
                            Log.i("flexx", "IN2");
                            FirebaseAuth mAuth = FirebaseAuth.getInstance();
                            if(mAuth!=null){

                                Log.i("flexx", "IN3");
                                String emailAddress = sharedPreferences.getString(USER_EMAIL, DEFAULT_USER_EMAIL);
                                String password = sharedPreferences.getString(USER_PASSWORD, DEFAULT_USER_PASSWORD);

                                if(!emailAddress.isEmpty() && !password.isEmpty()){

                                    Log.i("flexx", "IN4");
                                    try {
                                        mAuth.signInWithEmailAndPassword(emailAddress, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if (task.isSuccessful()) {
                                                    Log.i("flexx", "IN5");
                                                    FirebaseUser currentUser = mAuth.getCurrentUser();

                                                    if(currentUser != null){
                                                        Log.i("flexx", "IN6");
                                                        if(!currentUser.isEmailVerified()){
                                                            Log.i("flexx", "IN7");
                                                            mAuth.signOut();
                                                            sharedPreferences.edit().putBoolean(LauncherActivity.INTENT_TO_LOGIN, false).apply();
                                                        }
                                                    }
                                                }
                                                else {
                                                    Log.i(TAG, task.getException().getMessage());
                                                }
                                            }
                                        });
                                    }catch (Exception e){
                                        Log.e(TAG, e.toString());
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }
    }
}
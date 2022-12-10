package com.kandroid.iotdashboard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class LoginSemiPublicServerActivity extends AppCompatActivity {

    public EditText emailEditText, passwordEditText;
    public String TAG = "LoginSemiPublicServerActivity";
    public static SharedPreferences sharedPreferences;
    public TextInputLayout loginEmailTextInputLayout, passwordTextInputLayout;

    public void goToSignUp(View view){
        Intent intent = new Intent(getApplicationContext(), SignUpInSemiPublicServerActivity.class);
        startActivity(intent);
    }

    public void forgetPassword(View view){

        loginEmailTextInputLayout.setError(null);
        passwordTextInputLayout.setError(null);

        if (!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
            if (connectedRef != null) {
                connectedRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isConnected = snapshot.getValue(Boolean.class);
                        if (isConnected){
                            FirebaseAuth mAuth = FirebaseAuth.getInstance();
                            if(mAuth != null) {
                                String emailAddress = emailEditText.getText().toString().trim();

                                if (emailAddress.isEmpty()) {
                                    //emailEditText.setError("Email is required");
                                    //emailEditText.requestFocus();

                                    loginEmailTextInputLayout.setError("Email is required");
                                    return;
                                }

                                if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
                                    //emailEditText.setError("Invalid email address");
                                    //emailEditText.requestFocus();

                                    loginEmailTextInputLayout.setError("Invalid email address");
                                    return;
                                }

                                try {
                                    mAuth.fetchSignInMethodsForEmail(emailAddress)
                                            .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                                                @Override
                                                public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                                                    if (task.isSuccessful()) {
                                                        boolean isNewUser = task.getResult().getSignInMethods().isEmpty();
                                                        if (isNewUser) {
                                                            //email does not exist
                                                            Toast.makeText(LoginSemiPublicServerActivity.this, "This email does not exist", Toast.LENGTH_LONG).show();
                                                        } else {
                                                            //email exists
                                                            try{
                                                                mAuth.sendPasswordResetEmail(emailAddress)
                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if (task.isSuccessful()) {
                                                                                    Toast.makeText(LoginSemiPublicServerActivity.this, "Email sent to " + emailAddress, Toast.LENGTH_LONG).show();
                                                                                }
                                                                            }
                                                                        });
                                                            }catch(Exception e){
                                                                Toast.makeText(LoginSemiPublicServerActivity.this, e.getMessage() + emailAddress, Toast.LENGTH_LONG).show();
                                                                Log.e(TAG, e.toString());
                                                            }
                                                        }
                                                    }
                                                }
                                            });
                                }catch (Exception e){
                                    Toast.makeText(LoginSemiPublicServerActivity.this, "Invalid email address", Toast.LENGTH_LONG).show();
                                    Log.e(TAG, e.toString());
                                }
                            }
                        }
                        else Toast.makeText(LoginSemiPublicServerActivity.this, "Please connect to Firebase first", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            } else Toast.makeText(LoginSemiPublicServerActivity.this, "Please connect to Firebase first", Toast.LENGTH_LONG).show();
        }else Toast.makeText(LoginSemiPublicServerActivity.this, "Please connect to Firebase first", Toast.LENGTH_LONG).show();
    }

    public void logIn(View view){

        loginEmailTextInputLayout.setError(null);
        passwordTextInputLayout.setError(null);

        if (!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
            if(connectedRef!=null) {
                connectedRef.addListenerForSingleValueEvent(new ValueEventListener(){
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isConnected = snapshot.getValue(Boolean.class);
                        if (isConnected) {
                            FirebaseAuth mAuth = FirebaseAuth.getInstance();
                            if(mAuth!=null){
                                String emailAddress = emailEditText.getText().toString().trim();
                                String password = passwordEditText.getText().toString().trim();

                                if(emailAddress.isEmpty()){
                                    //emailEditText.setError("Email is required");
                                    //emailEditText.requestFocus();

                                    loginEmailTextInputLayout.setError("Email is required");
                                    return;
                                }

                                if(!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()){
                                    //emailEditText.setError("Invalid email address");
                                    //emailEditText.requestFocus();

                                    loginEmailTextInputLayout.setError("Invalid email address");
                                    return;
                                }

                                if(password.isEmpty()){
                                    //passwordEditText.setError("Password is required");
                                    //passwordEditText.requestFocus();

                                    passwordTextInputLayout.setError("Password is required");
                                    return;
                                }

                                if(password.length()<6){
                                    //passwordEditText.setError("Enter at least 6 characters");
                                    //passwordEditText.requestFocus();

                                    passwordTextInputLayout.setError("Enter at least 6 characters");
                                    return;
                                }

                                try {
                                    mAuth.signInWithEmailAndPassword(emailAddress, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                FirebaseUser currentUser = mAuth.getCurrentUser();

                                                if(currentUser != null){
                                                    String UID = currentUser.getUid();
                                                    String Email = currentUser.getEmail();

                                                    sharedPreferences.edit().putString(LauncherActivity.USER_EMAIL, Email).apply();
                                                    sharedPreferences.edit().putString(LauncherActivity.USER_ID, UID).apply();
                                                    sharedPreferences.edit().putString(LauncherActivity.USER_PASSWORD, password).apply();
                                                    sharedPreferences.edit().putBoolean(LauncherActivity.LAST_LOGIN_SUCCESS, true).apply();

                                                    if(currentUser.isEmailVerified()){
                                                        sharedPreferences.edit().putBoolean(LauncherActivity.INTENT_TO_LOGIN, true).apply();
                                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                        startActivity(intent);
                                                    }
                                                    else  {
                                                        Toast.makeText(LoginSemiPublicServerActivity.this, "Please verify your email first", Toast.LENGTH_LONG).show();
                                                        mAuth.signOut();
                                                        sharedPreferences.edit().putBoolean(LauncherActivity.INTENT_TO_LOGIN, false).apply();
                                                    }
                                                }
                                            }
                                            else {
                                                Toast.makeText(LoginSemiPublicServerActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                Log.e(TAG, task.getException().getMessage());
                                                passwordTextInputLayout.setError(" ");
                                                loginEmailTextInputLayout.setError(" ");
                                            }
                                        }
                                    });
                                }catch (Exception e){
                                    Log.e(TAG, e.toString());
                                    Toast.makeText(LoginSemiPublicServerActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                    passwordTextInputLayout.setError(" ");
                                    loginEmailTextInputLayout.setError(" ");
                                }
                            }
                        } else Toast.makeText(LoginSemiPublicServerActivity.this, "Please connect to Firebase first", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }else Toast.makeText(LoginSemiPublicServerActivity.this, "Please connect to Firebase first", Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_semi_public_server);
        
        sharedPreferences = getApplicationContext().getSharedPreferences(LauncherActivity.PACKAGE_NAME, Context.MODE_PRIVATE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.LoginToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        emailEditText = (EditText) findViewById(R.id.loginEmailEditText);
        passwordEditText = (EditText) findViewById(R.id.loginPasswordEditText);

        loginEmailTextInputLayout = (TextInputLayout) findViewById(R.id.loginEmailTextInputLayout);
        passwordTextInputLayout = (TextInputLayout) findViewById(R.id.passwordTextInputLayout);

        boolean intentToConnect = sharedPreferences.getBoolean(LauncherActivity.INTENT_TO_CONNECT, false);
        if(intentToConnect) maintainTheConnectionToFirebase();

        String emailAddress = sharedPreferences.getString(LauncherActivity.USER_EMAIL, null);
        if(emailAddress != null){
            emailEditText.setText(emailAddress);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void disconnectFromFireBaseDataBase(){

        sharedPreferences.edit().putBoolean(LauncherActivity.INTENT_TO_CONNECT, false).apply();
        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()){
            try {
                FirebaseDatabase.getInstance().goOffline();

                new android.os.Handler(Looper.getMainLooper()).postDelayed(
                        new Runnable() {
                            public void run() {
                                FirebaseApp.getInstance().delete();
                            }
                        },
                        800);

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public void maintainTheConnectionToFirebase(){

        if (!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
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
}
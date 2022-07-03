package com.kandroid.iotdashboard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class SignUpInSemiPublicServerActivity extends AppCompatActivity {

    public Button signUpButton;
    public EditText nameEditText, lastNameEditText,
            emailEditText, passwordEditText, confirmPasswordEditText;
    public String TAG = "SignUpInSemiPublicServerActivity";
    public static SharedPreferences sharedPreferences;

    public void signUp(View view){
        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()){
            DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
            if(connectedRef!=null) {
                    connectedRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean isConnected = snapshot.getValue(Boolean.class);
                            if (isConnected) {
                                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                                if (mAuth != null) {
                                    String name = nameEditText.getText().toString().trim();
                                    //String lastName = lastNameEditText.getText().toString().trim();
                                    String emailAddress = emailEditText.getText().toString().trim();
                                    String password = passwordEditText.getText().toString().trim();
                                    String retypedPassword = confirmPasswordEditText.getText().toString().trim();

                                    if (emailAddress.isEmpty()) {
                                        emailEditText.setError("Email is required");
                                        emailEditText.requestFocus();
                                        return;
                                    }

                                    if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
                                        emailEditText.setError("Invalid email address");
                                        emailEditText.requestFocus();
                                        return;
                                    }

                                    if (password.isEmpty()) {
                                        passwordEditText.setError("Password is required");
                                        passwordEditText.requestFocus();
                                        return;
                                    }

                                    if (password.length() < 6) {
                                        passwordEditText.setError("Enter at least 6 characters");
                                        passwordEditText.requestFocus();
                                        return;
                                    }

                                    if (retypedPassword.isEmpty()) {
                                        confirmPasswordEditText.setError("Password confirmation is required");
                                        confirmPasswordEditText.requestFocus();
                                        return;
                                    }

                                    if (!retypedPassword.equals(password)) {
                                        confirmPasswordEditText.setError("Re-typed password does not match");
                                        confirmPasswordEditText.requestFocus();
                                        return;
                                    }

                                    try {
                                        mAuth.createUserWithEmailAndPassword(emailAddress, password)
                                                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                                        if (task.isSuccessful()) {
                                                            final FirebaseUser user = mAuth.getCurrentUser();
                                                            if(user != null){

                                                                String UID = user.getUid();
                                                                String Email = user.getEmail();

                                                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                                        .setDisplayName(name).build();

                                                                user.updateProfile(profileUpdates);

                                                                sharedPreferences.edit().putString(LauncherActivity.USER_EMAIL, Email).apply();
                                                                sharedPreferences.edit().putString(LauncherActivity.USER_ID, UID).apply();
                                                                sharedPreferences.edit().putString(LauncherActivity.USER_PASSWORD, password).apply();
                                                                sharedPreferences.edit().putString(LauncherActivity.USER_FIRST_NAME, name).apply();
                                                                //sharedPreferences.edit().putString(LauncherActivity.USER_LAST_NAME, lastName).apply();

                                                                try {
                                                                    user.sendEmailVerification().addOnCompleteListener(SignUpInSemiPublicServerActivity.this, new OnCompleteListener() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task task) {

                                                                            if (task.isSuccessful()) {
                                                                                Toast.makeText(SignUpInSemiPublicServerActivity.this,
                                                                                        "Registered successfully. Verification email sent to: " + user.getEmail(),
                                                                                        Toast.LENGTH_LONG).show();

                                                                            } else {
                                                                                Toast.makeText(SignUpInSemiPublicServerActivity.this,
                                                                                        "Registered successfully. Failed to send verification email",
                                                                                        Toast.LENGTH_LONG).show();
                                                                            }
                                                                        }
                                                                    });
                                                                }catch(Exception e){
                                                                    Log.e(TAG, e.toString());
                                                                    Toast.makeText(SignUpInSemiPublicServerActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                                                }

                                                                new android.os.Handler(Looper.getMainLooper()).postDelayed(
                                                                        new Runnable() {
                                                                            public void run() {
                                                                                mAuth.signOut();
                                                                                sharedPreferences.edit().putBoolean(LauncherActivity.INTENT_TO_LOGIN, false).apply();
                                                                                Intent intent = new Intent(getApplicationContext(), LoginSemiPublicServerActivity.class);
                                                                                intent.putExtra(LauncherActivity.USER_EMAIL, emailAddress);
                                                                                startActivity(intent);
                                                                            }
                                                                        },
                                                                        500);
                                                            }
                                                        } else {
                                                            Toast.makeText(SignUpInSemiPublicServerActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                            Log.i(TAG, task.getException().getMessage().toString());
                                                        }
                                                    }
                                                });
                                    }catch (Exception e){
                                        Log.e(TAG, e.toString());
                                        Toast.makeText(SignUpInSemiPublicServerActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            } else Toast.makeText(SignUpInSemiPublicServerActivity.this, "Please connect to Firebase first", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_in_semi_public_server);
        
        sharedPreferences = getApplicationContext().getSharedPreferences(LauncherActivity.PACKAGE_NAME, Context.MODE_PRIVATE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.SignUpToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        nameEditText = (EditText) findViewById(R.id.nameEditText);
        //lastNameEditText = (EditText) findViewById(R.id.lastNameEditText);
        emailEditText = (EditText) findViewById(R.id.signUpEmailEditText);
        passwordEditText = (EditText) findViewById(R.id.signUpPasswordEditText);
        confirmPasswordEditText = (EditText) findViewById(R.id.retypeSignUpPasswordEditText);

        boolean intentToConnect = sharedPreferences.getBoolean(LauncherActivity.INTENT_TO_CONNECT, false);
        if(intentToConnect) maintainTheConnectionToFirebase();

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
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
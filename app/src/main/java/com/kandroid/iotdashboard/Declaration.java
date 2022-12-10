package com.kandroid.iotdashboard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.FirebaseApp;

public class Declaration extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    public void licenseAgreement(View view){
        Intent intent = new Intent(getApplicationContext(), EulaActivity.class);
        startActivity(intent);
    }

    public void privacyPolicy(View view){
        Intent intent = new Intent(getApplicationContext(), PrivacyPolicy.class);
        startActivity(intent);
    }

    public void agreeToConditions(View view){
        sharedPreferences.edit().putBoolean(LauncherActivity.CONDITIONS_AGREED, true).apply();
        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    public void run() {
                        Intent intent = new Intent(getApplicationContext(), LauncherActivity.class);
                        startActivity(intent);
                    }
                },
                100);

    }

    public void disagreeToConditions(View view){
        finishAffinity();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_declaration);
        sharedPreferences = getApplicationContext().getSharedPreferences(LauncherActivity.PACKAGE_NAME, Context.MODE_PRIVATE);
    }
}
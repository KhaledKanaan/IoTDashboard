package com.kandroid.iotdashboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Float.parseFloat;

public class AnalogTopicConfigurationActivity extends AppCompatActivity {

    public static String initialTopicTag, thingName, thingTag, TAG="Analog Topic Configuration Activity";

    public static AutoCompleteTextView useFeedbackNodeAutoCompleteTextView;
    private ArrayList<String> yesOrNo = new ArrayList<String>() {{add("Yes"); add("No");}};

    private String ERROR_NIL_HT_LL = "(Notify if <=) should be\n>= low limit", ERROR_NIH_LT_HL = "(Notify if >=) should be\n<= high limit",
            ERROR_NIH_HT_NIL = "(Notify if >=) should be\n> (Notify if <=)", ERROR_NIH_HT_LL = "(Notify if >=) should be\n>= low limit",
            ERROR_NIL_LT_HL = "(Notify if <=) should be\n<= high limit", ERROR_IV_HT_LL = "Initial value should be\n>= low limit",
            ERROR_IV_LT_HL = "Initial value should be\n<= high limit", ERROR_SV_HT_0 = "Step should be > 0",
            ERROR_SV_MAX_V = "Step should be <=\n(high limit - low limit)/2", ERROR_HL_HT_LL = "High limit should be\nhigher than low limit" ;

    public EditText topicEditText, topicNameEditText, initialValueEditText, stepEditText,
    unitEditText, highLimitEditText, lowLimitEditText, highLimitNotificationValueEditText,
    lowLimitNotificationValueEditText, feedbackValueEditText;

    public TextView thingDescriptionNameTextView, thingTagTextView, preTagTextView;

    public RadioButton ReadWriteRadioButton, ReadRadioButton;

    public boolean error, isTopicChanged;

    public static List<String> topicTagsList = new ArrayList<String>();

    public void saveConfigurationsToFirebase(DatabaseReference IoT_Database){

        error = false;
        String unit = unitEditText.getText().toString().trim();
        String topicName = topicNameEditText.getText().toString().trim();
        String topicTag = topicEditText.getText().toString().trim();
        String useFeedbackNode = useFeedbackNodeAutoCompleteTextView.getText().toString().trim();

        if(!(IoT_Database==null)){

            if(!(topicName.isEmpty())) {
                IoT_Database.child(thingTag).child(topicTag).child(Topics.DESCRIPTION).setValue(topicName);
            }else IoT_Database.child(thingTag).child(topicTag).child(Topics.DESCRIPTION).setValue(Topics.DEFAULT_DESCRIPTION);

            if(!(unit.isEmpty())){
                IoT_Database.child(thingTag).child(topicTag).child(Topics.UNIT).setValue(unit);
            }else IoT_Database.child(thingTag).child(topicTag).child(Topics.UNIT).setValue(Topics.DEFAULT_UNIT);

            if (ReadWriteRadioButton.isChecked()){
                IoT_Database.child(thingTag).child(topicTag).child(Topics.ACCESS).setValue(Topics.READWRITE);
            } else if (ReadRadioButton.isChecked()){
                IoT_Database.child(thingTag).child(topicTag).child(Topics.ACCESS).setValue(Topics.READ);
            }
            if(!(useFeedbackNode.isEmpty())){
                IoT_Database.child(thingTag).child(topicTag).child(Topics.USE_FEEDBACK_NODE).setValue(useFeedbackNode);
            }else IoT_Database.child(thingTag).child(topicTag).child(Topics.USE_FEEDBACK_NODE).setValue("No");

            IoT_Database.child(thingTag).child(topicTag).child(Topics.TYPE).setValue(Topics.ANALOG);

            //IoT_Database.child(thingTag).child(topicTag).child(Topics.FEEDBACK_NODE).setValue(Topics.DEFAULT_ANALOG_FEEDBACK_VALUE);

            manageHighAndLowLimits(IoT_Database);

            manageLowAndHighNotificationValues(IoT_Database);

            manageStepValue(IoT_Database);

            manageValue(IoT_Database);

            manageResults();

            if (isTopicChanged) {

                new android.os.Handler(Looper.getMainLooper()).postDelayed(
                        new Runnable() {
                            public void run() {
                                retrieveSettingsValuesFromFirebaseDatabase(topicTag);
                                isTopicChanged = false;
                            }
                        },
                        500);
            }
        }
    }

    private void getLastFeedbackValueAndSetItToTheNewTag(String oldTopicTag, String newTopicTag){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            IoT_Database.child(thingTag).child(oldTopicTag).child(Topics.FEEDBACK_NODE)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (!(dataSnapshot.getValue() == null)) {
                                String feedbackValue = dataSnapshot.getValue().toString();
                                if (feedbackValue != null) {
                                    IoT_Database.child(thingTag).child(newTopicTag).child(Topics.FEEDBACK_NODE).setValue(feedbackValue);
                                }else IoT_Database.child(thingTag).child(newTopicTag).child(Topics.FEEDBACK_NODE).setValue(Topics.DEFAULT_ANALOG_FEEDBACK_VALUE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
        }
    }

    public void saveConfigurations (View view){

        String topicTag = topicEditText.getText().toString().trim();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());

            if(topicTag.equals(initialTopicTag)){
                saveConfigurationsToFirebase(IoT_Database);
            }
            else{
                AlertDialog Dialog = new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Warning!")
                        .setMessage("You are about to change the topic tag, continue?")
                        .setPositiveButton(Html.fromHtml("<font color='black'>Yes</font>"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if(!(IoT_Database==null)){
                                    IoT_Database.child(thingTag).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            int count = 0;
                                            List<String> topicTagsList = new ArrayList<String>();

                                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) { // fetch all the topics tags from firebase DB and store them in a list
                                                topicTagsList.add(snapshot.getKey());
                                                count ++;
                                                if (count == dataSnapshot.getChildrenCount()){// make sure all topics tags are retrieved from the firebase DB before start listing them
                                                    if(topicTagsList.contains(topicTag)){ //tag is already used
                                                        Toast.makeText(getApplicationContext(),"\""+topicTag+"\""+" is already used", Toast.LENGTH_LONG).show();
                                                    }
                                                    else{
//                                                    saveConfigurationsToFirebase(IoT_Database);
//                                                    IoT_Database.child(thingTag).child(initialTopicTag).setValue(null);
//                                                    initialTopicTag = topicTag;

                                                        isTopicChanged = true;
                                                        getLastFeedbackValueAndSetItToTheNewTag(initialTopicTag, topicTag);
                                                        saveConfigurationsToFirebase(IoT_Database);

                                                        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                                                                new Runnable() {
                                                                    public void run() {
                                                                        IoT_Database.child(thingTag).child(initialTopicTag).setValue(null);
                                                                        initialTopicTag = topicTag;
                                                                    }
                                                                },
                                                                1000);
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
                        })
                        .setNegativeButton(Html.fromHtml("<font color='black'>Cancel</font>"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        }
    }

    public void manageResults(){

        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    public void run() {
                        if(!error) {
                            //topicEditText.setError(null);
                            //topicNameEditText.setError(null);
                            initialValueEditText.setError(null);
                            stepEditText.setError(null);
                            //unitEditText.setError(null);
                            lowLimitEditText.setError(null);
                            highLimitEditText.setError(null);
                            highLimitNotificationValueEditText.setError(null);
                            lowLimitNotificationValueEditText.setError(null);

                            Toast.makeText(AnalogTopicConfigurationActivity.this,"Configuration saved", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                500);

    }

    public void manageValue(DatabaseReference IoT_Database){

        String value = initialValueEditText.getText().toString().trim();
        String highLimitValue = highLimitEditText.getText().toString().trim();
        String lowLimitValue = lowLimitEditText.getText().toString().trim();
        String topicTag = topicEditText.getText().toString().trim();
        if(!(value.isEmpty())){
            float floatValue = parseFloat(value);
            if(!(lowLimitValue.isEmpty()) && !(highLimitValue.isEmpty())){
                float lowLimitFloatValue = parseFloat(lowLimitValue);
                float highLimitFloatValue = parseFloat(highLimitValue);
                if(floatValue <= highLimitFloatValue){
                    if(floatValue >= lowLimitFloatValue){
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(value);
                    }else {
                        initialValueEditText.setError(ERROR_IV_HT_LL);
                        initialValueEditText.requestFocus();
                        error = true;
                    }

                }else {
                    initialValueEditText.setError(ERROR_IV_LT_HL);
                    initialValueEditText.requestFocus();
                    error = true;
                }
            }
            else if((lowLimitValue.isEmpty()) && !(highLimitValue.isEmpty())){

                float highLimitFloatValue = parseFloat(highLimitValue);
                if(floatValue <= highLimitFloatValue){
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(value);
                }else {
                    initialValueEditText.setError(ERROR_IV_LT_HL);
                    initialValueEditText.requestFocus();
                    error = true;
                }
            }
            else if(!(lowLimitValue.isEmpty()) && (highLimitValue.isEmpty())){
                float lowLimitFloatValue = parseFloat(lowLimitValue);
                if(floatValue >= lowLimitFloatValue){
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(value);
                }else {
                    initialValueEditText.setError(ERROR_IV_HT_LL);
                    initialValueEditText.requestFocus();
                    error = true;
                }
            }
            else if((lowLimitValue.isEmpty()) && (highLimitValue.isEmpty())){

                IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(value);
                initialValueEditText.setText(value);
            }
        }
        else {
            if(!(lowLimitValue.isEmpty()) && !(highLimitValue.isEmpty()) || !(lowLimitValue.isEmpty()) && (highLimitValue.isEmpty())){

                float lowLimitFloatValue = Float.parseFloat(lowLimitValue);
                float floatValue = lowLimitFloatValue;
                value = String.valueOf(floatValue);
                IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(value);
                initialValueEditText.setText(value);
            }
            else if((lowLimitValue.isEmpty()) && !(highLimitValue.isEmpty())){

                float highLimitFloatValue = Float.parseFloat(highLimitValue);
                float floatValue = 0;
                if(highLimitFloatValue <= 0){
                    floatValue = highLimitFloatValue;
                }
                value = String.valueOf(floatValue);
                IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(floatValue);
                initialValueEditText.setText(value);
            }
            else if((lowLimitValue.isEmpty()) && (highLimitValue.isEmpty())){

                IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(Topics.DEFAULT_ANALOG_VALUE);
                initialValueEditText.setText(String.valueOf(Topics.DEFAULT_ANALOG_VALUE));

            }
        }
    }

    public void manageStepValue(DatabaseReference IoT_Database){

        String step = stepEditText.getText().toString().trim();
        String highLimitValue = highLimitEditText.getText().toString().trim();
        String lowLimitValue = lowLimitEditText.getText().toString().trim();
        String topicTag = topicEditText.getText().toString().trim();

        if(!(step.isEmpty())){
            float stepValue = Float.parseFloat(step);
            if (stepValue <= 0){
                stepEditText.setError(ERROR_SV_HT_0);
                stepEditText.requestFocus();
                error = true;
                return;
            }

            if(!(lowLimitValue.isEmpty()) && !(highLimitValue.isEmpty())){
                float lowLimitFloatValue = Float.parseFloat(lowLimitValue);
                float highLimitFloatValue = Float.parseFloat(highLimitValue);

                if(highLimitFloatValue > lowLimitFloatValue && stepValue <= ((highLimitFloatValue-lowLimitFloatValue)/2)){
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.STEP).setValue(step);
                }else {
                    stepEditText.setError(ERROR_SV_MAX_V);
                    stepEditText.requestFocus();
                    error = true;
//                    if(highLimitFloatValue > lowLimitFloatValue){
//                        stepValue = (highLimitFloatValue - lowLimitFloatValue)/10;
//                        IoT_Database.child(thingTag).child(topicTag).child(Topics.STEP).setValue(stepValue);
//                        stepEditText.setText(String.valueOf(stepValue));
//                    }
                }
            }
            else IoT_Database.child(thingTag).child(topicTag).child(Topics.STEP).setValue(step);

        } else if(!(lowLimitValue.isEmpty()) && !(highLimitValue.isEmpty())){
            float lowLimitFloatValue = Float.parseFloat(lowLimitValue);
            float highLimitFloatValue = Float.parseFloat(highLimitValue);
            if(highLimitFloatValue > lowLimitFloatValue){
                float stepValue = (highLimitFloatValue - lowLimitFloatValue)/10;
                step = String.valueOf(stepValue);
                IoT_Database.child(thingTag).child(topicTag).child(Topics.STEP).setValue(step);
                stepEditText.setText(String.valueOf(step));
            }
        } else {
            IoT_Database.child(thingTag).child(topicTag).child(Topics.STEP).setValue(Topics.DEFAULT_STEP);
            stepEditText.setText(String.valueOf(Topics.DEFAULT_STEP));
        }
    }

    public void manageHighAndLowLimits(DatabaseReference IoT_Database){

        String highLimitValue = highLimitEditText.getText().toString().trim();
        String lowLimitValue = lowLimitEditText.getText().toString().trim();
        String topicTag = topicEditText.getText().toString().trim();

        if(!(lowLimitValue.isEmpty()) && !(highLimitValue.isEmpty())){
            float lowLimitFloatValue = Float.parseFloat(lowLimitValue);
            float highLimitFloatValue = Float.parseFloat(highLimitValue);
            if(highLimitFloatValue > lowLimitFloatValue){
                IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT).setValue(highLimitValue);
                IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT).setValue(lowLimitValue);
            }else {
                highLimitEditText.setError(ERROR_HL_HT_LL);
                highLimitEditText.requestFocus();
                error = true;
            }
        }else if((lowLimitValue.isEmpty()) && !(highLimitValue.isEmpty())){
            float highLimitFloatValue = Float.parseFloat(highLimitValue);
            IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT).setValue(highLimitValue);
            IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT).setValue(Topics.DEFAULT_LOW_LIMIT);

        }else if(!(lowLimitValue.isEmpty()) && (highLimitValue.isEmpty())){
            float lowLimitFloatValue = Float.parseFloat(lowLimitValue);
            IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT).setValue(Topics.DEFAULT_HIGH_LIMIT);
            IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT).setValue(lowLimitValue);

        }else if((lowLimitValue.isEmpty()) && (highLimitValue.isEmpty())){
            IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT).setValue(Topics.DEFAULT_HIGH_LIMIT);
            IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT).setValue(Topics.DEFAULT_LOW_LIMIT);
        }
    }

    public void manageLowAndHighNotificationValues(DatabaseReference IoT_Database){

        String highLimitValue = highLimitEditText.getText().toString().trim();
        String lowLimitValue = lowLimitEditText.getText().toString().trim();
        String highLimitNotificationValue = highLimitNotificationValueEditText.getText().toString().trim();
        String lowLimitNotificationValue = lowLimitNotificationValueEditText.getText().toString().trim();
        String topicTag = topicEditText.getText().toString().trim();

        //Case 1:
        if(!(lowLimitNotificationValue.isEmpty()) && !(highLimitNotificationValue.isEmpty())){

            float lowLimitNotificationFloatValue = parseFloat(lowLimitNotificationValue);
            float highLimitNotificationFloatValue = parseFloat(highLimitNotificationValue);
            if(highLimitNotificationFloatValue > lowLimitNotificationFloatValue) {
                if (!(lowLimitValue.trim().isEmpty()) && !(highLimitValue.isEmpty())) {
                    float lowLimitFloatValue = parseFloat(lowLimitValue);
                    float highLimitFloatValue = parseFloat(highLimitValue);
                    if (lowLimitFloatValue <= lowLimitNotificationFloatValue) {

                        if (highLimitFloatValue >= highLimitNotificationFloatValue) {
                            IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                            IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                        } else {
                            highLimitNotificationValueEditText.setError(ERROR_NIH_LT_HL);
                            highLimitNotificationValueEditText.requestFocus();
                            error = true;
                        }

                    }else {
                        lowLimitNotificationValueEditText.setError(ERROR_NIL_HT_LL);
                        lowLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }
                }
                else if ((lowLimitValue.trim().isEmpty()) && !(highLimitValue.isEmpty())) {

                    float highLimitFloatValue = parseFloat(highLimitValue);

                    if (highLimitFloatValue >= highLimitNotificationFloatValue) {
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                    } else {
                        highLimitNotificationValueEditText.setError(ERROR_NIH_LT_HL);
                        highLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }
                }
                else if (!(lowLimitValue.trim().isEmpty()) && (highLimitValue.isEmpty())) {
                    float lowLimitFloatValue = parseFloat(lowLimitValue);

                    if (lowLimitFloatValue <= lowLimitNotificationFloatValue) {

                        IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                    }else {
                        lowLimitNotificationValueEditText.setError(ERROR_NIL_HT_LL);
                        lowLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }
                }
            }else {
                highLimitNotificationValueEditText.setError(ERROR_NIH_HT_NIL);
                highLimitNotificationValueEditText.requestFocus();
                error = true;
            }
        }

        //Case 2:
        else if((lowLimitNotificationValue.isEmpty()) && !(highLimitNotificationValue.isEmpty())){

            float highLimitNotificationFloatValue = parseFloat(highLimitNotificationValue);

                if (!(lowLimitValue.trim().isEmpty()) && !(highLimitValue.isEmpty())) {
                    float lowLimitFloatValue = parseFloat(lowLimitValue);
                    float highLimitFloatValue = parseFloat(highLimitValue);

                        if (highLimitFloatValue >= highLimitNotificationFloatValue) {

                            if (lowLimitFloatValue <= highLimitNotificationFloatValue) {

                                IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                                IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_LOW_LIMIT_NOTIFICATION_VALUE);

                            } else {
                                highLimitNotificationValueEditText.setError(ERROR_NIH_HT_LL);
                                highLimitNotificationValueEditText.requestFocus();
                                error = true;
                            }

                        } else {
                            highLimitNotificationValueEditText.setError(ERROR_NIH_LT_HL);
                            highLimitNotificationValueEditText.requestFocus();
                            error = true;
                        }
                }
                else if ((lowLimitValue.trim().isEmpty()) && !(highLimitValue.isEmpty())) {

                    float highLimitFloatValue = parseFloat(highLimitValue);

                    if (highLimitFloatValue >= highLimitNotificationFloatValue) {

                        IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_LOW_LIMIT_NOTIFICATION_VALUE);

                    } else {
                        highLimitNotificationValueEditText.setError(ERROR_NIH_LT_HL);
                        highLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }
                }
                else if (!(lowLimitValue.trim().isEmpty()) && (highLimitValue.isEmpty())) {
                    float lowLimitFloatValue = parseFloat(lowLimitValue);

                    if (lowLimitFloatValue <= highLimitNotificationFloatValue) {

                        IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_LOW_LIMIT_NOTIFICATION_VALUE);

                    } else {
                        highLimitNotificationValueEditText.setError(ERROR_NIH_HT_LL);
                        highLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }
                }
        }

        //Case 3:
        else if(!(lowLimitNotificationValue.isEmpty()) && (highLimitNotificationValue.isEmpty())){

            float lowLimitNotificationFloatValue = parseFloat(lowLimitNotificationValue);

            if (!(lowLimitValue.trim().isEmpty()) && !(highLimitValue.isEmpty())) {
                float lowLimitFloatValue = parseFloat(lowLimitValue);
                float highLimitFloatValue = parseFloat(highLimitValue);

                if (highLimitFloatValue >= lowLimitNotificationFloatValue) {

                    if (lowLimitFloatValue <= lowLimitNotificationFloatValue) {

                        IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_HIGH_LIMIT_NOTIFICATION_VALUE);
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                    } else {
                        lowLimitNotificationValueEditText.setError(ERROR_NIL_HT_LL);
                        lowLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }

                } else {
                    lowLimitNotificationValueEditText.setError(ERROR_NIL_LT_HL);
                    lowLimitNotificationValueEditText.requestFocus();
                    error = true;
                }
            }
            else if ((lowLimitValue.trim().isEmpty()) && !(highLimitValue.isEmpty())) {

                float highLimitFloatValue = parseFloat(highLimitValue);

                if (highLimitFloatValue >= lowLimitNotificationFloatValue) {

                    IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_HIGH_LIMIT_NOTIFICATION_VALUE);
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                } else {
                    lowLimitNotificationValueEditText.setError(ERROR_NIL_LT_HL);
                    lowLimitNotificationValueEditText.requestFocus();
                    error = true;
                }
            }
            else if (!(lowLimitValue.trim().isEmpty()) && (highLimitValue.isEmpty())) {
                float lowLimitFloatValue = parseFloat(lowLimitValue);

                if (lowLimitFloatValue <= lowLimitNotificationFloatValue) {

                    IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_HIGH_LIMIT_NOTIFICATION_VALUE);
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                } else {
                    lowLimitNotificationValueEditText.setError(ERROR_NIL_HT_LL);
                    lowLimitNotificationValueEditText.requestFocus();
                    error = true;
                }
            }
        }

        //Case 4:
        else if((lowLimitNotificationValue.isEmpty()) && (highLimitNotificationValue.isEmpty())){
            IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_HIGH_LIMIT_NOTIFICATION_VALUE);
            IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_LOW_LIMIT_NOTIFICATION_VALUE);
        }
    }

    public void retrieveSettingsValuesFromFirebaseDatabase(String topicTag){


        if(!topicTag.isEmpty() && !(topicTag==null)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());

                if(!(IoT_Database==null)) {

                    //topicEditText = findViewById(R.id.topicEditText);
                    topicEditText.setText(topicTag);

                    IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)) {
                                        String value = dataSnapshot.getValue().toString();
                                        float floatValue = parseFloat(value);
                                        //initialValueEditText = findViewById(R.id.initialValueEditText);
                                        initialValueEditText.setText(value);

                                        IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT)
                                                .addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if(!(dataSnapshot.getValue()==null)) {
                                                            String highLimitValue = dataSnapshot.getValue().toString();
                                                            float highLimitFloatValue = parseFloat(highLimitValue);
                                                            //highLimitEditText = findViewById(R.id.highLimitEditText);
                                                            highLimitEditText.setText(highLimitValue);
                                                            if(floatValue>highLimitFloatValue){
                                                                IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(highLimitValue);
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                                    }
                                                });
                                        IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT)
                                                .addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if(!(dataSnapshot.getValue()==null)) {
                                                            String lowLimitValue = dataSnapshot.getValue().toString();
                                                            float lowLimitFloatValue = parseFloat(lowLimitValue);
                                                            //lowLimitEditText = findViewById(R.id.lowLimitEditText);
                                                            lowLimitEditText.setText(lowLimitValue);
                                                            if(floatValue<lowLimitFloatValue){
                                                                IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(lowLimitValue);
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                                    }
                                                });

                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });

                    IoT_Database.child(thingTag).child(topicTag).child(Topics.ACCESS)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)) {
                                        String access = dataSnapshot.getValue().toString();

                                        // ReadWriteRadioButton = findViewById(R.id.RW_RadioButton);
                                        //ReadRadioButton = findViewById(R.id.R_RadioButton);

                                        if (access.equals(Topics.READWRITE)) {
                                            ReadWriteRadioButton.setChecked(true);
                                        }

                                        else if (access.equals(Topics.READ)) {
                                            ReadRadioButton.setChecked(true);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.DESCRIPTION)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)) {
                                        String topicName = dataSnapshot.getValue().toString();
                                        //topicNameEditText = findViewById(R.id.topicDescriptionEditText);
                                        topicNameEditText.setText(topicName);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.STEP)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)) {
                                        String step = dataSnapshot.getValue().toString();
                                        //stepEditText = findViewById(R.id.stepEditText);
                                        stepEditText.setText(step);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });

                    IoT_Database.child(thingTag).child(topicTag).child(Topics.UNIT)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)){
                                        String unit = dataSnapshot.getValue().toString();
                                        //unitEditText = findViewById(R.id.unitEditText);
                                        unitEditText.setText(unit);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)) {
                                        String highLimitNotificationValue = dataSnapshot.getValue().toString();
                                        //highLimitNotificationValueEditText = findViewById(R.id.highLimitNotificationValueEditText);
                                        highLimitNotificationValueEditText.setText(highLimitNotificationValue);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)) {
                                        String lowLimitNotificationValue = dataSnapshot.getValue().toString();
                                        //lowLimitNotificationValueEditText = findViewById(R.id.lowLimitNotificationValueEditText);
                                        lowLimitNotificationValueEditText.setText(lowLimitNotificationValue);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.FEEDBACK_NODE)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)) {
                                        String feedbackValue = dataSnapshot.getValue().toString();
                                        feedbackValueEditText.setText(feedbackValue);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });

                    IoT_Database.child(thingTag).child(topicTag).child(Topics.USE_FEEDBACK_NODE)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)) {
                                        String useFeedbackNode= dataSnapshot.getValue().toString();
                                        if(useFeedbackNode.equals("No")){
                                            useFeedbackNodeAutoCompleteTextView.setText("No", false);
                                        }else if(useFeedbackNode.equals("Yes")){
                                            useFeedbackNodeAutoCompleteTextView.setText("Yes", false);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                }
            }
        }
    }

    public void showFullTag (View view){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            String userID = mAuth.getCurrentUser().getUid();
            if(userID!=null){
                String Tag = userID + "/Things" + "/" + thingTag + "/" + initialTopicTag;
                AlertDialog newDialog = new AlertDialog.Builder(AnalogTopicConfigurationActivity.this)

                        .setTitle("Full tag:")
                        .setMessage(Tag)
                        .setPositiveButton(Html.fromHtml("<font color='black'><small>Copy to clipboard</small></font>"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!Tag.isEmpty()){
                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText(TAG, Tag);
                                    clipboard.setPrimaryClip(clip);

                                    Toast.makeText(AnalogTopicConfigurationActivity.this, "Tag copied to clipboard", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton(Html.fromHtml("<font color='black'><small>Back</small></font>"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        } else Toast.makeText(AnalogTopicConfigurationActivity.this, "Make sure you are connected and logged in", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analog_topic_configuration);

        Toolbar toolbar = (Toolbar) findViewById(R.id.AnalogTopicConfigToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent intent = getIntent();

        thingTag = intent.getStringExtra(Topics.THING_TAG);
        thingName = intent.getStringExtra(Topics.THING_DESCRIPTION_NAME);
        initialTopicTag = intent.getStringExtra(Topics.TOPIC_TAG);

        isTopicChanged = false;

        thingDescriptionNameTextView = findViewById(R.id.thingNameInTopicConfiguration);
        thingDescriptionNameTextView.setText(thingName);

        thingTagTextView = findViewById(R.id.thingTagInTopicConfiguration);
        thingTagTextView.setText(thingTag);

//        preTagTextView = findViewById(R.id.preTagTextView);
//        preTagTextView.setText(".../"+thingTag+"/");

        topicEditText = findViewById(R.id.topicEditText);
        topicNameEditText = findViewById(R.id.topicDescriptionEditText);
        initialValueEditText = findViewById(R.id.initialValueEditText);
        feedbackValueEditText = findViewById(R.id.feedbackValueEditText);
        stepEditText = findViewById(R.id.stepEditText);
        unitEditText = findViewById(R.id.unitEditText);
        lowLimitEditText = findViewById(R.id.lowLimitEditText);
        highLimitEditText = findViewById(R.id.highLimitEditText);
        highLimitNotificationValueEditText = findViewById(R.id.highLimitNotificationValueEditText);
        lowLimitNotificationValueEditText = findViewById(R.id.lowLimitNotificationValueEditText);
        ReadWriteRadioButton = findViewById(R.id.RW_RadioButton);
        ReadRadioButton = findViewById(R.id.R_RadioButton);
        useFeedbackNodeAutoCompleteTextView = findViewById(R.id.useFeedbackNode);

        ArrayAdapter<String> notifyIf_X_ArrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item,
                yesOrNo);

        useFeedbackNodeAutoCompleteTextView.setAdapter(notifyIf_X_ArrayAdapter);

        retrieveSettingsValuesFromFirebaseDatabase(initialTopicTag);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
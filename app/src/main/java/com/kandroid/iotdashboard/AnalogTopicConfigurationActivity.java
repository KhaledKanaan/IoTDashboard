package com.kandroid.iotdashboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;

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

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kandroid.iotdashboard.Topics.ACCESS;
import static com.kandroid.iotdashboard.Topics.ANALOG;
import static com.kandroid.iotdashboard.Topics.DEFAULT_DESCRIPTION;
import static com.kandroid.iotdashboard.Topics.DEFAULT_UNIT;
import static com.kandroid.iotdashboard.Topics.DESCRIPTION;
import static com.kandroid.iotdashboard.Topics.FEEDBACK_NODE;
import static com.kandroid.iotdashboard.Topics.HIGH_LIMIT;
import static com.kandroid.iotdashboard.Topics.HIGH_LIMIT_NOTIFICATION_VALUE;
import static com.kandroid.iotdashboard.Topics.LOW_LIMIT;
import static com.kandroid.iotdashboard.Topics.LOW_LIMIT_NOTIFICATION_VALUE;
import static com.kandroid.iotdashboard.Topics.NOTIFY_IF_RESET;
import static com.kandroid.iotdashboard.Topics.NOTIFY_IF_SET;
import static com.kandroid.iotdashboard.Topics.READ;
import static com.kandroid.iotdashboard.Topics.READWRITE;
import static com.kandroid.iotdashboard.Topics.STEP;
import static com.kandroid.iotdashboard.Topics.TEXT_ON_RESET;
import static com.kandroid.iotdashboard.Topics.TEXT_ON_SET;
import static com.kandroid.iotdashboard.Topics.TOPIC_NAME;
import static com.kandroid.iotdashboard.Topics.TYPE;
import static com.kandroid.iotdashboard.Topics.UNIT;
import static com.kandroid.iotdashboard.Topics.USE_FEEDBACK_NODE;
import static com.kandroid.iotdashboard.Topics.VALUE;
import static java.lang.Float.parseFloat;

public class AnalogTopicConfigurationActivity extends AppCompatActivity {

    public static String initialTopicTag, thingName, thingTag, TAG="Analog Topic Configuration Activity";

    public static AutoCompleteTextView useFeedbackNodeAutoCompleteTextView;
    private ArrayList<String> yesOrNo = new ArrayList<String>() {{add("Yes"); add("No");}};

    private String ERROR_NIL_HT_LL = "(Notify if <=) should be\n>= low limit", ERROR_NIH_LT_HL = "(Notify if >=) should be\n<= high limit",
            ERROR_NIH_HT_NIL = "(Notify if >=) should be\n> (Notify if <=)", ERROR_NIH_HT_LL = "(Notify if >=) should be\n>= low limit",
            ERROR_NIL_LT_HL = "(Notify if <=) should be\n<= high limit", ERROR_IV_HT_LL = "Value should be\n>= low limit",
            ERROR_IV_LT_HL = "Value should be\n<= high limit", ERROR_SV_HT_0 = "Step should be > 0",
            ERROR_SV_MAX_V = "Step should be <=\n(high limit - low limit)/2", ERROR_HL_HT_LL = "High limit should be\nhigher than low limit" ;

    public EditText topicEditText, topicNameEditText, initialValueEditText, stepEditText,
    unitEditText, highLimitEditText, lowLimitEditText, highLimitNotificationValueEditText,
    lowLimitNotificationValueEditText, feedbackValueEditText;

    public TextView thingDescriptionNameTextView, thingTagTextView, preTagTextView;

    public RadioButton ReadWriteRadioButton, ReadRadioButton;

    public boolean error, isTopicChanged;

    public static List<String> topicTagsList = new ArrayList<String>();

    public List<ValueEventListener> listeners = new ArrayList<ValueEventListener>();

    private ValueEventListener listener, maintainedConnectionListener, connectedRefListener;

    private FirebaseUser user;

    private DatabaseReference IoT_Database;

    public TextInputLayout initialValueTextInputLayout, stepTextInputLayout, feedbackValueTextInputLayout, lowLimitTextInputLayout, highLimitTextInputLayout,
            lowLimitNotificationValueTextInputLayout, highLimitNotificationValueTextInputLayout, topicTextInputLayout;

    public void saveConfigurationsToFirebase(DatabaseReference IoT_Database){

        error = false;
        String unit = unitEditText.getText().toString().trim();
        String topicName = topicNameEditText.getText().toString().trim();
        String topicTag = topicEditText.getText().toString().trim();
        String useFeedbackNode = useFeedbackNodeAutoCompleteTextView.getText().toString().trim();

        if(IoT_Database!=null){

            Map<String, Object> topicData = new HashMap<>();

            topicData.put(TYPE, ANALOG);

            if(!topicName.isEmpty()) {
                topicData.put(DESCRIPTION, topicName);
            }else topicData.put(DESCRIPTION, DEFAULT_DESCRIPTION);

            if(!unit.isEmpty()){
                topicData.put(UNIT, unit);
            }else topicData.put(UNIT, DEFAULT_UNIT);

            if (ReadWriteRadioButton.isChecked()){
                topicData.put(ACCESS, READWRITE);
            } else if (ReadRadioButton.isChecked()){
                topicData.put(ACCESS, READ);
            }
            if(!useFeedbackNode.isEmpty()){
                topicData.put(USE_FEEDBACK_NODE, useFeedbackNode);
            }else topicData.put(USE_FEEDBACK_NODE, "No");

            IoT_Database.child(thingTag).child(topicTag).updateChildren(topicData);


            manageValue(IoT_Database);

            manageHighAndLowLimits(IoT_Database);

            manageLowAndHighNotificationValues(IoT_Database);

            manageStepValue(IoT_Database);

            manageResults();

            if (isTopicChanged) {
                removeListeners();

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
        //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null && IoT_Database!=null) {
            //DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            IoT_Database.child(thingTag).child(oldTopicTag).child(Topics.FEEDBACK_NODE)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
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


        initialValueTextInputLayout.setError(null);
        stepTextInputLayout.setError(null);
        lowLimitTextInputLayout.setError(null);
        highLimitTextInputLayout.setError(null);
        lowLimitNotificationValueTextInputLayout.setError(null);
        highLimitNotificationValueTextInputLayout.setError(null);

        String topicTag = topicEditText.getText().toString().trim();

        //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null && IoT_Database!=null){
            //DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());

            //removeAllActiveListeners(IoT_Database);

            if(topicTag.equals(initialTopicTag)){
                saveConfigurationsToFirebase(IoT_Database);
            }
            else{
                AlertDialog Dialog = new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Warning!")
                        .setMessage("You are about to change the topic tag, continue?")
                        .setPositiveButton(HtmlCompat.fromHtml("<font color='black'>Yes</font>", HtmlCompat.FROM_HTML_MODE_LEGACY), new DialogInterface.OnClickListener() {
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


                                                        removeListeners();

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
                                                                300);
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
                        .setNegativeButton(HtmlCompat.fromHtml("<font color='black'>Cancel</font>", HtmlCompat.FROM_HTML_MODE_LEGACY), new DialogInterface.OnClickListener() {
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
//                            //topicEditText.setError(null);
//                            //topicNameEditText.setError(null);
//                            initialValueEditText.setError(null);
//                            stepEditText.setError(null);
//                            //unitEditText.setError(null);
//                            lowLimitEditText.setError(null);
//                            highLimitEditText.setError(null);
//                            highLimitNotificationValueEditText.setError(null);
//                            lowLimitNotificationValueEditText.setError(null);

                            Toast.makeText(AnalogTopicConfigurationActivity.this,"Configuration saved", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                300);

    }

    public void manageValue(DatabaseReference IoT_Database){

        String value = initialValueEditText.getText().toString().trim();
        String highLimitValue = highLimitEditText.getText().toString().trim();
        String lowLimitValue = lowLimitEditText.getText().toString().trim();
        String topicTag = topicEditText.getText().toString().trim();

        initialValueTextInputLayout = findViewById(R.id.initialValueTextInputLayout);

        if(value != null){

            boolean isValueNumeric = NumberUtils.isCreatable(value);
            boolean isHighLimitValueNumeric = NumberUtils.isCreatable(highLimitValue);
            boolean isLowLimitValueNumeric = NumberUtils.isCreatable(lowLimitValue);

            if(isValueNumeric){
                float floatValue = parseFloat(value);
                if(isLowLimitValueNumeric && isHighLimitValueNumeric){
                    float lowLimitFloatValue = parseFloat(lowLimitValue);
                    float highLimitFloatValue = parseFloat(highLimitValue);
                    if(floatValue <= highLimitFloatValue){
                        if(floatValue >= lowLimitFloatValue){
                            IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(value);
                        }else {
                            IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(lowLimitValue);
//                            initialValueTextInputLayout.setError(ERROR_IV_HT_LL);
//                            //initialValueEditText.setError(ERROR_IV_HT_LL);
//                            //initialValueEditText.requestFocus();
//                            error = true;
                        }

                    }else {
                        IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(highLimitValue);
//                        initialValueTextInputLayout.setError(ERROR_IV_LT_HL);
//                        //initialValueEditText.setError(ERROR_IV_LT_HL);
//                        //initialValueEditText.requestFocus();
//                        error = true;
                    }
                }
                else if(!isLowLimitValueNumeric && isHighLimitValueNumeric){

                    float highLimitFloatValue = parseFloat(highLimitValue);
                    if(floatValue <= highLimitFloatValue){
                        IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(value);
                    }else {
                        IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(highLimitValue);
//                        initialValueTextInputLayout.setError(ERROR_IV_LT_HL);
//                        //initialValueEditText.setError(ERROR_IV_LT_HL);
//                        //initialValueEditText.requestFocus();
//                        error = true;
                    }
                }
                else if(isLowLimitValueNumeric && !isHighLimitValueNumeric){
                    float lowLimitFloatValue = parseFloat(lowLimitValue);
                    if(floatValue >= lowLimitFloatValue){
                        IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(value);
                    }else {
                        IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(lowLimitValue);
//                        initialValueTextInputLayout.setError(ERROR_IV_HT_LL);
//                        //initialValueEditText.setError(ERROR_IV_HT_LL);
//                        //initialValueEditText.requestFocus();
//                        error = true;
                    }
                }
                else if(!isHighLimitValueNumeric && !isHighLimitValueNumeric){

                    IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(value);
                    initialValueEditText.setText(value);
                }
            }
            else {
                if(isLowLimitValueNumeric){
                    float lowLimitFloatValue = Float.parseFloat(lowLimitValue);
                    float floatValue = lowLimitFloatValue;
                    value = String.valueOf(floatValue);
                    IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(value);
                    initialValueEditText.setText(value);
                }
                else if(isHighLimitValueNumeric){
                    float highLimitFloatValue = Float.parseFloat(highLimitValue);
                    float floatValue = 0;
                    if(highLimitFloatValue <= 0){
                        floatValue = highLimitFloatValue;
                    }
                    value = String.valueOf(floatValue);
                    IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(floatValue);
                    initialValueEditText.setText(value);
                }
                else {
                    IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(Topics.DEFAULT_ANALOG_VALUE);
                    initialValueEditText.setText(String.valueOf(Topics.DEFAULT_ANALOG_VALUE));
                }
            }

        }
    }

    public void manageStepValue(DatabaseReference IoT_Database){

        String step = stepEditText.getText().toString().trim();
        String highLimitValue = highLimitEditText.getText().toString().trim();
        String lowLimitValue = lowLimitEditText.getText().toString().trim();
        String topicTag = topicEditText.getText().toString().trim();

        stepTextInputLayout = findViewById(R.id.stepTextInputLayout);

        if(step != null){

            boolean isStepNumeric = NumberUtils.isCreatable(step);
            boolean isHighLimitValueNumeric = NumberUtils.isCreatable(highLimitValue);
            boolean isLowLimitValueNumeric = NumberUtils.isCreatable(lowLimitValue);

            if(isStepNumeric){
                float stepValue = Float.parseFloat(step);
                if (stepValue <= 0){
                    stepTextInputLayout.setError(ERROR_SV_HT_0);
                    //stepEditText.setError(ERROR_SV_HT_0);
                    //stepEditText.requestFocus();
                    error = true;
                    return;
                }

                if(isLowLimitValueNumeric && isHighLimitValueNumeric){
                    float lowLimitFloatValue = Float.parseFloat(lowLimitValue);
                    float highLimitFloatValue = Float.parseFloat(highLimitValue);

                    if(highLimitFloatValue > lowLimitFloatValue && stepValue <= ((highLimitFloatValue-lowLimitFloatValue)/2)){
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.STEP).setValue(step);
                    }else {
                        stepTextInputLayout.setError(ERROR_SV_MAX_V);
                        //stepEditText.setError(ERROR_SV_MAX_V);
                        //stepEditText.requestFocus();
                        error = true;
//                    if(highLimitFloatValue > lowLimitFloatValue){
//                        stepValue = (highLimitFloatValue - lowLimitFloatValue)/10;
//                        IoT_Database.child(thingTag).child(topicTag).child(Topics.STEP).setValue(stepValue);
//                        stepEditText.setText(String.valueOf(stepValue));
//                    }
                    }
                }
                else IoT_Database.child(thingTag).child(topicTag).child(Topics.STEP).setValue(step);

            } else if(isLowLimitValueNumeric && isHighLimitValueNumeric){
                float lowLimitFloatValue = Float.parseFloat(lowLimitValue);
                float highLimitFloatValue = Float.parseFloat(highLimitValue);
                if(highLimitFloatValue > lowLimitFloatValue){
                    float stepValue = (highLimitFloatValue - lowLimitFloatValue)/10;
                    step = String.valueOf(stepValue);
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.STEP).setValue(step);
                    stepEditText.setText(step);
                }
            } else {
                IoT_Database.child(thingTag).child(topicTag).child(Topics.STEP).setValue(Topics.DEFAULT_STEP);
                stepEditText.setText(String.valueOf(Topics.DEFAULT_STEP));
            }

        }
    }

    public void manageHighAndLowLimits(DatabaseReference IoT_Database){

        String highLimitValue = highLimitEditText.getText().toString().trim();
        String lowLimitValue = lowLimitEditText.getText().toString().trim();
        String topicTag = topicEditText.getText().toString().trim();

        highLimitTextInputLayout = findViewById(R.id.highLimitTextInputLayout);
        lowLimitTextInputLayout = findViewById(R.id.lowLimitTextInputLayout);


        if(lowLimitValue != null && highLimitValue != null){

            boolean isHighLimitValueNumeric = NumberUtils.isCreatable(highLimitValue);
            boolean isLowLimitValueNumeric = NumberUtils.isCreatable(lowLimitValue);

            if(isLowLimitValueNumeric && isHighLimitValueNumeric){
                float lowLimitFloatValue = Float.parseFloat(lowLimitValue);
                float highLimitFloatValue = Float.parseFloat(highLimitValue);
                if(highLimitFloatValue > lowLimitFloatValue){
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT).setValue(highLimitValue);
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT).setValue(lowLimitValue);
                }else {
                    highLimitTextInputLayout.setError(ERROR_HL_HT_LL);
                    //highLimitEditText.setError(ERROR_HL_HT_LL);
                    //highLimitEditText.requestFocus();
                    error = true;
                }
            }else if(!isLowLimitValueNumeric && isHighLimitValueNumeric){
                float highLimitFloatValue = Float.parseFloat(highLimitValue);
                IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT).setValue(highLimitValue);
                IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT).setValue(Topics.DEFAULT_LOW_LIMIT);

            }else if(isLowLimitValueNumeric && !isHighLimitValueNumeric){
                float lowLimitFloatValue = Float.parseFloat(lowLimitValue);
                IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT).setValue(Topics.DEFAULT_HIGH_LIMIT);
                IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT).setValue(lowLimitValue);

            }else if(!isLowLimitValueNumeric && !isHighLimitValueNumeric){
                IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT).setValue(Topics.DEFAULT_HIGH_LIMIT);
                IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT).setValue(Topics.DEFAULT_LOW_LIMIT);
            }

        }
    }

    public void manageLowAndHighNotificationValues(DatabaseReference IoT_Database){

        String highLimitValue = highLimitEditText.getText().toString().trim();
        String lowLimitValue = lowLimitEditText.getText().toString().trim();
        String highLimitNotificationValue = highLimitNotificationValueEditText.getText().toString().trim();
        String lowLimitNotificationValue = lowLimitNotificationValueEditText.getText().toString().trim();
        String topicTag = topicEditText.getText().toString().trim();

        highLimitNotificationValueTextInputLayout = findViewById(R.id.highLimitNotificationValueTextInputLayout);
        lowLimitNotificationValueTextInputLayout = findViewById(R.id.lowLimitNotificationValueTextInputLayout);

        if(lowLimitNotificationValue != null && highLimitNotificationValue != null){

            boolean isLowLimitNotificationValueNumeric = NumberUtils.isCreatable(lowLimitNotificationValue);
            boolean isHighLimitNotificationValueNumeric = NumberUtils.isCreatable(highLimitNotificationValue);

            boolean isHighLimitValueNumeric = NumberUtils.isCreatable(highLimitValue);
            boolean isLowLimitValueNumeric = NumberUtils.isCreatable(lowLimitValue);

            //Case 1:
            if(isLowLimitNotificationValueNumeric && isHighLimitNotificationValueNumeric){

                float lowLimitNotificationFloatValue = parseFloat(lowLimitNotificationValue);
                float highLimitNotificationFloatValue = parseFloat(highLimitNotificationValue);

                if(highLimitNotificationFloatValue > lowLimitNotificationFloatValue) {
                    if (isLowLimitValueNumeric && isHighLimitValueNumeric) {
                        float lowLimitFloatValue = parseFloat(lowLimitValue);
                        float highLimitFloatValue = parseFloat(highLimitValue);
                        if (lowLimitFloatValue <= lowLimitNotificationFloatValue) {

                            if (highLimitFloatValue >= highLimitNotificationFloatValue) {
                                IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                                IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                            } else {
                                highLimitNotificationValueTextInputLayout.setError(ERROR_NIH_LT_HL);
                                //highLimitNotificationValueEditText.setError(ERROR_NIH_LT_HL);
                                //highLimitNotificationValueEditText.requestFocus();
                                error = true;
                            }

                        }else {
                            lowLimitNotificationValueTextInputLayout.setError(ERROR_NIL_HT_LL);
                            //lowLimitNotificationValueEditText.setError(ERROR_NIL_HT_LL);
                            //lowLimitNotificationValueEditText.requestFocus();
                            error = true;
                        }
                    }
                    else if (!isLowLimitValueNumeric && isHighLimitValueNumeric) {

                        float highLimitFloatValue = parseFloat(highLimitValue);

                        if (highLimitFloatValue >= highLimitNotificationFloatValue) {
                            IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                            IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                        } else {
                            highLimitNotificationValueTextInputLayout.setError(ERROR_NIH_LT_HL);
                            //highLimitNotificationValueEditText.setError(ERROR_NIH_LT_HL);
                            //highLimitNotificationValueEditText.requestFocus();
                            error = true;
                        }
                    }
                    else if (isLowLimitValueNumeric && !isHighLimitValueNumeric) {
                        float lowLimitFloatValue = parseFloat(lowLimitValue);

                        if (lowLimitFloatValue <= lowLimitNotificationFloatValue) {

                            IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                            IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                        }else {
                            lowLimitNotificationValueTextInputLayout.setError(ERROR_NIL_HT_LL);
                            //lowLimitNotificationValueEditText.setError(ERROR_NIL_HT_LL);
                            //lowLimitNotificationValueEditText.requestFocus();
                            error = true;
                        }
                    }
                }else {
                    highLimitNotificationValueTextInputLayout.setError(ERROR_NIH_HT_NIL);
                    //highLimitNotificationValueEditText.setError(ERROR_NIH_HT_NIL);
                    //highLimitNotificationValueEditText.requestFocus();
                    error = true;
                }
            }

            //Case 2:
            else if(!isLowLimitNotificationValueNumeric && isHighLimitNotificationValueNumeric){

                float highLimitNotificationFloatValue = parseFloat(highLimitNotificationValue);

                if (isLowLimitValueNumeric && isHighLimitValueNumeric) {
                    float lowLimitFloatValue = parseFloat(lowLimitValue);
                    float highLimitFloatValue = parseFloat(highLimitValue);

                    if (highLimitFloatValue >= highLimitNotificationFloatValue) {

                        if (lowLimitFloatValue <= highLimitNotificationFloatValue) {

                            IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                            IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_LOW_LIMIT_NOTIFICATION_VALUE);

                        } else {
                            highLimitNotificationValueTextInputLayout.setError(ERROR_NIH_HT_LL);
                            //highLimitNotificationValueEditText.setError(ERROR_NIH_HT_LL);
                            //highLimitNotificationValueEditText.requestFocus();
                            error = true;
                        }

                    } else {
                        highLimitNotificationValueTextInputLayout.setError(ERROR_NIH_LT_HL);
                        //highLimitNotificationValueEditText.setError(ERROR_NIH_LT_HL);
                        //highLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }
                }
                else if (!isLowLimitValueNumeric && isHighLimitValueNumeric) {

                    float highLimitFloatValue = parseFloat(highLimitValue);

                    if (highLimitFloatValue >= highLimitNotificationFloatValue) {

                        IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_LOW_LIMIT_NOTIFICATION_VALUE);

                    } else {
                        highLimitNotificationValueTextInputLayout.setError(ERROR_NIH_LT_HL);
                        //highLimitNotificationValueEditText.setError(ERROR_NIH_LT_HL);
                        //highLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }
                }
                else if (isLowLimitValueNumeric && !isHighLimitValueNumeric) {
                    float lowLimitFloatValue = parseFloat(lowLimitValue);

                    if (lowLimitFloatValue <= highLimitNotificationFloatValue) {

                        IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(highLimitNotificationValue);
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_LOW_LIMIT_NOTIFICATION_VALUE);

                    } else {
                        highLimitNotificationValueTextInputLayout.setError(ERROR_NIH_HT_LL);
                        //highLimitNotificationValueEditText.setError(ERROR_NIH_HT_LL);
                        //highLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }
                }
            }

            //Case 3:
            else if(isLowLimitNotificationValueNumeric && !isHighLimitNotificationValueNumeric){

                float lowLimitNotificationFloatValue = parseFloat(lowLimitNotificationValue);

                if (isLowLimitValueNumeric && isHighLimitValueNumeric) {
                    float lowLimitFloatValue = parseFloat(lowLimitValue);
                    float highLimitFloatValue = parseFloat(highLimitValue);

                    if (highLimitFloatValue >= lowLimitNotificationFloatValue) {

                        if (lowLimitFloatValue <= lowLimitNotificationFloatValue) {

                            IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_HIGH_LIMIT_NOTIFICATION_VALUE);
                            IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                        } else {
                            lowLimitNotificationValueTextInputLayout.setError(ERROR_NIL_HT_LL);
                            //lowLimitNotificationValueEditText.setError(ERROR_NIL_HT_LL);
                            //lowLimitNotificationValueEditText.requestFocus();
                            error = true;
                        }

                    } else {
                        lowLimitNotificationValueTextInputLayout.setError(ERROR_NIL_LT_HL);
                        //lowLimitNotificationValueEditText.setError(ERROR_NIL_LT_HL);
                        //lowLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }
                }
                else if (!isLowLimitValueNumeric && isHighLimitValueNumeric) {

                    float highLimitFloatValue = parseFloat(highLimitValue);

                    if (highLimitFloatValue >= lowLimitNotificationFloatValue) {

                        IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_HIGH_LIMIT_NOTIFICATION_VALUE);
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                    } else {
                        lowLimitNotificationValueTextInputLayout.setError(ERROR_NIL_LT_HL);
                        //lowLimitNotificationValueEditText.setError(ERROR_NIL_LT_HL);
                        //lowLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }
                }
                else if ( isLowLimitValueNumeric && !isHighLimitValueNumeric) {
                    float lowLimitFloatValue = parseFloat(lowLimitValue);

                    if (lowLimitFloatValue <= lowLimitNotificationFloatValue) {

                        IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_HIGH_LIMIT_NOTIFICATION_VALUE);
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(lowLimitNotificationValue);

                    } else {
                        lowLimitNotificationValueTextInputLayout.setError(ERROR_NIL_HT_LL);
                        //lowLimitNotificationValueEditText.setError(ERROR_NIL_HT_LL);
                        //lowLimitNotificationValueEditText.requestFocus();
                        error = true;
                    }
                }
            }

            //Case 4:
            else if(!isLowLimitNotificationValueNumeric && !isHighLimitNotificationValueNumeric){
                IoT_Database.child(thingTag).child(topicTag).child(Topics.HIGH_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_HIGH_LIMIT_NOTIFICATION_VALUE);
                IoT_Database.child(thingTag).child(topicTag).child(Topics.LOW_LIMIT_NOTIFICATION_VALUE).setValue(Topics.DEFAULT_LOW_LIMIT_NOTIFICATION_VALUE);
            }

        }
    }

    public void retrieveSettingsValuesFromFirebaseDatabase(String topicTag){

        removeListeners();

        if(!topicTag.isEmpty() && !(topicTag==null)) {
            //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                //DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                if(!(IoT_Database==null)) {
                    listener = IoT_Database.child(thingTag).child(topicTag).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot!=null){

                                topicEditText.setText(dataSnapshot.getKey());

                                Map<String, Object> topicData = new HashMap<>();
                                topicData = (Map<String, Object>) dataSnapshot.getValue();

                                if(topicData!=null){

                                    //value, name, type and access are common for all types of topic
                                    String value = String.valueOf(topicData.get(VALUE));
                                    String topicName = String.valueOf(topicData.get(DESCRIPTION));
                                    //String type = String.valueOf(topicData.get(TYPE));
                                    String access = String.valueOf(topicData.get(ACCESS));
                                    String highLimitValue = String.valueOf(topicData.get(HIGH_LIMIT));
                                    String lowLimitValue = String.valueOf(topicData.get(LOW_LIMIT));
                                    String step = String.valueOf(topicData.get(STEP));
                                    String unit = String.valueOf(topicData.get(UNIT));
                                    String highLimitNotificationValue = String.valueOf(topicData.get(HIGH_LIMIT_NOTIFICATION_VALUE));
                                    String lowLimitNotificationValue = String.valueOf(topicData.get(LOW_LIMIT_NOTIFICATION_VALUE));
                                    String useFeedbackNode = String.valueOf(topicData.get(USE_FEEDBACK_NODE));
                                    String feedback = String.valueOf(topicData.get(FEEDBACK_NODE));

                                    boolean isValueNumeric = NumberUtils.isCreatable(value);
                                    boolean isLowLimitValueNumeric = NumberUtils.isCreatable(lowLimitValue);
                                    boolean isHighLimitValueNumeric = NumberUtils.isCreatable(highLimitValue);
                                    boolean isStepNumeric = NumberUtils.isCreatable(step);
                                    boolean isHighLimitNotificationValueNumeric = NumberUtils.isCreatable(highLimitNotificationValue);
                                    boolean isLowLimitNotificationValueNumeric = NumberUtils.isCreatable(lowLimitNotificationValue);
                                    boolean isFeedbackNumeric = NumberUtils.isCreatable(feedback);


                                    ///
                                    if(isValueNumeric){
                                        initialValueEditText.setText(value);
                                        float floatValue = parseFloat(value);
                                    }else {
                                        if(topicData.get(VALUE)!=null){
                                            if(!value.isEmpty()) initialValueEditText.setText("NaN");
                                        }
                                    }

                                    ///
                                    if(isHighLimitValueNumeric){
                                        float highLimitFloatValue = parseFloat(highLimitValue);
                                        //highLimitEditText = findViewById(R.id.highLimitEditText);
                                        highLimitEditText.setText(highLimitValue);
                                        if(isValueNumeric){
                                            float floatValue = parseFloat(value);
                                            if(floatValue>highLimitFloatValue){
                                                IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(highLimitValue);
                                            }
                                        }
                                    }else {
                                        if(topicData.get(HIGH_LIMIT)!=null){
                                            if(!highLimitValue.isEmpty()) highLimitEditText.setText("NaN");
                                        }
                                    }

                                    ///
                                    if(isLowLimitValueNumeric){
                                        float lowLimitFloatValue = parseFloat(lowLimitValue);
                                        //lowLimitEditText = findViewById(R.id.lowLimitEditText);
                                        lowLimitEditText.setText(lowLimitValue);
                                        if(isValueNumeric){
                                            float floatValue = parseFloat(value);
                                            if(floatValue<lowLimitFloatValue){
                                                IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(lowLimitValue);
                                            }
                                        }
                                    }else{
                                        if(topicData.get(LOW_LIMIT)!=null){
                                            if(!lowLimitValue.isEmpty()) lowLimitEditText.setText("NaN");
                                        }
                                    }


                                    ///
                                    if(topicData.get(ACCESS)!=null){
                                        if (access.equals(Topics.READWRITE)) {
                                            ReadWriteRadioButton.setChecked(true);
                                        }

                                        else if (access.equals(READ)) {
                                            ReadRadioButton.setChecked(true);
                                        }
                                    }

                                    ///
                                    if(topicData.get(DESCRIPTION)!=null){
                                        topicNameEditText.setText(topicName);
                                    }


                                    ///
                                    if(isStepNumeric){
                                        stepEditText.setText(step);
                                    }else{
                                        if(topicData.get(STEP)!=null){
                                            if(!step.isEmpty()) stepEditText.setText("NaN");
                                        }
                                    }

                                    ///
                                    if(topicData.get(UNIT)!=null){
                                        unitEditText.setText(unit);
                                    }

                                    ///
                                    if(isHighLimitNotificationValueNumeric){
                                        highLimitNotificationValueEditText.setText(highLimitNotificationValue);
                                    }else{
                                        if(topicData.get(HIGH_LIMIT_NOTIFICATION_VALUE)!=null){
                                            if(!highLimitNotificationValue.isEmpty()) highLimitNotificationValueEditText.setText("NaN");
                                        }
                                    }

                                    ///
                                    if(isLowLimitNotificationValueNumeric){
                                        lowLimitNotificationValueEditText.setText(lowLimitNotificationValue);
                                    }else{
                                        if(topicData.get(LOW_LIMIT_NOTIFICATION_VALUE)!=null){
                                            if(!lowLimitNotificationValue.isEmpty()) lowLimitNotificationValueEditText.setText("NaN");
                                        }
                                    }

                                    ///
                                    if(isFeedbackNumeric){
                                        feedbackValueEditText.setText(feedback);
                                    }else{
                                        if (topicData.get(FEEDBACK_NODE)!=null){
                                            if(!feedback.isEmpty()) feedbackValueEditText.setText("NaN");
                                        }
                                    }

                                    ///
                                    if(topicData.get(USE_FEEDBACK_NODE)!=null){
                                        if(useFeedbackNode.equals("No")){
                                            useFeedbackNodeAutoCompleteTextView.setText("No", false);
                                        }else if(useFeedbackNode.equals("Yes")){
                                            useFeedbackNodeAutoCompleteTextView.setText("Yes", false);
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

    public void showFullTag (View view){

        //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            String userID = mAuth.getCurrentUser().getUid();
            if(userID!=null){
                String Tag = userID + "/Things" + "/" + thingTag + "/" + initialTopicTag;
                AlertDialog newDialog = new AlertDialog.Builder(AnalogTopicConfigurationActivity.this)

                        .setTitle("Topic path:")
                        .setMessage(Tag)
                        .setPositiveButton(HtmlCompat.fromHtml("<font color='black'><small>Copy to clipboard</small></font>", HtmlCompat.FROM_HTML_MODE_LEGACY), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!Tag.isEmpty()){
                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText(TAG, Tag);
                                    clipboard.setPrimaryClip(clip);

                                    Toast.makeText(AnalogTopicConfigurationActivity.this, "Topic path copied to clipboard", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton(HtmlCompat.fromHtml("<font color='black'><small>Back</small></font>", HtmlCompat.FROM_HTML_MODE_LEGACY), new DialogInterface.OnClickListener() {
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

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
        }

        Intent intent = getIntent();

        thingTag = intent.getStringExtra(Topics.THING_TAG);
        thingName = intent.getStringExtra(Topics.THING_DESCRIPTION_NAME);
        initialTopicTag = intent.getStringExtra(Topics.TOPIC_TAG);

        //Toast.makeText(getApplicationContext(), initialTopicTag, Toast.LENGTH_LONG).show();

        isTopicChanged = false;

        thingDescriptionNameTextView = findViewById(R.id.thingNameInTopicConfiguration);
        thingDescriptionNameTextView.setText(thingName);

        thingTagTextView = findViewById(R.id.thingTagInTopicConfiguration);
        thingTagTextView.setText(thingTag);


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


        initialValueTextInputLayout = findViewById(R.id.initialValueTextInputLayout);
        stepTextInputLayout = findViewById(R.id.stepTextInputLayout);
        lowLimitTextInputLayout = findViewById(R.id.lowLimitTextInputLayout);
        highLimitTextInputLayout = findViewById(R.id.highLimitTextInputLayout);
        lowLimitNotificationValueTextInputLayout = findViewById(R.id.lowLimitNotificationValueTextInputLayout);
        highLimitNotificationValueTextInputLayout = findViewById(R.id.highLimitNotificationValueTextInputLayout);
        topicTextInputLayout = findViewById(R.id.topicTextInputLayout);

        topicTextInputLayout.setPrefixText(thingTag+"/");

        ArrayAdapter<String> notifyIf_X_ArrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item,
                yesOrNo);

        useFeedbackNodeAutoCompleteTextView.setAdapter(notifyIf_X_ArrayAdapter);

        retrieveSettingsValuesFromFirebaseDatabase(initialTopicTag);


    }

    @Override
    protected void onResume() {
        super.onResume();
        maintainTheConnectionToFirebase();
    }

    public void maintainTheConnectionToFirebase(){
        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            DatabaseReference maintainConnectionRef = FirebaseDatabase.getInstance().getReference().child(LauncherActivity.MAINTAINED_CONNECTION_POINT);
            if (maintainConnectionRef != null) {
                maintainConnectionRef.setValue(true);
                maintainedConnectionListener = maintainConnectionRef.addValueEventListener(new ValueEventListener() {
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void removeListeners(){
        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user!=null){
                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                if(IoT_Database!=null){
                    try {
                        IoT_Database.child(thingTag).child(initialTopicTag).removeEventListener(listener);
                    }catch(Exception e){
                        Log.e(TAG, e.toString());
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        removeListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeListeners();
    }
}
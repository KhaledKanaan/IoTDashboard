package com.kandroid.iotdashboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

//import org.eclipse.paho.android.service.MqttAndroidClient;

import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class  Topics extends AppCompatActivity {

    GridLayout gridLayout;
    public Gson gson = new Gson();
    public int listenerId;
    public ConnectivityManager connectivityManager;
    public ConnectivityManager.NetworkCallback networkCallback;
    public static String thingTag, thingName,
            ANALOG="Analog", DIGITAL="Digital", MULTISTATE="Text", READWRITE="Read/Write", READ="Read", WRITE="Write",
            ON="On", OFF="Off", val = null, VALUE = "Value", TYPE="Type", ACCESS = "Access", DESCRIPTION = "Name_Description",
            STEP = "Step", TOPIC_NAME="Topic_Name", TOPIC_TAG = "Topic_Tag", USER_DB_ROOT, dataToSend, lowLimitValue, highLimitValue,
            lowLimitNotificationValue, highLimitNotificationValue, unit, HIGH_LIMIT = "High_Limit", LOW_LIMIT = "Low_Limit",
            HIGH_LIMIT_NOTIFICATION_VALUE = "High_Limit_Notification_Value", LOW_LIMIT_NOTIFICATION_VALUE = "Low_Limit_Notification_Value",
            UNIT = "Unit", THING_TAG = "Thing_Tag", THING_DESCRIPTION_NAME = "Thing_Description_Name", DEFAULT_ACCESS = READWRITE,
            DEFAULT_LOW_LIMIT = null, DEFAULT_HIGH_LIMIT= null, DEFAULT_HIGH_LIMIT_NOTIFICATION_VALUE = null, DEFAULT_LOW_LIMIT_NOTIFICATION_VALUE = null,
            DEFAULT_UNIT = null, DEFAULT_DESCRIPTION ="", DEFAULT_ANALOG_VALUE = "0", DEFAULT_STEP = "1", TEMP_NODE = "Temp_Node",
            TEXT_ON_SET = "Text_On_Set", TEXT_ON_RESET = "Text_On_Reset", NOTIFY_IF_SET = "Notify_If_Set", DEFAULT_BOOLEAN_VALUE = "false",
            NOTIFY_IF_RESET = "Notify_If_Reset", DEFAULT_TEXT_ON_SET = "On", DEFAULT_TEXT_ON_RESET = "Off", USE_FEEDBACK_NODE = "Use_Feedback_Node",
            DEFAULT_NOTIFY_IF_SET = "No", DEFAULT_NOTIFY_IF_RESET = "No", textOnSet = DEFAULT_TEXT_ON_SET, FEEDBACK_NODE = "Feedback_Node",
            textOnReset = DEFAULT_TEXT_ON_RESET, DEFAULT_MULTISTATE_VALUE = "", NOTIFY_IF_EQUALS = "Notify_If_Equals",
            DIGITAL_ON_COLOR ="#2DC11D", DIGITAL_OFF_COLOR = "#AC1F1C", DIGITAL_ERROR_COLOR = "#AC003A", DIGITAL_ANALO_NORMAL_COLOR = "#7199FF", DEFAULT_ANALOG_FEEDBACK_VALUE = "0";

    private String TAG = "TopicsActivity";

    private boolean firstPreview;

    public boolean connectedToTheInternet = false;

    private final int feedbackNodeReviewDelay = 1500;

    public String getViewTag(View view){
        return String.valueOf(view.getTag()).trim();
    }

    public boolean isTagUsed(String tag){

        boolean isUsed = false;
        int topicCount = gridLayout.getChildCount();
        for(int i=0; i<topicCount; i++ ){

            if(getViewTag(gridLayout.getChildAt(i).findViewById(R.id.topicCard)).equals(tag)){
                isUsed = true;
            }
        }
        return isUsed;
    }

    public void addTopicWithFAB_Button(View view){
        addTopic();
    }

    public void addTopic() {
        LayoutInflater inflater = Topics.this.getLayoutInflater();
        View promptsView = inflater.inflate(R.layout.topic_selection_dialog, null);
        TextView topicNameDialog = (TextView) promptsView.findViewById(R.id.topicName);
        TextView topicTagDialog = (TextView) promptsView.findViewById(R.id.topicTag);

        AlertDialog Dialog = new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_baseline_add_30)
                .setTitle(Html.fromHtml("<font color='#333333'>Topic</font>"))
                .setView(promptsView)
                .setPositiveButton(Html.fromHtml("<font color='black'>Ok</font>"), null)
                .setNegativeButton(Html.fromHtml("<font color='black'>Cancel</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Your code if no
                    }
                })
                .show();
        Button positiveButton = Dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tag = topicTagDialog.getText().toString().trim();
                String name = topicNameDialog.getText().toString().trim();

                if(!tag.isEmpty()){
                    if (!isTagUsed(tag)){
                        RadioButton analogRadioButton = (RadioButton) promptsView.findViewById(R.id.analogRadioButton);
                        RadioButton digitalRadioButton = (RadioButton) promptsView.findViewById(R.id.digitalRadioButton);
                        RadioButton multistateRadioButton = (RadioButton) promptsView.findViewById(R.id.multistateRadioButton);

                        if(analogRadioButton.isChecked()){

                            inflateAnalogTopicCard(tag, name);
                            setDefaultAnalogTopicSettingsToFireBaseDatabase(thingTag, tag, name);

                        }
                        else if(digitalRadioButton.isChecked()){

                            inflateDigitalTopicCard(tag, name);
                            setDefaultDigitalTopicSettingsToFireBaseDatabase(thingTag, tag, name);

                        }
                        else if(multistateRadioButton.isChecked()){
                            inflateMultiStateTopicCard(tag, name);
                            setDefaultMultiStateTopicSettingsToFireBaseDatabase(thingTag, tag, name);
                        }
                        Dialog.dismiss();

                        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                                new Runnable() {
                                    public void run() {
                                        //scroll down to the new topic:
                                        ScrollView sv = (ScrollView)findViewById(R.id.topicsScrollView);
                                        sv.scrollTo(0, sv.getBottom());
                                    }
                                },
                                500);
                    }
                    else {
                        Toast.makeText(Topics.this, "Tag already used", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(Topics.this, "Tag can't be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void setTopicCardViewTagsAndTexts(View view, String tag, String name, String type){

        view.setTag(tag);
        view.findViewById(R.id.topicCard).setTag(tag);
        view.findViewById(R.id.deleteImageButton).setTag(tag);
        //view.findViewById(R.id.editImageButton).setTag(tag);
        view.findViewById(R.id.settingsImageButton).setTag(tag);
        view.findViewById(R.id.topicValue).setTag(tag);
        view.findViewById(R.id.topicName).setTag(tag);
        view.findViewById(R.id.topicTag).setTag(tag);
        view.findViewById(R.id.mcuImageView).setTag(tag);

        if(type.equals(ANALOG)){
            view.findViewById(R.id.decreaseImageButton).setTag(tag);
            view.findViewById(R.id.increaseImageButton).setTag(tag);
            view.findViewById(R.id.step).setTag(tag);
        }
        else if(type.equals(DIGITAL)){
            view.findViewById(R.id.onButton).setTag(tag);
            view.findViewById(R.id.offButton).setTag(tag);
        }
        else if(type.equals(MULTISTATE)){
            view.findViewById(R.id.stateEditText).setTag(tag);
            view.findViewById(R.id.sendButton).setTag(tag);
        }

        //Set the thing name text in the CardView same as the one chosen in the first configuration dialog:
        TextView topicNameTextView = (TextView) view.findViewById(R.id.topicName);
        topicNameTextView.setText(name);

        //Set the thing tag text in the CardView same as the one chosen in the first configuration dialog:
        TextView topicTagTextView = (TextView) view.findViewById(R.id.topicTag);
        topicTagTextView.setText(tag);
    }

    public void setDefaultAnalogTopicSettingsToFireBaseDatabase(String thingTag, String topicTag, String topicName){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            if(!(IoT_Database==null)){
                IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(DEFAULT_ANALOG_VALUE);
                IoT_Database.child(thingTag).child(topicTag).child(ACCESS).setValue(DEFAULT_ACCESS);
                IoT_Database.child(thingTag).child(topicTag).child(TYPE).setValue(ANALOG);
                IoT_Database.child(thingTag).child(topicTag).child(DESCRIPTION).setValue(topicName);
                IoT_Database.child(thingTag).child(topicTag).child(STEP).setValue(DEFAULT_STEP);
                IoT_Database.child(thingTag).child(topicTag).child(UNIT).setValue(DEFAULT_UNIT);
                IoT_Database.child(thingTag).child(topicTag).child(LOW_LIMIT).setValue(DEFAULT_LOW_LIMIT);
                IoT_Database.child(thingTag).child(topicTag).child(HIGH_LIMIT).setValue(DEFAULT_HIGH_LIMIT);
                IoT_Database.child(thingTag).child(topicTag).child(HIGH_LIMIT_NOTIFICATION_VALUE).
                        setValue(DEFAULT_HIGH_LIMIT_NOTIFICATION_VALUE);
                IoT_Database.child(thingTag).child(topicTag).child(LOW_LIMIT_NOTIFICATION_VALUE).
                        setValue(DEFAULT_LOW_LIMIT_NOTIFICATION_VALUE);
                IoT_Database.child(thingTag).child(topicTag).child(USE_FEEDBACK_NODE).setValue("No");
                IoT_Database.child(thingTag).child(topicTag).child(FEEDBACK_NODE).setValue(DEFAULT_ANALOG_FEEDBACK_VALUE);

            }
        }
    }

    public void setDefaultDigitalTopicSettingsToFireBaseDatabase(String thingTag, String topicTag, String topicName){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            if(!(IoT_Database==null)){
                IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(false);
                IoT_Database.child(thingTag).child(topicTag).child(FEEDBACK_NODE).setValue(false);
                IoT_Database.child(thingTag).child(topicTag).child(USE_FEEDBACK_NODE).setValue("No");
                IoT_Database.child(thingTag).child(topicTag).child(ACCESS).setValue(DEFAULT_ACCESS);
                IoT_Database.child(thingTag).child(topicTag).child(TYPE).setValue(DIGITAL);
                IoT_Database.child(thingTag).child(topicTag).child(DESCRIPTION).setValue(topicName);
                IoT_Database.child(thingTag).child(topicTag).child(TEXT_ON_SET).setValue(DEFAULT_TEXT_ON_SET);
                IoT_Database.child(thingTag).child(topicTag).child(TEXT_ON_RESET).setValue(DEFAULT_TEXT_ON_RESET);
                IoT_Database.child(thingTag).child(topicTag).child(NOTIFY_IF_SET).setValue(DEFAULT_NOTIFY_IF_SET);
                IoT_Database.child(thingTag).child(topicTag).child(NOTIFY_IF_RESET).setValue(DEFAULT_NOTIFY_IF_RESET);

            }
        }
    }

    public void setDefaultMultiStateTopicSettingsToFireBaseDatabase(String thingTag, String topicTag, String topicName){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            if(!(IoT_Database==null)){
                IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(DEFAULT_MULTISTATE_VALUE);
                IoT_Database.child(thingTag).child(topicTag).child(ACCESS).setValue(DEFAULT_ACCESS);
                IoT_Database.child(thingTag).child(topicTag).child(TYPE).setValue(MULTISTATE);
                IoT_Database.child(thingTag).child(topicTag).child(DESCRIPTION).setValue(topicName);
                IoT_Database.child(thingTag).child(topicTag).child(NOTIFY_IF_EQUALS).setValue(null);

            }
        }
    }

    public synchronized void getSingleTopicDataFromFireBaseDatabaseAndUpdateTopicCard (View v){

        String topicTag = getViewTag(v);
        View view = v;

        if(!topicTag.isEmpty() && topicTag != null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                if(!(IoT_Database==null)){
                    // add listener to the topic node and get the relative data each time any node changes
                    IoT_Database.child(thingTag).child(topicTag)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)){

                                        // get the topic data and put it in a map
                                        Map<String, Object> topicData = new HashMap<>();
                                        topicData = (Map<String, Object>) dataSnapshot.getValue();

                                        //value, name, type and access are common for all types of topic
                                        String value = String.valueOf(topicData.get(VALUE));
                                        String name = String.valueOf(topicData.get(DESCRIPTION));
                                        String type = String.valueOf(topicData.get(TYPE));
                                        String access = String.valueOf(topicData.get(ACCESS));


                                        if(topicData.get(DESCRIPTION)!=null){
                                            TextView descriptionTextView = (TextView) view.findViewById(R.id.topicName);
                                            descriptionTextView.setText(name);
                                        }

                                        if(topicData.get(TYPE)!=null){
                                            TextView topicTypeTextView = (TextView) view.findViewById(R.id.topicType);
                                            topicTypeTextView.setText(type);
                                        }

                                        if(topicData.get(ACCESS)!=null){
                                            TextView topicAccessTextView = (TextView) view.findViewById(R.id.topicAccess);
                                            topicAccessTextView.setText(access);

                                            //set the visibilities of buttons base on access type:
                                            if(access.equals(READ)){
                                                if(type.equals(ANALOG)){
                                                    view.findViewById(R.id.increaseImageButton).setVisibility(View.INVISIBLE);
                                                    view.findViewById(R.id.decreaseImageButton).setVisibility(View.INVISIBLE);
                                                }
                                                else if(type.equals(DIGITAL)){
                                                    view.findViewById(R.id.onButton).setVisibility(View.INVISIBLE);
                                                    view.findViewById(R.id.offButton).setVisibility(View.INVISIBLE);
                                                }
                                                else if(type.equals(MULTISTATE)){
                                                    view.findViewById(R.id.sendButton).setVisibility(View.INVISIBLE);
                                                    view.findViewById(R.id.stateEditText).setVisibility(View.INVISIBLE);
                                                }
                                            }
                                            else if(access.equals(READWRITE)){
                                                if(type.equals(ANALOG)){
                                                    view.findViewById(R.id.increaseImageButton).setVisibility(View.VISIBLE);
                                                    view.findViewById(R.id.decreaseImageButton).setVisibility(View.VISIBLE);
                                                }
                                                else if(type.equals(DIGITAL)){
                                                    view.findViewById(R.id.onButton).setVisibility(View.VISIBLE);
                                                    view.findViewById(R.id.offButton).setVisibility(View.VISIBLE);
                                                }
                                                else if(type.equals(MULTISTATE)){
                                                    view.findViewById(R.id.sendButton).setVisibility(View.VISIBLE);
                                                    view.findViewById(R.id.stateEditText).setVisibility(View.VISIBLE);
                                                }
                                            }
                                        }

                                        TextView topicValueTextView = (TextView) view.findViewById(R.id.topicValue);

                                        if(type.equals(ANALOG)){

                                            String useFeedbackNode = String.valueOf(topicData.get(USE_FEEDBACK_NODE));
                                            String feedback = String.valueOf(topicData.get(FEEDBACK_NODE));
                                            String highLimit = String.valueOf(topicData.get(HIGH_LIMIT));
                                            String lowLimit = String.valueOf(topicData.get(LOW_LIMIT));
                                            String highLimitNotification = String.valueOf(topicData.get(HIGH_LIMIT_NOTIFICATION_VALUE));
                                            String lowLimitNotification = String.valueOf(topicData.get(LOW_LIMIT_NOTIFICATION_VALUE));
                                            String step = String.valueOf(topicData.get(STEP));
                                            String unit = String.valueOf(topicData.get(UNIT));


                                            if(topicData.get(VALUE)!= null && NumberUtils.isCreatable(value)){

                                                topicValueTextView.setTextColor(Color.parseColor(DIGITAL_ANALO_NORMAL_COLOR));

                                                if(topicData.get(HIGH_LIMIT)!=null && NumberUtils.isCreatable(highLimit)){
                                                    BigDecimal highLimitValue = new BigDecimal(highLimit);
                                                    BigDecimal analogValue = new BigDecimal(value);
                                                    if(analogValue.compareTo(highLimitValue)>0){
                                                        value = highLimit;
                                                        IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(highLimit);
                                                    }
                                                }

                                                if(topicData.get(LOW_LIMIT)!=null && NumberUtils.isCreatable(lowLimit)){
                                                    BigDecimal lowLimitValue = new BigDecimal(lowLimit);
                                                    BigDecimal analogValue = new BigDecimal(value);
                                                    if(analogValue.compareTo(lowLimitValue)<0){
                                                        value = lowLimit;
                                                        IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(lowLimit);
                                                    }
                                                }

                                                if(useFeedbackNode.equals("Yes")){
                                                    if(!firstPreview){
                                                        topicValueTextView.setText(value);
                                                        Map<String, Object> finalTopicData = topicData;
                                                        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                                                                new Runnable() {
                                                                    public void run() {
                                                                        if(finalTopicData.get(FEEDBACK_NODE)!=null){
                                                                            topicValueTextView.setText(feedback);
                                                                        }
                                                                    }
                                                                }, feedbackNodeReviewDelay);
                                                    }
                                                    else{
                                                        if(topicData.get(FEEDBACK_NODE)!=null){
                                                            topicValueTextView.setText(feedback);
                                                        }
                                                    }
                                                    view.findViewById(R.id.feedbackSignal).setVisibility(View.VISIBLE);

                                                }else{
                                                    topicValueTextView.setText(value);
                                                    view.findViewById(R.id.feedbackSignal).setVisibility(View.INVISIBLE);
                                                }
                                            }else {
                                                topicValueTextView.setText("NaN");
                                                topicValueTextView.setTextColor(Color.parseColor(DIGITAL_ERROR_COLOR));
                                            }


                                            if(topicData.get(STEP)!=null){
                                                TextView topicStepTextView = (TextView) view.findViewById(R.id.step);
                                                topicStepTextView.setText(step);
                                            }

                                            if(topicData.get(UNIT)!=null){
                                                TextView unitTextView = (TextView) view.findViewById(R.id.unitTextView);
                                                unitTextView.setText(unit);
                                            }
                                        }

                                        if(type.equals(DIGITAL)){
                                            String useFeedbackNode = String.valueOf(topicData.get(USE_FEEDBACK_NODE));
                                            String feedback = String.valueOf(topicData.get(FEEDBACK_NODE));
                                            String notifyIfSet = String.valueOf(topicData.get(NOTIFY_IF_SET));
                                            String notifyIfReset = String.valueOf(topicData.get(NOTIFY_IF_RESET));
                                            String textOnSet = String.valueOf(topicData.get(TEXT_ON_SET));
                                            String textOnReset = String.valueOf(topicData.get(TEXT_ON_RESET));

                                            if(topicData.get(VALUE)!=null){
                                                if(useFeedbackNode.equals("Yes")){
                                                    if(!firstPreview){
                                                        if(value.equals("true") && topicData.get(TEXT_ON_SET)!=null){
                                                            topicValueTextView.setText(textOnSet);
                                                            topicValueTextView.setTextColor(Color.parseColor(DIGITAL_ON_COLOR));
                                                            updateDigitalButtonsTexts(topicTag, view);
                                                        }

                                                        else if(value.equals("false") && topicData.get(TEXT_ON_RESET)!=null) {
                                                            topicValueTextView.setText(textOnReset);
                                                            topicValueTextView.setTextColor(Color.parseColor(DIGITAL_OFF_COLOR));
                                                            updateDigitalButtonsTexts(topicTag, view);
                                                        }

                                                        if(feedback!=null){
                                                            Map<String, Object> finalTopicData1 = topicData;
                                                            new android.os.Handler(Looper.getMainLooper()).postDelayed(
                                                                    new Runnable() {
                                                                        public void run() {
                                                                            if(feedback.equals("true") && finalTopicData1.get(TEXT_ON_SET)!=null){
                                                                                topicValueTextView.setText(textOnSet);
                                                                                topicValueTextView.setTextColor(Color.parseColor(DIGITAL_ON_COLOR));
                                                                                updateDigitalButtonsTexts(topicTag, view);
                                                                            }
                                                                            else if(feedback.equals("false") && finalTopicData1.get(TEXT_ON_RESET)!=null) {
                                                                                topicValueTextView.setText(textOnReset);
                                                                                topicValueTextView.setTextColor(Color.parseColor(DIGITAL_OFF_COLOR));
                                                                                updateDigitalButtonsTexts(topicTag, view);
                                                                            }
                                                                        }
                                                                    }, feedbackNodeReviewDelay);
                                                        }
                                                    }
                                                    else{
                                                        if(topicData.get(FEEDBACK_NODE)!=null){
                                                            if(feedback.equals("true") && topicData.get(TEXT_ON_SET)!=null){
                                                                topicValueTextView.setText(textOnSet);
                                                                topicValueTextView.setTextColor(Color.parseColor(DIGITAL_ON_COLOR));
                                                                updateDigitalButtonsTexts(topicTag, view);
                                                            }
                                                            else if(feedback.equals("false") && topicData.get(TEXT_ON_RESET)!=null) {
                                                                topicValueTextView.setText(textOnReset);
                                                                topicValueTextView.setTextColor(Color.parseColor(DIGITAL_OFF_COLOR));
                                                                updateDigitalButtonsTexts(topicTag, view);
                                                            }
                                                        }
                                                    }
                                                    view.findViewById(R.id.feedbackSignal).setVisibility(View.VISIBLE);
                                                }
                                                else if(useFeedbackNode.equals("No")){
                                                    if(value.equals("true") && topicData.get(TEXT_ON_SET)!=null){
                                                        topicValueTextView.setText(textOnSet);
                                                        topicValueTextView.setTextColor(Color.parseColor(DIGITAL_ON_COLOR));
                                                        updateDigitalButtonsTexts(topicTag, view);
                                                    }

                                                    else if(value.equals("false") && topicData.get(TEXT_ON_RESET)!=null) {
                                                        topicValueTextView.setText(textOnReset);
                                                        topicValueTextView.setTextColor(Color.parseColor(DIGITAL_OFF_COLOR));
                                                        updateDigitalButtonsTexts(topicTag, view);
                                                    }
                                                    view.findViewById(R.id.feedbackSignal).setVisibility(View.INVISIBLE);
                                                }
                                            }
                                        }

                                        if(type.equals(MULTISTATE)){
                                            Object notifyIfEquals = topicData.get(NOTIFY_IF_EQUALS);
                                            if(value!=null){
                                                topicValueTextView.setText(value);
                                            }
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

    public void updateDigitalButtonsTexts(String tTag, View v){

        View view = v;
        String topicTag = tTag;

        ImageButton onButton = (ImageButton) view.findViewById(R.id.onButton);
        ImageButton offButton = (ImageButton) view.findViewById(R.id.offButton);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            if(!(IoT_Database == null)){
                IoT_Database.child(thingTag).child(topicTag).child(Topics.TEXT_ON_SET)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (!(dataSnapshot.getValue() == null)) {
                                    String textOnSet = dataSnapshot.getValue().toString();
                                    //onButton.setText(textOnSet);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });

                IoT_Database.child(thingTag).child(topicTag).child(Topics.TEXT_ON_RESET)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (!(dataSnapshot.getValue() == null)) {
                                    String textOnReset = dataSnapshot.getValue().toString();
                                    //offButton.setText(textOnReset);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
            }
        }
    }

    public void inflateAnalogTopicCard(String tag, String name){

        if(!isTagUsed(tag)){
            View topicView = getLayoutInflater().inflate(R.layout.topic_analog,null, false);
            setTopicCardViewTagsAndTexts(topicView, tag, name, ANALOG);
            gridLayout.addView(topicView);

            // get all the topic data and update the the topic card:
            int parentViewPosition = getViewParentTopicCardPositionWithTheSameTag(tag);
            if(parentViewPosition>-1){
                View parentView = gridLayout.getChildAt(parentViewPosition);

                parentView.findViewById(R.id.increaseImageButton).setVisibility(View.INVISIBLE);
                parentView.findViewById(R.id.decreaseImageButton).setVisibility(View.INVISIBLE);

                getSingleTopicDataFromFireBaseDatabaseAndUpdateTopicCard(parentView);
            }

        }else Toast.makeText(Topics.this, "Tag "+ tag +" is already used", Toast.LENGTH_LONG).show();
    }

    public void inflateDigitalTopicCard(String tag, String name){

        if(!isTagUsed(tag)){
            View topicView = getLayoutInflater().inflate(R.layout.topic_digital,null, false);
            setTopicCardViewTagsAndTexts(topicView, tag, name, DIGITAL);
            gridLayout.addView(topicView);

            // get all the topic data and update the the topic card:
            int parentViewPosition = getViewParentTopicCardPositionWithTheSameTag(tag);
            if(parentViewPosition>-1){
                View parentView = gridLayout.getChildAt(parentViewPosition);

                parentView.findViewById(R.id.onButton).setVisibility(View.INVISIBLE);
                parentView.findViewById(R.id.offButton).setVisibility(View.INVISIBLE);

                getSingleTopicDataFromFireBaseDatabaseAndUpdateTopicCard(parentView);
            }
        }else Toast.makeText(Topics.this, "Tag "+ tag +" is already used", Toast.LENGTH_LONG).show();
    }

    public void inflateMultiStateTopicCard(String tag, String name){

        if(!isTagUsed(tag)){
            View topicView = getLayoutInflater().inflate(R.layout.topic_multistate,null, false);
            setTopicCardViewTagsAndTexts(topicView, tag, name, MULTISTATE);
            gridLayout.addView(topicView);

            // get all the topic data and update the the topic card:
            int parentViewPosition = getViewParentTopicCardPositionWithTheSameTag(tag);
            if(parentViewPosition>-1){
                View parentView = gridLayout.getChildAt(parentViewPosition);

                parentView.findViewById(R.id.sendButton).setVisibility(View.INVISIBLE);
                parentView.findViewById(R.id.stateEditText).setVisibility(View.INVISIBLE);

                getSingleTopicDataFromFireBaseDatabaseAndUpdateTopicCard(parentView);
            }
        }else Toast.makeText(Topics.this, "Tag " + tag + " is already used", Toast.LENGTH_LONG).show();
    }

    public int getViewParentTopicCardPosition(View view){
        int parentViewPosition = -1;
        int topicCount = gridLayout.getChildCount();
        if(topicCount>0){
            for(int i=0; i<topicCount; i++ ){
                if(getViewTag(gridLayout.getChildAt(i).findViewById(R.id.topicCard)).equals(getViewTag(view))){
                    parentViewPosition = i;
                }
            }
        }
        return parentViewPosition;
    }

    public int getViewParentTopicCardPositionWithTheSameTag(String tag){
        int parentViewPosition = -1;
        int topicCount = gridLayout.getChildCount();
        if(topicCount>0){
            for(int i=0; i<topicCount; i++ ){
                if(getViewTag(gridLayout.getChildAt(i).findViewById(R.id.topicCard)).equals(tag)){
                    parentViewPosition = i;
                }
            }
        }
        return parentViewPosition;
    }


    public void increaseValue(View v){

        View view = v;

        int parentViewPosition = getViewParentTopicCardPosition(view);
        if(parentViewPosition>-1){
            View parentView = gridLayout.getChildAt(parentViewPosition);
            String topicTag = getViewTag(parentView);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                if(IoT_Database != null){
                    IoT_Database.child(thingTag).child(topicTag).child(STEP)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.getValue() != null && NumberUtils.isCreatable(snapshot.getValue().toString().trim())) {
                                        BigDecimal stepValue = new BigDecimal(snapshot.getValue().toString().trim());
                                        IoT_Database.child(thingTag).child(topicTag).child(VALUE)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        if (snapshot.getValue() != null  && NumberUtils.isCreatable(snapshot.getValue().toString().trim())) {
                                                            BigDecimal analogValue = new BigDecimal(snapshot.getValue().toString().trim());
                                                            IoT_Database.child(thingTag).child(topicTag).child(HIGH_LIMIT)
                                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                        @Override
                                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                            if(!(snapshot.getValue() == null)){
                                                                                String highLimit = snapshot.getValue().toString().trim();
                                                                                if (!highLimit.isEmpty() && NumberUtils.isCreatable(highLimit)){
                                                                                    BigDecimal highLimitValue = new BigDecimal(highLimit);
                                                                                    if(!(analogValue.compareTo(highLimitValue) >= 0)){
                                                                                        boolean error = false;
                                                                                        BigDecimal result = new BigDecimal("0");
                                                                                        result = analogValue.add(stepValue);
                                                                                        if(result.compareTo(highLimitValue)>0){
                                                                                            result = highLimitValue;
                                                                                            Toast.makeText(getApplicationContext(),".../"+thingTag+"/"+topicTag+": High limit reached", Toast.LENGTH_SHORT).show();
                                                                                            IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(true);
                                                                                            IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(null);
                                                                                        }
                                                                                        try{
                                                                                            IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(String.valueOf(result));
                                                                                        }
                                                                                        catch (Exception e){
                                                                                            Log.e(TAG, e.toString());
                                                                                            error = true;
                                                                                        }
                                                                                    }else {
                                                                                        Toast.makeText(getApplicationContext(),".../"+thingTag+"/"+topicTag+": High limit reached", Toast.LENGTH_SHORT).show();
                                                                                        IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(true);
                                                                                        IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(null);
                                                                                    }
                                                                                }
                                                                            }else{
                                                                                try{
                                                                                    BigDecimal result = new BigDecimal("0");
                                                                                    result = analogValue.add(stepValue);
                                                                                    IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(String.valueOf(result));
                                                                                }
                                                                                catch (Exception e){
                                                                                    Log.e(TAG, e.toString());
                                                                                }
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                                        }
                                                                    });

                                                        }else Toast.makeText(getApplicationContext(), "The topic value is NaN", Toast.LENGTH_SHORT).show();
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                    }else Toast.makeText(getApplicationContext(), "Step value is NaN", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                }
            }
        }
    }

    public void decreaseValue(View v){

        View view = v;

        int parentViewPosition = getViewParentTopicCardPosition(view);
        if(parentViewPosition>-1){
            View parentView = gridLayout.getChildAt(parentViewPosition);
            String topicTag = getViewTag(parentView);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                if(IoT_Database!=null){
                    IoT_Database.child(thingTag).child(topicTag).child(STEP)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.getValue() != null && NumberUtils.isCreatable(snapshot.getValue().toString().trim())) {
                                        BigDecimal stepValue = new BigDecimal(snapshot.getValue().toString().trim());
                                        IoT_Database.child(thingTag).child(topicTag).child(VALUE)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        if (snapshot.getValue() != null && NumberUtils.isCreatable(snapshot.getValue().toString().trim())) {
                                                            BigDecimal analogValue = new BigDecimal(snapshot.getValue().toString().trim());
                                                            IoT_Database.child(thingTag).child(topicTag).child(LOW_LIMIT)
                                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                        @Override
                                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                            if (!(snapshot.getValue() == null)) {
                                                                                String lowLimit = snapshot.getValue().toString().trim();
                                                                                if (!lowLimit.isEmpty() && NumberUtils.isCreatable(lowLimit)) {
                                                                                    //float lowLimitValue = Float.parseFloat(lowLimit);
                                                                                    BigDecimal lowLimitValue = new BigDecimal(lowLimit);
                                                                                    if (!(analogValue.compareTo(lowLimitValue) <= 0)) {
                                                                                        boolean error = false;
                                                                                        BigDecimal result = new BigDecimal("0");
                                                                                        result = analogValue.subtract(stepValue);
                                                                                        if (result.compareTo(lowLimitValue) < 0) {
                                                                                            result = lowLimitValue;
                                                                                            Toast.makeText(getApplicationContext(), ".../"+thingTag + "/" + topicTag + ": Low limit reached", Toast.LENGTH_SHORT).show();
                                                                                            IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(true);
                                                                                            IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(null);
                                                                                        }
                                                                                        try {
                                                                                            IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(String.valueOf(result));
                                                                                        } catch (Exception e) {
                                                                                            Log.e(TAG, e.toString());
                                                                                            error = true;
                                                                                        }

                                                                                    } else {
                                                                                        Toast.makeText(getApplicationContext(), ".../"+thingTag + "/" + topicTag + ": Low limit reached", Toast.LENGTH_SHORT).show();
                                                                                        IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(true);
                                                                                        IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(null);
                                                                                    }

                                                                                }
                                                                            } else {
                                                                                try {
                                                                                    BigDecimal result = new BigDecimal("0");
                                                                                    result = analogValue.subtract(stepValue);
                                                                                    IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(String.valueOf(result));
                                                                                } catch (Exception e) {
                                                                                    Log.e(TAG, e.toString());
                                                                                }
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                                        }
                                                                    });
                                                        }else Toast.makeText(getApplicationContext(), "The topic value is NaN", Toast.LENGTH_SHORT).show();
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                    }else Toast.makeText(getApplicationContext(), "Step value is NaN", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                }
            }
        }
    }

    public void turnOn(View v){

        View view = v;
        int parentViewPosition = getViewParentTopicCardPosition(view);
        if(parentViewPosition>-1){
            View parentView = gridLayout.getChildAt(parentViewPosition);
            String topicTag = getViewTag(view);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                if(IoT_Database != null) {
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.TEXT_ON_SET)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!(dataSnapshot.getValue() == null)) {
                                        boolean error = false;
                                        try{
                                            IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(true);

                                            IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(true);
                                            IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(null);

                                        }catch (Exception e){
                                            Log.e(TAG, e.toString());
                                        }
                                    }
                                    else{
                                        try{
                                            IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(DEFAULT_TEXT_ON_SET);
                                        }catch (Exception e){
                                            Log.e(TAG, e.toString());
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

    public void turnOff(View v){

        View view = v;
        int parentViewPosition = getViewParentTopicCardPosition(view);
        if(parentViewPosition>-1){

            View parentView = gridLayout.getChildAt(parentViewPosition);
            String topicTag = getViewTag(view);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                if(IoT_Database != null) {
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.TEXT_ON_RESET)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!(dataSnapshot.getValue() == null)) {
                                        try{
                                            IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(false);

                                            IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(true);
                                            IoT_Database.child(thingTag).child(topicTag).child(TEMP_NODE).setValue(null);

                                        }catch (Exception e){
                                            Log.e(TAG, e.toString());
                                        }
                                    }
                                    else{
                                        try{
                                            IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(DEFAULT_TEXT_ON_RESET);
                                        }catch (Exception e){
                                            Log.e(TAG, e.toString());
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

    public void topicSettings(View v){

        View view = v;
        int parentViewPosition = getViewParentTopicCardPosition(view);
        if(parentViewPosition>-1){

            View parentView = gridLayout.getChildAt(parentViewPosition);

            TextView topicTypeTextView = (TextView) parentView.findViewById(R.id.topicType);
            String type = topicTypeTextView.getText().toString();

            TextView topicTagTextView = (TextView) parentView.findViewById(R.id.topicTag);
            String topicTag = topicTagTextView.getText().toString();

            if(type.equals(ANALOG)){

                if(!topicTag.isEmpty()){
                    Intent intent = new Intent(getApplicationContext(), AnalogTopicConfigurationActivity.class);
                    intent.putExtra(THING_TAG, thingTag);
                    intent.putExtra(THING_DESCRIPTION_NAME, thingName);
                    intent.putExtra(TOPIC_TAG, topicTag);
                    startActivity(intent);
                }
            }
            else if(type.equals(DIGITAL)){

                if(!topicTag.isEmpty()){
                    Intent intent = new Intent(getApplicationContext(), DigitalTopicConfigurationActivity.class);
                    intent.putExtra(THING_TAG, thingTag);
                    intent.putExtra(THING_DESCRIPTION_NAME, thingName);
                    intent.putExtra(TOPIC_TAG, topicTag);
                    startActivity(intent);
                }
            }
            else if(type.equals(MULTISTATE)){

                if(!topicTag.isEmpty()){
                    Intent intent = new Intent(getApplicationContext(), MultiStateTopicConfigurationActivity.class);
                    intent.putExtra(THING_TAG, thingTag);
                    intent.putExtra(THING_DESCRIPTION_NAME, thingName);
                    intent.putExtra(TOPIC_TAG, topicTag);
                    startActivity(intent);
                }
            }
        }
    }

    public void sendMS(View v){

        View view = v;

        boolean error = false;
        int parentViewPosition = getViewParentTopicCardPosition(view);
        if(parentViewPosition>-1){
            View parentView = gridLayout.getChildAt(parentViewPosition);
            String topicTag = getViewTag(view);
            EditText stateEditText = (EditText) parentView.findViewById(R.id.stateEditText);
            String state = stateEditText.getText().toString().trim();
//            TextView topicValueTextView = findViewById(R.id.topicValue);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                if(!(IoT_Database==null)) {
                    try{
                        IoT_Database.child(thingTag).child(topicTag).child(VALUE).setValue(state);
                    }
                    catch (Exception e){
                        Log.e(TAG, e.toString());
                        error = true;
                    }
                    if(!error){
                        stateEditText.setText("");
                    }
                }
            }
        }
    }

    public void deleteTopic(View view){

        AlertDialog Dialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Delete topic?")
                //.setMessage("All data related to this thing will be erased, continue?")
                .setPositiveButton(Html.fromHtml("<font color='black'>Yes</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int indexToBeDeleted = getViewParentTopicCardPosition(view);
                        if(indexToBeDeleted>-1) {
                            String topicTagToDelete= getViewTag(view);
                            gridLayout.removeViewAt(indexToBeDeleted);
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if(user != null){
                                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                                if(!(IoT_Database==null)){
                                    IoT_Database.child(thingTag).child(topicTagToDelete).removeValue();
                                }
                            }
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

    public synchronized void retrievePreviousTopicsFromFireBaseDatabaseAndListThem(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            if(IoT_Database != null){
                IoT_Database.child(thingTag).addListenerForSingleValueEvent(new ValueEventListener() { //addValueEventListener
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        int count = 0;
                        List<String> topicTagsList = new ArrayList<String>();
                        topicTagsList.clear();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) { // fetch all the topics tags from firebase DB and store them in a list
                            topicTagsList.add(snapshot.getKey());
                            count ++;
                            if (count == dataSnapshot.getChildrenCount()){ // make sure all topics tags are retrieved from the firebase DB before start listing them

                                for (int j = 0; j < topicTagsList.size(); j++) { // start listing them in the gridlayout

                                    if(!topicTagsList.get(j).isEmpty() && !(topicTagsList.get(j)==null)) {

                                        // read the description from firebase DB and update the topic card:
                                        int finalJ = j;
                                        IoT_Database.child(thingTag).child(topicTagsList.get(j)).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                IoT_Database.child(thingTag).child(topicTagsList.get(finalJ)).child(DESCRIPTION)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                if(!(dataSnapshot.getValue() == null)){
                                                                    String topicName = dataSnapshot.getValue().toString();

                                                                    // read the type from firebase DB and update the topic card:
                                                                    if(!topicName.equals(MainActivity.THING_NAME_TOPIC_DESCRIPTION)){

                                                                        IoT_Database.child(thingTag).child(topicTagsList.get(finalJ)).child(TYPE)
                                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                    @Override
                                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                                        if(!(dataSnapshot.getValue() == null)){
                                                                                            String type = dataSnapshot.getValue().toString();
                                                                                            if(!isTagUsed(topicTagsList.get(finalJ))){
                                                                                                if (type.equals(ANALOG)) {
                                                                                                    inflateAnalogTopicCard(topicTagsList.get(finalJ), topicName);
                                                                                                } else if (type.equals(DIGITAL)) {
                                                                                                    inflateDigitalTopicCard(topicTagsList.get(finalJ), topicName);
                                                                                                } else {
                                                                                                    inflateMultiStateTopicCard(topicTagsList.get(finalJ), topicName);
                                                                                                }
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
                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                            }
                                                        });
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }

                                    if (j == topicTagsList.size()-1){

                                        //remove unused topic card views (deleted by other devices while this device was offline):
                                        for(int k=0; k < gridLayout.getChildCount(); k++){

                                            if (!topicTagsList.contains(getViewTag(gridLayout.getChildAt(k)))){
                                                gridLayout.removeViewAt(k);
                                            }
                                        }

                                        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                                                new Runnable() {
                                                    public void run() {
                                                        firstPreview = false;
                                                    }
                                                }, feedbackNodeReviewDelay);
                                    }
                                }
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

    public void onAddOrRemoveChildren(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            if(!(IoT_Database==null)){
                IoT_Database.child(thingTag).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        retrievePreviousTopicsFromFireBaseDatabaseAndListThem();
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                        retrievePreviousTopicsFromFireBaseDatabaseAndListThem();
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
        firstPreview = true;
        listenerId = 0;
        retrievePreviousTopicsFromFireBaseDatabaseAndListThem();
        onAddOrRemoveChildren();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topics);

        Toolbar toolbar = (Toolbar) findViewById(R.id.topicsToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent intent = getIntent();
        thingTag = intent.getStringExtra("Tag");
        thingName = intent.getStringExtra("Name");

        TextView thingNameTextView = (TextView) findViewById(R.id.thingNameInTopics);
        TextView thingTagTextView = (TextView) findViewById(R.id.thingTagInTopics);

        thingNameTextView.setText(thingName);
        thingTagTextView.setText(thingTag);

        //toolbar.setTitle(thingTag);
        //toolbar.setSubtitle("Thing name: "+thingName+"  -  "+"Thing tag: "+thingTag);

        gridLayout = (GridLayout) findViewById(R.id.topicsGridLayout);

        monitorInternetConnection();
        getConnectionToFireBaseDataBaseStatus();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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

    public void getConnectionToFireBaseDataBaseStatus(){

        if (!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
            connectedRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = snapshot.getValue(Boolean.class);
                    CardView cr = findViewById(R.id.firebaseConnectionLed);
                    if(connected) {
                        cr.setCardBackgroundColor(getResources().getColor(R.color.color_8, getTheme()));
                    } else {
                        cr.setCardBackgroundColor(getResources().getColor(R.color.black, getTheme()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
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

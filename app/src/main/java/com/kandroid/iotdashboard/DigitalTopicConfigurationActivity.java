package com.kandroid.iotdashboard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DigitalTopicConfigurationActivity extends AppCompatActivity {

    public static AutoCompleteTextView initialValueAutoCompleteTextView, notifyIfSetAutoCompleteTextView,
            notifyIfResetAutoCompleteTextView, useFeedbackNodeAutoCompleteTextView;
    private ArrayList<String> notifyIf_X_ArrayList = new ArrayList<String>() {{add("Yes"); add("No");}};

    public static String topicTag, initialTopicTag, topicName, thingTag, thingName, TAG="Digital Topic Configuration Activity",
            previousValueOnSet, previousValueOnReset, previousValue,
            textOnSet, textOnReset, ERROR_TOR_NE_TOS = "Text on reset should not be\nequal to text on set",
            ERROR_TOS_NE_EMPTY = "Text on set should\nnot be empty", ERROR_TOR_NE_EMPTY = "Text on reset should\nnot be empty";

    public EditText topicEditText, topicNameEditText, textOnSetEditText, textOnResetEditText, feedbackValueEditText;

    public TextView thingDescriptionNameTextView, thingTagTextView;

    public RadioButton ReadWriteRadioButton, ReadRadioButton;

    public boolean error, isTopicChanged;

    public void getPreviousValueAndTextsOnSetAndReset(String topicTag){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null){
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            if(!(IoT_Database==null)) {

                IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (!(dataSnapshot.getValue() == null)) {
                                    previousValue = dataSnapshot.getValue().toString();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });

                IoT_Database.child(thingTag).child(topicTag).child(Topics.TEXT_ON_SET)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (!(dataSnapshot.getValue() == null)) {
                                    previousValueOnSet = dataSnapshot.getValue().toString();
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
                                    previousValueOnReset = dataSnapshot.getValue().toString();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });

            }
        }
    }

    public void retrieveValue(String topicTag, String textOnSet, String textOnReset){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (!(dataSnapshot.getValue() == null)) {
                                String value = dataSnapshot.getValue().toString();

                                ArrayList<String> textsOn_X_ArrayList = new ArrayList<String>() {{
                                    add(textOnSet);
                                    add(textOnReset);
                                }};
                                ArrayAdapter<String> initialValueArrayAdapter = new ArrayAdapter<String>(DigitalTopicConfigurationActivity.this,
                                        R.layout.support_simple_spinner_dropdown_item, textsOn_X_ArrayList);
                                initialValueAutoCompleteTextView.setAdapter(initialValueArrayAdapter);

//                          initialValueAutoCompleteTextView.setText(value, false);

                                if (value.equals("true")) {
                                    initialValueAutoCompleteTextView.setText(textOnSet, false);
                                } else if (value.equals("false")) {
                                    initialValueAutoCompleteTextView.setText(textOnReset, false);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
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
                                    if(feedbackValue.equals("true")){
                                        IoT_Database.child(thingTag).child(newTopicTag).child(Topics.FEEDBACK_NODE).setValue(true);
                                    }
                                    else if(feedbackValue.equals("false")) {
                                        IoT_Database.child(thingTag).child(newTopicTag).child(Topics.FEEDBACK_NODE).setValue(false);
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

    public void retrieveFeedbackValue(String topicTag, String textOnSet, String textOnReset) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            IoT_Database.child(thingTag).child(topicTag).child(Topics.FEEDBACK_NODE)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (!(dataSnapshot.getValue() == null)) {
                                String feedbackValue = dataSnapshot.getValue().toString();

                                ArrayList<String> textsOn_X_ArrayList = new ArrayList<String>() {{
                                    add(textOnSet);
                                    add(textOnReset);
                                }};
                                ArrayAdapter<String> initialValueArrayAdapter = new ArrayAdapter<String>(DigitalTopicConfigurationActivity.this,
                                        R.layout.support_simple_spinner_dropdown_item, textsOn_X_ArrayList);
                                initialValueAutoCompleteTextView.setAdapter(initialValueArrayAdapter);

                                if (feedbackValue.equals("true")) {
                                    initialValueAutoCompleteTextView.setText(textOnSet, false);
                                } else if (feedbackValue.equals("false")) {
                                    initialValueAutoCompleteTextView.setText(textOnReset, false);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
        }
    }

    public void retrieveSettingsValuesFromFirebaseDatabase(String topicTag){
        if(!topicTag.isEmpty() && !(topicTag==null)) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user!=null) {
                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());

                if (!(IoT_Database == null)) {

                    topicEditText.setText(topicTag);

                    IoT_Database.child(thingTag).child(topicTag).child(Topics.ACCESS)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!(dataSnapshot.getValue() == null)) {
                                        String access = dataSnapshot.getValue().toString();

                                        // ReadWriteRadioButton = findViewById(R.id.RW_RadioButton);
                                        //ReadRadioButton = findViewById(R.id.R_RadioButton);

                                        if (access.equals(Topics.READWRITE)) {
                                            ReadWriteRadioButton.setChecked(true);
                                        } else if (access.equals(Topics.READ)) {
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
                                    if (!(dataSnapshot.getValue() == null)) {
                                        String topicName = dataSnapshot.getValue().toString();
                                        //topicNameEditText = findViewById(R.id.topicDescriptionEditText);
                                        topicNameEditText.setText(topicName);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });


                    IoT_Database.child(thingTag).child(topicTag).child(Topics.TEXT_ON_SET)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    //String textOnSet = Topics.DEFAULT_TEXT_ON_SET;
                                    if (!(dataSnapshot.getValue() == null)) {
                                        String textOnSet = dataSnapshot.getValue().toString();
                                        textOnSetEditText.setText(textOnSet);

                                        String finalTextOnSet = textOnSet;

                                        IoT_Database.child(thingTag).child(topicTag).child(Topics.TEXT_ON_RESET)
                                                .addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        //String textOnReset = Topics.DEFAULT_TEXT_ON_RESET;
                                                        if (!(dataSnapshot.getValue() == null)) {
                                                            String textOnReset = dataSnapshot.getValue().toString();
                                                            textOnResetEditText.setText(textOnReset);

                                                            String finalTextOnReset = textOnReset;

                                                            IoT_Database.child(thingTag).child(topicTag).child(Topics.USE_FEEDBACK_NODE)
                                                                    .addValueEventListener(new ValueEventListener() {
                                                                        @Override
                                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                            if (!(dataSnapshot.getValue() == null)) {
                                                                                String useFeedbackNode = dataSnapshot.getValue().toString();
                                                                                if (useFeedbackNode.equals("No")) {
                                                                                    useFeedbackNodeAutoCompleteTextView.setText("No", false);
                                                                                    retrieveValue(topicTag, finalTextOnSet, finalTextOnReset);

                                                                                } else if (useFeedbackNode.equals("Yes")) {
                                                                                    useFeedbackNodeAutoCompleteTextView.setText("Yes", false);
                                                                                    retrieveFeedbackValue(topicTag, finalTextOnSet, finalTextOnReset);
                                                                                }
                                                                            } else {
                                                                                useFeedbackNodeAutoCompleteTextView.setText("No", false);
                                                                                retrieveValue(topicTag, finalTextOnSet, finalTextOnReset);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                                                        }
                                                                    });
                                                        } else {
                                                            //textOnResetEditText.setText(Topics.DEFAULT_TEXT_ON_RESET);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                                    }
                                                });
                                    } else {
                                        //textOnSetEditText.setText(Topics.DEFAULT_TEXT_ON_SET);
                                    }


                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });

                    IoT_Database.child(thingTag).child(topicTag).child(Topics.NOTIFY_IF_SET)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!(dataSnapshot.getValue() == null)) {
                                        String notifyIfSet = dataSnapshot.getValue().toString();
                                        if (notifyIfSet.equals("No")) {
                                            notifyIfSetAutoCompleteTextView.setText("No", false);
                                        } else if (notifyIfSet.equals("Yes")) {
                                            notifyIfSetAutoCompleteTextView.setText("Yes", false);
                                        }
                                    } //else notifyIfSetAutoCompleteTextView.setText("No", false);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.NOTIFY_IF_RESET)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!(dataSnapshot.getValue() == null)) {
                                        String notifyIfReset = dataSnapshot.getValue().toString();
                                        if (notifyIfReset.equals("No")) {
                                            notifyIfResetAutoCompleteTextView.setText("No", false);
                                        } else if (notifyIfReset.equals("Yes")) {
                                            notifyIfResetAutoCompleteTextView.setText("Yes", false);
                                        }
                                    } //else notifyIfResetAutoCompleteTextView.setText("No", false);
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
                }
            }
        }
    }

    public void saveConfigurationsToFirebase(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());

            error = false;

            if (!(IoT_Database == null)) {

                String topicTag = topicEditText.getText().toString().trim();
                String topicName = topicNameEditText.getText().toString().trim();
                String value = initialValueAutoCompleteTextView.getText().toString().trim();
                String textOnSet = textOnSetEditText.getText().toString().trim();
                String textOnReset = textOnResetEditText.getText().toString().trim();
                String notifyIfSet = notifyIfSetAutoCompleteTextView.getText().toString().trim();
                String notifyIfReset = notifyIfResetAutoCompleteTextView.getText().toString().trim();
                String useFeedbackNode = useFeedbackNodeAutoCompleteTextView.getText().toString().trim();

                if (!(topicName.isEmpty())) {
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.DESCRIPTION).setValue(topicName);
                } else
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.DESCRIPTION).setValue(Topics.DEFAULT_DESCRIPTION);

                if (ReadWriteRadioButton.isChecked()) {
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.ACCESS).setValue(Topics.READWRITE);
                } else if (ReadRadioButton.isChecked()) {
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.ACCESS).setValue(Topics.READ);
                }

                IoT_Database.child(thingTag).child(topicTag).child(Topics.NOTIFY_IF_SET).setValue(notifyIfSet);

                IoT_Database.child(thingTag).child(topicTag).child(Topics.NOTIFY_IF_RESET).setValue(notifyIfReset);

                IoT_Database.child(thingTag).child(topicTag).child(Topics.USE_FEEDBACK_NODE).setValue(useFeedbackNode);

                IoT_Database.child(thingTag).child(topicTag).child(Topics.TYPE).setValue(Topics.DIGITAL);

                manageValue(topicTag);

                manageTextsOnSetAndReset(topicTag);

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
    }

    public void saveConfigurations (View view){

        String topicTag = topicEditText.getText().toString().trim();

        if(topicTag.equals(initialTopicTag)){
            saveConfigurationsToFirebase();
        }
        else{
            AlertDialog Dialog = new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Warning!")
                    .setMessage("You are about to change the topic tag, continue?")
                    .setPositiveButton(Html.fromHtml("<font color='black'>Yes</font>"), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if(user!=null){
                            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
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
                                                    isTopicChanged = true;
                                                    getLastFeedbackValueAndSetItToTheNewTag(initialTopicTag, topicTag);
                                                    saveConfigurationsToFirebase();

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
                        }}
                    })
                    .setNegativeButton(Html.fromHtml("<font color='black'>Cancel</font>"), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        }
    }

    public void manageValue(String topicTag){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            if (!(IoT_Database == null)) {

                String textOnSet = textOnSetEditText.getText().toString().trim();
                String textOnReset = textOnResetEditText.getText().toString().trim();
                String value = initialValueAutoCompleteTextView.getText().toString().trim();

                ArrayList<String> textsOn_X_ArrayList = new ArrayList<String>() {{
                    add(textOnSet);
                    add(textOnReset);
                }};
                ArrayAdapter<String> initialValueArrayAdapter = new ArrayAdapter<String>(DigitalTopicConfigurationActivity.this,
                        R.layout.support_simple_spinner_dropdown_item, textsOn_X_ArrayList);
                initialValueAutoCompleteTextView.setAdapter(initialValueArrayAdapter);

                if (!(value.equals(textOnSet)) && !(value.equals(textOnReset))) {
                    if (previousValue.equals("true")) {
                        initialValueAutoCompleteTextView.setText(textOnSet, false);
                        //value = textOnSet;
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(true);
                        //IoT_Database.child(thingTag).child(topicTag).child(Topics.BOOLEAN_VALUE).setValue(true);
                        //previousValue = "true";
                    } else {
                        initialValueAutoCompleteTextView.setText(textOnReset, false);
                        //value = textOnReset;
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(false);
                        //IoT_Database.child(thingTag).child(topicTag).child(Topics.BOOLEAN_VALUE).setValue(false);
                        //previousValue = "false";
                    }

                } else {
                    if (value.equals(textOnSet)) {
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(true);
                        //IoT_Database.child(thingTag).child(topicTag).child(Topics.BOOLEAN_VALUE).setValue(true);
                        previousValue = "true";
                    } else {
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(false);
                        //IoT_Database.child(thingTag).child(topicTag).child(Topics.BOOLEAN_VALUE).setValue(false);
                        previousValue = "false";
                    }
                }
                previousValueOnSet = textOnSet;
                previousValueOnReset = textOnReset;
            }
        }
    }

    public void manageResults(){
        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    public void run() {
                        if(!error) {
                            initialValueAutoCompleteTextView.setError(null);
                            Toast.makeText(DigitalTopicConfigurationActivity.this,"Configuration saved",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                500);
    }

    public void manageTextsOnSetAndReset(String topicTag){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());

            String textOnSet = textOnSetEditText.getText().toString().trim();
            String textOnReset = textOnResetEditText.getText().toString().trim();

            if (!(textOnSet.isEmpty())) {
                if (!(textOnReset.isEmpty())) {
                    if (!(textOnSet.equals(textOnReset))) {
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.TEXT_ON_SET).setValue(textOnSet);
                        IoT_Database.child(thingTag).child(topicTag).child(Topics.TEXT_ON_RESET).setValue(textOnReset);
                    } else {
                        textOnResetEditText.setError(ERROR_TOR_NE_TOS);
                        textOnResetEditText.requestFocus();
                        error = true;
                    }
                } else {
                    textOnResetEditText.setError(ERROR_TOR_NE_EMPTY);
                    textOnResetEditText.requestFocus();
                    error = true;
                }
            } else {
                textOnSetEditText.setError(ERROR_TOS_NE_EMPTY);
                textOnSetEditText.requestFocus();
                error = true;
            }
        }
    }

//    public void buildMenu(View v){
//
//        textOnSet = textOnSetEditText.getText().toString().trim();
//        textOnReset = textOnResetEditText.getText().toString().trim();
//
//        ArrayList<String> textsOn_X_ArrayList = new ArrayList<String>() {{add(textOnSet); add(textOnReset);}};
//        initialValueArrayAdapter = new ArrayAdapter<String>(DigitalTopicConfigurationActivity.this,
//                R.layout.support_simple_spinner_dropdown_item, textsOn_X_ArrayList);
//        initialValueAutoCompleteTextView.setAdapter(initialValueArrayAdapter);
//    }

    public void updateValueDropDownOptions(){

        String textOnSet = textOnSetEditText.getText().toString().trim();
        String textOnReset = textOnResetEditText.getText().toString().trim();

        ArrayList<String> textsOn_X_ArrayList = new ArrayList<String>() {{add(textOnSet); add(textOnReset);}};
        ArrayAdapter<String> initialValueArrayAdapter = new ArrayAdapter<String>(DigitalTopicConfigurationActivity.this,
                R.layout.support_simple_spinner_dropdown_item, textsOn_X_ArrayList);
        initialValueAutoCompleteTextView.setAdapter(initialValueArrayAdapter);
    }

    public void showFullTag (View view){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            String userID = mAuth.getCurrentUser().getUid();
            if(userID!=null){
                String Tag = userID + "/Things" + "/" + thingTag + "/" + initialTopicTag;
                AlertDialog newDialog = new AlertDialog.Builder(DigitalTopicConfigurationActivity.this)

                        .setTitle("Full tag:")
                        .setMessage(Tag)
                        .setPositiveButton(Html.fromHtml("<font color='black'><small>Copy to clipboard</small></font>"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!Tag.isEmpty()){
                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText(TAG, Tag);
                                    clipboard.setPrimaryClip(clip);

                                    Toast.makeText(DigitalTopicConfigurationActivity.this, "Tag copied to clipboard", Toast.LENGTH_LONG).show();
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
        } else Toast.makeText(DigitalTopicConfigurationActivity.this, "Make sure you are connected and logged in", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_topic_configuration);

        Toolbar toolbar = (Toolbar) findViewById(R.id.DigitalTopicConfigToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent intent = getIntent();

        thingTag = intent.getStringExtra(Topics.THING_TAG);
        thingName = intent.getStringExtra(Topics.THING_DESCRIPTION_NAME);
        initialTopicTag = intent.getStringExtra(Topics.TOPIC_TAG);

        topicTag = initialTopicTag;
        isTopicChanged = false;

        topicEditText = findViewById(R.id.topicEditText);
        topicNameEditText = findViewById(R.id.topicDescriptionEditText);
        initialValueAutoCompleteTextView = findViewById(R.id.initialValueAutoCompleteTextView);
        textOnSetEditText = findViewById(R.id.textOnSetEditText);
        textOnResetEditText = findViewById(R.id.textOnResetEditText);
        feedbackValueEditText = findViewById(R.id.feedbackValueEditText);

        thingDescriptionNameTextView = findViewById(R.id.thingNameInTopicConfiguration);
        thingDescriptionNameTextView.setText(thingName);

        thingTagTextView = findViewById(R.id.thingTagInTopicConfiguration);
        thingTagTextView.setText(thingTag);

        ReadWriteRadioButton = findViewById(R.id.RW_RadioButton);
        ReadRadioButton = findViewById(R.id.R_RadioButton);

        retrieveSettingsValuesFromFirebaseDatabase(topicTag);

        notifyIfSetAutoCompleteTextView = findViewById(R.id.notifyIfSetAutoCompleteTextView);
        notifyIfResetAutoCompleteTextView = findViewById(R.id.notifyIfResetAutoCompleteTextView);
        useFeedbackNodeAutoCompleteTextView = findViewById(R.id.useFeedbackNode);

        ArrayAdapter<String> notifyIf_X_ArrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item,
                notifyIf_X_ArrayList);
        notifyIfSetAutoCompleteTextView.setAdapter(notifyIf_X_ArrayAdapter);
        notifyIfResetAutoCompleteTextView.setAdapter(notifyIf_X_ArrayAdapter);
        useFeedbackNodeAutoCompleteTextView.setAdapter(notifyIf_X_ArrayAdapter);

        getPreviousValueAndTextsOnSetAndReset(topicTag);

        textOnSetEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                updateValueDropDownOptions();
            }
        });

        textOnResetEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                updateValueDropDownOptions();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
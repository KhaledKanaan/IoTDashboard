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
import android.text.Html;
import android.text.TextUtils;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiStateTopicConfigurationActivity extends AppCompatActivity {

    private ArrayList<String> notifyIf_X_ArrayList = new ArrayList<String>() {{add("Yes"); add("No");}};

    public static String topicTag, initialTopicTag, topicName, value, access, thingTag, thingName, TAG = "MultiState Topic Configuration Activity";

    public EditText topicEditText, topicNameEditText, initialValueEditText, notifyIfEqualsEditText;

    public TextView thingDescriptionNameTextView, thingTagTextView;

    public RadioButton ReadWriteRadioButton, ReadRadioButton;

    public boolean error;

//    public static DatabaseReference IoT_Database;

    public static List<String> topicTagsList = new ArrayList<String>();

    public void retrieveSettingsValuesFromFirebaseDatabase(){

        if(!topicTag.isEmpty() && !(topicTag==null)) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());

                if(!(IoT_Database==null)) {

                    topicEditText.setText(topicTag);

                    IoT_Database.child(thingTag).child(topicTag).child(Topics.ACCESS)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)) {
                                        access = dataSnapshot.getValue().toString();

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
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)) {
                                        topicName = dataSnapshot.getValue().toString();
                                        //topicNameEditText = findViewById(R.id.topicDescriptionEditText);
                                        topicNameEditText.setText(topicName);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });

                    IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!(dataSnapshot.getValue()==null)) {
                                        value = dataSnapshot.getValue().toString();
                                        initialValueEditText.setText(value);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });

                    IoT_Database.child(thingTag).child(topicTag).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(!(dataSnapshot.getValue()==null)) {
                                ArrayList<String> values = new ArrayList<>();
                                for (DataSnapshot childSnapshot: dataSnapshot.child(Topics.NOTIFY_IF_EQUALS).getChildren()) {
                                    values.add(childSnapshot.getValue(String.class));
                                    String joinedValues = TextUtils.join(", ", values);
                                    notifyIfEqualsEditText.setText(joinedValues);
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

    public void saveConfigurationsToFirebase(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());

            error = false;

            if(!(IoT_Database==null)){

                if(!(topicName.isEmpty())) {
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.DESCRIPTION).setValue(topicName);
                }else IoT_Database.child(thingTag).child(topicTag).child(Topics.DESCRIPTION).setValue(Topics.DEFAULT_DESCRIPTION);

                if (ReadWriteRadioButton.isChecked()){
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.ACCESS).setValue(Topics.READWRITE);
                }

                else if (ReadRadioButton.isChecked()){
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.ACCESS).setValue(Topics.READ);
                }

                if(!(value == null)) {
                    IoT_Database.child(thingTag).child(topicTag).child(Topics.VALUE).setValue(value);
                }

                IoT_Database.child(thingTag).child(topicTag).child(Topics.TYPE).setValue(Topics.MULTISTATE);

                manageNotifyIfEqualsValues();

                Toast.makeText(this,"Configurations saved", Toast.LENGTH_SHORT).show();

            }
        }
    }

    public void saveConfigurations (View view){

        topicTag = topicEditText.getText().toString().trim();
        topicName = topicNameEditText.getText().toString().trim();
        value = initialValueEditText.getText().toString().trim();

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
                            if(user != null){
                                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                                if(!(IoT_Database==null)){
                                    IoT_Database.child(thingTag).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            int count = 0;
                                            topicTagsList.clear();

                                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) { // fetch all the topics tags from firebase DB and store them in a list
                                                topicTagsList.add(snapshot.getKey());
                                                count ++;
                                                if (count == dataSnapshot.getChildrenCount()){// make sure all topics tags are retrieved from the firebase DB before start listing them
                                                    if(topicTagsList.contains(topicTag)){ //tag is already used
                                                        Toast.makeText(getApplicationContext(),"\""+topicTag+"\""+" is already used", Toast.LENGTH_LONG).show();
                                                    }
                                                    else{
                                                        saveConfigurationsToFirebase();
                                                        IoT_Database.child(thingTag).child(initialTopicTag).setValue(null);
                                                        initialTopicTag = topicTag;
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
                    })
                    .setNegativeButton(Html.fromHtml("<font color='black'>Cancel</font>"), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        }
    }

    public void manageNotifyIfEqualsValues(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            String notifyIfEqualsValues = notifyIfEqualsEditText.getText().toString().trim();

            if (!notifyIfEqualsValues.equals("") && notifyIfEqualsValues != null){
                String[] notifyIfEqualsSeparatedValues = notifyIfEqualsValues.split(",");
                int index = 0;
                Map<String, String> map = new HashMap<>();

                for(String value : notifyIfEqualsSeparatedValues){
                    //notifyIfEqualsSeparatedValues[index] = value.trim();
                    index++;
                    map.put(String.valueOf(index), value.trim());
                    if(index == notifyIfEqualsSeparatedValues.length){
                        try{
                            IoT_Database.child(thingTag).child(topicTag).child(Topics.NOTIFY_IF_EQUALS).setValue(map);
                        }
                        catch (Exception e){
                            Log.e(TAG, e.toString());
                            error = true;
                        }
                    }
                }
            }else  IoT_Database.child(thingTag).child(topicTag).child(Topics.NOTIFY_IF_EQUALS).setValue(null);
        }
    }

    public void showFullTag (View view){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            String userID = mAuth.getCurrentUser().getUid();
            if(userID!=null){
                String Tag = userID + "/Things" + "/" + thingTag + "/" + initialTopicTag;
                AlertDialog newDialog = new AlertDialog.Builder(MultiStateTopicConfigurationActivity.this)

                        .setTitle("Full tag:")
                        .setMessage(Tag)
                        .setPositiveButton(Html.fromHtml("<font color='black'><small>Copy to clipboard</small></font>"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!Tag.isEmpty()){
                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText(TAG, Tag);
                                    clipboard.setPrimaryClip(clip);

                                    Toast.makeText(MultiStateTopicConfigurationActivity.this, "Tag copied to clipboard", Toast.LENGTH_LONG).show();
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
        } else Toast.makeText(MultiStateTopicConfigurationActivity.this, "Make sure you are connected and logged in", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_state_topic_configuration);

        Toolbar toolbar = (Toolbar) findViewById(R.id.MultiStateTopicConfigToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent intent = getIntent();

        thingTag = intent.getStringExtra(Topics.THING_TAG);
        thingName = intent.getStringExtra(Topics.THING_DESCRIPTION_NAME);
        initialTopicTag = intent.getStringExtra(Topics.TOPIC_TAG);

        topicTag = initialTopicTag;

        topicEditText = findViewById(R.id.topicEditText);
        topicNameEditText = findViewById(R.id.topicDescriptionEditText);
        initialValueEditText = findViewById(R.id.initialValueEditText);
        notifyIfEqualsEditText = findViewById(R.id.notifyIfEqualsEditText);

        thingDescriptionNameTextView = findViewById(R.id.thingNameInTopicConfiguration);
        thingDescriptionNameTextView.setText(thingName);

        thingTagTextView = findViewById(R.id.thingTagInTopicConfiguration);
        thingTagTextView.setText(thingTag);

        ReadWriteRadioButton = findViewById(R.id.RW_RadioButton);
        ReadRadioButton = findViewById(R.id.R_RadioButton);

        retrieveSettingsValuesFromFirebaseDatabase();

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
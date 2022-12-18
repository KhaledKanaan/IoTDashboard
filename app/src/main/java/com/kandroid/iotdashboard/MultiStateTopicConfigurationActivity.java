package com.kandroid.iotdashboard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
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

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kandroid.iotdashboard.Topics.ACCESS;
import static com.kandroid.iotdashboard.Topics.DEFAULT_DESCRIPTION;
import static com.kandroid.iotdashboard.Topics.DESCRIPTION;
import static com.kandroid.iotdashboard.Topics.DIGITAL;
import static com.kandroid.iotdashboard.Topics.MULTISTATE;
import static com.kandroid.iotdashboard.Topics.READ;
import static com.kandroid.iotdashboard.Topics.READWRITE;
import static com.kandroid.iotdashboard.Topics.TYPE;
import static com.kandroid.iotdashboard.Topics.VALUE;

public class MultiStateTopicConfigurationActivity extends AppCompatActivity {

    private ArrayList<String> notifyIf_X_ArrayList = new ArrayList<String>() {{add("Yes"); add("No");}};

    public static String topicTag, initialTopicTag, topicName, value, access, thingTag, thingName, TAG = "MultiState Topic Configuration Activity";

    public EditText topicEditText, topicNameEditText, initialValueEditText, notifyIfEqualsEditText;

    public TextView thingDescriptionNameTextView, thingTagTextView;

    public RadioButton ReadWriteRadioButton, ReadRadioButton;

    public TextInputLayout topicTextInputLayout;

    //public List<ValueEventListener> listeners = new ArrayList<ValueEventListener>();

    public boolean error, isTopicChanged;

    private FirebaseUser user;

    private DatabaseReference IoT_Database;

    private ValueEventListener listener, maintainedConnectionListener, connectedRefListener;

    public static List<String> topicTagsList = new ArrayList<String>();

    public void retrieveSettingsValuesFromFirebaseDatabase(){

        removeListeners();

        if(!topicTag.isEmpty() && !(topicTag==null)) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());

                if(!(IoT_Database==null)) {

                    topicEditText.setText(topicTag);

                    listener = IoT_Database.child(thingTag).child(topicTag).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.getValue()!=null){

                                Map<String, Object> topicData = new HashMap<>();
                                topicData = (Map<String, Object>) snapshot.getValue();

                                String access =  String.valueOf(topicData.get(Topics.ACCESS));
                                String description =  String.valueOf(topicData.get(Topics.DESCRIPTION));
                                String value =  String.valueOf(topicData.get(Topics.VALUE));
                                String notifyIfEquals =  String.valueOf(topicData.get(Topics.NOTIFY_IF_EQUALS));


                                ///
                                if(topicData.get(Topics.ACCESS)!=null){
                                    if (access.equals(Topics.READWRITE)) {
                                        ReadWriteRadioButton.setChecked(true);
                                    }

                                    else if (access.equals(Topics.READ)) {
                                        ReadRadioButton.setChecked(true);
                                    }
                                }

                                ///
                                if(topicData.get(Topics.DESCRIPTION)!=null){
                                    topicNameEditText.setText(description);
                                }else topicNameEditText.setText("");

                                ///
                                if(topicData.get(Topics.VALUE)!=null){
                                    initialValueEditText.setText(value);
                                }else initialValueEditText.setText("");


                                ///
                                if(topicData.get(Topics.NOTIFY_IF_EQUALS)!=null){
                                    ArrayList<String> values = new ArrayList<>();
                                    for (DataSnapshot childSnapshot: snapshot.child(Topics.NOTIFY_IF_EQUALS).getChildren()) {
                                        values.add(childSnapshot.getValue(String.class));
                                        String joinedValues = TextUtils.join(", ", values);
                                        notifyIfEqualsEditText.setText(joinedValues);
                                    }
                                }else notifyIfEqualsEditText.setText("");


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

    public void saveConfigurationsToFirebase(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            DatabaseReference IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());

            error = false;

            if(!(IoT_Database==null)){

                Map<String, Object> topicData = new HashMap<>();

                topicData.put(TYPE, MULTISTATE);

                if(!topicName.isEmpty()) {
                    topicData.put(DESCRIPTION, topicName);
                }else topicData.put(DESCRIPTION, DEFAULT_DESCRIPTION);

                if (ReadWriteRadioButton.isChecked()){
                    topicData.put(ACCESS, READWRITE);
                } else if (ReadRadioButton.isChecked()){
                    topicData.put(ACCESS, READ);
                }

                if(value != null) {
                    topicData.put(VALUE, value);
                }

                IoT_Database.child(thingTag).child(topicTag).updateChildren(topicData);

                manageNotifyIfEqualsValues();

                if (isTopicChanged) {


                    new android.os.Handler(Looper.getMainLooper()).postDelayed(
                            new Runnable() {
                                public void run() {
                                    retrieveSettingsValuesFromFirebaseDatabase();
                                    isTopicChanged = false;
                                }
                            },
                            500);
                }

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
                    .setPositiveButton(HtmlCompat.fromHtml("<font color='black'>Yes</font>", HtmlCompat.FROM_HTML_MODE_LEGACY), new DialogInterface.OnClickListener() {
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
                                                        removeListeners();
                                                        isTopicChanged = true;
                                                        saveConfigurationsToFirebase();

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

                        .setTitle("Topic path:")
                        .setMessage(Tag)
                        .setPositiveButton(HtmlCompat.fromHtml("<font color='black'><small>Copy to clipboard</small></font>", HtmlCompat.FROM_HTML_MODE_LEGACY), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!Tag.isEmpty()){
                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText(TAG, Tag);
                                    clipboard.setPrimaryClip(clip);

                                    Toast.makeText(MultiStateTopicConfigurationActivity.this, "Topic path copied to clipboard", Toast.LENGTH_LONG).show();
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

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            IoT_Database = MainActivity.getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
        }

        Intent intent = getIntent();

        thingTag = intent.getStringExtra(Topics.THING_TAG);
        thingName = intent.getStringExtra(Topics.THING_DESCRIPTION_NAME);
        initialTopicTag = intent.getStringExtra(Topics.TOPIC_TAG);

        //Toast.makeText(getApplicationContext(), initialTopicTag, Toast.LENGTH_LONG).show();

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

        topicTextInputLayout = findViewById(R.id.topicTextInputLayout);

        topicTextInputLayout.setPrefixText(thingTag+"/");

        retrieveSettingsValuesFromFirebaseDatabase();

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
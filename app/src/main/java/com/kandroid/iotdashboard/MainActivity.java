package com.kandroid.iotdashboard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kandroid.iotdashboard.Topics.VALUE;

public class MainActivity extends AppCompatActivity {

    public static String THING_NAME_TOPIC_TAG = "ThingName", THING_NAME_TOPIC_DESCRIPTION = "ThingName_", THINGS_DB_ROOT = "Things" , USER_DB_ROOT;
    public GridLayout gridLayout;
    public static SharedPreferences sharedPreferences;
    public Gson gson = new Gson();
    public static String TAG ="MainActivity";
    private DrawerLayout drawer;
    public static boolean connectedToTheInternet = true;
    public ConnectivityManager connectivityManager;
    public ConnectivityManager.NetworkCallback networkCallback;
    public FirebaseAuth.AuthStateListener mAuthStateListener;


    public void addThingWithFAB_Button(View view){
        addThing();
    }

    public int getViewParentThingCardPosition(View view){
        int parentViewPosition = -1;
        int thingCount = gridLayout.getChildCount();
        for(int i=0; i<thingCount; i++ ){

            if(getViewTag(gridLayout.getChildAt(i).findViewById(R.id.thingCard)).equals(getViewTag(view))){
                parentViewPosition = i;
            }
        }
        return parentViewPosition;
    }

    public int getViewParentThingCardPositionWithTheSameTag(String tag){

        int parentViewPosition = -1;
        int thingCount = gridLayout.getChildCount();
        for(int i=0; i<thingCount; i++ ){

            if(getViewTag(gridLayout.getChildAt(i).findViewById(R.id.thingCard)).equals(tag)){
                parentViewPosition = i;
            }
        }
        return parentViewPosition;
    }

    public void addOrUpdateThingToFireBaseDatabase (View view){
        // get the thing tag from the calling view:
        String thingTag = getViewTag(view);
        // get the thing name/description from the calling view:
        TextView thingNameTextView = (TextView) view.findViewById(R.id.thingName);
        String thingName = thingNameTextView.getText().toString();

        if(!thingTag.isEmpty() && !(thingTag==null)) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                DatabaseReference IoT_Database = getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                if(!(IoT_Database==null)){

                    IoT_Database.child(thingTag).child(THING_NAME_TOPIC_TAG).child(Topics.VALUE).setValue(thingName);
                    IoT_Database.child(thingTag).child(THING_NAME_TOPIC_TAG).child(Topics.ACCESS).setValue(Topics.READWRITE);
                    IoT_Database.child(thingTag).child(THING_NAME_TOPIC_TAG).child(Topics.TYPE).setValue(Topics.MULTISTATE);
                    IoT_Database.child(thingTag).child(THING_NAME_TOPIC_TAG).child(Topics.DESCRIPTION).setValue(THING_NAME_TOPIC_DESCRIPTION);

                }
            }
        }
    }

    public void addThing() {
        boolean intentToConnect = sharedPreferences.getBoolean(LauncherActivity.INTENT_TO_CONNECT, false);
        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty() && intentToConnect){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                    View promptsView = inflater.inflate(R.layout.first_configuration_dialog, null);
                    TextView thingNameDialog = (TextView) promptsView.findViewById(R.id.thingName);
                    TextView thingTagDialog = (TextView) promptsView.findViewById(R.id.thingTag);

                    AlertDialog Dialog = new AlertDialog.Builder(this)
                            .setIcon(R.drawable.ic_baseline_add_30)
                            .setTitle(Html.fromHtml("<font color='#333333'>Thing</font>"))
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
                            String tag = thingTagDialog.getText().toString().trim();
                            String name = thingNameDialog.getText().toString().trim();

                            if(!tag.isEmpty()){
                                if (!tagIsUsed(tag)){

                                    inflateThingCard(tag, name);
                                    Dialog.dismiss();

                                    new android.os.Handler(Looper.getMainLooper()).postDelayed(
                                            new Runnable() {
                                                public void run() {
                                                    //scroll down to the new thing:
                                                    ScrollView sv = (ScrollView)findViewById(R.id.thingsScrollView);
                                                    sv.scrollTo(0, sv.getBottom());
                                                }
                                            },
                                            500);


                                    int parentViewPosition = getViewParentThingCardPositionWithTheSameTag(tag);
                                    View parentView = gridLayout.getChildAt(parentViewPosition);
                                    addOrUpdateThingToFireBaseDatabase(parentView);

                                }
                                else {
                                    Toast.makeText(MainActivity.this, "Tag already used", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else {
                                Toast.makeText(MainActivity.this, "Tag can't be empty", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Please login first", Toast.LENGTH_LONG).show();
                }
        }else Toast.makeText(MainActivity.this, "Please connect to Firebase first", Toast.LENGTH_SHORT).show();
    }

    public void setThingCardViewTagsAndTexts(View view, String tag, String name){

        view.setTag(tag);
        view.findViewById(R.id.thingCard).setTag(tag);
        view.findViewById(R.id.deleteImageButton).setTag(tag);
        view.findViewById(R.id.editImageButton).setTag(tag);
        view.findViewById(R.id.thingName).setTag(tag);
        view.findViewById(R.id.thingTag).setTag(tag);
        view.findViewById(R.id.mcuImageView).setTag(tag);

        //Set the thing name text in the CardView same as the one chosen in the first configuration dialog:
        TextView thingNameTextView = (TextView) view.findViewById(R.id.thingName);
        thingNameTextView.setText(name);

        //Set the thing tag text in the CardView same as the one chosen in the first configuration dialog:
        TextView thingTagTextView = (TextView) view.findViewById(R.id.thingTag);
        thingTagTextView.setText(tag);
    }

    public void inflateThingCard(String thingTag, String thingName) {

        if(!tagIsUsed(thingTag)){

            View thingView = getLayoutInflater().inflate(R.layout.thing_card,null, false);
            setThingCardViewTagsAndTexts(thingView, thingTag, thingName);
            gridLayout.addView(thingView);

        }else Toast.makeText(MainActivity.this, "Tag " + thingTag + " is already used", Toast.LENGTH_SHORT).show();

    }

    public String getViewTag(View view){
        return String.valueOf(view.getTag()).trim();
    }

    public boolean tagIsUsed(String tag){
        boolean isUsed = false;
        int thingCount = gridLayout.getChildCount();
        for(int i=0; i<thingCount; i++ ){

            if(getViewTag(gridLayout.getChildAt(i).findViewById(R.id.thingCard)).equals(tag)){
                isUsed = true;
            }
        }
        return isUsed;
    }

    public void thingConfiguration(View view){

            LayoutInflater inflater = MainActivity.this.getLayoutInflater();
            View promptsView = inflater.inflate(R.layout.first_configuration_dialog, null);

            TextView thingNameDialog = (TextView) promptsView.findViewById(R.id.thingName);
            TextView thingTagDialog = (TextView) promptsView.findViewById(R.id.thingTag);

            //get previous assigned tag and name to show them in the dialog box:
            String currentTag = getViewTag(view);
            int parentIndex = getViewParentThingCardPositionWithTheSameTag(currentTag);
            View parentView = gridLayout.getChildAt(parentIndex);

            TextView nameTextView = (TextView) parentView.findViewById(R.id.thingName);

            thingNameDialog.setText(nameTextView.getText().toString());
            thingTagDialog.setText(currentTag);

            AlertDialog Dialog = new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_baseline_edit_30)
                    .setTitle(Html.fromHtml("<font color='#333333'>Edit thing</font>"))
                    .setView(promptsView)
                    .setPositiveButton(Html.fromHtml("<font color='black'>Ok</font>"), null)
                    .setNegativeButton(Html.fromHtml("<font color='black'>Cancel</font>"), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
            Button positiveButton = Dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String newName = thingNameDialog.getText().toString().trim();
                    String newTag = thingTagDialog.getText().toString().trim();

                    if(!newTag.isEmpty()){
                        if(!newTag.equals(currentTag)) {
                            ArrayList<String> keys = new ArrayList<>();

                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if(user != null){
                                DatabaseReference IoT_Database = getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                                if (!(IoT_Database == null)) {
                                    IoT_Database.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            int count = 0;
                                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                count++;
                                                String key = (String) snapshot.getKey();
                                                keys.add(key);
                                                if (count == dataSnapshot.getChildrenCount()) {
                                                    if (!keys.contains(newTag)) {
                                                        AlertDialog newDialog = new AlertDialog.Builder(MainActivity.this)
                                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                                .setTitle("Warning!")
                                                                .setMessage("You are about to change the thing tag, continue?")
                                                                .setPositiveButton(Html.fromHtml("<font color='black'>Yes</font>"), new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {

//                                                                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//                                                                        DatabaseReference IoT_Database = getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                                                                        if (!(IoT_Database == null)) {
                                                                            if(newName!=null){
                                                                                IoT_Database.child(currentTag).child(THING_NAME_TOPIC_TAG).child(Topics.VALUE).setValue(newName);
                                                                            }
                                                                            IoT_Database.child(currentTag).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                @Override
                                                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                                    Map<String, Object> thingData = new HashMap<>();
                                                                                    thingData = (Map<String, Object>) dataSnapshot.getValue();
                                                                                    Object data = thingData.get(currentTag);
                                                                                    thingData.remove(currentTag);
                                                                                    thingData.put(newTag, data);
                                                                                    IoT_Database.child(newTag).updateChildren(thingData);
                                                                                    IoT_Database.child(currentTag).setValue(null);

                                                                                }

                                                                                @Override
                                                                                public void onCancelled(@NonNull DatabaseError databaseError) {

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
                                                        Dialog.dismiss();
                                                    } else Toast.makeText(MainActivity.this, "\""+newTag+"\""+" is already used", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }
                        } else {
                            Dialog.dismiss();
                            if(newName!=null){
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                if(user != null){
                                    DatabaseReference IoT_Database = getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                                    if(!(IoT_Database==null)){
                                        IoT_Database.child(currentTag).child(THING_NAME_TOPIC_TAG).child(Topics.VALUE).setValue(newName);
                                    }
                                }
                            }
                        }
                    } else Toast.makeText(MainActivity.this, "Tag can't be empty", Toast.LENGTH_SHORT).show();
                }
            });
    }

    public void updateThingCardViewAtIndex(int index, String tag, String name){

        //Set the CardView tag same as the thing tag
        gridLayout.getChildAt(index).findViewById(R.id.thingCard).setTag(tag);
        //Set the deleteImageButton tag same as the thing tag
        gridLayout.getChildAt(index).findViewById(R.id.thingCard).findViewById(R.id.deleteImageButton).setTag(tag);
        //Set the editImageButton tag same as the thing tag
        gridLayout.getChildAt(index).findViewById(R.id.thingCard).findViewById(R.id.editImageButton).setTag(tag);

        //Set the thing name text in the CardView:
        TextView thingName = (TextView) gridLayout.getChildAt(index).findViewById(R.id.thingCard).findViewById(R.id.thingName);
        thingName.setText(name);

        //Set the thing tag text in the CardView:
        TextView thingTag = (TextView) gridLayout.getChildAt(index).findViewById(R.id.thingCard).findViewById(R.id.thingTag);
        thingTag.setText(tag);
    }

    public void editThing(View view){
        int parentViewIndex = getViewParentThingCardPosition(view);
        if(parentViewIndex>-1)
            thingConfiguration(gridLayout.getChildAt(parentViewIndex));
    }

    public void deleteThing(View view){

        AlertDialog Dialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Warning!")
                .setMessage("All data related to this thing will be erased, continue?")
                .setPositiveButton(Html.fromHtml("<font color='black'>Yes</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        int indexToBeDeleted = getViewParentThingCardPosition(view);
                        if(indexToBeDeleted>-1) {
                            String tagToBeDeleted = getViewTag(gridLayout.getChildAt(indexToBeDeleted));
                            gridLayout.removeViewAt(indexToBeDeleted);

                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if(user != null){
                                DatabaseReference IoT_Database = getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
                                if(!(IoT_Database==null)){
                                    IoT_Database.child(tagToBeDeleted).removeValue();
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

    public void brokerSettings(MenuItem item){
        Intent intent = new Intent(getApplicationContext(), ConnectToBrokerActivity.class);
        startActivity(intent);
    }

    public void openConnectToFirebaseActivity(MenuItem item){
        Intent intent = new Intent(getApplicationContext(), ConnectToFirebaseActivity.class);
        startActivity(intent);
    }

    public void goToLoginActivity(MenuItem item){

        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()){
            DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
            if (connectedRef != null) {
                connectedRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isConnected = snapshot.getValue(Boolean.class);
                        if (isConnected){
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                String userEmail = user.getEmail();
                                Toast.makeText(MainActivity.this, userEmail + "\nis already logged in", Toast.LENGTH_LONG).show();
                            }else{
                                Intent intent = new Intent(getApplicationContext(), LoginSemiPublicServerActivity.class);
                                startActivity(intent);
                            }
                        }else Toast.makeText(MainActivity.this, "Please connect to Firebase first", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }else Toast.makeText(MainActivity.this, "Please connect to Firebase first", Toast.LENGTH_LONG).show();
        }else Toast.makeText(MainActivity.this, "Please connect to Firebase first",Toast.LENGTH_LONG).show();
    }

    public void logout(MenuItem item){
        signOut();
    }

    public void signOut(){
        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                if(mAuth != null){
                    String email = mAuth.getCurrentUser().getEmail().toString();

                    if(mAuthStateListener!=null) mAuth.removeAuthStateListener(mAuthStateListener);
                    mAuth.signOut();
                    //mAuth.getCurrentUser().delete();
                    Toast.makeText(MainActivity.this, email + " signed out", Toast.LENGTH_LONG).show();
                    sharedPreferences.edit().putBoolean(LauncherActivity.INTENT_TO_LOGIN, false).apply();
                    gridLayout.removeAllViews();
                }
            } else
                Toast.makeText(MainActivity.this, "No logged in users", Toast.LENGTH_LONG).show();
        }else Toast.makeText(MainActivity.this, "No connection with Firebase",Toast.LENGTH_LONG).show();
    }

    public void retrievePreviousThingsFromFireBaseDataBaseAndListThem(String userDBRoot){
        //get previous things from FireBase Realtime database:
        DatabaseReference IoT_Database = getThingsDatabaseReference(LauncherActivity.SERVER_TYPE, userDBRoot);
        if(!(IoT_Database==null)){
            IoT_Database.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    int count = 0;
                    ArrayList<String> thingsTagsList = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) { // fetch all the things tags from firebase DB and store them in a list
                        thingsTagsList.add(snapshot.getKey());
                        count++;
                        if (count == dataSnapshot.getChildrenCount()) { // make sure all things tags are retrieved from the firebase DB before start listing them
                            Log.i("testC",thingsTagsList.toString());
                            for(int j =0; j<thingsTagsList.size(); j++){

                                if(!thingsTagsList.get(j).isEmpty() && !(thingsTagsList.get(j)==null)) {

                                    int finalJ = j;
                                    IoT_Database.child(thingsTagsList.get(j)).child(THING_NAME_TOPIC_TAG).child(VALUE)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    if (!(dataSnapshot.getValue() == null)){

                                                        String thingName = dataSnapshot.getValue().toString();
                                                        if(!tagIsUsed(thingsTagsList.get(finalJ))){
                                                            inflateThingCard(thingsTagsList.get(finalJ), thingName);
                                                        }
                                                        else {
                                                            int thingCardIndex = getViewParentThingCardPositionWithTheSameTag(thingsTagsList.get(finalJ));
                                                            updateThingCardViewAtIndex(thingCardIndex, thingsTagsList.get(finalJ), thingName);
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                }
                                if (j==thingsTagsList.size()-1){
                                    //remove unused thing card views (deleted by other devices while this device was offline):
                                    for(int k=0; k < gridLayout.getChildCount(); k++){
                                        if (!thingsTagsList.contains(getViewTag(gridLayout.getChildAt(k)))){
                                            gridLayout.removeViewAt(k);
                                        }
                                    }
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

    public void goToTopics(View view) {
        TextView tagTextView = (TextView) view.findViewById(R.id.thingCard).findViewById(R.id.thingTag);
        TextView nameTextView = (TextView) view.findViewById(R.id.thingCard).findViewById(R.id.thingName);
        String tag = tagTextView.getText().toString();
        String name = nameTextView.getText().toString();

        if(!tag.isEmpty() && !(tag==null)){
            Intent intent = new Intent(getApplicationContext(), Topics.class);
            intent.putExtra("Tag", tag);
            intent.putExtra("Name", name);
            startActivity(intent);
        }
    }


    private void retrieveCurrentFcmToken(){
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }
                        // Get new FCM registration token
                        String token = task.getResult();
                        DatabaseReference IoT_Database = FirebaseDatabase.getInstance().getReference(FireBaseCloudMessagingService.USER_NOTIFICATION_TOKENS).child(token);
                        IoT_Database.setValue(true);
                        // Log
                        Log.d(TAG, token);
                    }
                });
    }

    public static DatabaseReference getThingsDatabaseReference(String serverType, String userDBRoot){

        serverType = serverType.toLowerCase();
        DatabaseReference databaseReference = null;

        if(serverType.equals(LauncherActivity.PUBLIC.toLowerCase()) && !(MainActivity.THINGS_DB_ROOT==null) && !MainActivity.THINGS_DB_ROOT.isEmpty()
                && !(userDBRoot==null) && !userDBRoot.isEmpty()){
            databaseReference = FirebaseDatabase.getInstance().getReference(userDBRoot).child(MainActivity.THINGS_DB_ROOT);
        }
        else if(serverType.equals(LauncherActivity.PRIVATE.toLowerCase()) && !(MainActivity.THINGS_DB_ROOT==null) &&
                !MainActivity.THINGS_DB_ROOT.isEmpty()){
            databaseReference = FirebaseDatabase.getInstance().getReference(MainActivity.THINGS_DB_ROOT);
        }
        else if(serverType.equals(LauncherActivity.SEMI_PUBLIC.toLowerCase()) && !(MainActivity.THINGS_DB_ROOT==null) &&
                !MainActivity.THINGS_DB_ROOT.isEmpty() && !(userDBRoot==null) && !userDBRoot.isEmpty()){
            databaseReference = FirebaseDatabase.getInstance().getReference(userDBRoot).child(MainActivity.THINGS_DB_ROOT);
        }
        return databaseReference;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getApplicationContext().getSharedPreferences(LauncherActivity.PACKAGE_NAME, Context.MODE_PRIVATE);

        //set the toolbar as the action bar
        Toolbar toolbar = findViewById(R.id.dashboardToolbar);
        setSupportActionBar(toolbar);

        // initialize the drawer view
        drawer = findViewById(R.id.navDrawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        gridLayout = findViewById(R.id.gridLayout);



        //Toast.makeText(MainActivity.this, String.valueOf(intentToConnect), Toast.LENGTH_LONG).show();

//        Log.i("flexx login2", String.valueOf(intentToLogin));
//        Log.i("flexx connect2", String.valueOf(intentToConnect));

        monitorInternetConnection();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean intentToConnect = sharedPreferences.getBoolean(LauncherActivity.INTENT_TO_CONNECT, false);
        boolean intentToLogin = sharedPreferences.getBoolean(LauncherActivity.INTENT_TO_LOGIN, false);

        if(intentToConnect){
            if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
                maintainTheConnectionToFirebase();
                getConnectionToFireBaseDataBaseStatus();

                mAuthStateListener = new FirebaseAuth.AuthStateListener() {
                    @Override
                    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            retrievePreviousThingsFromFireBaseDataBaseAndListThem(user.getUid());
                            retrieveCurrentFcmToken();
                        }
                    }
                };
                FirebaseAuth.getInstance().addAuthStateListener(mAuthStateListener);

            }
        }else gridLayout.removeAllViews();

        if(intentToLogin){
            loginCurrentActiveUser();
        }
        else{
            gridLayout.removeAllViews();
        }
    }

    public void disconnectFromFireBaseDataBase(){

        sharedPreferences.edit().putBoolean(LauncherActivity.INTENT_TO_CONNECT, false).apply();
        if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
            try {
                FirebaseDatabase.getInstance().goOffline();
                new android.os.Handler(Looper.getMainLooper()).postDelayed(
                        new Runnable() {
                            public void run() {
                                FirebaseApp.getInstance().delete();
                            }
                        },
                        1000);

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }catch (Exception e){
                Log.e(TAG,e.toString());
            }

            super.onBackPressed();
            finishAffinity();
            finish();
        }
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

    public void maintainTheConnectionToFirebase(){

        DatabaseReference IoT_Database = FirebaseDatabase.getInstance().getReference().child(LauncherActivity.MAINTAINED_CONNECTION_POINT);

        if(IoT_Database!=null){
            IoT_Database.setValue(true);
            IoT_Database.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
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
                                String emailAddress = sharedPreferences.getString(LauncherActivity.USER_EMAIL, LauncherActivity.DEFAULT_USER_EMAIL);
                                String password = sharedPreferences.getString(LauncherActivity.USER_PASSWORD, LauncherActivity.DEFAULT_USER_PASSWORD);

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

    public void connectToFireBaseDataBase(){

        //if(FirebaseApp.getApps(getApplicationContext()).isEmpty()){

            String registeredDatabaseUrl = sharedPreferences.getString(LauncherActivity.DATABASE_URL, LauncherActivity.DEFAULT_DATABASE_URL);
            String registeredApiKey = sharedPreferences.getString(LauncherActivity.API_KEY, LauncherActivity.DEFAULT_API_KEY);
            String registeredApplicationId = sharedPreferences.getString(LauncherActivity.APPLICATION_ID, LauncherActivity.DEFAULT_APPLICATION_ID);
            
            if(!registeredDatabaseUrl.isEmpty()  && !registeredApiKey.isEmpty() && !registeredApplicationId.isEmpty()){

                try {
                    if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()){
                        FirebaseApp.getInstance().delete();
                    }
                }catch(Exception e){
                    Log.e(TAG, e.toString());
                }

                try {
                    FirebaseOptions options = new FirebaseOptions.Builder()
                            .setDatabaseUrl(registeredDatabaseUrl)
                            .setApiKey(registeredApiKey)
                            .setApplicationId(registeredApplicationId)
                            //.setProjectId(ProjectId)
                            .build();

                    FirebaseApp.initializeApp(this, options);

                    new android.os.Handler(Looper.getMainLooper()).postDelayed(
                            new Runnable() {
                                public void run() {
                                    maintainTheConnectionToFirebase();
                                    getConnectionToFireBaseDataBaseStatus();

                                    if(!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
                                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                        if (user != null) {
                                            retrievePreviousThingsFromFireBaseDataBaseAndListThem(user.getUid());
                                            retrieveCurrentFcmToken();
                                        }
                                    }
                                }
                            },
                            800);

                }catch (Exception e){
                    Log.e(TAG, e.toString());
                    if(!FirebaseApp.getApps(getApplicationContext()).isEmpty())  FirebaseApp.getInstance().delete();
                }
            }
            else {
                disconnectFromFireBaseDataBase();
                gridLayout.removeAllViews();
            }
       // }
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
                connectedToTheInternet = true;
                CardView cr = findViewById(R.id.internetConnectionLed);
                cr.setCardBackgroundColor(getResources().getColor(R.color.color_8, getTheme()));

            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);

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
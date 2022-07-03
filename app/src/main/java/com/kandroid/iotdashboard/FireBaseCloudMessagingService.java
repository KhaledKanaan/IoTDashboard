package com.kandroid.iotdashboard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
//import com.google.firebase.quickstart.fcm.R;
//
//import androidx.work.OneTimeWorkRequest;
//import androidx.work.WorkManager;

public class FireBaseCloudMessagingService extends FirebaseMessagingService {


    public static final String TAG = "FirebaseCloudMessagingService", USER_NOTIFICATION_TOKENS = "UserNotificationTokens";


    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            if (/* Check if data needs to be processed by long running job */ false) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob();
            } else {
                // Handle message within 10 seconds
                handleNow();
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());

        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.

        sendNotification(remoteMessage.getNotification().getBody());
    }


    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        sendRegistrationToServer(token);
    }



    private void scheduleJob() {
//        // [START dispatch_job]
//        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class)
//                .build();
//        WorkManager.getInstance(this).beginWith(work).enqueue();
//        // [END dispatch_job]
    }

    private void handleNow() {
       // Log.d(TAG, "Short lived task is done.");

    }

    public static DatabaseReference getUserNotificationTokensDatabaseReference(String serverType, String userDBRoot){

        serverType = serverType.toLowerCase();
        DatabaseReference databaseReference = null;

        if(serverType.equals(LauncherActivity.PUBLIC.toLowerCase()) && !(MainActivity.THINGS_DB_ROOT==null) && !MainActivity.THINGS_DB_ROOT.isEmpty()
                && !(userDBRoot==null) && !userDBRoot.isEmpty()){
            databaseReference = FirebaseDatabase.getInstance().getReference(userDBRoot).child(USER_NOTIFICATION_TOKENS);
        }
        else if(serverType.equals(LauncherActivity.PRIVATE.toLowerCase()) && !(MainActivity.THINGS_DB_ROOT==null) &&
                !MainActivity.THINGS_DB_ROOT.isEmpty()){
            databaseReference = FirebaseDatabase.getInstance().getReference(USER_NOTIFICATION_TOKENS);
        }
        else if(serverType.equals(LauncherActivity.SEMI_PUBLIC.toLowerCase()) && !(MainActivity.THINGS_DB_ROOT==null) &&
                !MainActivity.THINGS_DB_ROOT.isEmpty() && !(userDBRoot==null) && !userDBRoot.isEmpty()){
            databaseReference = FirebaseDatabase.getInstance().getReference(userDBRoot).child(USER_NOTIFICATION_TOKENS);
        }
        return databaseReference;
    }

//    public static DatabaseReference getUserNotificationTokensDatabaseReference(String serverType){
//
//        serverType = serverType.toLowerCase();
//        DatabaseReference databaseReference = null;
//        if(serverType.equals("public") && !(MainActivity.USER_DB_ROOT==null) && !MainActivity.USER_DB_ROOT.isEmpty()){
//            databaseReference = FirebaseDatabase.getInstance().getReference(MainActivity.USER_DB_ROOT).child(USER_NOTIFICATION_TOKENS);
//        }
//        else if(serverType.equals("private") && !(MainActivity.THINGS_DB_ROOT==null) && !MainActivity.THINGS_DB_ROOT.isEmpty()){
//            databaseReference = FirebaseDatabase.getInstance().getReference(USER_NOTIFICATION_TOKENS);
//        }
//        return databaseReference;
//
//    }

    private void sendRegistrationToServer(String token) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null){
            DatabaseReference IoT_Database = getUserNotificationTokensDatabaseReference(LauncherActivity.SERVER_TYPE, user.getUid());
            if(IoT_Database != null){
                IoT_Database.child(token).setValue(true);
            }
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    public void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 10 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = "ALARM_NOTIFICATION_CHANNEL_ID";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_baseline_notifications_active_24)
                        .setContentTitle("Alarm")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(10 /* ID of notification */, notificationBuilder.build());
    }

}

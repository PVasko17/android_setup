package com.devforfun.app.util;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class PushService extends FirebaseMessagingService {
    public PushService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> msg = remoteMessage.getData();

        SharedPreferences settings = getSharedPreferences(Constants.USER_INFO, 0);
        try {
            JSONObject user = new JSONObject(settings.getString(Constants.USER_INFO_PARAM, ""));

            if (user.length() != 0) {
                Intent broadcastIntent = new Intent();
                broadcastIntent.putExtra("notificationType", Integer.parseInt(msg.get("type")));
                broadcastIntent.setAction("com.devforfun.app.NOTIFICATION_EVENT");
                sendBroadcast(broadcastIntent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        super.onMessageReceived(remoteMessage);
    }

    @Override
    public void onNewToken(String refreshedToken) {

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        SharedPreferences settings = getSharedPreferences(Constants.FIREBASE_DEVICE_TOKEN, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Constants.FIREBASE_DEVICE_TOKEN, refreshedToken);
        Log.e("refreshedToken", refreshedToken);
        // Commit the edits!
        editor.commit();

        JSONObject user = MyActivity.getUserInfo(this);
        ServerRequest mAuthTask = new ServerRequest(null, null, false);
        if (user.length() != 0) {
            mAuthTask.updateDeviceToken();
        }
        super.onNewToken(refreshedToken);
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }
}
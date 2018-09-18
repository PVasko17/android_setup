package com.devforfun.app.util;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.devforfun.app.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

// First parameter - doInBackground parameter
// Second Parameter - onProgressUpdate parameter
// Third Parameter - doInBackground return Value
public class ServerRequest extends AsyncTask<String, Integer, Boolean> {

    private String hostName = Constants.BASE_URL + "api.php";
    private Map<String, String> params;
    public JSONObject httpResponse;
    private AsyncCallback callback;
    private WeakReference<Activity> activityReference;
    private boolean message;
    private String REQUEST_METHOD;
    private String API_ACTION;
    private boolean allowLogout;

    public ServerRequest(AsyncCallback callback, Activity activity, boolean message) {
        this.callback = callback;
        this.activityReference = new WeakReference<>(activity);
        this.message = message;
        this.params = new HashMap<String, String>();
        this.REQUEST_METHOD = "POST";
        this.allowLogout = true;
    }

    public interface AsyncCallback {
        void onFinished(Boolean result);
    }

    public static JSONArray addPage(JSONArray data, JSONArray old_data) {
        JSONArray new_data = new JSONArray();
        if (old_data != null) {
            new_data = old_data;
            int cnt = data.length();
            int i;
            for (i = 0; i < cnt; i++) {
                try {
                    new_data.put(data.getJSONObject(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            new_data = data;
        }

        return new_data;
    }

    public void login(String email, String password, int userStatus, String token) {
        API_ACTION = "?r=" + Constants.API_VERSION + "/login";
        SharedPreferences settings = activityReference.get().getSharedPreferences(Constants.FIREBASE_DEVICE_TOKEN, 0);
        String deviceToken = settings.getString(Constants.FIREBASE_DEVICE_TOKEN, "");

        if (!token.isEmpty()) {
            params.put("token", token);
        } else {
            params.put("params[email]", email);
            params.put("params[password]", password);
        }
        if (!deviceToken.isEmpty()) {
            params.put("deviceToken", deviceToken);
            params.put("platform", "android");
        }
        params.put("timezone", (TimeZone.getDefault().getRawOffset() / 1000) + "");
        allowLogout = false;
        this.execute(generateParams(params));
    }

    public void updateDeviceToken() {
        JSONObject user = getUserInfo();
        if (activityReference.get() != null) {
            SharedPreferences settings = activityReference.get().getSharedPreferences(Constants.FIREBASE_DEVICE_TOKEN, 0);
            String deviceToken = settings.getString(Constants.FIREBASE_DEVICE_TOKEN, "");

            API_ACTION = "?r=" + Constants.API_VERSION + "/deviceToken";
            params.put("deviceToken", deviceToken);
            params.put("platform", "android");
            try {
                allowLogout = false;
                params.put("token", user.getString("token"));
                this.execute(generateParams(params));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private String generateParams(Map<String, String> params) {
        Log.e("req", API_ACTION + " " + params.toString().replace(", ", "&"));

        String nameValuePairs = "";
        for (Map.Entry entry : params.entrySet()) {
            nameValuePairs = nameValuePairs.concat("&" + entry.getKey().toString() + "=" + entry.getValue().toString());
        }
        return nameValuePairs;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            URL url = new URL(hostName + API_ACTION);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            StringBuilder resp = new StringBuilder();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            dos.writeBytes(params[0]);
            dos.flush();
            dos.close();

            int response = connection.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = "";

            while ((line = br.readLine()) != null) {
                resp.append(line);
            }

            Log.e("resp", resp.toString());

            httpResponse = new JSONObject(resp.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("error", e.getMessage(), e);
        }

        return httpResponse != null;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (!success) {
            Toast.makeText(activityReference.get(), R.string.error_network, Toast.LENGTH_SHORT).show();
        } else {
            try {
                if (httpResponse.getInt("status") == 401 && allowLogout) {
                    logout();
                }
                if (message || (httpResponse.getInt("status") != 200 &&
                        httpResponse.getInt("status") != 501 && httpResponse.getInt("status") != 403)) {
                    Toast.makeText(activityReference.get(), httpResponse.getString("message"), Toast.LENGTH_LONG).show();
                } else if (httpResponse.getInt("status") == 403) {
                    new AlertDialog.Builder(activityReference.get(), R.style.AppTheme_AlertDialog)
                            .setMessage(R.string.error_not_allowed_action)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    activityReference.get().finish();
                                }
                            })
                            .setIcon(R.drawable.ic_warning_white)
                            .show();
                }
            } catch (Exception e) {
                Toast.makeText(activityReference.get(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        if (callback != null) {
            callback.onFinished(success);
        }
    }

    private void logout() {
        // save user data to device storage
        SharedPreferences settings = activityReference.get().getSharedPreferences(Constants.USER_INFO, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.apply();

        Intent login = new Intent(activityReference.get(), LoginActivity.class);
        login.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activityReference.get().startActivity(login);
    }

    private JSONObject getUserInfo() {
        try {
            SharedPreferences settings = activityReference.get().getSharedPreferences(Constants.USER_INFO, 0);
            return new JSONObject(settings.getString(Constants.USER_INFO_PARAM, ""));
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
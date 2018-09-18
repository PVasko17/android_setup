package com.devforfun.app.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.PopupMenu;

import com.devforfun.app.R;
import com.devforfun.app.callbacks.IPermissionCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class MyActivity extends AppCompatActivity {

    protected Context context;
    public MyActivity activity;
    protected NavigationDrawer drawer;

    private ServerRequest mCheckExpiredTask;

    protected int screenSize;

    public TabLayout tabBar;
    public Toolbar toolbar;
    public AppBarLayout appBar;
    private View offline_mode;
    public int currentApiVersion = android.os.Build.VERSION.SDK_INT;

    protected static final String DEBUG_TAG = "MinApp";

    protected JSONObject currentUser;

    private ServerRequest mValidateTask;

    protected Locale defaultLocale;

    protected String imagePath;
    protected Uri fileUri;
    protected int imageRequestCode;
    protected boolean isGPhotos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        if (android.os.Build.VERSION.SDK_INT >= 24) {
            defaultLocale = getResources().getConfiguration().getLocales().get(0);
        } else {
            defaultLocale = getResources().getConfiguration().locale;
        }

        turnPhoneLandscapeOff();

        if (savedInstanceState != null) {
            imagePath = savedInstanceState.getString("imagePath", "");
            fileUri = Uri.parse(savedInstanceState.getString("fileUri", ""));
            isGPhotos = savedInstanceState.getBoolean("isGPhotos", false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (drawer != null && drawer.pinDialog != null) {
            drawer.pinDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        offline_mode = findViewById(R.id.offline_mode);
        if (!isNetworkOnline()) {
            if (offline_mode != null) {
                offline_mode.setVisibility(View.VISIBLE);
            }
        } else {
            if (offline_mode != null) {
                offline_mode.setVisibility(View.GONE);
            }
        }
    }

    private void turnPhoneLandscapeOff() {
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    protected void initElements() {
        activity = this;
        context = this;
        initToolbar();

        currentUser = getUserInfo(context);
    }

    protected void initElementsDrawer(int activeTab, int highlightTab) {
        initElements();
        drawer = new NavigationDrawer(activity, activeTab, highlightTab);
        drawer.initDrawer();
    }

    private void updateCurrentUserInfo() {
        SharedPreferences prevTimePref = getSharedPreferences(Constants.PREV_EXPIRE_CHECK, 0);
        long prevSeconds = prevTimePref.getLong(Constants.PREV_EXPIRE_CHECK, 0);
        long currentSeconds = Calendar.getInstance().getTimeInMillis();

        try {
            if (currentUser.getInt("expired") == 1 || currentSeconds - prevSeconds > Constants.ACCESS_CHECK_TIMEOUT) {
                showProgress(true);
                mCheckExpiredTask = new ServerRequest(checkExpiredCallback, activity, false);
                mCheckExpiredTask.login("", "", 0, currentUser.getString("token"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void initToolbar() {
        tabBar = findViewById(R.id.tab_layout);
        toolbar = findViewById(R.id.toolbar);
        appBar = findViewById(R.id.app_bar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setHomeButtonEnabled(true);
//            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    protected void showProgress(boolean isShow) {
        View progressView = findViewById(R.id.progress);
        View formView = findViewById(R.id.content);
        if (progressView != null) {
            progressView.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }

        if (formView != null) {
            formView.setVisibility(isShow ? View.GONE : View.VISIBLE);
        }
    }

    public int getCurrentApiVersion() {
        return currentApiVersion;
    }

    public boolean isNetworkOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    protected boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    protected boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void hideToolbarElevation() {
        if (currentApiVersion >= Build.VERSION_CODES.LOLLIPOP) {
            if (appBar != null) {
                appBar.setElevation(0);
            }
            if (toolbar != null) {
                toolbar.setElevation(0);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void showToolbarElevation() {
        if (currentApiVersion >= Build.VERSION_CODES.LOLLIPOP) {
            if (appBar != null) {
                appBar.setElevation(getResources().getDimensionPixelOffset(R.dimen.toolBarElevation));
            }
            if (toolbar != null) {
                toolbar.setElevation(getResources().getDimensionPixelOffset(R.dimen.toolBarElevation));
            }
        }
    }

    public static JSONObject getUserInfo(Context context) {
        SharedPreferences settings = context.getSharedPreferences(Constants.USER_INFO, 0);
        JSONObject user = new JSONObject();
        try {
            user = new JSONObject(settings.getString(Constants.USER_INFO_PARAM, ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return user;
    }

    protected void logout() {
//        Clear user data on device storage
        SharedPreferences settings = context.getSharedPreferences(Constants.USER_INFO, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Constants.USER_INFO_PARAM, "");
        // Commit the edits!
        editor.commit();

        Intent loginScreen = new Intent(context, LoginActivity.class);
        loginScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(loginScreen);
    }

    /**
     * Converts given timestamp to "d MMM y"
     *
     * @param date Timestamp
     * @return formatted date
     */
    public static String convertTimestampToString(int date) {
        SimpleDateFormat outDateFormat = new SimpleDateFormat(Constants.TIME_PATTERN_DATE, Locale.getDefault());
        Date d = new java.util.Date((long) date * 1000);
        return outDateFormat.format(d);
    }

    /**
     * Converts given timestamp to any suggested format
     *
     * @param date   Timestamp
     * @param format SimpleDateFormat compatible format string
     * @return formatted date
     */
    public static String convertTimestampToString(int date, String format) {
        SimpleDateFormat outDateFormat = new SimpleDateFormat(format, Locale.getDefault());
        Date d = new java.util.Date((long) date * 1000);
        return outDateFormat.format(d).toLowerCase();
    }

    public static String convertSecondsToWorkTime(int seconds) {
        int hours = 0;
        int minutes = 0;

        if (seconds >= 3600) {
            hours = (int) Math.floor(seconds / 3600);
            seconds -= hours * 3600;
            minutes = (int) Math.floor(seconds / 60);
        } else {
            minutes = (int) Math.floor(seconds / 60);
            hours = 0;
        }

        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
    }

    /**
     * Converts given timestamp from milliseconds to "d MMM y"
     *
     * @param date Timestamp
     * @return formatted date
     */
    public static String convertTimestampToString(long date) {
        SimpleDateFormat outDateFormat = new SimpleDateFormat(Constants.TIME_PATTERN_DATE, Locale.getDefault());
        Date d = new java.util.Date(date);
        return outDateFormat.format(d);
    }

    /**
     * Converts given date string "y-M-d" to "d MMM y"
     *
     * @param date Date string
     * @return formatted date
     */
    public static String convertDateToTimestamp(String date) {
        SimpleDateFormat inDateFormat = new SimpleDateFormat("y-M-d", Locale.getDefault());
        try {
            Date d = inDateFormat.parse(date);
            return convertTimestampToString(d.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return convertTimestampToString(Calendar.getInstance().getTimeInMillis());
        }
    }

    public static long getCurrentGMTTime() {
        return Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis();
    }

    public JSONArray removeListItem(JSONArray list, int position) {
        if (currentApiVersion >= 19) {
            list.remove(position);
        } else {
            JSONArray tmp = new JSONArray();

            int i;
            int cnt = list.length();
            for (i = 0; i < cnt; i++) {
                if (i != position) {
                    try {
                        tmp.put(list.get(position));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            list = tmp;
        }

        return list;
    }

    private ServerRequest.AsyncCallback checkExpiredCallback = new ServerRequest.AsyncCallback() {
        @Override
        public void onFinished(Boolean result) {
            try {
                if (result && mCheckExpiredTask.httpResponse.getInt("status") == 200) {
                    SharedPreferences settings = getSharedPreferences(Constants.USER_INFO, 0);
                    SharedPreferences.Editor editor = settings.edit();

                    JSONObject data = mCheckExpiredTask.httpResponse.getJSONObject("data");
                    editor.putString(Constants.USER_INFO_PARAM, data.getString("info"));
                    editor.apply();
                    currentUser = data.getJSONObject("info");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                showProgress(false);
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (drawer != null) {
            if (drawer.drawer_layout.isDrawerOpen(drawer.navigationView)) {
                drawer.drawer_layout.closeDrawer(drawer.navigationView);
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            if (drawer != null) {
                if (drawer.drawer_layout.isDrawerOpen(drawer.navigationView)) {
                    drawer.drawer_layout.closeDrawer(drawer.navigationView);
                } else {
                    if (drawer.getIsSynced()) {
                        drawer.drawer_layout.openDrawer(drawer.navigationView);
                    } else {
                        super.onBackPressed();
                    }
                }
            } else {
                super.onBackPressed();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imagePath != null) {
            outState.putString("imagePath", imagePath);
        }
        if (fileUri != null) {
            outState.putString("fileUri", fileUri.toString());
        }
        outState.putBoolean("isGPhotos", isGPhotos);
    }

    //    Attach image
    protected IPermissionCallback permissionCallback;

    public static Bitmap resizeImage(String image) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(new FileInputStream(image), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (bitmap != null) {
            return Bitmap.createScaledBitmap(bitmap, Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_WIDTH, true);
        }
        return null;
    }

    protected void resizeCroppedImage() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, Constants.IMAGE_WIDTH, Constants.IMAGE_WIDTH, false);

//            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File imageFile = new File(new URI(fileUri.toString()));
            FileOutputStream out = null;

            out = new FileOutputStream(imageFile);
            resized.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance

            imagePath = imageFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // get image from file system
    protected void chooseImage(final View view) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.attach_image);

        if (imagePath != null && !imagePath.isEmpty()) {
            popupMenu.getMenu().findItem(R.id.image_remove).setVisible(true);
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.image_fs:
                        imageRequestCode = Constants.SELECT_IMAGE_REQUEST;
                        checkReadStoragePermission();
                        return true;
                    case R.id.image_camera:
                        imageRequestCode = Constants.CAPTURE_IMAGE_REQUEST;
                        checkCameraPermission();
                        return true;
                    case R.id.image_remove:
                        ImageButton imgView = (ImageButton) view;
//                        imgView.setImageResource(R.drawable.ic_add_circle_outline_black);
                        imgView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        imagePath = "";
                        fileUri = null;
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }

    protected void checkStoragePermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        Constants.STORAGE_PERMISSION);
            }
        } else {
            if (permissionCallback != null) {
                permissionCallback.storagePermissionGranted();
            }
        }
    }

    protected void checkWriteStoragePermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.WRITE_STORAGE_PERMISSION);
            }
        } else {
            if (permissionCallback != null) {
                permissionCallback.storageWritePermissionGranted();
            }
        }
    }

    protected void checkReadStoragePermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        Constants.READ_STORAGE_PERMISSION);
            }
        } else {
            if (permissionCallback != null) {
                permissionCallback.storageReadPermissionGranted();
            }
        }
    }

    protected void checkCameraPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CAMERA},
                        Constants.CAMERA_PERMISSION);
            }
        } else {
            if (permissionCallback != null) {
                permissionCallback.cameraPermissionGranted();
            }
        }
    }

    protected String getImagePath(Uri uri) {
        if (uri != null) {
            String path = "";
            String[] projection = {MediaStore.Images.Media.DATA};
            if (currentApiVersion < 19) {
                Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                int column_index = 0;
                if (cursor != null && cursor.moveToFirst()) {
                    column_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    path = cursor.getString(column_index);
                    cursor.close();
                }
            } else {
                String wholeID = DocumentsContract.getDocumentId(uri);

                // Split at colon, use second item in the array
                String id = wholeID.split(":")[1];


                // where id is equal to
                String sel = MediaStore.Images.Media._ID + "=?";

                Cursor cursor = getContentResolver().
                        query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                projection, sel, new String[]{id}, null);

                if (cursor != null) {

                    int columnIndex = cursor.getColumnIndex(projection[0]);

                    if (cursor.moveToFirst()) {
                        path = cursor.getString(columnIndex);
                    }
                    cursor.close();
                }
            }

            return path == null ? "" : path;
        }
        return "";
    }

    protected void getImagePath(Intent data) {
        fileUri = data.getData();
        String url = data.getData().toString();
        if (url.startsWith("content://com.google.android.apps.photos.content") ||
                url.startsWith("content://com.google.android.apps.docs.storage")) {
            isGPhotos = true;
            try {
                InputStream is = this.getContentResolver().openInputStream(fileUri);
                if (is != null) {
                    Bitmap pictureBitmap = BitmapFactory.decodeStream(is);
//                    createImagePreview();
                    File imageFile = createImageFile();
                    FileOutputStream out = null;

                    out = new FileOutputStream(imageFile);
                    pictureBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    out.close();
                    imagePath = imageFile.getAbsolutePath();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            isGPhotos = false;
            imagePath = getImagePath(fileUri);
        }
    }

    protected File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        imagePath = image.getAbsolutePath();
        return image;
    }

    protected void galleryAddPic(Activity activity) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);
        fileUri = Uri.fromFile(f);
        mediaScanIntent.setData(fileUri);
        activity.sendBroadcast(mediaScanIntent);
    }

    protected void selectPicture(int imageRequestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, imageRequestCode);
    }

    protected void takePicture(int imageRequestCode) {
        if (currentApiVersion < 21) {
            Intent cameraIntent = new Intent();
            cameraIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(cameraIntent, imageRequestCode);
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.devforfun.app.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, imageRequestCode);
                }
            }
        }
    }

    @NonNull
    protected RequestBody createPartFromString(String descriptionString) {
        return RequestBody.create(
                MultipartBody.FORM, descriptionString);
    }

    @NonNull
    protected MultipartBody.Part prepareFilePart(String partName, String filePath, Uri fileUri) {
        // https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
        // use the FileUtils to get the actual file by uri
        File file = new File(filePath);

        // create RequestBody instance from file
        String MIME = "image/jpeg";
        if (context.getContentResolver().getType(fileUri) != null) {
            MIME = context.getContentResolver().getType(fileUri);
        }
        RequestBody requestFile =
                RequestBody.create(
                        MediaType.parse(MIME),
                        file
                );

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (permissionCallback != null) {
            switch (requestCode) {
                case Constants.READ_STORAGE_PERMISSION:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        permissionCallback.storageReadPermissionGranted();
                    }
                    break;
                case Constants.WRITE_STORAGE_PERMISSION:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        permissionCallback.storageWritePermissionGranted();
                    }
                    break;
                case Constants.CAMERA_PERMISSION:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        permissionCallback.cameraPermissionGranted();
                    }
                    break;
                case Constants.STORAGE_PERMISSION:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        permissionCallback.storagePermissionGranted();
                    }
                    break;
            }
        }
    }
}
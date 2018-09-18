package com.devforfun.app.util;

public class Constants {
    public static final String BASE_URL = "HOST_URL";

    public static final String API_VERSION = "v1";

    public static final String USER_INFO = "UserInfo";
    public static final String USER_INFO_PARAM = "user_info";
    public static final String LAST_LOGIN = "LastLogin";
    public static final String FIREBASE_DEVICE_TOKEN = "deviceToken";
    public static final String PREV_EXPIRE_CHECK = "expiration_check";
    public static final String NOTIFICATIONS_COUNTER_PARAM = "notifications";

    public static final int IMAGE_THUMB_WIDTH = 150;
    public static final int IMAGE_WIDTH = 900;

//   it's one hour
    public static final long ACCESS_CHECK_TIMEOUT = 3600000;

    public static final String TIME_PATTERN_DATE = "d MMM y";
    public static final String TIME_PATTERN_DATE_TIME = "d MMM y K:mm a";
    public static final String TIME_PATTERN_TIME = "K:mm a";
    public static final String TIME_PATTERN_WEEK_DAY = "E, MMM d";
    public static final String TIME_PATTERN_WEEK_DAY_TIME = "E, MMM d K:mm a";

    public static final String TIME_PICKER_DIALOG = "TimePickerDialog";
    public static final String DATE_PICKER_DIALOG = "DatePickerDialog";

    public static final int READ_STORAGE_PERMISSION = 1;
    public static final int WRITE_STORAGE_PERMISSION = 2;
    public static final int CAMERA_PERMISSION = 3;
    public static final int STORAGE_PERMISSION = 4;

    public static final int SELECT_IMAGE_REQUEST = 1;
    public static final int CAPTURE_IMAGE_REQUEST = 2;
}

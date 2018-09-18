package com.devforfun.app.util;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.devforfun.app.R;
import com.devforfun.app.ListsActivity;
import com.devforfun.app.ManagerNotificationsActivity;
import com.devforfun.app.NotificationsActivity;
import com.devforfun.app.R;
import com.devforfun.app.UserSettingsActivity;
import com.devforfun.app.UserTimeSheetActivity;
import com.devforfun.app.callbacks.IDrawerLocationChanged;
import com.devforfun.app.diary.DiaryListActivity;
import com.devforfun.app.owner.CreateLocationActivity;
import com.devforfun.app.owner.OwnerSettingsActivity;
import com.devforfun.app.owner.StaffActivity;
import com.devforfun.app.owner.TimeSheetActivity;
import com.devforfun.app.recipes.RecipesActivity;
import com.devforfun.app.schedule.ScheduleListActivity;
import com.devforfun.app.wastage.WastageActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.MODE_PRIVATE;

public class NavigationDrawer {

    private ServerRequest mNotificationsTask;

    private MyActivity activity;
    private int active_tab;
    private int highlight_tab;
    private boolean isSynced;

    public NavigationView navigationView;
    public DrawerLayout drawer_layout;

    public Dialog pinDialog;

    private JSONObject currentUser;
    private JSONArray placesData;

    private SharedPreferences placePref;

    private boolean isPlacesMenu;
    private int notificationsNumber;

    public NavigationDrawer(MyActivity activity, int active_tab, int highlight_tab) {
        this.activity = activity;
        this.active_tab = active_tab;
        this.highlight_tab = highlight_tab;
    }

    public DrawerLayout initDrawer() {
        navigationView = activity.findViewById(R.id.navigation);
        drawer_layout = activity.findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(activity, drawer_layout,
                null, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                if (isPlacesMenu) {
                    resetDrawerMenu();
                }
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank
                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to drawer layout
        drawer_layout.addDrawerListener(actionBarDrawerToggle);
        //calling sync state is necessary or else your hamburger icon wont show up
        if (active_tab != 0) {
            isSynced = true;
            actionBarDrawerToggle.syncState();
        }

        currentUser = MyActivity.getUserInfo(activity);

        initDrawerHeader();
        updateDrawerMenu(isPlacesMenu = false);

        if (activity.getCurrentApiVersion() >= 23) {
            navigationView.setItemTextColor(activity.getResources().getColorStateList(R.color.activated_background,
                    activity.getApplicationContext().getTheme()));
        } else {
            navigationView.setItemTextColor(activity.getResources().getColorStateList(R.color.activated_background));
        }

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                return goByLink(menuItem.getItemId());
            }
        });

        if (activity.isNetworkOnline()) {
            mNotificationsTask = new ServerRequest(notificationsCallback, activity, false);
            mNotificationsTask.getNotificationsNumber();
        }

        return drawer_layout;
    }

    private void resetDrawerMenu() {
        updateDrawerMenu(isPlacesMenu = false);
    }

    private void updateDrawerMenu(boolean isPlaces) {
        navigationView.getMenu().clear();

        navigationView.inflateMenu(R.menu.menu_drawer);

        navigationView.setCheckedItem(active_tab);
        navigationView.getMenu().findItem(highlight_tab).setChecked(true);

//            Set up an action view to display number of notifications available
        SquareTextView actionView = navigationView.getMenu()
                .findItem(R.id.drawer_action_notifications).getActionView().findViewById(R.id.action_text);
        if (notificationsNumber > 0) {
            actionView.setText(Integer.toString(notificationsNumber));
            actionView.setVisibility(View.VISIBLE);
        } else {
            actionView.setVisibility(View.GONE);
        }
    }

    private void initDrawerHeader() {
        TextView menu_user_name = navigationView.getHeaderView(0).findViewById(R.id.menu_user_name);

//            Fill current user name
        try {
            menu_user_name.setText(currentUser.getString("full_name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean getIsSynced() {
        return isSynced;
    }

    public void updateDrawerHeader() {
        TextView menu_user_name = navigationView.getHeaderView(0).findViewById(R.id.menu_user_name);

        currentUser = MyActivity.getUserInfo(activity);
        try {
            menu_user_name.setText(currentUser.getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean goByLink(int itemId) {
        drawer_layout.closeDrawer(navigationView);

        if (itemId != active_tab) {
            switch (itemId) {
                case R.id.drawer_action_lists:
                    Intent lists = new Intent(activity, ListsActivity.class);
                    activity.startActivity(lists);
                    return true;
                default:
                    return false;
            }
        } else {
            // Close Drawer if current menu selected
            return false;
        }
    }

    private ServerRequest.AsyncCallback notificationsCallback = new ServerRequest.AsyncCallback() {
        @Override
        public void onFinished(Boolean result) {
            try {
                if (result && mNotificationsTask.httpResponse.getInt("status") == 200) {
                    SharedPreferences notifyPref = activity.getSharedPreferences(Constants.NOTIFICATIONS_COUNTER_PARAM, MODE_PRIVATE);
                    SharedPreferences.Editor notifyEdit = notifyPref.edit();

                    int scheduleNumber = mNotificationsTask.httpResponse.getJSONObject("data").getInt("schedule");
                    int notifyNumber = mNotificationsTask.httpResponse.getJSONObject("data").getInt("notifications");
                    int prevNotifyNumber = notifyPref.getInt(Constants.NOTIFICATIONS_COUNTER_PARAM, 0);

//                    Save current notifications number if user visits Notifications activity
                    if (activity.getLocalClassName().equals(NotificationsActivity.class.getSimpleName())) {
                        notifyEdit.putInt(Constants.NOTIFICATIONS_COUNTER_PARAM, notifyNumber);
                        notifyEdit.apply();
                    }

//                    Minus previous counter value if server value is bigger
                    if (notifyNumber > prevNotifyNumber) {
                        notifyNumber -= prevNotifyNumber;
                    }
//                    If the same counter value received from server, ignore this counter
                    else if (notifyNumber == prevNotifyNumber) {
                        notifyNumber = 0;
                    }

                    notificationsNumber = notifyNumber + scheduleNumber;
                    if (notificationsNumber > 0) {
                        SquareTextView actionView = navigationView.getMenu()
                                .findItem(R.id.drawer_action_notifications).getActionView().findViewById(R.id.action_text);
                        actionView.setText(Integer.toString(notificationsNumber));
                        actionView.setVisibility(View.VISIBLE);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
}
package com.hillal.hhhhhhh.data.update;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.hillal.hhhhhhh.R;

public class AppUpdateManager {
    private static final String TAG = "AppUpdateManager";
    private static final int UPDATE_REQUEST_CODE = 500;
    private final Context context;
    private final com.google.android.play.core.appupdate.AppUpdateManager appUpdateManager;

    public AppUpdateManager(Context context) {
        this.context = context;
        this.appUpdateManager = AppUpdateManagerFactory.create(context);
    }

    public void checkForUpdates(Activity activity) {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                startUpdate(activity, appUpdateInfo);
            }
        });
    }

    private void startUpdate(Activity activity, AppUpdateInfo appUpdateInfo) {
        try {
            AppUpdateOptions options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE)
                    .setAllowAssetPackDeletion(true)
                    .build();

            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activity,
                    options,
                    UPDATE_REQUEST_CODE
            );
        } catch (Exception e) {
            Log.e(TAG, "Error starting update: " + e.getMessage());
        }
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "Update flow failed! Result code: " + resultCode);
            }
        }
    }
} 
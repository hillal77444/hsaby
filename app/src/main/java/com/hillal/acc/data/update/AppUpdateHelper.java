package com.hillal.acc.data.update;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.hillal.acc.data.model.ServerAppUpdateInfo;
import com.hillal.acc.data.remote.DataManager;

public class AppUpdateHelper {
    private static final String TAG = "AppUpdateHelper";
    private static final int UPDATE_REQUEST_CODE = 500;
    private final Context context;
    private final DataManager dataManager;
    private final AppUpdateManager appUpdateManager;

    public AppUpdateHelper(Context context) {
        this.context = context;
        this.dataManager = new DataManager(context);
        this.appUpdateManager = AppUpdateManagerFactory.create(context);
    }

    public void checkForUpdates(Activity activity) {
        // التحقق من تحديثات Google Play
        checkGooglePlayUpdates(activity);
        
        // التحقق من تحديثات الخادم
        checkServerUpdates(activity);
    }

    private void checkGooglePlayUpdates(Activity activity) {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                startGooglePlayUpdate(activity, appUpdateInfo);
            }
        });
    }

    private void checkServerUpdates(Activity activity) {
        dataManager.checkForUpdates(new DataManager.ApiCallback() {
            @Override
            public void onSuccess(Object response) {
                if (response instanceof ServerAppUpdateInfo) {
                    ServerAppUpdateInfo updateInfo = (ServerAppUpdateInfo) response;
                    showServerUpdateDialog(activity, updateInfo);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking for server updates: " + error);
            }
        });
    }

    private void startGooglePlayUpdate(Activity activity, AppUpdateInfo appUpdateInfo) {
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
            Log.e(TAG, "Error starting Google Play update: " + e.getMessage());
        }
    }

    private void showServerUpdateDialog(Activity activity, ServerAppUpdateInfo updateInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("تحديث جديد متوفر")
                .setMessage("الإصدار الجديد: " + updateInfo.getVersion() + "\n\n" + updateInfo.getDescription())
                .setPositiveButton("تحديث", (dialog, which) -> {
                    openDownloadLink(updateInfo.getDownloadUrl());
                });

        if (!updateInfo.isForceUpdate()) {
            builder.setNegativeButton("لاحقاً", null);
        }

        AlertDialog dialog = builder.create();
        dialog.setCancelable(!updateInfo.isForceUpdate());
        dialog.show();
    }

    private void openDownloadLink(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening download link: " + e.getMessage());
        }
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "Google Play update flow failed! Result code: " + resultCode);
            }
        }
    }
} 
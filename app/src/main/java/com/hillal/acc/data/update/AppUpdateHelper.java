package com.hillal.acc.data.update;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Environment;
import android.app.ProgressDialog;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.hillal.acc.BuildConfig;
import com.hillal.acc.data.model.ServerAppUpdateInfo;
import com.hillal.acc.data.remote.DataManager;
import com.hillal.acc.data.room.AccountDao;
import com.hillal.acc.data.room.TransactionDao;
import com.hillal.acc.data.room.PendingOperationDao;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppUpdateHelper {
    private static final String TAG = "AppUpdateHelper";
    private static final int UPDATE_REQUEST_CODE = 500;
    private final Context context;
    private final DataManager dataManager;
    private final AppUpdateManager appUpdateManager;
    private final String currentVersion;
    private ServerAppUpdateInfo updateInfo;

    public AppUpdateHelper(Context context, AccountDao accountDao, TransactionDao transactionDao, PendingOperationDao pendingOperationDao) {
        this.context = context;
        this.dataManager = new DataManager(context, accountDao, transactionDao, pendingOperationDao);
        this.appUpdateManager = AppUpdateManagerFactory.create(context);
        this.currentVersion = getCurrentVersion();
    }

    private String getCurrentVersion() {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting current version", e);
            return "0.0.0";
        }
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
        Log.d(TAG, "Checking for server updates...");
        dataManager.checkForUpdates(currentVersion, new DataManager.ApiCallback() {
            @Override
            public void onSuccess(ServerAppUpdateInfo updateInfo) {
                Log.d(TAG, "Received update info: " + (updateInfo != null ? updateInfo.getVersion() : "null"));
                if (updateInfo != null) {
                    handleUpdateInfo(updateInfo);
                } else {
                    Log.d(TAG, "No updates available from server");
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking for updates: " + error);
                // إظهار رسالة للمستخدم
                activity.runOnUiThread(() -> {
                    new AlertDialog.Builder(context)
                        .setTitle("خطأ في التحقق من التحديثات")
                        .setMessage("حدث خطأ أثناء التحقق من وجود تحديثات: " + error)
                        .setPositiveButton("حسناً", null)
                        .show();
                });
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

    private void handleUpdateInfo(ServerAppUpdateInfo updateInfo) {
        if (updateInfo == null) {
            Log.d(TAG, "Update info is null");
            return;
        }

        Log.d(TAG, "Current version: " + currentVersion + ", New version: " + updateInfo.getVersion());
        if (isNewVersionAvailable(updateInfo.getVersion())) {
            Log.d(TAG, "New version is available");
            if (updateInfo.isForceUpdate()) {
                Log.d(TAG, "This is a forced update");
                showForceUpdateDialog(updateInfo);
            } else {
                Log.d(TAG, "This is an optional update");
                showUpdateDialog(updateInfo);
            }
        } else {
            Log.d(TAG, "No new version available");
        }
    }

    private boolean isNewVersionAvailable(String newVersion) {
        return compareVersions(newVersion, currentVersion) > 0;
    }

    private int compareVersions(String version1, String version2) {
        String[] v1 = version1.split("\\.");
        String[] v2 = version2.split("\\.");
        
        int length = Math.max(v1.length, v2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < v1.length ? Integer.parseInt(v1[i]) : 0;
            int num2 = i < v2.length ? Integer.parseInt(v2[i]) : 0;
            
            if (num1 > num2) return 1;
            if (num1 < num2) return -1;
        }
        return 0;
    }

    private void showForceUpdateDialog(ServerAppUpdateInfo updateInfo) {
        new AlertDialog.Builder(context)
            .setTitle("تحديث مطلوب")
            .setMessage(updateInfo.getDescription())
            .setCancelable(false)
            .setPositiveButton("تحديث الآن", (dialog, which) -> downloadAndInstallUpdate(updateInfo.getDownloadUrl()))
            .show();
    }

    private void showUpdateDialog(ServerAppUpdateInfo updateInfo) {
        new AlertDialog.Builder(context)
            .setTitle("تحديث متوفر")
            .setMessage(updateInfo.getDescription())
            .setPositiveButton("تحديث الآن", (dialog, which) -> downloadAndInstallUpdate(updateInfo.getDownloadUrl()))
            .setNegativeButton("لاحقاً", null)
            .show();
    }

    private void downloadAndInstallUpdate(String downloadUrl) {
        // ==================================================================================
        // هذا الجزء من الكود يقوم بتنزيل وتثبيت التحديث مباشرة.
        // إذا كنت تنوي رفع التطبيق على متجر Google Play، يجب إزالة هذا الجزء واستخدام Google Play In-App Updates API.
        // ==================================================================================
        try {
            Log.d(TAG, "Attempting to download and install from URL: " + downloadUrl);

            String fileName = "app-release.apk"; // اسم الملف الذي سيتم تنزيله
            File outputFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
            if (outputFile.exists()) {
                outputFile.delete(); // حذف الملف القديم إذا كان موجوداً
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setMimeType("application/vnd.android.package-archive");
            request.setTitle("تحميل تحديث التطبيق");
            request.setDescription("جاري تنزيل الإصدار الجديد من التطبيق...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationUri(Uri.fromFile(outputFile)); // تعيين الوجهة المحلية للملف

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = downloadManager.enqueue(request);

            // تسجيل BroadcastReceiver للاستماع عند اكتمال التنزيل
            BroadcastReceiver onComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (downloadId == id) {
                        context.unregisterReceiver(this); // إلغاء تسجيل المستقبل لتجنب تسرب الذاكرة

                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(downloadId);
                        Cursor cursor = downloadManager.query(query);

                        if (cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                                Log.d(TAG, "APK Download successful. Installing...");
                                installApk(outputFile);
                            } else {
                                int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                                int reason = cursor.getInt(reasonIndex);
                                Log.e(TAG, "APK Download failed. Reason: " + reason);
                                showSimpleAlertDialog("خطأ في التحميل", "فشل تنزيل ملف التحديث. يرجى المحاولة لاحقاً. رمز الخطأ: " + reason);
                            }
                        }
                        cursor.close();
                    }
                }
            };
            context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        } catch (Exception e) {
            Log.e(TAG, "Error initiating APK download: " + e.getMessage(), e);
            showSimpleAlertDialog("خطأ", "حدث خطأ أثناء بدء عملية التحديث. يرجى المحاولة لاحقاً.");
        }
    }

    private void installApk(File apkFile) {
        try {
            Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apkFile);
            Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            installIntent.setData(apkUri);
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(installIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error installing APK: " + e.getMessage(), e);
            showSimpleAlertDialog("خطأ في التثبيت", "حدث خطأ أثناء تثبيت التحديث. يرجى المحاولة يدوياً أو إعادة المحاولة لاحقاً.");
        }
    }

    // دالة مساعدة لعرض AlertDialog بسيط
    private void showSimpleAlertDialog(String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("حسناً", null)
                .show();
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "Google Play update flow failed! Result code: " + resultCode);
            }
        }
    }
} 
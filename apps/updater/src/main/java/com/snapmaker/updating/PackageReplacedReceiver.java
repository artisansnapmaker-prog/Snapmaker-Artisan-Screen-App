package com.snapmaker.updating;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class PackageReplacedReceiver extends BroadcastReceiver {
    private static final String TAG = "PackageReplacedReceiver";
    private static final String PACKAGE_NAME_FABSCREEN = "com.snapmaker.fabscreen";
    private static final String PACKAGE_NAME_A400 = "com.snapmaker.fabscreena400";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.toString());
        if ((Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()) && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
                || Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
            String action = intent.getAction();
            // e.g. "package:com.snapmaker.fabscreen"
            String packageNameData = intent.getDataString();
            if (action == null) return;
            if (packageNameData == null || packageNameData.length() <= 8) return;
            if (!action.equals(Intent.ACTION_PACKAGE_REPLACED)) return;
            // 8 is the length of "package:"
            String packageName = packageNameData.substring(8);
            if (!packageName.equals(PACKAGE_NAME_FABSCREEN) && !packageName.equals(PACKAGE_NAME_A400))
                return;

            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);

            // Launch newly installed app
            if (launchIntent != null) {
                launchIntent.putExtra("newApkInstalled", true);
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                context.startActivity(launchIntent);
                System.exit(0);
            }
        }
    }
}

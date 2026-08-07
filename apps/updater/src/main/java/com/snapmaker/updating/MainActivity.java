package com.snapmaker.updating;

import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {
    private static final String FABSCREEN_CRASH = "FABSCREEN_CRASH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(flags);

        initView();
        initBroadcastReceiver();
    }

    private void initView() {
        String packageName = getIntent().getStringExtra("package_name");
        String operation = getIntent().getStringExtra("operation");
        if (packageName.equals("com.snapmaker.fabscreen")) {
            // show updating progress
            if ("updating".equals(operation)) {
                replaceFragment(R.id.fcv_updating, J1UpdateFragment.class, null);
            }
        } else {
            replaceFragment(R.id.fcv_updating, DefaultUpdateFragment.class, null);
        }
    }

    private void replaceFragment(int containerId, Class<? extends Fragment> fragmentClass, Bundle args) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(containerId, fragmentClass, args)
                .commit();
    }

    private void initBroadcastReceiver() {
        PackageReplacedReceiver replacedReceiver = new PackageReplacedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addDataScheme("package");
        registerReceiver(replacedReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        TextView textView = findViewById(R.id.tv_content);
//        Button button = findViewById(R.id.btn_home_start);
//        Intent intent = getIntent();
//        if (intent != null && intent.getParcelableArrayListExtra(FABSCREEN_CRASH) != null) {
//            ArrayList<Intent> intents = intent.getParcelableArrayListExtra(FABSCREEN_CRASH);
//            button.setVisibility(View.VISIBLE);
//            // Gets the keeps stopping of the native language
//            int aerrApplicationId = getResources().getIdentifier("aerr_application", "string", "android");
//            textView.setText(getString(aerrApplicationId, "Snapmaker"));
//            int aerrRestartId = getResources().getIdentifier("aerr_restart", "string", "android");
//            button.setText(getString(aerrRestartId, "Snapmaker"));
//            button.setOnClickListener(onClick -> {
//                // Get the Activities that existed before carch was retrieved
//                if (intents != null) {
//                    // open fabscreen and send Activities List
//                    Intent startIntent = getPackageManager().getLaunchIntentForPackage(intent.getStringExtra(PACKAGE_NAME));
//                    startIntent.putParcelableArrayListExtra(FABSCREEN_CRASH, intents);
//                    startActivity(startIntent);
//                    System.exit(0);
//                }
//            });
//        } else {
//            textView.setText(R.string.loading_content);
//            button.setVisibility(View.INVISIBLE);
//        }
    }
}

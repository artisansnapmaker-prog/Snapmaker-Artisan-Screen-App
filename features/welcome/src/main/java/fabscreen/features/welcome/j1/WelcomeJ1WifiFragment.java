package fabscreen.features.welcome.j1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.welcome.R;
import fabscreen.features.welcome.R2;
import fabscreen.platform.base.lib.network.AccessPoint;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.common.wifi.adapter.APListAdapter;
import fabscreen.platform.core.ui.common.wifi.adapter.J1APListAdapter;
import fabscreen.platform.core.ui.viewmodel.WifiConnectionViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class WelcomeJ1WifiFragment extends BaseFragment {
    @BindView(R2.id.switch_wifi)
    SwitchCompat mSwitchWifi;
    @BindView(R2.id.rv_ap_list)
    RecyclerView mRvApList;
    @BindView(R2.id.progress_welcome_wifi)
    CircularProgressIndicator mProgress;
    @BindView(R2.id.tv_wifi_sencond_title)
    TextView mTvSecondTitle;
    @BindView(R2.id.tv_welcome_j1_wifi_skip)
    TextView mTvSkip;

    private WifiConnectionViewModel mViewModel;
    private final List<AccessPoint> mApList = new ArrayList<>();

    private final ActivityResultLauncher<String> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initView();
                }
            });

    public static Fragment newInstance() {
        return new WelcomeJ1WifiFragment();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        requestPermission();

    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initView();
        } else {
            requestPermissionsLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void initView() {

        mViewModel.getWifiConnectObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(connect -> {
                    mTvSkip.setText(connect ? R.string.all_next : R.string.all_skip);

                }, LogHelper::log);

        //用户在WiFi页面关了wifi进来这里，这边要强制打开wifi
        boolean wifiEnabled = mViewModel.isWifiEnabled();
        if (!wifiEnabled) {
            mViewModel.switchWifi(true);
        }

        APListAdapter adapter = new J1APListAdapter(mApList);
        mRvApList.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRvApList.setAdapter(adapter);
        adapter.setOnItemClickListener(ap -> {
            playNormalClickSound();
            mViewModel.setSelected(ap);
            if (ap.isEncrypted()) {
                if (requireActivity() instanceof WelcomeJ1Activity) {
                    ((WelcomeJ1Activity) requireActivity()).goToEnterPassword(ap.getSSID());
                }
            } else {
                mViewModel.connect();
                mRvApList.scrollToPosition(0);
            }

        });

        mViewModel.getAPListObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(aps -> {
                    mApList.clear();
                    mApList.addAll(aps);
                    adapter.notifyDataSetChanged();
                }, LogHelper::log);

        mViewModel.getSearchStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(searchState -> {
                    switch (searchState) {
                        case IDLE:
                        case SEARCHING:
                        case SEARCH_DONE_EMPTY:
                            mRvApList.setVisibility(View.GONE);
                            break;
                        case SEARCH_DONE:
                            mProgress.setVisibility(View.GONE);
                            mTvSecondTitle.setVisibility(View.GONE);
                            mRvApList.setVisibility(View.VISIBLE);
                            break;
                    }
                });

    }

    @Override
    protected WifiConnectionViewModel getViewModel() {
        return getViewModelProvider().get(WifiConnectionViewModel.class);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_welcome_j1_wifi;
    }

    @OnClick(R2.id.tv_welcome_j1_wifi_skip)
    void onClickSkip() {
        playNormalClickSound();
        if (!mViewModel.isConnected()) {
            DecisionDialog.create(getActivity())
                    .setDialogStatus(DecisionDialog.BTN_TWO, false, false, false, true)
                    .setContent(R.string.j1_welcome_dailog_skip_wifi_content)
                    .setType(DecisionDialog.TIP_TYPE)
                    .setContentColor(R.color.palette_grey_french)
                    .setFirstTv(R.string.all_cancel, R.color.select_dialog_grey_txt, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setSecondTv(R.string.all_skip, R.color.select_dialog_orange_txt, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ((WelcomeJ1Activity) requireActivity()).goToComplete();
                        }
                    }).show();
        } else {
            ((WelcomeJ1Activity) requireActivity()).goToComplete();
        }
    }

    @OnClick(R2.id.top_bar_back)
    void onClickBack() {
        playNormalClickSound();
        back();
    }
}

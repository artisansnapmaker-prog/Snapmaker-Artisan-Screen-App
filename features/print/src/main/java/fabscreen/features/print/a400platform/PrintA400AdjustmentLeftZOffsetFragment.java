package fabscreen.features.print.a400platform;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.orhanobut.logger.Logger;

import java.math.BigDecimal;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.structure.FDMZOffsetStructure;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.service.machine.structure.ZOffsetInfo;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;
import okio.Buffer;

public class PrintA400AdjustmentLeftZOffsetFragment extends BaseFragment {

    @BindView(R2.id.tv_offset_value)
    TextView mTvValue;
    //    @BindView(R2.id.sbg_z_offset_steps)
//    SegmentedButtonGroup mSbSteps;
    @BindView(fabscreen.platform.core.R2.id.tab_layout)
    TabLayout mTabLayout;
    @BindView(R2.id.tv_print_setting_name)
    TextView mTvSettingName;

    private IMachine mJ1Machine;

    private BehaviorSubject<Float> mZOffsetSubject = BehaviorSubject.createDefault(0f);
    private float mOffsetSize = 0.05f;
    private float[] mOffsetSizes = {0.02f, 0.05f, 0.1f};
    private String[] mTabs;

    public static Fragment newInstance() {
        return new PrintA400AdjustmentLeftZOffsetFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mJ1Machine = ServiceContainer.getInstance().getService(IMachine.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        // get Z Offset
        // 0xa0 0x16 input uint8 key, output uint8 result
        mTvSettingName.setText(R.string.a400_print_print_setting_left_z_offset_title);

        setLinTabValue();
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mOffsetSize = mOffsetSizes[tab.getPosition()];
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        mTabLayout.selectTab(mTabLayout.getTabAt(1));

        mJ1Machine.getFDMController().getZOffset(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    if (responseStructure.isSuccess()) {
                        FDMZOffsetStructure fdmzOffsetStructure = new FDMZOffsetStructure();
                        fdmzOffsetStructure.readBuffer(new Buffer().write(responseStructure.dataProp.toByteArray()));
                        List<ZOffsetInfo> zOffsetInfoList = fdmzOffsetStructure.getZOffsetInfoList();
                        if (zOffsetInfoList == null || zOffsetInfoList.size() == 0) return;
                        ZOffsetInfo zOffsetInfo = zOffsetInfoList.get(0);
                        mZOffsetSubject.onNext(zOffsetInfo.getZOffset());
                    }
                }, LogHelper::log);

        // set Z Offset
        // 0xa0 0x15 input uint8 key, uint8 extruderIndex, float zoffset value, output uint8 result

        mZOffsetSubject.observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(zOffset -> {
                    mTvValue.setText(String.valueOf(zOffset));
                });
    }

    public void setLinTabValue() {
        mTabs = new String[]{getString(R.string.all_0_02mm)
                , getString(R.string.all_0_05mm), getString(R.string.all_0_1mm)};
        if (mTabLayout.getTabCount() > 0) {
            for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                mTabLayout.getTabAt(i).setText(mTabs[i]);
            }
        } else {
            for (int i = 0; i < mTabs.length; i++) {
                mTabLayout.addTab(mTabLayout.newTab().setText(mTabs[i]));
            }
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_print_adjustment_z_offset;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @OnClick(R2.id.btn_z_offset_down)
    void onClickZOffsetDown() {
        playNormalClickSound();
        float value = subtract(mZOffsetSubject.getValue() + "", mOffsetSize + "");
        mJ1Machine.getFDMController().setZOffset(0, 0, value)
                .filter(ResponseStructure::isSuccess)
                .flatMap(responseStructure -> mJ1Machine.getFDMController().getZOffset(0))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    if (responseStructure.isSuccess()) {
                        FDMZOffsetStructure fdmzOffsetStructure = new FDMZOffsetStructure();
                        fdmzOffsetStructure.readBuffer(new Buffer().write(responseStructure.dataProp.toByteArray()));
                        List<ZOffsetInfo> zOffsetInfoList = fdmzOffsetStructure.getZOffsetInfoList();
                        if (zOffsetInfoList == null || zOffsetInfoList.size() == 0) return;
                        ZOffsetInfo zOffsetInfo = zOffsetInfoList.get(0);
                        mZOffsetSubject.onNext(zOffsetInfo.getZOffset());
                        Logger.e("down==value===%f", zOffsetInfo.getZOffset());
                    }
                }, LogHelper::log);
    }

    @OnClick(R2.id.btn_z_offset_up)
    void onClickZOffsetUp() {
        playNormalClickSound();
        float value = add(mZOffsetSubject.getValue() + "", mOffsetSize + "");
        mJ1Machine.getFDMController().setZOffset(0, 0, value)
                .filter(ResponseStructure::isSuccess)
                .flatMap(responseStructure -> mJ1Machine.getFDMController().getZOffset(0))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    if (responseStructure.isSuccess()) {
                        FDMZOffsetStructure fdmzOffsetStructure = new FDMZOffsetStructure();
                        fdmzOffsetStructure.readBuffer(new Buffer().write(responseStructure.dataProp.toByteArray()));
                        List<ZOffsetInfo> zOffsetInfoList = fdmzOffsetStructure.getZOffsetInfoList();
                        if (zOffsetInfoList == null || zOffsetInfoList.size() == 0) return;
                        ZOffsetInfo zOffsetInfo = zOffsetInfoList.get(0);
                        mZOffsetSubject.onNext(zOffsetInfo.getZOffset());
                        Logger.e("up==value===%f", zOffsetInfo.getZOffset());
                    }
                }, LogHelper::log);
    }

    public static float add(String v1, String v2) {

        BigDecimal b1 = new BigDecimal(v1);

        BigDecimal b2 = new BigDecimal(v2);

        return b1.add(b2).floatValue();
    }

    public static float subtract(String v1, String v2) {

        BigDecimal b1 = new BigDecimal(v1);

        BigDecimal b2 = new BigDecimal(v2);

        return b1.subtract(b2).floatValue();
    }
}

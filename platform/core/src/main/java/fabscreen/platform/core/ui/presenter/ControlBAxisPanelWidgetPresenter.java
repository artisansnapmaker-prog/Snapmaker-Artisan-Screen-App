package fabscreen.platform.core.ui.presenter;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import com.google.android.material.tabs.TabLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.service.machine.controller.MachineController;
import fabscreen.platform.base.service.machine.structure.CoordinateSystemInfo;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.core.R;
import fabscreen.platform.core.R2;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;

public class ControlBAxisPanelWidgetPresenter {
    //    @BindView(R2.id.sbg_control_b_axis_steps)
//    SegmentedButtonGroup mSbgBAxisSteps;
    @BindView(fabscreen.platform.core.R2.id.tab_layout)
    TabLayout mTabLayout;
    @BindView(R2.id.btn_widget_b_axis_clockwise)
    Button mBtnBAxisClockwise;
    @BindView(R2.id.btn_widget_b_axis_counterclockwise)
    Button mBtnBAxisCounterClockwise;
    private Context mContext;
    private CompositeDisposable mCompositeDisposable;
    private double mMoveStep = 1;
    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);
    private final MachineController mMachineController;
    private final IMachine mMachine;
    private int mCoordinateType = 0;
    private final MachineInfo mMachineInfo;
    private float[] mOffsetSizes = {0.2f, 1f, 5f, 90f};
    private String[] mTabs;

    public ControlBAxisPanelWidgetPresenter(CompositeDisposable compositeDisposable) {

        mCompositeDisposable = compositeDisposable;
        mMachine = ServiceContainer.getInstance().getService(IMachine.class);
        mMachineInfo = mMachine.getMachineInfoSubjectHolder().getValue();
        mMachineController = mMachine.getMachineController();
        int coordinateSystemIndex = mMachineInfo.workType == IMachine.WorkType.FDM ? 0 : 1;
        mMachineController.updateCoordinateSystem(coordinateSystemIndex);
    }

    private Context getContext() {
        return mContext;
    }

    public Observable<Boolean> getMovingEventObservable() {
        return mMovingEventSubject.hide();
    }

    public void bind(Context context, View view) {
        mContext = context;
        ButterKnife.bind(this, view);
        setLinTabValue();
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mMoveStep = mOffsetSizes[tab.getPosition()];
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        mTabLayout.selectTab(mTabLayout.getTabAt(1));

        Disposable sub = mMovingEventSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(movingEvent -> setEnabled(!movingEvent));
        mCompositeDisposable.add(sub);
    }

    void setLinTabValue() {
        mTabs = new String[]{
                mContext.getString(R.string.all_0_2degree),
                mContext.getString(R.string.all_1degree),
                mContext.getString(R.string.all_5degree),
                mContext.getString(R.string.all_90degree)};
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

    public void connect() {
        mBtnBAxisCounterClockwise.setOnClickListener(v -> {
            mMovingEventSubject.onNext(true);

            Disposable sub = mMachineController.pullCoordinate()
                    .concatMap((Function<ResponseStructure<CoordinateSystemInfo>, ObservableSource<ResponseStructure>>) coordinateSystemInfoResponseStructure -> {
                        Vector coordinate = mMachineController.getCachedCoordinate();
                        Vector b = new Vector();
                        b.setB((float) (coordinate.getB() - mMoveStep));
                        return mMachineController.gotoAbsolutePosition(b, 1800);
                    }).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(res -> {
                        mMovingEventSubject.onNext(false);
                    }, e -> {
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                    });
            mCompositeDisposable.add(sub);
        });

        mBtnBAxisClockwise.setOnClickListener(v -> {
            mMovingEventSubject.onNext(true);

            Disposable sub = mMachineController.pullCoordinate()
                    .concatMap((Function<ResponseStructure<CoordinateSystemInfo>, ObservableSource<ResponseStructure>>) coordinateSystemInfoResponseStructure -> {
                        Vector coordinate = mMachineController.getCachedCoordinate();
                        Vector b = new Vector();
                        b.setB((float) (coordinate.getB() + mMoveStep));
                        return mMachineController.gotoAbsolutePosition(b, 1800);
                    }).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(res -> {
                        mMovingEventSubject.onNext(false);
                    }, e -> {
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                    });
            mCompositeDisposable.add(sub);
        });
    }

    public void setEnabled(boolean enabled) {
        mBtnBAxisClockwise.setEnabled(enabled);
        mBtnBAxisCounterClockwise.setEnabled(enabled);
    }
}

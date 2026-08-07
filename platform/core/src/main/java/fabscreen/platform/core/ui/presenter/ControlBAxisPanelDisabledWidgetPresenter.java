package fabscreen.platform.core.ui.presenter;

import android.content.Context;
import android.view.View;

import com.google.android.material.tabs.TabLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.platform.core.R2;
import fabscreen.platform.core.ui.view.ActionButton;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

// FIXME: Temporary Presenter for disabling B Axis.
public class ControlBAxisPanelDisabledWidgetPresenter extends ControlBAxisPanelWidgetPresenter {
    //    @BindView(R2.id.sbg_control_b_axis_steps)
//    SegmentedButtonGroup mSbgBAxisSteps;
    @BindView(fabscreen.platform.core.R2.id.tab_layout)
    TabLayout mTabLayout;
    @BindView(R2.id.btn_widget_b_axis_clockwise)
    ActionButton mBtnBAxisClockwise;
    @BindView(R2.id.btn_widget_b_axis_counterclockwise)
    ActionButton mBtnBAxisCounterClockwise;
    private CompositeDisposable mCompositeDisposable;
    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);
    private float[] mOffsetSizes = {0.2f, 1f, 5f, 90f};
    private String[] mTabs;
    private double mMoveStep = 1;

    public ControlBAxisPanelDisabledWidgetPresenter(CompositeDisposable compositeDisposable) {
        super(compositeDisposable);
        mCompositeDisposable = compositeDisposable;
    }

    public Observable<Boolean> getMovingEventObservable() {
        return mMovingEventSubject.hide();
    }

    @Override
    public void bind(Context context, View view) {
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
                .subscribe(movingEvent -> {/**/});
        mCompositeDisposable.add(sub);
    }

    @Override
    public void setEnabled(boolean enabled) {
        mTabLayout.setEnabled(false);
        mBtnBAxisClockwise.setEnabled(false);
        mBtnBAxisCounterClockwise.setEnabled(false);
    }
}

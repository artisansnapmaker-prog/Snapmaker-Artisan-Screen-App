package fabscreen.platform.core.ui.presenter;

import static fabscreen.platform.core.ui.data.MoveController.Direction.BACKWARD;
import static fabscreen.platform.core.ui.data.MoveController.Direction.DOWN;
import static fabscreen.platform.core.ui.data.MoveController.Direction.FORWARD;
import static fabscreen.platform.core.ui.data.MoveController.Direction.LEFT;
import static fabscreen.platform.core.ui.data.MoveController.Direction.RIGHT;
import static fabscreen.platform.core.ui.data.MoveController.Direction.UP;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import com.google.android.material.tabs.TabLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.controller.MachineController;
import fabscreen.platform.core.R;
import fabscreen.platform.core.R2;
import fabscreen.platform.core.ui.data.MoveController;
import fabscreen.platform.core.ui.view.A400SteeringView;
import fabscreen.platform.core.ui.view.SteeringView;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class ControlXYZPanelWidgetPresenter {
    //    @BindView(R2.id.sbg_control_steps)
//    SegmentedButtonGroup mSbgControlSteps;
    @BindView(fabscreen.platform.core.R2.id.tab_layout)
    TabLayout mTabLayout;
    @BindView(R2.id.sv_control_panel_xy)
    A400SteeringView mSvControlXY;
    @BindView(R2.id.btn_control_panel_z_plus)
    Button mBtnControlZPlus;
    @BindView(R2.id.btn_control_panel_z_minus)
    Button mBtnControlZMinus;
    private CompositeDisposable mCompositeDisposable;
    private double mMoveStep = 0.1;
    private boolean mDisableXY = false;
    private boolean mDisableZ = false;
    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);
    private MachineController machineController;
    private float[] mOffsetSizes = {0.02f, 0.05f, 0.1f};
    private String[] mTabs;
    private Context mContext;

    public ControlXYZPanelWidgetPresenter(CompositeDisposable compositeDisposable) {
        machineController = ServiceContainer.getInstance().getService(IMachine.class).getMachineController();
        mCompositeDisposable = compositeDisposable;
    }


    public Observable<Boolean> getMovingEventObservable() {
        return mMovingEventSubject.hide();
    }

    public void bind(Context context, View view, int number) {
        mContext = context;
        ButterKnife.bind(this, view);
        if (number == 3) {
            mTabs = new String[]{
                    mContext.getString(R.string.all_0_02mm),
                    mContext.getString(R.string.all_0_05mm),
                    mContext.getString(R.string.all_0_1mm)};
            mOffsetSizes = new float[]{0.1f, 1f, 5f};
        } else {
            mTabs = new String[]{
                    mContext.getString(R.string.all_0_1mm),
                    mContext.getString(R.string.all_1mm),
                    mContext.getString(R.string.all_10mm),
                    mContext.getString(R.string.all_100mm)};
            mOffsetSizes = new float[]{0.1f, 1f, 10f, 100f};
        }
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

        mMoveStep = 1;

        Disposable sub = mMovingEventSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(movingEvent -> setEnabled(!movingEvent));
        mCompositeDisposable.add(sub);
    }


    public void setLinTabValue() {
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
        // x y panel
        mSvControlXY.setOnDirectionClickedListener(direction -> {

            switch (direction) {
                case SteeringView.DIRECTION_UP: {
                    mMovingEventSubject.onNext(true);
                    Disposable sub = MoveController.getInstance().stepToPosition(FORWARD, (float) mMoveStep)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(res -> {
                                mMovingEventSubject.onNext(false);
                            }, e -> {
                                LogHelper.log(e);
                                mMovingEventSubject.onNext(false);
                            });
                    mCompositeDisposable.add(sub);
                    break;
                }
                case SteeringView.DIRECTION_DOWN: {
                    mMovingEventSubject.onNext(true);
                    Disposable sub = MoveController.getInstance().stepToPosition(BACKWARD, (float) mMoveStep)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(res -> {
                                mMovingEventSubject.onNext(false);
                            }, e -> {
                                LogHelper.log(e);
                                mMovingEventSubject.onNext(false);
                            });
                    mCompositeDisposable.add(sub);
                    break;
                }
                case SteeringView.DIRECTION_LEFT: {
                    mMovingEventSubject.onNext(true);
                    Disposable sub = MoveController.getInstance().stepToPosition(LEFT, (float) mMoveStep)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(res -> {
                                mMovingEventSubject.onNext(false);
                            }, e -> {
                                LogHelper.log(e);
                                mMovingEventSubject.onNext(false);
                            });
                    mCompositeDisposable.add(sub);
                    break;
                }
                case SteeringView.DIRECTION_RIGHT: {
                    mMovingEventSubject.onNext(true);
                    Disposable sub = MoveController.getInstance().stepToPosition(RIGHT, (float) mMoveStep)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(res -> {
                                mMovingEventSubject.onNext(false);
                            }, e -> {
                                LogHelper.log(e);
                                mMovingEventSubject.onNext(false);
                            });
                    mCompositeDisposable.add(sub);
                    break;
                }
                default:
                    break;
            }
        });

        // z height button
        mBtnControlZMinus.setOnClickListener(v -> {
            mMovingEventSubject.onNext(true);
            Disposable sub = MoveController.getInstance().stepToPosition(DOWN, (float) mMoveStep)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(res -> {
                        mMovingEventSubject.onNext(false);
                    }, e -> {
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                    });
            mCompositeDisposable.add(sub);
        });
        mBtnControlZPlus.setOnClickListener(v -> {
            mMovingEventSubject.onNext(true);
            Disposable sub = MoveController.getInstance().stepToPosition(UP, (float) mMoveStep)
                    .observeOn(AndroidSchedulers.mainThread())
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
        mBtnControlZPlus.setEnabled(enabled && !mDisableZ);
        mBtnControlZMinus.setEnabled(enabled && !mDisableZ);
        mSvControlXY.setEnabled(enabled && !mDisableXY);
    }

    public void disabledXY() {
        mDisableXY = true;
        mSvControlXY.setEnabled(false);
    }

    public void disabledZ() {
        mDisableZ = true;
        mBtnControlZMinus.setEnabled(false);
        mBtnControlZPlus.setEnabled(false);
    }
}

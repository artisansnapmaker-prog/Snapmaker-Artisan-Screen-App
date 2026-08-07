package fabscreen.features.machinetools.setup.singledual.loadfilament;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.SuperToastHelper;
import fabscreen.platform.base.view.WarmTipDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class LoadFilamentFragment extends BaseFragment {

    private LoadFilamentViewModel mViewModel;

    public static Fragment newInstance() {
        return new LoadFilamentFragment();
    }

    @BindView(R2.id.tv_title)
    TextView mTvTitle;
    @BindView(R2.id.tv_sub_title)
    TextView mTvSubTitle;
    @BindView(R2.id.progress)
    LinearProgressIndicator mProgress;
    @BindView(R2.id.btn_close)
    Button mBtnClose;
    @BindView(R2.id.iv_demonstrate)
    ImageView mIvBlockSetup;
    @BindView(R2.id.tv_demonstrate_desc)
    TextView mTvBlockDesc;
    @BindView(R2.id.btn_start_or_next)
    Button mBtnStartOrNext;
    @BindView(R2.id.iv_guide_problem)
    ImageView mIvGuideProblem;

    private int mCurrentStep = 0;
    private WarmTipDialog mMoveDialog;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
    }

    private void initView() {
        mIvGuideProblem.setVisibility(View.GONE);
        mMoveDialog = WarmTipDialog.create(getActivity());
        mMoveDialog.setOUtSideCanTouch(false);
        mProgress.setMax(2);
        refreshView();
        mBtnClose.setVisibility(View.INVISIBLE);
        mViewModel.getLoadFilamentResultObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::onFilamentLoaded, this::handleError);

        HeatingNozzleDialogFragment heatingDialog = new HeatingNozzleDialogFragment();

        ServiceContainer.getInstance().getService(IMachine.class).getMachineController().getHeatedBed()
                .setAllTargetTemperature(65)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    if (!responseStructure.isSuccess()) {

                    }
                }, LogHelper::log);

        mViewModel.getHeatingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isHeating -> {
                    if (isHeating) {
                        Logger.d("load filament frag, heating...");
//                        showDialog(heatDialog);
                        heatingDialog.show(getChildFragmentManager(), "heating");
                    } else {
                        Logger.d("load filament frag, heating over...");
                        heatingDialog.dismiss();
                    }
                });

        mViewModel.getLoadingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isLoading -> {
                    if (isLoading) {
                        mMoveDialog
                                .setDialogWidthSize(WarmTipDialog.WarmTipDialogSize.SIZE_M)
                                .setTitle(R.string.guide_a400_extruding_the_filament)
                                .setContent(R.string.guide_a400_extruding_the_filament_content)
                                .setPic(R.drawable.ic_a400_load_filamentl_extrusion_materials)
                                .show();
                    } else {
                        mMoveDialog.dismiss();
                    }
                });
        mViewModel.getMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    if (isMoving) {
                        mMoveDialog
                                .setDialogWidthSize(WarmTipDialog.WarmTipDialogSize.SIZE_M)
                                .setProgressVisible(true)
                                .setType(WarmTipDialog.TIP_TYPE)
                                .setTitle(R.string.all_move_show)
                                .setContent(R.string.all_move_show_content)
                                .show();
                    } else {
                        mMoveDialog.dismiss();
                    }
                }, LogHelper::log);
    }

    private void onFilamentLoaded(int index) {
        if (index == 0) {
            DecisionDialog.create(requireContext())
                    .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                    .setPic(R.drawable.pic_a400_success_112x112)
                    .setType(DecisionDialog.NOTIFICATION_TYPE)
                    .setTitle(R.string.guide_a400_nozzle_left_load_confirm_dialog_title)
                    .setContent(R.string.guide_a400_nozzle_left_load_confirm_dialog_content)
                    .setFirstTv(R.string.guide_a400_load_filament, R.color.select_dialog_white_txt, (dialog, which) -> {
                        dialog.dismiss();
                        mViewModel.startExtrudeFilament(index);
                    })
                    .setSecondTv(R.string.all_continue, R.color.select_dialog_green_txt, (dialog, which) -> {
                        dialog.dismiss();
                        mCurrentStep++;
                        refreshView();
                        mViewModel.confirmLoad(index + 1);
                    })
                    .show();

        } else {
            DecisionDialog.create(requireContext())
                    .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                    .setPic(R.drawable.pic_a400_success_112x112)
                    .setType(DecisionDialog.NOTIFICATION_TYPE)
                    .setTitle(R.string.guide_a400_nozzle_right_load_confirm_dialog_title)
                    .setContent(R.string.guide_a400_nozzle_right_load_confirm_dialog_content)
                    .setFirstTv(R.string.guide_a400_load_filament, R.color.select_dialog_white_txt, (dialog, which) -> {
                        dialog.dismiss();
                        mViewModel.startExtrudeFilament(index);
                    })
                    .setSecondTv(R.string.all_done, R.color.select_dialog_green_txt, (dialog, which) -> {
                        dialog.dismiss();
                        mViewModel.confirmLoad(index + 1);
                        ((LoadFilamentActivity) requireActivity()).setResultAndFinish();
                    })
                    .show();
        }
    }

    private void handleError(Throwable e) {
        LogHelper.log(e);
        new SuperToastHelper.Builder()
                .setMessage(getString(R.string.all_failed))
                .build()
                .showToast(requireContext());
    }

    private void refreshView() {
        mProgress.setProgress(mCurrentStep);
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        switch (mCurrentStep) {
            case 0:
                mProgress.setVisibility(View.INVISIBLE);
                mTvSubTitle.setVisibility(View.GONE);
                mBtnClose.setVisibility(View.INVISIBLE);
                mTvTitle.setText(R.string.guide_a400_load_filament);
                mTvBlockDesc.setText(R.string.guide_a400_load_filament_msg);
                mBtnStartOrNext.setText(R.string.all_start);
                mBtnStartOrNext.setVisibility(View.VISIBLE);
                Glide.with(requireContext())
                        .load(R.drawable.pic_initialize_dual_extrusion_module_load_filament_578x434)
                        .apply(options)
                        .into(mIvBlockSetup);
                break;
            case 1:
                mProgress.setVisibility(View.VISIBLE);
                mTvSubTitle.setVisibility(View.VISIBLE);
                mBtnClose.setVisibility(View.VISIBLE);
                mTvTitle.setText(R.string.guide_a400_load_filament);
                mTvSubTitle.setText(R.string.guide_a400_double_extruder_step_1_2_subtitle);
                mTvBlockDesc.setText(R.string.guide_a400_double_extruder_step_1_2_msg);
                mBtnStartOrNext.setVisibility(View.INVISIBLE);
                Glide.with(requireContext())
                        .load(R.drawable.pic_initialize_dual_extrusion_module_load_filament_load_left_nozzle_578x434)
                        .apply(options)
                        .into(mIvBlockSetup);
                break;
            case 2:
                mProgress.setVisibility(View.VISIBLE);
                mTvSubTitle.setVisibility(View.VISIBLE);
                mBtnClose.setVisibility(View.VISIBLE);
                mTvTitle.setText(R.string.guide_a400_load_filament);
                mTvSubTitle.setText(R.string.guide_a400_double_extruder_step_2_2_subtitle);
                mTvBlockDesc.setText(R.string.guide_a400_double_extruder_step_2_2_msg);
                mBtnStartOrNext.setVisibility(View.INVISIBLE);
                Glide.with(requireContext())
                        .load(R.drawable.pic_initialize_dual_extrusion_module_load_filament_load_right_nozzle_578x434)
                        .apply(options)
                        .into(mIvBlockSetup);
                break;
        }
    }

    @OnClick(R2.id.btn_start_or_next)
    void onStartOrNextClicked() {
        playNormalClickSound();
        if (mCurrentStep == 0) {
            mCurrentStep++;
            refreshView();
        }
        mViewModel.reset()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mViewModel.heatExtruders();
                }, LogHelper::log);

    }

    @OnClick(R2.id.btn_close)
    void onClickBack() {
        // resetting status
        mCurrentStep = 0;
        refreshView();

    }

    @Override
    protected void back() {
        super.back();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_guide_setup;
    }

    @Override
    protected LoadFilamentViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(LoadFilamentViewModel.class);
    }
}

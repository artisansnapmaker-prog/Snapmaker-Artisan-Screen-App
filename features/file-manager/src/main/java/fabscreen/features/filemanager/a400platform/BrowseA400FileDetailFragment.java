package fabscreen.features.filemanager.a400platform;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Optional;
import fabscreen.features.filemanager.BrowseFileDetailViewModel;
import fabscreen.features.filemanager.DetailDesc;
import fabscreen.features.filemanager.R;
import fabscreen.features.filemanager.R2;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.SuperToastHelper;
import fabscreen.platform.core.ui.view.FileParsingDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class BrowseA400FileDetailFragment extends BaseFragment implements View.OnTouchListener {

    @BindView(R2.id.tv_browse_j1_file_detail_filename)
    TextView mTvFilename;
    @BindView(R2.id.tv_browse_j1_file_detail_info)
    TextView mTvFileInfo;
    @BindView(R2.id.tv_browse_j1_file_detail_image)
    ImageView mIvFileImage;
    @BindView(R2.id.gl_browse_j1_file_detail_desc)
    GridLayout mGvDetailDesc;
    @BindView(R2.id.btn_browse_j1_file_detail_start)
    Button startBtn;
    @BindView(R2.id.rl_a400_browse_detail_empty)
    RelativeLayout mRlBrowseDetailEmpty;

    @BindView(R2.id.rl_a400_browse_fdm_heated_bed_mode)
    View mViewHeatedBedMode;
    @BindView(R2.id.rl_a400_browse_fdm_heated_bed_mode_select)
    RelativeLayout mRlHeatedBedMode;
    @BindView(R2.id.tv_a400_browse_fdm_heated_bed_mode_select)
    TextView mTvHeatedBedMode;
    @BindView(R2.id.tv_a400_browse_fdm_heated_bed_mode)
    TextView mTvHeatedBedModeTitle;

    protected FileParsingDialog mFabLoading;
    private BrowseFileDetailViewModel mViewModel;
    protected IMachine mMachine;

    private DecisionDialog mDecisionDialog;
    protected PopupWindow mHeatedBedModePopUpWindow;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setOnTouchListener(this);
        mViewModel = getViewModel();
        mViewModel.setFile(((BrowseA400Activity) requireActivity()).getShowFile());
        mMachine = ServiceContainer.getInstance().getService(IMachine.class);
        initView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_browse_print_file_info;
    }

    @Override
    protected BrowseFileDetailViewModel getViewModel() {
        return getViewModelProvider().get(BrowseFileDetailViewModel.class);
    }

    @Override
    protected void back() {
        if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
            mDecisionDialog.dismiss();
        }
        super.back();
    }

    public void initView() {
        // set filename
        startBtn.setText(mMachine.getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM ?
                R.string.all_print : R.string.all_next);

        String filename = mViewModel.getFileName();
        if (!filename.equals("NULL")) {
            mTvFilename.setText(filename);
        }
        mTvFilename.setText(filename);
        if (!mViewModel.getFileInfo().equals("NULL")) {
            mTvFileInfo.setText(mViewModel.getFileInfo());
        } else {
            mTvFileInfo.setText("");
        }

        mFabLoading = FileParsingDialog.create(getContext());
        mFabLoading.setContent(requireContext().getString(R.string.all_browse_copying_to_local));

        Bitmap thumbnail = mViewModel.getGcodeThumbnail();
        if (thumbnail != null) {
            mIvFileImage.setImageBitmap(thumbnail);
        } else if (mViewModel.getBrowseShowFile() != null) {
            Glide.with(requireContext())
                    .load(mViewModel.getBrowseShowFile().getDefaultDisplay())
                    .into(mIvFileImage);
        }

        ArrayList<DetailDesc> showData = mViewModel.getShowData();
        if (showData.isEmpty()) {
            mRlBrowseDetailEmpty.setVisibility(View.VISIBLE);
            mGvDetailDesc.setVisibility(View.INVISIBLE);
        } else {
            for (int i = 0; i < showData.size(); i++) {
                DetailA400DataView detailView = new DetailA400DataView(getContext(), showData.get(i).getDetailDataName(), showData.get(i).getDetailDataValue());
                mGvDetailDesc.addView(detailView.initialize(), updateDetailViewParams(i));
            }
        }
        mDecisionDialog = DecisionDialog.create(getActivity())
                .setType(DecisionDialog.ERROR_TYPE)
                .setDialogStatus(DecisionDialog.BTN_TWO, true, true, true, false)
                .setPic(R.drawable.ic_fail_224x224);

        mViewModel.getUsbStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(hasUsb -> {
                    if (!hasUsb && !((BrowseA400Activity) requireActivity()).getShowFile().getIFile().isLocal()) {
                        back();
                    }
                }, LogHelper::log);

        // Hard code here.
        if (mMachine.getMachineInfoSubjectHolder().getValue().workType != IMachine.WorkType.FDM) {
            mTvHeatedBedModeTitle.setVisibility(TextView.INVISIBLE);
            mRlHeatedBedMode.setVisibility(View.INVISIBLE);
        } else {
            initHeatedBedPopUpModeWindow();
            // Whole Bed for default.
            mViewModel.setHeatedBedMode(1);
            mTvHeatedBedMode.setText(R.string.a400_popup_fdm_heated_bed_mode_whole_bed);
        }
    }

    private void initHeatedBedPopUpModeWindow() {
        View heatedBedModePopup = getLayoutInflater().inflate(R.layout.popup_a400_fdm_heated_bed_mode, (ViewGroup) requireView(), false);
        TextView tvWholeBed = heatedBedModePopup.findViewById(R.id.tv_browse_pop_up_heated_bed_mode_whole_bed);
        TextView tvInnerZone = heatedBedModePopup.findViewById(R.id.tv_browse_pop_up_heated_bed_mode_inner_zone);

        tvWholeBed.setOnClickListener(v -> {
            playNormalClickSound();
            mHeatedBedModePopUpWindow.dismiss();
            mViewModel.setHeatedBedMode(1);
            mTvHeatedBedMode.setText(R.string.a400_popup_fdm_heated_bed_mode_whole_bed);
        });

        tvInnerZone.setOnClickListener(v -> {
            playNormalClickSound();
            mHeatedBedModePopUpWindow.dismiss();
            mViewModel.setHeatedBedMode(0);
            mTvHeatedBedMode.setText(R.string.a400_popup_fdm_heated_bed_mode_inner_zone);
        });

        mHeatedBedModePopUpWindow = new PopupWindow(heatedBedModePopup, DimensUtils.dp2pxInt(240), DimensUtils.dp2pxInt(218));
        mHeatedBedModePopUpWindow.setElevation(8);
    }

    private GridLayout.LayoutParams updateDetailViewParams(int index) {
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
        layoutParams.width = DimensUtils.dp2pxInt(318f);
        layoutParams.rowSpec = GridLayout.spec(index / 2);
        layoutParams.columnSpec = GridLayout.spec(index % 2);
        layoutParams.topMargin = DimensUtils.dp2pxInt(24f);
        layoutParams.setGravity(Gravity.START);
        return layoutParams;
    }

    private void prepareStartPrint() {
        // TODO: Needs to re-thick about prepare procedure.
        //  Normally we prepare files, check if environment or resources is safe for the print before route to next page.
        //  We can sort out which limit/condition should check more clearly by different models or modules,
        //  instead of checking everything and make browse business mixed with prepare business massively.
        if (isJ1()) {
            mRouter.routeToPrintPage().start(getContext());
            back();
        } else {
            mViewModel.checkToolhead()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(pass -> {
                        if (pass) {
                            checkExtruder();
                        } else {
                            showToolheadMismatchDialog();
                        }
                    }, e -> {
                        showToolheadMismatchDialog();
                        LogHelper.log(e);
                    });
        }
    }

    public String getMachineHeadName(int headType) {
        String headName = getString(R.string.all_tool_head_unknown);
        switch (headType) {
            case Module.ModuleType.HEAD_3DP:
                headName = getString(R.string.a400_file_detail_dialog_inconsistent_tool_head_3dp);
                break;
            case Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER:
                headName = getString(R.string.a400_file_detail_dialog_inconsistent_tool_head_3dp_double);
                break;
            case Module.ModuleType.HEAD_CNC:
                headName = getString(R.string.a400_file_detail_dialog_inconsistent_tool_head_cnc);
                break;
            case Module.ModuleType.HEAD_CNC_200W:
                headName = getString(R.string.a400_file_detail_dialog_inconsistent_tool_head_cnc_200);
                break;
            case Module.ModuleType.HEAD_LASER:
                headName = getString(R.string.a400_file_detail_dialog_inconsistent_tool_head_laser);
                break;
            case Module.ModuleType.HEAD_LASER_10W:
                headName = getString(R.string.a400_file_detail_dialog_inconsistent_tool_head_laser_10);
                break;
            case Module.ModuleType.HEAD_LASER_20W:
                headName = getString(R.string.a400_file_detail_dialog_inconsistent_tool_head_laser_20);
                break;
            case Module.ModuleType.HEAD_LASER_40W:
                headName = getString(R.string.a400_file_detail_dialog_inconsistent_tool_head_laser_40);
                break;
            case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                headName = getString(R.string.a400_file_detail_dialog_inconsistent_tool_head_laser_2_infrared);
                break;
        }
        return headName;
    }

    private boolean isJ1() {
        MachineInfo machineInfo = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue();
        return machineInfo.seriesId == IMachine.MachineSeries.J && machineInfo.modelId == IMachine.MachineModel.J1;
    }

    private void checkExtruder() {
        mViewModel.checkExtruder()
                .flatMap(isPass -> {
                    if (isPass) {
                        return mViewModel.setFDMHeatedBedWorkMode(mViewModel.getHeatedBedMode())
                                .flatMap(response -> Observable.just(true));
                    } else {
                        return Observable.just(false);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(pass -> {
                    if (pass) {
                        checkDualExtruderRetractionLimit();
                    } else {
                        showExtruderMismatchDialog();
                    }
                }, e -> {
                    showExtruderMismatchDialog();
                    LogHelper.log(e);
                });
    }

    private void checkCNCLockingBlock() {
        mViewModel.checkLockingBlockOrigin()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        mRouter.routeToPrintPage().start(getContext());
                        back();
                    } else {
                        new SuperToastHelper.Builder()
                                .setMessage("load CNC Locking Block data error")
                                .build()
                                .showToast(requireContext());
                    }
                });
    }

    private void checkDualExtruderRetractionLimit() {
        mViewModel.checkFileExtruderRetractionDistance()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(pass -> {
                    if (pass) {
                        if (mMachine.getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM) {
                            showFDMCleanUpConfirmDialog();
                        } else if (mMachine.getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.CNC) {
                            checkCNCLockingBlock();
                        } else {
                            mRouter.routeToPrintPage().start(getContext());
                            back();
                        }
                    } else {
                        Logger.d("Retraction Distance exceed in G-code was detected.");
                        showFDMExtruderRetractionLimitDialog();
                    }
                }, e -> {
                    showFDMExtruderRetractionLimitDialog();
                    LogHelper.log(e);
                });
    }

    private void showFDMCleanUpConfirmDialog() {
        DecisionDialog.create(requireContext())
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.ic_a400_clean_up_112x112)
                .setTitle(getString(R.string.a400_file_manager_a400_procedure_start_confirm_dialog_title,
                        getString(R.string.all_print)))
                .setContent(R.string.a400_file_manager_a400_procedure_start_confirm_dialog_content_3dp)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setSecondTv(R.string.all_next, R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    mRouter.routeToPrintPage().start(getContext());
                    back();
                })
                .show();
    }

    private void showToolheadMismatchDialog() {
        String tipStr = getString(R.string.a400_file_detail_dialog_inconsistent_tool_head_tip,
                mViewModel.getGcodeInfo().getHeaderNameID() != -1 ? getString(mViewModel.getGcodeInfo().getHeaderNameID()) : getString(R.string.all_tool_head_unknown),
                getMachineHeadName(mMachine.getMachineInfoSubjectHolder().getValue().headType));

        mDecisionDialog.setWarmTv(tipStr, R.color.palette_red_monza);
        mDecisionDialog.setTitle(R.string.a400_file_detail_dialog_inconsistent_tool_head_title)
                .setContent(R.string.a400_file_detail_dialog_inconsistent_tool_head_message)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_left_text_color,
                        (dialog, which) -> dialog.dismiss())
                .setSecondTv(R.string.all_continue, R.color.select_dialog_red_txt,
                        (dialog, which) -> {
                            dialog.dismiss();
                            checkExtruder();
                        }).show();
    }

    private void showExtruderMismatchDialog() {
        String nozzleDiameterTip = getString(R.string.a400_file_detail_dialog_inconsistent_nozzle_diameter_warm_tip_file) +
                (mViewModel.getGcodeInfo().getNozzle_0_Diameter() == -1 ? "" : getString(R.string.a400_file_detail_dialog_inconsistent_nozzle_diameter_warm_tip_left, mViewModel.getGcodeInfo().getNozzle_0_Diameter() + "")) + " " +
                (mViewModel.getGcodeInfo().getNozzle_1_Diameter() == -1 ? "" : getString(R.string.a400_file_detail_dialog_inconsistent_nozzle_diameter_warm_tip_right, mViewModel.getGcodeInfo().getNozzle_1_Diameter() + "")) +
                "\n" +
                getString(R.string.a400_file_detail_dialog_inconsistent_nozzle_diameter_warm_tip_machine) +
                getString(R.string.a400_file_detail_dialog_inconsistent_nozzle_diameter_warm_tip_left, mMachine.getFDMController().getToolheadStatusSubjectHolder(0).getValue().getExtruderList().get(0).getDiameter() + "") + " " +
                getString(R.string.a400_file_detail_dialog_inconsistent_nozzle_diameter_warm_tip_right, mMachine.getFDMController().getToolheadStatusSubjectHolder(0).getValue().getExtruderList().get(1).getDiameter() + "");

        mDecisionDialog.setTitle(R.string.a400_file_detail_dialog_inconsistent_nozzle_diameter_title)
                .setWarmTv(nozzleDiameterTip, R.color.palette_red_monza)
                .setContent(R.string.a400_file_detail_dialog_inconsistent_nozzle_diameter_message)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_left_text_color, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setSecondTv(R.string.all_continue, R.color.select_dialog_red_txt, (dialog, which) -> {
                    dialog.dismiss();
                    checkDualExtruderRetractionLimit();
                }).show();
    }

    private void showFDMExtruderRetractionLimitDialog() {
        String detectedRetractionTip = "";
        if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
            mDecisionDialog.dismiss();
        }

        mDecisionDialog = DecisionDialog.create(getActivity())
                .setType(DecisionDialog.ERROR_TYPE)
                .setDialogStatus(DecisionDialog.BTN_TWO, true, true, true, false)
                .setPic(R.drawable.ic_fail_224x224);
        mDecisionDialog.setTitle(R.string.a400_file_detail_dialog_inappropriate_retraction_distance_title)
                .setContent(R.string.a400_file_detail_dialog_inappropriate_retraction_distance_message)
                .setFirstTv(R.string.all_cancel, R.color.select_dialog_left_text_color, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setSecondTv(R.string.all_continue, R.color.select_dialog_red_txt, (dialog, which) -> {
                    dialog.dismiss();
                    Logger.i("Continue to print.");
                    // Start routing to print module, prepare for print job.
                    showFDMCleanUpConfirmDialog();
                }).show();
    }

    private void showHeatedBedWindowPopup() {
        if (mHeatedBedModePopUpWindow.isShowing()) {
            mHeatedBedModePopUpWindow.dismiss();
        } else {
            mHeatedBedModePopUpWindow.setFocusable(true);
            mHeatedBedModePopUpWindow.setTouchable(true);
            mHeatedBedModePopUpWindow.setOutsideTouchable(true);
            mHeatedBedModePopUpWindow.showAsDropDown(mViewHeatedBedMode, DimensUtils.dp2pxInt(240), DimensUtils.dp2pxInt(-15));
        }
    }

    @Optional
    @OnClick({R2.id.tv_browse_j1_file_detail_bg})
    public void onTouchOutside() {
        back();
    }

    @Optional
    @OnClick({R2.id.btn_browse_j1_file_detail_cancel})
    public void onClickBack() {
        playNormalClickSound();
        back();
    }

    @OnClick(R2.id.btn_browse_j1_file_detail_start)
    void onClickStart() {
        playNormalClickSound();
        if (!mFabLoading.isShowing()) mFabLoading.show();
        mViewModel.handleResult()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mFabLoading.dismiss();
                    if (success) {
                        // check if toolhead and extruder match
                        prepareStartPrint();
                    } else {
                        new SuperToastHelper.Builder()
                                .setDrawable(R.drawable.ic_pic_a400_error_68x68)
                                .setTitle(getString(R.string.a400_browse_file) + mViewModel.getFileName())
                                .setMessage(getString(R.string.a400_browse_copy_error))
                                .build()
                                .showToast(requireContext());
                        back();
                    }
                }, e -> {
                    LogHelper.log(e);
                    mFabLoading.dismiss();
                    new SuperToastHelper.Builder()
                            .setDrawable(R.drawable.ic_pic_a400_error_68x68)
                            .setTitle(getString(R.string.a400_browse_file) + mViewModel.getFileName())
                            .setMessage(getString(R.string.a400_browse_copy_error))
                            .build()
                            .showToast(requireContext());
                    back();
                    // TODO: 2022/7/23  Toast.makeText(getContext(), "拷贝失败,e:" + e.toString(), Toast.LENGTH_LONG).show();
                });
    }

    @OnClick({R2.id.rl_a400_browse_fdm_heated_bed_mode_select})
    void onClickModeSelect() {
        playNormalClickSound();
        showHeatedBedWindowPopup();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return true;
    }
}

package fabscreen.features.print.a400platform;

import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_PRINTING;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.features.print.a400platform.viewmodel.A400PrintViewModel;
import fabscreen.platform.base.helper.StringToValueUtils;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.parser.IGcodeParser;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.controller.PrintEvent;
import fabscreen.platform.base.service.machine.entity.parts.Extruder;
import fabscreen.platform.base.service.machine.entity.parts.LaserTube;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.data.FilamentState;
import fabscreen.platform.core.ui.view.FileParsingDialog;
import fabscreen.platform.core.ui.view.PrintDetailCard;
import fabscreen.platform.core.ui.view.StepIntroductionDialog;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class PrintA400Fragment extends BaseFragment {
    @BindView(R2.id.iv_print_file_diagram)
    ImageView mIvPrintFileDiagram;
    @BindView(R2.id.iv_print_base_show)
    ImageView mIvPrintBaseShow;
    @BindView(R2.id.tv_a400_print_filename)
    TextView mTvFilename;
    @BindView(R2.id.tv_print_remaining_time)
    TextView mTvRemainingTime;
    @BindView(R2.id.tv_a400_print_progress)
    TextView mTvPrintProgress;
    @BindView(R2.id.pb_print_progress)
    CircularProgressIndicator mPvPrintProgress;
    @BindView(R2.id.btn_a400_print_pause)
    ImageView mBtnPause;
    @BindView(R2.id.btn_a400_print_resume)
    ImageView mBtnResume;
    @BindView(R2.id.btn_a400_print_stop)
    ImageView mBtnStop;
    @BindView(R2.id.li_print_detail)
    LinearLayout mLiPrintDetail;

    // Views
    private StepIntroductionDialog mCheckFilamentProcessTipDialog;
    public FileParsingDialog mFabWorkingChangeDialog;
    DecisionDialog mDecisionDialog;
    private DecisionDialog mIsShowFilamentDialog;
    private CustomKeyboardUtil mCustomKeyboardUtil;

    // Interfaces
    private IRouter mRouter;
    private IPrintWorkspace mWorkspace;

    // ViewModel
    private A400PrintViewModel mViewModel;

    // Parameters
    private boolean isDoubleExtruder = false;
    private boolean isStateFail = false;
    private boolean isStop;

    // Disposables
    Disposable mDisCheckFilamentProcess;
    Disposable mDisCheckoutExtruderTemperature;
    Disposable mPrintControllerCallbackSub;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRouter = ServiceContainer.getInstance().getService(IRouter.class);
        mWorkspace = ServiceContainer.getInstance().getService(IPrintWorkspace.class);
        mViewModel = getFragmentScopeViewModel(A400PrintViewModel.class);
        initView();
        initPrint();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_print;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPrintControllerCallback();
        mViewModel.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mViewModel.onPause();
    }

    @OnClick({R2.id.iv_print_back_home, R2.id.tv_print_back_home})
    @Override
    protected void back() {
        playSwitchSound();
        // Exit print page but not finishing the print job.
        mRouter.routeToHome().start(requireActivity());
        requireActivity().finish();
    }

    private void initView() {
        mCustomKeyboardUtil = new CustomKeyboardUtil(requireContext());
        mFabWorkingChangeDialog = FileParsingDialog.create(getActivity());

        mIvPrintFileDiagram.setImageBitmap(ServiceContainer.getInstance().getService(IGcodeParser.class).getGcodeThumbnail());

        // Initialize print detail view.
        GradientDrawable drawable = new GradientDrawable();
        GradientDrawable dra = new GradientDrawable();
        int width = 24;
        drawable.setSize(12, 1);
        mLiPrintDetail.setDividerDrawable(drawable);
        mLiPrintDetail.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);

        dra.setSize(width, 1);
        mLiPrintDetail.setDividerDrawable(dra);
        mLiPrintDetail.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);

        switch (mViewModel.getWorkType()) {
            case FDM:
                mIvPrintBaseShow.setBackgroundResource(R.drawable.pic_a400_print_base_show_fdm);
                init3DPPanel();
                break;
            case LASER:
                mIvPrintBaseShow.setBackgroundResource(R.drawable.pic_a400_print_base_show_laser);
                initLaserPanel();
                break;
            case CNC:
                mIvPrintBaseShow.setBackgroundResource(R.drawable.pic_a400_print_base_show_cnc);
                initCNCPanel();
                break;
            default:
                break;
        }
        // Initialize file and progress data.
        mTvFilename.setText(mWorkspace.getFileName());
        mTvFilename.setSelected(true);
        mTvRemainingTime.setText(getString(R.string.a400_print_remaining_time_desc) + formatTime(mWorkspace.getEstimatedTime()));
    }

    public float getInputValue(float value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public int getInputValue(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    void initLaserPanel() {
        // Init Laser Power Card.
        PrintDetailCard laserCard = new PrintDetailCard(getContext())
                .setIcon(R.drawable.icon_laser_gray_128x128)
                .setDetailsName(R.string.print_laser_power);
        laserCard.setClickable(true);
        mCustomKeyboardUtil.bindKeyboardListener(laserCard, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    float value = getInputValue(Float.parseFloat(s.toString()), 0, 100);
                    mViewModel.setLaserPower(0, value)
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(responseStructure -> {
                            }, LogHelper::log);

                }

            }
        });

        laserCard.setOnClickListener(v -> {
            mCustomKeyboardUtil.setPreInputText(String.valueOf(laserCard.getDetailsPercentValue()));
            mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
            mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
        });
        // Subscribe laser events.
        mViewModel.getLaserToolHeadInfoObservable(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(laserToolheadInfo -> {
                    LaserTube laserTube = laserToolheadInfo.getLaserTube();
                    laserCard.setDetailsPercentValue((int) laserTube.getCurrentPower());
                    laserCard.setProgressValue((int) (laserTube.getCurrentPower() / 100f * 100));
                }, LogHelper::log);

        mLiPrintDetail.addView(laserCard);

        // Init Laser Work Speed card.
        PrintDetailCard workSpeedCard = new PrintDetailCard(getContext())
                .setIcon(R.drawable.icon_work_speed_gray_64x64)
                .setDetailsName(R.string.print_work_speed);
        workSpeedCard.setClickable(true);
        mCustomKeyboardUtil.bindKeyboardListener(workSpeedCard, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    int workSpeed = getInputValue(StringToValueUtils.parseInt(s.toString()), 10, 500);
                    mViewModel.setPrintWorkSpeed(IMachine.WorkType.LASER, 0, 0, workSpeed)
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(responseStructure -> {
                            }, LogHelper::log);
                }
            }
        });

        workSpeedCard.setOnClickListener(v -> {
            mCustomKeyboardUtil.setPreInputText(String.valueOf(workSpeedCard.getDetailsPercentValue()));
            mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
            mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_NUMBER);
        });        // Subscribe laser work speed data.
        mViewModel.getTookHeadSpeedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(workSpeedList -> {
                    if (workSpeedList.isEmpty()) return;
                    workSpeedCard.setDetailsPercentValue(workSpeedList.get(0));
                    workSpeedCard.setProgressValue((int) (workSpeedList.get(0) / 500f * 100));
                }, LogHelper::log);
        mLiPrintDetail.addView(workSpeedCard);
    }

    void initCNCPanel() {
        // Init CNC Spindle Speed card.
        PrintDetailCard cncCard = new PrintDetailCard(getContext())
                .setIcon(R.drawable.icon_spindle_speed_gray_64x64)
                .setDetailsName(R.string.print_cnc);
        cncCard.setClickable(true);
        // Subscribe CNC spindle speed.
        mViewModel.getCncToolHeadInfoObservable(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(cncToolheadInfo -> {
                    if (mViewModel.is200WattCNC()) {
                        cncCard.setDetailsSingleValue((int) cncToolheadInfo.getCurrentSpeed());
                        cncCard.setProgressValue((int) (cncToolheadInfo.getCurrentSpeed() / 18000f * 100));
                    } else {
                        cncCard.setDetailsPercentValue((int) cncToolheadInfo.getCurrentPower());
                        cncCard.setProgressValue((int) (cncToolheadInfo.getCurrentPower() / 100f * 100));
                    }
                }, LogHelper::log);

        mCustomKeyboardUtil.bindKeyboardListener(cncCard, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s)) {
                    int value = getInputValue(StringToValueUtils.parseInt(s.toString()), 0, mViewModel.is200WattCNC() ? 18000 : 100);
                    mViewModel.setCNCTarget(0, value)
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(responseStructure -> {
                            }, LogHelper::log);
                }


            }
        });
        cncCard.setOnClickListener(v -> {
            mCustomKeyboardUtil.setPreInputText(String.valueOf(cncCard.getDetailsPercentValue()));
            mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
            mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_NUMBER);
        });
        mLiPrintDetail.addView(cncCard);

        // Init CNC Work Speed Card.
        PrintDetailCard workSpeedCard = new PrintDetailCard(getContext())
                .setIcon(R.drawable.icon_work_speed_gray_64x64)
                .setDetailsName(R.string.print_work_speed);
        workSpeedCard.setClickable(true);
        mCustomKeyboardUtil.bindKeyboardListener(workSpeedCard, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    int workSpeed = getInputValue(StringToValueUtils.parseInt(s.toString()), 10, 500);
                    mViewModel.setPrintWorkSpeed(IMachine.WorkType.CNC, 0, 0, workSpeed)
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(responseStructure -> {
                            }, LogHelper::log);
                }

            }
        });
        workSpeedCard.setOnClickListener(v -> {
            mCustomKeyboardUtil.setPreInputText(String.valueOf(workSpeedCard.getDetailsPercentValue()));
            mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
            mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_NUMBER);
        });
        mLiPrintDetail.addView(workSpeedCard);

        // Subscribe CNC work speed.
        mViewModel.getTookHeadSpeedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(workSpeedList -> {
                    if (workSpeedList.isEmpty()) return;
                    workSpeedCard.setDetailsPercentValue(workSpeedList.get(0));
                    workSpeedCard.setProgressValue((int) (workSpeedList.get(0) / 500f * 100));
                }, LogHelper::log);
    }

    void init3DPPanel() {
        isDoubleExtruder = mViewModel.isDoubleExtruder();

        // Init FDM Left Extruder (as No.0 Extruder) Card.
        PrintDetailCard ExtruderLeftCard = new PrintDetailCard(getContext())
                .setIcon(R.drawable.icon_nozzle_left_gray_64x64)
                .setDetailsName(isDoubleExtruder ? R.string.all_left_nozzle_temp_abbr : R.string.all_nozzle_temp_abbr);
        ExtruderLeftCard.setClickable(true);
        mCustomKeyboardUtil.bindKeyboardListener(ExtruderLeftCard, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    int temperature = getInputValue(StringToValueUtils.parseInt(s.toString()), 0, isDoubleExtruder ? 300 : 275);
                    mViewModel.setExtruderTemperature(0, 0, temperature)
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(responseStructure -> {
                            }, LogHelper::log);
                }
            }
        });
        ExtruderLeftCard.setOnClickListener(v -> {
            mCustomKeyboardUtil.setPreInputText(String.valueOf(ExtruderLeftCard.getDetailsTargetValue()));
            mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
            mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_NUMBER);
        });
        mLiPrintDetail.addView(ExtruderLeftCard);

        PrintDetailCard ExtruderRightCard = null;
        if (isDoubleExtruder) {
            ExtruderRightCard = new PrintDetailCard(getContext())
                    .setIcon(R.drawable.icon_nozzle_right_gray_64x64)
                    .setDetailsName(R.string.all_right_nozzle_temp_abbr);
            mViewModel.setFilamentState(mViewModel.getFilamentStateValue().setExtruderNum(2));
        } else {
            mViewModel.setFilamentState(mViewModel.getFilamentStateValue().setExtruderNum(1));
        }

        PrintDetailCard finalExtruderRightCard = ExtruderRightCard;
        // Init FDM Right Extruder (as No.1 Extruder) Card.
        // Only Init when right extruder exists(as it works with FDM Dual Extruder).
        if (finalExtruderRightCard != null) {
            finalExtruderRightCard.setClickable(true);

            mCustomKeyboardUtil.bindKeyboardListener(finalExtruderRightCard, new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!TextUtils.isEmpty(s.toString())) {
                        int temperature = getInputValue(StringToValueUtils.parseInt(s.toString()), 0, isDoubleExtruder ? 300 : 275);
                        mViewModel.setExtruderTemperature(0, 1, temperature)
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(responseStructure -> {
                                }, LogHelper::log);
                    }
                }
            });
            finalExtruderRightCard.setOnClickListener(v -> {
                mCustomKeyboardUtil.setPreInputText(String.valueOf(finalExtruderRightCard.getDetailsTargetValue()));
                mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
                mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_NUMBER);
            });
            mLiPrintDetail.addView(finalExtruderRightCard);
        }

        // Subscribe FDM extruder data, including extruder(s) temperature info, filament sensor state.
        mViewModel.getToolheadStatusSubjectHolder(0)
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(fdmToolheadStatus -> {
                    Extruder leftExtruder = fdmToolheadStatus.getExtruderList().get(0);
                    float leftExtruderTemp = leftExtruder.getTemperature();
                    float leftExtruderTargetTemp = leftExtruder.getTargetTemperature();
                    boolean leftExtruderFilamentStatus = leftExtruder.getFilamentStatus();
                    boolean hasMultipleExtruders = fdmToolheadStatus.getExtruderList().size() > 1;
                    ExtruderLeftCard.setDetailsCurrentValue((int) leftExtruderTemp);
                    ExtruderLeftCard.setDetailsTargetValue((int) leftExtruderTargetTemp);
                    ExtruderLeftCard.setProgressValue((int) (leftExtruderTemp / (hasMultipleExtruders ? 300f : 275f) * 100));

                    mViewModel.setFilamentState(mViewModel.getFilamentStateValue().setFilamentState(0,
                            leftExtruderFilamentStatus,
                            leftExtruderTargetTemp,
                            leftExtruderTargetTemp != 0 && leftExtruderTargetTemp - 5 <= leftExtruderTemp,
                            leftExtruder.getState() == 1));

                    // If FDM ToolHead has two or more extruders
                    if (hasMultipleExtruders) {
                        Extruder rightExtruder = fdmToolheadStatus.getExtruderList().get(1);
                        float rightExtruderTemp = rightExtruder.getTemperature();
                        float rightExtruderTargetTemp = rightExtruder.getTargetTemperature();
                        boolean rightExtruderFilamentStatus = rightExtruder.getFilamentStatus();
                        finalExtruderRightCard.setDetailsCurrentValue((int) rightExtruderTemp);
                        finalExtruderRightCard.setDetailsTargetValue((int) rightExtruderTargetTemp);
                        finalExtruderRightCard.setProgressValue((int) (rightExtruderTemp / (fdmToolheadStatus.getExtruderList().size() > 1 ? 300f : 275f) * 100));

                        mViewModel.setFilamentState(mViewModel.getFilamentStateValue().setFilamentState(1,
                                rightExtruderFilamentStatus,
                                rightExtruderTargetTemp,
                                rightExtruderTargetTemp != 0 && rightExtruderTargetTemp - 5 <= rightExtruderTemp,
                                rightExtruder.getState() == 1
                        ));
                    }
                }, LogHelper::log);

        PrintDetailCard extruderBedCard = new PrintDetailCard(getContext())
                .setIcon(R.drawable.icon_heated_bed_gray_64x64)
                .setDetailsName(R.string.print_heated_bed_temp);
        extruderBedCard.setClickable(true);

        mCustomKeyboardUtil.bindKeyboardListener(extruderBedCard, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    int temperature = getInputValue(StringToValueUtils.parseInt(s.toString()), 0, 110);
                    mViewModel.setDefaultModeTargetTemperature(temperature)
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(responseStructure -> {
                            }, LogHelper::log);
                }

            }
        });
        extruderBedCard.setOnClickListener(v -> {
            mCustomKeyboardUtil.setPreInputText(String.valueOf(extruderBedCard.getDetailsTargetValue()));
            mCustomKeyboardUtil.showKeyboard(v, CustomKeyboardUtil.INPUT_TYPE_NUMBER_DECIMAL);
            mCustomKeyboardUtil.setNumberInputType(InputType.TYPE_CLASS_NUMBER);
        });
        mViewModel.getHeatedBedStatusSubjectHolder()
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(heatedBedStatus -> {
                    extruderBedCard.setDetailsCurrentValue((int) heatedBedStatus.getZoneList().get(0).getCurrentTemperature());
                    extruderBedCard.setDetailsTargetValue(heatedBedStatus.getZoneList().get(0).getTargetTemperature());
                    extruderBedCard.setProgressValue((int) ((heatedBedStatus.getZoneList().get(0).getCurrentTemperature() / 110f) * 100));
                }, LogHelper::log);
        mLiPrintDetail.addView(extruderBedCard);

        // Material breaking occurs
        mViewModel.getFilamentSubjectObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(triggered -> {
                    if (triggered) {
                        if (mIsShowFilamentDialog != null && mIsShowFilamentDialog.isShowing())
                            return;
                        mIsShowFilamentDialog = DecisionDialog.create(getContext())
                                .setTitle(R.string.a400_print_dialog_warning_filament_run_out_title)
                                .setType(DecisionDialog.WARMING_TYPE)
                                .setContent(getString(R.string.a400_print_dialog_warning_filament_run_out_triggered_content,
                                        mViewModel.getFilamentStateValue().getFailureFilamentIndex() == 0 ?
                                                getString(R.string.all_left_extruder_abbr) :
                                                getString(R.string.all_right_extruder_abbr)))
                                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, true)
                                .setPic(R.drawable.pic_a400_warning_112x112)
                                .setFirstTv(getString(R.string.all_cancel), R.color.select_dialog_white_txt, ((dialog, which) -> {
                                    mViewModel.setFilament(true);
                                    dialog.dismiss();
                                }))
                                .setSecondTv(getString(R.string.a400_print_dialog_warning_filament_run_out_action_load_filament), R.color.select_dialog_yellow_txt, (dialog, which) -> {
                                    dialog.dismiss();
                                    enterFilamentRefillProcedure();
                                });
                        mIsShowFilamentDialog.show();
                    }
                });
    }

    void initPrint() {
        mViewModel.initPrint();

        mBtnStop.setVisibility(Button.VISIBLE);
        showButtonByPrintState(mViewModel.getPrintStateValue());

        mViewModel.getPrintStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::showButtonByPrintState, LogHelper::log);

        // enable or disable buttons
        mViewModel.getWaitingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(waiting -> {
                    mBtnPause.setEnabled(!waiting);
                    mBtnResume.setEnabled(!waiting);
                    mBtnStop.setEnabled(!waiting);
                });

        mViewModel.getEnclosureSubjectObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(triggered -> {
                    if (triggered) {
                        DecisionDialog.create(getContext())
                                .setCanceledOnTouchOutSide(false)
                                .setType(DecisionDialog.WARMING_TYPE)
                                .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                                .setContent(getString(R.string.a400_dialog_print_enclosure_open_format_desc, getString(R.string.a400_dialog_print_enclosure_dialog_default_job)))
                                .setContentColor(R.color.palette_white_pure)
                                .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> {
                                    mViewModel.setEnclosure(true);
                                    dialog.dismiss();
                                }))
                                .show();
                    }
                });

        mViewModel.getUpdateProgressObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(printProgress -> {
                    mTvRemainingTime.setText(getString(R.string.a400_print_remaining_time_desc) + printProgress.formatTime(requireContext()));
                    mTvPrintProgress.setText(printProgress.percentage + "%");
                    mPvPrintProgress.setProgress(printProgress.percentage);
                });
    }

    private void showButtonByPrintState(int status) {
        mBtnPause.setVisibility(SYSTEM_STATUS_PRINTING.valueEquals(status) ? Button.VISIBLE : Button.INVISIBLE);
        mBtnResume.setVisibility(!SYSTEM_STATUS_PRINTING.valueEquals(status) ? Button.VISIBLE : Button.INVISIBLE);
    }


    private void getPrintControllerCallback() {
        Disposable subscribe = mViewModel.getPrintEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::onPrintControllerCallback, LogHelper::log);

        if (!subscribe.isDisposed()) {
            if (mPrintControllerCallbackSub != null && !mPrintControllerCallbackSub.isDisposed()) {
                mPrintControllerCallbackSub.dispose();
            }
            mPrintControllerCallbackSub = subscribe;
        }
    }

    private void onPrintControllerCallback(PrintEvent printEvent) {
        DecisionDialog decisionDialog = DecisionDialog.create(getContext());
        int retCode = printEvent.getErrorCode();
        // TODO: Split out into method.
        switch (printEvent.getPrintEventState()) {
            case STATE_SUCCESS:
                // oh we started
                Logger.i("Print started.");
                isStateFail = false;
                break;
            case START_FAIL:
                Logger.w("Unable to start printing, ret code %d", retCode);
                isStateFail = true;
                if (retCode == 17) return;
                if (retCode == 222) {
                    mViewModel.setFilament(false);
                } else {
                    decisionDialog = DecisionDialog.create(getContext())
                            .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                            .setContent(retCode == 227 ? getString(R.string.a400_print_enclosure_operation_trigger) : (getString(R.string.print_warning_start_unable) + "\nretCode:" + retCode))
                            .setContentColor(R.color.palette_white_pure)
                            .setType(DecisionDialog.WARMING_TYPE)
                            .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                    decisionDialog.show();
                }
                break;
            case PAUSE_SUCCESS:
                Logger.i("Print paused.");
                if (mFabWorkingChangeDialog != null && mFabWorkingChangeDialog.isShowing()) {
                    mFabWorkingChangeDialog.dismiss();
                }
                break;
            case PAUSE_FAIL:
                Logger.w("Unable to pause printing, ret code %d", retCode);
                if (mFabWorkingChangeDialog != null && mFabWorkingChangeDialog.isShowing()) {
                    mFabWorkingChangeDialog.dismiss();
                }

                if (retCode == 17) return;
                // Just a confirm
                decisionDialog = DecisionDialog.create(getContext())
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContentColor(R.color.palette_white_pure)
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                switch (retCode) {
                    case 222: {
                        mViewModel.setFilament(false);
                        if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
                            mDecisionDialog.dismiss();
                        }
                        return;
                    }
                    case 227:
                        decisionDialog.setContent(getString(R.string.a400_print_enclosure_operation_trigger));
                        break;
                    default:
                        decisionDialog.setContent(getString(R.string.print_warning_pause_unable) + "\nretCode:" + retCode);
                        break;
                }
                decisionDialog.show();
                break;
            case RESUME_SUCCESS:
                Logger.i("Print resumed.");
                break;
            case RESUME_FAIL:
                Logger.w("Unable to resume printing, ret code %d", retCode);
                if (retCode == 17) return;
                decisionDialog = DecisionDialog.create(getContext())
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContentColor(R.color.palette_white_pure)
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                switch (retCode) {
                    case 222: {
                        mViewModel.setFilament(false);
                        if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
                            mDecisionDialog.dismiss();
                        }
                        return;
                    }
                    case 227: {
                        decisionDialog.setContent(getString(R.string.a400_print_enclosure_operation_trigger));
                        break;
                    }
                    default: {
                        decisionDialog.setContent(getString(R.string.print_warning_resume_unable) + "\nretCode:" + retCode);
                        break;
                    }
                }
                decisionDialog.show();
                break;
            case POWER_LOSS_RESUME_SUCCESS:
                Logger.i("Print recovered.");
                // we resumed from power outage
                mViewModel.setPowerOutageFlag(false);
                break;
            case POWER_LOSS_RESUME_FAIL:
                Logger.w("Failed to recover from power loss, ret code %d", retCode);
                if (retCode == 17) return;
                decisionDialog = DecisionDialog.create(getContext())
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContentColor(R.color.palette_white_pure)
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                switch (retCode) {
                    case 222: {
                        mViewModel.setFilament(false);
                        if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
                            mDecisionDialog.dismiss();
                        }
                        return;
                    }
                    case 227: {
                        //ResumeFromPowerOutage
                        decisionDialog.setContent(getString(R.string.a400_print_enclosure_operation_trigger));
                        break;
                    }
                    default: {
                        decisionDialog.setContent(R.string.print_warning_resume_unable);
                        break;
                    }
                }
                decisionDialog.show();
                break;
            case STOP_SUCCESS:
                if (isStop) return;
                isStop = true;
                Logger.i("print stopped.");
                if (mFabWorkingChangeDialog != null && mFabWorkingChangeDialog.isShowing()) {
                    mFabWorkingChangeDialog.dismiss();
                }
                mRouter.routeToHome().startAndClear(getContext());
                break;
            case STOP_FAIL:
                Logger.w("Unable to stop printing, ret code %d", retCode);
                if (mFabWorkingChangeDialog != null && mFabWorkingChangeDialog.isShowing()) {
                    mFabWorkingChangeDialog.dismiss();
                }

                if (retCode == 17) return;
                decisionDialog = DecisionDialog.create(getContext())
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContentColor(R.color.palette_white_pure)
                        .setContent(R.string.print_warning_stop_unable)
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                decisionDialog.show();
                break;
            case FINISH_SUCCESS:
                if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
                    mDecisionDialog.dismiss();
                }
                Logger.i("Print Finished.");
                ((PrintA400Activity) requireActivity()).gotoPrintCompleteFragment();
                break;
            case FINISH_FAIL:
                Logger.w("Unable to finish printing, ret code %d", retCode);
                if (retCode == 17) return;
                decisionDialog = DecisionDialog.create(getContext())
                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
                        .setType(DecisionDialog.WARMING_TYPE)
                        .setContentColor(R.color.palette_white_pure)
                        .setContent(R.string.print_warning_finish_unable)
                        .setFirstTv(R.string.all_btn_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
                decisionDialog.show();
                break;
            case OPEN_DOOR_PAUSE:
                mViewModel.setEnclosure(false);
                break;
//                decisionDialog = DecisionDialog.create(getContext())
//                        .setDialogStatus(DecisionDialog.BTN_ONE, false, false, false, true)
//                        .setType(DecisionDialog.WARMING_TYPE)
//                        .setContentColor(R.color.palette_white_pure)
//                        .setContent(getString(R.string.a400_print_enclosure_open, getString(mViewModel.isIsFdm() ? R.string.printing_toast_3dp_print : R.string.printing_toast_other)))
//                        .setFirstTv(R.string.a400_print_got_it, R.color.select_dialog_blue_txt, ((dialog, which) -> dialog.dismiss()));
//                decisionDialog.show();
//                break;
            default:
                break;
        }
        if (decisionDialog != null && decisionDialog.isShowing()) {
            if (mDecisionDialog != null && mDecisionDialog.isShowing()) {
                mDecisionDialog.dismiss();
            }
            mDecisionDialog = decisionDialog;
        }
    }

    public static String formatTime(double time) {
        int hour = (int) (time) / 3600;
        int minute = ((int) (time) % 3600) / 60;
        int second = ((int) (time) % 60);

        if (hour < 1) {
            return ServiceContainer.getInstance().getService(IAppService.class).getApp().getString(fabscreen.platform.base.R.string.date_helper_format_time_minute_second, minute, second);
        } else {
            return ServiceContainer.getInstance().getService(IAppService.class).getApp().getString(fabscreen.platform.base.R.string.date_helper_format_time_hour_minute, hour, minute);
        }
    }

    // filament refill procedure
    // 1. Checkout which extruder should refill filament;
    // 2. Checkout extruder temperature, heated up if target is 0;
    // 3. Checkout filament sensor is on or off, start filling filament process;
    // 4. extrude a certain length filament and ask user if ready. If not, continue extruding;
    // 5. user click continue printing, leave refill procedure and resume print.
    private void enterFilamentRefillProcedure() {
        if (mDisCheckoutExtruderTemperature != null && !mDisCheckoutExtruderTemperature.isDisposed()) {
            mDisCheckoutExtruderTemperature.dispose();
        }
        FileParsingDialog loadingDialog = FileParsingDialog.create(requireContext()).setContent(R.string.a400_print_dialog_warning_filament_run_out_action_heating_nozzle);
        loadingDialog.show();
        mDisCheckoutExtruderTemperature = mViewModel.checkoutExtruderTemperature()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(filamentState -> {
                    if (filamentState.isTemperatureReached()) {
                        loadingDialog.dismiss();
                        CheckFilamentProcess();
                    }
                }, LogHelper::log);
    }

    private void CheckFilamentProcess() {
        if (mDisCheckoutExtruderTemperature != null && !mDisCheckoutExtruderTemperature.isDisposed()) {
            mDisCheckoutExtruderTemperature.dispose();
        }
        if (mDisCheckFilamentProcess != null && !mDisCheckFilamentProcess.isDisposed()) {
            mDisCheckFilamentProcess.dispose();
        }

        FilamentState mFilamentState = mViewModel.getFilamentStateValue();
        if (mCheckFilamentProcessTipDialog != null) {
            mCheckFilamentProcessTipDialog.dismiss();
        }
        mCheckFilamentProcessTipDialog = StepIntroductionDialog.create(requireContext());
        mCheckFilamentProcessTipDialog.setCanceledOnTouchOutSide(false);
        mCheckFilamentProcessTipDialog.setOnClickBack(v -> {
            mCheckFilamentProcessTipDialog.dismiss();
            mViewModel.setFilament(true);
            mDisCheckoutExtruderTemperature.dispose();
        });
        if (!mFilamentState.getNowFilamentState()) {
            mCheckFilamentProcessTipDialog.setImage(mViewModel.isDoubleExtruder() ?
                    R.drawable.pic_a400_3dp_double_extruder_pluck_ilament_644x362 :
                    R.drawable.pic_a400_3dp_pluck_ilament_644x362
            );
            mCheckFilamentProcessTipDialog.setTitle(R.string.a400_print_dialog_warning_filament_run_out_pull_out_filament_title);
            mCheckFilamentProcessTipDialog.setContent(R.string.a400_print_dialog_warning_filament_run_out_pull_out_filament_content);
            mCheckFilamentProcessTipDialog.show();
            mDisCheckFilamentProcess = mViewModel.getFilamentStateObservable()
                    .as(bindToLifecycle())
                    .subscribe(filamentState -> {
                        if (mFilamentState.getNowFilamentState()) {
                            mCheckFilamentProcessTipDialog.dismiss();
                            CheckFilamentProcess();
                        }
                    }, LogHelper::log);
        } else {
            mCheckFilamentProcessTipDialog.setImage(mViewModel.isDoubleExtruder() ?
                    R.drawable.pic_a400_3dp_double_extruder_insert_ilament_1065x388 :
                    R.drawable.pic_a400_3dp_insert_ilament_644x362);
            mCheckFilamentProcessTipDialog.setTitle(R.string.a400_print_dialog_warning_filament_run_out_insert_filament_title);
            mCheckFilamentProcessTipDialog.setContent(R.string.a400_print_dialog_warning_filament_run_out_insert_filament_content);
            mCheckFilamentProcessTipDialog.show();
            mDisCheckFilamentProcess = mViewModel.getFilamentStateObservable()
                    .as(bindToLifecycle())
                    .subscribe(filamentState -> {
                        if (!mFilamentState.getNowFilamentState()) {
                            mCheckFilamentProcessTipDialog.dismiss();
                            fillingFilamentProcess();
                        }
                    }, LogHelper::log);
        }
    }

    private void fillingFilamentProcess() {
        if (mDisCheckFilamentProcess != null && !mDisCheckFilamentProcess.isDisposed()) {
            mDisCheckFilamentProcess.dispose();
        }
        FileParsingDialog loadingDialog = FileParsingDialog.create(requireContext()).setContent(R.string.a400_print_dialog_warning_filament_run_out_action_start_loading);
        loadingDialog.show();
        mViewModel.requestActivatedExtrusion(0, 100, 240, 0, 0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(structure -> {
                    loadingDialog.dismiss();
                    if (structure.isSuccess()) {
                        Logger.d("Filament loaded.");
                        checkFilamentComplete();
                    } else if (!structure.isBusy()) {
                        Logger.d("Filament load fail.");
                        showFilamentExtruderFailedDialog(structure.resultProp.getValue());
                    }
                }, e -> {
                    loadingDialog.dismiss();
                    mViewModel.setFilament(true);
                    LogHelper.log(e);
                    showFilamentExtruderFailedDialog(10086);
                });
    }

    private void checkFilamentComplete() {
        DecisionDialog.create(getContext())
                .setTitle(R.string.control_load_filament_success)
                .setContent(R.string.control_load_filament_success_content)
                .setType(DecisionDialog.NOTIFICATION_TYPE)
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                .setPic(R.drawable.pic_a400_success_112x112)
                .setFirstTv(R.string.control_load_filament, R.color.select_dialog_white_txt, ((dialog, which) -> {
                    dialog.dismiss();
                    mViewModel.setFilament(true);
                    fillingFilamentProcess();
                }))
                .setSecondTv(R.string.all_continue_printing, R.color.select_a400_dialog_success_txt, (dialog, which) -> {
                    dialog.dismiss();
                    mViewModel.setFilament(true);
                    if (isStateFail) {
                        mViewModel.startPrint();
                    } else {
                        mViewModel.requestMachineResume();
                    }
                }).show();
    }

    private void showFilamentExtruderFailedDialog(int value) {
        DecisionDialog.create(getContext())
                .setContent(String.format("An unknown exception occurred, error code %d", value))
                .setType(DecisionDialog.WARMING_TYPE)
                .setDialogStatus(DecisionDialog.BTN_ONE, true, false, false, true)
                .setPic(R.drawable.pic_a400_warning_112x112)
                .setFirstTv(getString(R.string.all_cancel), R.color.select_dialog_white_txt, ((dialog, which) -> {
                    mViewModel.setFilament(true);
                    dialog.dismiss();
                })).show();
    }

    @OnClick(R2.id.btn_a400_print_stop)
    void onClickControlStop() {
        playNormalClickSound();
        if (isStateFail) {
            mRouter.routeToHome().start(requireActivity());
        } else {
            DecisionDialog.create(getContext())
                    .setTitle(mViewModel.isIsFdm() ? R.string.a400_print_action_stop_printing : R.string.a400_print_action_stop_job)
                    .setContent(getString(R.string.a400_print_action_confirmation_content_format,
                            getString(mViewModel.isIsFdm() ? R.string.a400_print_action_stop_printing : R.string.all_stop_the_job).toLowerCase()))
                    .setType(DecisionDialog.WARMING_TYPE)
                    .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, true)
                    .setPic(R.drawable.pic_a400_warning_112x112)
                    .setFirstTv(getString(R.string.all_cancel), R.color.select_dialog_white_txt, ((dialog, which) -> dialog.dismiss()))
                    .setSecondTv(R.string.all_stop, R.color.select_dialog_yellow_txt, (dialog, which) -> {
                        dialog.dismiss();
                        mFabWorkingChangeDialog.setContent(mViewModel.isIsFdm() ? R.string.a400_print_stoping : R.string.a400_job_stoping).show();
                        mViewModel.requestMachineStop();
                    }).show();
        }
    }

    @OnClick(R2.id.btn_a400_print_pause)
    void onClickControlPause() {
        playNormalClickSound();
        DecisionDialog.create(getContext())
                .setTitle(mViewModel.isIsFdm() ? R.string.a400_print_action_pause_printing : R.string.a400_print_action_pause_job)
                .setContent(getString(R.string.a400_print_action_confirmation_content_format,
                        getString(mViewModel.isIsFdm() ? R.string.a400_print_action_pause_printing : R.string.a400_print_action_pause_the_job).toLowerCase()))
                .setType(DecisionDialog.WARMING_TYPE)
                .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, true)
                .setPic(R.drawable.pic_a400_warning_112x112)
                .setFirstTv(getString(R.string.all_cancel), R.color.select_dialog_white_txt, ((dialog, which) -> dialog.dismiss()))
                .setSecondTv(R.string.all_pause, R.color.select_dialog_yellow_txt, (dialog, which) -> {
                    dialog.dismiss();
                    mFabWorkingChangeDialog.setContent(mViewModel.isIsFdm() ? R.string.a400_print_pausing : R.string.a400_job_pausing).show();
                    mViewModel.requestMachinePause();
                }).show();
    }

    @OnClick(R2.id.btn_a400_print_resume)
    void onClickControlResume() {
        playNormalClickSound();
        if (isStateFail) {
            mViewModel.startPrint();
        } else {
            mViewModel.requestMachineResume();
        }
    }

    @OnClick(R2.id.btn_a400_print_setting)
    void onClickSettings() {
        playSwitchSound();
        ((PrintA400Activity) requireActivity()).gotoA400AdjustmentContainerFragment();
    }
}

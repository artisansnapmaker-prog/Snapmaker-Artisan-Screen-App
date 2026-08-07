package fabscreen.features.home;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.orhanobut.logger.Logger;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.platform.base.RoutePath;
import fabscreen.platform.base.helper.Md5Util;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.parser.IGcodeParser;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.controller.NewPrintController;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.structure.BaseStructure;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.core.ui.view.FileParsingDialog;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

@Route(path = RoutePath.HOME_LAND)
public class HomePrintIdleModuleFragment extends BaseFragment {

    @BindView(R2.id.iv_a400_home_model_pic)
    ImageView mIvA400HomeModelPic;
    private int mFileType = 0;
    protected FileParsingDialog fabLoading;
    DecisionDialog mPrintPowerLossDialog;

    public static HomePrintIdleModuleFragment newInstance() {
        return new HomePrintIdleModuleFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();

        Logger.d("Requesting Print Power Outage...");
        IMachine machine = ServiceContainer.getInstance().getService(IMachine.class);
        if (machine.getMachineStatusSubjectHolder().getValue().connected) {
            NewPrintController newPrintController = machine.getNewPrintController();
            newPrintController.requestPowerOutageStatus()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(responseStructure -> {
                        if (responseStructure.isSuccess()) {
                            Logger.d("Power loss outage detected.");
                            handlePrintPowerLoss(responseStructure);
                        } else {
                            Logger.d("Power loss issues return " + responseStructure.resultProp.getValue());
                        }
                    }, LogHelper::log);
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_home_print_idle;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        fabLoading = FileParsingDialog.create(requireContext()).setContent(getString(R.string.all_tip_parse_file_loading));
    }

    @Override
    public void onResume() {
        super.onResume();
        IMachine.WorkType workType = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType;
        switch (workType) {
            case CNC:
                mFileType = 3;
                Glide.with(requireContext()).load(R.drawable.ic_a400_home_cnc_pic).into(mIvA400HomeModelPic);
                break;
            case FDM:
                mFileType = 1;
                Glide.with(requireContext()).load(R.drawable.ic_a400_home_3dp_pic).into(mIvA400HomeModelPic);
                break;
            case LASER:
                mFileType = 2;
                int headType = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().headType;
                switch (headType) {
                    case Module.ModuleType.HEAD_LASER:
                    case Module.ModuleType.HEAD_LASER_10W:
                        Glide.with(requireContext()).load(R.drawable.ic_a400_home_laser_pic).into(mIvA400HomeModelPic);
                        break;
                    case Module.ModuleType.HEAD_LASER_20W:
                        Glide.with(requireContext()).load(R.drawable.ic_a400_home_laser_20w_pic).into(mIvA400HomeModelPic);
                        break;
                    case Module.ModuleType.HEAD_LASER_40W:
                        Glide.with(requireContext()).load(R.drawable.ic_a400_home_laser_40w_pic).into(mIvA400HomeModelPic);
                        break;
                    case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                        Glide.with(requireContext()).load(R.drawable.ic_a400_home_laser_2w_pic).into(mIvA400HomeModelPic);
                        break;

                }
                break;
            case NONE:
            default:
                mFileType = 0;
                mIvA400HomeModelPic.setImageDrawable(null);
                break;
        }
    }

    @OnClick(R2.id.btn_home_start)
    void onClickStart() {
        playNormalClickSound();
        ServiceContainer.getInstance().getService(IRouter.class).routeToFilesPage(mFileType).start(getContext());
    }

    void handlePrintPowerLoss(ResponseStructure response) {
        BaseStructure gcodeFileInfo = (BaseStructure) response.dataProp;
        String md5 = (String) gcodeFileInfo.getProp("md5").getValue();
        String filename = (String) gcodeFileInfo.getProp("filename").getValue();

        IPrintWorkspace workspace = ServiceContainer.getInstance().getService(IPrintWorkspace.class);
        workspace.initLastPrintFile();
        if (workspace.getPrintFile() == null) {
            Logger.w("Could not find file in workspace!");
            return;
        }

        if (filename.equals(workspace.getFileName())) {
            DecisionDialog decisionDialog = DecisionDialog.create(requireContext())
                    .setDialogStatus(DecisionDialog.BTN_TWO, true, false, true, false)
                    .setType(DecisionDialog.WARMING_TYPE)
                    .setPic(R.drawable.ic_yellow_warn)
                    .setTitle(R.string.power_loss_recovery_title)
                    .setContent(R.string.power_loss_recovery_message)
                    .setFirstTv(R.string.all_cancel, R.color.select_dialog_white_txt, (dialog, which) -> {
                        dialog.dismiss();
                        Logger.d("Power loss recovery canceled, clear up power loss marker.");
                        ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().requestPrintPowerLossClearMarker()
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(responseStructure -> {
                                }, LogHelper::log);
                    })
                    .setSecondTv(R.string.all_resume, R.color.select_dialog_yellow_txt, (dialog, which) -> {
                        fabLoading.show();
                        IGcodeParser gcodeParser = ServiceContainer.getInstance().getService(IGcodeParser.class);
                        gcodeParser.startParse(workspace.getPrintFile(), ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType);
                        gcodeParser.getParseProgressObservable()
                                .throttleLast(100, TimeUnit.MILLISECONDS)
                                .distinctUntilChanged()
                                .takeUntil(progress -> progress == 100)
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(progress -> {
//                                    fabLoading.dismiss();
                                    if (progress == -1) {
                                        Logger.d("Try to parse file %s failed." + workspace.getPrintFile().getAbsolutePath());
                                    }
                                    if (progress == 100) {
                                        dialog.dismiss();
                                        fabLoading.dismiss();
                                        ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().setPowerOutageFlag(true);
                                        workspace.setFileMD5Value(md5);
                                        workspace.setFileTotalLineCount((int) gcodeParser.getTotalLinesCount());
                                        workspace.setEstimatedTime(workspace.getEstimatedTime());
                                        workspace.setApplyMultiExtruder(workspace.isApplyMultiExtruder());
                                        mRouter.routeToPrintPage().start(getContext());
                                    }
                                }, e -> {
                                    Logger.e("Try to parse file %s failed." + workspace.getPrintFile().getAbsolutePath() + "\nError: " + e);
                                    fabLoading.dismiss();
                                });
                    });
            decisionDialog.show();
            if (decisionDialog.isShowing()) {
                if (mPrintPowerLossDialog != null && mPrintPowerLossDialog.isShowing()) {
                    mPrintPowerLossDialog.dismiss();
                }
                mPrintPowerLossDialog = decisionDialog;
            }
        } else {
            Logger.w("Power loss detected but file not matched! \nRequest: %s, lastPrint: %s",
                    filename + "@" +md5,
                    workspace.getFileName() + "@" + Md5Util.fileToMD5(workspace.getPrintFile().getAbsolutePath()));
        }
    }
}

package fabscreen.features.print.a400platform;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.print.R;
import fabscreen.features.print.R2;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.parser.IGcodeParser;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.MenuAdapter;
import fabscreen.platform.base.view.WarmTipDialog;
import fabscreen.platform.core.ui.view.PullDownMenu;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PrintA400CompleteFragment extends BaseFragment {
    @BindView(R2.id.tv_print_file_name)
    TextView mTvFilename;
    @BindView(R2.id.tv_print_time)
    TextView mTvPrintTime;
    @BindView(R2.id.iv_print_file_diagram)
    ImageView mIvPrintFileDiagram;
    @BindView(R2.id.iv_print_base_show)
    ImageView mIvPrintBaseShow;
    @BindView(R2.id.btn_print_continue)
    Button mBtnPrintContinue;
    @BindView(R2.id.btn_print_again)
    Button mBtnPrintAgain;
    @BindView(R2.id.layout_print_continue)
    RelativeLayout mLayoutPrintContinue;
    @BindView(R2.id.layout_print_again)
    RelativeLayout mLayoutPrintAgain;
    @BindView(R2.id.iv_dropdown_arrow)
    ImageView mIvDropdownArrow;
    @BindView(R2.id.iv_dropdown_arrow_again)
    ImageView mIvDropdownArrowAgain;

    private IMachine mA400Machine;
    private MenuAdapter mMenuAdapter;
    private String[] mMenuOptions;

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize menu options
        mMenuOptions = new String[]{
                getString(fabscreen.platform.base.R.string.all_print_again),
                getString(fabscreen.platform.base.R.string.all_next_job)
        };
        initMenu();

        IPrintWorkspace workspace = ServiceContainer.getInstance().getService(IPrintWorkspace.class);
        Bitmap bitmap = ServiceContainer.getInstance().getService(IGcodeParser.class).getGcodeThumbnail();
        mTvFilename.setText(workspace.getFileName());
        mTvFilename.setVisibility(TextView.GONE);
        int elapsed = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getTickCounter().getCount();
        mTvPrintTime.setText(formatTime(elapsed));
        mIvPrintFileDiagram.setImageBitmap(bitmap);
        mA400Machine = ServiceContainer.getInstance().getService(IMachine.class);
        IMachine.WorkType workType = mA400Machine.getMachineInfoSubjectHolder().getValue().workType;
        switch (workType) {
            case FDM:
                mIvPrintBaseShow.setImageResource(R.drawable.pic_a400_print_base_show_fdm);
                break;
            case LASER:
                mIvPrintBaseShow.setImageResource(R.drawable.pic_a400_print_base_show_laser);
                break;
            case CNC:
                mIvPrintBaseShow.setImageResource(R.drawable.pic_a400_print_base_show_cnc);
                break;
            default:
                break;
        }
        playProcedureCompleteSound();
        
        // Setup dropdown menu for print continue button
        setupPrintContinueDropdown();
    }

    private void initMenu() {
        ArrayList<String> menuItems = new ArrayList<>(Arrays.asList(mMenuOptions));
        mMenuAdapter = new MenuAdapter(getContext(), menuItems);
        mMenuAdapter.setOnItemClickListener((view, position) -> {
            playNormalClickSound();
            handleMenuItemClick(position);
            PullDownMenu.dismiss();
        });
    }

    private void setupPrintContinueDropdown() {
        // Button clicks execute actions directly
        mBtnPrintContinue.setOnClickListener(v -> {
            playNormalClickSound();
            onClickContinue();
        });
        
        mBtnPrintAgain.setOnClickListener(v -> {
            playNormalClickSound();
            onClickPrintAgain();
        });
        
        // Arrow clicks show the menu for switching
        mIvDropdownArrow.setOnClickListener(v -> {
            playNormalClickSound();
            showPrintOptionsMenu(mLayoutPrintContinue);
        });
        
        mIvDropdownArrowAgain.setOnClickListener(v -> {
            playNormalClickSound();
            showPrintOptionsMenu(mLayoutPrintAgain);
        });
    }

    private void showPrintOptionsMenu(View anchorView) {
        PullDownMenu.create(getContext(), mMenuAdapter)
                .showBelowView(anchorView, -(int) DimensUtils.dp2px(120), -(int) DimensUtils.dp2px(300));
    }

    private void handleMenuItemClick(int position) {
        // Menu is only for switching button display, not executing actions
        switch (position) {
            case 0: // Print Again
                switchToPrintAgainMode();
                break;
            case 1: // Continue to Next Job
                switchToContinueMode();
                break;
        }
    }

    private void switchToPrintAgainMode() {
        // Hide the continue button layout and show print again button layout
        mLayoutPrintContinue.setVisibility(View.GONE);
        mLayoutPrintAgain.setVisibility(View.VISIBLE);
    }

    private void switchToContinueMode() {
        // Hide print again button layout and show the continue button layout
        mLayoutPrintAgain.setVisibility(View.GONE);
        mLayoutPrintContinue.setVisibility(View.VISIBLE);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_print_complete;
    }

    @OnClick(R2.id.btn_print_complete)
    void OnClickComplete() {
        playNormalClickSound();
        ServiceContainer.getInstance().getService(IRouter.class).routeToHome().startAndClear(requireContext());
    }

    private void onClickContinue() {
        playNormalClickSound();
        requireActivity().finish();
    }

    private void onClickPrintAgain() {
        playNormalClickSound();
        if (ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM) {
            // Call the activity's goToPrint method to start printing again
            if (requireActivity() instanceof PrintA400Activity) {
                ((PrintA400Activity) requireActivity()).goToPrint();
            }
        } else {
            WarmTipDialog movingDialog = WarmTipDialog.create(requireContext())
                    .setDialogWidthSize(WarmTipDialog.WarmTipDialogSize.SIZE_M)
                    .setPic(R.drawable.ic_block_setup)
                    .setTitle(R.string.all_move_show)
                    .setContent(R.string.all_move_show_content);
            movingDialog.show();
            ServiceContainer.getInstance().getService(IMachine.class)
                    .getMachineController()
                    .goToOrigin()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        movingDialog.dismiss();
                        // Call the activity's goToPrint method to start printing again
                        if (requireActivity() instanceof PrintA400Activity) {
                            ((PrintA400Activity) requireActivity()).goToPrint();
                        }
                    }, e -> {
                        movingDialog.dismiss();
                        LogHelper.log(e);
                    });
        }
    }
}


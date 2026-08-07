package fabscreen.features.filemanager.j1Platform;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.filemanager.BrowseFileDetailViewModel;
import fabscreen.features.filemanager.BrowseFileListAdapter;
import fabscreen.features.filemanager.BrowseViewModel;
import fabscreen.features.filemanager.R;
import fabscreen.features.filemanager.R2;
import fabscreen.features.filemanager.a400platform.BrowseA400Activity;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.parser.IGcodeParser;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.structure.BaseStructure;
import fabscreen.platform.base.service.machine.structure.prop.UInt8Prop;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.base.view.DecisionDialog;
import fabscreen.platform.base.view.MenuAdapter;
import fabscreen.platform.core.ui.view.FabAlert;
import fabscreen.platform.core.ui.view.FabProgressDialog;
import fabscreen.platform.core.ui.view.PullDownMenu;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class BrowseFileListFragment extends BaseFragment implements BrowseFileListAdapter.OnItemClickListener {
    protected FabProgressDialog fabLoading;
    @BindView(R2.id.btn_browse_file_list_top_folder)
    TextView mTvFolder;
    @BindView(R2.id.btn_top_bar_back)
    Button mBtnBack;
    @BindView(R2.id.rcv_browse_local_file_list)
    RecyclerView mRcvLocalFileList;
    @BindView(R2.id.btn_browse_file_list_sort_option)
    Button mBtnListSort;
    @BindView(R2.id.ll_browse_external_file_list_empty)
    LinearLayout mLlFileListEmpty;
    @BindView(R2.id.tl_browse_file_list)
    TabLayout mTabLayout;
    @BindView(R2.id.iv_browse_external_file_list_empty)
    ImageView mIvEmpty;
    @BindView(R2.id.tv_browse_external_file_list_empty_title)
    TextView mTvEmptyTitle;
    @BindView(R2.id.tv_browse_external_file_list_empty_content)
    TextView mTvEmptyContent;

    private IRouter mRouter;
    private IPrintWorkspace mPrintWorkspace;

    private IGcodeParser mParser;

    private MenuAdapter mMenuAdapter;

    private BrowseFileListAdapter mLocalFileListAdapter;
    private GridLayoutManager mGridLayoutManager;
    private BrowseFileListAdapter mExternalFileListAdapter;

    private BrowseViewModel mViewModel;
    private BrowseFileDetailViewModel browseFileDetailViewModel;
    private final int mPrintMode = 0;
    private boolean mIsJ1;
    private BrowseViewModel.FileType filterType;
    private String[] mTabs;
    private int mSelectMunPosition = 0;

    public static Fragment newInstance(int fileType) {
        // FIXME: FileType should be passed directly for use
        BrowseViewModel.FileType type;
        switch (fileType) {
            case 1:
                type = BrowseViewModel.FileType.FILE_TYPE_GCODE;
                break;
            case 2:
                type = BrowseViewModel.FileType.FILE_TYPE_NC;
                break;
            case 3:
                type = BrowseViewModel.FileType.FILE_TYPE_CNC;
                break;
            case 4:
                type = BrowseViewModel.FileType.FILE_TYPE_UPDATE;
                break;
            case 0:
            default:
                type = BrowseViewModel.FileType.FILE_TYPE_UNKNOWN;
                break;

        }
        Fragment fragment = new BrowseFileListFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("file_type", type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRouter = ServiceContainer.getInstance().getService(IRouter.class);
        mPrintWorkspace = ServiceContainer.getInstance().getService(IPrintWorkspace.class);
        mParser = ServiceContainer.getInstance().getService(IGcodeParser.class);
        mIsJ1 = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().seriesId == IMachine.MachineSeries.J;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();

        mTabs = new String[]{getString(R.string.all_local), "USB"};
        for (String tab : mTabs) {
            mTabLayout.addTab(mTabLayout.newTab().setText(tab));
        }
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        //本地
                        playNormalClickSound();
                        initLocalFiles();
                        updateLocalFilesLists();
                        break;
                    case 1:
                        //USB
                        playNormalClickSound();
                        initExternalFiles();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        mIvEmpty.setImageResource(mIsJ1 ? R.drawable.pic_file_normal_160x160
                : R.drawable.pic_a400_file_normal);

        if (bundle != null)
            filterType = (BrowseViewModel.FileType) bundle.getSerializable("file_type");

        browseFileDetailViewModel = getViewModel();
        initLocalFiles();
        updateLocalFilesLists();
        initMenu();

        if (mIsJ1 && mViewModel.getFileManagerStateValue()) {
//            initExternalFiles();
//            mTabLayout.setScrollPosition(1, 0, true);
            Objects.requireNonNull(mTabLayout.getTabAt(1)).select();
        }

        ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController()
                .requestPrintModeStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    BaseStructure baseStructure = (BaseStructure) responseStructure.dataProp;
                    int printMode = ((UInt8Prop) baseStructure.getProp("printMode")).getValue();
                    Logger.d("Print Mode " + printMode);
                });
        fabLoading = new FabProgressDialog(getContext());
        fabLoading.setMessage(R.string.file_parsing);
    }

    @Override
    protected BrowseFileDetailViewModel getViewModel() {
        return getViewModelProvider().get(BrowseFileDetailViewModel.class);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_browse_j1_file_list;
    }

    private void initLocalFiles() {
        mViewModel = new BrowseViewModel(true, filterType);
        mViewModel.listFiles();
        mViewModel.nowFolderObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(folderName -> {
                    if (folderName.isEmpty()) {
                        if (mIsJ1) {
                            mBtnBack.setVisibility(View.VISIBLE);
                        } else {
                            mBtnBack.setVisibility(View.INVISIBLE);
                        }
                        mTabLayout.setVisibility(View.VISIBLE);
                        mTvFolder.setVisibility(View.INVISIBLE);
                    } else {
                        mTvFolder.setVisibility(View.VISIBLE);
                        mTabLayout.setVisibility(View.INVISIBLE);
                        if (mIsJ1) {
                            mTvFolder.setText(folderName);
                        } else {
                            mBtnBack.setVisibility(View.INVISIBLE);
                            mTvFolder.setText(R.string.a400_file_return_to_previous_menu);
                        }
                    }
                });
    }

    private void initExternalFiles() {
        mViewModel = new BrowseViewModel(false, filterType);
        mViewModel.getFileManagerStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isAttach -> {
                    if (isAttach) {
                        mViewModel.listFiles();
                        updateUsbFilesLists();
                    } else {
                        showLocalOrUsbTip(false);
                    }
                }, LogHelper::log);
        mViewModel.nowFolderObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(folderName -> {
                    if (folderName.isEmpty()) {
                        if (mIsJ1) {
                            mBtnBack.setVisibility(View.VISIBLE);
                        } else {
                            mBtnBack.setVisibility(View.INVISIBLE);
                        }
                        mTabLayout.setVisibility(View.VISIBLE);
                        mTvFolder.setVisibility(View.INVISIBLE);
                    } else {
                        mTvFolder.setVisibility(View.VISIBLE);
                        mTabLayout.setVisibility(View.INVISIBLE);
                        if (mIsJ1) {
                            mTvFolder.setText(folderName);
                        } else {
                            mBtnBack.setVisibility(View.INVISIBLE);
                            mTvFolder.setText(R.string.a400_file_return_to_previous_menu);
                        }
                    }
                });
    }

    private void updateLocalFilesLists() {
        // view
        mLocalFileListAdapter = new BrowseFileListAdapter(mIsJ1);
        mLocalFileListAdapter.setOnItemClickListener(this);
        mGridLayoutManager = new GridLayoutManager(getContext(), 4);
        mRcvLocalFileList.setAdapter(mLocalFileListAdapter);
        mRcvLocalFileList.setLayoutManager(mGridLayoutManager);

        // update file lists
        mViewModel.getFileListItemsObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(items -> {
                    if (items != null && items.size() > 0) {
                        mRcvLocalFileList.setVisibility(View.VISIBLE);
                        mLlFileListEmpty.setVisibility(View.GONE);
                    } else {
                        showLocalOrUsbTip(true);
                    }
                    mLocalFileListAdapter.setFileItems(items);
                    mLocalFileListAdapter.notifyDataSetChanged();
                });
    }

    private void updateUsbFilesLists() {
        mExternalFileListAdapter = new BrowseFileListAdapter(mIsJ1);
        mExternalFileListAdapter.setOnItemClickListener(this);
        mGridLayoutManager = new GridLayoutManager(getContext(), 4);
        mRcvLocalFileList.setAdapter(mExternalFileListAdapter);
        mRcvLocalFileList.setLayoutManager(mGridLayoutManager);

        // update file lists
        mViewModel.getFileListItemsObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(items -> {
                    if (items.size() > 0) {
                        mRcvLocalFileList.setVisibility(View.VISIBLE);
                        mLlFileListEmpty.setVisibility(View.GONE);
                    } else {
                        showLocalOrUsbTip(false);
                    }
                    mExternalFileListAdapter.setFileItems(items);
                    mExternalFileListAdapter.notifyDataSetChanged();
                });
    }

    public void showLocalOrUsbTip(boolean isLocal) {
        mLlFileListEmpty.setVisibility(View.VISIBLE);
        mRcvLocalFileList.setVisibility(View.GONE);
        if (mIsJ1) {
            mIvEmpty.setImageResource(isLocal ? R.drawable.pic_file_normal_160x160 : R.drawable.pic_file_error_160x160);
            mTvEmptyContent.setText(isLocal ? R.string.j1_file_empty : R.string.j1_file_usb_flash_drive_not_recognized_content);
        }
        mTvEmptyTitle.setVisibility(isLocal ? View.GONE : View.VISIBLE);
    }

    private void initMenu() {
        ArrayList<String> menuItems = new ArrayList<>();
        menuItems.add(getString(R.string.j1_file_date_ascending));
        menuItems.add(getString(R.string.j1_file_date_descending));
        menuItems.add(getString(R.string.j1_file_name_ascending));
        menuItems.add(getString(R.string.j1_file_name_descending));

        mMenuAdapter = new MenuAdapter(getContext(), menuItems);
        mMenuAdapter.setOnItemClickListener((view, position) -> {
            playNormalClickSound();
            mSelectMunPosition = position;
            switch (position) {
                case 0:
                    mViewModel.setFilter(BrowseViewModel.FILTER_DATE_ASCENDING);
                    break;
                case 1:
                    mViewModel.setFilter(BrowseViewModel.FILTER_DATE_DESCENDING);
                    break;
                case 2:
                    mViewModel.setFilter(BrowseViewModel.FILTER_NAME_ASCENDING);
                    break;
                case 3:
                    mViewModel.setFilter(BrowseViewModel.FILTER_NAME_DESCENDING);
                    break;
            }
            PullDownMenu.dismiss();
        });
    }

    @OnClick(R2.id.btn_top_bar_back)
    void onClickBack() {
        playNormalClickSound();
        if (mIsJ1 && !mViewModel.nowFolderValue().isEmpty()) {
            mViewModel.popDirectory();
        } else {
            back();
        }
    }

    @OnClick(R2.id.btn_browse_file_list_top_folder)
    void onClickTopFolder() {
        playNormalClickSound();
        mViewModel.popDirectory();
    }

    @OnClick(R2.id.btn_browse_file_list_sort_option)
    void onClickSort() {
        playNormalClickSound();
        PullDownMenu.create(getContext(), mMenuAdapter)
                .showBelowView(mBtnListSort, -(int) DimensUtils.dp2px(190), 10);
        if (mMenuAdapter != null) {
            mMenuAdapter.setSelectPosition(mSelectMunPosition);
        }
    }

    @Override
    public void onItemClick(int position) {
        playNormalClickSound();
        Logger.d("User click:%d", position);
        BrowseViewModel.BrowseJ1FileItem item = mViewModel.getFileListItems().get(position);
        switch (item.fileType) {
            case FILE_TYPE_NC:
                startParse(IMachine.WorkType.LASER, item.getFilePath());
                break;
            case FILE_TYPE_CNC:
                startParse(IMachine.WorkType.CNC, item.getFilePath());
                break;
            case FILE_TYPE_GCODE:
                startParse(IMachine.WorkType.FDM, item.getFilePath());
                break;
            case FILE_TYPE_UPDATE:
                userConfirmUpdate(item);
                break;
            case FILE_TYPE_DIRECTORY:
                mViewModel.gotoDirectory(item.getFilePath());
                break;
            case FILE_TYPE_LOG:
            case FILE_TYPE_UNKNOWN:
            default:
                Logger.e("踏入了未知领域 %s，请联系开发", item.getFilePath());
                break;
        }


    }

    private void userConfirmUpdate(BrowseViewModel.BrowseJ1FileItem item) {
        DecisionDialog.create(requireContext())
                .setContent("Do you want to update the firmware?")
                .setType(DecisionDialog.TIP_TYPE)
                .setDialogStatus(2, false, false, false, true)
                .setFirstTv("Cancel", R.color.select_dialog_white_txt, (dialog, which) -> {
                    dialog.dismiss();
                    /*requireActivity().setResult(Activity.RESULT_CANCELED);
                    requireActivity().finish();*/
                })
                .setSecondTv("Update", R.color.select_dialog_orange_txt, (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent();
                    intent.putExtra("file_path", item.getFilePath());
                    intent.putExtra("is_local", mViewModel.isLocal());
                    finishActivityWithResultOk(intent);
                })
                .show();
    }

    private void startParse(IMachine.WorkType workType, String filePath) {
        // Show dialog when we started to parse the file, prevent causing bugs when user commit another action.
        if (!fabLoading.isShowing()) {
            fabLoading.show();
        }

        // TODO: Product requirements, document parsing ahead of time
        mParser.startParse(filePath, mViewModel.isLocal(), workType);
        mParser.getParseProgressObservable()
                .throttleLast(100, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .takeUntil(progress -> progress == 100)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(progress -> {
                    if (progress == -1) {
                        Logger.d("Try to parse file %s failed." + filePath);
                        FabAlert.alert(getContext(), "Read file " + filePath + " failed! Please Check USB disk status.");
                        fabLoading.dismiss();
                    }
                    if (progress == 100) {
                        fabLoading.dismiss();
                        browseFileDetailViewModel.setFile(filePath, mViewModel.isLocal());
                        FragmentActivity fragmentActivity = requireActivity();
                        if (fragmentActivity instanceof BrowseJ1Activity) {
                            ((BrowseJ1Activity) fragmentActivity).gotoBrowseFileDetailFragment();
                        } else if (fragmentActivity instanceof BrowseA400Activity) {
                            ((BrowseA400Activity) fragmentActivity).gotoBrowseFileDetailFragment();
                        }

//                        handleResult(selectFile);
                    }
                }, e -> {
                    Logger.d("Try to parse file %s failed." + filePath + "\nError: " + e);
                    FabAlert.alert(getContext(), "Read file " + filePath + " failed! Please Check USB disk status.");
                    fabLoading.dismiss();
                });
    }
}

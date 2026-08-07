package fabscreen.features.machinetools.calibration.a400platform.cnc.originAssistant;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.features.machinetools.calibration.A400CalibrationBaseFragment;
import fabscreen.features.machinetools.cncassist.origin.CNCOriginAssistantBitItem;
import fabscreen.features.machinetools.cncassist.origin.CNCOriginAssistantBitsAdapter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class CNCOriginAssistantSetCarvingToolLandFragment extends A400CalibrationBaseFragment implements CNCOriginAssistantBitsAdapter.OnItemClickListener {
    @BindView(R2.id.gv_origin_assistant_bits)
    GridView mGvBits;
    @BindView(R2.id.btn_cnc_origin_assistant_set_carving_tool_next)
    Button mBtnNext;
    @BindView(R2.id.iv_cnc_origin_assistant_set_carving_tool_pc)
    ImageView mIvPic;
    @BindView(R2.id.view_guide_progress_bar)
    LinearProgressIndicator mGuideProgressBar;
    private A400CNCOriginAssistantViewModel mViewModel;
    private float mBitDiameter = -1;
    private float mBitLength = -1;
    private ArrayList<CNCOriginAssistantBitItem> mBitsList;
    private CNCOriginAssistantBitsAdapter mBitAdapter;
    private BehaviorSubject<CNCOriginAssistantBitItem> mSelectedItemSubject = BehaviorSubject.create();


    public static CNCOriginAssistantSetCarvingToolLandFragment newInstance() {
        return new CNCOriginAssistantSetCarvingToolLandFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGuideProgressBar.setMax(9);
        mGuideProgressBar.setProgress(3);
        setTitle(R.string.calibration_cnc_origin_assistant);
        setContent(getString(R.string.a400_cnc_origin_select_bit_title));
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        Glide.with(requireContext())
                .load(mViewModel.is200wCnc() ?
                        R.drawable.pic_cnc_200w_origin_assistant_select_bit :
                        R.drawable.pic_cnc_origin_assistant_select_bit
                )
                .apply(options)
                .into(mIvPic);

        initData();
        initAdapter();

        mBtnNext.setEnabled(false);
        mSelectedItemSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(item -> {
                    boolean isDefaultBit = item.isDefaultBit();
                    mBtnNext.setEnabled(isDefaultBit);

                    if (isDefaultBit) {
                        mBitDiameter = item.getBitDiameter();
                        mBitLength = item.getBitLength();
                    }
                });
    }

    private void initData() {
        mBitsList = new ArrayList<>();

        mBitsList.add(new CNCOriginAssistantBitItem(CNCOriginAssistantBitItem.TYPE_ITEM_BIT_DEFAULT,
                getResources().getString(R.string.cnc_origin_assistant_flat_end_mill),
                R.drawable.pic_flat_end_mill_single, 3.175f, 50f, getString(R.string.cnc_origin_assistant_double_tip)));
        mBitsList.add(new CNCOriginAssistantBitItem(CNCOriginAssistantBitItem.TYPE_ITEM_BIT_DEFAULT,
                getResources().getString(R.string.cnc_origin_assistant_ball_end_mill),
                R.drawable.pic_ball_end_mill, 3.175f, 50f, getString(R.string.cnc_origin_assistant_double_tip)));
        mBitsList.add(new CNCOriginAssistantBitItem(CNCOriginAssistantBitItem.TYPE_ITEM_BIT_DEFAULT,
                getResources().getString(R.string.cnc_origin_assistant_straight_groove_v_bit),
                R.drawable.pic_straight_groove_v_bit, 3.175f, 50f, getString(R.string.cnc_origin_assistant_double_tip)));
        mBitsList.add(new CNCOriginAssistantBitItem(CNCOriginAssistantBitItem.TYPE_ITEM_BIT_DEFAULT,
                getResources().getString(R.string.cnc_origin_assistant_flat_end_mill),
                R.drawable.pic_flat_end_mill_double_other, 1.5f, 50f, getString(R.string.cnc_origin_assistant_single_and_msg_small_diameter_tip)));
        mBitsList.add(new CNCOriginAssistantBitItem(CNCOriginAssistantBitItem.TYPE_ITEM_BIT_DEFAULT,
                getResources().getString(R.string.cnc_origin_assistant_flat_end_mill),
                R.drawable.pic_flat_end_mill_double, 3.175f, 50f, getString(R.string.cnc_origin_assistant_single_and_middle_diameter_tip)));
        // custom bit button
        mBitsList.add(new CNCOriginAssistantBitItem(CNCOriginAssistantBitItem.TYPE_ITEM_BIT_CUSTOM,
                getResources().getString(R.string.cnc_origin_assistant_custom_bit),
                R.drawable.btn_cnc_origin_assistant_custom_bit_70x70, -1, -1, ""));
    }

    private void initAdapter() {
        mBitAdapter = new CNCOriginAssistantBitsAdapter(getContext());
        mBitAdapter.setOnItemClickListener(this);
        mBitAdapter.setItems(mBitsList);
        mGvBits.setAdapter(mBitAdapter);

        mBitAdapter.notifyDataSetChanged();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_cnc_origin_assistant_set_carving_tool_land;
    }

    @Override
    protected A400CNCOriginAssistantViewModel getViewModel() {
        return getViewModelProvider().get(A400CNCOriginAssistantViewModel.class);
    }

    @OnClick(R2.id.btn_cnc_origin_assistant_set_carving_tool_next)
    void onClickNext() {
        playNormalClickSound();
        mViewModel.setBitDiameterInput(String.valueOf(mBitDiameter));
        mViewModel.setBitLengthInput(String.valueOf(mBitLength));

        if (getActivity() != null) {
            ((CncOriginAssistantActivity) getActivity()).gotoCNCOriginAssistantSetOriginIntroFragment();
        }
    }

    @Override
    public void onItemDefaultBitClick(View view, int position) {
        playNormalClickSound();
        mSelectedItemSubject.onNext(mBitAdapter.getItem(position));
        mBitAdapter.setSelectedPosition(position);
    }

    @Override
    public void onItemCustomBitClick(View view, int position) {
        playNormalClickSound();
        CustomBitDialog.create(getContext(), mViewModel)
                .onBackClick((dialog, which) -> {
                    dialog.dismiss();
                })
                .onBackNext((dialog, which) -> {
                    if (getActivity() != null) {
                        ((CncOriginAssistantActivity) getActivity()).gotoCNCOriginAssistantSetOriginIntroFragment();
                    }
                    dialog.dismiss();
                })
                .show();
    }

}

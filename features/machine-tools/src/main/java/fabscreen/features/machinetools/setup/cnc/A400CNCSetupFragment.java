package fabscreen.features.machinetools.setup.cnc;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.base.view.BaseFragment;

public class A400CNCSetupFragment extends BaseFragment {

    private A400CNCSetupViewModel mViewModel;

    public static Fragment newInstance() {
        return new A400CNCSetupFragment();
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
    ImageView mIvDemonstrate;
    @BindView(R2.id.tv_demonstrate_desc)
    TextView mTvDemonstrateDesc;
    @BindView(R2.id.btn_start_or_next)
    Button mBtnStartOrNext;
    @BindView(R2.id.cb_demonstrate)
    CheckBox mCb;
    @BindView(R2.id.tv_demonstrate_guide)
    TextView mTvGuide;
    @BindView(R2.id.iv_guide_problem)
    ImageView mIvGuideProblem;

    private int mCurrentStep = 0;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
    }

    @Override
    protected A400CNCSetupViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(A400CNCSetupViewModel.class);
    }

    private void initView() {
        mTvTitle.setVisibility(View.VISIBLE);
        mTvSubTitle.setVisibility(View.VISIBLE);
        mProgress.setVisibility(View.VISIBLE);
        mIvGuideProblem.setVisibility(View.GONE);
        mProgress.setMax(4);
        mCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mBtnStartOrNext.setEnabled(isChecked);
            }
        });
        refreshView();
    }

    private void refreshView() {
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(18));
        mProgress.setProgress(mCurrentStep + 1);
        switch (mCurrentStep) {
            case 0:
                mCb.setVisibility(View.GONE);
                mTvGuide.setVisibility(View.GONE);
                mBtnStartOrNext.setEnabled(true);
                Glide.with(requireContext())
                        .load(R.drawable.pic_setup_for_cnc_job_safety_goggles)
                        .apply(options)
                        .into(mIvDemonstrate);
                mTvTitle.setText(R.string.guide_a400_cnc_initial_setup_guidance);
                mTvSubTitle.setText(R.string.guide_a400_cnc_1_4_subtitle);
                mTvDemonstrateDesc.setText(R.string.guide_a400_cnc_1_4_msg);
                mBtnStartOrNext.setText(R.string.all_next);
                mBtnClose.setVisibility(View.INVISIBLE);
                break;
            case 1:
                mCb.setChecked(false);
                mBtnStartOrNext.setEnabled(false);
                mCb.setVisibility(View.VISIBLE);
                mTvGuide.setVisibility(View.VISIBLE);
                mTvGuide.setText(HtmlCompat.fromHtml(requireContext().getResources().getString(mViewModel.isRotaryAvailable() ?
                                R.string.a400_cnc_four_axial_guide_accept : R.string.a400_cnc_tri_axial_guide_accept)
                        , HtmlCompat.FROM_HTML_MODE_LEGACY));
                mTvTitle.setText(R.string.guide_a400_cnc_initial_setup_guidance);
                mTvSubTitle.setText(R.string.guide_a400_cnc_2_4_subtitle);
                mTvDemonstrateDesc.setText(HtmlCompat.fromHtml(mViewModel.isRotaryAvailable() ? getString(R.string.guide_a400_cnc_2_4_four_axial_msg)
                        : getString(R.string.guide_a400_cnc_2_4_tri_axial_msg), HtmlCompat.FROM_HTML_MODE_LEGACY));
                mBtnStartOrNext.setText(R.string.all_next);
                Glide.with(requireContext())
                        .load(mViewModel.isRotaryAvailable() ?
                                R.drawable.pic_guide_cnc_four_fix_prepare_material_360x320
                                : R.drawable.pic_guide_cnc_prepare_material_360x320)
                        .apply(options)
                        .into(mIvDemonstrate);
                mBtnClose.setVisibility(View.VISIBLE);
                break;
            case 2:
                mBtnStartOrNext.setEnabled(false);
                mCb.setVisibility(View.VISIBLE);
                mCb.setChecked(false);
                mTvGuide.setVisibility(View.VISIBLE);
                mTvGuide.setText(HtmlCompat.fromHtml(requireContext().getResources().getString(mViewModel.isRotaryAvailable() ?
                                R.string.a400_cnc_four_axial_guide_accept : R.string.a400_cnc_tri_axial_guide_accept)
                        , HtmlCompat.FROM_HTML_MODE_LEGACY));
                Glide.with(requireContext())
                        .load(R.drawable.pic_guide_cnc_prepare_tool_head_360x320)
                        .apply(options)
                        .into(mIvDemonstrate);
                mTvTitle.setText(R.string.guide_a400_cnc_initial_setup_guidance);
                mTvSubTitle.setText(R.string.guide_a400_cnc_3_4_subtitle);
                mTvDemonstrateDesc.setText(HtmlCompat.fromHtml(mViewModel.isRotaryAvailable() ? getString(R.string.guide_a400_cnc_3_4_four_axial_msg)
                        : getString(R.string.guide_a400_cnc_3_4_tri_axial_msg), HtmlCompat.FROM_HTML_MODE_LEGACY));
                mBtnStartOrNext.setText(R.string.all_next);
                mBtnClose.setVisibility(View.VISIBLE);
                break;
            case 3:
                mBtnStartOrNext.setEnabled(true);
                mCb.setVisibility(View.GONE);
                mTvGuide.setVisibility(View.GONE);
                Glide.with(requireContext())
                        .load(getPicToolsScreen())
                        .apply(options)
                        .into(mIvDemonstrate);
                mTvTitle.setText(R.string.guide_a400_cnc_initial_setup_guidance);
                mTvSubTitle.setText(R.string.guide_a400_cnc_4_4_subtitle);
                mTvDemonstrateDesc.setText(HtmlCompat.fromHtml(mViewModel.isRotaryAvailable() ? getString(R.string.guide_a400_cnc_4_4_four_axial_msg)
                        : getString(R.string.guide_a400_cnc_4_4_tri_axial_msg), HtmlCompat.FROM_HTML_MODE_LEGACY));
                mBtnStartOrNext.setText(R.string.all_done);
                mBtnClose.setVisibility(View.VISIBLE);
                break;
        }
    }

    private int getPicToolsScreen() {
        Locale currentLanguage = ((BaseActivity) requireActivity()).getCurrentLanguage();
        if (currentLanguage == Locale.ENGLISH) {
            return mViewModel.isRotaryAvailable() ?
                    R.drawable.pic_tools_screen_four_fix_en_578x434
                    : R.drawable.pic_tools_screen_en_578x434;
        } else if (currentLanguage == Locale.SIMPLIFIED_CHINESE) {
            return mViewModel.isRotaryAvailable() ?
                    R.drawable.pic_tools_screen_four_fix_zh_578x434
                    : R.drawable.pic_tools_screen_zh_578x434;
        } else {
            return mViewModel.isRotaryAvailable() ?
                    R.drawable.pic_tools_screen_four_fix_en_578x434
                    : R.drawable.pic_tools_screen_en_578x434;
        }
    }

    @OnClick(R2.id.btn_start_or_next)
    void onStartOrNextClicked() {
        playNormalClickSound();
        if (mCurrentStep < 3) {
            mCurrentStep++;
            refreshView();
        } else {
            finishActivityWithResultOk();
        }
    }

    @OnClick(R2.id.btn_close)
    void onCloseClicked() {
        playNormalClickSound();
        mCurrentStep = 0;
        refreshView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_guide_setup;
    }
}

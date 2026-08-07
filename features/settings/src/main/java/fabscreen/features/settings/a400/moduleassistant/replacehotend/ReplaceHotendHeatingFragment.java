package fabscreen.features.settings.a400.moduleassistant.replacehotend;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import butterknife.BindView;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ReplaceHotendHeatingFragment extends BaseFragment {

    private ReplaceHotendViewModel mViewModel;

    @BindView(R2.id.tv_temp_desc)
    TextView mTvTempDesc;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_replace_hotend_heating;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ReplaceHotendViewModel.class);
        initView();
    }

    private void initView() {
        mViewModel.getNozzleTempObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshTempDesc, LogHelper::log);
    }

    private void refreshTempDesc(ReplaceHotendViewModel.NozzleTemp temps) {
        String rawDesc = getString(R.string.replace_hotend_l_nozzle_temp_title) + " : " + temps.l + "/" + temps.targetL + "°C｜"+getString(R.string.replace_hotend_r_nozzle_temp_title) + " : " + temps.r + "/" + temps.targetR + "°C";
        SpannableString spannable = new SpannableString(rawDesc);
        spannable.setSpan(new ForegroundColorSpan(0xFFFFFFFF), rawDesc.indexOf(":") + 1, rawDesc.indexOf("/"), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(0xFFFFFFFF), rawDesc.lastIndexOf(":") + 1, rawDesc.lastIndexOf("/"), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mTvTempDesc.setText(spannable);
    }
}

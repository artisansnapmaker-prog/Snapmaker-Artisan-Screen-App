package fabscreen.features.welcome.a400;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.welcome.R;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.R2;
import fabscreen.platform.core.ui.common.WelcomeNameViewModel;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400WelcomeNameFragment extends BaseFragment {
    @BindView(R2.id.et_welcome_name_input)
    TextView mEtMachineName;
    @BindView(R2.id.btn_welcome_name_next)
    Button mBtnNext;
    private WelcomeNameViewModel mViewModel;
    private CustomKeyboardUtil mCustomKeyboardUtil;

    public static A400WelcomeNameFragment newInstance() {
        return new A400WelcomeNameFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCustomKeyboardUtil = new CustomKeyboardUtil(requireActivity());
        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_welcom_name;
    }

    @Override
    protected WelcomeNameViewModel getViewModel() {
        return getViewModelProvider().get(WelcomeNameViewModel.class);
    }

    private void initView() {
        mCustomKeyboardUtil.bindKeyboardListener(mEtMachineName, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String name = String.valueOf(s).substring(0, Math.min(s.length(), 32));
                mEtMachineName.setText(name);
                mViewModel.updateName(name);
            }
        });
        mEtMachineName.setOnClickListener(v -> {
            mCustomKeyboardUtil.setPreInputText(String.valueOf(mEtMachineName.getText()));
            mCustomKeyboardUtil.showKeyboard(mEtMachineName, CustomKeyboardUtil.INPUT_TYPE_QWERTY_ABC);
        });

        mViewModel.getNameObservable()
                .take(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(name -> mEtMachineName.setText(name));

        mViewModel.getNameTipObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(tip -> {
                    switch (tip) {
                        case WelcomeNameViewModel.TIP_OK:
                            mBtnNext.setEnabled(true);
                            break;
                        case WelcomeNameViewModel.TIP_EMPTY:
                            mBtnNext.setEnabled(false);
                            break;
                    }
                });
    }

    @OnClick(R2.id.btn_welcome_name_next)
    void onClickSave() {
        playNormalClickSound();
        mViewModel.saveName();

        if (getActivity() != null) {
            ((A400WelcomeActivity) getActivity()).startWiFiFragment();
        }
    }
}

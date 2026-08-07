package fabscreen.features.settings.wifi;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;

import java.util.List;

import fabscreen.features.settings.R;
import fabscreen.platform.base.lib.network.AccessPoint;
import fabscreen.platform.base.view.SuperToastHelper;
import fabscreen.platform.core.ui.common.wifi.adapter.A400APListAdapter;
import fabscreen.platform.core.ui.common.wifi.adapter.APListAdapter;
import fabscreen.platform.core.ui.view.FabInputDialog;
import fabscreen.platform.core.ui.view.customkeyboard.CustomKeyboardUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class A400SettingsWifiFragment extends SettingsWifiFragment {

    public static Fragment newInstance() {
        return new A400SettingsWifiFragment();
    }

    @Override
    protected APListAdapter getAPListAdapter(List<AccessPoint> list) {
        return new A400APListAdapter(list);
    }

    @Override
    protected void bindKeyboardInputText(View view) {
        mCustomKeyboardUtil.bindKeyboardListener(view, new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String afterText = s.toString();
                if (afterText.length() < 8 || afterText.length() > 63) {
                    new SuperToastHelper.Builder()
                            .setDrawable(R.drawable.ic_pic_a400_error_68x68)
                            .setTitle(getString(R.string.a400_dialog_settings_wifi_password_length_invalid_title))
                            .setMessage(getString(R.string.a400_dialog_settings_wifi_password_length_invalid_desc))
                            .build()
                            .showToast(requireContext());
                    mViewModel.setSelected(null);
                } else {
                    mViewModel.setPassword(afterText);
                    mViewModel.connect();
                    mRvApList.scrollToPosition(0);
                }
            }
        });
    }

    @Override
    protected void goPassword(AccessPoint ap) {
        String selectedPassword = mViewModel.getSelectedPassword();
        mCustomKeyboardUtil.setPreInputText(selectedPassword);
        mCustomKeyboardUtil.showKeyboard(mRvApList, CustomKeyboardUtil.INPUT_TYPE_QWERTY_ABC);

    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_settings_wifi;
    }
}

package fabscreen.features.settings.a400.terms;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.features.settings.a400.A400SettingsActivity;
import fabscreen.platform.base.view.BaseFragment;

public class A400SettingsTermsFragment extends BaseFragment {

    @BindView(R2.id.checkBox)
    CheckBox mCheckBox;

    public static A400SettingsTermsFragment newInstance() {
        return new A400SettingsTermsFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_settings_security;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(getString(R.string.a400_settings_terms_and_conditions_title));

        mCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> playNormalClickSound());
    }

    @OnClick(R2.id.tv_experience_program)
    void onExperienceProgramClick() {
        playNormalClickSound();
        if (requireActivity() instanceof A400SettingsActivity) {
            ((A400SettingsActivity) requireActivity()).goToLongTextDisplay(
                    R.string.a400_settings_terms_experience_improvement_page_title,
                    R.string.a400_settings_terms_experience_improvement_page_content
            );
        }
    }


}

package fabscreen.features.welcome.a400;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.features.welcome.R;
import fabscreen.features.welcome.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.common.FakeLanguageLoadingActivity;

public class A400WelcomeLanguageFragment extends BaseFragment {
    @BindView(R2.id.rv_language)
    RecyclerView mRvLanguage;
    @BindView(R2.id.iv_still_bg)
    ImageView mIvStillBg;

    private List<LanguageItem> mLanguageList;
    private A400LanguageAdapter mLanguageAdapter;
    private int mSelectPosition = 0;


    public static A400WelcomeLanguageFragment newInstance() {
        return new A400WelcomeLanguageFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireView().postDelayed(() -> mIvStillBg.setVisibility(View.INVISIBLE), 1000);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Glide.with(this).load(R.drawable.pic_a400_welcome_change_language_bg).dontAnimate().into(mIvStillBg);

        Locale currentLanguage = ((BaseActivity) requireActivity()).getCurrentLanguage();
        mLanguageList = new ArrayList<>();
        mLanguageList.add(new LanguageItem(Locale.SIMPLIFIED_CHINESE, "中文"));
        mLanguageList.add(new LanguageItem(Locale.ENGLISH, "English"));
        mLanguageList.add(new LanguageItem(Locale.GERMAN, "Deutsch"));
//        mLanguageList.add("日本語");
//        mLanguageList.add("Français");
//        mLanguageList.add("Español");
//        mLanguageList.add("한국어");
//        mLanguageList.add("Italiano");
//        mLanguageList.add("");
//        mLanguageList.add("Pycckий");
//        mLanguageList.add("Українська");
//        mLanguageList.add("");
        mLanguageAdapter = new A400LanguageAdapter(mLanguageList);
        mRvLanguage.setAdapter(mLanguageAdapter);
        mRvLanguage.setLayoutManager(new GridLayoutManager(requireContext(), Math.min(mLanguageList.size(), 4)));
        for (int i = 0; i < mLanguageList.size(); i++) {
            if (mLanguageList.get(i).getLocale().equals(currentLanguage)) {
                mSelectPosition = i;
            }
        }
        mLanguageAdapter.selectPosition(mSelectPosition);
        mLanguageAdapter.setOnItemClickListener(new A400LanguageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                playNormalClickSound();
                mSelectPosition = position;
                mLanguageAdapter.selectPosition(position);
                startFakeLoading();
                requireView().postDelayed(() -> {
                    ServiceContainer.getInstance().getService(IPreferences.class).getHelper().setMachineSetupLanguage(true);
                    ((BaseActivity) requireActivity()).setLanguage(mLanguageList.get(mSelectPosition).getLocale());
                }, 100);
            }
        });
    }

    private void startFakeLoading() {
        mIvStillBg.setVisibility(View.VISIBLE);
        Intent intent = new Intent(requireContext(), FakeLanguageLoadingActivity.class);
        intent.putExtra("resId", R.drawable.pic_a400_welcome_change_language_bg);
        startActivity(intent);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_a400_welcome_language;
    }


    @OnClick(R2.id.btn_welcome_set_language_next)
    void onClickNext() {
        playNormalClickSound();

        if (getActivity() != null) {
            ((A400WelcomeActivity) getActivity()).startHelloFragment();
        }
    }
}

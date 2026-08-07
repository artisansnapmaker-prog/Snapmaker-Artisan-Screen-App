package fabscreen.features.settings.language;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import fabscreen.features.settings.R;
import fabscreen.features.settings.R2;
import fabscreen.features.settings.a400.A400SettingsActivity;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.base.view.BaseActivity;
import fabscreen.platform.base.view.BaseFragment;
import fabscreen.platform.core.ui.common.FakeLanguageLoadingActivity;

public class S30SettingsLanguageFragment extends BaseFragment {
    @BindView(R2.id.lv_settings_language)
    ListView mLvLanguageList;
    @BindView(R2.id.view_settings_top_bar)
    RelativeLayout mRlTopBar;
    private SettingsLanguageAdapter mAdapter;
    private ArrayList<LanguageItem> mLanguages;

    public static S30SettingsLanguageFragment newInstance() {
        return new S30SettingsLanguageFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.all_language);
        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_s30_settings_language;
    }

    void initView() {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mLvLanguageList.getLayoutParams();
        params.topMargin = requireActivity() instanceof A400SettingsActivity ? 0 : (int) DimensUtils.dp2px(12);
        mLvLanguageList.setDividerHeight(requireActivity() instanceof A400SettingsActivity ? (int) DimensUtils.dp2px(16) : 0);
        mRlTopBar.setVisibility(requireActivity() instanceof A400SettingsActivity ? View.VISIBLE : View.GONE);
        // init languages
        mLanguages = new ArrayList<>();
        mLanguages.add(new LanguageItem(Locale.SIMPLIFIED_CHINESE));
        mLanguages.add(new LanguageItem(Locale.ENGLISH));
        mLanguages.add(new LanguageItem(Locale.GERMAN));
//        mLanguages.add(new LanguageItem(Locale.FRENCH));
//        mLanguages.add(new LanguageItem(Locale.JAPANESE));

        Locale currentLanguage = ((BaseActivity) requireActivity()).getCurrentLanguage();
        mAdapter = new SettingsLanguageAdapter(requireActivity());

        mAdapter.setOnLanguageItemClickListener(item -> {
            if (!item.locale.equals(currentLanguage)) {
                playNormalClickSound();
                setSelectedLanguage(item.locale);
                startFakeLoading();
                ((BaseActivity) requireActivity()).setLanguage(item.locale);
            }
        });

        mAdapter.setItems(mLanguages);
        mLvLanguageList.setAdapter(mAdapter);

        // set current language selected
        setSelectedLanguage(currentLanguage);
        mAdapter.notifyDataSetChanged();
    }

    private void startFakeLoading() {
        Intent intent = new Intent(requireActivity(), FakeLanguageLoadingActivity.class);
        intent.putExtra("resId", R.drawable.pic_a400_change_language_bg);
        startActivity(intent);
    }

    void setSelectedLanguage(Locale locale) {
        if (mLanguages == null) return;

        for (LanguageItem l : mLanguages) {
            l.selected = l.locale.equals(locale);
        }
    }

    public interface OnLanguageSelectedListener {
        void onSelectedItem(LanguageItem item);
    }

    public static class LanguageItem {
        public Locale locale;
        public boolean selected;

        public LanguageItem(Locale locale) {
            this.locale = locale;
            this.selected = false;
        }
    }

    static class SettingsLanguageAdapter extends BaseAdapter {
        private List<LanguageItem> mItems;
        private OnLanguageSelectedListener mListener;
        private Activity mActivity;

        public SettingsLanguageAdapter(Activity activity) {
            mActivity = activity;
        }

        public void setOnLanguageItemClickListener(OnLanguageSelectedListener listener) {
            mListener = listener;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public LanguageItem getItem(int position) {
            return mItems.get(position);
        }

        public void setItems(ArrayList<LanguageItem> items) {
            mItems = items;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                int itemLayout;
                if (mActivity instanceof A400SettingsActivity) {
                    itemLayout = R.layout.item_a400_language;
                } else {
                    itemLayout = R.layout.item_j1_language;
                }
                convertView = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
            }

            TextView tvLanguageName = convertView.findViewById(R.id.tv_language_name);
            TextView tvLanguageSpell = convertView.findViewById(R.id.tv_language_spell);
            LanguageItem item = getItem(position);
            ImageView ivTick = convertView.findViewById(R.id.iv_tick);

            if (mActivity instanceof A400SettingsActivity) {
                tvLanguageName.setTextColor(item.selected ? 0xffffffff : 0xffC9C9C9);
            }
            tvLanguageName.setText(item.locale.getDisplayLanguage());
            tvLanguageSpell.setText(item.locale.getDisplayLanguage(item.locale));
            ivTick.setVisibility(item.selected ? View.VISIBLE : View.INVISIBLE);
            convertView.setActivated(item.selected);
            convertView.setOnClickListener(v -> mListener.onSelectedItem(item));
            return convertView;
        }
    }
}

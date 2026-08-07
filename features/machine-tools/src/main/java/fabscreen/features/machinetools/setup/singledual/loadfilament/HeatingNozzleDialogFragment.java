package fabscreen.features.machinetools.setup.singledual.loadfilament;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.uber.autodispose.AutoDispose;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.features.machinetools.R;
import fabscreen.features.machinetools.R2;
import fabscreen.platform.base.helper.DimensUtils;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class HeatingNozzleDialogFragment extends DialogFragment {
    @BindView(R2.id.tv_heating_content)
    TextView mTvHeatingContent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_a400_heating_nozzle_with_temp, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(fabscreen.platform.core.R.drawable.all_dialog_round_background);
            getDialog().getWindow().setLayout((int) DimensUtils.dp2px(780), (int) DimensUtils.dp2px(440));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCancelable(false);
        HeatingNozzleDialogViewModel viewModel = new ViewModelProvider(this).get(HeatingNozzleDialogViewModel.class);
        viewModel.getTempsObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_DESTROY)))
                .subscribe(temps -> mTvHeatingContent.setText(getString(R.string.a400_load_filament_heating_content, temps[0] + "/" + temps[1])), LogHelper::log);
    }
}

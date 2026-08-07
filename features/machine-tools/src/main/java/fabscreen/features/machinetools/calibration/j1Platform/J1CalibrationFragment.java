package fabscreen.features.machinetools.calibration.j1Platform;

import android.view.View;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import fabscreen.features.machinetools.R;
import fabscreen.platform.base.R2;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.core.ui.common.leftsection.J1LeftSectionsAdapter;
import fabscreen.platform.core.ui.common.leftsection.LeftSectionsAdapter;
import fabscreen.platform.core.ui.common.leftsection.SectionAndDetailContainerFragment;
import fabscreen.platform.core.ui.common.leftsection.SectionItem;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class J1CalibrationFragment extends SectionAndDetailContainerFragment {
    boolean isGuide = false;
    @BindView(R2.id.top_bar_back)
    Button button;

    public static Fragment newInstance() {
        return new J1CalibrationFragment();
    }

    @Override
    protected List<SectionItem> getLeftSections() {
        if (getArguments() != null) {
            isGuide = getArguments().getBoolean("is_guide", false);
        }
        List<SectionItem> items = new ArrayList<>();
        items.add(new SectionItem(requireContext(), R.string.calibration_heated_bed_leveing_title, J1CalibrationModeFragment.newInstance(J1CalibrationMode.J1CalibrationModeIndex.HEATED_BED_LEVELING, isGuide)));
        items.add(new SectionItem(requireContext(), R.string.calibration_Z_offset_calibration_title, J1CalibrationModeFragment.newInstance(J1CalibrationMode.J1CalibrationModeIndex.Z_OFFSET_CALIBRATION, isGuide)));
        items.add(new SectionItem(requireContext(), R.string.calibration_XY_offset_calibration_title, J1CalibrationModeFragment.newInstance(J1CalibrationMode.J1CalibrationModeIndex.XY_OFFSET_CALIBRATION, isGuide)));
        items.add(new SectionItem(requireContext(), R.string.calibration_calibration_check_title, J1CalibrationModeFragment.newInstance(J1CalibrationMode.J1CalibrationModeIndex.CALIBRATION_CHECK, isGuide)));
        return items;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_j1_section_and_detail_container;
    }

    @Override
    protected LeftSectionsAdapter getSectionsAdapter(List<SectionItem> sectionItems) {
        return new J1LeftSectionsAdapter(sectionItems);
    }

    @Override
    protected String getTitle() {
        return "Calibration";
    }

    @Override
    public void onResume() {
        super.onResume();
        ServiceContainer.getInstance().getService(IMachine.class).getMachineController().getHeatedBed().setZoneTargetTemperature(0, 0)
                .flatMap(response -> ServiceContainer.getInstance().getService(IMachine.class).getFDMController().stopExtruderHeat())
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                }, LogHelper::log);
        if (isGuide) {
            button.setVisibility(View.INVISIBLE);
            IPreferences.Helper helper = ServiceContainer.getInstance().getService(IPreferences.class).getHelper();
            if (!helper.getGuideLevelingBed()) {
                setSelection(0);
            } else if (!helper.getGuideLevelingZ()) {
                setSelection(1);
            } else if (!helper.getGuideLevelingXY()) {
                setSelection(2);
            } else if (!helper.getGuideCheckPrint()) {
                setSelection(3);
            } else {
                ((J1CalibrationActivity) requireActivity()).gotoGuideSuccess();
            }
        }
    }
}

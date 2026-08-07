package fabscreen.platform.base.helper;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import fabscreen.platform.base.R;
import fabscreen.platform.base.model.ModuleCompact;
import fabscreen.platform.base.service.machine.entity.Module;

import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_AIR_PURIFIER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_CNC_CALIBRATION;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_DRY_BOX;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_EMERGENCY_BUTTON;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_ENCLOSURE;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_ENCLOSURE_A400;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_FAN;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_HEATED_BED_A400;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_HEATED_BED_J1;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_HEATED_BED_S20;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_LEDS;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ADDON_TOOL_HEAD_ORIGINAL_TO_2_0_CONVERTER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.EMERGENCY_BUTTON_A400;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC_200W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_10W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_20W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_2W_INFRARED;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_40W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.LINEAR_MODULE_TBS_2019;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.LINEAR_MODULE_TMC_2021;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.LINEAR_A400;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.LINEAR_J1;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.ROTARY_MODULE;

public class ModuleIdNameMapper {
    public static List<String> convertIdsToNames(Context context, List<Integer> ids) {
        List<String> nameList = new ArrayList<>();
        for (int id : ids) {
            nameList.add(getModuleNameByIdIndex(context, id, 0));
        }
        return nameList;
    }

    public static List<String> convertModuleCompactToNames(Context context, List<ModuleCompact> moduleCompacts) {
        List<String> nameList = new ArrayList<>();
        for (ModuleCompact moduleCompact : moduleCompacts) {
            nameList.add(getModuleNameByIdIndex(context, moduleCompact.id, moduleCompact.index));
        }
        return nameList;
    }

    public static String getModuleNameByIdIndex(Context context, int id, int index) {
        /**
         * Workaround here. Due to the reason that some module would NOT DISPLAY for users,
         * Modules that list below will map as NULL OBJECT instead of names.
         * (Can check IDs in {@link Module.ModuleType )
         * <p>
         *  id  04      MODULE_DEVICE_ID_LIGHT_BAR          ADDON_LEDS
         *  id  09      MODULE_DEVICE_ID_CNC_TOOL_SETTING   ADDON_CNC_CALIBRATION
         *  id  10      MODULE_DEVICE_ID_PRINT_V_SM1        ADDON_TOOLHEAD_CONVERTER
         *  id  11      MODULE_DEVICE_ID_FAN                ADDON_FAN
         *  id  18      MODULE_DEVICE_ID_CALIBRATOR         ADDON_FDM_CALIBRATOR
         */

        switch (id) {
            case HEAD_3DP:
                return context.getString(R.string.all_tool_head_3dp);
            case HEAD_CNC:
                return context.getString(R.string.all_tool_head_cnc);
            case HEAD_LASER:
                return context.getString(R.string.all_tool_head_laser);
            case HEAD_3DP_DOUBLE_EXTRUDER:
                return context.getString(R.string.all_tool_head_dual_extruder);
            case HEAD_LASER_10W:
                return context.getString(R.string.all_tool_head_laser_10w);
            case HEAD_LASER_20W:
                return context.getString(R.string.all_tool_head_laser_20w);
            case HEAD_LASER_40W:
                return context.getString(R.string.all_tool_head_laser_40w);
            case HEAD_LASER_2W_INFRARED:
                return context.getString(R.string.all_tool_head_laser_2w_infrared);
            case HEAD_CNC_200W:
                return context.getString(R.string.all_tool_head_cnc_200w);
            case ADDON_ENCLOSURE:
            case ADDON_ENCLOSURE_A400:
                return context.getString(R.string.all_enclosure);
            case ROTARY_MODULE:
                return context.getString(R.string.all_rotary_module);
            case ADDON_AIR_PURIFIER:
                return context.getString(R.string.all_air_purifier);
            case EMERGENCY_BUTTON_A400:
                return context.getString(R.string.all_emergency_stop);
            case ADDON_HEATED_BED_A400:
                return context.getString(R.string.print_heated_bed);
            case LINEAR_A400:
                switch (index) {
                    case 0:
                        return context.getString(R.string.all_linear_module_x_title);
                    case 1:
                        return context.getString(R.string.all_linear_module_y1_title);
                    case 2:
                        return context.getString(R.string.all_linear_module_z1_title);
                    case 3:
                        return context.getString(R.string.all_linear_module_x_title);
                    case 4:
                        return context.getString(R.string.all_linear_module_y2_title);
                    case 5:
                        return context.getString(R.string.all_linear_module_z2_title);
                    default:
                        return context.getString(R.string.all_tool_head_unknown);
                }
            case ADDON_DRY_BOX:
//                return context.getString(R.string.all_dry_box_module);
            case ADDON_LEDS:
            case ADDON_FAN:
            case ADDON_TOOL_HEAD_ORIGINAL_TO_2_0_CONVERTER:
            case ADDON_EMERGENCY_BUTTON:
            case LINEAR_MODULE_TBS_2019:
            case ADDON_CNC_CALIBRATION:
            case ADDON_HEATED_BED_S20:
            case ADDON_HEATED_BED_J1:
            case LINEAR_J1:
            case LINEAR_MODULE_TMC_2021:
            default:
                return null;
        }
    }
}

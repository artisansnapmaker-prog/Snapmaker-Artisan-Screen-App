package fabscreen.platform.base.service.machine.controller;

import java.util.HashSet;

public enum MachineOperationStatus {
    /**
     * Machine operation status
     * https://github.com/Snapmaker/Controller2022-Marlin/blob/a400-dev-tmp/snapmaker/src/snapmaker.h
     */
    SYSTEM_STATUS_IDLE(0),
    SYSTEM_STATUS_STARTING(1),
    SYSTEM_STATUS_PRINTING(2),
    SYSTEM_STATUS_PAUSING(3),
    SYSTEM_STATUS_PAUSED(4),
    SYSTEM_STATUS_STOPING(5),
    SYSTEM_STATUS_STOPED(6),
    SYSTEM_STATUS_FINISHING(7),
    SYSTEM_STATUS_COMPLETED(8),
    SYSTEM_STATUS_RECOVERING(9),
    SYSTEM_STATUS_RESUMING(10),
    SYSTEM_STATUS_EMERGENCY_STOP(11),
    SYSTEM_STATUS_POWER_LOSS(12),
    SYSTEM_STATUS_REPLACE_MODE(13),

    // 3DP calibration
    SYSTEM_STATUS_XY_CALIBRATING(31),
    SYSTEM_STATUS_XY_CALIBRATING_PRINTING(32),
    SYSTEM_STATUS_AUTO_BEDLEVEL(33),
    SYSTEM_STATUS_MANUAL_BEDLEVEL(34),
    SYSTEM_STATUS_AUTO_BED_DETECTION(35),
    SYSTEM_STATUS_MANUAL_BED_DETECTION(36),
    SYSTEM_STATUS_PROBE_SENSOR_CALIBRATION(37),

    // Laser calibration
    SYSTEM_STATUS_LASER_CALI_START(63),
    //    SYSTEM_STATUS_LASER_DETECT_THICKNESS_AUTO = SYSTEM_STATUS_LASER_CALI_START,
    SYSTEM_STATUS_LASER_DETECT_PLATFORM_POSITION(64),
    SYSTEM_STATUS_LASER_CAMERA_CAPTURE(65),
    SYSTEM_STATUS_LASER_DETECT_FOCAL_LENGTH(66),
    SYSTEM_STATUS_LASER_DETECT_4AXIS_CENTER_POSITION(67),
    //    SYSTEM_STATUS_LASER_CALI_END = SYSTEM_STATUS_LASER_DETECT_4AXIS_CENTER_POSITION(68),
    SYSTEM_STATUS_LASER_CALIBRATION_PRINTING(68),

    // CNC calibration
    SYSTEM_STATUS_CNC_CALIBRATING(95),

    // upgrade
    SYSTEM_STATUS_APP_UPGRADE(127),
    SYSTEM_STATUS_MODULE_UPGRADE(128);

    private static final HashSet<MachineOperationStatus> PRINT_STATUS = new HashSet<MachineOperationStatus>() {{
        add(SYSTEM_STATUS_STARTING);
        add(SYSTEM_STATUS_PRINTING);
        add(SYSTEM_STATUS_PAUSING);
        add(SYSTEM_STATUS_PAUSED);
        add(SYSTEM_STATUS_STOPING);
        add(SYSTEM_STATUS_STOPED);
        add(SYSTEM_STATUS_FINISHING);
        add(SYSTEM_STATUS_RECOVERING);
        add(SYSTEM_STATUS_RESUMING);
        add(SYSTEM_STATUS_XY_CALIBRATING_PRINTING);
        add(SYSTEM_STATUS_LASER_CALIBRATION_PRINTING);
    }};

    private static final HashSet<MachineOperationStatus> PRINT_STATUS_CHANGES = new HashSet<MachineOperationStatus>() {{
        add(SYSTEM_STATUS_STARTING);
        add(SYSTEM_STATUS_PAUSING);
        add(SYSTEM_STATUS_STOPING);
        add(SYSTEM_STATUS_FINISHING);
        add(SYSTEM_STATUS_RECOVERING);
        add(SYSTEM_STATUS_RESUMING);
    }};

    private int value = 0;

    private MachineOperationStatus(int value) {
        this.value = value;
    }

    public static MachineOperationStatus valueOf(int value) {
        switch (value) {
            case 0:
                return SYSTEM_STATUS_IDLE;
            case 1:
                return SYSTEM_STATUS_STARTING;
            case 2:
                return SYSTEM_STATUS_PRINTING;
            case 3:
                return SYSTEM_STATUS_PAUSING;
            case 4:
                return SYSTEM_STATUS_PAUSED;
            case 5:
                return SYSTEM_STATUS_STOPING;
            case 6:
                return SYSTEM_STATUS_STOPED;
            case 7:
                return SYSTEM_STATUS_FINISHING;
            case 8:
                return SYSTEM_STATUS_COMPLETED;
            case 9:
                return SYSTEM_STATUS_RECOVERING;
            case 10:
                return SYSTEM_STATUS_RESUMING;
            case 11:
                return SYSTEM_STATUS_EMERGENCY_STOP;
            case 12:
                return SYSTEM_STATUS_POWER_LOSS;
            case 13:
                return SYSTEM_STATUS_REPLACE_MODE;
            case 31:
                return SYSTEM_STATUS_XY_CALIBRATING;
            case 32:
                return SYSTEM_STATUS_XY_CALIBRATING_PRINTING;
            case 33:
                return SYSTEM_STATUS_AUTO_BEDLEVEL;
            case 34:
                return SYSTEM_STATUS_MANUAL_BEDLEVEL;
            case 35:
                return SYSTEM_STATUS_AUTO_BED_DETECTION;
            case 36:
                return SYSTEM_STATUS_MANUAL_BED_DETECTION;
            case 37:
                return SYSTEM_STATUS_PROBE_SENSOR_CALIBRATION;

            case 63:
                return SYSTEM_STATUS_LASER_CALI_START;
            case 64:
                return SYSTEM_STATUS_LASER_DETECT_PLATFORM_POSITION;
            case 65:
                return SYSTEM_STATUS_LASER_CAMERA_CAPTURE;
            case 66:
                return SYSTEM_STATUS_LASER_DETECT_FOCAL_LENGTH;
            case 67:
                return SYSTEM_STATUS_LASER_DETECT_4AXIS_CENTER_POSITION;
            case 68:
                return SYSTEM_STATUS_LASER_CALIBRATION_PRINTING;
            case 95:
                return SYSTEM_STATUS_CNC_CALIBRATING;

            case 127:
                return SYSTEM_STATUS_APP_UPGRADE;
            case 128:
                return SYSTEM_STATUS_MODULE_UPGRADE;
            default:
                return null;
        }
    }

    public static boolean isPrinting(int value) {
        MachineOperationStatus machineOperationStatus = MachineOperationStatus.valueOf(value);
        if (machineOperationStatus == null) return false;
        return PRINT_STATUS.contains(machineOperationStatus);
    }

    public static boolean isPrintChange(int value) {
        MachineOperationStatus machineOperationStatus = MachineOperationStatus.valueOf(value);
        if (machineOperationStatus == null) return false;
        return PRINT_STATUS_CHANGES.contains(machineOperationStatus);
    }

    public int value() {
        return this.value;
    }

    public boolean valueEquals(int status) {
        return this.value == status;
    }
}

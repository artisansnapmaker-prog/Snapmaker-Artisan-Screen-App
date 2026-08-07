package fabscreen.platform.base.service.machine.connection;

import fabscreen.platform.base.service.machine.controller.MachineOperationStatus;
import fabscreen.platform.base.service.machine.controller.PrintEvent;
import fabscreen.platform.base.service.machine.controller.PrintEventState;
import fabscreen.platform.base.service.machine.structure.print.BatchBufferInfo;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class MockPrintStateMachine {
    private PrintEvent event;
    MachineOperationStatus status;

    private PublishSubject<BatchBufferInfo> mPrintGcodeRequestSubject = PublishSubject.create();


    MockPrintStateMachine() {
    }

    public Observable<BatchBufferInfo> getPrintGcodeRequestObservable() {
        return mPrintGcodeRequestSubject.hide();
    }

    void changeMachineStatus(MachineOperationStatus intentStatus) {
        status = intentStatus;
    }


    enum MachineEventResult {
        PRINT_FINISH, // Ordinal start from 0
        G_CODE_PAUSED,
        G_CODE_FILAMENT_REPLACE_TRIGGERED,
        FILAMENT_SENSOR_TRIGGERED,
        MOTOR_STALL_DETECTION_TRIGGERED,
        TEMPERATURE_PROTECTION_TRIGGERED,
        RECEIVED_PRINT_NUMBER_NOT_MATCHED,
        GET_PRINT_G_CODE_FAILED,
        EMERGENCY_STOP_TRIGGERED,
        TOOL_HEAD_RECOVER_FAILED,
        PARAMETER_INVALID_FOR_STOP,
        PRINT_STOPPED_FAILED,
        REQUEST_STOP_FROM_CLIENT, // Like notification for all clients
        PARAMETER_INVALID_FOR_PAUSE,
        ENVIRONMENT_SAVED_FAILED,
        PRINT_PAUSED_FAILED,
        ENCLOSURE_DOOR_OPEN_PAUSE_TRIGGERED,
        ERROR_EXTRUDER_DETECTED,
        FDM_EXTRUDER_RECOGNISED_FAILED,
        FDM_EXTRUDER_TEMPERATURE_ERROR,
        EXCEPTION_CAUSED_PRINT_PAUSED,
        REQUEST_RESUME_FROM_CLIENT,
        EXCEPTION_CAUSED_PRINT_STOPPED, // Ordinal 22
    }

}

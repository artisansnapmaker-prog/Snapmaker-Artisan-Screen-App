package fabscreen.platform.base.legacy.server.http.handlers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;

import com.orhanobut.logger.Logger;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.framework.body.JsonBody;
import com.yanzhenjie.andserver.framework.body.StreamBody;
import com.yanzhenjie.andserver.framework.body.StringBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.ResponseBody;
import com.yanzhenjie.andserver.http.multipart.MultipartFile;
import com.yanzhenjie.andserver.util.MediaType;
import com.yanzhenjie.andserver.util.StatusCode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fabscreen.platform.base.R;
import fabscreen.platform.base.helper.Md5Util;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.file.FabLocalFile;
import fabscreen.platform.base.lib.file.IFile;
import fabscreen.platform.base.lib.parser.IGcodeParser;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.model.HTTPEventBus;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.INetwork;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.IRouter;
import fabscreen.platform.base.service.machine.MachineInfo;
import fabscreen.platform.base.service.machine.MachineStatus;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.service.machine.controller.FDMController;
import fabscreen.platform.base.service.machine.controller.ErrorController;
import fabscreen.platform.base.service.machine.controller.MachineOperationStatus;
import fabscreen.platform.base.service.machine.controller.NewPrintController;
import fabscreen.platform.base.service.machine.controller.PrintEventState;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.entity.module.Enclosure;
import fabscreen.platform.base.service.machine.entity.module.HeatedBed;
import fabscreen.platform.base.service.machine.entity.parts.Extruder;
import fabscreen.platform.base.service.machine.entity.parts.Fan;
import fabscreen.platform.base.service.machine.entity.toolhead.FdmToolhead;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.service.machine.structure.prop.StringProp;
import fabscreen.platform.base.service.machine.structure.prop.UInt8Prop;
import fabscreen.platform.base.view.SuperToastHelper;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.SingleSubject;
import okio.Buffer;

@RestController
class OrcaRequestHandler {
    private static final String URI_LOCAL_FILE = "/api/files/local";
    private static final String URI_CHECK_VERSION = "/api/version";
    private static final String URI_DASHBOARD_STATUS = "/api/dashboard/status";
    private static final String URI_DASHBOARD_THUMBNAIL = "/api/dashboard/thumbnail";
    private static final String URI_DASHBOARD_PAUSE = "/api/dashboard/job/pause";
    private static final String URI_DASHBOARD_RESUME = "/api/dashboard/job/resume";
    private static final String URI_DASHBOARD_CANCEL = "/api/dashboard/job/cancel";
    private static final String URI_ROOT = "/";
    private static final String DASHBOARD_REQUEST_HEADER = "X-Artisan-Dashboard";
    private static final long TELEMETRY_REFRESH_INTERVAL_MS = 2_000L;
    private static final long ACTION_DEBOUNCE_MS = 1_000L;
    private static final long ACTION_TIMEOUT_MS = 20_000L;

    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final String mDashboardToken = UUID.randomUUID().toString();
    private final Object mThumbnailLock = new Object();
    private final Object mActionLock = new Object();
    private final Object mTelemetryLock = new Object();
    private volatile long mLastTelemetryRefreshAt;
    private long mLastActionAt;
    private long mNextActionId;
    private long mPendingActionId;
    private boolean mActionPending;
    private String mLastAction = "";
    private String mLastActionResult = "";
    private int mLastActionErrorCode;
    private long mLastActionCompletedAt;
    private Disposable mDashboardActionDisposable;
    private Runnable mDashboardActionTimeout;
    private Disposable mToolheadRefreshDisposable;
    private Disposable mBedRefreshDisposable;
    private Bitmap mCachedThumbnailBitmap;
    private byte[] mCachedThumbnailBytes;

    @PostMapping(path = URI_LOCAL_FILE)
    void uploadFile(HttpRequest request, HttpResponse response,
                    @RequestParam(name = "file") MultipartFile file) {
        String isNeedPrint = request.getParameter("print");
        if (isNeedPrint == null) {
            response.setStatus(HttpResponse.SC_BAD_REQUEST);
            response.setBody(new StringBody("parameter \"print\" not found"));
            return;
        }
        if (!MachineOperationStatus.SYSTEM_STATUS_IDLE.valueEquals(ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getPrintState()) && isNeedPrint.equals("true")) {
            response.setStatus(HttpResponse.SC_SERVICE_UNAVAILABLE);
            response.setBody(new StringBody("Machine is not in idle status"));
            return;
        }

        if (file.isEmpty()) {
            response.setBody(new StringBody("Empty file body"));
            response.setStatus(HttpResponse.SC_BAD_REQUEST);
            return;
        }

        if (file.getFilename() == null) {
            response.setBody(new StringBody("Empty file name"));
            response.setStatus(HttpResponse.SC_BAD_REQUEST);
            return;
        }
        String uploadFilename = file.getFilename().trim();
        if (uploadFilename.isEmpty()
                || uploadFilename.equals(".")
                || uploadFilename.equals("..")
                || uploadFilename.indexOf('/') >= 0
                || uploadFilename.indexOf('\\') >= 0
                || uploadFilename.indexOf('\0') >= 0) {
            response.setBody(new StringBody("Invalid file name"));
            response.setStatus(HttpResponse.SC_BAD_REQUEST);
            return;
        }
        ErrorController.EmergencyStopState emergencyStopState = ServiceContainer.getInstance().getService(IAppService.class).getEmergencyStopState();
        switch (emergencyStopState) {
            case EMERGENCY_STOP_STATE_RELEASE:
            case EMERGENCY_STOP_STATE_PRESS:
                response.setStatus(HttpResponse.SC_SERVICE_UNAVAILABLE);
                response.setBody(new StringBody("Machine is not in idle status"));
                Logger.d("Current in emergency stop state, stop receiving file.");
                return;
            case EMERGENCY_STOP_STATE_NORMAL:
            default:
                break;
        }

        File targetFile;
        try {
            File filesDir = ServiceContainer.getInstance()
                    .getService(IAppService.class)
                    .getFilesDir()
                    .getCanonicalFile();
            targetFile = new File(filesDir, uploadFilename).getCanonicalFile();
            if (!filesDir.equals(targetFile.getParentFile())) {
                response.setBody(new StringBody("Invalid file name"));
                response.setStatus(HttpResponse.SC_BAD_REQUEST);
                return;
            }
            file.transferTo(targetFile);
//            HTTPEventBus.getInstance().onReceiveFile(targetFile);
        } catch (IOException e) {
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        IAppService appService = ServiceContainer.getInstance().getService(IAppService.class);
        response.setBody(new StringBody("Upload successfully."));
        new SuperToastHelper.Builder()
                .setDrawable(R.drawable.ic_pic_a400_success_68x68)
                .setTitle(appService.getNowViewContext().getString(R.string.all_remote_toast_file_received))
                .setMessage(uploadFilename)
                .build()
                .showToast(appService.getNowViewContext());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        if (isNeedPrint.equals("true")) {
            Disposable sub = startPrint(targetFile).subscribe(success -> {
                if (success) {
                    response.setStatus(StatusCode.SC_OK);
                } else {
                    response.setStatus(StatusCode.SC_SERVICE_UNAVAILABLE);
                }
                countDownLatch.countDown();
            }, e -> {
                response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
                countDownLatch.countDown();
            });
            mDisposable.add(sub);
        } else {
            countDownLatch.countDown();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(URI_CHECK_VERSION)
    void checkVersion(HttpResponse response) {
        JSONObject versionResponse = new JSONObject();
        try {
            versionResponse.put("api", "0.1");
            versionResponse.put("server", "1.2.3");
            versionResponse.put("text", "OctoPrint 1.2.3/Screen Dummy");
            versionResponse.put("machineConnection", true);
            final ResponseBody body = new JsonBody(versionResponse);
            response.setBody(body);
            response.setStatus(StatusCode.SC_OK);
        } catch (JSONException e) {
            LogHelper.log(e);
        }
    }

    @GetMapping(value = URI_ROOT, produces = "text/html; charset=utf-8")
    void getRoot(HttpResponse response) {
        try {
            String html = loadDashboardAsset();
            response.setHeader("Content-Type", "text/html; charset=utf-8");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("Referrer-Policy", "no-referrer");
            response.setHeader("Content-Security-Policy",
                    "default-src 'self'; img-src 'self' data:; style-src 'unsafe-inline'; " +
                            "script-src 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'; " +
                            "base-uri 'none'; form-action 'none'");
            response.setStatus(StatusCode.SC_OK);
            response.setBody(new StringBody(html));
        } catch (IOException e) {
            LogHelper.log(e);
            response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
            response.setBody(new StringBody("<!doctype html><html><body>Dashboard unavailable.</body></html>"));
        }
    }

    @GetMapping(path = URI_DASHBOARD_STATUS)
    void getDashboardStatus(HttpResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        try {
            IMachine machine = ServiceContainer.getInstance().getService(IMachine.class);
            maybeRefreshTelemetry(machine);
            writeJson(response, StatusCode.SC_OK, buildDashboardStatus(machine));
        } catch (Exception e) {
            LogHelper.log(e);
            writeJsonError(response, StatusCode.SC_INTERNAL_SERVER_ERROR, "Unable to read printer status.");
        }
    }

    @GetMapping(path = URI_DASHBOARD_THUMBNAIL)
    void getDashboardThumbnail(HttpResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");

        try {
            IMachine machine = ServiceContainer.getInstance().getService(IMachine.class);
            MachineInfo info = machine.getMachineInfoSubjectHolder().getValue();
            int state = machine.getNewPrintController().getPrintState();
            if (info == null || info.workType != IMachine.WorkType.FDM || !isJobState(state)) {
                response.setStatus(StatusCode.SC_NOT_FOUND);
                return;
            }

            Bitmap bitmap = ServiceContainer.getInstance().getService(IGcodeParser.class).getGcodeThumbnail();
            if (bitmap == null || bitmap.isRecycled()) {
                response.setStatus(StatusCode.SC_NOT_FOUND);
                return;
            }

            byte[] png;
            synchronized (mThumbnailLock) {
                if (bitmap != mCachedThumbnailBitmap || mCachedThumbnailBytes == null) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        return;
                    }
                    mCachedThumbnailBitmap = bitmap;
                    mCachedThumbnailBytes = output.toByteArray();
                }
                png = mCachedThumbnailBytes;
            }

            response.setStatus(StatusCode.SC_OK);
            response.setBody(new StreamBody(
                    new ByteArrayInputStream(png),
                    png.length,
                    MediaType.IMAGE_PNG
            ));
        } catch (Exception e) {
            LogHelper.log(e);
            response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = URI_DASHBOARD_PAUSE)
    void pauseDashboardJob(HttpRequest request, HttpResponse response) {
        handleDashboardJobAction("pause", request, response);
    }

    @PostMapping(path = URI_DASHBOARD_RESUME)
    void resumeDashboardJob(HttpRequest request, HttpResponse response) {
        handleDashboardJobAction("resume", request, response);
    }

    @PostMapping(path = URI_DASHBOARD_CANCEL)
    void cancelDashboardJob(HttpRequest request, HttpResponse response) {
        handleDashboardJobAction("cancel", request, response);
    }

    private JSONObject buildDashboardStatus(IMachine machine) throws JSONException {
        MachineInfo info = machine.getMachineInfoSubjectHolder().getValue();
        MachineStatus machineStatus = machine.getMachineStatusSubjectHolder().getValue();
        NewPrintController printController = machine.getNewPrintController();
        IAppService appService = ServiceContainer.getInstance().getService(IAppService.class);
        IPrintWorkspace workspace = ServiceContainer.getInstance().getService(IPrintWorkspace.class);
        IGcodeParser parser = ServiceContainer.getInstance().getService(IGcodeParser.class);

        boolean machineInfoReady = info != null && info.moduleList != null;
        boolean isFdm = machineInfoReady && info.workType == IMachine.WorkType.FDM;
        boolean connected = machineStatus != null && machineStatus.connected;
        int state = printController.getPrintState();
        FdmToolhead.FdmToolheadStatus fdmStatus = getFdmStatus(machine, isFdm);
        HeatedBed heatedBed = findModule(info, HeatedBed.class);
        Enclosure enclosure = findModule(info, Enclosure.class);
        boolean enclosureDoorOpen = enclosure != null
                && enclosure.getEnclosureStatusValue() != null
                && enclosure.getEnclosureStatusValue().isDoorOpen();
        boolean activeJob = isJobState(state);
        boolean filamentRunout = connected
                && activeJob
                && (printController.isFilamentRunout()
                || hasActiveFilamentRunout(fdmStatus, true));
        boolean resumeFilamentSafe = state != MachineOperationStatus.SYSTEM_STATUS_PAUSED.value()
                || isResumeFilamentSafe(machine);
        ErrorController.EmergencyStopState emergencyStopState = appService.getEmergencyStopState();
        boolean emergencyStop = emergencyStopState != ErrorController.EmergencyStopState.EMERGENCY_STOP_STATE_NORMAL;

        JSONObject root = new JSONObject();
        root.put("timestamp", System.currentTimeMillis());

        JSONObject machineJson = new JSONObject();
        String machineName = ServiceContainer.getInstance()
                .getService(IPreferences.class)
                .getHelper()
                .getMachineName();
        machineJson.put("name", emptyIfNull(machineName));
        machineJson.put("infoReady", machineInfoReady);
        machineJson.put("model", machineInfoReady ? emptyIfNull(info.getModelName()) : "");
        machineJson.put("workType", info == null || info.workType == null ? "NONE" : info.workType.name());
        machineJson.put("isFdm", isFdm);
        machineJson.put("connected", connected);
        machineJson.put("homed", machineStatus != null && machineStatus.isHomed);
        machineJson.put("homing", machineStatus != null && machineStatus.isHoming);
        machineJson.put("state", connected ? stateName(state) : "Disconnected");
        machineJson.put("stateCode", state);
        machineJson.put("ip", getIPAddress());
        machineJson.put("mac", emptyIfNull(getMacAddr()));
        machineJson.put("controllerFirmware", machineInfoReady ? emptyIfNull(info.controllerFWVersion) : "");
        machineJson.put("packageVersion", emptyIfNull(getCurrentVersion()));
        machineJson.put("appVersion", emptyIfNull(appService.getApp().getAppVersionName()));
        machineJson.put("serial", !machineInfoReady
                ? ""
                : firstNonEmpty(info.productSerialNumber, info.burnSerialNumber));
        machineJson.put("productId", machineInfoReady ? info.productId : JSONObject.NULL);
        machineJson.put("headType", machineInfoReady ? info.headType : JSONObject.NULL);

        JSONObject position = new JSONObject();
        Vector currentPosition = machineStatus == null ? null : machineStatus.currentPosition;
        position.put("x", currentPosition == null ? JSONObject.NULL : rounded(currentPosition.getX(), 2));
        position.put("y", currentPosition == null ? JSONObject.NULL : rounded(currentPosition.getY(), 2));
        position.put("z", currentPosition == null ? JSONObject.NULL : rounded(currentPosition.getZ(), 2));
        machineJson.put("position", position);
        machineJson.put("modules", buildModulesJson(info));
        root.put("machine", machineJson);

        root.put("toolhead", buildToolheadJson(machine, fdmStatus, isFdm, connected));
        root.put("bed", buildBedJson(info, heatedBed, connected));
        root.put("job", buildJobJson(isFdm, state, printController, workspace, parser));

        JSONObject safety = new JSONObject();
        safety.put("emergencyStop", emergencyStop);
        safety.put("emergencyStopState", emergencyStopName(emergencyStopState));
        safety.put("enclosureDoorOpen", enclosureDoorOpen);
        safety.put("filamentRunout", filamentRunout);
        safety.put("filamentTelemetryAvailable", hasActiveExtruderTelemetry(fdmStatus));
        root.put("safety", safety);

        boolean actionPending;
        String lastAction;
        String lastActionResult;
        int lastActionErrorCode;
        long lastActionCompletedAt;
        synchronized (mActionLock) {
            actionPending = mActionPending;
            lastAction = mLastAction;
            lastActionResult = mLastActionResult;
            lastActionErrorCode = mLastActionErrorCode;
            lastActionCompletedAt = mLastActionCompletedAt;
        }

        boolean baseControlsAvailable = connected && isFdm && !emergencyStop && !actionPending;
        JSONObject controls = new JSONObject();
        controls.put("canPause", baseControlsAvailable
                && state == MachineOperationStatus.SYSTEM_STATUS_PRINTING.value());
        controls.put("canResume", baseControlsAvailable
                && state == MachineOperationStatus.SYSTEM_STATUS_PAUSED.value()
                && !enclosureDoorOpen
                && !filamentRunout
                && resumeFilamentSafe);
        controls.put("canCancel", baseControlsAvailable
                && (state == MachineOperationStatus.SYSTEM_STATUS_PRINTING.value()
                || state == MachineOperationStatus.SYSTEM_STATUS_PAUSED.value()));
        controls.put("commandPending", actionPending);
        controls.put("blockedReason", actionPending
                ? "Waiting for the printer"
                : controlBlockedReason(
                connected,
                isFdm,
                emergencyStop,
                enclosureDoorOpen,
                filamentRunout || !resumeFilamentSafe,
                state
        ));
        JSONObject lastCommand = new JSONObject();
        lastCommand.put("action", lastAction);
        lastCommand.put("result", lastActionResult);
        lastCommand.put("errorCode", lastActionErrorCode);
        lastCommand.put("completedAt", lastActionCompletedAt);
        controls.put("lastCommand", lastCommand);
        root.put("controls", controls);

        return root;
    }

    private JSONObject buildToolheadJson(
            IMachine machine,
            FdmToolhead.FdmToolheadStatus fdmStatus,
            boolean isFdm,
            boolean connected
    ) throws JSONException {
        JSONObject toolhead = new JSONObject();
        FDMController fdmController = machine.getFDMController();
        boolean available = isFdm
                && fdmController != null
                && fdmController.getToolHeadCounts() > 0
                && fdmController.getFdmToolhead(0) != null;

        List<Extruder> extruders = fdmStatus == null || fdmStatus.getExtruderList() == null
                ? Collections.emptyList()
                : snapshotList(fdmStatus.getExtruderList());
        Extruder left = findExtruder(extruders, Extruder.EXTRUDER_LEFT);
        Extruder right = findExtruder(extruders, Extruder.EXTRUDER_RIGHT);
        boolean hasLoadedTelemetry = isExtruderTelemetryReady(left)
                || isExtruderTelemetryReady(right);
        boolean telemetryAvailable = connected && hasLoadedTelemetry;

        toolhead.put("available", available);
        toolhead.put("telemetryAvailable", telemetryAvailable);
        toolhead.put("stale", available && !connected && hasLoadedTelemetry);
        toolhead.put("active", telemetryAvailable && fdmStatus.isActive());
        toolhead.put("type", available ? fdmController.getHeadType() : JSONObject.NULL);

        JSONArray nozzles = new JSONArray();
        nozzles.put(buildNozzleJson(
                telemetryAvailable ? left : null,
                Extruder.EXTRUDER_LEFT,
                "Left"
        ));
        nozzles.put(buildNozzleJson(
                telemetryAvailable ? right : null,
                Extruder.EXTRUDER_RIGHT,
                "Right"
        ));
        toolhead.put("nozzles", nozzles);

        JSONArray fans = new JSONArray();
        List<Fan> fanList = fdmStatus == null || fdmStatus.getFanList() == null
                ? Collections.emptyList()
                : snapshotList(fdmStatus.getFanList());
        for (Fan fan : fanList) {
            if (fan == null || !telemetryAvailable) continue;
            JSONObject fanJson = new JSONObject();
            int speedPwm = fan.getSpeedLevel();
            fanJson.put("id", fan.getId());
            fanJson.put("type", fan.getType());
            fanJson.put("name", fanName(fan.getId()));
            fanJson.put("speedPwm", speedPwm);
            fanJson.put("speedPercent", Math.max(0, Math.min(100, Math.round(speedPwm / 255f * 100f))));
            fans.put(fanJson);
        }
        toolhead.put("fans", fans);
        return toolhead;
    }

    private JSONObject buildNozzleJson(Extruder extruder, int id, String side) throws JSONException {
        JSONObject nozzle = new JSONObject();
        boolean available = extruder != null
                && (extruder.getDiameter() > 0f
                || extruder.getTemperature() > 0f
                || extruder.getTargetTemperature() > 0f);
        nozzle.put("id", id);
        nozzle.put("side", side);
        nozzle.put("available", available);

        if (!available) {
            nozzle.put("diameterMm", JSONObject.NULL);
            nozzle.put("currentC", JSONObject.NULL);
            nozzle.put("targetC", JSONObject.NULL);
            nozzle.put("heating", false);
            nozzle.put("atTarget", false);
            nozzle.put("active", false);
            nozzle.put("model", "");
            nozzle.put("modelCode", JSONObject.NULL);
            nozzle.put("filamentMissing", false);
            nozzle.put("filamentRunout", false);
            nozzle.put("detectionEnabled", false);
            nozzle.put("state", JSONObject.NULL);
            return nozzle;
        }

        float current = extruder.getTemperature();
        float target = extruder.getTargetTemperature();
        boolean detectionEnabled = extruder.getFilamentDetectionStatus() == 0;
        nozzle.put("diameterMm", extruder.getDiameter() > 0f
                ? rounded(extruder.getDiameter(), 2)
                : JSONObject.NULL);
        nozzle.put("currentC", rounded(current, 1));
        nozzle.put("targetC", rounded(target, 1));
        nozzle.put("heating", target > 0f && current + 2f < target);
        nozzle.put("atTarget", target > 0f && Math.abs(current - target) <= 5f);
        nozzle.put("active", extruder.getState() == 1);
        nozzle.put("model", extruderModelName(extruder.getModel()));
        nozzle.put("modelCode", extruder.getModel());
        boolean filamentMissing = detectionEnabled && extruder.getFilamentStatus();
        nozzle.put("filamentMissing", filamentMissing);
        nozzle.put("filamentRunout", extruder.getState() == 1 && filamentMissing);
        nozzle.put("detectionEnabled", detectionEnabled);
        nozzle.put("state", extruder.getState());
        return nozzle;
    }

    private JSONObject buildBedJson(
            MachineInfo info,
            HeatedBed heatedBed,
            boolean connected
    ) throws JSONException {
        JSONObject bedJson = new JSONObject();
        boolean available = info != null && info.isHeatedBedAvailable && heatedBed != null;
        HeatedBed.HeatedBedStatus status = available
                ? heatedBed.getHeatedBedStatusSubjectHolder().getValue()
                : null;
        List<HeatedBed.ZoneInfo> zones = status == null || status.getZoneList() == null
                ? Collections.emptyList()
                : snapshotList(status.getZoneList());
        HeatedBed.ZoneInfo inner = findBedZone(zones, 0);
        HeatedBed.ZoneInfo outer = findBedZone(zones, 1);
        boolean hasLoadedTelemetry = isBedZoneTelemetryReady(inner)
                || isBedZoneTelemetryReady(outer);
        boolean telemetryAvailable = connected && hasLoadedTelemetry;
        int mode = status == null ? -1 : status.getWorkMode();
        boolean modeValid = mode == HeatedBed.HeatedBedStatus.HEATED_BED_STATUS_WORK_MODE_INNER
                || mode == HeatedBed.HeatedBedStatus.HEATED_BED_STATUS_WORK_MODE_WHOLE;
        boolean wholeBed = telemetryAvailable
                && mode == HeatedBed.HeatedBedStatus.HEATED_BED_STATUS_WORK_MODE_WHOLE;

        bedJson.put("available", available);
        bedJson.put("telemetryAvailable", telemetryAvailable);
        bedJson.put("stale", available && !connected && hasLoadedTelemetry);
        bedJson.put("modeCode", telemetryAvailable && modeValid ? mode : JSONObject.NULL);
        bedJson.put("mode", !available
                ? "Unavailable"
                : !telemetryAvailable || !modeValid
                ? "Unknown"
                : wholeBed ? "Whole bed (inner + outer)" : "Inner zone");

        JSONArray zonesJson = new JSONArray();
        zonesJson.put(buildBedZoneJson(
                telemetryAvailable ? inner : null,
                0,
                "Inner",
                telemetryAvailable
        ));
        zonesJson.put(buildBedZoneJson(
                telemetryAvailable ? outer : null,
                1,
                "Outer",
                telemetryAvailable && wholeBed
        ));
        bedJson.put("zones", zonesJson);
        return bedJson;
    }

    private JSONObject buildBedZoneJson(
            HeatedBed.ZoneInfo zone,
            int id,
            String name,
            boolean selected
    ) throws JSONException {
        JSONObject zoneJson = new JSONObject();
        zoneJson.put("id", id);
        zoneJson.put("name", name);
        zoneJson.put("available", zone != null);
        zoneJson.put("selected", selected);
        if (zone == null) {
            zoneJson.put("currentC", JSONObject.NULL);
            zoneJson.put("targetC", JSONObject.NULL);
            zoneJson.put("heating", false);
        } else {
            float current = zone.getCurrentTemperature();
            int target = zone.getTargetTemperature();
            zoneJson.put("currentC", rounded(current, 1));
            zoneJson.put("targetC", target);
            zoneJson.put("heating", selected && target > 0 && current + 2f < target);
        }
        return zoneJson;
    }

    private JSONObject buildJobJson(
            boolean isFdm,
            int state,
            NewPrintController controller,
            IPrintWorkspace workspace,
            IGcodeParser parser
    ) throws JSONException {
        JSONObject job = new JSONObject();
        boolean active = isFdm && isJobState(state);
        if (!active) {
            job.put("active", false);
            job.put("state", stateName(state));
            job.put("filename", "");
            job.put("fileSizeBytes", JSONObject.NULL);
            job.put("progressPercent", 0);
            job.put("progressRatio", 0);
            job.put("elapsedSeconds", 0);
            job.put("estimatedSeconds", 0);
            job.put("remainingSeconds", 0);
            job.put("totalLines", 0);
            job.put("currentLine", 0);
            job.put("printModeCode", JSONObject.NULL);
            job.put("printMode", "");
            job.put("thumbnailAvailable", false);
            job.put("layers", JSONObject.NULL);
            job.put("layerHeightMm", JSONObject.NULL);
            job.put("materialWeightG", JSONObject.NULL);
            job.put("materialLengthM", JSONObject.NULL);
            job.put("nozzleLeftMm", JSONObject.NULL);
            job.put("nozzleRightMm", JSONObject.NULL);
            job.put("materials", new JSONArray());
            JSONObject emptyWorkSize = new JSONObject();
            emptyWorkSize.put("x", JSONObject.NULL);
            emptyWorkSize.put("y", JSONObject.NULL);
            job.put("workSize", emptyWorkSize);
            return job;
        }

        float rawProgress = controller.getProgress();
        float progress = Float.isNaN(rawProgress) || Float.isInfinite(rawProgress)
                ? 0f
                : Math.max(0f, Math.min(1f, rawProgress));
        int elapsedSeconds = Math.max(0, controller.getTickCounter().getCount());
        float rawEstimated = workspace.getEstimatedTime();
        int estimatedSeconds = Float.isNaN(rawEstimated) || Float.isInfinite(rawEstimated)
                ? 0
                : Math.max(0, Math.round(rawEstimated));
        int remainingSeconds = active
                ? Math.max(0, Math.round(
                (1f - progress) * elapsedSeconds
                        + (1f - progress) * (1f - progress) * estimatedSeconds
        ))
                : 0;
        int totalLines = controller.getTotalLines();
        if (totalLines <= 0) totalLines = workspace.getFileTotalLineCount();
        int currentLine = totalLines > 0 ? Math.round(progress * totalLines) : 0;
        IFile printFile = workspace.getPrintFile();

        job.put("active", active);
        job.put("state", stateName(state));
        job.put("filename", emptyIfNull(workspace.getFileName()));
        job.put("fileSizeBytes", printFile == null ? JSONObject.NULL : printFile.length());
        job.put("progressPercent", rounded(progress * 100f, 1));
        job.put("progressRatio", rounded(progress, 4));
        job.put("elapsedSeconds", elapsedSeconds);
        job.put("estimatedSeconds", estimatedSeconds);
        job.put("remainingSeconds", remainingSeconds);
        job.put("totalLines", Math.max(0, totalLines));
        job.put("currentLine", currentLine);
        job.put("printModeCode", workspace.getPrintMode());
        job.put("printMode", printModeName(workspace.getPrintMode()));
        job.put("thumbnailAvailable", active && parser.getGcodeThumbnail() != null);

        job.put("layers", optionalParserNumber(parser.getLayerNumber()));
        job.put("layerHeightMm", optionalParserNumber(parser.getLayerHeight()));
        job.put("materialWeightG", optionalParserNumber(parser.getMaterialWeight()));
        job.put("materialLengthM", optionalParserNumber(parser.getMaterialLength()));
        job.put("nozzleLeftMm", optionalParserNumber(parser.getNozzle_0_Diameter()));
        job.put("nozzleRightMm", optionalParserNumber(parser.getNozzle_1_Diameter()));

        JSONArray materials = new JSONArray();
        addNonEmpty(materials, parser.getMaterial_0());
        addNonEmpty(materials, parser.getMaterial_1());
        job.put("materials", materials);

        JSONObject workSize = new JSONObject();
        workSize.put("x", optionalParserNumber(parser.getWorkSizeX()));
        workSize.put("y", optionalParserNumber(parser.getWorkSizeY()));
        job.put("workSize", workSize);
        return job;
    }

    private JSONArray buildModulesJson(MachineInfo info) throws JSONException {
        JSONArray result = new JSONArray();
        if (info == null || info.moduleList == null) return result;

        List<Module> modules = snapshotList(info.moduleList);
        for (Module module : modules) {
            if (module == null || module.getModuleInfo() == null) continue;
            Module.ModuleInfo moduleInfo = module.getModuleInfo();
            JSONObject json = new JSONObject();
            json.put("id", moduleInfo.getModuleId());
            json.put("index", moduleInfo.getModuleIndex());
            json.put("state", moduleInfo.getModuleState());
            json.put("serial", moduleInfo.getSn());
            json.put("hardwareVersion", moduleInfo.hardwareVersionProp.getValue());
            json.put("firmwareVersion", emptyIfNull(moduleInfo.getFirmwareVersion()));
            result.put(json);
        }
        return result;
    }

    private void handleDashboardJobAction(
            final String action,
            HttpRequest request,
            HttpResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");

        if (!isDashboardRequestAuthorized(request)) {
            writeJsonError(response, StatusCode.SC_FORBIDDEN, "Dashboard authorization failed.");
            return;
        }

        try {
            IMachine machine = ServiceContainer.getInstance().getService(IMachine.class);
            IAppService appService = ServiceContainer.getInstance().getService(IAppService.class);
            MachineInfo info = machine.getMachineInfoSubjectHolder().getValue();
            MachineStatus machineStatus = machine.getMachineStatusSubjectHolder().getValue();
            NewPrintController controller = machine.getNewPrintController();
            int state = controller.getPrintState();

            if (machineStatus == null || !machineStatus.connected) {
                writeJsonError(response, StatusCode.SC_CONFLICT, "Printer is disconnected.");
                return;
            }
            if (info == null || info.workType != IMachine.WorkType.FDM) {
                writeJsonError(response, StatusCode.SC_CONFLICT, "3D printing mode is not active.");
                return;
            }
            if (appService.getEmergencyStopState()
                    != ErrorController.EmergencyStopState.EMERGENCY_STOP_STATE_NORMAL) {
                writeJsonError(response, StatusCode.SC_CONFLICT, "Emergency stop is active.");
                return;
            }

            if ("pause".equals(action)
                    && state != MachineOperationStatus.SYSTEM_STATUS_PRINTING.value()) {
                writeJsonError(response, StatusCode.SC_CONFLICT, "The job is not in a pausable state.");
                return;
            }
            if ("resume".equals(action)) {
                if (state != MachineOperationStatus.SYSTEM_STATUS_PAUSED.value()) {
                    writeJsonError(response, StatusCode.SC_CONFLICT, "The job is not paused.");
                    return;
                }
                Enclosure enclosure = findModule(info, Enclosure.class);
                if (enclosure != null
                        && enclosure.getEnclosureStatusValue() != null
                        && enclosure.getEnclosureStatusValue().isDoorOpen()) {
                    writeJsonError(response, StatusCode.SC_CONFLICT, "Close the enclosure door before resuming.");
                    return;
                }
                if (!isResumeFilamentSafe(machine)) {
                    writeJsonError(response, StatusCode.SC_CONFLICT,
                            "Resolve the filament condition and wait for live telemetry before resuming.");
                    return;
                }
            }
            if ("cancel".equals(action)
                    && state != MachineOperationStatus.SYSTEM_STATUS_PRINTING.value()
                    && state != MachineOperationStatus.SYSTEM_STATUS_PAUSED.value()) {
                writeJsonError(response, StatusCode.SC_CONFLICT, "The job is not in a cancellable state.");
                return;
            }

            final long actionId;
            long now = SystemClock.elapsedRealtime();
            synchronized (mActionLock) {
                if (mActionPending) {
                    writeJsonError(response, StatusCode.SC_CONFLICT, "Another job command is in progress.");
                    return;
                }
                if (now - mLastActionAt < ACTION_DEBOUNCE_MS) {
                    writeJsonError(response, StatusCode.SC_CONFLICT, "Another job command is still settling.");
                    return;
                }
                mLastActionAt = now;
                mActionPending = true;
                actionId = ++mNextActionId;
                mPendingActionId = actionId;
                mLastAction = action;
                mLastActionResult = "pending";
                mLastActionErrorCode = 0;
            }

            final Runnable timeoutRunnable =
                    () -> finishDashboardAction(actionId, action, "timed_out", -1);
            synchronized (mActionLock) {
                mDashboardActionTimeout = timeoutRunnable;
            }
            if (!mMainHandler.postDelayed(timeoutRunnable, ACTION_TIMEOUT_MS)) {
                finishDashboardAction(actionId, action, "queue_failed", -1);
                writeJsonError(response, StatusCode.SC_INTERNAL_SERVER_ERROR, "Unable to queue the job command.");
                return;
            }

            boolean posted = mMainHandler.post(() -> {
                try {
                    synchronized (mActionLock) {
                        if (!mActionPending || mPendingActionId != actionId) return;
                    }

                    MachineStatus latestMachineStatus = machine.getMachineStatusSubjectHolder().getValue();
                    MachineInfo latestInfo = machine.getMachineInfoSubjectHolder().getValue();
                    if (latestMachineStatus == null
                            || !latestMachineStatus.connected
                            || latestInfo == null
                            || latestInfo.moduleList == null
                            || latestInfo.workType != IMachine.WorkType.FDM
                            || appService.getEmergencyStopState()
                            != ErrorController.EmergencyStopState.EMERGENCY_STOP_STATE_NORMAL) {
                        finishDashboardAction(actionId, action, "safety_changed", -1);
                        return;
                    }

                    if ("resume".equals(action)) {
                        Enclosure latestEnclosure = findModule(latestInfo, Enclosure.class);
                        boolean doorOpen = latestEnclosure != null
                                && latestEnclosure.getEnclosureStatusValue() != null
                                && latestEnclosure.getEnclosureStatusValue().isDoorOpen();
                        if (doorOpen || !isResumeFilamentSafe(machine)) {
                            finishDashboardAction(actionId, action, "safety_changed", -1);
                            return;
                        }
                    }

                    int latestState = controller.getPrintState();
                    boolean stateStillValid = ("pause".equals(action)
                            && latestState == MachineOperationStatus.SYSTEM_STATUS_PRINTING.value())
                            || ("resume".equals(action)
                            && latestState == MachineOperationStatus.SYSTEM_STATUS_PAUSED.value())
                            || ("cancel".equals(action)
                            && (latestState == MachineOperationStatus.SYSTEM_STATUS_PRINTING.value()
                            || latestState == MachineOperationStatus.SYSTEM_STATUS_PAUSED.value()));
                    if (!stateStillValid) {
                        finishDashboardAction(actionId, action, "state_changed", -1);
                        return;
                    }

                    Disposable eventDisposable = controller.getPrintEventObservable()
                            .observeOn(AndroidSchedulers.mainThread())
                            .filter(event -> eventMatchesAction(action, event.getPrintEventState()))
                            .take(1)
                            .subscribe(event -> {
                                PrintEventState eventState = event.getPrintEventState();
                                boolean success = eventState == PrintEventState.PAUSE_SUCCESS
                                        || eventState == PrintEventState.RESUME_SUCCESS
                                        || eventState == PrintEventState.STOP_SUCCESS;
                                finishDashboardAction(
                                        actionId,
                                        action,
                                        success ? "succeeded" : "failed",
                                        event.getErrorCode()
                                );
                            }, error -> {
                                LogHelper.log(error);
                                finishDashboardAction(actionId, action, "failed", -1);
                            });
                    synchronized (mActionLock) {
                        if (mActionPending && mPendingActionId == actionId) {
                            mDashboardActionDisposable = eventDisposable;
                        } else {
                            eventDisposable.dispose();
                        }
                    }

                    boolean commandStarted;
                    if ("pause".equals(action)) {
                        commandStarted = controller.pause();
                    } else if ("resume".equals(action)) {
                        commandStarted = controller.resume();
                    } else {
                        commandStarted = controller.stop();
                    }
                    if (!commandStarted) {
                        finishDashboardAction(actionId, action, "busy", 254);
                    }
                } catch (Exception e) {
                    LogHelper.log(e);
                    finishDashboardAction(actionId, action, "failed", -1);
                }
            });

            if (!posted) {
                finishDashboardAction(actionId, action, "queue_failed", -1);
                writeJsonError(response, StatusCode.SC_INTERNAL_SERVER_ERROR, "Unable to queue the job command.");
                return;
            }

            JSONObject result = new JSONObject();
            result.put("ok", true);
            result.put("accepted", true);
            result.put("action", action);
            writeJson(response, StatusCode.SC_ACCEPTED, result);
        } catch (Exception e) {
            LogHelper.log(e);
            writeJsonError(response, StatusCode.SC_INTERNAL_SERVER_ERROR, "Unable to control the print job.");
        }
    }

    private boolean eventMatchesAction(String action, PrintEventState state) {
        if ("pause".equals(action)) {
            return state == PrintEventState.PAUSE_SUCCESS || state == PrintEventState.PAUSE_FAIL;
        }
        if ("resume".equals(action)) {
            return state == PrintEventState.RESUME_SUCCESS || state == PrintEventState.RESUME_FAIL;
        }
        return state == PrintEventState.STOP_SUCCESS || state == PrintEventState.STOP_FAIL;
    }

    private void finishDashboardAction(
            long actionId,
            String action,
            String result,
            int errorCode
    ) {
        Disposable disposable = null;
        Runnable timeout = null;
        synchronized (mActionLock) {
            if (!mActionPending || mPendingActionId != actionId) return;
            mActionPending = false;
            mPendingActionId = 0L;
            mLastAction = action;
            mLastActionResult = result;
            mLastActionErrorCode = errorCode;
            mLastActionCompletedAt = System.currentTimeMillis();
            disposable = mDashboardActionDisposable;
            mDashboardActionDisposable = null;
            timeout = mDashboardActionTimeout;
            mDashboardActionTimeout = null;
        }
        if (disposable != null && !disposable.isDisposed()) disposable.dispose();
        if (timeout != null) mMainHandler.removeCallbacks(timeout);
    }

    private void maybeRefreshTelemetry(IMachine machine) {
        MachineInfo info = machine.getMachineInfoSubjectHolder().getValue();
        MachineStatus status = machine.getMachineStatusSubjectHolder().getValue();
        if (info == null
                || info.workType != IMachine.WorkType.FDM
                || status == null
                || !status.connected) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        synchronized (mTelemetryLock) {
            if (now - mLastTelemetryRefreshAt < TELEMETRY_REFRESH_INTERVAL_MS) return;
            mLastTelemetryRefreshAt = now;
        }

        mMainHandler.post(() -> {
            try {
                FDMController fdmController = machine.getFDMController();
                if (fdmController != null && fdmController.getToolHeadCounts() > 0) {
                    FdmToolhead toolhead = fdmController.getFdmToolhead(0);
                    if (toolhead != null
                            && (mToolheadRefreshDisposable == null
                            || mToolheadRefreshDisposable.isDisposed())) {
                        mToolheadRefreshDisposable = toolhead.requestInfo().subscribe(ignored -> {
                        }, LogHelper::log);
                    }
                }

                MachineInfo latestInfo = machine.getMachineInfoSubjectHolder().getValue();
                HeatedBed heatedBed = findModule(latestInfo, HeatedBed.class);
                if (heatedBed != null
                        && (mBedRefreshDisposable == null || mBedRefreshDisposable.isDisposed())) {
                    mBedRefreshDisposable = heatedBed.requestInfo().subscribe(ignored -> {
                    }, LogHelper::log);
                }
            } catch (Exception e) {
                LogHelper.log(e);
            }
        });
    }

    private FdmToolhead.FdmToolheadStatus getFdmStatus(IMachine machine, boolean isFdm) {
        if (!isFdm) return null;
        FDMController controller = machine.getFDMController();
        if (controller == null || controller.getToolHeadCounts() <= 0) return null;
        FdmToolhead toolhead = controller.getFdmToolhead(0);
        if (toolhead == null || toolhead.getToolheadStatusSubjectHolder() == null) return null;
        return toolhead.getToolheadStatusSubjectHolder().getValue();
    }

    private boolean hasActiveFilamentRunout(
            FdmToolhead.FdmToolheadStatus status,
            boolean jobActive
    ) {
        if (!jobActive || status == null || status.getExtruderList() == null) return false;
        List<Extruder> extruders = snapshotList(status.getExtruderList());
        for (Extruder extruder : extruders) {
            if (extruder != null
                    && extruder.getState() == 1
                    && extruder.getFilamentDetectionStatus() == 0
                    && extruder.getFilamentStatus()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasActiveExtruderTelemetry(FdmToolhead.FdmToolheadStatus status) {
        if (status == null || status.getExtruderList() == null) return false;
        for (Extruder extruder : snapshotList(status.getExtruderList())) {
            if (extruder != null
                    && extruder.getState() == 1
                    && isExtruderTelemetryReady(extruder)) {
                return true;
            }
        }
        return false;
    }

    private boolean isResumeFilamentSafe(IMachine machine) {
        NewPrintController controller = machine.getNewPrintController();
        if (controller.isFilamentRunout()) return false;
        FdmToolhead.FdmToolheadStatus status = getFdmStatus(machine, true);
        if (!hasActiveExtruderTelemetry(status)) return false;
        return !hasActiveFilamentRunout(status, true);
    }

    private boolean isJobState(int state) {
        return state == MachineOperationStatus.SYSTEM_STATUS_STARTING.value()
                || state == MachineOperationStatus.SYSTEM_STATUS_PRINTING.value()
                || state == MachineOperationStatus.SYSTEM_STATUS_PAUSING.value()
                || state == MachineOperationStatus.SYSTEM_STATUS_PAUSED.value()
                || state == MachineOperationStatus.SYSTEM_STATUS_STOPING.value()
                || state == MachineOperationStatus.SYSTEM_STATUS_FINISHING.value()
                || state == MachineOperationStatus.SYSTEM_STATUS_RECOVERING.value()
                || state == MachineOperationStatus.SYSTEM_STATUS_RESUMING.value();
    }

    private String stateName(int state) {
        MachineOperationStatus status = MachineOperationStatus.valueOf(state);
        if (status == null) return "Unknown (" + state + ")";

        switch (status) {
            case SYSTEM_STATUS_IDLE:
                return "Ready";
            case SYSTEM_STATUS_STARTING:
                return "Starting";
            case SYSTEM_STATUS_PRINTING:
                return "Printing";
            case SYSTEM_STATUS_PAUSING:
                return "Pausing";
            case SYSTEM_STATUS_PAUSED:
                return "Paused";
            case SYSTEM_STATUS_STOPING:
                return "Cancelling";
            case SYSTEM_STATUS_STOPED:
                return "Cancelled";
            case SYSTEM_STATUS_FINISHING:
                return "Finishing";
            case SYSTEM_STATUS_COMPLETED:
                return "Completed";
            case SYSTEM_STATUS_RECOVERING:
                return "Recovering";
            case SYSTEM_STATUS_RESUMING:
                return "Resuming";
            case SYSTEM_STATUS_EMERGENCY_STOP:
                return "Emergency stop";
            case SYSTEM_STATUS_POWER_LOSS:
                return "Power-loss recovery";
            case SYSTEM_STATUS_REPLACE_MODE:
                return "Tool replacement";
            case SYSTEM_STATUS_XY_CALIBRATING:
            case SYSTEM_STATUS_XY_CALIBRATING_PRINTING:
            case SYSTEM_STATUS_AUTO_BEDLEVEL:
            case SYSTEM_STATUS_MANUAL_BEDLEVEL:
            case SYSTEM_STATUS_AUTO_BED_DETECTION:
            case SYSTEM_STATUS_MANUAL_BED_DETECTION:
            case SYSTEM_STATUS_PROBE_SENSOR_CALIBRATION:
                return "Calibration";
            case SYSTEM_STATUS_APP_UPGRADE:
            case SYSTEM_STATUS_MODULE_UPGRADE:
                return "Updating";
            default:
                return "Busy";
        }
    }

    private String controlBlockedReason(
            boolean connected,
            boolean isFdm,
            boolean emergencyStop,
            boolean enclosureDoorOpen,
            boolean filamentRunout,
            int state
    ) {
        if (!connected) return "Printer disconnected";
        if (!isFdm) return "3D printing mode is not active";
        if (emergencyStop) return "Emergency stop active";
        if (state == MachineOperationStatus.SYSTEM_STATUS_PAUSED.value() && enclosureDoorOpen) {
            return "Close the enclosure door to resume";
        }
        if (state == MachineOperationStatus.SYSTEM_STATUS_PAUSED.value() && filamentRunout) {
            return "Resolve filament runout to resume";
        }
        if (MachineOperationStatus.isPrintChange(state)) return stateName(state);
        if (!isJobState(state)) return "No active print job";
        return "";
    }

    private String printModeName(int mode) {
        switch (mode) {
            case IPrintWorkspace.PRINT_MODE_DUAL_EXTRUDER_BACK_UP:
                return "Backup";
            case IPrintWorkspace.PRINT_MODE_CLONE:
                return "Copy";
            case IPrintWorkspace.PRINT_MODE_MIRROR:
                return "Mirror";
            case IPrintWorkspace.PRINT_MODE_NORMAL:
            default:
                return "Normal";
        }
    }

    private String extruderModelName(int model) {
        switch (model) {
            case Extruder.EXTRUDER_MATERIAL_BRASS_NTC:
                return "Brass (NTC)";
            case Extruder.EXTRUDER_MATERIAL_BRASS_PT100:
                return "Brass (PT100)";
            case Extruder.EXTRUDER_MATERIAL_HARDENED_STEEL_PT100:
                return "Hardened steel (PT100)";
            default:
                return "Unknown";
        }
    }

    private String emergencyStopName(ErrorController.EmergencyStopState state) {
        if (state == null) return "Unknown";
        switch (state) {
            case EMERGENCY_STOP_STATE_PRESS:
                return "Pressed";
            case EMERGENCY_STOP_STATE_RELEASE:
                return "Released; reset required";
            case EMERGENCY_STOP_STATE_NORMAL:
            default:
                return "Normal";
        }
    }

    private String fanName(int id) {
        if (id == 0) return "Part cooling";
        if (id == 1) return "Right cooling";
        if (id == 2) return "Heat sink";
        return "Fan " + (id + 1);
    }

    private Extruder findExtruder(List<Extruder> extruders, int id) {
        for (Extruder extruder : extruders) {
            if (extruder != null && extruder.getId() == id) return extruder;
        }
        return null;
    }

    private boolean isExtruderTelemetryReady(Extruder extruder) {
        return extruder != null && extruder.getDiameter() > 0f;
    }

    private HeatedBed.ZoneInfo findBedZone(List<HeatedBed.ZoneInfo> zones, int id) {
        for (HeatedBed.ZoneInfo zone : zones) {
            if (zone != null && zone.getZoneIndex() == id) return zone;
        }
        return null;
    }

    private boolean isBedZoneTelemetryReady(HeatedBed.ZoneInfo zone) {
        return zone != null
                && (zone.getCurrentTemperature() > 0f || zone.getTargetTemperature() > 0);
    }

    private <T extends Module> T findModule(MachineInfo info, Class<T> type) {
        if (info == null || info.moduleList == null) return null;
        List<Module> modules = snapshotList(info.moduleList);
        for (Module module : modules) {
            if (type.isInstance(module)) return type.cast(module);
        }
        return null;
    }

    private <T> List<T> snapshotList(List<T> source) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return new ArrayList<>(source);
            } catch (RuntimeException ignored) {
            }
        }
        return Collections.emptyList();
    }

    private Object optionalParserNumber(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value) || value < 0f) return JSONObject.NULL;
        return rounded(value, 2);
    }

    private Object optionalParserNumber(int value) {
        return value < 0 ? JSONObject.NULL : value;
    }

    private double rounded(float value, int places) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 0d;
        double factor = Math.pow(10d, places);
        return Math.round(value * factor) / factor;
    }

    private void addNonEmpty(JSONArray array, String value) {
        if (!TextUtils.isEmpty(value) && !"null".equalsIgnoreCase(value)) array.put(value);
    }

    private String firstNonEmpty(String first, String second) {
        return !TextUtils.isEmpty(first) ? first : emptyIfNull(second);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String loadDashboardAsset() throws IOException {
        IAppService appService = ServiceContainer.getInstance().getService(IAppService.class);
        StringBuilder html = new StringBuilder();
        try (InputStream input = appService.getAppContext().getAssets().open("artisan_dashboard.html");
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_8)
             )) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                html.append(buffer, 0, read);
            }
        }
        return html.toString().replace("__ARTISAN_DASHBOARD_TOKEN__", mDashboardToken);
    }

    private boolean isDashboardRequestAuthorized(HttpRequest request) {
        if (!mDashboardToken.equals(request.getHeader(DASHBOARD_REQUEST_HEADER))) return false;

        String fetchSite = request.getHeader("Sec-Fetch-Site");
        if (!TextUtils.isEmpty(fetchSite) && !"same-origin".equalsIgnoreCase(fetchSite)) {
            return false;
        }

        String host = request.getHeader("Host");
        if (TextUtils.isEmpty(host)) return false;
        String hostName = host;
        int portSeparator = hostName.lastIndexOf(':');
        if (portSeparator > 0) hostName = hostName.substring(0, portSeparator);
        String localAddress = getIPAddress();
        if (!hostName.equals(localAddress)
                && !hostName.equals("127.0.0.1")
                && !hostName.equalsIgnoreCase("localhost")) {
            return false;
        }

        String origin = request.getHeader("Origin");
        return TextUtils.isEmpty(origin) || origin.equalsIgnoreCase("http://" + host);
    }

    private void writeJson(HttpResponse response, int status, JSONObject json) {
        response.setStatus(status);
        response.setBody(new JsonBody(json));
    }

    private void writeJsonError(HttpResponse response, int status, String message) {
        JSONObject error = new JSONObject();
        try {
            error.put("ok", false);
            error.put("error", message);
        } catch (JSONException ignored) {
        }
        writeJson(response, status, error);
    }

    public Observable<Boolean> startPrint(File file) {
        SingleSubject<Boolean> resultSubject = SingleSubject.create();
        if (!MachineOperationStatus.SYSTEM_STATUS_IDLE.valueEquals(ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getPrintState())) {
            // Return result.
            return Observable.just(false);
        } else {
            String filename = file.getName();
            if (!file.exists() || filename.isEmpty()) {
                return Observable.just(false);
            } else {
                IGcodeParser mParser = ServiceContainer.getInstance().getService(IGcodeParser.class);
                IAppService appService = ServiceContainer.getInstance().getService(IAppService.class);
                mParser.startParse(appService.getFilesDir() + "/" + file.getName(), true, ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().workType);
                Disposable ParserSub = mParser.getParseProgressObservable()
                        .throttleLast(100, TimeUnit.MILLISECONDS)
                        .distinctUntilChanged()
                        .takeUntil(progress -> progress == 100)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(progress -> {
                            if (progress == -1) {
                                resultSubject.onSuccess(false);
                            } else if (progress == 100) {
                                IPrintWorkspace workspace = ServiceContainer.getInstance().getService(IPrintWorkspace.class);
                                workspace.setPrintMode(mParser.getCustomPrintMode());
                                workspace.setPrintSource(0);
                                workspace.setFileTotalLineCount(mParser.getTotalLinesCount());
                                workspace.setEstimatedTime(mParser.getEstimatedTime());
                                workspace.setFileMD5Value(Md5Util.fileToMD5(file));
                                NewPrintController printController = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController();
                                IFile file2 = new FabLocalFile(file);
                                Disposable sub = workspace.addFileToWorkspace(file2)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(success -> {
                                            if (success) {
                                                printController.setStartFromRemoteFlag(true);
                                                ServiceContainer.getInstance().getService(IRouter.class).routeToPrintPage().start(appService.getNowViewContext());
                                            } else {
                                                printController.setStartFromRemoteFlag(false);
                                                resultSubject.onSuccess(false);
                                            }
                                        }, e -> {
                                            LogHelper.log(e);
                                            printController.setStartFromRemoteFlag(false);
                                            resultSubject.onSuccess(false);
                                        });
                                mDisposable.add(sub);
                                sub = ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getPrintEventObservable()
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(printEvent -> {
                                            if (printEvent.getPrintEventState() == PrintEventState.STATE_SUCCESS) {
                                                resultSubject.onSuccess(true);
                                            } else if (printEvent.getPrintEventState() == PrintEventState.START_FAIL) {
                                                resultSubject.onSuccess(false);
                                            }
                                        });
                                mDisposable.add(sub);
                            }
                        }, e -> {
                            Logger.e(e.toString());
                        });
                mDisposable.add(ParserSub);
            }
        }
        resultSubject.onSuccess(true);
        return resultSubject.toObservable();
    }

    private String getIPAddress() {
        // check ip address
        String addressString = "Not Connected";
        try {
            List<NetworkInterface> interfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaceList) {
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        String sAddr = address.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (isIPv4) {
                            addressString = sAddr;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            LogHelper.log(e);
        }
        return addressString;
    }

    private String getMacAddr() {
        return ServiceContainer.getInstance().getService(INetwork.class).getMacAddress();
    }

    public String getCurrentVersion() {
        String versionFull = ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getLastUpdatePackageVersion();
        String curVersion;
        if (TextUtils.isEmpty(versionFull)) {
            curVersion = ServiceContainer.getInstance().getService(IAppService.class).getApp().getAppVersionName();
        } else {
            curVersion = versionFull;
            String[] versionSplits = versionFull.split("_");
            if (versionSplits.length > 1) {
                curVersion = versionSplits[1];
            }
        }
        return curVersion;
    }
}

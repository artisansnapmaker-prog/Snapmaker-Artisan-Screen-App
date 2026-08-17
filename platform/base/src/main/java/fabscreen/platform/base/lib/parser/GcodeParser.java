package fabscreen.platform.base.lib.parser;

import static fabscreen.platform.base.service.IMachine.WorkType.CNC;
import static fabscreen.platform.base.service.IMachine.WorkType.FDM;
import static fabscreen.platform.base.service.IMachine.WorkType.LASER;
import static fabscreen.platform.base.service.IMachine.WorkType.NONE;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_3DP_DOUBLE_EXTRUDER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_CNC_200W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_10W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_20W;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_2W_INFRARED;
import static fabscreen.platform.base.service.machine.entity.Module.ModuleType.HEAD_LASER_40W;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import fabscreen.platform.base.R;
import fabscreen.platform.base.helper.StringHelper;
import fabscreen.platform.base.instantiation.IServiceIdentifier;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.file.IFile;
import fabscreen.platform.base.model.ModelBoundary;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IFileManagerService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okio.BufferedSource;
import okio.Okio;

public class GcodeParser implements IGcodeParser, IServiceIdentifier {
    private static final String BOUND_HEADER_START_MARK_CURA = ";START_OF_HEADER";
    private static final String BOUND_HEADER_END_MARK_CURA = ";END_OF_HEADER";
    private static final String BOUND_HEADER_START_MARK_SNAPMAKER = ";Header Start";
    private static final String BOUND_HEADER_END_MARK_SNAPMAKER = ";Header End";
    private static String TAG = "GcodeParser";
    // comment marker
    private static final String BOUNDS_START_MARK = ";Start GCode end";
    private static final String BOUNDS_END_MARK = ";End GCode begin";
    private static final String BOUNDS_END_MARK_V1 = ";--- End G-code Begin ---";
    private boolean mIsParsingHeader = false;
    private int mHeaderVersion = 0;

    // Reader
    private long totalBytes;
    private long readBytes;
    private BufferedSource source;
    private String[] lineArgs = new String[20];
    private int lineArgCount;

    // G-code state
    private boolean mIsCurrentGcodeStateWorking = false;
    @NonNull
    private Position currentPosition;

    private boolean mShouldUpdateAttribute = true;
    private boolean isAbsoluteCoordinate = true;

    private float mBedTargetTemperature = 0;
    private float mNozzleTarget_0_Temperature = 0;
    private float mNozzleTarget_1_Temperature = 0;

    private float mCurrentLineFeedRate = 0;
    private double mFeedRateAmount = 0;
    private int mFeedRateCount = 0;

    private float extrusionAmount = 0;

    private float mPower = 0;

    private float mSpindleSpeed = 0;

    private IMachine.WorkType mFileType = NONE;
    private int mParseProgress = 0;

    private float mEstimateTime = 0;
    private int mTotalLinesCount = 0;

    private float mWorkSpeed = 0;
    private float mJogSpeed = 0;

    private float mDiameter = 0;

    private int mCustomPrintMode = 0;
    private boolean mIsDefineT0 = false;
    private boolean mIsDefineT1 = false;

    private Bitmap mGcodeThumbnail;
    private byte[] mGcodeThumbnailBytes;
    private long mGcodeThumbnailArea = -1;
    private final OrcaThumbnailBlockParser mOrcaThumbnailParser = new OrcaThumbnailBlockParser();
    private ModelBoundary mModelBoundary;
    private ArrayList<Position> mToolPath = new ArrayList<>();

    private BehaviorSubject<Integer> mParseProgressSubject = BehaviorSubject.createDefault(0);
    private Scheduler.Worker mParseWorker;

    private int mToolHead = -1;
    private float mNozzle_0_Diameter = -1;
    private float mNozzle_1_Diameter = -1;
    private int mLayerNumber = -1;
    private float mLayerHeight = -1;
    private float mMaterialWeight = -1;
    private float mMaterialLength = -1;
    private String mNozzle_0_Material = null;
    private String mNozzle_1_Material = null;
    private String mRenderMethod = null;
    private int mIsRotate = -1;
    private float mWorkSizeX = -1;
    private float mWorkSizeY = -1;
    private String mOrigin = null;
    private String mMachine = null;
    private int mToolHeadNameID = -1;

    private int mCurrentExtruder = 0;
    private int mT0RetractionCount = 0;
    private int mT1RetractionCount = 0;
    private float mLastEAxisPosition = 0;
    private boolean mFDMRetractionParamAcquired = false;
    private float mExtruder0RetractionDistance = 0;
    private float mExtruder0SwitchRetractionDistance = 0;
    private float mExtruder1RetractionDistance = 0;
    private float mExtruder1SwitchRetractionDistance = 0;

    private HeaderParamsChecker mHeaderChecker;

    public GcodeParser() {
        mModelBoundary = new ModelBoundary();
        currentPosition = new Position(0, 0, 0);
    }

    @Override
    public void destroy() {
        if (mParseWorker != null) {
            mParseWorker.dispose();
            mParseWorker = null;
        }

        if (mToolPath != null) {
            mToolPath.clear();
        }
    }

    @Override
    public void startParse(String filePath, boolean isLocal, IMachine.WorkType fileType) {
        IFile search = ServiceContainer.getInstance().getService(IFileManagerService.class)
                .getDevice(isLocal)
                .search(filePath);

        startParse(search, fileType);
    }

    @Override
    public void startParse(IFile file, IMachine.WorkType fileType) {
        synchronized (GcodeParser.this) {
            mFileType = fileType;
            try {
                totalBytes = file.length();
                readBytes = 0;
                source = Okio.buffer(Okio.source(file.getInputStream()));
            } catch (IOException e) {
                LogHelper.log(e);
            }
            // Create parse worker if not exist
            if (mParseWorker == null) {
                mParseWorker = Schedulers.computation().createWorker();
            }

            // Start parse here in computation scheduler
            mParseWorker.schedule(this::parse);
        }
    }

    @Override
    public void startParse(InputStream io, IMachine.WorkType fileType) {
        synchronized (GcodeParser.this) {
            mFileType = fileType;
            try {
                totalBytes = io.available();
                readBytes = 0;
                source = Okio.buffer(Okio.source(io));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Create parse worker if not exist
            if (mParseWorker == null) {
                mParseWorker = Schedulers.computation().createWorker();
            }

            // Start parse here in computation scheduler
            mParseWorker.schedule(this::parse);
        }
    }

    private void parse() {
        String line;
        int linesCount = 0;
        int newLineBytes = 1;

        // Peek to count total lines
        try {
            BufferedSource peek = source.peek();
            // https://github.com/square/okio/blob/master/okio/jvm/src/main/java/okio/Buffer.kt#L648
            // Check if the file uses "\n" or "\r\n", which will affect our byte calculation
            long carriage = peek.indexOf((byte) ('\r'), 0, 64);
            if (carriage != -1) {
                newLineBytes = 2;
            }
            peek.close();
        } catch (IOException e) {
            e.printStackTrace();
            mParseProgressSubject.onNext(-1);
            return;
        }

        // Reset progress
        resetResult();
        mParseProgressSubject.onNext(0);
        mTotalLinesCount = 0;
        mIsParsingHeader = false;
        mHeaderChecker = HeaderParamsChecker.getInstance();
        mHeaderChecker.reset();

        while (true) {
            try {
                line = source.readUtf8Line();

                if (line == null) break;

                // stop parsing if header exists and get totalLinesCount
                if (checkParamsRequired() && !mIsParsingHeader) {
                    break;
                }

                // Update progress
                readBytes += line.length() + newLineBytes;
                mParseProgress = (int) (100 * readBytes / totalBytes);
                if (mParseProgress <= 100 && mParseProgress > mParseProgressSubject.getValue()) {
                    mParseProgressSubject.onNext(mParseProgress);
                }

                // Fixme: Need to refactor format number for parsing args instead of throwing exception
                parseLine(line);
                linesCount++;
            } catch (Exception e) {
                e.printStackTrace();
                LogHelper.log(e);
                mParseProgressSubject.onNext(-1);
                return;
            }
        }
        try {
            if (source != null) {
                source.close();
            }
        } catch (IOException e) {
            LogHelper.log(e);
        }

        if (!mHeaderChecker.isTotalLinesCheck()) {
            mTotalLinesCount = (mTotalLinesCount == 0) ? linesCount : mTotalLinesCount;
        }
        checkPathClose();

        // Set progress to 100
        mParseProgressSubject.onNext(100);
    }

    private void resetResult() {
        if (mModelBoundary != null) {
            mModelBoundary = new ModelBoundary();
        }
        mToolHead = -1;
        mGcodeThumbnail = null;
        mGcodeThumbnailBytes = null;
        mGcodeThumbnailArea = -1;
        mOrcaThumbnailParser.reset();
        mTotalLinesCount = 0;
        mEstimateTime = 0;
        mNozzleTarget_0_Temperature = 0;
        mNozzleTarget_1_Temperature = 0;
        mBedTargetTemperature = 0;
        mPower = 0;
        mWorkSpeed = 0;
        mJogSpeed = 0;
        mDiameter = 0;
        mNozzle_0_Diameter = -1;
        mNozzle_1_Diameter = -1;
        mLayerNumber = -1;
        mLayerHeight = -1;
        mMaterialWeight = -1;
        mMaterialLength = -1;
        mNozzle_0_Material = null;
        mNozzle_1_Material = null;
        mRenderMethod = null;
        mIsRotate = -1;
        mWorkSizeX = -1;
        mWorkSizeY = -1;
        mOrigin = null;
        mMachine = null;
        mCustomPrintMode = 0;
        mIsDefineT0 = false;
        mIsDefineT1 = false;
        mToolHeadNameID = -1;
        mHeaderVersion = 0;
        mFDMRetractionParamAcquired = false;
        mLastEAxisPosition = 0;
        mT0RetractionCount = 0;
        mT1RetractionCount = 0;
        mExtruder0RetractionDistance = 0;
        mExtruder0SwitchRetractionDistance = 0;
        mExtruder1RetractionDistance = 0;
        mExtruder1SwitchRetractionDistance = 0;
        mCurrentExtruder = 0;
        mShouldUpdateAttribute = true;
    }

    private boolean checkParamsRequired() {
        return mTotalLinesCount != 0 && (mFileType != IMachine.WorkType.FDM || mFDMRetractionParamAcquired);
    }

    private void parseLine(final String line) throws NumberFormatException {
        if (line.isEmpty()) {
            return;
        }

        if (mOrcaThumbnailParser.consumeLine(line)) {
            OrcaThumbnailBlockParser.Result result = mOrcaThumbnailParser.takeCompleted();
            if (result != null) {
                decodeAndStoreThumbnail(result.getEncodedData());
            }
            return;
        }

        // Straight comment
        if (line.charAt(0) == ';') {
            if (containsEndGcodeMark(line)) {
                mShouldUpdateAttribute = false;
            }

            //  parse header markers
            if (mIsParsingHeader) {
                if (line.equals(BOUND_HEADER_END_MARK_SNAPMAKER) || line.equals(BOUND_HEADER_END_MARK_CURA)) {
                    if (line.equals(BOUND_HEADER_END_MARK_CURA)) {
                        mFileType = FDM;
                    }
                    mIsParsingHeader = false;
                } else {
                    if (mHeaderVersion > 0) {
                        // Parse header according to the header version.
                        parseHeader(line, mHeaderVersion);
                    } else {
                        // Parse header that using former format(before 2023)
                        parseHeader(line);
                    }
                }
                return;
            } else {
                if (line.equals(BOUND_HEADER_START_MARK_SNAPMAKER) || line.equals(BOUND_HEADER_START_MARK_CURA)) {
                    mIsParsingHeader = true;
                }
                return;
            }
        }

        // Parse line to separate arguments
        final int length = line.length();

        lineArgCount = 0;
        int pos = 0;
        while (pos < length) {
            while (pos < length && line.charAt(pos) == ' ') pos++;

            if (pos == length) break;
            if (line.charAt(pos) == ';') break;

            int start = pos;
            pos++;
            while (pos < length) {
                char c = line.charAt(pos);
                if (c == ' ' || c == ';' || StringHelper.isAlphabetic(c)) break;
                pos++;
            }

            lineArgs[lineArgCount++] = line.substring(start, pos);
            if (lineArgCount >= 20) break;
        }

        if (lineArgCount == 0) return;

        // Parse arguments based on G-code command
        switch (lineArgs[0]) {
            case "G0":
            case "G1": {
                parseG0G1();
                break;
            }
            case "G4": {
                parseG4();
                break;
            }
            case "G20":
            case "G21": {
                break;
            }
            case "G28": {
//                parseG28();
                break;
            }
            case "G90": {
                // absolute position
                isAbsoluteCoordinate = true;
                break;
            }
            case "G91": {
                // relative position
                isAbsoluteCoordinate = false;
                break;
            }
            case "G92": {
                parseG92();
                break;
            }
            case "M3":
            case "M5": {
                if (mHeaderChecker.isLaserPowerCheck()) break;

                parseM3M5();
                break;
            }
            case "M83": {
                isAbsoluteCoordinate = false;
            }
            case "M140":
            case "M190": {
                if (mHeaderChecker.isHeatedBedTempCheck()) break;
                // heated bed
                parseHeatedBedTemperature();
                break;
            }
            case "M104":
            case "M109": {
                if (mHeaderChecker.isNozzleTempCheck()) break;
                // nozzle
                parseNozzleTemperature();
                break;
            }
            case "T0":
                mIsDefineT0 = true;
                mCurrentExtruder = 0;
                break;
            case "T1":
                mIsDefineT1 = true;
                mCurrentExtruder = 1;
                break;
            case "M605": {
                if (mHeaderChecker.isPrintModeCheck()) break;

                parseM605();
                break;
            }
            default:
                break;
        }
    }

    private void checkPathStart() {
        if (!mIsCurrentGcodeStateWorking) {
            mIsCurrentGcodeStateWorking = true;

            currentPosition.setAsStartPoint();
//            mToolPath.add(currentPosition);
        }
    }

    private void checkPathClose() {
        if (mIsCurrentGcodeStateWorking) {
            mIsCurrentGcodeStateWorking = false;

            currentPosition.setAsEndPoint();
        }
    }

    private void parseHeader(String line) throws NumberFormatException {
        final int length = line.length();

        lineArgCount = 0;
        // skip comment mark
        int pos = 1;

        while (pos < length) {
            while (pos < length && line.charAt(pos) == ' ') pos++;

            if (pos == length) break;

            int start = pos;
            while (pos < length && line.charAt(pos) != ':') pos++;

            lineArgs[lineArgCount++] = line.substring(start, pos);

            pos++;
        }

        if (lineArgCount == 0) return;
        if ("null".equals(lineArgs[1])) return;
        // Parse arguments based on G-code command
        switch (lineArgs[0]) {
            case "Version": {
                mHeaderVersion = Integer.parseInt(lineArgs[1]);
                break;
            }
            case "header_type": {
                switch (lineArgs[1]) {
                    case "3dp":
                        mFileType = FDM;
                        break;
                    case "laser":
                        mFileType = LASER;
                        break;
                    case "cnc":
                        mFileType = CNC;
                        break;
                    default:
                        break;
                }
                break;
            }
            case "tool_head": {
                switch (lineArgs[1]) {
                    case "singleExtruderToolheadForOriginal":
                        break;
                    case "singleExtruderToolheadForSM2":
                        mToolHead = HEAD_3DP;
                        mToolHeadNameID = R.string.all_tool_head_3dp;
                        break;
                    case "dualExtruderToolheadForSM2":
                        mToolHead = HEAD_3DP_DOUBLE_EXTRUDER;
                        mToolHeadNameID = R.string.all_tool_head_dual_extruder;
                        break;
                    case "levelOneLaserToolheadForOriginal":
                        break;
                    case "levelTwoLaserToolheadForOriginal":
                        break;
                    case "levelOneLaserToolheadForSM2":
                        mToolHead = HEAD_LASER;
                        mToolHeadNameID = R.string.all_tool_head_laser;
                        break;
                    case "levelTwoLaserToolheadForSM2":
                        mToolHead = HEAD_LASER_10W;
                        mToolHeadNameID = R.string.all_tool_head_laser_10w;
                        break;
                    case "standardCNCToolheadForOriginal":
                        break;
                    case "standardCNCToolheadForSM2":
                        mToolHead = HEAD_CNC;
                        mToolHeadNameID = R.string.all_tool_head_cnc;
                        break;
                    case "levelTwoCNCToolheadForSM2":
                        mToolHead = HEAD_CNC_200W;
                        mToolHeadNameID = R.string.all_tool_head_cnc_200w;
                        break;
                    case "20W Laser Module":
                        mToolHead = HEAD_LASER_20W;
                        mToolHeadNameID = R.string.all_tool_head_laser_20w;
                        break;
                    case "40W Laser Module":
                        mToolHead = HEAD_LASER_40W;
                        mToolHeadNameID = R.string.all_tool_head_laser_40w;
                        break;
                    case "2W Laser Module":
                        mToolHead = HEAD_LASER_2W_INFRARED;
                        mToolHeadNameID = R.string.all_tool_head_laser_2w_infrared;
                        break;
                    default:
                        break;
                }
                break;
            }
            case "thumbnail": {
                decodeAndStoreThumbnail(lineArgs[2]);
                break;
            }
            case "file_total_lines": {
                mTotalLinesCount = Integer.valueOf(lineArgs[1]);
                mHeaderChecker.setTotalLinesCheck(true);
                break;
            }
            case "estimated_time(s)":
            case "PRINT.TIME": {
                mEstimateTime = Float.valueOf(lineArgs[1]);
                mHeaderChecker.setEstimatedTimeCheck(true);
                break;
            }
            case "min_x(mm)":
            case "PRINT.SIZE.MIN.X": {
                mModelBoundary.setMinX(Float.valueOf(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "max_x(mm)":
            case "PRINT.SIZE.MAX.X": {
                mModelBoundary.setMaxX(Float.valueOf(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "min_y(mm)":
            case "PRINT.SIZE.MIN.Y": {
                mModelBoundary.setMinY(Float.valueOf(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "max_y(mm)":
            case "PRINT.SIZE.MAX.Y": {
                mModelBoundary.setMaxY(Float.valueOf(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "min_z(mm)":
            case "PRINT.SIZE.MIN.Z": {
                mModelBoundary.setMinZ(Float.valueOf(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "max_z(mm)":
            case "PRINT.SIZE.MAX.Z": {
                mModelBoundary.setMaxZ(Float.valueOf(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "min_b(mm)": {
                mModelBoundary.setMinB(Float.valueOf(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "max_b(mm)": {
                mModelBoundary.setMaxB(Float.valueOf(lineArgs[1]));
                mHeaderChecker.setBoundaryCheck(true);
                break;
            }
            case "nozzle_temperature(°C)":
            case "nozzle_0_temperature(°C)":
            case "EXTRUDER_TRAIN.0.INITIAL_TEMPERATURE": {
                mNozzleTarget_0_Temperature = Float.valueOf(lineArgs[1]);
                mHeaderChecker.setNozzleTempCheck(true);
                break;
            }
            case "nozzle_1_temperature(°C)":
            case "EXTRUDER_TRAIN.1.INITIAL_TEMPERATURE": {
                if (lineArgs[1].equals("null")) return;
                mNozzleTarget_1_Temperature = Float.valueOf(lineArgs[1]);
                mHeaderChecker.setNozzleTempCheck(true);
                break;
            }
            case "build_plate_temperature(°C)":
            case "BUILD_PLATE.INITIAL_TEMPERATURE": {
                mBedTargetTemperature = Float.valueOf(lineArgs[1]);
                mHeaderChecker.setHeatedBedTempCheck(true);
                break;
            }
            case "spindle_speed(mm/minute)": {
                mSpindleSpeed = Float.valueOf(lineArgs[1]);
                break;
            }
            case "power(%)": {
                mPower = Float.valueOf(lineArgs[1]);
                mHeaderChecker.setLaserPowerCheck(true);
                break;
            }
            case "work_speed(mm/minute)": {
                mWorkSpeed = Float.valueOf(lineArgs[1]);
                break;
            }
            case "jog_speed(mm/minute)": {
                mJogSpeed = Float.valueOf(lineArgs[1]);
                break;
            }
            case "diameter": {
                mDiameter = Float.valueOf(lineArgs[1]);
                break;
            }
            case "nozzle_0_diameter":
            case "nozzle_0_diameter(mm)": {
                if ("null".equals(lineArgs[1])) return;
                mNozzle_0_Diameter = Float.valueOf(lineArgs[1]);
                break;
            }
            case "nozzle_1_diameter":
            case "nozzle_1_diameter(mm)": {
                if ("null".equals(lineArgs[1])) return;
                mNozzle_1_Diameter = Float.valueOf(lineArgs[1]);
                break;
            }
            case "layer_number": {
                mLayerNumber = Integer.valueOf(lineArgs[1]);
                break;
            }
            case "layer_height": {
                mLayerHeight = Float.parseFloat(lineArgs[1]);
                break;
            }
            //FIXME :Remove this code after filming.
            case "matierial_weight": {
                mMaterialWeight = Float.parseFloat(lineArgs[1]);
                break;
            }
            //FIXME :Remove this code after filming.
            case "matierial_length": {
                mMaterialLength = Float.parseFloat(lineArgs[1]);
                break;
            }
            case "nozzle_0_material": {
                mNozzle_0_Material = lineArgs[1];
                break;
            }
            case "nozzle_1_material": {
                mNozzle_1_Material = lineArgs[1];
                break;
            }
            case "renderMethod": {
                mRenderMethod = lineArgs[1];
                break;
            }
            case "is_rotate": {
                mIsRotate = lineArgs[1].equals("true") ? 1 : 0;
                mModelBoundary.setDimension((mIsRotate > 0) ? ModelBoundary.DIMENSION_BY : ModelBoundary.DIMENSION_XY);
                break;
            }
            case "work_size_x": {
                mWorkSizeX = Float.valueOf(lineArgs[1]);
                break;
            }
            case "work_size_y": {
                mWorkSizeY = Float.valueOf(lineArgs[1]);
                break;
            }
            case "origin": {
                mOrigin = lineArgs[1];
                Context context = ServiceContainer.getInstance().getService(IAppService.class).getAppContext();
                switch (mOrigin) {
                    case "center":
                        mOrigin = context.getString(R.string.all_gcode_parser_work_origin_center);
                        break;
                    case "top-left":
                        mOrigin = context.getString(R.string.all_gcode_parser_work_origin_top_left);
                        break;
                    case "top-right":
                        mOrigin = context.getString(R.string.all_gcode_parser_work_origin_top_right);
                        break;
                    case "bottom-left":
                        mOrigin = context.getString(R.string.all_gcode_parser_work_origin_bottom_left);
                        break;
                    case "bottom-right":
                        mOrigin = context.getString(R.string.all_gcode_parser_work_origin_bottom_right);
                        break;
                    case "positionA":
                        mOrigin = context.getString(R.string.all_gcode_parser_work_origin_bottom_position_a);
                        break;
                    case "positionB":
                        mOrigin = context.getString(R.string.all_gcode_parser_work_origin_bottom_position_b);
                        break;
                    default:
                        break;
                }
                break;
            }
            case "machine": {
                mMachine = lineArgs[1];
                break;
            }
            case "Extruder 0 Retraction Distance": {
                mExtruder0RetractionDistance = Float.parseFloat(lineArgs[1]);
                mFDMRetractionParamAcquired = true;
                mHeaderChecker.setExtruder0RetractionCheck(true);
                break;
            }
            case "Extruder 0 Switch Retraction Distance": {
                mExtruder0SwitchRetractionDistance = Float.parseFloat(lineArgs[1]);
                break;
            }
            case "Extruder 1 Retraction Distance": {
                mExtruder1RetractionDistance = Float.parseFloat(lineArgs[1]);
                mFDMRetractionParamAcquired = true;
                mHeaderChecker.setExtruder1RetractionCheck(true);
                break;
            }
            case "Extruder 1 Switch Retraction Distance": {
                mExtruder1SwitchRetractionDistance = Float.parseFloat(lineArgs[1]);
                break;
            }
            default:
                break;
        }
    }

    private void parseHeader(String line, int headerVersion) throws NumberFormatException {
        final int length = line.length();
        lineArgCount = 0;

        switch (headerVersion) {
            case 1:
            default:
                // skip comment mark
                int pos = 1;

                while (pos < length) {
                    while (pos < length && line.charAt(pos) == ' ') pos++;

                    if (pos == length) break;

                    int start = pos;
                    while (pos < length && line.charAt(pos) != ':') pos++;

                    lineArgs[lineArgCount++] = line.substring(start, pos);

                    pos++;
                }

                if (lineArgCount == 0) return;
                if ("null".equals(lineArgs[1])) return;
                // Parse arguments based on G-code command
                switch (lineArgs[0]) {
                    case "Slicer":
                        break;
                    case "Printer":
                        break;
                    case "Estimated Print Time":
                        mEstimateTime = Integer.parseInt(lineArgs[1]);
                        mHeaderChecker.setEstimatedTimeCheck(true);
                        break;
                    case "Lines":
                        mTotalLinesCount = Integer.parseInt(lineArgs[1]);
                        mHeaderChecker.setTotalLinesCheck(true);
                        break;
                    case "Extruder Mode":
                        break;
                    case "Extruder 0 Nozzle Size":
                        mNozzle_0_Diameter = Float.parseFloat(lineArgs[1]);
                        break;
                    case "Extruder 0 Material":
                        mNozzle_0_Material = lineArgs[1];
                        break;
                    case "Extruder 0 Print Temperature":
                        mNozzleTarget_0_Temperature = Float.parseFloat(lineArgs[1]);
                        mHeaderChecker.setNozzleTempCheck(true);
                        break;
                    case "Extruder 0 Retraction Distance":
                        mExtruder0RetractionDistance = Float.parseFloat(lineArgs[1]);
                        mFDMRetractionParamAcquired = true;
                        mHeaderChecker.setExtruder0RetractionCheck(true);
                        break;
                    case "Extruder 0 Switch Retraction Distance":
                        mExtruder0SwitchRetractionDistance = Float.parseFloat(lineArgs[1]);
                        break;
                    case "Extruder 1 Nozzle Size":
                        mNozzle_1_Diameter = Float.parseFloat(lineArgs[1]);
                        break;
                    case "Extruder 1 Material":
                        mNozzle_1_Material = lineArgs[1];
                        break;
                    case "Extruder 1 Print Temperature":
                        mNozzleTarget_1_Temperature = Float.parseFloat(lineArgs[1]);
                        mHeaderChecker.setNozzleTempCheck(true);
                        break;
                    case "Extruder 1 Retraction Distance":
                        mExtruder1RetractionDistance = Float.parseFloat(lineArgs[1]);
                        mFDMRetractionParamAcquired = true;
                        mHeaderChecker.setExtruder1RetractionCheck(true);
                        break;
                    case "Extruder 1 Switch Retraction Distance":
                        mExtruder1SwitchRetractionDistance = Float.parseFloat(lineArgs[1]);
                        break;
                    case "Bed Temperature":
                        mBedTargetTemperature = Float.parseFloat(lineArgs[1]);
                        mHeaderChecker.setHeatedBedTempCheck(true);
                        break;
                    case "Extruder(s) Used":
                        break;
                    case "Work Range - Min X":
                        mModelBoundary.setMinX(Float.parseFloat(lineArgs[1]));
                        mHeaderChecker.setBoundaryCheck(true);
                        break;
                    case "Work Range - Min Y":
                        mModelBoundary.setMinY(Float.parseFloat(lineArgs[1]));
                        mHeaderChecker.setBoundaryCheck(true);
                        break;
                    case "Work Range - Min Z":
                        mModelBoundary.setMinZ(Float.parseFloat(lineArgs[1]));
                        mHeaderChecker.setBoundaryCheck(true);
                        break;
                    case "Work Range - Max X":
                        mModelBoundary.setMaxX(Float.parseFloat(lineArgs[1]));
                        mHeaderChecker.setBoundaryCheck(true);
                        break;
                    case "Work Range - Max Y":
                        mModelBoundary.setMaxY(Float.parseFloat(lineArgs[1]));
                        mHeaderChecker.setBoundaryCheck(true);
                        break;
                    case "Work Range - Max Z":
                        mModelBoundary.setMaxZ(Float.parseFloat(lineArgs[1]));
                        mHeaderChecker.setBoundaryCheck(true);
                        break;
                    case "Thumbnail":
                        decodeAndStoreThumbnail(lineArgs[2]);
                        break;
                }
                break;
        }
    }

    /**
     * Parse G0 and G1 command from lineArgs.
     * <p>
     * G0-G1 : Linear Move, add a straight line movement to the planer.
     * Movements can be set in Relative Mode or Absolute Mode using <b>G90</b> or <b>G91</b> command.
     * <p>
     * Also <b>M83</b> command can set "E coordinate", which is interpreted as relative.
     * <p>
     * `X`,`Y`,`Z` represent the coordinate on the axis.
     * <p>
     * `A`,`B`,`C` represent the rotation axis.
     * <p>
     * `E` for the extruder axis, it describes the position of the filament in terms of the extruder feeder.
     * <p>
     * `F` represent the maximum movement rate of the move.
     * <p>
     * Examples:
     * <p>
     * G1 F1500
     * <p>
     * G1 X90.6 Y13.8 E22.4 F3000
     * <p>
     * G1 X80 Y20 E36 F1500
     * <p>
     * G0 F2400 X49.071 Y22.466 E0.43903
     */
    private void parseG0G1() throws NumberFormatException {
        float x = 0, y = 0, z = 0, e, feedRate;

        x = currentPosition.x;
        y = currentPosition.y;
        z = currentPosition.z;

        for (int i = 1; i < lineArgCount; i++) {
            switch (lineArgs[i].charAt(0)) {
                case 'X': {
                    x = Float.parseFloat(lineArgs[i].substring(1));
                    break;
                }
                case 'Y': {
                    y = Float.parseFloat(lineArgs[i].substring(1));
                    break;
                }
                case 'Z': {
                    z = Float.parseFloat(lineArgs[i].substring(1));
                    break;
                }
                case 'E': {
                    if (mShouldUpdateAttribute) {
                        e = Float.parseFloat(lineArgs[i].substring(1));
                        extrusionAmount = isAbsoluteCoordinate ? (e) : (extrusionAmount + e);
                        if (mFileType == IMachine.WorkType.FDM && (!mHeaderChecker.isExtruder0RetractionCheck() || !mHeaderChecker.isExtruder1RetractionCheck())) {
                            calculateRetraction(e);
                        }
                    }
                    break;
                }
                case 'F': {
                    if (mShouldUpdateAttribute) {
                        feedRate = Float.parseFloat(lineArgs[i].substring(1));
                        if (feedRate != 0 && feedRate != mCurrentLineFeedRate) {
                            mCurrentLineFeedRate = feedRate;
                        }
                    }
                    break;
                }
            }
        }

        boolean pointMoved = (x != currentPosition.x || y != currentPosition.y || z != currentPosition.z);
        if (!pointMoved) {
            return;
        }

        Position position = new Position(x, y, z);

        if (lineArgs[0].equals("G0")) {
            checkPathClose();

            // Calculate ETA
            if (mCurrentLineFeedRate != 0 && !mHeaderChecker.isEstimatedTimeCheck()) {
                final float segmentTime = currentPosition.distanceTo(position) / mCurrentLineFeedRate * 60;
                mEstimateTime += segmentTime;
            }

            // Update current position
            currentPosition = position;

//            mModelBoundary.updateBoundary(position);
        } else if (lineArgs[0].equals("G1")) {
            checkPathStart();

            if (mCurrentLineFeedRate != 0 && !mHeaderChecker.isEstimatedTimeCheck()) {
                final float segmentTime = currentPosition.distanceTo(position) / mCurrentLineFeedRate * 60;
                mEstimateTime += segmentTime;

                // Calculate average work speed FeedRate
                mFeedRateAmount += mCurrentLineFeedRate;
                mFeedRateCount++;
            }

            currentPosition = position;

//            mToolPath.add(position);
            if (!mHeaderChecker.isBoundaryCheck()) {
                mModelBoundary.updateBoundary(position);
            }
        }
    }

    private void calculateRetraction(float ePosition) {
        // Absolute mode
        float delta = isAbsoluteCoordinate ? ePosition - mLastEAxisPosition : ePosition;
        if (delta < 0 && delta > -8.0f) {
//            Logger.d("retraction detected T%d %.4f", mCurrentExtruder, delta);
//            Logger.d("gcode %s", Arrays.toString(lineArgs));
            if (mCurrentExtruder == 0) {
                if (mT0RetractionCount == 0) {
                    mExtruder0RetractionDistance = -delta;
                }

                if (mExtruder0RetractionDistance != -delta) {
                    mT0RetractionCount = 1;
                }

                if (mT0RetractionCount > 3 && mExtruder0RetractionDistance > -delta) {
                    mExtruder0RetractionDistance = -delta;
                }
                mT0RetractionCount++;
            } else {
                if (mT1RetractionCount == 0) {
                    mExtruder1RetractionDistance = -delta;
                }

                if (mExtruder1RetractionDistance != -delta) {
                    mT1RetractionCount = 1;
                }

                if (mT1RetractionCount > 3 && mExtruder1RetractionDistance > -delta) {
                    mExtruder1RetractionDistance = -delta;
                }
                mT1RetractionCount++;
            }
//            Logger.d("T0 retracted %.2f at %d times, T1 retracted %.2f at %d times",
//                    mExtruder0RetractionDistance, mT0RetractionCount,
//                    mExtruder1RetractionDistance, mT1RetractionCount);
        }
        mLastEAxisPosition = ePosition;
    }

    /**
     * Parse G4 G-code command.
     * <p>
     * G4: Dwell, pause the command queue and waits for a period of time.
     * <p>
     * Using `S` or `P` parameters, if both included, `S` takes precedence.
     * <p>
     * P[time(ms)]
     * <p>
     * S[time(sec)]
     */
    private void parseG4() throws NumberFormatException {
        for (int i = 1; i < lineArgCount; i++) {
            switch (lineArgs[i].charAt(0)) {
                case 'S': {
                    final float dwellTime = Float.parseFloat(lineArgs[i].substring(1));
                    mEstimateTime += dwellTime;
                    return;
                }
                case 'P': {
                    final float dwellTime = Float.parseFloat(lineArgs[i].substring(1));
                    mEstimateTime += dwellTime * 0.001;
                    return;
                }
            }
        }
    }

    private void parseG28() {
        // TODO: implement an offset for absolute position
        if (lineArgCount < 2) {
            checkPathClose();
            currentPosition = new Position(0, 0, 0);
        } else {
            float x = currentPosition.x;
            float y = currentPosition.y;
            float z = currentPosition.z;

            for (int i = 1; i < lineArgCount; i++) {
                switch (lineArgs[i].charAt(0)) {
                    case 'X': {
                        x = 0;
                        break;
                    }
                    case 'Y': {
                        y = 0;
                        break;
                    }
                    case 'Z': {
                        z = 0;
                        break;
                    }
                }
            }

            checkPathClose();
            currentPosition = new Position(x, y, z);
        }
    }

    /**
     * G92
     */
    private void parseG92() {
        // TODO: implement an offset for absolute position
        for (int i = 1; i < lineArgCount; i++) {
            if (lineArgs[i].charAt(0) == 'E') {
                mLastEAxisPosition = Float.parseFloat(lineArgs[i].substring(1));
            }
        }
    }

    private void parseM3M5() throws NumberFormatException {
        for (int i = 1; i < lineArgCount; i++) {
            switch (lineArgs[i].charAt(0)) {
                case 'S': {
                    mPower = Float.parseFloat(lineArgs[i].substring(1)) / 255 * 100;
                    break;
                }
                case 'P': {
                    mPower = Float.parseFloat(lineArgs[i].substring(1));
                    break;
                }
            }
        }
    }

    private void parseHeatedBedTemperature() throws NumberFormatException {
        for (int i = 1; i < lineArgCount; i++) {
            if (lineArgs[i].charAt(0) == 'S') {
                float temperature = Float.parseFloat(lineArgs[i].substring(1));
                if (mBedTargetTemperature < temperature) {
                    mBedTargetTemperature = temperature;
                }
            }
        }
    }

    private void parseNozzleTemperature() throws NumberFormatException {
        for (int i = 1; i < lineArgCount; i++) {
            if (lineArgs[i].charAt(0) == 'S') {
                float temperature = Float.parseFloat(lineArgs[i].substring(1));
                if (mNozzleTarget_0_Temperature < temperature) {
                    mNozzleTarget_0_Temperature = temperature;
                }
            }
        }
    }

    /**
     * M605: Set behavior print mode for dual extruder or "IDEX" machine.
     * <p>
     * [S] parameter represent print mode in dual x
     */
    private void parseM605() throws NumberFormatException {
        for (int i = 1; i < lineArgCount; i++) {
            switch (lineArgs[i].charAt(0)) {
                case 'S': {
                    mCustomPrintMode = Integer.parseInt(lineArgs[i].substring(1));
                }
            }
        }
    }

    private boolean containsEndGcodeMark(String line) {
        return line.contains(BOUNDS_END_MARK) || line.contains(BOUNDS_END_MARK_V1);
    }

    public ArrayList<Position> getToolPath() {
        return mToolPath;
    }

    private void decodeAndStoreThumbnail(String encodedImage) {
        try {
            int commaIndex = encodedImage.indexOf(',');
            String encodedData = commaIndex >= 0
                    ? encodedImage.substring(commaIndex + 1)
                    : encodedImage;
            if (encodedData.isEmpty()
                    || encodedData.length() > OrcaThumbnailBlockParser.MAX_ENCODED_CHARACTERS) {
                return;
            }
            byte[] bitmapArray = Base64.decode(encodedData, Base64.DEFAULT);
            if (bitmapArray.length == 0) {
                return;
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length, bounds);
            long area = (long) bounds.outWidth * bounds.outHeight;
            if (bounds.outWidth <= 0
                    || bounds.outHeight <= 0
                    || bounds.outWidth > OrcaThumbnailBlockParser.MAX_IMAGE_DIMENSION
                    || bounds.outHeight > OrcaThumbnailBlockParser.MAX_IMAGE_DIMENSION
                    || area <= 0
                    || area > OrcaThumbnailBlockParser.MAX_IMAGE_PIXELS
                    || area <= mGcodeThumbnailArea) {
                return;
            }

            Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
            if (bitmap != null) {
                mGcodeThumbnailBytes = bitmapArray.clone();
                mGcodeThumbnail = bitmap;
                mGcodeThumbnailArea = area;
            }
        } catch (Exception e) {
            LogHelper.log(e);
        }
    }

    @Override
    public IMachine.WorkType getFileType() {
        return mFileType;
    }

    @Override
    public Bitmap getGcodeThumbnail() {
        return mGcodeThumbnail;
    }

    public byte[] getGcodeThumbnailBytes() {
        return mGcodeThumbnailBytes;
    }

    @Override
    public int getTotalLinesCount() {
        return mTotalLinesCount;
    }

    @Override
    public float getBedTargetTemperature() {
        return mBedTargetTemperature;
    }

    @Override
    public float getNozzleTargetTemperature() {
        return mNozzleTarget_0_Temperature;
    }

    @Override
    public float getPower() {
        return mPower;
    }

//    @Override
//    public float getCNCPower() {
//        return mCNCPower;
//    }

    @Override
    public float getWorkSpeed() {
        if (mWorkSpeed != 0) {
            return mWorkSpeed;
        } else {
            if (mFeedRateCount == 0) return 0;

            return (float) mFeedRateAmount / mFeedRateCount;
        }
    }

    @Override
    public float getJogSpeed() {
        return mJogSpeed;
    }

    @Override
    public float getDiameter() {
        return mDiameter;
    }

    @Override
    public float getSpindleSpeed() {
        return mSpindleSpeed;
    }

    public float getAverageFeedRate() {
        if (mFeedRateCount == 0) return 0;

        return (float) mFeedRateAmount / mFeedRateCount;
    }

    @Override
    public float getEstimatedTime() {
        return mEstimateTime;
    }

    @Override
    public ModelBoundary getBoundary() {
        return mModelBoundary;
    }

    @Override
    public Observable<Integer> getParseProgressObservable() {
        return mParseProgressSubject;
    }

    public int getHeaderType() {
        return mToolHead;
    }

    @Override
    public int getHeaderNameID() {
        return mToolHeadNameID;
    }

    public float getNozzle_0_Diameter() {
        return mNozzle_0_Diameter;
    }

    public float getNozzle_1_Diameter() {
        return mNozzle_1_Diameter;
    }

    public int getLayerNumber() {
        return mLayerNumber;
    }

    public float getLayerHeight() {
        return mLayerHeight;
    }

    public float getMaterialWeight() {
        return mMaterialWeight;
    }

    public float getMaterialLength() {
        return mMaterialLength;
    }

    public String getMaterial_0() {
        return mNozzle_0_Material;
    }

    public String getMaterial_1() {
        return mNozzle_1_Material;
    }

    public String getRenderMethod() {
        return mRenderMethod;
    }

    public int isContainRotation() {
        return mIsRotate;
    }

    public float getWorkSizeX() {
        return mWorkSizeX;
    }

    public float getWorkSizeY() {
        return mWorkSizeY;
    }

    public String getOrigin() {
        return mOrigin;
    }

    public float getNozzleTarget_1_Temperature() {
        return mNozzleTarget_1_Temperature;
    }

    @Override
    public float getExtruder0RetractionDistance() {
        return mExtruder0RetractionDistance;
    }

    @Override
    public float getExtruder1RetractionDistance() {
        return mExtruder1RetractionDistance;
    }

    @Override
    public float getExtruder0SwitchRetractionDistance() {
        return mExtruder0SwitchRetractionDistance;
    }

    @Override
    public float getExtruder1SwitchRetractionDistance() {
        return mExtruder1SwitchRetractionDistance;
    }

    @Override
    public int getCustomPrintMode() {
        return mCustomPrintMode;
    }

    @Override
    public boolean isApplyMultiExtruder() {
        // TODO: how to change extruder in DualExtruderFDM
        return (mIsDefineT0 && mIsDefineT1);
    }
}

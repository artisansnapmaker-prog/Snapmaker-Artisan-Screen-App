package fabscreen.features.machinetools.calibration.a400platform.laser.w_10.cameraCalibration;

import static fabscreen.platform.base.service.machine.controller.MachineOperationStatus.SYSTEM_STATUS_LASER_CAMERA_CAPTURE;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import fabscreen.features.machinetools.R;
import fabscreen.platform.base.helper.GsonHelper;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.file.FabLocalFile;
import fabscreen.platform.base.lib.file.IFile;
import fabscreen.platform.base.lib.parser.IGcodeParser;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.model.ILaserCameraController;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.service.machine.controller.MachineOperationStatus;
import fabscreen.platform.base.service.machine.controller.NewPrintController;
import fabscreen.platform.base.service.machine.controller.PrintEvent;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

public class A400CameraCalibration10wViewModel extends BaseViewModel {
    private final BehaviorSubject<Boolean> mWaitingSubject = BehaviorSubject.createDefault(false);
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    IPreferences mPreference;
    private IMachine mA400Machine;
    private NewPrintController mNewPrintController;
    private IPrintWorkspace mWorkspace;
    private IGcodeParser mParser;
    private Context mContext;
    private ILaserCameraController laserCameraController;
    private ArrayList<Point> mCorners = new ArrayList<>();
    private boolean nowEnclosureAvailableState;

    public A400CameraCalibration10wViewModel() {
        super();
        mA400Machine = ServiceContainer.getInstance().getService(IMachine.class);
        mWorkspace = ServiceContainer.getInstance().getService(IPrintWorkspace.class);
        mNewPrintController = mA400Machine.getNewPrintController();
        mParser = ServiceContainer.getInstance().getService(IGcodeParser.class);
        mPreference = ServiceContainer.getInstance().getService(IPreferences.class);
        laserCameraController = mA400Machine.getLaserController().getLaserCameraController();
        mContext = getServiceContainer().getService(IAppService.class).getAppContext();
    }

    public Observable<CameraCalibrationState> init() {
        initPoint();
        if (mA400Machine.getMachineInfoSubjectHolder().getValue().isEnclosureAvailable) {
            nowEnclosureAvailableState = mA400Machine.getMachineController().getEnclosure().getEnclosureStatusValue().isLedOn();
        }
        return Observable.zip(
                        initPrintFile(),
                        checkHome(0),
                        (checkHomeResponse,
                         printFileResponse) -> {
                            if (!printFileResponse) {
                                return CameraCalibrationState.INIT_PRINT_FILE_FAIL;
                            } else if (!checkHomeResponse) {
                                return CameraCalibrationState.CHECK_HOME_FAIL;
                            }
                            return CameraCalibrationState.CHECK_HOME_SUCCESS;
                        })
                .flatMap(cameraCalibrationState -> {
                    if (CameraCalibrationState.CHECK_HOME_SUCCESS.equals(cameraCalibrationState)) {
                        return initPosition().map(responseStructure -> responseStructure.isSuccess() ? CameraCalibrationState.READY_SUCCESS : CameraCalibrationState.MOVE_POSITION_FAIL);
                    }
                    return Observable.just(cameraCalibrationState);
                });
    }

    private void initPoint() {
        float x = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().size.getX() / 2;
        float y = ServiceContainer.getInstance().getService(IMachine.class).getMachineInfoSubjectHolder().getValue().size.getY() / 2;
        mCorners.add(new Point((int) (x - 50), (int) (y + 50)));
        mCorners.add(new Point((int) (x + 50), (int) (y + 50)));
        mCorners.add(new Point((int) (x + 50), (int) (y - 50)));
        mCorners.add(new Point((int) (x - 50), (int) (y - 50)));
    }

    public Observable<Boolean> checkHome(int index) {
        IMachine service = ServiceContainer.getInstance().getService(IMachine.class);
        if (!service.getMachineStatusSubjectHolder().getValue().isHomed) {
            return service.getMachineController().updateCoordinateSystem(0)
                    .flatMap(machineStatus -> service.getMachineController().home(0))
                    .flatMap(integer -> service.getMachineController().updateCoordinateSystem(index))
                    .flatMap(machineStatus -> Observable.just(machineStatus.isHomed));
        } else {
            return service.getMachineController().updateCoordinateSystem(index)
                    .flatMap(machineStatus -> Observable.just(machineStatus.isHomed));
        }
    }

    private Observable<Boolean> initPrintFile() {
        File printFile = copyPrintFile();
        if (printFile == null) {
            return Observable.just(false);
        }
        IFile mPrintFile = new FabLocalFile(printFile);
        mParser.destroy();
        mParser.startParse(mPrintFile, IMachine.WorkType.LASER);
        return mParser.getParseProgressObservable()
                .throttleLast(100, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .takeUntil(progress -> progress == 100)
                .filter(progress -> progress == -1 || progress == 100)
                .flatMap(progress -> {
                    if (progress == 100) {
                        mNewPrintController.reset();
                        mWorkspace.setPrintFile(mPrintFile);
                        mNewPrintController.setFile(mPrintFile);
                        mNewPrintController.setTotalLines(mParser.getTotalLinesCount());
                        return Observable.just(true);
                    } else {
                        return Observable.just(false);
                    }
                });
    }

    private File copyPrintFile() {
        InputStream is = mContext.getResources().openRawResource(R.raw.a400_laser_camera_calibration);
        File file = null;
        try {
            file = new File(mContext.getCacheDir().getAbsoluteFile() + "/10WLaserCameraCalibration.gcode");
            if (file.exists()) {
                file.delete();
            }
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                int read;
                byte[] bytes = new byte[20480];
                while ((read = is.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            }
        } catch (Exception e) {
            file = null;
            Logger.e("Copy print file error.");
            LogHelper.log(e);
        } finally {
            try {
                is.close();
            } catch (Exception ignored) {

            }
        }
        return file;
    }

    private Observable<ResponseStructure> initPosition() {
        float bottomZ = mA400Machine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getLaserFocalLength() + mA400Machine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getPlatformHeight();
        Vector vector = new Vector();
        vector.setX(mA400Machine.getMachineInfoSubjectHolder().getValue().size.getX() / 2);
        vector.setY(mA400Machine.getMachineInfoSubjectHolder().getValue().size.getY() / 2);
        vector.setZ(bottomZ);
        return mA400Machine.getMachineController().gotoAbsolutePosition(vector);
    }

    public Observable<Boolean> getWaitingObservable() {
        return mWaitingSubject.hide();
    }

    public boolean isPrinting() {
        return MachineOperationStatus.isPrinting(mNewPrintController.getPrintState());
    }

    public void requestMachineStop() {
        mWaitingSubject.onNext(true);
        mNewPrintController.stop();
    }

    public boolean isCalibrationMode() {
        return SYSTEM_STATUS_LASER_CAMERA_CAPTURE.valueEquals(mNewPrintController.getPrintState());
    }

    public void startPrint() {
        mWaitingSubject.onNext(true);
        mCompositeDisposable.clear();
        // Power Panic
        boolean powerOutageFlag = mNewPrintController.getRecoveryFlag();
        if (powerOutageFlag) {
            Logger.d("Try Power Loss recovering..");
            mNewPrintController.recover();
        } else {
            mNewPrintController.start();
        }
    }

    public Observable<Integer> getPrintStateObservable() {
        Observable<Integer> printStateObservable = mNewPrintController.getPrintStateObservable();
        printStateObservable
                .as(bindToLifecycle())
                .subscribe(integer -> mWaitingSubject.onNext(MachineOperationStatus.isPrintChange(integer)), LogHelper::log);
        return printStateObservable;
    }

    public Observable<PrintEvent> getPrintEventObservable() {
        Observable<PrintEvent> printEventObservable = mNewPrintController.getPrintEventObservable();
        printEventObservable
                .as(bindToLifecycle())
                .subscribe(printEvent -> mWaitingSubject.onNext(false), LogHelper::log);
        return printEventObservable;
    }

    public void setPowerOutageFlag(boolean flag) {
        mNewPrintController.setPowerOutageFlag(flag);
    }

    public Observable<Boolean> toDoTakePhoto() {
        String cameraCalibrationTakePhotoVector = mPreference.getHelper().getCameraCalibrationTakePhotoVector();
        Vector vectorByModuleID = new GsonHelper().getVectorByModuleID(cameraCalibrationTakePhotoVector,
                mA400Machine.getMachineInfoSubjectHolder().getValue().modelId,
                mA400Machine.getLaserController().getHeadType());
        return turnLight(true)
                .flatMap(success -> ServiceContainer.getInstance().getService(IMachine.class).getMachineController().gotoAbsolutePosition(vectorByModuleID, 1500))
                .flatMap(success -> (mA400Machine.getLaserController().getHeadType() == Module.ModuleType.HEAD_LASER_10W) ?
                        ServiceContainer.getInstance().getService(IMachine.class).getLaserController().getLaserCameraController().setPhotoQuality(10) :
                        Observable.just(success)
                )
                .flatMap(success -> ServiceContainer.getInstance().getService(IMachine.class).getLaserController().getLaserCameraController().requestCapturePhoto())
                .flatMap(success -> ServiceContainer.getInstance().getService(IMachine.class).getLaserController().getLaserCameraController().watchPhotoReceive())
                .flatMap(bitmap -> {
                    return turnLight(false).take(1).flatMap(aBoolean -> Observable.just(bitmap));
                })
                .flatMap(bitmap -> {
                    Logger.d("Capture image succeed.");
                    String path = ServiceContainer.getInstance().getService(IAppService.class).getCacheDir() + "/10WLaserCalibration.jpg";
                    Matrix m = new Matrix();
                    m.postRotate(90);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                    FileOutputStream out = new FileOutputStream(path);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    process(bitmap);
                    return Observable.just(true);
                });
    }

    public Observable<Boolean> turnLightAndReset() {
        return turnLight(false)
                .flatMap(aboo -> checkHome(1));
    }

    private Observable<Boolean> turnLight(boolean isOpen) {
//        laserCameraController
//                .setCameraAutoWhiteBalance(!isOpen)
//                .observeOn(AndroidSchedulers.mainThread())
//                .as(bindToLifecycle())
//                .subscribe(success -> {
//                    Log.d("DEBUG", "camera auto white balance " + !isOpen + success);
//                }, LogHelper::log);
        return laserCameraController.setCameraLighting(isOpen)
                .flatMap(aBoolean -> {
                    if (mA400Machine.getMachineInfoSubjectHolder().getValue().isEnclosureAvailable) {
                        return mA400Machine.getMachineController().getEnclosure().setEnclosureLedLevel(isOpen ? 100 : nowEnclosureAvailableState ? 100 : 0)
                                .take(1)
                                .flatMap(responseStructure -> Observable.just(responseStructure.isSuccess()));
                    } else {
                        return Observable.just(aBoolean);
                    }
                });
    }


    private Bitmap getGreyscaleImage(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Bitmap greyscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(greyscale);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorFilter);
        canvas.drawBitmap(image, 0, 0, paint);

        return greyscale;
    }

    private void process(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Bitmap greyscale = getGreyscaleImage(image);

        Equation top = getLine(greyscale, width * 2 / 5, height / 3, width / 5, height / 8
        );

        Equation bottom = getLine(greyscale, width * 2 / 5, height * 4 / 7, width / 5, height / 8);

        Equation left = getLine(greyscale, width * 3 / 10, height * 6 / 15, width / 7, height / 5);

        Equation right = getLine(greyscale, width * 3 / 5, height * 6 / 15, width / 7, height / 5);

        for (int x = 0; x < width; x++) {
            int y = Math.round(top.m * x + top.c);
            if (0 <= y && y < height) {
                image.setPixel(x, y, Color.GREEN);
            }
        }
        for (int x = 0; x < width; x++) {
            int y = Math.round(bottom.m * x + bottom.c);
            if (0 <= y && y < height) {

                image.setPixel(x, y, Color.GREEN);
            }
        }
        for (int y = 0; y < height; y++) {
            int x = Math.round((y - left.c) / left.m);
            if (0 <= x && x < width) {
                image.setPixel(x, y, Color.GREEN);
            }
        }
        for (int y = 0; y < height; y++) {
            int x = Math.round((y - right.c) / right.m);
            if (0 <= x && x < width) {
                image.setPixel(x, y, Color.GREEN);
            }
        }

        ArrayList<Point> points = new ArrayList<>();

        points.add(getCross(top, left));
        points.add(getCross(top, right));
        points.add(getCross(bottom, right));
        points.add(getCross(bottom, left));

        Log.d("DEBUG", "points " + points);
        Log.d("DEBUG", "mCorners " + mCorners);

        // save result into a JSONObject
        JSONObject result = new JSONObject();
        JSONArray jsonPoints = new JSONArray();
        JSONArray jsonCorners = new JSONArray();
        try {
            for (Point p : points) {
                JSONObject point = new JSONObject();
                point.put("x", p.x);
                point.put("y", p.y);
                jsonPoints.put(point);
            }

            for (Point c : mCorners) {
                JSONObject point = new JSONObject();
                point.put("x", c.x);
                point.put("y", c.y);
                jsonCorners.put(point);
            }

            result.put("points", jsonPoints);
            result.put("corners", jsonCorners);
        } catch (JSONException e) {
            LogHelper.log(e);
        }

        Log.d("DEBUG", "JSON \n " + result.toString());

        // save result in preferences
        Logger.d("Camera calibration result : %s", result.toString());
        mPreference.getHelper().set10WLaserCameraCalibration(result.toString());
    }

    public Observable<ResponseStructure> exitCalibration(boolean isSave) {
        return mA400Machine.getLaserController().exitCalibration(isSave);
    }

    private Equation getLine(Bitmap image, int x0, int y0, int width, int height) {
        // crop
        Bitmap cropped = Bitmap.createBitmap(image, x0, y0, width, height);
//        saveJpg(cropped, "laser_camera_calibration_" + mIndex++ + ".jpg");

        // binarization
        normalize(cropped);
        binarization(cropped);

        boolean useYRegression = (width < height);

        ArrayList<Point> points = new ArrayList<>();

        long[] sum = new long[4];
        Equation eq;
        while (true) {
            points.clear();
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int v = cropped.getPixel(x, y) & 0xff;
                    if (v == 0) {
                        if (useYRegression) {
                            points.add(new Point(y, x));
                        } else {
                            points.add(new Point(x, y));
                        }
                    }
                }
            }

            eq = regression(points);

            // calculate distance
            float maxDistance = 0;
            for (Point point : points) {
                float y = point.x * eq.m + eq.c;
                float dist = Math.abs(y - point.y);
                maxDistance = Math.max(maxDistance, dist);
            }

            if (maxDistance < 1) {
                break;
            }

            for (Point point : points) {
                float y = point.x * eq.m + eq.c;
                float dist = Math.abs(y - point.y);
                if (dist > maxDistance * 0.8) {
                    if (useYRegression) {
                        cropped.setPixel(point.y, point.x, Color.WHITE);
                    } else {
                        cropped.setPixel(point.x, point.y, Color.WHITE);
                    }
                }
            }
        }

        if (useYRegression) {
            eq.m = 1 / eq.m;
            eq.c = -(x0 + eq.c) * eq.m + y0;
        } else {
            eq.c = eq.c - eq.m * x0 + y0;
        }

        return eq;
    }

    private void normalize(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        long total = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int v = image.getPixel(x, y) & 0xff;
                total += v;
            }
        }
        double avg = total / (width * height);

        // divide image into cells and normalize the cell use image average color.
        final int cellSize = 30;
        for (int i = 0; i < width; i += cellSize) {
            for (int j = 0; j < height; j += cellSize) {
                int maxX = Math.min(i + cellSize, width);
                int maxY = Math.min(j + cellSize, height);

                total = 0;
                for (int x = i; x < maxX; x++) {
                    for (int y = j; y < maxY; y++) {
                        int v = image.getPixel(x, y) & 0xff;
                        total += v;
                    }
                }

                double cellAvg = total / ((maxX - i) * (maxY - j));
                int diff = (int) Math.round((cellAvg - avg) * 0.8);

                for (int x = i; x < maxX; x++) {
                    for (int y = j; y < maxY; y++) {
                        int v = image.getPixel(x, y) & 0xff;
                        int newValue = v - diff;
                        image.setPixel(x, y, 0xff000000 | (0x010101 * newValue));
                    }
                }
            }
        }

        // classic normalize
        int min = 255;
        int max = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int v = image.getPixel(x, y) & 0xff;

                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int v = image.getPixel(x, y) & 0xff;
                int newValue = (int) Math.round(255.0 * (v - min) / (max - min));
                image.setPixel(x, y, 0xff000000 | (0x010101 * newValue));
            }
        }
    }

    private Point getCross(Equation a, Equation b) {
        float x = -(a.c - b.c) / (a.m - b.m);
        float y = a.m * x + a.c;
        return new Point((int) x, (int) y);
    }

    private void saveJpg(Bitmap cropped, String name) {
        try {
            File file = new File(ServiceContainer.getInstance().getService(IAppService.class).getFilesDir(), name);
            FileOutputStream out = new FileOutputStream(file);
            cropped.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            Logger.d("saveJpg: " + e);
        }
    }

    private void binarization(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int v = image.getPixel(x, y) & 0xff;

                if (v >= 128) {
                    image.setPixel(x, y, Color.WHITE);
                } else {
                    image.setPixel(x, y, Color.BLACK);
                }
            }
        }
    }

    private float round(float number) {
        int factor = 1;
        for (int i = 0; i < 4; i++) {
            factor *= 10;
        }
        return 1.0f * Math.round(number * factor) / factor;
    }

    private Equation regression(ArrayList<Point> points) {
        long[] sum = new long[4];

        for (int i = 0; i < 4; i++) {
            sum[i] = 0;
        }

        for (Point point : points) {
            sum[0] += point.x;
            sum[1] += point.y;
            sum[2] += point.x * point.x;
            sum[3] += point.x * point.y;
        }

        long size = points.size();
        long run = size * sum[2] - sum[0] * sum[0];
        long rise = size * sum[3] - sum[0] * sum[1];
        float gradient = run == 0 ? 0 : round(1.0f * rise / run);
        float intercept = round(1.0f * sum[1] / size - gradient * sum[0] / size);

        return new Equation(gradient, intercept);
    }

    public enum CameraCalibrationState {
        INIT_PRINT_FILE_FAIL,
        CHECK_HOME_FAIL,
        CHECK_HOME_SUCCESS,
        MOVE_POSITION_FAIL,
        READY_SUCCESS;
    }

    class Equation {
        float m;
        float c;

        Equation(float gradient, float intercept) {
            this.m = gradient;
            this.c = intercept;
        }
    }
}

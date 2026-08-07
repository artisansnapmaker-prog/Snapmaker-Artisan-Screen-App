package fabscreen.features.print.a400platform.viewmodel;

import com.orhanobut.logger.Logger;

import fabscreen.platform.base.Constants;
import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.print.IPrintWorkspace;
import fabscreen.platform.base.model.ModelBoundary;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.IPreferences;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class PrintReadyViewModel extends BaseViewModel {
    private final IMachine mMachine;
    private int mPrepareMode = 0;
    //    private float mMaterialThickness = 0.5f;
    private float mAutoThickness = 0.5f;
    private final ModelBoundary mBoundary;
    private Vector mMachineVector = new Vector();
    private boolean mIsOriginIndicatorActive = false;
    private final BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.create();
    private final PublishSubject<Float> mThicknessMeasureSubject = PublishSubject.create();
    public BehaviorSubject<Float> mMaterialThicknessSubject = BehaviorSubject.createDefault(-1f);

    public PrintReadyViewModel() {
        mMachine = ServiceContainer.getInstance().getService(IMachine.class);
        mBoundary = ServiceContainer.getInstance().getService(IPrintWorkspace.class).getModelBoundary();
        mPrepareMode = ServiceContainer.getInstance().getService(IPreferences.class).getHelper().getLaserPrintZOriginMode();

        if (mMachine.getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.LASER) {
            float laserFocalLength = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getLaserFocalLength();
            Logger.d("print vm, focal len is: %f", laserFocalLength);
        }
    }

    public void onStop() {
        mMaterialThicknessSubject.onNext(-1f);
    }

    public IMachine getMachine() {
        return mMachine;
    }

    public int getPrepareMode() {
        return mPrepareMode;
    }

    public void setPrepareMode(int prepareMode) {
        mPrepareMode = prepareMode;
    }

    public Observable<Float> getMaterialThicknessObservable() {
        return mMaterialThicknessSubject.hide();
    }

    public void saveMaterialThickness(float thickness) {
        Logger.d("material thickness is: %s", thickness);
        mMaterialThicknessSubject.onNext(thickness);
//        mMaterialThickness = thickness;
    }

    public float getMaterialThicknessValue() {
        return mMaterialThicknessSubject.getValue();
    }

    public Observable<Float> getThicknessMeasure() {
        return mThicknessMeasureSubject.hide();
    }

    public boolean getIsRotaryAvailable() {
        return mMachine.getMachineInfoSubjectHolder().getValue().isRotaryAvailable;
    }

    public int getHeadType() {
        return mMachine.getMachineInfoSubjectHolder().getValue().headType;
    }

    public IMachine.WorkType getWorkType() {
        return mMachine.getMachineInfoSubjectHolder().getValue().workType;
    }

    public Observable<Boolean> getMovingObservable() {
        return mIsMovingSubject.hide();
    }

    public Observable<ResponseStructure> gotoAbsolutePosition(Vector vector) {
        return ServiceContainer.getInstance().getService(IMachine.class)
                .getMachineController()
                .gotoAbsolutePosition(vector, 6000);
    }

    public Observable<Boolean> runBoundary(boolean currentAsOrigin) {
        Observable<Boolean> observable;
        // If need to set current position as xy origin, set it first.
        if (currentAsOrigin) {
            boolean usingBAxis = (mBoundary.getDimension() == ModelBoundary.DIMENSION_BY);
            observable = usingBAxis ? setYZBOrigin() : setXYZOrigin();
        } else {
            observable = Observable.just(true);
        }
        return observable
                .doOnSubscribe(disposable -> mIsMovingSubject.onNext(true))
                .flatMap(success -> mMachine.getMachineController().goToBoundaryVertex(0, mBoundary, 1800))
                .flatMap(response -> Observable.just(response.isSuccess()))
                .doOnNext(success -> mIsMovingSubject.onNext(false))
                .doOnError(e -> mIsMovingSubject.onNext(false));
    }

    public Observable<Boolean> setXYOrigin() {
        Vector vector = new Vector();
        vector.setX(0);
        vector.setY(0);
        return mMachine.getMachineController().setWorkOrigin(vector)
                .flatMap(structure -> Observable.just(structure.isSuccess()));
    }

    public Observable<Boolean> setXYZOrigin() {
        Vector vector = new Vector();
        vector.setX(0);
        vector.setY(0);
        vector.setZ(0);
        return mMachine.getMachineController().setWorkOrigin(vector)
                .flatMap(structure -> Observable.just(structure.isSuccess()));
    }

    public Observable<Boolean> setYZBOrigin() {
        Vector vector = new Vector();
        vector.setY(0);
        vector.setZ(0);
        vector.setB(0);
        return mMachine.getMachineController().setWorkOrigin(vector)
                .flatMap(structure -> Observable.just(structure.isSuccess()));
    }

    public Observable<Boolean> moveToZ() {
        Observable<Boolean> returnObservable = Observable.just(false);
        mIsMovingSubject.onNext(true);
        if (getIsRotaryAvailable()) {
            switch (getHeadType()) {
                case Module.ModuleType.HEAD_LASER:
                    returnObservable = moveToZWithLaser1600mwModuleAndRotary();
                    break;
                case Module.ModuleType.HEAD_LASER_10W:
                    returnObservable = moveToZWithLaser10wModuleAndRotary();
                    break;
                case Module.ModuleType.HEAD_LASER_20W:
                    returnObservable = moveToZWithLaser20wModuleAndRotary();
                    break;
                case Module.ModuleType.HEAD_LASER_40W:
                    returnObservable = moveToZWithLaser40ModuleAndRotary();
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                    returnObservable = moveToZWithLaser2wModuleAndRotary();
                    break;
                default:
                    break;
            }
        } else {
            switch (getHeadType()) {
                case Module.ModuleType.HEAD_LASER:
                    returnObservable = moveToZWithLaser1600mwModule();
                    break;
                case Module.ModuleType.HEAD_LASER_10W:
                    returnObservable = moveToZWithLaser10wModule();
                    break;
                case Module.ModuleType.HEAD_LASER_20W:
                    returnObservable = moveToZWithLaser20wModule();
                    break;
                case Module.ModuleType.HEAD_LASER_40W:
                    returnObservable = moveToZWithLaser40wModule();
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                    returnObservable = moveToZWithLaser2wModule();
                    break;
                default:
                    break;
            }
        }
        return returnObservable
                .doOnNext(aBoolean -> mIsMovingSubject.onNext(false))
                .doOnError(throwable -> mIsMovingSubject.onNext(false));
    }

    private Observable<Boolean> moveToZWithLaser1600mwModuleAndRotary() {
        return Observable.just(false);
    }

    private Observable<Boolean> moveToZWithLaser10wModuleAndRotary() {
        Observable<Boolean> returnObservable = Observable.just(false);
        switch (mPrepareMode) {
            case 0:
                returnObservable = mMachine.getMachineController().updateCoordinateSystem(0)
                        .flatMap(status -> mMachine.getMachineController().getCurrentCoordinateObservable())
                        .flatMap(vector1 -> mMachine.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(vector1)))
                        .flatMap(vector2 -> {
                            float laserFocalLength = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getLaserFocalLength();
                            float axisCenterHeight = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getAxisCenterHeight();
                            Vector vector = new Vector();
                            vector.setZ(vector2.getZ() - laserFocalLength - axisCenterHeight - mMaterialThicknessSubject.getValue() / 2);
                            return mMachine.getMachineController().setWorkOrigin(vector);
                        })
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            case 1:
                Vector vector = new Vector();
                float laserFocalLength = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getLaserFocalLength();
                vector.setZ(-laserFocalLength);
                returnObservable = mMachine.getMachineController().setWorkOrigin(vector)
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            case 2:
                Vector vector1 = new Vector();
                vector1.setZ(0);
                returnObservable = mMachine.getMachineController().setWorkOrigin(vector1)
                        .flatMap(response -> Observable.just(response.isSuccess()));
                break;
            default:
        }
        return returnObservable;
    }

    private Observable<Boolean> moveToZWithLaser20wModuleAndRotary() {
        Observable<Boolean> returnObservable = Observable.just(false);
        switch (mPrepareMode) {
            case 0:
                returnObservable = mMachine.getMachineController().updateCoordinateSystem(0)
                        .flatMap(status -> mMachine.getMachineController().getCurrentCoordinateObservable())
                        .flatMap(vector1 -> mMachine.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(vector1)))
                        .flatMap(vector2 -> {
                            float axisCenterHeight = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getAxisCenterHeight();
                            Vector vector = new Vector();
                            vector.setZ(vector2.getZ() - axisCenterHeight - mMaterialThicknessSubject.getValue() / 2);
                            return mMachine.getMachineController().setWorkOrigin(vector);
                        })
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            case 1:
                Vector vector1 = new Vector();
                vector1.setZ(0);
                returnObservable = mMachine.getMachineController().setWorkOrigin(vector1)
                        .flatMap(response -> Observable.just(response.isSuccess()));
                break;
            default:
                break;
        }
        return returnObservable;
    }

    private Observable<Boolean> moveToZWithLaser40ModuleAndRotary() {
        Observable<Boolean> returnObservable = Observable.just(false);
        switch (mPrepareMode) {
            case 0:
                returnObservable = mMachine.getMachineController().updateCoordinateSystem(0)
                        .flatMap(status -> mMachine.getMachineController().getCurrentCoordinateObservable())
                        .flatMap(vector1 -> mMachine.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(vector1)))
                        .flatMap(vector2 -> {
                            float axisCenterHeight = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getAxisCenterHeight();
                            Vector vector = new Vector();
                            vector.setZ(vector2.getZ() - axisCenterHeight - mMaterialThicknessSubject.getValue() / 2);
                            return mMachine.getMachineController().setWorkOrigin(vector);
                        })
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            case 1:
                Vector vector1 = new Vector();
                vector1.setZ(0);
                returnObservable = mMachine.getMachineController().setWorkOrigin(vector1)
                        .flatMap(response -> Observable.just(response.isSuccess()));
                break;
            default:
                break;
        }
        return returnObservable;
    }

    private Observable<Boolean> moveToZWithLaser2wModuleAndRotary() {
        Observable<Boolean> returnObservable = Observable.just(false);
        switch (mPrepareMode) {
            case 0:
                returnObservable = mMachine.getMachineController().updateCoordinateSystem(0)
                        .flatMap(status -> mMachine.getMachineController().getCurrentCoordinateObservable())
                        .flatMap(vector1 -> mMachine.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(vector1)))
                        .flatMap(vector2 -> {
                            float axisCenterHeight = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getAxisCenterHeight();
                            Vector vector = new Vector();
                            vector.setZ(vector2.getZ() - axisCenterHeight - mMaterialThicknessSubject.getValue() / 2);
                            return mMachine.getMachineController().setWorkOrigin(vector);
                        })
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            case 1:
                Vector vector1 = new Vector();
                vector1.setZ(0);
                returnObservable = mMachine.getMachineController().setWorkOrigin(vector1)
                        .flatMap(response -> Observable.just(response.isSuccess()));
                break;
            default:
                break;
        }
        return returnObservable;
    }


    private Observable<Boolean> moveToZWithLaser1600mwModule() {
        return Observable.just(false);
    }

    private Observable<Boolean> moveToZWithLaser10wModule() {
        Observable<Boolean> returnObservable = Observable.just(false);
        switch (mPrepareMode) {
            case 0:
                returnObservable = autoThicknessMeasureToZ();
                break;
            case 1:
                returnObservable = mMachine.getMachineController().updateCoordinateSystem(0)
                        .flatMap(status -> mMachine.getMachineController().getCurrentCoordinateObservable())
                        .flatMap(vector1 -> mMachine.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(vector1)))
                        .flatMap(vector2 -> {
                            float laserFocalLength = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getLaserFocalLength();
                            float platformHeight = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getPlatformHeight();
                            Vector vector = new Vector();
                            vector.setZ(vector2.getZ() - laserFocalLength - platformHeight - mMaterialThicknessSubject.getValue());
                            return mMachine.getMachineController().setWorkOrigin(vector);
                        })
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            case 2:
                Vector vector = new Vector();
//                    float laserFocalLength = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getLaserFocalLength();
                vector.setZ(0);
                returnObservable = mMachine.getMachineController().setWorkOrigin(vector)
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            case 3:
                Vector vector1 = new Vector();
                vector1.setZ(0);
                returnObservable = mMachine.getMachineController().setWorkOrigin(vector1)
                        .flatMap(response -> Observable.just(response.isSuccess()));
                break;
            default:
        }
        return returnObservable;
    }

    private Observable<Boolean> moveToZWithLaser20wModule() {
        Observable<Boolean> returnObservable = Observable.just(false);
        switch (mPrepareMode) {
            case 0:
                returnObservable = mMachine.getMachineController().updateCoordinateSystem(0)
                        .flatMap(status -> mMachine.getMachineController().getCurrentCoordinateObservable())
                        .flatMap(vector1 -> mMachine.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(vector1)))
                        .flatMap(vector2 -> {
                            float platformHeight = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getPlatformHeight();
                            Vector vector = new Vector();
                            vector.setZ(vector2.getZ() - platformHeight - mMaterialThicknessSubject.getValue());
                            return mMachine.getMachineController().setWorkOrigin(vector);
                        })
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            case 1:
                Vector vector = new Vector();
//                    float laserFocalLength = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getLaserFocalLength();
                vector.setZ(0);
                returnObservable = mMachine.getMachineController().setWorkOrigin(vector)
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            default:
                break;
        }
        return returnObservable;
    }

    private Observable<Boolean> moveToZWithLaser40wModule() {
        Observable<Boolean> returnObservable = Observable.just(false);
        switch (mPrepareMode) {
            case 0:
                returnObservable = mMachine.getMachineController().updateCoordinateSystem(0)
                        .flatMap(status -> mMachine.getMachineController().getCurrentCoordinateObservable())
                        .flatMap(vector1 -> mMachine.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(vector1)))
                        .flatMap(vector2 -> {
                            float platformHeight = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getPlatformHeight();
                            Vector vector = new Vector();
                            vector.setZ(vector2.getZ() - platformHeight - mMaterialThicknessSubject.getValue());
                            return mMachine.getMachineController().setWorkOrigin(vector);
                        })
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            case 1:
                Vector vector = new Vector();
//                    float laserFocalLength = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getLaserFocalLength();
                vector.setZ(0);
                returnObservable = mMachine.getMachineController().setWorkOrigin(vector)
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            default:
                break;
        }
        return returnObservable;
    }

    private Observable<Boolean> moveToZWithLaser2wModule() {
        Observable<Boolean> returnObservable = Observable.just(false);
        switch (mPrepareMode) {
            case 0:
                returnObservable = mMachine.getMachineController().updateCoordinateSystem(0)
                        .flatMap(status -> mMachine.getMachineController().getCurrentCoordinateObservable())
                        .flatMap(vector1 -> mMachine.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(vector1)))
                        .flatMap(vector2 -> {
                            float platformHeight = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getPlatformHeight();
                            Vector vector = new Vector();
                            vector.setZ(vector2.getZ() - platformHeight - mMaterialThicknessSubject.getValue());
                            return mMachine.getMachineController().setWorkOrigin(vector);
                        })
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            case 1:
                Vector vector = new Vector();
//                    float laserFocalLength = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getLaserFocalLength();
                vector.setZ(0);
                returnObservable = mMachine.getMachineController().setWorkOrigin(vector)
                        .flatMap(structure -> {
                            Vector vector1 = new Vector();
                            vector1.setZ(0);
                            return mMachine.getMachineController().gotoAbsolutePosition(vector1);
                        })
                        .flatMap(structure -> Observable.just(structure.isSuccess()));
                break;
            default:
                break;
        }
        return returnObservable;
    }

    public Observable<Boolean> autoThicknessMeasureToZ() {
        mAutoThickness = -200;
        mIsMovingSubject.onNext(true);
        float initX = mMachine.getMachineInfoSubjectHolder().getValue().size.getX() * 0.5f - Constants.LASER_CAMERA_OFFSET_X + Constants.LASER_MEASURE_OFFSET_X;
        float initY = mMachine.getMachineInfoSubjectHolder().getValue().size.getY() * 0.5f - Constants.LASER_CAMERA_OFFSET_Y;
        //Measure height.
        float initZ = 170f;
        return ServiceContainer.getInstance().getService(IMachine.class).getNewPrintController().getAutoThickness(initX, initY, initZ, 1800)
                .flatMap(autoThickness -> {
                    mAutoThickness = autoThickness;
                    ServiceContainer.getInstance().getService(IPreferences.class).getHelper().setTestLaserAutoThickness(mAutoThickness);
                    mThicknessMeasureSubject.onNext(mAutoThickness);
                    return mAutoThickness != -200 ? mMachine.getMachineController().updateCoordinateSystem(0).flatMap(machineStatus -> Observable.just(true)) : Observable.just(false);
                })
                .flatMap(success -> {
                    if (success) {
                        return mMachine.getMachineController().getCurrentCoordinateObservable()
                                .flatMap(vector1 -> {
                                    mMachineVector = vector1;
                                    return Observable.just(true);
                                });
                    }

                    return Observable.just(false);
                })
                .flatMap(success -> success ? mMachine.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(true)) : Observable.just(success))
                .flatMap(success -> {
                    if (success) {
                        if (mAutoThickness == -200) return Observable.just(false);
                        float laserFocalLength = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getLaserFocalLength();
                        float platformHeight = mMachine.getLaserController().getLaserToolhead().getLaserToolHeadInfoValue().getPlatformHeight();
                        Vector vector3 = new Vector();
                        vector3.setZ(mMachineVector.getZ() - laserFocalLength - platformHeight - mAutoThickness);
                        return mMachine.getMachineController().setWorkOrigin(vector3).flatMap(machineStatus -> Observable.just(true));
                    } else {
                        return Observable.just(false);
                    }
                })
                .flatMap(success -> {
                    if (success) {
                        Vector vector1 = new Vector();
                        vector1.setZ(0);
                        return mMachine.getMachineController().gotoAbsolutePosition(vector1).flatMap(machineStatus -> Observable.just(true));
                    } else {
                        return Observable.just(success);
                    }
                });
    }

    public void updateMode() {
        IPreferences.Helper helper = ServiceContainer.getInstance().getService(IPreferences.class).getHelper();
        if (getIsRotaryAvailable()) {
            switch (getHeadType()) {
                case Module.ModuleType.HEAD_LASER:
                    mPrepareMode = helper.getFourAxisLaserPrintZOriginMode();
                    break;
                case Module.ModuleType.HEAD_LASER_10W:
                    mPrepareMode = helper.getFourAxis10WLaserPrintZOriginMode();
                    break;
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    mPrepareMode = helper.getFourAxisLaser20wPrintZOriginMode();
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                    mPrepareMode = helper.getFourAxisLaser2wPrintZOriginMode();
                    break;
            }
        } else {
            switch (getHeadType()) {
                case Module.ModuleType.HEAD_LASER:
                case Module.ModuleType.HEAD_LASER_10W:
                    mPrepareMode = helper.getLaserPrintZOriginMode();
                    break;
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    mPrepareMode = helper.getLaser20wPrintZOriginMode();
                    break;
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                    mPrepareMode = helper.getLaser2wPrintZOriginMode();
                    break;
            }
        }
    }

    public boolean isPrepareModeNeedMaterialHeight() {
        boolean needMaterialHeight = false;
        if (getIsRotaryAvailable()) {
            switch (getHeadType()) {
                case Module.ModuleType.HEAD_LASER:
                case Module.ModuleType.HEAD_LASER_10W:
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    needMaterialHeight = (mPrepareMode == 0);
            }
        } else {
            switch (getHeadType()) {
                case Module.ModuleType.HEAD_LASER:
                case Module.ModuleType.HEAD_LASER_2W_INFRARED:
                case Module.ModuleType.HEAD_LASER_20W:
                case Module.ModuleType.HEAD_LASER_40W:
                    needMaterialHeight = (mPrepareMode == 0);
                    break;
                case Module.ModuleType.HEAD_LASER_10W:
                    needMaterialHeight = (mPrepareMode == 1);
            }
        }
        return needMaterialHeight;
    }

    public Observable<Boolean> checkHome() {
        IMachine service = ServiceContainer.getInstance().getService(IMachine.class);
        if (!service.getMachineStatusSubjectHolder().getValue().isHomed) {
            mIsMovingSubject.onNext(true);
            return service.getMachineController().updateCoordinateSystem(0)
                    .flatMap(machineStatus -> service.getMachineController().home(0))
                    .flatMap(integer -> Observable.just(integer == 0))
                    .doOnNext(aBoolean -> mIsMovingSubject.onNext(false))
                    .flatMap(aBoolean -> {
                        if (aBoolean) {
                            if (service.getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM) {
                                return Observable.just(aBoolean);
                            } else {
                                return service.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(machineStatus.coordinateID == 1));
                            }
                        } else {
                            return Observable.just(aBoolean);
                        }
                    });
        } else {
            if (service.getMachineInfoSubjectHolder().getValue().workType == IMachine.WorkType.FDM) {
                return Observable.just(true);
            } else {
                return service.getMachineController().updateCoordinateSystem(1).flatMap(machineStatus -> Observable.just(machineStatus.coordinateID == 1));
            }
        }
    }

    public Observable<Boolean> checkMoveLaserReadyPosition() {
        IMachine service = ServiceContainer.getInstance().getService(IMachine.class);
        if (!service.getMachineStatusSubjectHolder().getValue().isHomed) {
            mIsMovingSubject.onNext(true);
            return service.getMachineController().updateCoordinateSystem(0)
                    .flatMap(machineStatus -> service.getMachineController().home(0))
                    .flatMap(machineStatus -> moveLaserReadyPosition())
                    .flatMap(integer -> service.getMachineController().updateCoordinateSystem(1))
                    .flatMap(machineStatus -> Observable.just(machineStatus.isHomed))
                    .doOnNext(aBoolean -> mIsMovingSubject.onNext(false));
        } else if (mPrepareMode == 0 && !getIsRotaryAvailable()) {
            mIsMovingSubject.onNext(true);
            Vector vector = new Vector();
            vector.setX(mMachine.getMachineInfoSubjectHolder().getValue().size.getX() * 0.5f - Constants.LASER_CAMERA_OFFSET_X + Constants.LASER_MEASURE_OFFSET_X);
            vector.setY(mMachine.getMachineInfoSubjectHolder().getValue().size.getY() * 0.5f - Constants.LASER_CAMERA_OFFSET_Y);
            vector.setZ(170f);
            return service.getMachineController().updateCoordinateSystem(0)
                    .flatMap(machineStatus -> service.getMachineController().gotoAbsolutePosition(vector, 6000))
                    .flatMap(integer -> service.getMachineController().updateCoordinateSystem(1))
                    .flatMap(machineStatus -> Observable.just(machineStatus.isHomed))
                    .doOnNext(aBoolean -> mIsMovingSubject.onNext(false));
        } else {
            return Observable.just(true);
        }
    }

    public void setOriginIndicatorState(boolean active) {
        int headType = mMachine.getMachineInfoSubjectHolder().getValue().headType;
        float power = mMachine.getLaserController().getAvailableLaserIndicatorPower();
        switch (headType) {
            case Module.ModuleType.HEAD_LASER:
            case Module.ModuleType.HEAD_LASER_10W:
                mMachine.getLaserController().setLaserPower(0, active ? power : 0f)
                        .as(bindToLifecycle())
                        .subscribe(response -> {
                            mIsOriginIndicatorActive = active;
                        }, LogHelper::log);
                break;
            case Module.ModuleType.HEAD_LASER_2W_INFRARED:
            case Module.ModuleType.HEAD_LASER_20W:
            case Module.ModuleType.HEAD_LASER_40W:
                mMachine.getLaserController().setCrossLineLaserPointer(active ? 1 : 0)
                        .as(bindToLifecycle())
                        .subscribe(response -> {
                            if (!response.isSuccess()) {
                                mIsOriginIndicatorActive = active;
                                Logger.d("Set");
                            }
                        }, LogHelper::log);
                break;
            default:
                break;
        }
    }

    public boolean isOriginIndicatorActive() {
        return mIsOriginIndicatorActive;
    }

    public Observable<ResponseStructure> moveLaserReadyPosition() {
        float initX = mMachine.getMachineInfoSubjectHolder().getValue().size.getX() * 0.5f;
        float initY = mMachine.getMachineInfoSubjectHolder().getValue().size.getY() * 0.5f;
        float initZ = mMachine.getMachineInfoSubjectHolder().getValue().size.getZ();
        if (mPrepareMode == 0 && !getIsRotaryAvailable()) {
            initX = mMachine.getMachineInfoSubjectHolder().getValue().size.getX() * 0.5f - Constants.LASER_CAMERA_OFFSET_X + Constants.LASER_MEASURE_OFFSET_X;
            initY = mMachine.getMachineInfoSubjectHolder().getValue().size.getY() * 0.5f - Constants.LASER_CAMERA_OFFSET_Y;
            initZ = 170f;
        }
        Vector vector = new Vector();
        vector.setX(initX);
        vector.setY(initY);
        vector.setZ(initZ);
        return ServiceContainer.getInstance().getService(IMachine.class).getMachineController().gotoAbsolutePosition(vector, 6000);
    }

    public Observable<ResponseStructure> setFocusAssistLight(boolean state) {
        return mMachine.getLaserController().switchFocusAssistLight(state ? 1 : 0);
    }

    public void setExposeTime(int i) {
        try {
            ServiceContainer.getInstance().getService(IMachine.class).getLaserController().getLaserCameraController().setExposeTime(i)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(aBoolean -> {
                    }, LogHelper::log);
        } catch (Exception e) {
            LogHelper.log(e);
        }
    }

}

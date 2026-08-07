package fabscreen.platform.base.service.machine.entity.toolhead;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.List;

import fabscreen.platform.base.R;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.IStructure;
import fabscreen.platform.base.service.machine.MachineConnectionController;
import fabscreen.platform.base.service.machine.entity.Module;
import fabscreen.platform.base.service.machine.entity.Toolhead;
import fabscreen.platform.base.service.machine.entity.parts.Fan;
import fabscreen.platform.base.service.machine.entity.parts.LaserTube;
import fabscreen.platform.base.service.machine.structure.BaseStructure;
import fabscreen.platform.base.service.machine.structure.LaserIndicatorPowerStructure;
import fabscreen.platform.base.service.machine.structure.LaserSafetyStateStructure;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.service.machine.structure.prop.ArrayProp;
import fabscreen.platform.base.service.machine.structure.prop.FloatProp;
import fabscreen.platform.base.service.machine.structure.prop.UInt8Prop;
import fabscreen.platform.lib.LogHelper;
import fabscreen.platform.lib.SubjectHolder;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import okio.Buffer;

public class LaserToolhead extends Toolhead {
    private BehaviorSubject<LaserToolheadInfo> mLaserToolheadInfoSubject = BehaviorSubject.createDefault(new LaserToolheadInfo());
    private SubjectHolder<LaserToolheadInfo> mLaserToolheadInfoSubjectHolder = new SubjectHolder<>(mLaserToolheadInfoSubject);
    private BehaviorSubject<LaserSafetyStateStructure> mLaserSafetyStateSubject = BehaviorSubject.createDefault(new LaserSafetyStateStructure());
    private SubjectHolder<LaserSafetyStateStructure> mLaserSafetyStateSubjectHolder = new SubjectHolder<>(mLaserSafetyStateSubject);
    private BehaviorSubject<LaserIndicatorPowerStructure> mLaserIndicatorPowerSubject = BehaviorSubject.createDefault(new LaserIndicatorPowerStructure(0, -1f));
    private SubjectHolder<LaserIndicatorPowerStructure> mLaserIndicatorPowerSubjectHolder = new SubjectHolder<>(mLaserIndicatorPowerSubject);
    // LaserShotPower class subject
    private final CompositeDisposable mDisposables = new CompositeDisposable();

    private String bluetoothMacAddress = "";
    IAppService mAppService;
    boolean isShow = false;
    ModuleInfo mModuleInfo;

    public LaserToolhead(ModuleInfo info, IMachine mc, MachineConnectionController cc, IAppService appService) {
        super(info, mc, cc);
        mModuleInfo = info;
        mAppService = appService;
    }

    @Override
    public void init() {
        Disposable subscribe = requestInfo()
                .subscribe(responseStructure -> {
                }, LogHelper::log);
        mDisposables.add(subscribe);

        ResponseStructure<BaseStructure> baseStructureResponseStructure = new ResponseStructure<>();
        BaseStructure laserTubeStateRequest = new BaseStructure() {
            @Override
            protected void init() {
                addProp("key", new UInt8Prop());
                addProp("currentPower", new FloatProp());
                addProp("targetPower", new FloatProp());
            }
        };
        baseStructureResponseStructure.dataProp = laserTubeStateRequest;

        subscribe = mConnectionController.watch(0x12, 0xa1, baseStructureResponseStructure).subscribe(response -> {
            BaseStructure responseStructure = response.dataProp;
            int key = (int) responseStructure.getProp("key").getValue();
            if (key == mModuleInfo.getKey()) {
                LaserToolheadInfo value = mLaserToolheadInfoSubject.getValue();
                LaserTube laserTube = new LaserTube();
                laserTube.setTargetPower((Float) responseStructure.getProp("targetPower").getValue());
                laserTube.setCurrentPower((Float) responseStructure.getProp("currentPower").getValue());
                value.setLaserTube(laserTube);
                mLaserToolheadInfoSubject.onNext(value);
            }
        });
        mDisposables.add(subscribe);

        subscribe = mConnectionController.watch(0x12, 0xa0, new ResponseStructure<>(new LaserSafetyStateStructure())).subscribe(response -> {
            LaserSafetyStateStructure laserSafetyStateStructure = response.dataProp;
            int key = laserSafetyStateStructure.getKey();
            if (key == mModuleInfo.getKey()) {
                if (laserSafetyStateStructure.getState() != 0) {
                    isShow = true;
                    String errorType = "";
                    String errorDesc = "";
                    // Build content data
                    if ((laserSafetyStateStructure.getState() & (1 << 1)) > 0) {
                        // Temperature over heated
                        errorType = getAppContext().getString(R.string.laser_10W_dialog_temperature_too_high);
                        errorDesc = "\n" + getAppContext().getString(R.string.laser_10W_dialog_temperature_too_high_desc);
                    } else if ((laserSafetyStateStructure.getState() & (1 << 2)) > 0) {
                        // laser not place correctly.
                        errorType = getAppContext().getString(R.string.laser_10W_dialog_not_placed_correctly);
                        errorDesc = "\n" + getAppContext().getString(R.string.laser_10W_dialog_not_placed_correctly_desc);
                    } else {
                        errorType = "Laser Safety Error : \n";
                        errorDesc = laserSafetyStateStructure.toString();
                    }

                    mAppService.dataPipe(errorType + errorDesc, true);
                } else if (isShow) {
                    isShow = false;
                    mAppService.dataPipe("", false);
                }
                mLaserSafetyStateSubject.onNext(laserSafetyStateStructure);
            }
        });
        mDisposables.add(subscribe);

        // Try requesting laser indicator power from tool head.
        // If firmware not supported or tool head did not had initial value,
        // we will keep it "invalid" for presenting status, but using default power when laser need to active.
        updateLaserIndicatorPower();
    }

    private float getIndicatorLaserDefaultTargetPower() {
        float targetPower = 0f;
        switch (mModuleInfo.getModuleId()) {
            case Module.ModuleType.HEAD_LASER:
                targetPower = 0.5f;
                break;
            case Module.ModuleType.HEAD_LASER_10W:
                targetPower = 1f;
                break;
            case Module.ModuleType.HEAD_LASER_20W:
            case Module.ModuleType.HEAD_LASER_40W:
                targetPower = 0.2f;
                break;
            case ModuleType.HEAD_LASER_2W_INFRARED:
                targetPower = 0f;
                break;
            default:
                break;
        }
        return targetPower;
    }

    @Override
    public String getDisplayName() {
        int headType = mModuleInfo.getModuleId();
        switch (headType) {
            case ModuleType.HEAD_LASER:
                return getAppContext().getString(R.string.all_tool_head_laser);
            case ModuleType.HEAD_LASER_10W:
                return getAppContext().getString(R.string.all_tool_head_laser_10w);
            case ModuleType.HEAD_LASER_20W:
                return getAppContext().getString(R.string.all_tool_head_laser_20w);
            case ModuleType.HEAD_LASER_40W:
                return getAppContext().getString(R.string.all_tool_head_laser_40w);
            case ModuleType.HEAD_LASER_2W_INFRARED:
                return getAppContext().getString(R.string.all_tool_head_laser_2w_infrared);
            default:
                return "unknown laser module";
        }
    }

    public String getBluetoothMacAddress() {
        return bluetoothMacAddress;
    }

    public void setBluetoothMacAddress(String macAddress) {
        this.bluetoothMacAddress = macAddress;
    }

    @Override
    public Observable<ResponseStructure<LaserToolheadInfo>> requestInfo() {
        BaseStructure laserRequest = new BaseStructure() {
            @Override
            protected void init() {
                addProp("key", new UInt8Prop());
            }
        };
        laserRequest.getProp("key").setValue(getModuleInfo().getKey());
        LaserToolheadInfo laserToolheadInfo = new LaserToolheadInfo();
        laserToolheadInfo.fansListProp.addElement(new Fan());
        ResponseStructure<LaserToolheadInfo> laserToolheadInfoResponseStructure = new ResponseStructure<>();
        laserToolheadInfoResponseStructure.dataProp = laserToolheadInfo;
        // FIXME: 2022/1/27 request multiple times
        return mConnectionController.request(0x12, 0x01, laserRequest, laserToolheadInfoResponseStructure)
                .doOnNext(responseStructure -> {
                    mLaserToolheadInfoSubject.onNext(responseStructure.dataProp);
                });
    }

    private void updateLaserIndicatorPower() {
        BaseStructure baseStructure = new BaseStructure() {
            @Override
            protected void init() {
                addProp("key", new UInt8Prop(getModuleInfo().getKey()));
            }
        };
        BaseStructure dataStructure = new BaseStructure() {
            @Override
            protected void init() {
                addProp("power", new FloatProp());
            }
        };
        ResponseStructure responseStructure = new ResponseStructure();
        responseStructure.dataProp = dataStructure;
        Disposable subscribe = mConnectionController.request(0x12, 0x13, baseStructure, responseStructure)
                .subscribe(respnse -> {
                    if (respnse.isSuccess()) {
                        LaserIndicatorPowerStructure laserIndicatorPowerStructure = new LaserIndicatorPowerStructure();
                        laserIndicatorPowerStructure.readBuffer(new Buffer().write(respnse.dataProp.toByteArray()));
                        Logger.d("get laser indicator power %.2f", laserIndicatorPowerStructure.getLaserIndicatorPower());
                        mLaserIndicatorPowerSubject.onNext(laserIndicatorPowerStructure);
                    } else {
                        Logger.w("Fetch laser indicator power failed, returning %d", respnse.resultProp.getValue());
                    }
                }, LogHelper::log);
        mDisposables.add(subscribe);
    }

    public Observable<ResponseStructure> requestLaserIndicatorPower() {
        BaseStructure structure = new BaseStructure() {
            @Override
            protected void init() {
                addProp("key", new UInt8Prop());
            }
        };
        structure.getProp("key").setValue(mModuleInfo.getKey());

        BaseStructure dataStructure = new BaseStructure() {
            @Override
            protected void init() {
                addProp("power", new FloatProp());
            }
        };
        ResponseStructure responseStructure = new ResponseStructure();
        responseStructure.dataProp = dataStructure;
        return mConnectionController.request(0x12, 0x13, structure, responseStructure)
                .doOnNext(response -> {
                    if (response.isSuccess()) {
                        LaserIndicatorPowerStructure laserIndicatorPowerStructure = new LaserIndicatorPowerStructure();
                        laserIndicatorPowerStructure.readBuffer(new Buffer().write(responseStructure.dataProp.toByteArray()));
                        mLaserIndicatorPowerSubject.onNext(laserIndicatorPowerStructure);
                    }
                });
    }

    public Observable<ResponseStructure> setLaserIndicatorPower(float power) {
        BaseStructure structure = new BaseStructure() {
            @Override
            protected void init() {
                addProp("key", new UInt8Prop());
                addProp("power", new FloatProp());
            }
        };
        structure.getProp("key").setValue(mModuleInfo.getKey());
        structure.getProp("power").setValue(power);

        return mConnectionController.request(0x12, 0x14, structure, new ResponseStructure())
                .doOnNext(responseStructure -> updateLaserIndicatorPower());
    }

    public Observable<LaserToolheadInfo> getLaserToolHeadInfoObservable() {
        return mLaserToolheadInfoSubjectHolder.getObservable();
    }

    public LaserToolheadInfo getLaserToolHeadInfoValue() {
        return mLaserToolheadInfoSubjectHolder.getValue();
    }

    public Observable<LaserSafetyStateStructure> getLaserSafetyStateObservable() {
        return mLaserSafetyStateSubjectHolder.getObservable();
    }

    public LaserSafetyStateStructure getLaserSafetyStateValue() {
        return mLaserSafetyStateSubjectHolder.getValue();
    }

    public Observable<LaserIndicatorPowerStructure> getLaserIndicatorPowerObservable() {
        return mLaserIndicatorPowerSubjectHolder.getObservable();
    }

    public LaserIndicatorPowerStructure getLaserIndicatorPowerValue() {
        return mLaserIndicatorPowerSubjectHolder.getValue();
    }

    // TODO: need to return null or empty value for settings
    public float getAvailableLaserIndicatorPower() {
        float indicatorPower = getIndicatorLaserDefaultTargetPower();
        if (mLaserIndicatorPowerSubjectHolder.getValue().getLaserIndicatorPower() < 0) {
            Logger.d("Get default target power %.2f.", indicatorPower);
        } else {
            indicatorPower = mLaserIndicatorPowerSubjectHolder.getValue().getLaserIndicatorPower();
            Logger.d("Get Laser Indicator power %.2f.", indicatorPower);
            float minLimit = 0f;
            switch (mModuleInfo.getModuleId()) {
                case ModuleType.HEAD_LASER:
                case ModuleType.HEAD_LASER_10W:
                    minLimit = 0.5f;
                    break;
                case ModuleType.HEAD_LASER_20W:
                case ModuleType.HEAD_LASER_40W:
                    minLimit = 0.2f;
                    break;
                case ModuleType.HEAD_LASER_2W_INFRARED:
                    minLimit = 0f;
                    break;
            }
            indicatorPower = Math.max(minLimit, Math.min(indicatorPower, 3f));
        }
        return indicatorPower;
    }

    @Override
    public void reset() {
        mDisposables.clear();
    }

    public static class LaserToolheadInfo implements IStructure {
        private UInt8Prop keyProp = new UInt8Prop();
        private UInt8Prop headStatusProp = new UInt8Prop();
        private FloatProp laserFocalLengthProp = new FloatProp();
        private FloatProp platformHeightProp = new FloatProp();
        private FloatProp axisCenterHeightProp = new FloatProp();
        private LaserTube laserTubeProp = new LaserTube();
        private ArrayProp<Fan> fansListProp = new ArrayProp<>();

        @Override
        public byte[] toByteArray() {
            Buffer buffer = new Buffer();
            buffer.write(keyProp.toByteArray());
            buffer.write(headStatusProp.toByteArray());
            buffer.write(laserFocalLengthProp.toByteArray());
            buffer.write(platformHeightProp.toByteArray());
            buffer.write(axisCenterHeightProp.toByteArray());
            buffer.write(laserTubeProp.toByteArray());
            buffer.write(fansListProp.toByteArray());
            return buffer.readByteArray();
        }

        @Override
        public Buffer readBuffer(Buffer buffer) throws IOException {
            keyProp.readBuffer(buffer);
            headStatusProp.readBuffer(buffer);
            laserFocalLengthProp.readBuffer(buffer);
            platformHeightProp.readBuffer(buffer);
            axisCenterHeightProp.readBuffer(buffer);
            laserTubeProp.readBuffer(buffer);
            fansListProp.readBuffer(buffer);
            return buffer;
        }

        public int getKey() {
            return keyProp.getValue();
        }

        public void setKey(int key) {
            keyProp.setValue(key);
        }

        public int getHeadStatus() {
            return headStatusProp.getValue();
        }

        public void setHeadStatus(int headStatus) {
            headStatusProp.setValue(headStatus);
        }

        public float getLaserFocalLength() {
            return laserFocalLengthProp.getValue();
        }

        public void setLaserFocalLength(float laserFocalLength) {
            laserFocalLengthProp.setValue(laserFocalLength);
        }

        public void setPlatformHeight(float platformHeight) {
            platformHeightProp.setValue(platformHeight);
        }

        public float getPlatformHeight() {
            return platformHeightProp.getValue();
        }

        public void setAxisCenterHeight(float height) {
            axisCenterHeightProp.setValue(height);
        }

        public float getAxisCenterHeight() {
            return axisCenterHeightProp.getValue();
        }

        public LaserTube getLaserTube() {
            return laserTubeProp;
        }

        public void setLaserTube(LaserTube laserTube) {
            laserTubeProp = laserTube;
        }

        public List<Fan> getFans() {
            return fansListProp.getValue();
        }

        public void setFansList(List<Fan> fansList) {
            fansListProp.setValue(fansList);
        }

        @Override
        public String toString() {
            return "LaserToolheadInfo{" +
                    "keyProp=" + keyProp +
                    ", headStatusProp=" + headStatusProp +
                    ", laserFocalLengthProp=" + laserFocalLengthProp +
                    ", platformHeightProp=" + platformHeightProp +
                    ", axisCenterHeightProp=" + axisCenterHeightProp +
                    ", laserTubeProp=" + laserTubeProp +
                    ", fansListProp=" + fansListProp +
                    '}';
        }
    }
}

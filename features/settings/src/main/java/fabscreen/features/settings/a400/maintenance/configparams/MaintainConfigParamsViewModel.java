package fabscreen.features.settings.a400.maintenance.configparams;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.structure.BaseStructure;
import fabscreen.platform.base.service.machine.structure.DeviationStructure;
import fabscreen.platform.base.service.machine.structure.FDMZOffsetStructure;
import fabscreen.platform.base.service.machine.structure.LaserIndicatorPowerStructure;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import fabscreen.platform.base.service.machine.structure.ZOffsetInfo;
import fabscreen.platform.base.service.machine.structure.prop.ArrayProp;
import fabscreen.platform.base.view.BaseViewModel;
import fabscreen.platform.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import okio.Buffer;

public class MaintainConfigParamsViewModel extends BaseViewModel {

    private final BehaviorSubject<String> mFDMZOffset0Subj = BehaviorSubject.create();
    private final BehaviorSubject<String> mFDMZOffset1Subj = BehaviorSubject.create();
    private final BehaviorSubject<String> mFDMXOffsetSubj = BehaviorSubject.create();
    private final BehaviorSubject<String> mFDMYOffsetSubj = BehaviorSubject.create();
    private final BehaviorSubject<String> mFocalLenSubj = BehaviorSubject.create();
    private final BehaviorSubject<String> mPlatformHeightSubj = BehaviorSubject.create();
    private final BehaviorSubject<String> mRotaryCenterHeightSubj = BehaviorSubject.create();
    private final BehaviorSubject<String> mFireSensorSensitivitySubj = BehaviorSubject.create();
    private final BehaviorSubject<String> mCrossLineIndicatorXOffsetSubj = BehaviorSubject.create();
    private final BehaviorSubject<String> mCrossLineIndicatorYOffsetSubj = BehaviorSubject.create();
    private final BehaviorSubject<String> mLaserIndicatorPowerSubj = BehaviorSubject.create();

    private final IMachine mMachine;

    public MaintainConfigParamsViewModel() {
        //noinspection deprecation
        mMachine = getServiceContainer().getService(IMachine.class);
    }

    public int getHeadType() {
        return mMachine.getMachineInfoSubjectHolder().getValue().headType;
    }

    public void fetchZOffset() {
        mMachine.getFDMController().getZOffset(0)
                .as(bindToLifecycle())
                .subscribe(response -> {
                    if (response.isSuccess()) {
                        FDMZOffsetStructure fdmzOffsetStructure = new FDMZOffsetStructure();
                        fdmzOffsetStructure.readBuffer(new Buffer().write(response.dataProp.toByteArray()));
                        List<ZOffsetInfo> zOffsetInfoList = fdmzOffsetStructure.getZOffsetInfoList();
                        mFDMZOffset0Subj.onNext(String.valueOf(zOffsetInfoList.get(0).getZOffset()));
                        if (zOffsetInfoList.size() > 1) {
                            mFDMZOffset1Subj.onNext(String.valueOf(zOffsetInfoList.get(1).getZOffset()));
                        }
                    }
                }, LogHelper::log);
    }

    @SuppressWarnings("rawtypes")
    public void fetchXYOffset() {
        mMachine.getFDMController().getExtruderOffset(0)
                .as(bindToLifecycle())
                .subscribe(response -> {
                    if (response.isSuccess()) {
                        // noinspection unchecked
                        ArrayProp<DeviationStructure> dataProp = (ArrayProp) response.dataProp;
                        ArrayList<DeviationStructure> value = (ArrayList<DeviationStructure>) dataProp.getValue();
                        mFDMXOffsetSubj.onNext(String.valueOf(value.get(0).getValue()));
                        mFDMYOffsetSubj.onNext(String.valueOf(value.get(1).getValue()));
                    }
                });
    }

    public void fetchFocalLength() {
        mFocalLenSubj.onNext(String.valueOf(mMachine.getLaserController().getLaserToolHeadInfoValue().getLaserFocalLength()));
    }

    public void fetchPlatformHeight() {
        mPlatformHeightSubj.onNext(String.valueOf(mMachine.getLaserController().getLaserToolHeadInfoValue().getPlatformHeight()));
    }

    public void fetch4AxisCenterHeight() {
        mRotaryCenterHeightSubj.onNext(String.valueOf(mMachine.getLaserController().getLaserToolHeadInfoValue().getAxisCenterHeight()));
    }

    public void fetchFireSensorSensitivity() {
        mMachine.getLaserController()
                .getFireSensorSensitivity(0)
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    if (responseStructure.isSuccess()) {
                        BaseStructure structure = (BaseStructure) responseStructure.dataProp;
                        mFireSensorSensitivitySubj.onNext(String.valueOf(structure.getProp("value").getValue()));
                    }
                });
    }

    public void fetchCrossLineIndicatorOffset() {
        mMachine.getLaserController().getCrossLineIndicatorOffset(0)
                .as(bindToLifecycle())
                .subscribe(responseStructure -> {
                    if (responseStructure.isSuccess()) {
                        BaseStructure structure = (BaseStructure) responseStructure.dataProp;
                        mCrossLineIndicatorXOffsetSubj.onNext(String.valueOf(structure.getProp("indicatorXOffset").getValue()));
                        mCrossLineIndicatorYOffsetSubj.onNext(String.valueOf(structure.getProp("indicatorYOffset").getValue()));
                    }
                });
    }

    public void fetchLaserIndicatorPower() {
       mMachine.getLaserController().requestLaserIndicatorPower().as(bindToLifecycle()).subscribe(responseStructure -> {
           if (responseStructure.isSuccess()) {
               LaserIndicatorPowerStructure laserIndicatorPowerStructure = new LaserIndicatorPowerStructure();
               laserIndicatorPowerStructure.readBuffer(new Buffer().write(responseStructure.dataProp.toByteArray()));
               float laserValue = laserIndicatorPowerStructure.getLaserIndicatorPower();
               Logger.d("get laser indicator result %.2f", laserValue);
               mLaserIndicatorPowerSubj.onNext(String.valueOf(laserValue));
           } else {
               Logger.w("Fetch laser indicator power failed %d", responseStructure.resultProp.getValue());
           }
       }, LogHelper::log);
    }

    public void setZOffset(int extruderIndex, float value) {
        mMachine.getFDMController().setZOffset(0, extruderIndex, value)
                .as(bindToLifecycle())
                .subscribe(response -> fetchZOffset(), LogHelper::log);
    }

    public void setXOffset(float value) {
        List<DeviationStructure> offsets = new ArrayList<>();
        offsets.add(new DeviationStructure(1, 0, value));
        mMachine.getFDMController().setExtruderOffset(0, offsets);
        fetchXYOffset();
    }

    public void setYOffset(float value) {
        List<DeviationStructure> offsets = new ArrayList<>();
        offsets.add(new DeviationStructure(1, 1, value));
        mMachine.getFDMController().setExtruderOffset(0, offsets);
        fetchXYOffset();
    }

    public void setFocalLen(float value) {
        mMachine.getLaserController().setFocalLength(value)
                .as(bindToLifecycle())
                .subscribe(response -> fetchFocalLength(), LogHelper::log);
    }

    public void setPlatformHeight(float value) {
        mMachine.getLaserController().setPlatformHeight(value)
                .as(bindToLifecycle())
                .subscribe(response -> fetchPlatformHeight(), LogHelper::log);
    }

    public void setRotaryCenterHeight(float value) {
        mMachine.getLaserController().setRotaryAxisCenterHeight(value)
                .as(bindToLifecycle())
                .subscribe(response -> fetch4AxisCenterHeight(), LogHelper::log);
    }

    public void setFireSensorSensitivity(int value) {
        mMachine.getLaserController().setFireSensorSensitivity(0, value)
                .as(bindToLifecycle()).subscribe(response -> fetchFireSensorSensitivity(), LogHelper::log);
    }

    public void setCrossLineIndicatorXOffset(float xOffset) {
        mMachine.getLaserController().setCrossLineIndicatorOffset(0, xOffset, Float.parseFloat(mCrossLineIndicatorYOffsetSubj.getValue()))
                .as(bindToLifecycle())
                .subscribe(response -> fetchCrossLineIndicatorOffset(), LogHelper::log);
    }

    public void setCrossLineIndicatorYOffset(float yOffset) {
        mMachine.getLaserController().setCrossLineIndicatorOffset(0, Float.parseFloat(mCrossLineIndicatorXOffsetSubj.getValue()), yOffset)
                .as(bindToLifecycle())
                .subscribe(response -> fetchCrossLineIndicatorOffset(), LogHelper::log);
    }

    public Observable<ResponseStructure> setLaserIndicatorPower(float power) {
        return mMachine.getLaserController()
                .setLaserIndicatorPower(0, power)
                .doOnNext(responseStructure -> fetchLaserIndicatorPower());
    }

    public Observable<String> getZOffset0Observable() {
        return mFDMZOffset0Subj.hide();
    }

    public Observable<String> getZOffset1Observable() {
        return mFDMZOffset1Subj.hide();
    }

    public Observable<String> getXOffsetObservable() {
        return mFDMXOffsetSubj.hide();
    }

    public Observable<String> getYOffsetObservable() {
        return mFDMYOffsetSubj.hide();
    }

    public Observable<String> getFocalLenObservable() {
        return mFocalLenSubj.hide();
    }

    public Observable<String> getPlatformHeightObservable() {
        return mPlatformHeightSubj.hide();
    }

    public Observable<String> getRotaryCenterHeightObservable() {
        return mRotaryCenterHeightSubj.hide();
    }

    public Observable<String> getFireSensorSensorSensitivityObservable() {
        return mFireSensorSensitivitySubj.hide();
    }

    public Observable<String> getCrossLineIndicatorOffsetXObservable() {
        return mCrossLineIndicatorXOffsetSubj.hide();
    }

    public Observable<String> getCrossLineIndicatorOffsetYObservable() {
        return mCrossLineIndicatorYOffsetSubj.hide();
    }

    public Observable<String> getLaserIndicatorPowerObservable() {
        return mLaserIndicatorPowerSubj.hide();
    }

    public String getCurrentRotaryCenterHeight() {
        return mRotaryCenterHeightSubj.getValue();
    }

    public String getCurrentPlatformHeight() {
        return mPlatformHeightSubj.getValue();
    }

    public String getCurrentFocalLength() {
        return mFocalLenSubj.getValue();
    }

    public String getCurrentYOffset() {
        return mFDMYOffsetSubj.getValue();
    }

    public String getCurrentXOffset() {
        return mFDMXOffsetSubj.getValue();
    }

    public String getCurrentZOffset(int index) {
        return index == 0 ? mFDMZOffset0Subj.getValue() : mFDMZOffset1Subj.getValue();
    }

    public String getFireSensorSensitivity() {
        return mFireSensorSensitivitySubj.getValue();
    }

    public String getCrossLineIndicatorXOffset() {
        return mCrossLineIndicatorXOffsetSubj.getValue();
    }

    public String getCrossLineIndicatorYOffset() {
        return mCrossLineIndicatorYOffsetSubj.getValue();
    }

    public String getLaserIndicatorPower() {
        return mLaserIndicatorPowerSubj.getValue();
    }

    public boolean isRotary() {
        return mMachine.getMachineInfoSubjectHolder().getValue().isRotaryAvailable;
    }

    public IMachine.WorkType getWorkType() {
        return mMachine.getMachineInfoSubjectHolder().getValue().workType;
    }

}

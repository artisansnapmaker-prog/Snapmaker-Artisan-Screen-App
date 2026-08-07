package fabscreen.platform.core.ui.data;

public class FilamentState {
    private boolean mIsFailureState;
    private boolean mLeftFailureState;
    private boolean mRightFailureState;
    private boolean mLeftNowFilamentState;
    private boolean mRightNowFilamentState;
    private boolean mLeftFilamentStateChange;
    private boolean mRightFilamentStateChange;

    private float mLeftExtruderTargetTemp;
    private float mRightExtruderTargetTemp;
    private boolean isLeftTemperatureReached;
    private boolean isRightTemperatureReached;

    private int mFailureFilamentIndex;
    private int mExtruderNum;

    public int getFailureFilamentIndex() {
        return mFailureFilamentIndex;
    }

    public int getExtruderNum() {
        return mExtruderNum;
    }


    public FilamentState setExtruderNum(int extruderNum) {
        mExtruderNum = extruderNum;
        return this;
    }

    public FilamentState setIsFailureState(boolean isFailureState) {
        mIsFailureState = isFailureState;
        if (mExtruderNum == 1) {
            if (mLeftFilamentStateChange) {
                mLeftFailureState = mLeftNowFilamentState;
                mFailureFilamentIndex = 0;
                mLeftFilamentStateChange = false;
            } else if (mRightFilamentStateChange) {
                mRightFailureState = mRightNowFilamentState;
                mFailureFilamentIndex = 1;
                mRightFilamentStateChange = false;
            }
        } else if (mExtruderNum == 2) {
            mLeftFailureState = mLeftNowFilamentState;
            mRightFailureState = mRightNowFilamentState;
            if (mLeftFilamentStateChange) {
                mFailureFilamentIndex = 0;
                mLeftFilamentStateChange = false;
            } else if (mRightFilamentStateChange) {
                mFailureFilamentIndex = 1;
                mRightFilamentStateChange = false;
            }
        }
        return this;
    }


    public FilamentState setFilamentState(int index, boolean extruderFilamentStatus, float extruderTargetTemp, boolean isTemperatureReached, boolean isActivation) {
        if (index == 0) {
            if (mLeftNowFilamentState != extruderFilamentStatus) {
                mLeftFilamentStateChange = true;
            }
            mLeftNowFilamentState = extruderFilamentStatus;
            if (!mIsFailureState) {
                mLeftExtruderTargetTemp = extruderTargetTemp;
            }
            isLeftTemperatureReached = isTemperatureReached;
            if (isActivation && extruderFilamentStatus) {
                mFailureFilamentIndex = 0;
            }
        } else if (index == 1) {
            if (mRightNowFilamentState != extruderFilamentStatus) {
                mRightFilamentStateChange = true;
            }
            mRightNowFilamentState = extruderFilamentStatus;
            if (!mIsFailureState) {
                mRightExtruderTargetTemp = extruderTargetTemp;
            }
            isRightTemperatureReached = isTemperatureReached;
            if (isActivation && extruderFilamentStatus) {
                mFailureFilamentIndex = 1;
            }
        }
        return this;
    }

    public float getTarget() {
        return mFailureFilamentIndex == 0 ? mLeftExtruderTargetTemp : mRightExtruderTargetTemp;
    }

    public float getLeftTarget() {
        return mLeftExtruderTargetTemp;
    }

    public float getRightTarget() {
        return mRightExtruderTargetTemp;
    }

    public boolean isTemperatureReached() {
        return mFailureFilamentIndex == 0 ? isLeftTemperatureReached : isRightTemperatureReached;
    }

    public boolean getNowFilamentState() {
        return mFailureFilamentIndex == 0 ? mLeftNowFilamentState : mRightNowFilamentState;
    }

    @Override
    public String toString() {
        return "FilamentState{" +
                "mIsFailureState=" + mIsFailureState +
                ", mLeftFailureState=" + mLeftFailureState +
                ", mRightFailureState=" + mRightFailureState +
                ", mLeftNowFilamentState=" + mLeftNowFilamentState +
                ", mRightNowFilamentState=" + mRightNowFilamentState +
                ", mLeftFilamentStateChange=" + mLeftFilamentStateChange +
                ", mRightFilamentStateChange=" + mRightFilamentStateChange +
                ", mLeftExtruderTargetTemp=" + mLeftExtruderTargetTemp +
                ", mRightExtruderTargetTemp=" + mRightExtruderTargetTemp +
                ", isLeftTemperatureReached=" + isLeftTemperatureReached +
                ", isRightTemperatureReached=" + isRightTemperatureReached +
                ", mFailureFilamentIndex=" + mFailureFilamentIndex +
                ", mExtruderNum=" + mExtruderNum +
                '}';
    }
}

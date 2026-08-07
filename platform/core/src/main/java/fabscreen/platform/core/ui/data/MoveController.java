package fabscreen.platform.core.ui.data;

import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.Vector;
import fabscreen.platform.base.service.machine.controller.MachineController;
import fabscreen.platform.base.service.machine.structure.ResponseStructure;
import io.reactivex.Observable;


public class MoveController {

    private final MachineController mMachineController;

    private MoveController() {
        IMachine machine = ServiceContainer.getInstance().getService(IMachine.class);
        mMachineController = machine.getMachineController();
    }

    public static MoveController getInstance() {
        return XYZMoveControllerHolder.INSTANCE;
    }

    @Deprecated
    public Observable<ResponseStructure> moveByStep(Direction direction, float stepWidth) {
        return stepToPosition(direction, stepWidth);
    }

    public Observable<ResponseStructure> stepToPosition(Direction direction, float stepWidth) {
        return stepToPosition(direction, stepWidth, 1800);
    }

    public Observable<ResponseStructure> stepToPosition(Direction direction, float stepWidth, int feedrate) {
        // Get latest position and move to new position.
        return mMachineController.getCurrentCoordinateObservable().flatMap(vector -> {
            // Assemble xyz temp vector, we assume that every move should locate single position(xyz coordinate),
            // not X1 plus X2 with two location.
            Vector tempVector = new Vector();
            if (direction == Direction.LEFT || direction == Direction.RIGHT) {
                tempVector.setX(vector.getX());
            } else {
                tempVector.setX2(vector.getX2());
            }
            tempVector.setY(vector.getY());
            tempVector.setZ(vector.getZ());
            // check step change.
            switch (direction) {
                case FORWARD:
                    tempVector.setY(vector.getY() - stepWidth);
                    break;
                case BACKWARD:
                    tempVector.setY(vector.getY() + stepWidth);
                    break;
                case LEFT:
                    tempVector.setX(vector.getX() - stepWidth);
                    break;
                case RIGHT:
                    tempVector.setX(vector.getX() + stepWidth);
                    break;
                case UP:
                    tempVector.setZ(vector.getZ() + stepWidth);
                    break;
                case DOWN:
                    tempVector.setZ(vector.getZ() - stepWidth);
                    break;
                case B_CLOCKWISE:
                    tempVector.setB(vector.getB() - stepWidth);
                    break;
                case B_COUNTERCLOCKWISE:
                    tempVector.setB(vector.getB() + stepWidth);
                    break;
                case X2_LEFT:
                    tempVector.setX2(vector.getX2() - stepWidth);
                    break;
                case X2_RIGHT:
                    tempVector.setX2(vector.getX2() + stepWidth);
                    break;
                default:
                    break;
            }

            return mMachineController.gotoAbsolutePosition(tempVector, feedrate);
        });
    }

    public enum Direction {
        IDLE, DISABLE, FORWARD, BACKWARD, LEFT, RIGHT, UP, DOWN, B_CLOCKWISE, B_COUNTERCLOCKWISE, X2_LEFT, X2_RIGHT;

        public static boolean isRotary(Direction direction) {
            return direction == B_CLOCKWISE || direction == B_COUNTERCLOCKWISE;
        }
    }

    private static class XYZMoveControllerHolder {
        private static final MoveController INSTANCE = new MoveController();
    }
}

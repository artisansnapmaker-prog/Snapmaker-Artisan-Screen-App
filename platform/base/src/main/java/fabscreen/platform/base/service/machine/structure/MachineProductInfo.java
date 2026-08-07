package fabscreen.platform.base.service.machine.structure;

import java.io.IOException;

import fabscreen.platform.base.service.IMachine;
import fabscreen.platform.base.service.machine.IStructure;
import fabscreen.platform.base.service.machine.structure.prop.StringProp;
import fabscreen.platform.base.service.machine.structure.prop.UInt32Prop;
import fabscreen.platform.base.service.machine.structure.prop.UInt8Prop;
import okio.Buffer;

public class MachineProductInfo implements IStructure {
    private int mMachineSeries = IMachine.MachineSeries.UNDEFINED;

    private UInt8Prop modelProp = new UInt8Prop();
    private UInt8Prop controllerHWVersionProp = new UInt8Prop();
    private UInt32Prop burnSerialNumberProp = new UInt32Prop();
    private StringProp controllerFWVersionProp = new StringProp();
    private StringProp productSerialNumberProp = new StringProp();

    @Override
    public byte[] toByteArray() {
        Buffer buffer = new Buffer();
        buffer.write(modelProp.toByteArray());
        buffer.write(controllerHWVersionProp.toByteArray());
        buffer.write(burnSerialNumberProp.toByteArray());
        buffer.write(controllerFWVersionProp.toByteArray());
        buffer.write(productSerialNumberProp.toByteArray());
        return buffer.readByteArray();
    }

    @Override
    public Buffer readBuffer(Buffer buffer) throws IOException {
        try {
            Buffer clone = buffer.clone();
            controllerHWVersionProp.readBuffer(clone);
            burnSerialNumberProp.readBuffer(clone);
            controllerFWVersionProp.readBuffer(clone);
            if (clone.size() == 0) {
                modelProp = new UInt8Prop(3);
                return clone;
            } else {
                throw new IllegalStateException("Is true");
            }
        } catch (Exception e) {
            modelProp.readBuffer(buffer);
            controllerHWVersionProp.readBuffer(buffer);
            burnSerialNumberProp.readBuffer(buffer);
            controllerFWVersionProp.readBuffer(buffer);
            productSerialNumberProp.readBuffer(buffer);
        }
        return buffer;
    }

    @Override
    public String toString() {
        return "MachineProductInfo{" +
                "\nmodelProp=" + modelProp +
                ",\n controllerHWVersionProp=" + controllerHWVersionProp +
                ",\n snProp=" + burnSerialNumberProp +
                ",\n controllerFWVersionProp=" + controllerFWVersionProp +
                ",\n controllerSnProp=" + productSerialNumberProp +
                '}';
    }

    public int getBrand() {
        int brand = IMachine.MachineSeries.UNDEFINED;
        switch (modelProp.getValue()) {
            case 0:
            case 1:
            case 2:
            case 3:
                return IMachine.MachineSeries.A;
            case 4:
                return IMachine.MachineSeries.J;
        }
        return brand;
    }

    public void setSeries(int brand) {
        mMachineSeries = brand;
    }

    public void setModel(int model) {
        int fullModel = -1;
        switch (mMachineSeries) {
            case IMachine.MachineSeries.A:
                switch (model) {
                    case IMachine.MachineModel.A150:
                        fullModel = 0;
                        break;
                    case IMachine.MachineModel.A250:
                        fullModel = 1;
                        break;
                    case IMachine.MachineModel.A350:
                        fullModel = 2;
                        break;
                    case IMachine.MachineModel.A400:
                        fullModel = 3;
                }
                break;

            case IMachine.MachineSeries.J:
                fullModel = 4;
                break;
        }

        modelProp.setValue(fullModel);
    }

    public int getModel() {
        switch (modelProp.getValue()) {
            case 0:
                return IMachine.MachineModel.A150;
            case 1:
                return IMachine.MachineModel.A250;
            case 2:
                return IMachine.MachineModel.A350;
            case 3:
                return IMachine.MachineModel.A400;
            case 4:
                return IMachine.MachineModel.J1;
            default:
                return IMachine.MachineModel.UNDEFINED;
        }
    }

    public int getProductId() {
        return modelProp.getValue();
    }

    public String getControllerFWVersion() {
        return controllerFWVersionProp.getValue();
    }

    public void setControllerFWVersion(String controllerVersion) {
        controllerFWVersionProp.setValue(controllerVersion);
    }

    public String getProductSerialNumber() {
        return productSerialNumberProp.getValue();
    }

    public void setProductSerialNumber(String productSerialNumber) {
        productSerialNumberProp.setValue(productSerialNumber);
    }

    public String getBurnSerialNumber() {
        return burnSerialNumberProp.getValue().toString();
    }

    public void setBurnSerialNumber(String sn) {
        burnSerialNumberProp.setValue(Long.valueOf(sn));
    }
}

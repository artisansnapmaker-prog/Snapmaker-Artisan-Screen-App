package fabscreen.platform.base.service.machine.structure;

import java.io.IOException;

import fabscreen.platform.base.service.machine.IStructure;
import fabscreen.platform.base.service.machine.structure.prop.FloatProp;
import fabscreen.platform.base.service.machine.structure.prop.UInt8Prop;
import okio.Buffer;

public class LaserIndicatorPowerStructure implements IStructure {
    private FloatProp laserPowerProp = new FloatProp();
    public LaserIndicatorPowerStructure() {

    }

    public LaserIndicatorPowerStructure(int key, float laserPower) {
        this.laserPowerProp.setValue(laserPower);
    }

    public float getLaserIndicatorPower() {
        return laserPowerProp.getValue();
    }

    @Override
    public byte[] toByteArray() {
        Buffer buffer = new Buffer();
        buffer.write(laserPowerProp.toByteArray());
        return buffer.readByteArray();
    }

    @Override
    public Buffer readBuffer(Buffer buffer) throws IOException {
        laserPowerProp.readBufferToValue(buffer);
        return buffer;
    }

    @Override
    public String toString() {
        return "LaserIndicatorPowerStructure{" +
                ", laserPowerProp=" + laserPowerProp +
                '}';
    }
}

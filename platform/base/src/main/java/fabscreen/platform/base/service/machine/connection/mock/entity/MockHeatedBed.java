package fabscreen.platform.base.service.machine.connection.mock.entity;

import java.util.ArrayList;
import java.util.List;

import fabscreen.platform.base.service.machine.entity.module.HeatedBed;

public class MockHeatedBed extends MockModule {
    private int mKey;
    private List<MockZone> mMockZonList;
    private int mWorkMode;

    public MockHeatedBed(int index, List<MockZone> mockZoneList) {
        mKey = index;
        mMockZonList = mockZoneList;
        mWorkMode = 0x01;
    }

    public int getIndex() {
        return mKey;
    }

    public void setIndex(int index) {
        this.mKey = index;
    }

    public List<MockZone> getMockZonList() {
        return mMockZonList;
    }

    public int getWorkMode() {
        return mWorkMode;
    }

    public void setWorkMode(int mode) {
        this.mWorkMode = mode;
    }

    public void setMockZonList(List<MockZone> mockZonList) {
        this.mMockZonList = mockZonList;
    }

    @Override
    public String toString() {
        return "MockHeatedBed{" +
                "key=" + key +
                ", moduleId=" + moduleId +
                ", moduleIndex=" + moduleIndex +
                ", moduleState=" + moduleState +
                '}';
    }

    public HeatedBed.HeatedBedStatus getBedInfo() {
        HeatedBed.HeatedBedStatus heatedBedStatus = new HeatedBed.HeatedBedStatus();
        heatedBedStatus.setKey(key);
        ArrayList<HeatedBed.ZoneInfo> zoneInfoArrayList = new ArrayList<>();
        for (int i = 0; i < getMockZonList().size(); i++) {
            zoneInfoArrayList.add(getMockZonList().get(i).getZoneInfo());
        }
        heatedBedStatus.setZoneList(zoneInfoArrayList);
        heatedBedStatus.setWorkMode(getWorkMode());

        return heatedBedStatus;
    }
}

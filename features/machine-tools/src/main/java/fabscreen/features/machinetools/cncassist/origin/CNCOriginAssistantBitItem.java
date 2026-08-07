package fabscreen.features.machinetools.cncassist.origin;

public class CNCOriginAssistantBitItem {
    public final static int TYPE_ITEM_BIT_DEFAULT = 0;
    public final static int TYPE_ITEM_BIT_CUSTOM = 1;
    private int itemType;
    private String bitName;
    private int bitResId;
    private float bitDiameter;
    private float bitLength;
    private String bitTip;

    public CNCOriginAssistantBitItem(int type, String name, int resid, float diameter, float length, String bitTip) {
        itemType = type;
        bitName = name;
        bitResId = resid;
        bitDiameter = diameter;
        bitLength = length;
        this.bitTip = bitTip;
    }

    public int getItemType() {
        return itemType;
    }

    public void setItemType(int type) {
        itemType = type;
    }

    public boolean isDefaultBit() {
        return itemType == TYPE_ITEM_BIT_DEFAULT;
    }

    public String getBitName() {
        return bitName;
    }

    public void setBitName(String bitName) {
        this.bitName = bitName;
    }

    public int getBitResId() {
        return bitResId;
    }

    public void setBitResId(int bitResId) {
        this.bitResId = bitResId;
    }

    public float getBitDiameter() {
        return bitDiameter;
    }

    public void setBitDiameter(float bitDiameter) {
        this.bitDiameter = bitDiameter;
    }

    public float getBitLength() {
        return bitLength;
    }

    public void setBitLength(float bitLength) {
        this.bitLength = bitLength;
    }


    public String getBitTip() {
        return bitTip;
    }

    public void setBitTip(String bitTip) {
        this.bitTip = bitTip;
    }
}

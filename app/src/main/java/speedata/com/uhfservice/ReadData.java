package speedata.com.uhfservice;

/**
 * Created by 张明_ on 2017/5/15.
 */

public class ReadData {
    private String EPC;
    private int RSSI;
    private int count;

    public ReadData(String EPC, int RSSI, int count) {
        this.EPC = EPC;
        this.RSSI = RSSI;
        this.count = count;
    }

    public String getEPC() {
        return EPC;
    }

    public void setEPC(String EPC) {
        this.EPC = EPC;
    }

    public int getRSSI() {
        return RSSI;
    }

    public void setRSSI(int RSSI) {
        this.RSSI = RSSI;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

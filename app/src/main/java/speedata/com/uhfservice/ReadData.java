package speedata.com.uhfservice;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by 张明_ on 2017/5/15.
 */

public class ReadData implements Parcelable{
    private String EPC;
    private int RSSI;
    private int count;

    public ReadData(String EPC, int RSSI, int count) {
        this.EPC = EPC;
        this.RSSI = RSSI;
        this.count = count;
    }

    protected ReadData(Parcel in) {
        EPC = in.readString();
        RSSI = in.readInt();
        count = in.readInt();
    }

    public static final Creator<ReadData> CREATOR = new Creator<ReadData>() {
        @Override
        public ReadData createFromParcel(Parcel in) {
            return new ReadData(in);
        }

        @Override
        public ReadData[] newArray(int size) {
            return new ReadData[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(EPC);
        dest.writeInt(RSSI);
        dest.writeInt(count);
    }
}

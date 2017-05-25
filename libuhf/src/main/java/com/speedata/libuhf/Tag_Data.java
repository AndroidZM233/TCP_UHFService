package com.speedata.libuhf;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by 张明_ on 2016/12/16.
 */

public class Tag_Data implements Parcelable{
    public Tag_Data(byte[] n_tid, byte[] n_epc,String rssi) {
        tid = n_tid;
        epc = n_epc;
        this.rssi=rssi;
    }

    public byte[] tid;
    public byte[] epc;
    public String rssi;


    protected Tag_Data(Parcel in) {
        tid = in.createByteArray();
        epc = in.createByteArray();
        rssi = in.readString();
    }

    public static final Creator<Tag_Data> CREATOR = new Creator<Tag_Data>() {
        @Override
        public Tag_Data createFromParcel(Parcel in) {
            return new Tag_Data(in);
        }

        @Override
        public Tag_Data[] newArray(int size) {
            return new Tag_Data[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(tid);
        dest.writeByteArray(epc);
        dest.writeString(rssi);
    }
}

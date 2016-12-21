/*
*
* Copyright (c) PLUX S.A., All Rights Reserved.
* (www.plux.info)
*
* This software is the proprietary information of PLUX S.A.
* Use is subject to license terms.
*
*/

package info.plux.pluxapi.bitalino;

import android.os.Parcel;
import android.os.Parcelable;

public class BITalinoDescription implements Parcelable {
    private boolean isBITalino2;
    private float fwVersion;

    public BITalinoDescription(boolean isBITalino2, float fwVersion) {
        this.isBITalino2 = isBITalino2;
        this.fwVersion = fwVersion;
    }

    public boolean isBITalino2() {
        return isBITalino2;
    }

    public void setBITalino2(boolean BITalino2) {
        isBITalino2 = BITalino2;
    }

    public float getFwVersion() {
        return fwVersion;
    }

    public void setFwVersion(float fwVersion) {
        this.fwVersion = fwVersion;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.isBITalino2 ? (byte) 1 : (byte) 0);
        dest.writeFloat(this.fwVersion);
    }

    protected BITalinoDescription(Parcel in) {
        this.isBITalino2 = in.readByte() != 0;
        this.fwVersion = in.readFloat();
    }

    public static final Creator<BITalinoDescription> CREATOR = new Creator<BITalinoDescription>() {
        @Override
        public BITalinoDescription createFromParcel(Parcel source) {
            return new BITalinoDescription(source);
        }

        @Override
        public BITalinoDescription[] newArray(int size) {
            return new BITalinoDescription[size];
        }
    };
}

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

import java.util.Arrays;

public class BITalinoState implements Parcelable {
    private String identifier;
    private int[] analog = new int[6];
    private int analogOutput;
    private int battery;
    private int batThreshold;
    private int[] digital = new int[4];

    public BITalinoState(String identifier, int[] analog, int analogOutput, int battery, int batThreshold, int[] digital) {
        this.identifier = identifier;
        this.analog = analog;
        this.analogOutput = analogOutput;
        this.battery = battery;
        this.batThreshold = batThreshold;
        this.digital = digital;
    }

    public BITalinoState(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int getAnalog(final int pos) {
        return analog[pos];
    }

    public void setAnalog(final int pos, final int value) throws IndexOutOfBoundsException {
        this.analog[pos] = value;
    }

    public int getAnalogOutput() {
        return analogOutput;
    }

    public void setAnalogOutput(int analogOutput) {
        this.analogOutput = analogOutput;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public int getBatThreshold() {
        return batThreshold;
    }

    public void setBatThreshold(int batThreshold) {
        this.batThreshold = batThreshold;
    }

    public int getDigital(final int pos) {
        return digital[pos];
    }

    public void setDigital(final int pos, final int value) throws IndexOutOfBoundsException {
        this.digital[pos] = value;
    }

    public String toString(){
        return identifier + ": Analog: " + Arrays.toString(analog) + "; Battery: " + battery + "; BatThreshold: " + batThreshold + "; Digital: " + Arrays.toString(digital);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.identifier);
        dest.writeIntArray(this.analog);
        dest.writeInt(this.analogOutput);
        dest.writeInt(this.battery);
        dest.writeInt(this.batThreshold);
        dest.writeIntArray(this.digital);
    }

    protected BITalinoState(Parcel in) {
        this.identifier = in.readString();
        this.analog = in.createIntArray();
        this.analogOutput = in.readInt();
        this.battery = in.readInt();
        this.batThreshold = in.readInt();
        this.digital = in.createIntArray();
    }

    public static final Creator<BITalinoState> CREATOR = new Creator<BITalinoState>() {
        @Override
        public BITalinoState createFromParcel(Parcel source) {
            return new BITalinoState(source);
        }

        @Override
        public BITalinoState[] newArray(int size) {
            return new BITalinoState[size];
        }
    };
}
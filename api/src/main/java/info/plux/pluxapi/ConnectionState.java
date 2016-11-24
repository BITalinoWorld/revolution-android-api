/*
*
* Copyright (c) PLUX S.A., All Rights Reserved.
* (www.plux.info)
*
* This software is the proprietary information of PLUX S.A.
* Use is subject to license terms.
*
*/
package info.plux.pluxapi;

import android.os.Parcel;
import android.os.Parcelable;

public class ConnectionState implements Parcelable {
    public String identifier;
    public int connectionState;

    public ConnectionState (String identifier, int connectionState){
        this.identifier = identifier;
        this.connectionState = connectionState;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.identifier);
        dest.writeInt(this.connectionState);
    }

    public ConnectionState(Parcel in) {
        this.identifier = in.readString();
        this.connectionState = in.readInt();
    }

    public static final Creator<ConnectionState> CREATOR
            = new Creator<ConnectionState>() {
        public ConnectionState createFromParcel(Parcel in) {
            return new ConnectionState(in);
        }

        public ConnectionState[] newArray(int size) {
            return new ConnectionState[size];
        }
    };
}
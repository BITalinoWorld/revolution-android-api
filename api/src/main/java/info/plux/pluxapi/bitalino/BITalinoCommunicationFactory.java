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

import android.content.Context;
import info.plux.pluxapi.Communication;
import info.plux.pluxapi.bitalino.ble.BLECommunication;
import info.plux.pluxapi.bitalino.bth.BTHCommunication;
import info.plux.pluxapi.bitalino.bth.OnBITalinoDataAvailable;

public class BITalinoCommunicationFactory {
    private final String TAG = this.getClass().getSimpleName();

    public BITalinoCommunication getCommunication(Communication type, Context activityContext){
        BITalinoCommunication communication = null;
        switch (type){
            case BLE:
                //BLE
                communication = new BLECommunication(activityContext);
                break;
            case BTH:
                //BTH
                communication =  new BTHCommunication(activityContext, null);
                break;
        }
        return communication;
    }

    public BITalinoCommunication getCommunication(Communication type, Context activityContext, OnBITalinoDataAvailable callback){
        BITalinoCommunication communication = null;
        switch (type){
            case BLE:
                //BLE
                communication = new BLECommunication(activityContext);
                break;
            case BTH:
                //BTH
                communication =  new BTHCommunication(activityContext, callback);
                break;
        }

        return communication;
    }
}
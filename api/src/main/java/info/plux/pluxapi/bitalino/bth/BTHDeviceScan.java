/*
*
* Copyright (c) PLUX S.A., All Rights Reserved.
* (www.plux.info)
*
* This software is the proprietary information of PLUX S.A.
* Use is subject to license terms.
*
*/
package info.plux.pluxapi.bitalino.bth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import info.plux.pluxapi.Constants;

import java.util.Set;

public class BTHDeviceScan {
    private final String TAG = this.getClass().getSimpleName();

    private Context activityContext;

    private BluetoothAdapter mBtAdapter;

    public BTHDeviceScan (final Context activityContext) {
        this.activityContext = activityContext;

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (device.getBondState() != BluetoothDevice.BOND_BONDED && testPLUXDevice(device.getName())) {
                        Intent scanIntent = new Intent(Constants.ACTION_MESSAGE_SCAN);
                        scanIntent.putExtra("device", device);
                        activityContext.sendBroadcast(scanIntent);
                    }
                }
            }
        };
        activityContext.registerReceiver(mReceiver, filter);
    }


    public void getPairedDevices(){

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if(testPLUXDevice(device.getName())) {
                    Intent scanIntent = new Intent(Constants.ACTION_MESSAGE_SCAN);
                    scanIntent.putExtra("device", device);
                    activityContext.sendBroadcast(scanIntent);
                }
            }
        }
    }


    protected void stopScan() {
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
    }

    public void doDiscovery() {

        // If we're already discovering, STOP it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    private boolean testPLUXDevice (String name) {
        if (name != null){
            if (name.toLowerCase().contains("bitalino")) {
                return true;
            }
        }
        return false;
    }


}
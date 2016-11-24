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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.Set;

public class BTHDeviceScan {
    private final String TAG = this.getClass().getSimpleName();

    private Context activityContext;

    private BluetoothAdapter mBtAdapter;
    private BroadcastReceiver mReceiver;

    public BTHDeviceScan(final Context activityContext) {
        this.activityContext = activityContext;

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    Log.d(TAG, "device found: " + device.getName() + " - " + device.getAddress());


                    if (testPLUXDevice(device.getName(), device.getAddress())) {
                        Intent scanIntent = new Intent(Constants.ACTION_MESSAGE_SCAN);
                        scanIntent.putExtra(Constants.EXTRA_DEVICE_SCAN, device);
                        activityContext.sendBroadcast(scanIntent);
                    }
                }
            }
        };
        activityContext.registerReceiver(mReceiver, filter);
    }

    public void closeScanReceiver() {
        activityContext.unregisterReceiver(mReceiver);
    }


    public void getPairedDevices() {

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (testPLUXDevice(device.getName(), device.getAddress())) {
                    Intent scanIntent = new Intent(Constants.ACTION_MESSAGE_SCAN);
                    scanIntent.putExtra(Constants.EXTRA_DEVICE_SCAN, device);
                    activityContext.sendBroadcast(scanIntent);

                    Log.d(TAG, "device found: " + device.getName());
                }
            }
        }
    }


    public void stopScan() {
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

    public static boolean testPLUXDevice(String name, String address) {
        if (name != null) {
            if (name.toLowerCase().contains("bitalino")) {
                return true;
            }
        }
        else{
            if (address.contains("20:15:12:")){
                return true;
            }
        }
        return false;
    }


}
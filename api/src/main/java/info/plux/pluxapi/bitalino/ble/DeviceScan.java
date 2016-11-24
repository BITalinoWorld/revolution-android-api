/*
*
* Copyright (c) PLUX S.A., All Rights Reserved.
* (www.plux.info)
*
* This software is the proprietary information of PLUX S.A.
* Use is subject to license terms.
*
*/

package info.plux.pluxapi.bitalino.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import info.plux.pluxapi.Constants;

import java.util.ArrayList;

public class DeviceScan {
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private Context activityContext;

    private static final int REQUEST_ENABLE_BT = 1;

    private ArrayList<BluetoothDevice> devices= new ArrayList<BluetoothDevice>();

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 5000;

    public DeviceScan(Context activityContext){
        mHandler = new Handler();

        this.activityContext = activityContext;

        final BluetoothManager bluetoothManager = (BluetoothManager) activityContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    public void scanLeDevice(final boolean enable) {
        mLeDevices = new ArrayList<BluetoothDevice>();
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    //invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        //invalidateOptionsMenu();
    }


    // Device scan callback.
    private ArrayList<BluetoothDevice> mLeDevices;
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            if(!mLeDevices.contains(device) && testPLUXDevice(device.getName())){
                                mLeDevices.add(device);
                                devices.add(device);
                                Intent intent = new Intent(Constants.ACTION_MESSAGE_SCAN);
                                intent.putExtra("device", device);
                                activityContext.sendBroadcast(intent);
                            }
                        }
                    });
                }
            };

    public boolean isBTEnabled() {
        if (mBluetoothAdapter.isEnabled()) {
            return true;
        }
        return false;
    }

    public void enableBT() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        //activityContext.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
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

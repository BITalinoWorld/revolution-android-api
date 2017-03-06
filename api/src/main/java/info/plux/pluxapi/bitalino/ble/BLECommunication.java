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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import info.plux.pluxapi.CommandArguments;
import info.plux.pluxapi.bitalino.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static info.plux.pluxapi.Constants.*;

public class BLECommunication extends BITalinoCommunication {
    private final String TAG = this.getClass().getSimpleName();

    //PLUX UUID
    private final static UUID UUID_COMMANDS = UUID.fromString(PluxGattAttributes.COMMANDS);
    private final static UUID UUID_FRAMES = UUID.fromString(PluxGattAttributes.FRAMES);
    //Log Types
    private final int LOG_CONNECTED          = 0;
    private final int LOG_DISCONNECTED       = 1;
    private final int LOG_READY              = 2;
    private final int LOG_DATA_STREAM        = 3;
    private final int LOG_SEARCH_SERVICES    = 4;
    private final int LOG_ENABLE_COMMANDS    = 5;
    private final int LOG_ENABLE_FRAMES      = 6;
    private final int LOG_START              = 7;
    private final int LOG_STOP               = 8;
    //Log alarm String constants
    private final String LOG_ALARM                   = "info.plux.api.bioplux.ble.BLECommunication.LOG_ALARM";
    private final String LOG_ALARM_EXTRA_ID          = "info.plux.api.bioplux.ble.BLECommunication.LOG_ALARM_EXTRA_ID";
    private final String LOG_ALARM_EXTRA_ACTION      = "info.plux.api.bioplux.ble.BLECommunication.LOG_ALARM_EXTRA_ACTION";
    private final String LOG_ALARM_EXTRA_IDENTIFIER  = "info.plux.api.bioplux.ble.BLECommunication.LOG_ALARM_EXTRA_IDENTIFIER";
    //Log Time Constants
    private final int NOW = 0;
    private final int WAIT_TIME_1SECONDS = 1000;
    private final int WAIT_TIME_2SECONDS = 2*WAIT_TIME_1SECONDS;
    private final int WAIT_TIME_3SECONDS = 3*WAIT_TIME_1SECONDS;
    private final int WAIT_TIME_5SECONDS = 5*WAIT_TIME_1SECONDS;
    private final String commandsAction  = "COMMANDS - Error on notification enable";
    private final Context activityContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private String mBluetoothDeviceAddress;
    private int nChannels = 8;
    private int previousSeq = 0;
    private States currentState = States.NO_CONNECTION;
    //Log flags
    private boolean isLogAlarmRegistered = false;
    private boolean isReady = false;
    private boolean isConnected = false;
    private boolean isDataStreaming = true;
    private boolean inAcquisition = false;
    private boolean isCommandsEnabled = false;
    private boolean isFramesEnabled = false;
    private boolean isServiceDiscovered = false;
    private boolean isDataStreamingAlarmSet = false;
    private boolean disconnectFired = false;
    //Time Variables
    private long lastSampleTimeStamp = -1;
    private long initialTimeStamp = 0;
    private long initialReconnectTimeStamp;
    //Characteristics
    private BluetoothGattCharacteristic characteristicCommands;
    private int freqDivisor;
    List<BluetoothGattCharacteristic> pluxCharacteristics;

    private boolean isWaitingForState = false;
    private boolean isWaitingForVersion = false;

    private boolean isBITalino2 = false;
    private boolean isStateCorrect = false;
    private int stateTotalBytes = 16;


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                Log.i(TAG, mBluetoothDeviceAddress + " - Connected to GATT server.");

                // Attempts to discover services after successful connection.

                Log.i(TAG, mBluetoothDeviceAddress + " - Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                setLogAlarm(LOG_SEARCH_SERVICES, WAIT_TIME_1SECONDS);
                setLogAlarm(LOG_CONNECTED, NOW);

                if(isDataStreamingAlarmSet){
                    setLogAlarm(LOG_DATA_STREAM, WAIT_TIME_3SECONDS);
                }

                isConnected = true;
                setLogAlarm(LOG_READY, WAIT_TIME_2SECONDS);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                setState(States.DISCONNECTED);

                Log.i(TAG, mBluetoothDeviceAddress + " - Disconnected from GATT server.");

                if(disconnectFired){
                    close();
                }
                else{
                    reconnect();
                }

                isConnected = false;
                isReady = false;
                setLogAlarm(LOG_DISCONNECTED, NOW);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isServiceDiscovered = true;

                broadcastUpdate();

                setupCharacteristicNotifications(getSupportedGattServices());
            } else {
                Log.w(TAG, mBluetoothDeviceAddress + " - onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(characteristic);
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                Log.i(TAG, mBluetoothDeviceAddress + " - characteristic write success");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, mBluetoothDeviceAddress + " - onDescriptorWrite " +  " - " + descriptor.getUuid() + " status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if ((descriptor.getCharacteristic().getUuid()).equals(UUID.fromString(PluxGattAttributes.COMMANDS))) {
                    //enable indications - Frames Characteristic
                    setCharacteristicNotification(pluxCharacteristics.get(1));

                    Log.i(TAG, mBluetoothDeviceAddress + " - Commands " + status);
                    isCommandsEnabled = true;

                    if(initialReconnectTimeStamp != 0){
                        long finalReconnectTimeStamp = Calendar.getInstance().getTimeInMillis();
                        long deltaReconnect = finalReconnectTimeStamp - initialReconnectTimeStamp;
                        Log.d(TAG, "[" + mBluetoothDeviceAddress + "] " + "Reconnect Time: " + Math.round(deltaReconnect/1000) + " s");
                        initialReconnectTimeStamp = 0;
                    }

                    isReady = true;

                } else if ((descriptor.getCharacteristic().getUuid()).equals(UUID.fromString(PluxGattAttributes.FRAMES))) {
                    Log.i(TAG, mBluetoothDeviceAddress + " - FRAMES " + status);

                    setState(States.CONNECTED);

                    //get version
                    try {
                        getVersion();
                    } catch (BITalinoException e) {
                        e.printStackTrace();
                    }

                    if(initialReconnectTimeStamp != 0){
                        long finalReconnectTimeStamp = Calendar.getInstance().getTimeInMillis();
                        long deltaReconnect = finalReconnectTimeStamp - initialReconnectTimeStamp;
                        Log.d(TAG, "[" + mBluetoothDeviceAddress + "] " + "Reconnect Time: " + Math.round(deltaReconnect/1000) + " s");
                        initialReconnectTimeStamp = 0;
                    }

                    isReady = true;
                    isFramesEnabled = true;
                }
            } else {
                switch (status){
                    case 132:
                        Log.e(TAG, "descriptor write error " + status + " -> GATT_BUSY");
                        break;
                    case 133:
                        Log.e(TAG, "descriptor write error " + status + " -> GATT_ERROR");
                        break;
                    default:
                        Log.e(TAG, "descriptor write error " + status);
                        break;
                }
                //reconnect();

            }
        }
    };
    private final BroadcastReceiver LogAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = intent.getIntExtra(LOG_ALARM_EXTRA_ID, 0);
            String identifier = intent.getStringExtra(LOG_ALARM_EXTRA_IDENTIFIER);
            String action = "";


            if (identifier != null && identifier.equals(mBluetoothDeviceAddress)) {

                if (intent.hasExtra(LOG_ALARM_EXTRA_ACTION)) {
                    action = intent.getStringExtra(LOG_ALARM_EXTRA_ACTION);
                }

                Intent alarmIntent = new Intent(LOG_ALARM);
                alarmIntent.putExtra(LOG_ALARM_EXTRA_ID, id);
                alarmIntent.putExtra(LOG_ALARM_EXTRA_IDENTIFIER, identifier);

                if (!action.equals("")) {
                    alarmIntent.putExtra(LOG_ALARM_EXTRA_ACTION, action);
                }
                PendingIntent pendingIntent = PendingIntent.getBroadcast(activityContext, id, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
                AlarmManager alarmManager = (AlarmManager) activityContext.getSystemService(Context.ALARM_SERVICE);

                String payLoad = "";
                switch (id) {
                    case LOG_CONNECTED:
                        payLoad = "[" + mBluetoothDeviceAddress + "] " + "Connected";
                        break;
                    case LOG_DISCONNECTED:
                        payLoad = "[" + mBluetoothDeviceAddress + "] " + "Disconnected";
                        break;
                    case LOG_READY:
                        if (!isReady) {
                            payLoad = "[" + mBluetoothDeviceAddress + "] " + "Device Not Ready";
                            reconnect();
                        } else {
                            payLoad = "[" + mBluetoothDeviceAddress + "] " + "Device Ready";
                            alarmManager.cancel(pendingIntent);
                        }
                        break;
                    case LOG_DATA_STREAM:

                        long deltaTime;
                        if(lastSampleTimeStamp == -1){
                            deltaTime = WAIT_TIME_2SECONDS;
                        }
                        else {
                            deltaTime = Calendar.getInstance().getTimeInMillis() - lastSampleTimeStamp;
                        }

                        if (deltaTime >= WAIT_TIME_2SECONDS) {
                            isDataStreaming = false;
                            initialReconnectTimeStamp = Calendar.getInstance().getTimeInMillis();
                        }

                        if (!isDataStreaming && isConnected) {
                            payLoad = "[" + mBluetoothDeviceAddress + "] " + "No Data Streaming";

                            reconnect();

                            alarmManager.cancel(pendingIntent);
                        } else if (isDataStreaming) {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + WAIT_TIME_1SECONDS, pendingIntent);
                        } else {
                            alarmManager.cancel(pendingIntent);
                        }

                        break;
                    case LOG_SEARCH_SERVICES:
                        if (!isServiceDiscovered) {
                            payLoad = "[" + mBluetoothDeviceAddress + "] " + "Services Searched with success";
                        }

                        break;
                    case LOG_ENABLE_COMMANDS:
                        if (!isCommandsEnabled) {
                            payLoad = "[" + mBluetoothDeviceAddress + "] " + action;
                        }
                        break;
                    case LOG_ENABLE_FRAMES:
                        if (!isFramesEnabled) {
                            payLoad = "[" + mBluetoothDeviceAddress + "] " + action;
                        }
                        break;
                    case LOG_START:
                        payLoad = "[" + mBluetoothDeviceAddress + "] " + "Acquisition Started";
                        break;
                    case LOG_STOP:
                        payLoad = "[" + mBluetoothDeviceAddress + "] " + "Acquisition Stopped";
                        break;
                }

                if (!payLoad.equals("")) {
                    final Intent logIntent = new Intent(ACTION_LOG_AVAILABLE);
                    logIntent.putExtra(EXTRA_LOG, payLoad);
                    activityContext.sendBroadcast(logIntent);
                }
            }
        }
    };

    public BLECommunication(Context activityContext) {
        super(activityContext);
        this.activityContext = activityContext;

        init();
    }



    @Override
    public void init() {

        //mCommandsInterpretation = new CommandsInterpretation();
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) activityContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
            }
        }

        activityContext.registerReceiver(LogAlarmReceiver, new IntentFilter(LOG_ALARM));
        isLogAlarmRegistered = true;

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        }

    }

    @Override
    public void closeReceivers() {
        if(isLogAlarmRegistered){
            activityContext.unregisterReceiver(LogAlarmReceiver);
            isLogAlarmRegistered = false;
        }
    }

    /**
     * Return the current connection state.
     */
    public synchronized States getState() {
        return currentState;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(States state) {
        currentState = state;

        // Give the new state to the Handler so the UI Activity can update
        Intent intent = new Intent(ACTION_STATE_CHANGED);
        intent.putExtra(IDENTIFIER, mBluetoothDeviceAddress);
        intent.putExtra(EXTRA_STATE_CHANGED, state.getId());
        activityContext.sendBroadcast(intent);
    }

    private void broadcastUpdate() {
        final Intent intent = new Intent(ACTION_GATT_SERVICES_DISCOVERED);
        intent.putExtra(IDENTIFIER, mBluetoothDeviceAddress);
        activityContext.sendBroadcast(intent);
    }

    private int k = 0;
    private byte[] buffer = new byte[totalBytes];
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(ACTION_DATA_AVAILABLE);

        if (UUID_FRAMES.equals(characteristic.getUuid())) {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if(currentState == States.ACQUISITION_OK) {
                    //parse frames
                    for (byte aData : data) {
                        buffer[k] = aData;
                        k++;
                        if (k == totalBytes) {
                            BITalinoFrame bitatinoFrame = null;
                            try {
                                bitatinoFrame = BITalinoFrameDecoder.decode(mBluetoothDeviceAddress, buffer, analogChannels, totalBytes);

                                intent.putExtra(EXTRA_DATA, bitatinoFrame);
                                activityContext.sendBroadcast(intent);

                            } catch (BITalinoException | IOException e) {
                                e.printStackTrace();
                            }
                            k = 0;

                            buffer = new byte[totalBytes];

                            if (bitatinoFrame.getSequence() - previousSeq != 1 && previousSeq - bitatinoFrame.getSequence() != 0 && Math.abs(previousSeq - bitatinoFrame.getSequence()) != 15) {
                                int nSeq = bitatinoFrame.getSequence() - previousSeq;
                                Log.e(TAG, "[" + mBluetoothDeviceAddress + "] " + "Seq: " + nSeq);
                            }

                            lastSampleTimeStamp = Calendar.getInstance().getTimeInMillis();
                            isDataStreaming = true;

                            previousSeq = bitatinoFrame.getSequence();
                        }
                    }
                }
                else {

                    if(isWaitingForState){
                        BITalinoState bitalinoState = null;

                        try {
                            bitalinoState = BITalinoStateDecoder.decode(mBluetoothDeviceAddress, data, isStateCorrect);
                        } catch (IOException | BITalinoException e) {
                            e.printStackTrace();
                        }

                        Log.d(TAG, "BITalino state: " + bitalinoState.toString());

                        isWaitingForState = false;

                        Intent stateIntent = new Intent(ACTION_COMMAND_REPLY);
                        stateIntent.putExtra(IDENTIFIER, mBluetoothDeviceAddress);
                        stateIntent.putExtra(EXTRA_COMMAND_REPLY, bitalinoState);
                        activityContext.sendBroadcast(stateIntent);
                    }
                    else if(isWaitingForVersion){
                        String[] versionArray = new String(data, StandardCharsets.UTF_8).split("_v");

                        if (Float.parseFloat(versionArray[1]) >= 5) {
                            isBITalino2 = true;
                            Log.d(TAG, "isBITalino2: " + isBITalino2 + " -> " + Float.parseFloat(versionArray[1]));
                            if(Float.parseFloat(versionArray[1]) >= 5.2f){
                                isStateCorrect = true;
                                stateTotalBytes = 17;
                            }
                        }

                        isWaitingForVersion = false;

                        Intent descriptionIntent = new Intent(ACTION_COMMAND_REPLY);
                        descriptionIntent.putExtra(IDENTIFIER, mBluetoothDeviceAddress);
                        descriptionIntent.putExtra(EXTRA_COMMAND_REPLY, new BITalinoDescription(isBITalino2, Float.parseFloat(versionArray[1])));
                        activityContext.sendBroadcast(descriptionIntent);
                    }

                }
            }
        } else if (UUID_COMMANDS.equals(characteristic.getUuid())) {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                Log.d(TAG, "UUID_COMMANDS -> data.length: " + data.length);
                if(currentState == States.ACQUISITION_OK) {
                    Log.d(TAG, "data.length: " + data.length);
                    //parse frames
                    for (byte aData : data) {
                        buffer[k] = aData;
                        k++;
                        if (k == totalBytes) {
                            BITalinoFrame bitatinoFrame = null;
                            try {
                                bitatinoFrame = BITalinoFrameDecoder.decode(mBluetoothDeviceAddress, buffer, analogChannels, totalBytes);

                                intent.putExtra(EXTRA_DATA, bitatinoFrame);
                                activityContext.sendBroadcast(intent);

                            } catch (BITalinoException | IOException e) {
                                e.printStackTrace();
                            }
                            k = 0;

                            buffer = new byte[totalBytes];

                            if (bitatinoFrame.getSequence() - previousSeq != 1 && previousSeq - bitatinoFrame.getSequence() != 0 && Math.abs(previousSeq - bitatinoFrame.getSequence()) != 15) {
                                int nSeq = bitatinoFrame.getSequence() - previousSeq;
                                Log.e(TAG, "[" + mBluetoothDeviceAddress + "] " + "Seq: " + nSeq);
                            }

                            lastSampleTimeStamp = Calendar.getInstance().getTimeInMillis();
                            isDataStreaming = true;

                            previousSeq = bitatinoFrame.getSequence();
                        }
                    }
                }
                else {
                    String description = null;
                    try {
                        description = new String(data, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    Intent descriptionIntent = new Intent(ACTION_COMMAND_REPLY);
                    descriptionIntent.putExtra(EXTRA_COMMAND_REPLY, description);
                    activityContext.sendBroadcast(descriptionIntent);
                }
            }
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.d(TAG, stringBuilder.toString());
                //intent.putExtra(Constants.EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
    }


    @Override
    public void setDataStreamingAlarm() {
        isDataStreamingAlarmSet = true;
    }

    @Override
    public boolean start(int[] analogChannels, int sampleRate) throws BITalinoException {
        boolean flag = false;
        if(isConnected) {
            if (currentState.equals(States.CONNECTED)) {
                this.analogChannels = validateAnalogChannels(analogChannels);
                this.totalBytes = calculateTotalBytes(analogChannels);
                buffer = new byte[totalBytes];
                setFreq(sampleRate);

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                CommandArguments commandArguments = new CommandArguments();
                commandArguments.setAnalogChannels(analogChannels);

                byte[] command = BITalino.START.getCommand(commandArguments).command;

                flag = writeCharacteristic(characteristicCommands, command);

                setState(States.ACQUISITION_OK);

                inAcquisition = true;

                if(isDataStreamingAlarmSet){
                    setLogAlarm(LOG_DATA_STREAM, WAIT_TIME_3SECONDS);
                }

            } else {
                throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IDLE);
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return flag;
    }

    @Override
    protected boolean setFreq(int sampleRate) throws BITalinoException {
        CommandArguments commandArguments = new CommandArguments();
        commandArguments.setSampleRate(validateSampleRate(sampleRate));

        byte[] command = BITalino.SET_FREQ.getCommand(commandArguments).command;

        return writeCharacteristic(characteristicCommands, command);
    }

    @Override
    public boolean stop() throws BITalinoException {
        boolean flag = false;
        if(isConnected) {
            if (currentState.equals(States.ACQUISITION_OK)) {

                CommandArguments commandArguments = new CommandArguments();
                byte[] command = BITalino.STOP.getCommand(commandArguments).command;

                flag = writeCharacteristic(characteristicCommands, command);

                setState(States.CONNECTED);

                inAcquisition = false;

            } else {
                throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IN_ACQUISITION_MODE);
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }

        return flag;
    }

    @Override
    public boolean connect(String address) throws BITalinoException {
        mBluetoothDeviceAddress = address;

        if(currentState.equals(States.DISCONNECTED) || currentState.equals(States.ENDED) || currentState.equals(States.NO_CONNECTION)) {
            if (mBluetoothAdapter == null || address == null) {
                Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
                return false;
            }


            // Previously connected device.  Try to reconnect.
            if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
                Log.d(TAG, mBluetoothDeviceAddress + " - Trying to use an existing mBluetoothGatt for connection.");
                if (mBluetoothGatt.connect()) {
                    Log.d(TAG, mBluetoothDeviceAddress + " - Connected.");
                    setState(States.CONNECTING);
                    return true;
                } else {
                    Log.d(TAG, mBluetoothDeviceAddress + " - Failed to connect.");
                    return false;
                }
            }

            BluetoothDevice devicePlux = mBluetoothAdapter.getRemoteDevice(address);
            if (devicePlux == null) {
                Log.w(TAG, mBluetoothDeviceAddress + " - Device not found.  Unable to connect.");
                return false;
            }

            //device.createBond();
            mBluetoothGatt = devicePlux.connectGatt(activityContext, true, mGattCallback);
            Log.d(TAG, mBluetoothDeviceAddress + " - Trying to create a new connection.");

            setState(States.CONNECTING);
        }
        else{
            throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IDLE);
        }

        return true;
    }

    @Override
    public void disconnect() throws BITalinoException {

        if(currentState.equals(States.CONNECTED) || currentState.equals(States.ACQUISITION_OK)) {

            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }

            disconnectFired = true;

            cancelLogAlarm(LOG_DATA_STREAM);

            closeReceivers();

            mBluetoothGatt.disconnect();
        }
        else{
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
    }

    @Override
    public boolean getVersion() throws BITalinoException {
        boolean flag = false;
        if(isConnected) {
            if(!inAcquisition) {

                CommandArguments commandArguments = new CommandArguments();
                commandArguments.setBLE(false);

                byte[] command = BITalino.VERSION.getCommand(commandArguments).command;

                flag = writeCharacteristic(characteristicCommands, command);

                isWaitingForVersion = true;
            } else {
                throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IDLE);
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return flag;
    }

    @Override
    public boolean battery(int value) throws BITalinoException {
        boolean flag = false;
        if(isConnected) {
            if(!inAcquisition) {

                if(value < 0 || value > 63){
                    throw new BITalinoException(BITalinoErrorTypes.INVALID_PARAMETER);
                }

                //commandBattery = (byte)(value) << 2

                CommandArguments commandArguments = new CommandArguments();
                commandArguments.setBatteryThreshold(value);
                byte[] command = BITalino.BATTERY.getCommand(commandArguments).command;

                flag = writeCharacteristic(characteristicCommands, command);

                isWaitingForVersion = true;
            } else {
                throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IDLE);
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return flag;
    }

    @Override
    public boolean trigger(int[] digitalChannels) throws BITalinoException {
        boolean flag = false;
        if(isConnected) {
            if (digitalChannels.length != 0 && digitalChannels.length != 2) {
                throw new BITalinoException(BITalinoErrorTypes.INVALID_PARAMETER);
            }

            CommandArguments commandArguments = new CommandArguments();
            commandArguments.setBITalino2(isBITalino2);
            commandArguments.setDigitalChannels(digitalChannels);

            byte[] command = BITalino.TRIGGER.getCommand(commandArguments).command;

            flag = writeCharacteristic(characteristicCommands, command);
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return flag;
    }

    @Override
    public boolean state() throws BITalinoException {
        boolean flag = false;
        if(isConnected) {
            if(isBITalino2) {
                if(!inAcquisition) {

                    CommandArguments commandArguments = new CommandArguments();

                    byte[] command = BITalino.STATE.getCommand(commandArguments).command;

                    flag = writeCharacteristic(characteristicCommands, command);

                    isWaitingForState = true;
                } else {
                    throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IDLE);
                }
            }
            else {
                throw new BITalinoException(BITalinoErrorTypes.NOT_SUPPORTED);
            }

        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return flag;
    }

    @Override
    public boolean pwm(int pwmOutput) throws BITalinoException {
        boolean flag = false;
        if(isConnected) {
            if(isBITalino2) {

                if(pwmOutput < 0 || pwmOutput > 255){
                    throw new BITalinoException(BITalinoErrorTypes.INVALID_PARAMETER);
                }

                CommandArguments commandArguments = new CommandArguments();
                commandArguments.setPwmOutput(pwmOutput);

                byte[] command = BITalino.PWM.getCommand(commandArguments).command;

                flag = writeCharacteristic(characteristicCommands, command);

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                flag = writeCharacteristic(characteristicCommands, new byte[]{(byte)(pwmOutput)});


            }else {
                    throw new BITalinoException(BITalinoErrorTypes.NOT_SUPPORTED);
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return flag;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    private void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        Log.d(TAG, mBluetoothDeviceAddress + " - Close mBluetoothGatt.");

    }

    private void setupCharacteristicNotifications(List<BluetoothGattService> gattServices) {

        if (gattServices == null) {
            return;
        }

//        Log.d(TAG,"gattServices: " +  gattServices.size());

        // Loops through available GATT Services.
//        int i = 0;
        for (BluetoothGattService gattService : gattServices) {

//            Log.d(TAG, i + " -> " + gattService.getUuid().toString());
//            i++;

            // Loops through available Characteristics.
            if (gattService.getUuid().equals(UUID.fromString(PluxGattAttributes.DataS))) {
                pluxCharacteristics = gattService.getCharacteristics();

//                int j = 0;
//                for (BluetoothGattCharacteristic characteristic: pluxCharacteristics) {
//                    Log.d(TAG, j + " -> " + characteristic.getUuid().toString());
//                    j++;
//                }

                //Set commands characteristic notification
                if(gattService.getUuid().equals(UUID.fromString(PluxGattAttributes.DataS))) {
                    setCharacteristicNotification(pluxCharacteristics.get(0));
                }

                setLogAlarm();

            }
        }
    }

    /**
     * Enables or disables notification on a give characteristic.
     *  @param characteristic Characteristic to act on.
     *
     */
    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        // enable local notifications - maximum of 4 characteristics is available
        //boolean result = mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        //System.out.println("Characteristic: " + characteristic.getUuid().toString() + "RESULT: " + result + " enabled " + enabled + "\n");

        if (UUID_COMMANDS.equals(characteristic.getUuid())) {
            //Commands
            characteristicCommands = characteristic;

            mBluetoothGatt.setCharacteristicNotification(characteristicCommands, true);

            BluetoothGattDescriptor descriptorCommands = characteristicCommands.getDescriptor(UUID.fromString(PluxGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));

            descriptorCommands.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptorCommands);
        }
        if (UUID_FRAMES.equals(characteristic.getUuid())) {
            //Frames

            mBluetoothGatt.setCharacteristicNotification(characteristic, true);

            BluetoothGattDescriptor descriptorFrames = characteristic.getDescriptor(UUID.fromString(PluxGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));

            descriptorFrames.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptorFrames);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    private List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * Write to the characteristic
     *
     * @param characteristic
     * @param msg
     * @return
     */
    private boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] msg) {

        //check mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }

        characteristic.setValue(msg);
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    private byte[][] parseByteArray(byte[] command) {
        byte[][] msg = new byte[2][];

        if (command.length > 20) {
            msg[0] = Arrays.copyOfRange(command, 0, 20);
            msg[1] = Arrays.copyOfRange(command, 20, command.length);
        }

        return msg;
    }

    private void setLogAlarm(int id, int intervalMillis) {
        Intent alarmIntent = new Intent(LOG_ALARM);
        alarmIntent.putExtra(LOG_ALARM_EXTRA_ID, id);
        alarmIntent.putExtra(LOG_ALARM_EXTRA_IDENTIFIER, mBluetoothDeviceAddress);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activityContext, id, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) activityContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + intervalMillis, pendingIntent);
    }

    private void setLogAlarm() {
        Intent alarmIntent = new Intent(LOG_ALARM);
        alarmIntent.putExtra(LOG_ALARM_EXTRA_ID, LOG_ENABLE_COMMANDS);
        alarmIntent.putExtra(LOG_ALARM_EXTRA_IDENTIFIER, mBluetoothDeviceAddress);
        alarmIntent.putExtra(LOG_ALARM_EXTRA_ACTION, commandsAction);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activityContext, LOG_ENABLE_COMMANDS, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) activityContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + WAIT_TIME_1SECONDS, pendingIntent);
    }

    private void cancelLogAlarm(int id){
        AlarmManager alarmManager = (AlarmManager) activityContext.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(LOG_ALARM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activityContext, id, alarmIntent, PendingIntent.FLAG_ONE_SHOT);

        alarmManager.cancel(pendingIntent);
    }

    private void reconnect(){
        Log.d(TAG, "reconnect()");

        if(!disconnectFired) {

            isReady = false;
            isConnected = false;
            isDataStreaming = true;
            isCommandsEnabled = false;
            isFramesEnabled = false;
            isServiceDiscovered = false;

            try {
                disconnect();
            } catch (BITalinoException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                connect(mBluetoothDeviceAddress);
            } catch (BITalinoException e) {
                e.printStackTrace();
            }

            //Log.d(TAG, "Try to reconnect: " + connect(mBluetoothDeviceAddress));
        }
    }
}
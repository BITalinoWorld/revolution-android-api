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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Parcelable;
import android.util.Log;
import info.plux.pluxapi.CommandArguments;
import info.plux.pluxapi.bitalino.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;

import static info.plux.pluxapi.Constants.*;

public class BTHCommunication extends BITalinoCommunication {

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    /*
     * Alarm Variables
     */
    private final String ALARM           = "info.plux.pluxapi.bitalino.bth.BTHCommunication.ALARM";
    private final String ALARM_ID        = "info.plux.pluxapi.bitalino.bth.BTHCommunication.ALARM_ID";
    private final String ALARM_DEVICE_ID = "info.plux.pluxapi.bitalino.bth.BTHCommunication.ALARM_DEVICE_ID";
    //Alarm Types
    private final int DATA_STREAM_ALARM      = 0;
    private final int WAIT_TIME_1SECONDS     = 1000;
    private final int WAIT_TIME_2SECONDS     = 2 * WAIT_TIME_1SECONDS;
    private final int WAIT_TIME_3SECONDS     = 3 * WAIT_TIME_1SECONDS;
    private final int WAIT_TIME_5SECONDS     = 5 * WAIT_TIME_1SECONDS;
    public String mBluetoothDeviceAddress = "00:00:00:00:00:00";
    private final String TAG = this.getClass().getSimpleName();
    private final Context activityContext;
    // Member fields
    private final BluetoothAdapter mAdapter;
    private final OnBITalinoDataAvailable callback;
    private final BroadcastReceiver incomingPairRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //pair from device: dev.getName()
                Log.d(TAG, "bitalino - incomingPairRequestReceiver");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if(device != null && device.getName() != null && device.getName().toLowerCase().contains("bitalino")) {
//                        if (info.plux.pluxapi.bioplux.bth.BTHDeviceScan.testPLUXDevice(device.getName())) {
                            try {
                                int pin = 1234;
                                //the pin in case you need to accept for an specific pin
                                Log.d("PIN", " " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0));
                                //maybe you look for a name or address
                                Log.d("Bonded", device.getName());
                                byte[] pinBytes;
                                pinBytes = ("" + pin).getBytes("UTF-8");
                                device.setPin(pinBytes);
                                //setPairing confirmation if needed
                                device.setPairingConfirmation(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
//                        }
                    }
                }
            }
        }
    };
    /*
     * Connection Variables
     */
    private BluetoothDevice bluetoothDevice;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private States currentState;
    private boolean isConnected = false;
    private boolean inAcquisition = false;
    private boolean isBITalino2 = false;
    private boolean isStateCorrect = false;
    private int stateTotalBytes = 16;
    private boolean isIncomingPairRequestReceiverRegistered = false;
    /*
     * Acquisition Variables
     */
    private CommandDecoder commandDecoder = new CommandDecoder();
    private boolean isDataStreaming = false;
    private long lastSampleTimeStamp;
    private boolean isWaitingForState = false, isWaitingForVersion = false; ;
    private boolean isAlarmReceiverRegistered = false;
    private final BroadcastReceiver AlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = intent.getIntExtra(ALARM_ID, 0);
            String identifier = intent.getStringExtra(ALARM_DEVICE_ID);

            if(identifier != null && identifier.equals(mBluetoothDeviceAddress)) {

                Intent alarmIntent = new Intent(ALARM);
                alarmIntent.putExtra(ALARM_ID, id);
                alarmIntent.putExtra(ALARM_DEVICE_ID, mBluetoothDeviceAddress);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(activityContext, id, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
                AlarmManager alarmManager = (AlarmManager) activityContext.getSystemService(Context.ALARM_SERVICE);

                switch (id) {
                    case DATA_STREAM_ALARM:
                        long deltaTime = Calendar.getInstance().getTimeInMillis() - lastSampleTimeStamp;

                        if (deltaTime >= WAIT_TIME_3SECONDS) {
                            isDataStreaming = false;
                            alarmManager.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + WAIT_TIME_5SECONDS, pendingIntent);
                        }

                        if (!isDataStreaming && isConnected) {
                            Log.e(TAG, "No Data Streaming");

                            connectionLost();

                            alarmManager.cancel(pendingIntent);
                        } else if (isDataStreaming) {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + WAIT_TIME_5SECONDS, pendingIntent);
                        } else {
                            alarmManager.cancel(pendingIntent);
                        }
                        break;
                }
            }
        }
    };

    private int previousSeq = 0;

    public BTHCommunication(Context activityContext, final OnBITalinoDataAvailable callBack) {
        super(activityContext);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.callback = callBack;
        this.activityContext = activityContext;

        init();
    }

    @Override
    public void init() {
        activityContext.registerReceiver(AlarmReceiver, new IntentFilter(ALARM));
        isAlarmReceiverRegistered = true;

        //Set pairing request intent
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        activityContext.registerReceiver(incomingPairRequestReceiver, intentFilter);
        isIncomingPairRequestReceiverRegistered = true;
    }

    @Override
    public void closeReceivers() {
        if(isAlarmReceiverRegistered){
            activityContext.unregisterReceiver(AlarmReceiver);
            isAlarmReceiverRegistered = false;
        }

        if(isIncomingPairRequestReceiverRegistered){
            activityContext.unregisterReceiver(incomingPairRequestReceiver);
            isIncomingPairRequestReceiverRegistered = false;
        }
    }

    @Override
    public void setDataStreamingAlarm() {

    }

    @Override
    public boolean start(int[] analogChannels, int sampleRate) throws BITalinoException {
        if(isConnected) {
            if (currentState.equals(States.CONNECTED)) {
                this.analogChannels = validateAnalogChannels(analogChannels);
                this.totalBytes = calculateTotalBytes(analogChannels);
                setFreq(sampleRate);

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                CommandArguments commandArguments = new CommandArguments();
                commandArguments.setAnalogChannels(analogChannels);

                byte[] command = BITalino.START.getCommand(commandArguments).command;

                try {
                    mConnectedThread.write(command);
                } catch (Exception e) {
                    throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
                }

                setState(States.ACQUISITION_OK);

                inAcquisition = true;

                setAlarm();
            } else {
                throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IDLE);
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return true;
    }

    @Override
    protected boolean setFreq(int sampleRate) throws BITalinoException {
        CommandArguments commandArguments = new CommandArguments();
        commandArguments.setSampleRate(validateSampleRate(sampleRate));

        byte[] command = BITalino.SET_FREQ.getCommand(commandArguments).command;

        try {
            mConnectedThread.write(command);
        } catch (Exception e) {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return true;
    }

    /**
     * Stops acquisition.
     *
     * @throws BITalinoException
     */
    public boolean stop() throws BITalinoException {
        if(isConnected) {
            if (currentState.equals(States.ACQUISITION_OK)) {

                CommandArguments commandArguments = new CommandArguments();
                byte[] command = BITalino.STOP.getCommand(commandArguments).command;

                try {
                    mConnectedThread.write(command);
                } catch (Exception e) {
                    throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION);
                }

                setState(States.CONNECTED);

                inAcquisition = false;
            } else {
                throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IN_ACQUISITION_MODE);
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return false;
    }

    @Override
    public boolean connect(String address) throws BITalinoException {
        launch(address);
        return true;
    }

    @Override
    public void disconnect() throws BITalinoException {
        if(isConnected) {
            isConnected = false;
            inAcquisition = false;

            setState(States.DISCONNECTED);

            stopThreads();
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
    }

    @Override
    public boolean getVersion() throws BITalinoException {
        if(isConnected) {
            if(!inAcquisition) {

                CommandArguments commandArguments = new CommandArguments();
                commandArguments.setBLE(false);

                byte[] command = BITalino.VERSION.getCommand(commandArguments).command;

                try {
                    mConnectedThread.write(command);
                } catch (Exception e) {
                    throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION);
                }

                isWaitingForVersion = true;
            } else {
                throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IDLE);
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return true;
    }

    @Override
    public boolean battery(int value) throws BITalinoException {
        if(isConnected) {
            if(!inAcquisition) {

                if(value < 0 || value > 63){
                    throw new BITalinoException(BITalinoErrorTypes.INVALID_PARAMETER);
                }

                //commandBattery = (byte)(value) << 2

                CommandArguments commandArguments = new CommandArguments();
                commandArguments.setBatteryThreshold(value);
                byte[] command = BITalino.BATTERY.getCommand(commandArguments).command;

                try {
                    mConnectedThread.write(command);
                } catch (Exception e) {
                    throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION);
                }
            } else {
                throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IDLE);
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return true;
    }

    @Override
    public boolean trigger(int[] digitalChannels) throws BITalinoException {
        CommandArguments commandArguments = new CommandArguments();
        commandArguments.setBITalino2(isBITalino2);
        commandArguments.setDigitalChannels(digitalChannels);

        if(isConnected) {
            if(isBITalino2 && (currentState.equals(States.ACQUISITION_OK) || currentState.equals(States.CONNECTED))) {

                if (digitalChannels.length != 0 && digitalChannels.length != 2) {
                    throw new BITalinoException(BITalinoErrorTypes.INVALID_PARAMETER);
                }

                byte[] command = BITalino.TRIGGER.getCommand(commandArguments).command;

                try {
                    mConnectedThread.write(command);
                } catch (Exception e) {
                    throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION);
                }

            }
            else{
                if (currentState.equals(States.ACQUISITION_OK)) {

                    if (digitalChannels.length != 0 && digitalChannels.length != 4) {
                        throw new BITalinoException(BITalinoErrorTypes.INVALID_PARAMETER);
                    }

                    byte[] command = BITalino.TRIGGER.getCommand(commandArguments).command;

                    try {
                        mConnectedThread.write(command);
                    } catch (Exception e) {
                        throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION);
                    }


                } else {
                    throw new BITalinoException(BITalinoErrorTypes.DEVICE_NOT_IN_ACQUISITION_MODE);
                }
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }

        return true;
    }


    @Override
    public boolean state() throws BITalinoException {
        if(isConnected) {
            if(isBITalino2) {

                CommandArguments commandArguments = new CommandArguments();
                byte[] command = BITalino.STATE.getCommand(commandArguments).command;

                try {
                    mConnectedThread.write(command);
                } catch (Exception e) {
                    throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION);
                }

                isWaitingForState = true;
            }
            else {
                throw new BITalinoException(BITalinoErrorTypes.NOT_SUPPORTED);
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return true;
    }

    @Override
    public boolean pwm(int pwmOutput) throws BITalinoException {
        if(isConnected) {
            if(isBITalino2) {

                if(pwmOutput < 0 || pwmOutput > 255){
                    throw new BITalinoException(BITalinoErrorTypes.INVALID_PARAMETER);
                }

                CommandArguments commandArguments = new CommandArguments();
                byte[] command = BITalino.PWM.getCommand(commandArguments).command;

                try {
                    mConnectedThread.write(command);
                } catch (Exception e) {
                    throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION);
                }

                try {
                    mConnectedThread.write(new byte[]{(byte)(pwmOutput)});
                } catch (Exception e) {
                    throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION);
                }
            }
            else {
                throw new BITalinoException(BITalinoErrorTypes.NOT_SUPPORTED);
            }
        } else {
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
        return true;
    }

    /*
     * Bluetooth - Threads
     */
    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void launch(String address) {
        Log.d(TAG, "start");


        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            if(!mConnectThread.isInterrupted()) {
                mConnectThread.interrupt();
            }
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            if(!mConnectedThread.isInterrupted()) {
                mConnectedThread.interrupt();
            }
            mConnectedThread = null;
        }

        setState(States.LISTEN);

        mBluetoothDeviceAddress = address;
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);

        Log.d(TAG, "Stopping Bluetooth discovery.");
        bluetoothAdapter.cancelDiscovery();

        connect(bluetoothDevice);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            if(!mConnectThread.isInterrupted()) {
                mConnectThread.interrupt();
            }
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            if(!mConnectedThread.isInterrupted()) {
                mConnectedThread.interrupt();
            }
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(States.CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *  @param socket The BluetoothSocket on which the connection was made
     *
     */
    private synchronized void connected(BluetoothSocket socket, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            if(!mConnectThread.isInterrupted()) {
                mConnectThread.interrupt();
            }
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            if(!mConnectedThread.isInterrupted()) {
                mConnectedThread.interrupt();
            }
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
//        mConnectedThread.setPriority(Thread.MAX_PRIORITY);
        mConnectedThread.start();

        setState(States.CONNECTED);
    }

    /**
     * Stop all threads
     */
    private synchronized void stopThreads() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            if(!mConnectThread.isInterrupted()) {
                mConnectThread.interrupt();
            }
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            if(!mConnectedThread.isInterrupted()) {
                mConnectedThread.interrupt();
            }
            mConnectedThread = null;
        }

        setState(States.NO_CONNECTION);
    }

    private void connectionLost(){
        Log.d(TAG, "connectionLost");

        inAcquisition = false;
        isConnected = false;

        // Send a failure message back to the Activity
        setState(States.DISCONNECTED);

        // Start the service over to restart listening mode
        connect(bluetoothDevice);

    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            bluetoothDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = "Secure";

            Log.d(TAG, "getBondState -> " + mmDevice.getBondState());

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType + mBluetoothDeviceAddress);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionLost();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (this) {
                if(mConnectThread != null){
                    if(!mConnectThread.isInterrupted()){
                        mConnectThread.interrupt();
                    }
                }
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mSocketType);


        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private long aliveTime = 0;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            isConnected = true;
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            this.setPriority(Thread.MAX_PRIORITY);

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread"+ mBluetoothDeviceAddress);
            setName("ConnectedThread" + mBluetoothDeviceAddress);
            //byte[] buffer = new byte[1024];
            byte[] buffer = new byte[1024];
            int bytes;

            try {
                getVersion();
            } catch (BITalinoException e) {
                e.printStackTrace();
            }

            // Keep listening to the InputStream while connected
            while (isConnected) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

//                    Log.d(TAG, "bytes: " + bytes);
//
//                    Log.d(TAG, "buffer[0]: " + String.format("%02X",buffer[0]));

                    //fix for "sleepy" bth connection
                    if((System.currentTimeMillis() - aliveTime) > 3000){
                        System.gc();

                        if(isBITalino2) {
                            write(new byte[]{0x50});
                        }
                        else{
                            write(new byte[]{0x03});
                        }

                        aliveTime = System.currentTimeMillis();
                    }

                    if (bytes > 0) {
//                        Log.d(TAG, "totalBytes: " + totalBytes);
                        if (inAcquisition) {

                            byte[][] dataReceived = commandDecoder.parseReceivedData(CommandDecoder.CommandType.DATA_FRAMES, buffer, bytes, totalBytes);

//                            Log.d(TAG, "dataReceived.length: " + dataReceived.length);

                            for (byte[] byteArray : dataReceived) {
                                if (byteArray != null) {
                                    BITalinoFrame bitalinoFrame = null;

                                    isDataStreaming = true;
                                    lastSampleTimeStamp = Calendar.getInstance().getTimeInMillis();

                                    try {
                                        bitalinoFrame = BITalinoFrameDecoder.decode(mBluetoothDeviceAddress, byteArray, analogChannels, totalBytes);
                                    } catch (IOException | BITalinoException e) {
                                        e.printStackTrace();
                                    }

                                    if (bitalinoFrame.getSequence() - previousSeq != 1 && previousSeq - bitalinoFrame.getSequence() != 0 && Math.abs(previousSeq - bitalinoFrame.getSequence()) != 15) {
                                        int nSeq = bitalinoFrame.getSequence() - previousSeq;
                                        Log.e(TAG, mBluetoothDeviceAddress + " - lost: " + nSeq + " frames");
                                    }
                                    previousSeq = bitalinoFrame.getSequence();

                                    callback.onBITalinoDataAvailable(bitalinoFrame);
                                } else {
                                    break;
                                }
                            }
                        } else {
                            if(isWaitingForState){

                                byte[][] dataReceived = commandDecoder.parseReceivedData(CommandDecoder.CommandType.STATE, buffer, bytes, 16);

                                Log.d(TAG, "dataReceived.length: " + dataReceived.length);

                                for (byte[] byteArray : dataReceived) {
                                    if (byteArray != null) {
                                        BITalinoState bitalinoState = null;

                                        try {
                                            bitalinoState = BITalinoStateDecoder.decode(mBluetoothDeviceAddress, byteArray, isStateCorrect);
                                        } catch (IOException | BITalinoException e) {
                                            e.printStackTrace();
                                        }

                                        Log.d(TAG, "BITalino state: " + bitalinoState.toString());

                                        isWaitingForState = false;

                                        sendReplyBroadcast(bitalinoState);

                                    } else {
                                        break;
                                    }
                                }
                            }
                            else if(isWaitingForVersion){
                                byte[][] dataReceived = commandDecoder.parseReceivedData(CommandDecoder.CommandType.VERSION, buffer, bytes, stateTotalBytes);

                                for (byte[] byteArray : dataReceived) {
                                    if (byteArray != null) {
                                        String description = "Description: " + new String(byteArray, StandardCharsets.UTF_8);

                                        float version = 0;

                                        if(bluetoothDevice.getName() != null && bluetoothDevice.getName().equals("bitalino")){
//                                            String[] versionArray = new String(byteArray, StandardCharsets.UTF_8).split("_");

                                            isBITalino2 = false;
//
//                                            Log.d(TAG, Arrays.toString(versionArray));
//
//                                            version = Float.parseFloat(versionArray[1].substring(1,4));
                                        }
                                        else {
                                            String[] versionArray = new String(byteArray, StandardCharsets.UTF_8).split("_v");

                                            if (Float.parseFloat(versionArray[1]) >= 5) {
                                                isBITalino2 = true;
                                                Log.d(TAG, "isBITalino2: " + isBITalino2 + " -> " + Float.parseFloat(versionArray[1]));
                                                if (Float.parseFloat(versionArray[1]) >= 5.2f) {
                                                    isStateCorrect = true;
                                                    stateTotalBytes = 17;
                                                }
                                            }

                                            version = Float.parseFloat(versionArray[1]);
                                        }

                                        Log.d(TAG, description);
                                        Log.d(TAG, "version: " + version);

                                        isWaitingForVersion = false;

                                        sendReplyBroadcast(new BITalinoDescription(isBITalino2, version));

                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    try {
                        mmSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    if(isConnected) {
                        connectionLost();
                    }

                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /*
     * Auxiliary methods
     */

    /**
     * Return the current connection state.
     */
    public synchronized States getState() {
        return currentState;
    }

    /**
     * Set the current state of the BTH connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(States state) {
        Log.d(TAG, "setState() " + state + " -> " + state.name());
        currentState = state;

        // Give the new state to the Handler so the UI Activity can update
        Intent intent = new Intent(ACTION_STATE_CHANGED);
        intent.putExtra(IDENTIFIER, mBluetoothDeviceAddress);
        intent.putExtra(EXTRA_STATE_CHANGED, state.getId());
        activityContext.sendBroadcast(intent);
    }

    private void sendReplyBroadcast(Parcelable extraArgument){
        Intent intent = new Intent(ACTION_COMMAND_REPLY);
        intent.putExtra(IDENTIFIER, mBluetoothDeviceAddress);
        intent.putExtra(EXTRA_COMMAND_REPLY, extraArgument);
        activityContext.sendBroadcast(intent);
    }

    /*
     * Alarm
     */

    private void setAlarm() {
        Intent alarmIntent = new Intent(ALARM);
        alarmIntent.putExtra(ALARM_ID, DATA_STREAM_ALARM);
        alarmIntent.putExtra(ALARM_DEVICE_ID, mBluetoothDeviceAddress);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activityContext, DATA_STREAM_ALARM, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) activityContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + WAIT_TIME_2SECONDS, pendingIntent);
    }








}
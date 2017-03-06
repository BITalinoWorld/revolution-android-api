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

import java.util.Arrays;

public abstract class BITalinoCommunication {
    protected int sampleRate;
    protected int totalBytes;
    protected int[] analogChannels;
    protected String address;
    protected Context activityContext;

    public BITalinoCommunication(Context activityContext){
        this.activityContext = activityContext;
    }

    /**
     *
     */
    public abstract void init();

    public abstract void closeReceivers();

    /**
     *
     */
    public abstract void setDataStreamingAlarm();

    /**
     * Starts the acquisition mode of the device. An exception is thrown if the device is already acquiring.
     * The sampleRate must be 1Hz, 10Hz, 100Hz or 1000Hz.
     * On acquisition mode, the frames sent by the bluetooth device are received by the phone and then sent to the local broadcast receiver in the BLE case, or to the OnBITalinoDataAvailable callback in the BTH case, using the BITalinoFrame object.
     * @param analogChannels an array with the active analog channels
     * @param sampleRate the sampling frequency value
     * @return true if the command is sent successfully to the BITalino device, false otherwise
     * @throws BITalinoException
     */
    public abstract boolean start(int[] analogChannels, int sampleRate) throws BITalinoException;

    /**
     *
     * @param sampleRate
     * @return
     * @throws BITalinoException
     */
    protected abstract boolean setFreq(int sampleRate) throws BITalinoException;

    /**
     * Stops the acquisition mode in the device. An exception is throw if the acquisition mode is not active
     * @return true if the command is sent successfully, false otherwise
     * @throws BITalinoException
     */
    public abstract boolean stop() throws BITalinoException;

    /**
     *  Tries tp cpmmect tp tje device with the given MAC address
     * @param address Media Access Control (MAC) address, the unique identifier of the device
     * @return true if the connection to the device is successful, false otherwise
     * @throws BITalinoException
     */
    public abstract boolean connect(String address) throws BITalinoException;

    /**
     * Disconnects the device and closes the connection channel created.
     * @throws BITalinoException
     */
    public abstract void disconnect() throws BITalinoException;

    /**
     * Get the device's firmware version
     * @return true if the command is sent successfully, false otherwise
     * @throws BITalinoException
     */
    public abstract boolean getVersion() throws BITalinoException;

    /**
     * Sets a new battery threshold for the low-battery LED
     * @param value the new battery threshold value
     * @return true if the command is sent successfully, false otherwise
     * @throws BITalinoException
     */
    public abstract boolean battery(int value) throws BITalinoException;

    /**
     * Assigns the digital output states
     * @param digitalChannels an array with the digital channels to enable set as 1, and digital channels to disable set as 0
     * @return true of the command is sent successfully, false otherwise
     * @throws BITalinoException
     */
    public abstract boolean trigger(int[] digitalChannels) throws BITalinoException;

    /**
     * Asks fot the device's current state [BITalino 2 only]
     * @return true of the command is sent successfully, false otherwise
     * @throws BITalinoException
     */
    public abstract boolean state() throws BITalinoException;

    /**
     * Assigns the analog (PWM) output value [BITalino 2 only]
     * @param pwmOutput analog output [0,255]
     * @return true of the command is sent successfully, false otherwise
     * @throws BITalinoException
     */
    public abstract boolean pwm(int pwmOutput) throws BITalinoException;

    /**
     * Validates the sampling rate value
     * @param sampleRate
     * @return
     */
    protected int validateSampleRate (int sampleRate){
        return sampleRate != 1 && sampleRate != 10 && sampleRate != 100 && sampleRate != 1000 ? 1000 : sampleRate;
    }

    /**
     * Validate the Analog Channels array
     * @param channels
     * @return
     */
    protected int[] validateAnalogChannels (int[] channels){
        if (channels.length < 1 || channels.length > 6) {
            try {
                throw new BITalinoException(BITalinoErrorTypes.INVALID_ANALOG_CHANNELS);
            } catch (BITalinoException e) {
                e.printStackTrace();
            }
        }
        // validate analog channels identifiers
        for (int channel : channels) {
            if (channel < 0 || channel > 5) {
                try {
                    throw new BITalinoException(BITalinoErrorTypes.INVALID_ANALOG_CHANNELS);
                } catch (BITalinoException e) {
                    e.printStackTrace();
                }
            }
        }
        Arrays.sort(channels);

        analogChannels = channels;

        return channels;
    }

    /**
     * Calculates the byte array length
     * @param analogChannels
     * @return
     */
    protected int calculateTotalBytes(int [] analogChannels){
        return analogChannels.length <= 4 ? (int) Math.ceil((12f + 10f * analogChannels.length) / 8) : (int) Math.ceil((52f + 6f * (analogChannels.length - 4)) / 8);
    }


}
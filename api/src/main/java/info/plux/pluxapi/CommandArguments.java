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



public class CommandArguments {

    //BITalino
    private boolean isBITalino2;
    private boolean isBLE;
    private int[] analogChannels;
    private int sampleRate;
    private int[] digitalChannels;
    private int batteryThreshold;
    private int pwmOutput;

    public CommandArguments(){}

    /*
     * BITalino
     */
    public boolean isBITalino2() {
        return isBITalino2;
    }

    public void setBITalino2(boolean BITalino2) {
        isBITalino2 = BITalino2;
    }

    public boolean isBLE() {
        return isBLE;
    }

    public void setBLE(boolean BLE) {
        isBLE = BLE;
    }

    public int[] getAnalogChannels() {
        return analogChannels;
    }

    public void setAnalogChannels(int[] analogChannels) {
        this.analogChannels = analogChannels;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int[] getDigitalChannels() {
        return digitalChannels;
    }

    public void setDigitalChannels(int[] digitalChannels) {
        this.digitalChannels = digitalChannels;
    }

    public int getBatteryThreshold() {
        return batteryThreshold;
    }

    public void setBatteryThreshold(int batteryThreshold) {
        this.batteryThreshold = batteryThreshold;
    }

    public int getPwmOutput() {
        return pwmOutput;
    }

    public void setPwmOutput(int pwmOutput) {
        this.pwmOutput = pwmOutput;
    }
}
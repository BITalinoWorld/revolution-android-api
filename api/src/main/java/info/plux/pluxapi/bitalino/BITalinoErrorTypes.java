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

import java.util.HashMap;
import java.util.Map;

public enum BITalinoErrorTypes {
    BT_DEVICE_NOT_CONNECTED         (0, "Bluetooth Device not connected"),
    PORT_COULD_NOT_BE_OPENED        (1, "The communication port could not be initialized. The provided parameters could not be set."),
    DEVICE_NOT_IDLE                 (2, "Device not in idle mode"),
    DEVICE_NOT_IN_ACQUISITION_MODE  (3, "Device not is acquisition mode"),
    UNDEFINED_SAMPLING_RATE_NOT     (4, "The Sampling Rate chose cannot be set in BITalino. Choose 1000,100,10 or 1"),
    LOST_COMMUNICATION              (5, "The device lost communication"),
    INVALID_PARAMETER               (6, "Invalid parameter"),
    INVALID_THRESHOLD               (7, "The threshold value must be between 0 and 63"),
    INVALID_ANALOG_CHANNELS         (8, "The number of analog channels available are between 0 and 5"),
    DECODE_INVALID_DATA             (9, "Incorrect data to be decoded"),
    INVALID_DIGITAL_CHANNELS        (10, "To set the digital outputs, the input array must have 4 items [BITalino1] or 2 items [BITalino2], one for each channel."),
    INVALID_MAC_ADDRESS             (11, "MAC address not valid."),
    NOT_SUPPORTED                   (12, "This method is not supported for this device."),
    UNDEFINED                       (13, "UNDEFINED ERROR");

    private final int value;
    private final String description;

    private static Map<Integer, BITalinoErrorTypes> map = new HashMap<Integer, BITalinoErrorTypes>();

    static {
        for (BITalinoErrorTypes error : BITalinoErrorTypes.values())
            map.put(error.getValue(), error);
    }

    BITalinoErrorTypes(final int value, final String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static BITalinoErrorTypes fromValue(final int value) {
        return map.get(value);
    }
}
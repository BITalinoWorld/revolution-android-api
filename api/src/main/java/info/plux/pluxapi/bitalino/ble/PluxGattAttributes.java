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

import java.util.HashMap;

/**
 * This class includes the PLUX's GATT attributes
 */
public class PluxGattAttributes {
    private static HashMap<String, String> attributes   = new HashMap();
    public static String CLIENT_CHARACTERISTIC_CONFIG   = "00002902-0000-1000-8000-00805f9b34fb";
    public static String COMMANDS 	                    = "4051eb11-bf0a-4c74-8730-a48f4193fcea";
    public static String FRAMES 	                    = "40fdba6b-672e-47c4-808a-e529adff3633";
    public static String DataS                          = "c566488a-0882-4e1b-a6d0-0b717e652234";

    static {
        // App Services.
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put("00002a26-0000-1000-8000-00805f9b34fb", "Firmware Version");
        attributes.put(DataS , "Data Service");

        // App Characteristics.
        attributes.put(FRAMES, "Frames characteristic");
        attributes.put(COMMANDS, "Commands characteristic");

        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Name Service");
        attributes.put("00002a00-0000-1000-8000-00805f9b34fb", "Device Name");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}





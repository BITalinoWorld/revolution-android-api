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

public class BITalinoCRC {
    private static final String TAG = "BITalinoCRC";

    // CRC-4, poly = x^4 + x^1 + x^0, init = 0x00
    private static byte[] CRC4tab = { (byte) 0x00, (byte) 0x03, (byte) 0x06,
            (byte) 0x05, (byte) 0x0C, (byte) 0x0F, (byte) 0x0A, (byte) 0x09,
            (byte) 0x0B, (byte) 0x08, (byte) 0x0D, (byte) 0x0E, (byte) 0x07,
            (byte) 0x04, (byte) 0x01, (byte) 0x02 };

    public static byte getCRC4(byte[] data) {
        byte crc = (byte)0x00;

        for (int i = 0; i < data.length - 1; i++){
            //crc = CRC8tab[crc ^ data[i]];
            crc = (byte)((CRC4tab[crc] & 0xFF) ^ ((data[i] & 0xFF) >> 4));
            crc = (byte)((CRC4tab[crc] & 0xFF) ^ ((data[i] & 0xFF) & 0x0F));
        }

        //for last byte
        crc = (byte)((CRC4tab[crc] & 0xFF) ^ ((data[data.length - 1] & 0xFF) >> 4));
        crc = (byte)(CRC4tab[crc] & 0xFF);

        return crc;
    }
}
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

import android.annotation.TargetApi;
import android.os.Build;

import java.io.IOException;

public class BITalinoFrameDecoder {
    private static final String TAG = "BITalinoFrameDecoder";

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static BITalinoFrame decode(final String identifier, final byte[] buffer, final int[] analogChannels, final int totalBytes) throws IOException, BITalinoException {

        try {
            BITalinoFrame frame;
            final int j = (totalBytes - 1);

            //get frame CRC
            byte byteCRC = (byte)((buffer[j] & 0xFF) & 0x0F);

            //CRC4 is for the all packet, from the sequence_number until the last byte of byte_n
            byte[] arrayCRC = buffer;

            //test if the received CRC is equal to the one calculated
            if(Byte.compare(byteCRC, BITalinoCRC.getCRC4(arrayCRC)) == 0){
                frame = new BITalinoFrame(identifier);
                frame.setSequence(((buffer[j - 0] & 0xF0) >> 4) & 0xf);
                frame.setDigital(0, (buffer[j - 1] >> 7) & 0x01);
                frame.setDigital(1, (buffer[j - 1] >> 6) & 0x01);
                frame.setDigital(2, (buffer[j - 1] >> 5) & 0x01);
                frame.setDigital(3, (buffer[j - 1] >> 4) & 0x01);

                // parse buffer frame
                if (analogChannels.length >= 1) {
                    frame.setAnalog(analogChannels[0], (((buffer[j - 1] & 0xF) << 6) | ((buffer[j - 2] & 0XFC) >> 2)) & 0x3ff);
                }
                if (analogChannels.length >= 2) {
                    frame.setAnalog(analogChannels[1], (((buffer[j - 2] & 0x3) << 8) | (buffer[j - 3]) & 0xff) & 0x3ff);
                }
                if (analogChannels.length >= 3) {
                    frame.setAnalog(analogChannels[2], (((buffer[j - 4] & 0xff) << 2) | (((buffer[j - 5] & 0xc0) >> 6))) & 0x3ff);
                }
                if (analogChannels.length >= 4) {
                    frame.setAnalog(analogChannels[3], (((buffer[j - 5] & 0x3F) << 4) | ((buffer[j - 6] & 0xf0) >> 4)) & 0x3ff);
                }
                if (analogChannels.length >= 5) {
                    frame.setAnalog(analogChannels[4], (((buffer[j - 6] & 0x0F) << 2) | ((buffer[j - 7] & 0xc0) >> 6)) & 0x3f);
                }
                if (analogChannels.length >= 6) {
                    frame.setAnalog(analogChannels[5], (buffer[j - 7] & 0x3F));
                }
            } else {
                frame = new BITalinoFrame(identifier);
                frame.setSequence(-1);
            }
            return frame;
        } catch (Exception e) {
            throw new BITalinoException(BITalinoErrorTypes.DECODE_INVALID_DATA);
        }
    }
}
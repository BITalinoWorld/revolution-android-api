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

public class BITalinoStateDecoder {
    private static final String TAG = "BITalinoStateDecoder";

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static BITalinoState decode(final String identifier, final byte[] buffer, boolean isStateCorrect) throws IOException, BITalinoException {
        int totalBytes;
        if(isStateCorrect){
            totalBytes = 17;
        }
        else{
            totalBytes = 16;
        }

        try {
            BITalinoState state;
            final int j = (totalBytes - 1);

            //get frame CRC
            byte byteCRC = (byte)((buffer[j] & 0xFF) & 0x0F);

            //CRC4 is for the all packet, from the sequence_number until the last byte of byte_n
            byte[] arrayCRC = buffer;

            //test if the received CRC is equal to the one calculated
            if(Byte.compare(byteCRC, BITalinoCRC.getCRC4(arrayCRC)) == 0){
                state = new BITalinoState(identifier);
                if(isStateCorrect) {
                    state.setDigital(0, (buffer[j] >> 7) & 0x01);
                    state.setDigital(1, (buffer[j] >> 6) & 0x01);
                    state.setDigital(2, (buffer[j] >> 5) & 0x01);
                    state.setDigital(3, (buffer[j] >> 4) & 0x01);

                    state.setAnalogOutput((buffer[j - 1] & 0xFF));

                    state.setBatThreshold((buffer[j - 2] & 0xFF));

                    state.setBattery((buffer[j - 3] & 0xFF) << 8 | (buffer[j - 4] & 0xFF));

                    // parse buffer frame
                    state.setAnalog(0, ((buffer[j - 5] & 0xFF) << 8 | (buffer[j - 6] & 0xFF)));
                    state.setAnalog(1, ((buffer[j - 7] & 0xFF) << 8 | (buffer[j - 8] & 0xFF)));
                    state.setAnalog(2, ((buffer[j - 9] & 0xFF) << 8 | (buffer[j - 10] & 0xFF)));
                    state.setAnalog(3, ((buffer[j - 11] & 0xFF) << 8 | (buffer[j - 12] & 0xFF)));
                    state.setAnalog(4, ((buffer[j - 13] & 0xFF) << 8 | (buffer[j - 14] & 0xFF)));
                    state.setAnalog(5, ((buffer[j - 15] & 0xFF) << 8 | (buffer[j - 16] & 0xFF)));
                }
                else {
                    state.setDigital(0, (buffer[j] >> 7) & 0x01);
                    state.setDigital(1, (buffer[j] >> 6) & 0x01);
                    state.setDigital(2, (buffer[j] >> 5) & 0x01);
                    state.setDigital(3, (buffer[j] >> 4) & 0x01);

                    state.setBatThreshold((buffer[j - 1] & 0xFF));

                    state.setBattery((buffer[j - 2] & 0xFF) << 8 | (buffer[j - 3] & 0xFF));

                    // parse buffer frame
                    state.setAnalog(0, ((buffer[j - 4] & 0xFF) << 8 | (buffer[j - 5] & 0xFF)));
                    state.setAnalog(1, ((buffer[j - 6] & 0xFF) << 8 | (buffer[j - 7] & 0xFF)));
                    state.setAnalog(2, ((buffer[j - 8] & 0xFF) << 8 | (buffer[j - 9] & 0xFF)));
                    state.setAnalog(3, ((buffer[j - 10] & 0xFF) << 8 | (buffer[j - 11] & 0xFF)));
                    state.setAnalog(4, ((buffer[j - 12] & 0xFF) << 8 | (buffer[j - 13] & 0xFF)));
                    state.setAnalog(5, ((buffer[j - 14] & 0xFF) << 8 | (buffer[j - 15] & 0xFF)));
                }
            } else {
                state = new BITalinoState(identifier);
            }
            return state;
        } catch (Exception e) {
            throw new BITalinoException(BITalinoErrorTypes.DECODE_INVALID_DATA);
        }
    }
}
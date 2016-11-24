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

import java.util.Arrays;

public class CommandDecoder {
    private static final String TAG = "CommandDecoder";

    private byte[] incompleteByteArray;

    public enum CommandType{
        DATA_FRAMES     (0),
        VERSION         (1),
        STATE           (2);

        private int id;

        CommandType(final int id) {
            this.id = id;
        }

        public static CommandType fromFirstByte(final int id) {
            CommandType returnCommandType = null;
            for(CommandType firstByteElement: CommandType.values()){
                if(firstByteElement.id == id){
                    returnCommandType = firstByteElement;
                }
            }
            return returnCommandType;
        }

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public byte[][] parseReceivedData (CommandType commandType, byte[] buffer, int bytes, int totalBytes){
        byte[][] receivedData = new byte[1000][];
        boolean isVersionBufferComplete = false;
        int i = 0, j = 0, length;

        byte[] realBuffer = new byte[bytes];
        System.arraycopy(buffer, 0, realBuffer, 0, bytes);
        byte[] dataArray;

        if(incompleteByteArray != null){
            dataArray = new byte[incompleteByteArray.length + realBuffer.length];
            // copy incompleteByteArray to dataArray
            System.arraycopy(incompleteByteArray, 0, dataArray, 0, incompleteByteArray.length);
            // copy buffer to dataArray
            System.arraycopy(realBuffer, 0, dataArray, incompleteByteArray.length, realBuffer.length);
            incompleteByteArray = null;
        }
        else{
            dataArray = realBuffer;
        }

        while(i < dataArray.length){
            if(commandType == CommandType.DATA_FRAMES || commandType == CommandType.STATE){
                length = totalBytes;
            }
            else{ //CommandType.VERSION
                isVersionBufferComplete = (dataArray[dataArray.length - 1] == ("\n").getBytes()[0]);

                length = dataArray.length;
            }

            if(commandType != CommandType.VERSION) {
                if ((i + length) > dataArray.length) {
                    incompleteByteArray = Arrays.copyOfRange(dataArray, i, dataArray.length);
                } else {
                    receivedData[j] = Arrays.copyOfRange(dataArray, i, i + length);
                    incompleteByteArray = null;
                }
            }
            else{
                if(isVersionBufferComplete){
                    receivedData[j] = Arrays.copyOfRange(dataArray, i, i + length);
                    incompleteByteArray = null;
                }
                else{
                    incompleteByteArray = Arrays.copyOfRange(dataArray, i, dataArray.length);
                }
            }

            i += length;
            j++;
        }

        return receivedData;
    }

}
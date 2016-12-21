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

import info.plux.pluxapi.CommandArguments;
import info.plux.pluxapi.CommandProperties;
import info.plux.pluxapi.Device;

public enum BITalino implements Device<CommandProperties> {

    START {
        @Override
        public CommandProperties getCommand(CommandArguments arguments) {
            int[] bitalinoChannels = arguments.getAnalogChannels();
            int bit = 1;
            for (int channel : bitalinoChannels)
                bit = bit | 1 << (2 + channel);

            byte[] cmd = {(byte)bit};

            return new CommandProperties(cmd, 1);
        }
    },
    SET_FREQ {
        @Override
        public CommandProperties getCommand(CommandArguments arguments) {
            int bitalinoFreq = arguments.getSampleRate();
            byte[] freqCommand = new byte[1];
            switch(bitalinoFreq){
                case 1:
                    freqCommand[0] = (byte)0x03;
                    break;
                case 10:
                    freqCommand[0] = (byte)0x43;
                    break;
                case 100:
                    freqCommand[0] = (byte)0x83;
                    break;
                case 1000:
                    freqCommand[0] = (byte)0xC3;
                    break;
            }

            return new CommandProperties(freqCommand, 1);
        }
    },
    STOP {
        @Override
        public CommandProperties getCommand(CommandArguments arguments) {
            byte[] endCommand = {(byte)0x00};
            return new CommandProperties(endCommand, 1);
        }
    }, VERSION {
        @Override
        public CommandProperties getCommand(CommandArguments argument) {
            boolean isBLE = argument.isBLE();

            byte[] descriptionCommand = {(byte)0x07}; //BTH
            //byte[] descriptionCommand = {(byte)0x0F}; //BLE
            return new CommandProperties(descriptionCommand, 1);
        }
    }, TRIGGER{
        @Override
        public CommandProperties getCommand(CommandArguments arguments) {
            boolean isBITalino2 = arguments.isBITalino2();
            int[] bitalinoDigitalChannels = arguments.getDigitalChannels();
            byte data;

            if(isBITalino2){
                data = (byte) 0xB3;
            }
            else{
                data = (byte) 0x03;
            }

            int i = 0;
            for (int digitalChannel: bitalinoDigitalChannels) {
//                data |= (byte)(0x04 << i);
                data |= (byte)(digitalChannel<<(2+i));
                i++;
            }

            byte[] triggerCommand = new byte[]{data};
            return new CommandProperties(triggerCommand, triggerCommand.length);

        }
    }, STATE {
        @Override
        public CommandProperties getCommand(CommandArguments argument) {
            byte[] command = {(byte)0x0B}; //BTH

            return new CommandProperties(command, 1);
        }
    }, PWM {
        @Override
        public CommandProperties getCommand(CommandArguments argument) {
            byte[] command = {(byte) 0xA3};

            return new CommandProperties(command, 1);
        }
    }, BATTERY {
        @Override
        public CommandProperties getCommand(CommandArguments argument) {
            int batteryThreshold = argument.getBatteryThreshold();

            byte[] command = {(byte)(batteryThreshold << 2)};
            
            return new CommandProperties(command, 1);
        }
    }
}

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

/**
 * Defines several constants used between {@link .example.MainActivity} and the UI.
 */
public interface Constants {

    String ACTION_STATE_CHANGED             = "info.plux.pluxapi.ACTION_STATE_CHANGED";
    String ACTION_CONNECTED                 = "info.plux.pluxapi.ACTION_CONNECTED";
    String ACTION_DISCONNECTED              = "info.plux.pluxapi.ACTION_DISCONNECTED";
    String ACTION_GATT_SERVICES_DISCOVERED  = "info.plux.pluxapi.ACTION_GATT_SERVICES_DISCOVERED";
    String ACTION_DATA_AVAILABLE            = "info.plux.pluxapi.ACTION_DATA_AVAILABLE";
    String ACTION_DEVICE_READY              = "info.plux.pluxapi.ACTION_DEVICE";
    String ACTION_MESSAGE_SCAN              = "info.plux.pluxapi.ACTION_MESSAGE_SCAN";
    String ACTION_EVENT_AVAILABLE           = "info.plux.pluxapi.ACTION_EVENT_AVAILABLE";
    String ACTION_COMMAND_REPLY             = "info.plux.pluxapi.ACTION_COMMAND_REPLY";
    String ACTION_LOG_AVAILABLE             = "info.plux.pluxapi.ACTION_LOG_AVAILABLE";

    String UPDATE_TIME                      = "info.plux.pluxapi.UPDATE_TIME";

    String EXTRA_STATE_CHANGED              = "info.plux.pluxapi.EXTRA_STATE_CHANGED";
    String EXTRA_DEVICE_SCAN                = "info.plux.pluxapi.EXTRA_DEVICE_SCAN";
    String EXTRA_DEVICE_RSSI                = "info.plux.pluxapi.EXTRA_DEVICE_RSSI";
    String EXTRA_DESCRIPTION                = "info.plux.pluxapi.EXTRA_DESCRIPTION";
    String EXTRA_DATA                       = "info.plux.pluxapi.EXTRA_DATA";
    String EXTRA_EVENT                      = "info.plux.pluxapi.EXTRA_EVENT";
    String EXTRA_COMMAND_REPLY              = "info.plux.pluxapi.EXTRA_COMMAND_REPLY";
    String EXTRA_LOG                        = "info.plux.pluxapi.EXTRA_LOG";
    String EXTRA_TIME                       = "info.plux.pluxapi.EXTRA_TIME";

    String PLUX_DEVICE                      = "info.plux.pluxapi.PLUX_DEVICE";

    String BATTERY_EVENT                    = "info.plux.pluxapi.BATTERY_EVENT";
    String ON_BODY_EVENT                    = "info.plux.pluxapi.ON_BODY_EVENT";
    String DISCONNECT_EVENT                 = "info.plux.pluxapi.DISCONNECT_EVENT";
    String IDENTIFIER                       = "info.plux.pluxapi.IDENTIFIER";

    int MESSAGE_STATE_CHANGE         = 0;
    int MESSAGE_DEVICE_SELECTED      = 1;
    int MESSAGE_DATA_PACKAGE         = 2;
    int MESSAGE_NO_DATA_STREAMING    = 3;

    enum States {
        NO_CONNECTION(0),
        LISTEN(1),
        CONNECTING(2),
        CONNECTED(3),
        ACQUISITION_TRYING(4),
        ACQUISITION_OK(5),
        ACQUISITION_STOPPING(6),
        DISCONNECTED(7),
        ENDED(8);

        private final int id;

        States(final int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        public static States getStates(int id) {
            States statesElement = null;
            for (States state : values()) {
                if (state.id == id) {
                    statesElement = state;
                }
            }
            return statesElement;
        }
    }



}

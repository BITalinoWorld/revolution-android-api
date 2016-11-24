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

public enum Communication {
    UNKNOWN (0),
    BTH     (1), //classical
    BLE     (2),
    DUAL    (3);

    private int id;

    Communication(int id){
        this.id = id;
    }

    public static Communication getById(final int id){
        for(final Communication item: values()){
            if(item.id == id){
                return item;
            }
        }
        throw new IllegalArgumentException("Unable to find the type of BTH communication: " + id);
    }
}

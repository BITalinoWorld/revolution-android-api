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

public class BITalinoException extends Exception {

    private final int code;

    public BITalinoException(final BITalinoErrorTypes errorType) {
        super(errorType.getDescription());
        code = errorType.getValue();
    }

    public int getCode() {
        return code;
    }

}
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

public interface Device<K> {

    K getCommand(CommandArguments arguments);

}

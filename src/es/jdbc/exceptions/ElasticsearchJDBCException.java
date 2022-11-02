/*
 * Copyright (C) 2018 The elasticsearch-jdbc-wrapper Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 *                  ___====-_  _-====___
 *            _--^^^#####//      \\#####^^^--_
 *         _-^##########// (    ) \\##########^-_
 *        -############//  |\^^/|  \\############-
 *      _/############//   (@::@)   \\############\_
 *     /#############((     \\//     ))#############\
 *    -###############\\    (oo)    //###############-
 *   -#################\\  / VV \  //#################-
 *  -###################\\/      \//###################-
 * _#/|##########/\######(   /\   )######/\##########|\#_
 * |/ |#/\#/\#/\/  \#/\##\  |  |  /##/\#/  \/\#/\#/\#| \|
 * `  |/  V  V  `   V  \#\| |  | |/#/  V   '  V  V  \|  '
 *    `   `  `      `   / | |  | | \   '      '  '   '
 *                     (  | |  | |  )
 *                    __\ | |  | | /__
 *                   (vvv(VVV)(VVV)vvv)
 *  Code is far away from bug with the dragon's protection.
 */
package es.jdbc.exceptions;

/**
 * This exception is raised if there are any issues that occur during module execution.
 * 
 * <p>This exception is a {@link RuntimeException} because it is exposed to the client.  Using a
 * {@link RuntimeException} avoids bad coding practices on the client side where they catch the
 * exception and do nothing.
 * 
 * @author Ming Zhu
 * 
 */
public class ElasticsearchJDBCException extends RuntimeException {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 226985045599530380L;
    
    /**
     * Constructor.
     * @param message
     */
    public ElasticsearchJDBCException(final String message) {
        super(message);
    }
    
    /**
     * Constructor.
     * @param ex
     */
    public ElasticsearchJDBCException(final Throwable ex) {
        super(ex);
    }
}

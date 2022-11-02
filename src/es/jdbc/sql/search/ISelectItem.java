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
package es.jdbc.sql.search;

public interface ISelectItem {
    /**
     * 名称をセット.<br>
     * ソースの場合はソース名称、関数の場合は関数名称とする.
     * @param name 名称
     */
    public void setName(String name);
    /**
     * 名称を取得.<br>
     * ソースの場合はソース名称、関数の場合は関数名称とする.
     * @return 名称
     */
    public String getName();
    /**
     * 対象タイプ名称をセット.
     * @param type 対象タイプ名称
     */
    public void setType(String type);
    /**
     * 対象タイプ名称を取得.
     * @return 対象タイプ名称
     */
    public String getType();
    /**
     * @return the isDistinct
     */
    public boolean isDistinct();
    /**
     * @param isDistinct the isDistinct to set
     */
    public void setDistinct(boolean isDistinct);
    /**
     * 関数であるかを取得.
     * @return 関数であるか
     */
    public boolean isFunction();
}

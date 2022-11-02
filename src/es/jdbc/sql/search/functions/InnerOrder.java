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
package es.jdbc.sql.search.functions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.elasticsearch.search.aggregations.BucketOrder;
//import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.sort.SortOrder;

import es.jdbc.sql.search.ISelectItem;

public class InnerOrder extends AbstractCommonFunction {
    /**
     * Aggregation用Orderリストを作成.
     * @return Aggregation用Orderリスト
     */
    public List<BucketOrder> createOrderList() {
        List<BucketOrder> retValue = new ArrayList<BucketOrder>();
        
        //Orderパラメーター
        for (ISelectItem orderParam : this.parameters) {
            if (orderParam instanceof OrderItem) {
                OrderItem orderItem = (OrderItem) orderParam;
                retValue.add(orderItem.createOrder());
            }
        }
        
        return retValue;
    }
    /**
     * 検索Query用Sortマップを作成.
     * @return 検索Query用Sortマップ
     */
    public LinkedHashMap<String, SortOrder> createSortMap() {
        LinkedHashMap<String, SortOrder> retValue = new LinkedHashMap<String, SortOrder>();
        //Orderパラメーター
        for (ISelectItem orderParam : this.parameters) {
            if (orderParam instanceof OrderItem) {
                OrderItem orderItem = (OrderItem) orderParam;
                retValue.put(orderItem.getFieldName(), orderItem.getSortOrder());
            }
        }
        return retValue;
    }
    /**
     * コンストラクター.
     */
    public InnerOrder() {
        this.parameters = new ArrayList<ISelectItem>();
    }
}

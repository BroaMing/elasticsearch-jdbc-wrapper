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

import org.elasticsearch.search.aggregations.BucketOrder;
//import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.sort.SortOrder;

import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.sources.IValue;

public class OrderItem extends AbstractCommonFunction {
    /** ソート情報. */
    public static enum SORT_ORDER {
        ASC("ASC", SortOrder.ASC),
        DESC("DESC", SortOrder.DESC);
        
        private String key;
        private SortOrder order;
        private SORT_ORDER(String key, SortOrder order) {
            this.key = key;
            this.order = order;
        }
        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }
        /**
         * @param key the key to set
         */
        public void setKey(String key) {
            this.key = key;
        }
        /**
         * @return the order
         */
        public SortOrder getOrder() {
            return order;
        }
        /**
         * @param order the order to set
         */
        public void setOrder(SortOrder order) {
            this.order = order;
        }
        
        public static SORT_ORDER findSortOrder(Object sortName) {
            SORT_ORDER retValue = SORT_ORDER.ASC;
            for (SORT_ORDER sort : values()) {
                if (sort.key.equals(sortName)) {
                    retValue = sort;
                    break;
                }
            }
            return retValue;
        }
    }
    /**
     * 項目名称を取得.
     * @return 項目名称
     */
    public String getFieldName() {
        return this.parameters.get(0).getName();
    }
    /**
     * Order情報を取得.
     * @return Order情報
     */
    public BucketOrder createOrder() {
        String fieldName = this.parameters.get(0).getName();
        SORT_ORDER sort = SORT_ORDER.ASC;
        if (this.parameters.size() > 1) {
            ISelectItem sortItem = this.parameters.get(1);
            if (sortItem instanceof IValue) {
                IValue sortValue = (IValue) this.parameters.get(1);
                sort = SORT_ORDER.findSortOrder(sortValue.getValue());
            } else {
                sort = SORT_ORDER.findSortOrder(sortItem.getName());
            }
        }
        
        return BucketOrder.aggregation(fieldName, SortOrder.ASC.equals(sort.getOrder()));
    }
    /**
     * ソート情報を取得.
     * @return ソート情報
     */
    public SortOrder getSortOrder() {
        SORT_ORDER retValue = SORT_ORDER.ASC;
        if (this.parameters.size() > 1) {
            ISelectItem sortItem = this.parameters.get(1);
            if (sortItem instanceof IValue) {
                IValue sortValue = (IValue) this.parameters.get(1);
                retValue = SORT_ORDER.findSortOrder(sortValue.getValue());
            } else {
                retValue = SORT_ORDER.findSortOrder(sortItem.getName());
            }
        }
        return retValue.getOrder();
    }
    /**
     * コンストラクター.
     */
    public OrderItem() {
        this.parameters = new ArrayList<ISelectItem>();
    }
}

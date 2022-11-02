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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
//import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
//import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.sources.ISource;
import es.jdbc.utils.CommonParams;
import es.jdbc.utils.VariablesFactory;

public class OverPartition extends AbstractCommonFunction {
    /**
     * コンストラクター.
     */
    public OverPartition() {
        this.parameters = new ArrayList<ISelectItem>();
    }
    
    /**
     * 
     * @param sqlNode
     * @param nodeName
     * @param nodeValue
     * @return
     */
    private Map<Object, Object> setSqlNodes(Map<Object, Object> sqlNode, String nodeName, Object nodeValue) {
        if (sqlNode == null) {
            sqlNode = new HashMap<Object, Object>();
        }
        Map<Object, Object> retValue = new HashMap<Object, Object>();
        retValue.put(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.COLUMN_TYPE_AGGS.getKey(), nodeValue);
        sqlNode.put(nodeName, retValue);
        return retValue;
    }
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#buildAggregation()
     */
    @SuppressWarnings("unchecked")
    @Override
    public AbstractAggregationBuilder<?> buildAggregation() {
        /** OVER_PARTITION("GROUP BY 1", "GROUP BY 2", "GROUP BY 3"...) */
        // 戻り値
        AbstractAggregationBuilder<?> retValue = null;
        AbstractAggregationBuilder<?> preBuilder = null;
        if (this.parameters.size() > 0) {
            //SQL Nodes情報
            Map<Object, Object> sqlNodeMap = VariablesFactory.VARIABLES_STORE.SQL_NODES_STORE.get(Map.class);
            boolean sqlNodeApproach = sqlNodeMap != null;
            for (ISelectItem param : this.parameters) {
                AbstractAggregationBuilder<?> currentValue = null;
                String groupByName = param.getName();
                //SQL Nodes情報
                if (sqlNodeApproach && sqlNodeMap.containsKey(groupByName)) {
                    Map<Object, Object> sqlNode = (Map<Object, Object>) sqlNodeMap.get(groupByName);
                    currentValue = (AbstractAggregationBuilder<?>) sqlNode.get(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.COLUMN_TYPE_AGGS.getKey());
                    this.isSqlNodeUsed = true;
                    sqlNodeMap = sqlNode;
                } else {
                    if (param instanceof ISource) {
                        currentValue = AggregationBuilders.terms(groupByName).field(groupByName).size(Integer.MAX_VALUE);
                        sqlNodeMap = this.setSqlNodes(sqlNodeMap, groupByName, currentValue);
                    } else if (param instanceof IFunction) {
                        IFunction aggsFunction = (IFunction) param;
                        if (param instanceof InnerOrder) {
                            if (preBuilder != null && preBuilder instanceof TermsAggregationBuilder) {
                                InnerOrder innerOrder = (InnerOrder) param;
                                //TermsBuilder termsNode = ((TermsBuilder) preBuilder);
                                TermsAggregationBuilder termsNode = ((TermsAggregationBuilder) preBuilder);
                                //Orderリスト
                                List<BucketOrder> orderList = innerOrder.createOrderList();
                                termsNode.order(BucketOrder.compound(orderList));
                            }
                        } else if (param instanceof InnerSize) {
                            if (preBuilder != null && preBuilder instanceof TermsAggregationBuilder) {
                                InnerSize innerSize = (InnerSize) param;
                                //TermsBuilder termsNode = ((TermsBuilder) preBuilder);
                                TermsAggregationBuilder termsNode = ((TermsAggregationBuilder) preBuilder);
                                termsNode.size(innerSize.createSize());
                            }
                        } else {
                            currentValue = aggsFunction.buildAggregation();
                            sqlNodeMap = this.setSqlNodes(sqlNodeMap, groupByName, currentValue);
                        }
                    }
                    sqlNodeApproach = false;
                }
                
                if (currentValue != null) {
                    if (retValue == null) {
                        retValue = currentValue;
                    } else if (!sqlNodeApproach) {
                        ((AggregationBuilder) preBuilder).subAggregation(currentValue);
                    }
                    preBuilder = currentValue;
                }
            }
        }
        this.baseAggregation = preBuilder;
        return retValue;
    }
}

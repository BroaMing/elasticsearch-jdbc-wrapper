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

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;

import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.sources.ISource;
import es.jdbc.utils.CommonParams;
import es.jdbc.utils.CommonUtils;

public class Min extends AbstractCommonFunction {
    /**
     * コンストラクター.
     */
    public Min() {
        this.parameters = new ArrayList<ISelectItem>();
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#fillResult(java.util.List, java.util.List)
     */
    @Override
    public void fillResult(final List<Map<String, Object>> aggsContainer, final List<Aggregation> aggsList) {
        if (aggsContainer == null || aggsList == null) {
            return;
        }
        //全Aggsをループ
        for (Aggregation aggInfo : aggsList) {
            //
            fillResult(aggsContainer, new HashMap<String, Object>(), aggInfo);
        }
    }
    /**
     * Aggregation結果をコンテナーへ填充する.
     * @param aggsContainer コンテナー
     * @param resultMap 結果マップ
     * @param aggregation Aggregation結果
     */
    private void fillResult(final List<Map<String, Object>> aggsContainer, final Map<String, Object> resultMap, final Aggregation aggregation) {
        if (aggregation == null) { return; }
        
        if (aggregation instanceof Terms) {
            Terms terms = ((Terms) aggregation);
            String termName = CommonUtils.findOriginalHashKey(terms.getName(), CommonParams.AGGREGATION_KEYS_HASH_KEY_PATTERN);
            
            List<? extends Bucket> bucketList = terms.getBuckets();
            if (bucketList != null && bucketList.size() > 0) {
                for (Bucket bucket : bucketList) {
                    Map<String, Object> bucketMap = new HashMap<String, Object>(resultMap);
                    bucketMap.put(termName, bucket.getKey());
                    fillResult(aggsContainer, bucketMap, bucket.getAggregations());
                }
            }
        } else if (aggregation instanceof Filter) {
            Filter filter = (Filter) aggregation;
            fillResult(aggsContainer, resultMap, filter.getAggregations());
        } else if (aggregation instanceof org.elasticsearch.search.aggregations.bucket.nested.Nested) {
            org.elasticsearch.search.aggregations.bucket.nested.Nested nested = (org.elasticsearch.search.aggregations.bucket.nested.Nested) aggregation;
            fillResult(aggsContainer, resultMap, nested.getAggregations());
        } else if (aggregation instanceof org.elasticsearch.search.aggregations.metrics.min.Min) {
            org.elasticsearch.search.aggregations.metrics.min.Min value = 
                    (org.elasticsearch.search.aggregations.metrics.min.Min) aggregation;
            if (StringUtils.equals(this.getName(), value.getName())) {
                resultMap.put(value.getName(), Double.isInfinite(value.getValue())?null:value.getValueAsString());
                resultMap.put(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.COLUMN_TYPE_AGGS.getKey(), value.getName());
                resultMap.put(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.TYPE.getKey(), this.getType());
                aggsContainer.add(resultMap);
            }
        }
    }
    
    /**
     * Aggregation結果をコンテナーへ填充する.
     * @param aggsContainer コンテナー
     * @param resultMap 結果マップ
     * @param aggregation Aggregation結果リスト
     */
    private void fillResult(final List<Map<String, Object>> aggsContainer, final Map<String, Object> resultMap, final Aggregations aggregations) {
        if (aggregations != null) {
            List<Aggregation> subAggList = aggregations.asList();
            if (subAggList != null && subAggList.size() > 0) {
                for (Aggregation subAgg : subAggList) {
                    fillResult(aggsContainer, resultMap, subAgg);
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#buildAggregation()
     */
    @Override
    public AbstractAggregationBuilder<?> buildAggregation() {
        // 戻り値
        AbstractAggregationBuilder<?> retValue = null;
        AbstractAggregationBuilder<?> preBuilder = null;
        if (parameters.size() > 0) {
            for (ISelectItem param : parameters) {
                if (param instanceof ISource) {
                    AbstractAggregationBuilder<?> aggs = AggregationBuilders.min(this.getName()).field(param.getName());
                    if (retValue == null) {
                        retValue = aggs;
                    } else {
                        ((AggregationBuilder) preBuilder).subAggregation(aggs);
                    }
                } else if (param instanceof IFunction) {
                    if (param instanceof Nested) {
                        if (this.isNestedExists) {
                            continue;
                        } else {
                            this.isNestedExists = true;
                        }
                    }
                    IFunction funcParam = ((IFunction) param);
                    AbstractAggregationBuilder<?> funcAggression = funcParam.buildAggregation();
                    
                    if (retValue == null) {
                        retValue = funcAggression;
                    } else {
                        ((AggregationBuilder) preBuilder).subAggregation(funcAggression);
                    }
                    if (funcParam.findBaseAggregation() == null) {
                        preBuilder = funcAggression;
                    } else {
                        preBuilder = funcParam.findBaseAggregation();
                    }
                    //SQL Nodes情報
                    this.isSqlNodeUsed |= funcParam.isSqlNodeUsed();
                }
            }
        }
        return retValue;
    }
}

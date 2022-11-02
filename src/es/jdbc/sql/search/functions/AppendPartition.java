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
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;

import es.jdbc.sql.search.ISelectItem;
import es.jdbc.utils.CommonParams;
import es.jdbc.utils.CommonUtils;

public class AppendPartition extends AbstractCommonFunction {
    /**
     * コンストラクター.
     */
    public AppendPartition() {
        this.parameters = new ArrayList<ISelectItem>();
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem#addFunctionParameter(jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem)
     */
    @Override
    public void addParameter(ISelectItem param) {
        super.addParameter(param);
        if (this.parameters.size() == 2) {
            this.setName(this.parameters.get(1).getName());
        }
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
            
            //全Bucketをループして処理
            List<? extends Bucket> bucketList = terms.getBuckets();
            if (bucketList != null && bucketList.size() > 0) {
                for (Bucket bucket : bucketList) {
                    Map<String, Object> bucketMap = new HashMap<String, Object>(resultMap);
                    bucketMap.put(termName, bucket.getKey());
                    if (this.getName().equals(termName)) {
                        bucketMap.put(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.COLUMN_TYPE_AGGS.getKey(), this.getName());
                        bucketMap.put(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.TYPE.getKey(), this.getType());
                        aggsContainer.add(bucketMap);
                    } else {
                        fillResult(aggsContainer, bucketMap, bucket.getAggregations());
                    }
                }
            } else if (this.getName().equals(termName)) {
                Map<String, Object> bucketMap = new HashMap<String, Object>(resultMap);
                bucketMap.put(termName, null);
                bucketMap.put(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.COLUMN_TYPE_AGGS.getKey(), this.getName());
                bucketMap.put(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.TYPE.getKey(), this.getType());
                aggsContainer.add(bucketMap);
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
        List<Aggregation> subAggList = null;
        if (aggregations != null && (subAggList = aggregations.asList()) != null && subAggList.size() > 0) {
            for (Aggregation subAgg : subAggList) {
                fillResult(aggsContainer, resultMap, subAgg);
            }
        }
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#buildAggregation()
     */
    @Override
    public AbstractAggregationBuilder<?> buildAggregation() {
        /** APPEND_PARTITION("NAME", "ALIAS NAME") */
        // 戻り値
        String fieldName = this.parameters.get(0).getName();
        String termsName = null;
        if (this.parameters.size() > 1) {
            termsName = this.getName();
        } else {
            termsName = String.format(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.HASH_KEY_FORMAT.getKey(),
                    fieldName, this.hashCode());
        }
        return this.baseAggregation = AggregationBuilders.terms(termsName).field(fieldName).size(Integer.MAX_VALUE);
    }
}

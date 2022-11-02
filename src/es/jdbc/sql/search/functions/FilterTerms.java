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
import java.util.List;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.sources.IValue;
import es.jdbc.utils.CommonParams;

public class FilterTerms extends AbstractCommonFunction {
    /**
     * コンストラクター.
     */
    public FilterTerms() {
        this.parameters = new ArrayList<ISelectItem>();
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#buildAggregation()
     */
    @Override
    public AbstractAggregationBuilder<?> buildAggregation() {
        /** FILTER_TERMS("カラム名称", "パラメーター1", "パラメーター2"...) */
        //カラム名称
        ISelectItem pathItem = this.parameters.get(0);
        String fieldName = pathItem.getName();
        //フィルター名称
        String filterName = this.getName();
        if (IFunction.FUNCTION_LIST.FILTER_TERMS.getName().equalsIgnoreCase(this.getName())) {
            filterName = String.format(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.HASH_KEY_FORMAT.getKey(), 
                    this.getName(), this.hashCode());
        }
        //パラメーター
        List<Object> paramList = new ArrayList<Object>();
        for (int i=1; i<this.parameters.size(); i++) {
            IValue value = (IValue) this.parameters.get(i);
            paramList.add(value.getValue());
        }
        
        //return this.baseAggregation = AggregationBuilders.filter(filterName).filter(QueryBuilders.termsQuery(fieldName, paramList.toArray()));
        return this.baseAggregation = AggregationBuilders.filter(filterName, QueryBuilders.termsQuery(fieldName, paramList.toArray()));
    }
}

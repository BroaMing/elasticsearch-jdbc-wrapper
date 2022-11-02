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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.sources.IValue;
import es.jdbc.utils.CommonParams;

public class Contains extends AbstractCommonFunction {
    
    private static final String GROUPING_FUZZY_VALUE_REGEX = " *Fuzzy\\(\\{(.*?)\\}\\) *";
    private static final Pattern GROUPING_FUZZY_VALUE_REGEX_PATTERN = Pattern.compile(GROUPING_FUZZY_VALUE_REGEX);
    
    /**
     * コンストラクター.
     */
    public Contains() {
        this.parameters = new ArrayList<ISelectItem>();
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#buildAggregation()
     */
    @Override
    public AbstractAggregationBuilder<?> buildAggregation() {
        /** CONTAINS(カラム名称1, カラム名称2, ..., 検索テキスト). */
        List<ISelectItem> columns = new ArrayList<ISelectItem>();
        int endIndex = this.parameters.size() - 1;
        for (int i=0; i<endIndex; i++) {
            columns.add(this.parameters.get(i));
        }
        IValue text = (IValue) this.parameters.get(endIndex);
        String value = StringUtils.defaultString((String) text.getValue());
        //Fuzzy式を書き換え
        value = rewriteFuzzy(value);
        
        //フィルター名称
        String filterName = this.getName();
        if (IFunction.FUNCTION_LIST.CONTAINS.getName().equalsIgnoreCase(this.getName())) {
            filterName = String.format(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.HASH_KEY_FORMAT.getKey(), 
                    this.getName(), this.hashCode());
        }
        //QueryStringQuery作成
        QueryStringQueryBuilder queryStringBuilder = QueryBuilders.queryStringQuery(value);
        //queryStringBuilder.
        for (ISelectItem column : columns) {
            queryStringBuilder.field(column.getName());
        }
        //return AggregationBuilders.filter(filterName).filter(queryStringBuilder);
        return AggregationBuilders.filter(filterName, queryStringBuilder);
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#buildQuery()
     */
    @Override
    public QueryBuilder buildQuery() {
        /** CONTAINS(カラム名称1, カラム名称2, ..., 検索テキスト, 点数フラグ). 点数フラグ(任意)：0=点数取得しない(デフォルト) 1=点数取得する */
        List<ISelectItem> columns = new ArrayList<ISelectItem>();
        int textIndex = this.parameters.size() - 2;
        int scoreFlgIndex = this.parameters.size() - 1;
        if (!(this.parameters.get(textIndex) instanceof IValue)) {
            textIndex++;
            scoreFlgIndex = -1;
        }
        for (int i=0; i<textIndex; i++) {
            columns.add(this.parameters.get(i));
        }
        
        //検索テキスト取得
        IValue text = (IValue) this.parameters.get(textIndex);
        String value = StringUtils.defaultString((String) text.getValue());
        //Fuzzy式を書き換え
        value = rewriteFuzzy(value);
        
        //Scoreは「1:取得する」か、「0:取得しない」か
        Long scoreFlg = 0L;
        if (scoreFlgIndex != -1) {
            IValue scoreFlgValue = (IValue) this.parameters.get(scoreFlgIndex);
            scoreFlg = (Long) scoreFlgValue.getValue();
        }
        
        //QueryStringQuery作成
        QueryStringQueryBuilder queryStringBuilder = QueryBuilders.queryStringQuery(value);
        for (ISelectItem column : columns) {
            queryStringBuilder.field(column.getName());
        }
        
        //Score取得フラグ
        this.isScoreQuery = scoreFlg == 1L;
        
        //Query作成
        //return QueryBuilders.boolQuery().must(queryStringBuilder);
        return queryStringBuilder;
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#subQueryContainer()
     */
    @Override
    public BoolQueryBuilder subQueryContainer() {
        return this.subQueryContainer;
    }
    
    protected String rewriteFuzzy(final String value) {
        String retValue = value;
        if (StringUtils.isBlank(retValue)) {
            return retValue;
        }
        
        Matcher matcher = GROUPING_FUZZY_VALUE_REGEX_PATTERN.matcher(value);
        while (matcher.find()) {
            String fuzzyExpression = matcher.group();
            String fuzzyValue = " " + matcher.group(1) + " ";
            retValue = retValue.replace(fuzzyExpression, fuzzyValue);
        }
        return retValue;
    }
}

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

import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.sources.IValue;

import org.elasticsearch.index.query.Operator;

public class ContainsMatch extends AbstractCommonFunction {
    
    //private static final String GROUPING_FUZZY_VALUE_REGEX = " *\\(? *Fuzzy\\(\\{(.*?)\\}\\) *(OR|AND)? *\\)? *(OR|AND)?";
    private static final String GROUPING_FUZZY_VALUE_REGEX = " *\\(? *Fuzzy\\(\\{(.*?)\\}\\) *(OR)? *\\)? *(AND)?";
    private static final Pattern GROUPING_FUZZY_VALUE_REGEX_PATTERN = Pattern.compile(GROUPING_FUZZY_VALUE_REGEX);
    
    /**
     * コンストラクター.
     */
    public ContainsMatch() {
        this.parameters = new ArrayList<ISelectItem>();
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#buildQuery()
     */
    @Override
    public QueryBuilder buildQuery() {
        /** CONTAINS(カラム名称1, カラム名称2, ..., 検索テキスト, 点数フラグ).点数フラグ(任意)：0=点数取得しない(デフォルト) 1=点数取得する */
        List<String> columns = new ArrayList<String>();
        //検索テキストIndex
        int textIndex = this.parameters.size() - 2;
        //点数フラグIndex
        int scoreFlgIndex = this.parameters.size() - 1;
        //点数フラグが存在しない場合、検索テキストIndexを+1
        if (!(this.parameters.get(textIndex) instanceof IValue)) {
            textIndex = scoreFlgIndex;
            scoreFlgIndex = -1;
        }
        //カラム名称リストを作成
        for (int i=0; i<textIndex; i++) {
            columns.add(this.parameters.get(i).getName());
        }
        
        //Scoreは「1:取得する」か、「0:取得しない」か
        Long scoreFlg = 0L;
        if (scoreFlgIndex != -1) {
            //点数フラグは有効である場合、点数フラグ値を取得
            IValue scoreFlgValue = (IValue) this.parameters.get(scoreFlgIndex);
            scoreFlg = (Long) scoreFlgValue.getValue();
        }
        //点数フラグは「1:有効」である場合は、スコア検索をTrueとする
        this.isScoreQuery = scoreFlg == 1L;
        
        //検索テキスト取得
        IValue text = (IValue) this.parameters.get(textIndex);
        String value = StringUtils.defaultString((String) text.getValue());
        //Fuzzy式を分析
        List<List<String>> fuzzyList = analyzeFuzzyValue(value);
        
        //Query作成
        BoolQueryBuilder queryRoot = QueryBuilders.boolQuery();
        for (List<String> subFuzzyQuery : fuzzyList) {
            //BoolQueryBuilder subQueryRoot = QueryBuilders.boolQuery();
            for (String fuzzyQuery : subFuzzyQuery) {
                //subQueryRoot.should(QueryBuilders.multiMatchQuery(fuzzyQuery, columns.toArray(new String[]{})).operator(Operator.AND));
                queryRoot.should(QueryBuilders.multiMatchQuery(fuzzyQuery, columns.toArray(new String[]{})).operator(Operator.AND));
            }
            //queryRoot.must(subQueryRoot);
        }
        return queryRoot;
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#subQueryContainer()
     */
    @Override
    public BoolQueryBuilder subQueryContainer() {
        return this.subQueryContainer;
    }
    /**
     * Fuzzy式を分析.
     * @param value 分析対象値
     * @return 分析結果.
     */
    protected List<List<String>> analyzeFuzzyValue(final String value) {
        List<List<String>> retValue = new ArrayList<List<String>>();
        
        List<String> orList = new ArrayList<String>();
        retValue.add(orList);
        Matcher matcher = GROUPING_FUZZY_VALUE_REGEX_PATTERN.matcher(value);
        while (matcher.find()) {
            String keyValue = matcher.group(1);
            //String orMark = matcher.group(2);
            String andMark = matcher.group(3);
            //重複を除外とする
            if (!orList.contains(keyValue)) {
                orList.add(keyValue);
            }
            if (StringUtils.isNotBlank(andMark)) {
                orList = new ArrayList<String>();
                retValue.add(orList);
            }
        }
        
        return retValue;
    }
}

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
package es.jdbc.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import es.jdbc.exceptions.ElasticsearchJDBCException;
import es.jdbc.sql.analyzers.SelectAnalyzer;
import es.jdbc.sql.beans.AnalyzerPolicy;
import es.jdbc.sql.beans.SQLElements;
import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.functions.IFunction;
import es.jdbc.sql.search.functions.Nested;
import es.jdbc.sql.search.sources.ISource;
import es.jdbc.utils.CommonParams;
import es.jdbc.utils.VariablesFactory;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.WithItem;

public class SearchSQLAnalyzer {

    /**
     * SQL Text.
     */
    protected String sqlText;
    /**
     * Parameter List of SQL.
     */
    protected List<Object> paramList;
    /**
     * analyzerPolicy.
     */
    protected AnalyzerPolicy analyzerPolicy;
    
    public SearchSQLAnalyzer(final String sqlText, final List<Object> paramList, final AnalyzerPolicy analyzerPolicy) {
        this.sqlText = sqlText;
        this.paramList = paramList;
        this.analyzerPolicy = analyzerPolicy;
    }
    
    /**
     * Select自動変換処理.
     * @param statement Select
     * @param paramList パラメーターリスト
     * @throws Exception 想定外異常
     */
    private SQLInfo executeConvert(final Select statement, final List<Object> paramList) throws Exception {
        // 1、WithList処理
        // With文Where解析結果DSLオブジェクト
        SQLInfo withSqlInfo = null;;
        if (!analyzerPolicy.isIgnoreWithClause()) {
            // WithList情報取得
            List<WithItem> withItemList = statement.getWithItemsList();
            if (withItemList != null && withItemList.size() > 0) {
                // With検索が存在する場合、ElasticsearchSelectVisitorにてWith文を解析
                SelectAnalyzer withSelectVisitor = new SelectAnalyzer(SelectAnalyzer.PROCESS_MODE.WITH_CLAUSE, paramList, analyzerPolicy);
                // 全With解析
                for (WithItem with : withItemList) {
                    with.accept(withSelectVisitor);
                }
                withSqlInfo = withSelectVisitor.getAnalyticSqlInfo();
            }
        }

        // 2、メインSelect処理
        // SelectVisitor作成
        SelectAnalyzer selectVisitor = new SelectAnalyzer(SelectAnalyzer.PROCESS_MODE.NORMAL_CLAUSE, paramList, analyzerPolicy);
        // 追加検索条件ありの場合
        if (withSqlInfo != null) {
            // 追加検索条件を設定
            selectVisitor.setAdditionalQuery(withSqlInfo.getQueryClauses());
        }
        
        // メインSelect分析処理を実行
        SelectBody selectBody = statement.getSelectBody();
        selectBody.accept(selectVisitor);
        
        return selectVisitor.getAnalyticSqlInfo();
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
    /**
     * 検索リクエスト作成.
     * @param sqlInfo NoSQL検索条件
     * @param sourceInfos 抽出項目リスト(Source)
     * @param functionInfos 集計情報リスト(Aggregation)
     * @return
     */
    @SuppressWarnings("unchecked")
    private void buildSearchRequest(SQLElements sqlElement) {
        SQLInfo sqlInfo = sqlElement.getSqlInfo();
        List<String> sources = sqlElement.getSources();
        Map<String, IFunction> functions = sqlElement.getFunctions();
        List<ISelectItem> groupByItems = sqlInfo.getGroupByItems();
        
        SearchRequest searchRequest = new SearchRequest().searchType(analyzerPolicy.getSearchType());
        sqlElement.setRequest(searchRequest);
        // Set Search Target Index
        //searchRequest.indices(sqlInfo.getTableName());
        if (StringUtils.isNoneBlank(analyzerPolicy.getMainIndex())) {
            searchRequest.indices(analyzerPolicy.getMainIndex());
        } else {
            searchRequest.indices(sqlInfo.getTableName());
        }

        // SourceBuilder
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                //.from(analyzerPolicy.getFetchSize()>0?analyzerPolicy.getFromPosition():0)
                .from(analyzerPolicy.getFromPosition())
                .size(analyzerPolicy.getFetchSize())
                .timeout(TimeValue.timeValueSeconds(analyzerPolicy.getTimeoutSeconds()));
        searchRequest.source(sourceBuilder);
        //検索DSL文設定
        //BoolQueryBuilder queryClauses = QueryBuilders.boolQuery();
        //queryClauses.filter(sqlInfo.getQueryClauses());
        BoolQueryBuilder queryClauses = sqlInfo.getQueryClauses();
        if (sqlInfo.getScoreQueryClauses() != null) {
            //Minスコア設定
            if (analyzerPolicy.getMinScore() > 0) {
                sourceBuilder.minScore(analyzerPolicy.getMinScore());
            }
            //スコア検索条件追加
            queryClauses.must(sqlInfo.getScoreQueryClauses());
        }
        //Set Filter when Using Filter Clause
        if (analyzerPolicy.isUseFilterClause()) {
        	sourceBuilder.query(QueryBuilders.boolQuery().filter(queryClauses));
        } else {
        	sourceBuilder.query(queryClauses);
        }
        // Source情報(抽出項目リスト)設定あり、且つ抽出件数は0件(集計検索のみ)でない場合
        if (analyzerPolicy.getFetchSize() > 0 && sources != null && sources.size() > 0) {
            //Source情報(抽出項目リスト)設定をリクエストへセット
            sourceBuilder.fetchSource(sources.toArray(new String[sources.size()]), null);
        }
        //ソート情報設定あり、且つ抽出件数は0件(集計検索のみ)でない場合
        if (sqlInfo.isSortInfoExists() && analyzerPolicy.getFetchSize() > 0) {
            //ソート情報をリクエストへ設定
            Map<String, SortOrder> sortList = sqlInfo.getSortList();
            Iterator<String> fieldNames = sortList.keySet().iterator();
            while(fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                SortOrder order = sortList.get(fieldName);
                //request.addSort(fieldName, order);
                sourceBuilder.sort(fieldName, order);
            }
        }
        
        // Function情報(Aggregation情報)
        //  取得項目に関数項目が存在する場合のみ
        if (functions != null && functions.size() > 0) {
            // 00.
            if (analyzerPolicy.getFetchSize() > 0 && sources != null && sources.size() > 0 
                    && groupByItems != null && sources.size() == groupByItems.size()) {
                boolean isAggsOnly = true;
                for (ISelectItem groupByItem : groupByItems) {
                    if (!sources.contains(groupByItem.getName())) {
                        isAggsOnly = false;
                        break;
                    }
                }
                if (isAggsOnly) {
                    sourceBuilder.size(0);
                }
            }
            
            //Agrregationルートノート
            AggregationBuilder rootAggs = null;
            //Aggregation追加ベースノート（土台）
            AggregationBuilder baseAggs = null;
            //Nested存在フラグ
            boolean isNestedExisted = false;
            
            // 01.Group By情報作成
            int nestedIndex = -1;
            if (groupByItems != null && groupByItems.size() > 0) {
                //  [Group By]リスト
                LinkedHashMap<String, AggregationBuilder> groupByAggs = new LinkedHashMap<String, AggregationBuilder>();
                //[Group By]Aggregation処理
                Iterator<ISelectItem> groupInter = groupByItems.iterator();
                while (groupInter.hasNext()) {
                    ISelectItem groupByInfo = groupInter.next();
                    if (groupByInfo instanceof ISource) {
                        String termName = groupByInfo.getName();
                        TermsAggregationBuilder termBuider = AggregationBuilders.terms(termName).field(termName);
                        groupByAggs.put(termName, termBuider);
                    } else if (groupByInfo instanceof IFunction) {
                        //isNested
                        if ((isNestedExisted |= groupByInfo instanceof Nested) && nestedIndex < 0) {
                            nestedIndex = groupByAggs.size();
                        }
                        IFunction aggsFunction = (IFunction) groupByInfo;
                        AggregationBuilder funcAggression = (AggregationBuilder) aggsFunction.buildAggregation();
                        groupByAggs.put(aggsFunction.getName(), funcAggression);
                    }
                }
                
                // 02.[Aggregation Sort]可否判定
                boolean isAggregationSort = false;
                if (sqlInfo.isSortInfoExists()) {
                    //ソート項目名称リスト
                    List<Object> sortNameList = Arrays.asList(sqlInfo.getSortList().keySet().toArray());
                    //Nested項目であるソート情報が存在する場合は、[Aggregation Sort]を不可とする
                    boolean isNestedSort = false;
                    for (Object sortName : sortNameList) {
                        IFunction function = functions.get(sortName);
                        if (function != null && function.isNestedParamExists()) {
                            isNestedSort = true;
                            break;
                        }
                    }
                    
                    //Nestedソートでない場合
                    if (!isNestedSort) {
                        //[Group By]1件のみの場合は[Aggregation Sort]を可能とする
                        int groupSize = groupByAggs.size();
                        isAggregationSort = groupSize == 1;
                        //2件以上である場合
                        if (!isAggregationSort) {
                            //[Group By]全件ループし、全GroupBy項目はSortリストの先頭に存在する場合はOK
                            Iterator<String> groupIter = groupByAggs.keySet().iterator();
                            //インデックス合計
                            int indexSum = 0;
                            while(groupIter.hasNext()) {
                                String groupByName = groupIter.next();
                                if (sortNameList.contains(groupByName)) {
                                    indexSum += sortNameList.indexOf(groupByName);
                                } else {
                                    indexSum = -1;
                                    break;
                                }
                            }
                            //GroupBy項目はSortリストの先頭に存在するか
                            if (indexSum != -1) {
                                int factorialValue = 0;
                                for (int i=1; i<groupSize; i++) {
                                    factorialValue += i;
                                }
                                isAggregationSort = indexSum == factorialValue;
                            }
                        }
                    }
                }
                //[Aggregation Sort]可否判定結果をセット
                //(ResultSet作成時にOrderBy処理が必要かどうかを判定する際に利用)
                sqlInfo.setAggregationSorted(isAggregationSort);
                
                // 03.ソート情報をAggsリストへ加味し、各Aggregationの土台となる「ベースAggs」を作成
                LinkedHashMap<AggregationBuilder, List<BucketOrder>> baseAggsList = new LinkedHashMap<AggregationBuilder, List<BucketOrder>>();
                // 全ソート項目リスト（ベースAggs並び替え用）
                final List<BucketOrder> orderedOrderList = new ArrayList<BucketOrder>();
                // GroupBy項目以外のOrder項目リスト
                List<BucketOrder> outOfGroupByOrders = new ArrayList<BucketOrder>();
                // GroupBy項目名称リスト
                List<Object> groupNameArray = Arrays.asList(groupByAggs.keySet().toArray());
                //SQL Nodes情報
                Map<Object, Object> sqlNode = VariablesFactory.VARIABLES_STORE.SQL_NODES_STORE.get(Map.class);
                if (isAggregationSort) {
                    //ソートリスト
                    Map<String, SortOrder> sortList = sqlInfo.getSortList();
                    //ソート項目名称を全件ループ
                    Iterator<String> fieldNames = sortList.keySet().iterator();
                    while (fieldNames.hasNext()) {
                        // 現在処理中ソート情報
                        String fieldName = fieldNames.next();
                        SortOrder orderValue = sortList.get(fieldName);
                        // 処理中Aggregation
                        AggregationBuilder currentAggs = null;
                        // ソート情報は「Group By」のAggsに存在、且つAggs型はTermsBuilderである場合のみ処理対象とする
                        if (groupByAggs.containsKey(fieldName)
                                && (currentAggs = groupByAggs.remove(fieldName)) instanceof TermsAggregationBuilder) {
                            // 現在Aggs情報インデックス
                            int currentIndex = groupNameArray.indexOf(fieldName);
                            if (isNestedExisted && currentIndex > nestedIndex) {
                                //Nested情報
                                AggregationBuilder nested = groupByAggs.get(IFunction.FUNCTION_LIST.NESTED.getName());
                                if (!baseAggsList.containsKey(nested)) {
                                    //ベースAggsへ追加
                                    baseAggsList.put(nested, null);
                                }
                            }
                            //現在Aggsへソート情報を加味
                            BucketOrder termOrder = BucketOrder.key(orderValue.equals(SortOrder.ASC));
                            //BucketOrder termOrder = BucketOrder.aggregation("_term", orderValue.equals(SortOrder.ASC));
                            //該当ベースAggsに属するOrderリストへ追加
                            List<BucketOrder> currentOrder = new ArrayList<BucketOrder>();
                            currentOrder.add(termOrder);
                            //ソート項目リストへ追加
                            orderedOrderList.add(termOrder);
                            //ベースAggsへ項目追加
                            baseAggsList.put(currentAggs, currentOrder);
                            //SQL Nodes情報追加
                            sqlNode = this.setSqlNodes(sqlNode, fieldName, currentAggs);
                        } else {
                            //GroupBy項目以外のOrder項目の場合
                            BucketOrder outOfGroupByOrder = BucketOrder.aggregation(fieldName, orderValue.equals(SortOrder.ASC));
                            outOfGroupByOrders.add(outOfGroupByOrder);
                            orderedOrderList.add(outOfGroupByOrder);
                        }
                    }
                }
                //GroupBy情報が存在する場合、全件「ベースAggs」へ追加
                if (groupByAggs.size() > 0) {
                    Iterator<String> groupNames = groupByAggs.keySet().iterator();
                    while (groupNames.hasNext()) {
                        String groupName = groupNames.next();
                        AggregationBuilder groupByAgg = groupByAggs.get(groupName);
                        baseAggsList.put(groupByAgg, new ArrayList<BucketOrder>());
                        //SQL Nodes情報追加
                        sqlNode = this.setSqlNodes(sqlNode, groupName, groupByAgg);
                    }
                }
                
                // 04.「ベースAggs」情報により、Aggregation土台を作成する
                int aggsSize = baseAggsList.size();
                int aggsIndex = 0;
                AggregationBuilder preAggs = null;
                Iterator<AggregationBuilder> aggsIter = baseAggsList.keySet().iterator();
                while (aggsIter.hasNext()) {
                    //ベースAggs
                    baseAggs = aggsIter.next();
                    //ベースAggsのソート情報
                    List<BucketOrder> orders = baseAggsList.get(baseAggs);
                    //ベースAggsはTermsである場合、デフォルトで「size:0(全件取得)」とする
                    if (baseAggs instanceof TermsAggregationBuilder) {
                        //((TermsAggregationBuilder) baseAggs).size(0);
                        ((TermsAggregationBuilder) baseAggs).size(CommonParams.RECORD_COUNT_SIZE_PER_SELECT);
                        //baseAggs = new CompositeAggregationBuilder(String.valueOf(baseAggs.hashCode()), null).subAggregation(baseAggs);
                    }
                    
                    //ルートAggs(初件の場合)
                    if (aggsIndex++ == 0) {
                        //ルートAggsへ付与
                        rootAggs = baseAggs;
                        //集計項目取得総件数は制限あり、且つルートAggsはTerms、且つソート条件なし又はソート情報が設定完了
                        if (analyzerPolicy.getAggsSize() > 0 && rootAggs instanceof TermsAggregationBuilder
                                && (isAggregationSort || !sqlInfo.isSortInfoExists())) {
                            //集計項目取得総件数をセット
                            ((TermsAggregationBuilder) rootAggs).size(analyzerPolicy.getAggsSize());
                        }
                    }
                    //最後のAggs
                    if (aggsIndex == aggsSize) {
                        //GroupByソート情報以外のソート情報が存在する場合
                        if (outOfGroupByOrders.size() > 0) {
                            //ソート情報の設定土台が存在である場合
                            if (orders != null) {
                                //ソート情報を追加し、順番を並び替え
                                orders.addAll(outOfGroupByOrders);
                                Collections.sort(orders, new Comparator<BucketOrder>() {
                                    public int compare(final BucketOrder o1, final BucketOrder o2) {
                                        return orderedOrderList.indexOf(o1) - orderedOrderList.indexOf(o2);
                                    }
                                });
                            }
                        }
                    }
                    
                    //Order情報再設定
                    if (orders != null && orders.size() > 0) {
                        ((TermsAggregationBuilder) baseAggs).order(BucketOrder.compound(orders));
                    }
                    
                    if (preAggs == null) {
                        preAggs = baseAggs;
                    } else {
                        preAggs.subAggregation(baseAggs);
                    }
                }
            }
            
            // 05.全集計関数をリクエストへ追加
            Collection<IFunction> aggsItems = functions.values();
            for (IFunction aggsItem : aggsItems) {
                //集計検索条件を取得
                if (!aggsItem.isTotalAggregation()) {
                    aggsItem.setNestedExists(isNestedExisted);
                }
                AbstractAggregationBuilder<?> aggsBuilder = aggsItem.buildAggregation();
                if (aggsBuilder != null) {
                    //集計検索文を取得できた場合、リクエストへ設定
                    if (aggsItem.isTotalAggregation() || baseAggs == null) {
                        if (!aggsItem.isSqlNodeUsed()) {
                            //セルフ土台が存在しないTotalAggsの場合、Request直下へ追加
                            sourceBuilder.aggregation(aggsBuilder);
                        }
                    } else {
                        baseAggs.subAggregation(aggsBuilder);
                    }
                }
            }
            
            // 06.「ベースAggs」にサブ集計情報が存在する場合、「ベースAggs」をリクエストへ追加
            if (rootAggs != null) {
                sourceBuilder.aggregation(rootAggs);
            }
            
            // 07.「From」と「Size」情報をSqlInfoへ追加(ResultSetにてデータの切り捨て等を実施)
            sqlInfo.setFrom(analyzerPolicy.getFromPosition());
            int size = analyzerPolicy.getFetchSize();
            if (size <= 0) {
                size = analyzerPolicy.getAggsSize();
                if (size <= 0) {
                    size = CommonParams.RECORD_COUNT_SIZE_PER_SELECT;
                }
            }
            sqlInfo.setSize(size);
        }
        
        //return searchRequest;
    }
    
    public SQLElements analyze() {
        SQLElements retValue = new SQLElements();
        try (VariablesFactory.VARIABLES_STORE sqlNodesStore = VariablesFactory.VARIABLES_STORE.SQL_NODES_STORE;) {
            // 
            sqlNodesStore.set(new HashMap<Object, Object>());
            // SQL Statement
            Statement statement = CCJSqlParserUtil.parse(sqlText);
            // SQL Convert
            SQLInfo sqlInfo = executeConvert((Select) statement, paramList);
            retValue.setSqlInfo(sqlInfo);
            
            //SelectItem情報取得
            // 取得Source情報
            //List<String> sourceInfos = new ArrayList<String>();
            // 取得Function情報
            //Map<String, IFunction> functionInfos = new LinkedHashMap<String, IFunction>();
            // Select項目情報が存在する場合、Source/Function分類処理を実施
            if (sqlInfo.isSelectItemExists()) {
                List<ISelectItem> selectItems = sqlInfo.getSelectItems();
                for (ISelectItem selectItem : selectItems) {
                    if (selectItem.isFunction()) {
                        IFunction function = (IFunction) selectItem;
                        //Functionである場合、集計検索条件を取得
                        retValue.addFunction(selectItem.getName(), function);
                    } else {
                        //Sourceである場合、抽出項目リストへ設定
                        retValue.addSource(selectItem.getName());
                    }
                }
            }
            
            buildSearchRequest(retValue);
        } catch (Exception e) {
            throw new ElasticsearchJDBCException(e);
        }
        return retValue;
    }
}

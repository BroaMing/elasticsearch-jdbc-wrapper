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
package es.jdbc.sql.analyzers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;

import es.jdbc.sql.SQLInfo;
import es.jdbc.sql.beans.AnalyzerPolicy;
import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.SelectItemFactory;
import es.jdbc.utils.CommonParams;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;

/**
 * This analyzer is designed for analyzing the SELECT clauses in SQL.
 * 
 * @author Ming Zhu
 * 
 */
public class SelectAnalyzer implements SelectVisitor {

    /**
     * Type of the SELECT clause.
     * Which is the normal select,
     * the select in WITH clause,
     * or the select in EXISTS clause.
     */
    public static enum PROCESS_MODE {
        //normal select
        NORMAL_CLAUSE,
        //select in WITH
        WITH_CLAUSE,
        //select in EXISTS
        EXISTS_CLAUSE;
    }
    
    //type of the SELECT clause.
    protected PROCESS_MODE processMode;
    //value list of the [?] clause.
    protected List<Object> paramList;
    //analyzed data of the SQL.
    protected SQLInfo analyticSqlInfo;
    
    /**
     * SELECT文間のクエリタイプ.
     */
    protected CommonParams.BOOL_QUERY_TYPE selectQueryExpressionType;
    /**
     * 追加DSL情報(WithList).
     */
    protected BoolQueryBuilder additionalQuery;
    /**
     * 現在処理中SQL文のLevel(サブクエリ存在する場合に使用).
     */
    protected int processingLevel;
    /**
     * ローカルソース名称リスト.
     */
    protected List<ISelectItem> currentSelectItems;
    /**
     * ローカル検索項目.
     */
    protected BoolQueryBuilder currentQueryBuilder;
    /**
     * ローカルFrom項目Visitor.
     */
    protected FromAnalyzer currentFromItemVisitor;
    /**
     * analyzerPolicy.
     */
    protected AnalyzerPolicy analyzerPolicy;
    
    /**
     * コンストラクター.
     * @param paramList パラメーターリスト.
     */
    public SelectAnalyzer(final List<Object> paramList, final AnalyzerPolicy analyzerPolicy) {
        //初期化処理
        initialize(PROCESS_MODE.NORMAL_CLAUSE, paramList, analyzerPolicy);
    }
    /**
     * コンストラクター.
     * @param paramList パラメーターリスト.
     */
    public SelectAnalyzer(final PROCESS_MODE processMode, final List<Object> paramList, final AnalyzerPolicy analyzerPolicy) {
        //初期化処理
        initialize(processMode, paramList, analyzerPolicy);
    }
    /**
     * 初期化処理
     * @param processMode 処理モード
     * @param paramList パラメーターリスト
     */
    private void initialize(final PROCESS_MODE processMode, final List<Object> paramList, final AnalyzerPolicy analyzerPolicy) {
        //パラメーターリスト設定
        this.paramList = paramList;
        //SELECT文間のクエリタイプ初期化
        this.selectQueryExpressionType = CommonParams.BOOL_QUERY_TYPE.MUST;
        //現在処理中SQL文のLevel
        this.processingLevel = 1;
        //SQL分析情報
        this.analyticSqlInfo = new SQLInfo();
        //処理モード設定
        this.processMode = processMode;
        
        this.analyzerPolicy = analyzerPolicy;
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.statement.select.SelectVisitor#visit(net.sf.jsqlparser.statement.select.PlainSelect)
     */
    @Override
    public void visit(PlainSelect arg0) {
        //1、Select処理
        //ローカルパラメーター取得
        List<ISelectItem> currentSelectItems = this.getCurrentSelectItems();
        //SelectItem
        List<SelectItem> selectItemList = null;
        if (arg0 != null && (selectItemList = arg0.getSelectItems()) != null && selectItemList.size() > 0) {
            //SelectItemVisitor初期化
            SelectItemAnalyzer selectItemVisitor = new SelectItemAnalyzer(paramList, analyzerPolicy);
            //全抽出項目をループ
            for (SelectItem item : selectItemList) {
                item.accept(selectItemVisitor);
            }
            //ソースリストへ追加
            List<ISelectItem> visitedSelectItems = selectItemVisitor.getSelectItems();
            if (currentSelectItems.size() == 0) {
                //Select情報はまだ存在していない場合、全件セットする
                currentSelectItems.addAll(visitedSelectItems);
            } else if (visitedSelectItems != null && visitedSelectItems.size() > 0) {
                //Select情報は既に存在している場合、同名Source項目をFunction項目にて入れ替え
                List<ISelectItem> substituteItemList = new ArrayList<ISelectItem>();
                //既存Select情報を全件ループ
                Iterator<ISelectItem> currentSelectItemInter = currentSelectItems.iterator();
                CurrentSelectLoop:
                while (currentSelectItemInter.hasNext()) {
                    ISelectItem currentSelectItem = currentSelectItemInter.next();
                    //Functionでない項目（Source項目）
                    if (!currentSelectItem.isFunction()) {
                        //Visitor結果リストを全件ループ
                        for (ISelectItem visitedSelectItem : visitedSelectItems) {
                            //同名Functionが存在する場合
                            if (StringUtils.equals(currentSelectItem.getName(), visitedSelectItem.getName())
                                    && visitedSelectItem.isFunction()) {
                                //既存Source項目を削除
                                currentSelectItemInter.remove();
                                //入れ替え用リストへ追加
                                substituteItemList.add(visitedSelectItem);
                                continue CurrentSelectLoop;
                            }
                        }
                    }
                }
                //入れ替え項目を追加
                currentSelectItems.addAll(substituteItemList);
            }
        }

        //2、From処理
        //  From Visitor取得
        FromAnalyzer fromItemVisitor = this.getFromItemVisitor();
        arg0.getFromItem().accept(fromItemVisitor);
        //  テーブル名称設定
        analyticSqlInfo.setTableName(fromItemVisitor.getFirstTable());
        //  SelectItemにtype情報を設定
        String processTableName = analyzerPolicy.getMainIndex();
        if (PROCESS_MODE.EXISTS_CLAUSE.equals(this.processMode) || !StringUtils.isNotBlank(analyzerPolicy.getMainIndex())) {
            processTableName = fromItemVisitor.getFirstTable();
        }
        for (ISelectItem selectItem : currentSelectItems) {
            selectItem.setType(processTableName);
        }
        
        //3、Where処理
        BoolQueryBuilder whereQueryBuilder = null;
        List<QueryBuilder> whereScoreQueryBuilders = null;
        //  且つWhere文存在する場合
        if (arg0.getWhere() != null) {
            WhereAnalyzer expressionVisitor = new WhereAnalyzer(paramList, analyzerPolicy);
            expressionVisitor.setTableName(fromItemVisitor.getFirstTable());
            arg0.getWhere().accept(expressionVisitor);
            whereQueryBuilder = expressionVisitor.getQueryBuilder();
            whereScoreQueryBuilders = expressionVisitor.getScoreQueryBuilderStack();
        }
        
        //分析結果をセット
        if (this.processingLevel == 1) {
            //SQL文レベル1の場合、最終項目へまとめ
            // ソースリスト
            this.analyticSqlInfo.addSelectItemInfo(currentSelectItems);
            
            // 検索項目
            //QueryBuilder作成
            if (whereQueryBuilder == null) {
                whereQueryBuilder = QueryBuilders.boolQuery();
            }
            //「_type」パラメーター設定、追加検索項目設定
            if (PROCESS_MODE.NORMAL_CLAUSE.equals(this.processMode)) {
                    //&& StringUtils.isNotBlank(this.analyticSqlInfo.getTableName())) {
                //追加検索項目を加味
                //if (this.analyticSqlInfo.getTableName().equals(analyzerPolicy.getMainIndex()) && this.additionalQuery != null && this.additionalQuery.hasClauses()) {
            	if (this.additionalQuery != null && this.additionalQuery.hasClauses()) {
                    // MUSTへ追加
                    whereQueryBuilder.must(this.additionalQuery);
                }
            }
            
            //ローカルクエリ
            if (this.currentQueryBuilder != null) {
                // MUSTへ追加
                whereQueryBuilder.must(this.currentQueryBuilder);
            }
            this.analyticSqlInfo.addQueryClause(selectQueryExpressionType, whereQueryBuilder);

            //GroupBy処理(GroupByにバイナリ変数が存在しない)
            if (!this.analyticSqlInfo.isGroupByItemsExists()) {
                buildGroupByItems(arg0.getGroupByColumnReferences());
            }
            //ソートリストがまだ未設定である場合、ソート情報を作成
            if (!this.analyticSqlInfo.isSortInfoExists()) {
                this.buildSortInfo(arg0.getOrderByElements());
            }
            
            //Score検索条件が存在する場合
            if (whereScoreQueryBuilders != null && whereScoreQueryBuilders.size() > 0) {
                BoolQueryBuilder scoreQuery = QueryBuilders.boolQuery();
                for (QueryBuilder whereScoreQueryBuilder : whereScoreQueryBuilders) {
                    scoreQuery.must(whereScoreQueryBuilder);
                }
                this.analyticSqlInfo.setScoreQueryClauses(scoreQuery);
            }
            
            //ローカルパラメーターリセット
            this.resetCurrentParameter();
        } else if (whereQueryBuilder != null) {
            //SQLレベル1でない場合、ローカル検索項目へセット
            if (this.currentQueryBuilder == null) {
                this.currentQueryBuilder = whereQueryBuilder;
            } else {
                this.currentQueryBuilder.must(whereQueryBuilder);
            }
        }
    }

    /**
     * SELECT並列文(UNION(ALL)のみサポート)
     */
    @Override
    public void visit(SetOperationList arg0) {
        //ソートリストがまだ未設定である場合、ソート情報を作成
        if (!this.analyticSqlInfo.isSortInfoExists()) {
            this.buildSortInfo(arg0.getOrderByElements());
        }
        
        //Unionの場合は、SQL間は「OR」関係
        this.selectQueryExpressionType = CommonParams.BOOL_QUERY_TYPE.SHOULD;
        //Select全件処理
        for (SelectBody select : arg0.getSelects()) {
            select.accept(this);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.statement.select.SelectVisitor#visit(net.sf.jsqlparser.statement.select.WithItem)
     */
    @Override
    public void visit(WithItem arg0) {
        arg0.getSelectBody().accept(this);
    }
    /**
     * ソート情報をSQL分析情報へ追加.
     * @param orderElements ソート情報
     */
    public void buildSortInfo(final List<OrderByElement> orderElements) {
        if (orderElements == null || orderElements.size() == 0) {
            return;
        }
        //Order情報を全件ループ
        for (OrderByElement orderElement : orderElements) {
            //Columnによってのソートのみを有効とする
            if (orderElement.getExpression() instanceof Column) {
                String fieldName = ((Column) orderElement.getExpression()).getColumnName();
                SortOrder order = orderElement.isAsc()?SortOrder.ASC:SortOrder.DESC;
                this.analyticSqlInfo.addSortInfo(fieldName, order);
            }
        }
    }
    /**
     * ソート情報をSQL分析情報へ追加.
     * @param orderElements ソート情報
     */
    public void buildGroupByItems(final List<Expression> groupByElements) {
        if (groupByElements == null || groupByElements.size() == 0) {
            return;
        }
        //GroupBy情報を全件ループ
        for (Expression groupByElement : groupByElements) {
            // Columnによってのソートのみを有効とする
            ISelectItem groupByItem = SelectItemFactory.manufacture(groupByElement, this.paramList);
            if (groupByItem != null) {
                this.analyticSqlInfo.addGroupByItem(groupByItem);
            }
        }
    }

    /**
     * @param additionalQuery the additionalQuery to set
     */
    public void setAdditionalQuery(final BoolQueryBuilder additionalQuery) {
        this.additionalQuery = additionalQuery;
    }
    /**
     * 処理中SQL文Level+1.
     */
    public void increaseCurrentLevel() {
        this.processingLevel++;
    }
    /**
     * 処理中SQL文Level-1.
     */
    public void decreaseCurrentLevel() {
        this.processingLevel--;
    }
    /**
     * ローカルソース名称リスト取得.
     * @return ローカルソース名称リスト
     */
    public List<ISelectItem> getCurrentSelectItems() {
        if (this.currentSelectItems == null) {
            this.currentSelectItems = new ArrayList<ISelectItem>();
        }
        return this.currentSelectItems;
    }
    /**
     * ローカルFrom項目Visitor取得.
     * @return ローカルFrom項目Visitor
     */
    public FromAnalyzer getFromItemVisitor() {
        if (this.currentFromItemVisitor == null) {
            this.currentFromItemVisitor = new FromAnalyzer();
            this.currentFromItemVisitor.setSelectVisitor(this);
        }
        return this.currentFromItemVisitor;
    }
    /**
     * ローカル項目値リセット.
     */
    public void resetCurrentParameter() {
        this.currentSelectItems = null;
        this.currentQueryBuilder = null;
        this.currentFromItemVisitor = null;
    }

    /**
     * @return the analyticSqlInfo
     */
    public SQLInfo getAnalyticSqlInfo() {
        return analyticSqlInfo;
    }

    /**
     * @param analyticSqlInfo the analyticSqlInfo to set
     */
    public void setAnalyticSqlInfo(SQLInfo analyticSqlInfo) {
        this.analyticSqlInfo = analyticSqlInfo;
    }
}

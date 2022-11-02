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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;

import es.jdbc.sql.search.ISelectItem;
import es.jdbc.utils.CommonParams;

public class SQLInfo {
    /**
     * テーブル名称.
     */
    protected String tableName;
    /**
     * 検索項目(取得項目).
     */
    protected BoolQueryBuilder queryClauses;
    /**
     * ソースリスト(取得項目).
     */
    protected List<ISelectItem> selectItems;
    /**
     * ソート項目(取得項目).
     */
    protected Map<String, SortOrder> sortList;
    /**
     * GroupBy項目.
     */
    protected List<ISelectItem> groupByItems;
    /**
     * Score検索項目(取得項目).
     */
    protected BoolQueryBuilder scoreQueryClauses;
    /**
     * Aggregationソート存在するか.
     */
    protected boolean isAggregationSorted;
    /**
     * From.
     */
    protected int from;
    /**
     * Size.
     */
    protected int size;
    /**
     * コンストラクター.
     */
    public SQLInfo() {
        this.queryClauses = QueryBuilders.boolQuery();
        this.selectItems = new ArrayList<ISelectItem>();
        this.sortList = new LinkedHashMap<String, SortOrder>();
        this.groupByItems = new ArrayList<ISelectItem>();
        this.isAggregationSorted = false;
        this.from = 0;
        this.size = CommonParams.RECORD_COUNT_SIZE_PER_SELECT;
    }
    
    /**
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }
    /**
     * @param tableName the tableName to set
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    /**
     * @return the queryClauses
     */
    public BoolQueryBuilder getQueryClauses() {
        return queryClauses;
    }
    /**
     * @param queryClauses the queryClauses to set
     */
    public void setQueryClauses(BoolQueryBuilder queryClauses) {
        this.queryClauses = queryClauses;
    }
    /**
     * @return the sourceList
     */
    public List<ISelectItem> getSelectItems() {
        return selectItems;
    }
    /**
     * @param sourceList the sourceList to set
     */
    public void setSelectItems(List<ISelectItem> sourceList) {
        this.selectItems = sourceList;
    }
    /**
     * ソース情報が存在するか(true:存在する false:存在しない).
     * @return ソース情報が存在するか
     */
    public boolean isSelectItemExists() {
        return this.selectItems.size() > 0;
    }
    /**
     * @return the sortList
     */
    public Map<String, SortOrder> getSortList() {
        return sortList;
    }

    /**
     * @param sortList the sortList to set
     */
    public void setSortList(Map<String, SortOrder> sortList) {
        this.sortList = sortList;
    }
    
    /**
     * ソート情報が存在するか(true:存在する false:存在しない).
     * @return ソート情報が存在するか
     */
    public boolean isSortInfoExists() {
        return this.sortList.size() > 0;
    }
    /**
     * ソート情報追加.
     * @param sortInfo ソート情報
     */
    public void addSortInfo(final String fieldName, final SortOrder order) {
        this.sortList.put(fieldName, order);
    }
    
    /**
     * @return the groupByItems
     */
    public List<ISelectItem> getGroupByItems() {
        return groupByItems;
    }

    /**
     * @param groupByItems the groupByItems to set
     */
    public void setGroupByItems(List<ISelectItem> groupByItems) {
        this.groupByItems = groupByItems;
    }
    
    /**
     * ソート情報が存在するか(true:存在する false:存在しない).
     * @return ソート情報が存在するか
     */
    public boolean isGroupByItemsExists() {
        return this.groupByItems.size() > 0;
    }
    /**
     * ソート情報追加.
     * @param sortInfo ソート情報
     */
    public void addGroupByItem(final ISelectItem groupByItem) {
        this.groupByItems.add(groupByItem);
    }

    /**
     * ソース情報追加.
     * @param sourceInfo ソース情報
     */
    public void addSelectItemInfo(final ISelectItem selectItem) {
        this.selectItems.add(selectItem);
    }
    /**
     * ソース情報一括追加.
     * @param sourceInfos ソース情報
     */
    public void addSelectItemInfo(final List<ISelectItem> selectItems) {
        if (selectItems != null) {
            this.selectItems.addAll(selectItems);
        }
    }
    
    /**
     * 検索条件情報追加.
     * @param queryType 条件情報タイプ
     * @param queryClause 検索条件情報
     */
    public void addQueryClause(CommonParams.BOOL_QUERY_TYPE queryType, BoolQueryBuilder queryClause) {
        //検索条件が存在しない場合、追加せずに終了
        if (queryClause == null || !queryClause.hasClauses()) {
            return;
        }
        // DSLオブジェクト作成
        if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(queryType)) {
            this.queryClauses.should(queryClause);
        } else if (CommonParams.BOOL_QUERY_TYPE.MUST_NOT.equals(queryType)) {
            this.queryClauses.mustNot(queryClause);
        } else {
            this.queryClauses.must(queryClause);
        }
    }
    /**
     * 検索条件情報が存在するか(true:存在する false:存在しない).
     * @return 検索条件情報が存在するか
     */
    public boolean isQueryClauseExists() {
        return this.queryClauses.hasClauses();
    }
    
    /**
     * @return the isAggregationSorted
     */
    public boolean isAggregationSorted() {
        return isAggregationSorted;
    }
    /**
     * @param isAggregationSorted the isAggregationSorted to set
     */
    public void setAggregationSorted(boolean isAggregationSorted) {
        this.isAggregationSorted = isAggregationSorted;
    }
    /**
     * @return the from
     */
    public int getFrom() {
        return from;
    }
    /**
     * @param from the from to set
     */
    public void setFrom(int from) {
        this.from = from;
    }
    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }
    /**
     * @param size the size to set
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * @return the scoreQueryClauses
     */
    public BoolQueryBuilder getScoreQueryClauses() {
        return scoreQueryClauses;
    }
    /**
     * @param scoreQueryClauses the scoreQueryClauses to set
     */
    public void setScoreQueryClauses(BoolQueryBuilder scoreQueryClauses) {
        this.scoreQueryClauses = scoreQueryClauses;
    }
}

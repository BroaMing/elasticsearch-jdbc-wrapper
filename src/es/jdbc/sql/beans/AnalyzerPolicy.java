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
package es.jdbc.sql.beans;

import java.io.Serializable;

import org.elasticsearch.action.search.SearchType;

import es.jdbc.utils.CommonParams;

public class AnalyzerPolicy implements Serializable {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 3356874924599919951L;
    /**
     * Search Target Main Index.
     */
    private String mainIndex;
    /**
     * Ignore Value Condition(Where).
     */
    private boolean ignoreValueCondition;
    /**
     * Ignore With Clauses.
     */
    private boolean ignoreWithClause;
    /**
     * Use Filter Clause.
     */
    private boolean useFilterClause;
    /**
     * from.
     */
    private int fromPosition;
    /**
     * size.
     */
    private int fetchSize;
    /**
     * aggs size.
     */
    private int aggsSize;
    /**
     * min score.
     */
    private float minScore;
    /**
     * timeout Seconds 
     */
    private int timeoutSeconds;
    /**
     * searchType.
     */
    private SearchType searchType;
    /**
     * joinRelation.
     */
    private JoinRelation joinRelation;
    
    public static AnalyzerPolicy getInstance() {
        AnalyzerPolicy retValue = new AnalyzerPolicy();
        retValue.setIgnoreValueCondition(false);
        retValue.setIgnoreWithClause(true);
        retValue.setUseFilterClause(false);
        retValue.setFromPosition(0);
        retValue.setFetchSize(CommonParams.RECORD_COUNT_SIZE_PER_SELECT);
        retValue.setAggsSize(0);
        retValue.setMinScore(0F);
        retValue.setSearchType(SearchType.QUERY_THEN_FETCH);
        retValue.setTimeoutSeconds(CommonParams.SEARCH_TIMEOUT_TIME_SECOND);
        return retValue;
    }
    
    private AnalyzerPolicy() { }
    //
    //public AnalyzerPolicy() {
    //    ignoreValueCondition = false;
    //    ignoreWithClause = true;
    //    fromPosition = 0;
    //    fetchSize = CommonParams.RECORD_COUNT_SIZE_PER_SELECT;
    //    aggsSize = 0;
    //    searchType = SearchType.QUERY_THEN_FETCH;
    //    timeoutSeconds = CommonParams.SEARCH_TIMEOUT_TIME_SECOND;
    //}
    //public String getSqlText() {
    //    return sqlText;
    //}
    //public void setSqlText(String sqlText) {
    //    this.sqlText = sqlText;
    //}
    public String getMainIndex() {
        return mainIndex;
    }
    public AnalyzerPolicy setMainIndex(String mainIndex) {
        this.mainIndex = mainIndex;
        return this;
    }
    public boolean isIgnoreValueCondition() {
        return ignoreValueCondition;
    }
    public AnalyzerPolicy setIgnoreValueCondition(boolean ignoreValueCondition) {
        this.ignoreValueCondition = ignoreValueCondition;
        return this;
    }
    public boolean isIgnoreWithClause() {
        return ignoreWithClause;
    }
    public AnalyzerPolicy setIgnoreWithClause(boolean ignoreWithClause) {
        this.ignoreWithClause = ignoreWithClause;
        return this;
    }
    public boolean isUseFilterClause() {
		return useFilterClause;
	}
	public void setUseFilterClause(boolean useFilterClause) {
		this.useFilterClause = useFilterClause;
	}
	//public List<String> getSubIndexes() {
    //    return subIndexes;
    //}
    //public AnalyzerPolicy setSubIndexes(List<String> subIndexes) {
    //    this.subIndexes = subIndexes;
    //    return this;
    //}
    public int getFromPosition() {
        return fromPosition;
    }
    public AnalyzerPolicy setFromPosition(int fromPosition) {
        this.fromPosition = fromPosition;
        return this;
    }
    public int getFetchSize() {
        return fetchSize;
    }
    public AnalyzerPolicy setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
        return this;
    }
    public int getAggsSize() {
        return aggsSize;
    }
    public AnalyzerPolicy setAggsSize(int aggsSize) {
        this.aggsSize = aggsSize;
        return this;
    }
    public float getMinScore() {
        return minScore;
    }
    public AnalyzerPolicy setMinScore(float minScore) {
        this.minScore = minScore;
        return this;
    }
    public SearchType getSearchType() {
        return searchType;
    }
    public AnalyzerPolicy setSearchType(SearchType searchType) {
        this.searchType = searchType;
        return this;
    }
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    public AnalyzerPolicy setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        return this;
    }
    public JoinRelation getJoinRelation() {
        return joinRelation;
    }
    public AnalyzerPolicy setJoinRelation(JoinRelation joinRelation) {
        this.joinRelation = joinRelation;
        return this;
    }
}

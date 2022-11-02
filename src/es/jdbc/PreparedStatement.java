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
package es.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.joda.time.DateTime;

import es.jdbc.exceptions.ElasticsearchJDBCException;
import es.jdbc.sql.SQLInfo;
import es.jdbc.sql.SearchSQLAnalyzer;
import es.jdbc.sql.beans.AnalyzerPolicy;
import es.jdbc.sql.beans.SQLElements;
import es.jdbc.sql.search.functions.IFunction;
import es.jdbc.utils.CommonParams;


public class PreparedStatement implements java.sql.PreparedStatement {
    
    /**
     * Parameter List of SQL.
     */
    protected List<Object> paramList;
    /**
     * Elasticsearch Client.
     */
    protected RestHighLevelClient client;
    /**
     * SQL Text.
     */
    protected String sqlText;
    /**
     * analyzerPolicy.
     */
    protected AnalyzerPolicy analyzerPolicy;
    /**
     * SQL Element.
     */
    protected SQLElements sqlElements;
    ///**
    // * Search Target Main Index.
    // */
    //protected String mainIndex;
    ///**
    // * Search Target Sub Indexes.
    // */
    //protected List<String> subIndexes;
    ///**
    // * from.
    // */
    //protected int fromPosition;
    ///**
    // * size.
    // */
    //protected int fetchSize;
    ///**
    // * aggs size.
    // */
    //protected int aggsSize;
    ///**
    // * min score.
    // */
    //protected float minScore;
    ///**
    // * searchType.
    // */
    //protected SearchType searchType;
    
    /**
     * Constructor.
     * @param client
     * @param analyzerPolicy
     */
    public PreparedStatement(final RestHighLevelClient client) throws Exception {
        initialize(null, client, AnalyzerPolicy.getInstance());
    }
    /**
     * Constructor.
     * @param client
     * @param analyzerPolicy
     */
    public PreparedStatement(final RestHighLevelClient client, final AnalyzerPolicy analyzerPolicy) throws Exception {
        initialize(null, client, analyzerPolicy);
    }
    
    /**
     * Constructor.
     * @param sqlText
     * @param client
     * @param analyzerPolicy
     */
    public PreparedStatement(final String sqlText, final RestHighLevelClient client, final AnalyzerPolicy analyzerPolicy) throws Exception {
        initialize(sqlText, client, analyzerPolicy);
    }
    
    private void initialize(final String sqlText, final RestHighLevelClient client, final AnalyzerPolicy analyzerPolicy) {
        this.paramList = new ArrayList<Object>();
        this.sqlText = sqlText;
        this.client = client;
        this.analyzerPolicy = analyzerPolicy;
    }
    
    /**
     * ResultSet作成.
     * @param sqlInfo NoSQL検索条件
     * @param functionInfos 集計情報リスト(Aggregation)
     * @param response 検索レスポンス
     * @return ResultSet
     * @throws SQLException 
     * @throws Exception 想定外異常
     */
    public ResultSet buildResultSet(SQLElements sqlElement, SearchResponse response) throws SQLException {
        SQLInfo sqlInfo = sqlElement.getSqlInfo();
        Map<String, IFunction> functionInfos = sqlElement.getFunctions();
        
        //ResultSet作成
        ResultSet resultSet = new ResultSet();
        
        //検索結果取得(hits)
        SearchHits searchHits = null;
        if (response != null 
                && (searchHits = response.getHits()) != null
                && searchHits.getTotalHits() > 0) {
            //総件数を設定
            resultSet.setHitsTotal(searchHits.getTotalHits());
            //全取得情報をループ
            SearchHit[] hitArray = searchHits.getHits();
            for (SearchHit hit : hitArray) {
                //タイプ名称
                //String typeName = hit.getType();
                Map<String, Object> sourceMap = hit.getSourceAsMap();
                resultSet.addRowData(analyzerPolicy.getMainIndex(), sourceMap);
            }
        }
        //Aggregation情報処理
        Aggregations aggs = null;
        if (response != null && (aggs = response.getAggregations()) != null) {
            //集計情報コンテナー
            List<Map<String, Object>> aggsContainer = new ArrayList<Map<String, Object>>();
            //集計情報リスト
            List<Aggregation> aggsList = aggs.asList();
            //全集計関数をループし、Aggregation情報を取得
            for (IFunction function : functionInfos.values()) {
                function.fillResult(aggsContainer, aggsList);
            }
            //集計情報をResultSetへマージする
            resultSet.mergeWithAggsMap(aggsContainer, sqlInfo);
        }
        
        return resultSet;
    }
    
    /**
     * Execute Query.
     * @return ResultSet
     * @throws SQLException
     */
    @Override
    public ResultSet executeQuery() throws SQLException {
        ResultSet retValue = null;
        
        try {
        	if (sqlElements == null) {
                SearchSQLAnalyzer analyzer = new SearchSQLAnalyzer(sqlText, paramList, analyzerPolicy);
                sqlElements = analyzer.analyze();
System.out.println(sqlElements.getRequest().source());
        	}

            SearchResponse response = this.client.search(sqlElements.getRequest());
            //レスポンスによりResultSet作成
            retValue = this.buildResultSet(sqlElements, response);
        } catch (IOException e) {
            throw new ElasticsearchJDBCException(e);
        }
        
        return retValue;
    }
    
    /**
     * Execute Query.
     * @return ResultSet
     * @throws SQLException
     */
    @Override
    public ResultSet executeQuery(String arg0) throws SQLException {
        this.sqlText = arg0;
        return executeQuery();
    }
    
    public SQLElements getSQLElements() {
    	if (sqlElements == null) {
            SearchSQLAnalyzer analyzer = new SearchSQLAnalyzer(sqlText, paramList, analyzerPolicy);
            //SearchRequest searchRequest = analyzer.analyze();
            sqlElements = analyzer.analyze();
    	}
    	return sqlElements;
    }
    
    /**
     * Add Parameter to Parameter List
     * @param index
     * @param value
     */
    private void setObjectToParamList(final int index, final Object value) {
        int diff = (paramList.size() + 1) - index;
        if (diff == 0) {
            paramList.add(value);
        } else if (diff > 0) {
            paramList.set(index - 1, value);
        } else {
            paramList.addAll(Arrays.asList(new Object[-1 * diff]));
            paramList.add(value);
        }
    }

    @Override
    public void setString(int arg0, String arg1) throws SQLException {
        setObjectToParamList(arg0, arg1);
    }

    @Override
    public void setInt(int arg0, int arg1) throws SQLException {
        setObjectToParamList(arg0, arg1);
    }

    @Override
    public void setLong(int arg0, long arg1) throws SQLException {
        setObjectToParamList(arg0, arg1);
    }

    @Override
    public void setFloat(int arg0, float arg1) throws SQLException {
        setObjectToParamList(arg0, arg1);
    }

    @Override
    public void setDate(int arg0, Date arg1) throws SQLException {
        String dateTimeString = null;
        if (arg1 != null) {
            DateTime dateTime = CommonParams.ELASTICSEARCH_DATE_FORMATTER_NORMAL.parseDateTime(CommonParams.DATE_FORMATTER.format(arg1));
            dateTimeString = dateTime.toString();
        }
        setObjectToParamList(arg0, dateTimeString);
    }

    @Override
    public void setTimestamp(int arg0, Timestamp arg1) throws SQLException {
        String dateTimeString = null;
        if (arg1 != null) {
        	Date date = new Date(arg1.getTime());
            DateTime dateTime = CommonParams.ELASTICSEARCH_DATE_FORMATTER_NORMAL.parseDateTime(CommonParams.DATE_FORMATTER.format(date));
            dateTimeString = dateTime.toString();
        }
        setObjectToParamList(arg0, dateTimeString);
    }

    @Override
    public void setBigDecimal(int arg0, BigDecimal arg1) throws SQLException {
        setObjectToParamList(arg0, arg1);
    }
    
    /**
     * Global Variations.
     */
    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public RestHighLevelClient getClient() {
        return client;
    }

    public void setClient(RestHighLevelClient client) {
        this.client = client;
    }

    public AnalyzerPolicy getAnalyzerPolicy() {
        return analyzerPolicy;
    }

    public void setAnalyzerPolicy(AnalyzerPolicy analyzerPolicy) {
        this.analyzerPolicy = analyzerPolicy;
    }

    /**
     * Below are not implemented yet.
     */
    @Override
    public void addBatch(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void cancel() throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void clearBatch() throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void clearWarnings() throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void close() throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean execute(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean execute(String arg0, int arg1) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean execute(String arg0, int[] arg1) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean execute(String arg0, String[] arg1) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int[] executeBatch() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int executeUpdate(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int executeUpdate(String arg0, int arg1) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int executeUpdate(String arg0, int[] arg1) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int executeUpdate(String arg0, String[] arg1) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Connection getConnection() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getFetchSize() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxRows() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getMoreResults(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getResultSetType() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isClosed() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPoolable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setCursorName(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setEscapeProcessing(boolean arg0) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setFetchDirection(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setFetchSize(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setMaxFieldSize(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setMaxRows(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setPoolable(boolean arg0) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setQueryTimeout(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addBatch() throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void clearParameters() throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean execute() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int executeUpdate() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setArray(int arg0, Array arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setAsciiStream(int arg0, InputStream arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setAsciiStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setAsciiStream(int arg0, InputStream arg1, long arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBinaryStream(int arg0, InputStream arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBinaryStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBinaryStream(int arg0, InputStream arg1, long arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBlob(int arg0, Blob arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBlob(int arg0, InputStream arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBlob(int arg0, InputStream arg1, long arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBoolean(int arg0, boolean arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setByte(int arg0, byte arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBytes(int arg0, byte[] arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setCharacterStream(int arg0, Reader arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setCharacterStream(int arg0, Reader arg1, int arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setClob(int arg0, Clob arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setClob(int arg0, Reader arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setClob(int arg0, Reader arg1, long arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDate(int arg0, Date arg1, Calendar arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDouble(int arg0, double arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setNCharacterStream(int arg0, Reader arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setNCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setNClob(int arg0, NClob arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setNClob(int arg0, Reader arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setNClob(int arg0, Reader arg1, long arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setNString(int arg0, String arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setNull(int arg0, int arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setNull(int arg0, int arg1, String arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setObject(int arg0, Object arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setObject(int arg0, Object arg1, int arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setObject(int arg0, Object arg1, int arg2, int arg3) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setRef(int arg0, Ref arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setRowId(int arg0, RowId arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setSQLXML(int arg0, SQLXML arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setShort(int arg0, short arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setTime(int arg0, Time arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setTime(int arg0, Time arg1, Calendar arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setTimestamp(int arg0, Timestamp arg1, Calendar arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setURL(int arg0, URL arg1) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setUnicodeStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        // TODO Auto-generated method stub
        
    }
}

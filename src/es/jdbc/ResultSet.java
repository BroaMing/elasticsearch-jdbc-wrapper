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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;

import es.jdbc.exceptions.ElasticsearchJDBCException;
import es.jdbc.sql.SQLInfo;
import es.jdbc.utils.CommonParams;

public class ResultSet implements java.sql.ResultSet {

    /** 行データリスト. */
    protected List<RowData> rowDataList = new ArrayList<RowData>();
    /** 行データIterator. */
    protected ListIterator<RowData> rowDataIterator;
    /** ローカル行データ. */
    protected RowData currentRowData;
    /** 総件数. */
    protected long hitsTotal = 0L;

    /** 行データ. */
    class RowData {
        private String name;
        private Map<String, Object> source;
        /**
         * @return the type
         */
        public String getName() {
            return name;
        }
        /**
         * @param type the type to set
         */
        public void setName(String name) {
            this.name = name;
        }
        /**
         * @return the source
         */
        public Map<String, Object> getSource() {
            return source;
        }
        /**
         * @param source the source to set
         */
        public void setSource(Map<String, Object> source) {
            this.source = source;
        }
    }

    /**
     * 行データ追加.
     * @param type タイプ名称
     * @param source ソース
     */
    public void addRowData(final String name, final Map<String, Object> source) {
        RowData rowData = new RowData();
        rowData.setName(name);
        rowData.setSource(source);
        rowDataList.add(rowData);
    }
    
    /**
     * 処理中データのタイプ名称を取得.
     * @return タイプ名称
     */
    public String getCurrentDataName() {
        return this.currentRowData.getName();
    }
    
    /**
     * マージコンテナーを検索する.
     * @param currentData 検索対象データ
     * @param aggsRowData マージ対象データ
     * @return
     * @throws SQLException 
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> seekContainerForMerge(Map<String, Object> currentData, Map<String, Object> aggsRowData) {
        //戻り値
        List<Map<String, Object>> retValue = new ArrayList<Map<String, Object>>();
        if (currentData == null || currentData.isEmpty()) {
            return retValue;
        }
        //判定用Map
        Map<String, Object> judgeMap = new LinkedHashMap<String, Object>(aggsRowData);
        //サブ判定用Map
        Map<List<Map<String, Object>>, LinkedHashMap<String, Object>> subJudgeInfo = 
                new HashMap<List<Map<String, Object>>, LinkedHashMap<String, Object>>();
        //判定名称を全件ループし、マージ対象コンテナーを検索
        Iterator<String> judgeNames = judgeMap.keySet().iterator();
        while (judgeNames.hasNext()) {
            //判定名称
            String judgeName = judgeNames.next();
            //判定名称は存在しない場合、処理を中断
            if (!this.existsColumn(currentData, judgeName)) {
                break;
            }
            //対象値を検索
            Object soughtValue = this.seekValue(currentData, judgeName);
            if (soughtValue instanceof List) {
                //検索対象はリストの場合、項目名に階層マーク「.」が存在する場合のみを処理対象とする
                if (StringUtils.defaultString(judgeName).indexOf(".") > -1) {
                    String[] subJudgeNames = StringUtils.split(judgeName, ".");
                    //項目対象の１つ上階層
                    String[] subJudgeListName = (String[]) ArrayUtils.subarray(subJudgeNames, 0, subJudgeNames.length-1);
                    //項目対象名称
                    String subJudgeElementName = subJudgeNames[subJudgeNames.length-1];
                    //項目対象の１つ上階層リストを取得
                    Object expendedList = this.seekValue(currentData, subJudgeListName);
                    //取得結果はリストである場合のみ処理
                    if (expendedList instanceof List) {
                        List<Map<String, Object>> subJudgeList = (List<Map<String, Object>>) expendedList;
                        if (subJudgeInfo.containsKey(subJudgeList)) {
                            //初回処理でない場合
                            Map<String, Object> subAggsRowData = subJudgeInfo.get(subJudgeList);
                            subAggsRowData.put(subJudgeElementName, aggsRowData.get(judgeName));
                        } else {
                            //初回処理である場合
                            LinkedHashMap<String, Object> subAggsRowData = new LinkedHashMap<String, Object>();
                            subJudgeInfo.put(subJudgeList, subAggsRowData);
                            subAggsRowData.put(subJudgeElementName, aggsRowData.get(judgeName));
                        }
                    }
                }
            } else {
                if (StringUtils.equals(Objects.toString(soughtValue, null), Objects.toString(aggsRowData.get(judgeName), null))) {
                    //判定名称対象から削除
                    judgeNames.remove();
                } else {
                    //１つの項目でも不一致が存在する場合、処理を中断
                    retValue.clear();
                    return retValue;
                }
            }
        }
        
        if (judgeMap.size() == 0) {
            //判定名称対象は存在しない場合、該当検索対象データを「マージコンテナー」として返す
            retValue.add(currentData);
        } else if (subJudgeInfo.size() > 0) {
            //サブ判定用Mapが存在する場合、サブ検索対象に対して全件検索を実施
            Iterator<List<Map<String, Object>>> subJudgeLists = subJudgeInfo.keySet().iterator();
            while (subJudgeLists.hasNext()) {
                //検索対象リスト
                List<Map<String, Object>> subJudgeList = subJudgeLists.next();
                //マージ対象データ
                Map<String, Object> subAggsRowData = subJudgeInfo.get(subJudgeList);
                for (Map<String, Object> subJudgeMap : subJudgeList) {
                    //検索対象データ
                    List<Map<String, Object>> subContainers = this.seekContainerForMerge(subJudgeMap, subAggsRowData);
                    //戻り値へ検索結果をセット
                    retValue.addAll(subContainers);
                }
            }
        }
        
        return retValue;
    }
    
    /**
     * リストソート順：マップキー数降順.
     */
    public static final Comparator<Map<String, Object>> SORT_POLICY_KEY_COUNT_DESC = 
            new Comparator<Map<String, Object>>() {
                public int compare(final Map<String, Object> o1, final Map<String, Object> o2) {
                    return o2.keySet().size() - o1.keySet().size();
                }
            };
    /**
     * Aggregation情報をRowDataへマージする
     * @param aggsContainer Aggsリスト
     * @param sqlInfo SQL文分析情報
     * @throws SQLException 
     * @throws Exception 想定外異常
     */
    @SuppressWarnings({ "resource", "unchecked" })
    //public void mergeWithAggsMap(final List<Map<String, Object>> aggsContainer, final SQLInfo sqlInfo) throws Exception {
    public void mergeWithAggsMap(final List<Map<String, Object>> aggsContainer, final SQLInfo sqlInfo) throws SQLException {
        //GroupBy項目の多いレコードを先頭に
        Collections.sort(aggsContainer, SORT_POLICY_KEY_COUNT_DESC);
        //元ResultSetに存在しないデータ件数(未使用)
        @SuppressWarnings("unused")
        int newResultCnt = 0;
        for (Map<String, Object> aggsRowData : aggsContainer) {
            //Aggregation項目名称リスト取得(データ項目でないため削除)
            String aggsName = (String) aggsRowData.remove(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.COLUMN_TYPE_AGGS.getKey());
            //タイプ情報を取得(データ項目でないため削除)
            String aggsType = (String) aggsRowData.remove(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.TYPE.getKey());
            //マージであるか(true:マージである false:新規追加)
            boolean isMerged = false;
            //RowDataへマージ
            ResultSet resultSet = this;
            while (resultSet.next()) {
                //同一typeでない場合、次へ
                if (!StringUtils.defaultString(resultSet.getCurrentDataName()).equals(aggsType)) {
                    continue;
                }
                //マージ判定用Map
                Map<String, Object> judgeRowData = new LinkedHashMap<String, Object>(aggsRowData);
                //マージ対象Aggregation情報を判定条件から除外
                Object aggsValue = judgeRowData.remove(aggsName);
                //マージ対象コンテナーを検索
                List<Map<String, Object>> containers = this.seekContainerForMerge(this.currentRowData.getSource(), judgeRowData);
                if (containers.size() > 0) {
                    //マージ対象コンテナーが存在する場合、コンテナー対象に対してデータをセット
                    for (Map<String, Object> container : containers) {
                        if (container.containsKey(aggsName) && container.get(aggsName) != null) {
                            Object value = container.get(aggsName);
                            if (value instanceof List) {
                                ((List<Object>) value).add(aggsValue);
                            } else {
                                List<Object> valueList = new ArrayList<Object>();
                                valueList.add(value);
                                valueList.add(aggsValue);
                                container.put(aggsName, valueList);
                            }
                        } else {
                            container.put(aggsName, aggsValue);
                        }
                    }
                    //マージである
                    isMerged = true;
                }
                
            }
            //新規追加(一致した行は存在しなかった)場合、新行として追加
            if (!isMerged) {
                newResultCnt++;
                this.addRowData(aggsType, aggsRowData);
            }
            //インデックスをリセット
            resultSet.resetIndex();
        }
        
        //検索条件にソート情報が存在、且つ未ソートである場合場合、ソート処理を行う
        if (sqlInfo.isSortInfoExists() && !sqlInfo.isAggregationSorted()) {
            final Map<String, SortOrder> orderList = sqlInfo.getSortList();
            // ResultSetデータ全件比較
            Collections.sort(this.rowDataList, new Comparator<RowData>() {
                public int compare(final RowData o1, final RowData o2) {
                    // 比較用ビルダ
                    CompareToBuilder compareBuilder = new CompareToBuilder();
                    // ソート情報を全件ループ
                    Iterator<String> fieldNames = orderList.keySet().iterator();
                    while (fieldNames.hasNext()) {
                        // ソート項目名称
                        String fieldName = fieldNames.next();
                        // ソート順(ASC/DESC)
                        SortOrder order = orderList.get(fieldName);
                        // ソート項目値1
                        Object value1 = o1.getSource().get(fieldName);
                        // ソート項目値2
                        Object value2 = o2.getSource().get(fieldName);

                        // 該当ソート項目名称は存在しない場合
                        ResultSet.this.currentRowData = o1;
                        if (!ResultSet.this.existsColumn(fieldName)) {
                            // [.]にて分割し、項目値を再検索
                            // ソート項目値1
                            String[] levelFieldName = StringUtils.split(fieldName, ".");
                            try {
                                value1 = ResultSet.this.seekMapValue(Object.class, levelFieldName);
                            } catch (SQLException e) { }
                            // ソート項目値2
                            ResultSet.this.currentRowData = o2;
                            try {
                                value2 = ResultSet.this.seekMapValue(Object.class,levelFieldName);
                            } catch (SQLException e) { }
                            ResultSet.this.currentRowData = null;
                        }
                        // ソート順判定
                        if (SortOrder.ASC.equals(order)) {
                            // ASC
                            compareBuilder.append(value1, value2);
                        } else {
                            // DESC
                            compareBuilder.append(value2, value1);
                        }
                    }
                    // 比較結果を返し、ResultSetをソートする
                    return compareBuilder.toComparison();
                }
            });
        }
        
        //「From -- Size」間のデータを絞り込み
        if (sqlInfo.getFrom() > 0 || this.rowDataList.size() > sqlInfo.getSize()) {
            int dataSize = this.rowDataList.size();
            int fromIndex = sqlInfo.getFrom();
            int endIndex = dataSize > sqlInfo.getSize() ? sqlInfo.getSize() : dataSize;
            
            if (dataSize <= fromIndex) {
                this.rowDataList.clear();
            } else {
                this.rowDataList = this.rowDataList.subList(sqlInfo.getFrom(), endIndex);
            }
        }
        //インデックスをリセット
        this.resetIndex();
    }
    
    /* (non-Javadoc)
     * @see java.sql.ResultSet#next()
     */
    public boolean next() throws SQLException {
        if (this.rowDataIterator == null) {
            this.rowDataIterator = rowDataList.listIterator();
        }
        boolean retValue = false;
        if (retValue = this.rowDataIterator.hasNext()) {
            this.currentRowData = this.rowDataIterator.next();
        }
        return retValue;
    }
    
    /**
     * Iteratorインデックスをリセット.
     */
    public void resetIndex() {
        this.rowDataIterator = rowDataList.listIterator();
    }
    
    /* (non-Javadoc)
     * @see java.sql.ResultSet#getString(java.lang.String)
     */
    public String getString(String columnLabel) throws SQLException {
        if (!this.currentRowData.getSource().containsKey(columnLabel)) {
            throw new SQLException(columnLabel + " not exists");
        }
        Object value = this.currentRowData.getSource().get(columnLabel);
        if (value == null) {
            return null;
        } else {
            return String.valueOf(value);
        }
    }
    
    /* (non-Javadoc)
     * @see java.sql.ResultSet#getInt(java.lang.String)
     */
    public int getInt(String columnLabel) throws SQLException {
        if (!this.currentRowData.getSource().containsKey(columnLabel)) {
            throw new SQLException(columnLabel + " not exists");
        }
        Object retValue = this.currentRowData.getSource().get(columnLabel);
        if (retValue == null) {
            return 0;
        } else {
            return new BigDecimal(String.valueOf(retValue)).intValue();
        }
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getLong(java.lang.String)
     */
    public long getLong(String columnLabel) throws SQLException {
        if (!this.currentRowData.getSource().containsKey(columnLabel)) {
            throw new SQLException(columnLabel + " not exists");
        }
        
        Object retValue = this.currentRowData.getSource().get(columnLabel);
        if (retValue == null) {
            return 0L;
        } else {
            return new BigDecimal(String.valueOf(retValue)).longValue();
        }
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getDate(java.lang.String)
     */
    public java.sql.Date getDate(String columnLabel) throws SQLException {
        String charValue = getString(columnLabel);
        if (charValue == null) {
            return null;
        }
        DateTime dateTimeValue = CommonParams.ELASTICSEARCH_DATE_FORMATTER_ELASTIC.parseDateTime(charValue);
        return new java.sql.Date(dateTimeValue.toDate().getTime());
    }
    /**
     * 日付データ取得.<br>
     * (Elasticsearch検索の場合、日付取得にはgetDate()関数をりようできない).
     * @param columnLabel 項目名称
     * @return 日付データ
     * @throws SQLException 想定外異常
     */
    public java.util.Date getDateTime(String columnLabel) throws SQLException {
        String charValue = getString(columnLabel);
        if (charValue == null) {
            return null;
        }
        DateTime dateTimeValue = CommonParams.ELASTICSEARCH_DATE_FORMATTER_ELASTIC.parseDateTime(charValue);
        return dateTimeValue.toDate();
    }
    /* (non-Javadoc)
     * @see java.sql.ResultSet#getTimestamp(java.lang.String)
     */
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        java.util.Date datetime = getDateTime(columnLabel);
        if (datetime == null) {
            return null;
        }
        return new Timestamp(datetime.getTime());
    }
    /**
     * リストデータを取得する
     * @param columnLabel 項目名称
     * @param classType データ型
     * @return リストデータ
     * @throws SQLException 
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String columnLabel, Class<T> classType) throws SQLException {
        if (!this.currentRowData.getSource().containsKey(columnLabel)) {
            throw new SQLException(columnLabel + " not exists");
        }
        Object retValue = this.currentRowData.getSource().get(columnLabel);
        if (retValue == null) {
            return null;
        } else if (retValue.getClass().isAssignableFrom(classType)) {
            List<T> retValueList = new ArrayList<T>();
            retValueList.add((T) retValue);
            return retValueList;
        } else {
            return (List<T>) retValue;
        }
    }
    /**
     * 階層データを取得する.
     * @param classType データ型
     * @param columnLabels 項目名称
     * @return 階層データ
     * @throws SQLException 
     */
    @SuppressWarnings("unchecked")
    public <T> T seekMapValue(Class<T> classType, String...columnLabels) throws SQLException {
        boolean columnLabelExists = true;
        Object retValue = null;
        if (columnLabels != null && columnLabels.length > 0) {
            retValue = this.currentRowData.getSource();
            for (String columnLabel : columnLabels) {
                if (retValue != null && retValue instanceof Map) {
                    Map<String, Object> seekMap = (Map<String, Object>) retValue;
                    if (seekMap.containsKey(columnLabel)) {
                        retValue = seekMap.get(columnLabel);
                        continue;
                    }
                }
                retValue = null;
                columnLabelExists = false;
                break;
            }
        }
        if (columnLabelExists) {
            if (retValue == null) {
                return null;
            } else {
                return (T) retValue;
            }
        } else {
            throw new SQLException(StringUtils.join(columnLabels, ".") + " not exist");
        }
    }
    
    /**
     * 階層データを取得する.
     * @param columnLabels 項目名称
     * @return 対象データ値
     * @throws SQLException 
     */
    public Object seekValue(String...columnLabels) throws SQLException {
        return seekValue(this.currentRowData.getSource(), columnLabels);
    }
    /**
     * 階層データを取得する.
     * @param 探索対象
     * @param columnLabels 項目名称
     * @return 対象データ値
     * @throws SQLException 
     */
    @SuppressWarnings("unchecked")
    public Object seekValue(Object currentData, String...columnLabels) {
        if (!this.existsColumn(currentData, columnLabels)) {
            throw new ElasticsearchJDBCException(StringUtils.join(columnLabels, ".") + " not exist");
        }
        //戻り値
        Object retValue = null;
        if (columnLabels != null && columnLabels.length > 0) {
            retValue = currentData;
            SeekLoop:
            for (int i=0; i < columnLabels.length; i++) {
                String columnLabel = columnLabels[i];
                if (retValue != null && retValue instanceof Map) {
                    Map<String, Object> seekMap = (Map<String, Object>) retValue;
                    if (seekMap.containsKey(columnLabel)) {
                        retValue = seekMap.get(columnLabel);
                        continue SeekLoop;
                    }
                    if (StringUtils.isNotBlank(columnLabel) && columnLabel.indexOf(".") > -1) {
                        String[] subLabels = StringUtils.split(columnLabel, ".");
                        if (this.existsColumn(retValue, subLabels)) {
                            retValue = seekValue(seekMap, subLabels);
                            continue SeekLoop;
                        }
                    }
                } else if (retValue != null && retValue instanceof List) {
                    List<Object> seekList = (List<Object>) retValue;
                    String[] subLabel = (String[]) ArrayUtils.subarray(columnLabels, i, columnLabels.length);
                    if (this.existsColumn(retValue, subLabel)) {
                        List<Object> listValue = new ArrayList<Object>();
                        for (Object seekObject : seekList) {
                            Object tempData = seekValue(seekObject, subLabel);
                            listValue.add(tempData);
                        }
                        retValue = listValue;
                        break SeekLoop;
                    }
                }
                
                retValue = null;
                break;
            }
        }
        
        return retValue;
    }
    /**
     * 対象項目が存在するか.
     * @param columnLabels 項目名称
     * @return true:存在する false:存在しない
     */
    public boolean existsColumn(String...columnLabels) {
        return this.existsColumn(this.currentRowData.getSource(), columnLabels);
    }
    
    /**
     * 対象項目が存在するか.
     * @param currentData 探索対象
     * @param columnLabels 項目名称
     * @return true:存在する false:存在しない
     */
    @SuppressWarnings("unchecked")
    public boolean existsColumn(Object currentData, String...columnLabels) {
        boolean retValue = false;
        if (columnLabels != null && columnLabels.length > 0) {
            ExistLoop:
            for (int i=0; i < columnLabels.length; i++) {
                String columnLabel = columnLabels[i];
                if (currentData != null && currentData instanceof Map) {
                    Map<String, Object> judgeMap = (Map<String, Object>) currentData;
                    if (judgeMap.containsKey(columnLabel)) {
                        retValue = true;
                        currentData = judgeMap.get(columnLabel);
                        continue ExistLoop;
                    }
                    if (StringUtils.isNotBlank(columnLabel) && columnLabel.indexOf(".") > -1) {
                        String[] subLabels = StringUtils.split(columnLabel, ".");
                        if (retValue = existsColumn(judgeMap, subLabels)) {
                            continue ExistLoop;
                        }
                    }
                } else if (currentData != null && currentData instanceof List) {
                    List<Object> judgeList = (List<Object>) currentData;
                    
                    if (judgeList != null && judgeList.size() > 0) {
                        String[] subLabel = (String[]) ArrayUtils.subarray(columnLabels, i, columnLabels.length);
                        for (Object judgeObject : judgeList) {
                            if (retValue = existsColumn(judgeObject, subLabel)) {
                                break ExistLoop;
                            }
                        }
                    }
                }
                retValue = false;
                break;
            }
        }
        return retValue;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getObject(java.lang.String)
     */
    public Object getObject(String columnLabel) throws SQLException {
        if (!this.currentRowData.getSource().containsKey(columnLabel)) {
            throw new SQLException(columnLabel + " not exists");
        }
        return this.currentRowData.getSource().get(columnLabel);
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#close()
     */
    public void close() throws SQLException {
        //実装しない
    }
    
    /**
     * データ総件数を取得する.
     * @return データ総件数
     */
    public long getHitsTotal() {
        return hitsTotal;
    }

    /**
     * データ総件数をセットする.
     * @param hitsTotal データ総件数
     */
    public void setHitsTotal(long hitsTotal) {
        this.hitsTotal = hitsTotal;
    }
    
    /**
     * 取得されたデータ件数を取得する.
     * @return データ件数
     */
    public Integer getDataRowCount() {
        return this.rowDataList.size();
    }

    /**
     * TODO 以下未実装
     */
    
    /* (non-Javadoc)
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    public <T> T unwrap(Class<T> iface) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    public boolean isWrapperFor(Class<?> iface) throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#wasNull()
     */
    public boolean wasNull() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getString(int)
     */
    public String getString(int columnIndex) throws SQLException {
        
        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBoolean(int)
     */
    public boolean getBoolean(int columnIndex) throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getByte(int)
     */
    public byte getByte(int columnIndex) throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getShort(int)
     */
    public short getShort(int columnIndex) throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getInt(int)
     */
    public int getInt(int columnIndex) throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getLong(int)
     */
    public long getLong(int columnIndex) throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getFloat(int)
     */
    public float getFloat(int columnIndex) throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getDouble(int)
     */
    
    public double getDouble(int columnIndex) throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBigDecimal(int, int)
     */
    public BigDecimal getBigDecimal(int columnIndex, int scale)
            throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBytes(int)
     */
    public byte[] getBytes(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getDate(int)
     */
    public java.sql.Date getDate(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getTime(int)
     */
    public Time getTime(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getTimestamp(int)
     */
    public Timestamp getTimestamp(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getAsciiStream(int)
     */
    public InputStream getAsciiStream(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getUnicodeStream(int)
     */
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBinaryStream(int)
     */
    public InputStream getBinaryStream(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBoolean(java.lang.String)
     */
    public boolean getBoolean(String columnLabel) throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getByte(java.lang.String)
     */
    public byte getByte(String columnLabel) throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getShort(java.lang.String)
     */
    public short getShort(String columnLabel) throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getFloat(java.lang.String)
     */
    public float getFloat(String columnLabel) throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getDouble(java.lang.String)
     */
    public double getDouble(String columnLabel) throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBigDecimal(java.lang.String, int)
     */
    public BigDecimal getBigDecimal(String columnLabel, int scale)
            throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBytes(java.lang.String)
     */
    public byte[] getBytes(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getTime(java.lang.String)
     */
    public Time getTime(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getAsciiStream(java.lang.String)
     */
    public InputStream getAsciiStream(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getUnicodeStream(java.lang.String)
     */
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBinaryStream(java.lang.String)
     */
    public InputStream getBinaryStream(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getWarnings()
     */
    public SQLWarning getWarnings() throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#clearWarnings()
     */
    public void clearWarnings() throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getCursorName()
     */
    public String getCursorName() throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getMetaData()
     */
    public ResultSetMetaData getMetaData() throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getObject(int)
     */
    public Object getObject(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#findColumn(java.lang.String)
     */
    public int findColumn(String columnLabel) throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getCharacterStream(int)
     */
    public Reader getCharacterStream(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getCharacterStream(java.lang.String)
     */
    public Reader getCharacterStream(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBigDecimal(int)
     */
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBigDecimal(java.lang.String)
     */
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#isBeforeFirst()
     */
    public boolean isBeforeFirst() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#isAfterLast()
     */
    public boolean isAfterLast() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#isFirst()
     */
    public boolean isFirst() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#isLast()
     */
    public boolean isLast() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#beforeFirst()
     */
    public void beforeFirst() throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#afterLast()
     */
    public void afterLast() throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#first()
     */
    public boolean first() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#last()
     */
    public boolean last() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getRow()
     */
    public int getRow() throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#absolute(int)
     */
    public boolean absolute(int row) throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#relative(int)
     */
    public boolean relative(int rows) throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#previous()
     */
    public boolean previous() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#setFetchDirection(int)
     */
    public void setFetchDirection(int direction) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getFetchDirection()
     */
    public int getFetchDirection() throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#setFetchSize(int)
     */
    public void setFetchSize(int rows) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getFetchSize()
     */
    public int getFetchSize() throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getType()
     */
    public int getType() throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getConcurrency()
     */
    public int getConcurrency() throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#rowUpdated()
     */
    public boolean rowUpdated() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#rowInserted()
     */
    public boolean rowInserted() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#rowDeleted()
     */
    public boolean rowDeleted() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNull(int)
     */
    public void updateNull(int columnIndex) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBoolean(int, boolean)
     */
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateByte(int, byte)
     */
    public void updateByte(int columnIndex, byte x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateShort(int, short)
     */
    public void updateShort(int columnIndex, short x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateInt(int, int)
     */
    public void updateInt(int columnIndex, int x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateLong(int, long)
     */
    public void updateLong(int columnIndex, long x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateFloat(int, float)
     */
    public void updateFloat(int columnIndex, float x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateDouble(int, double)
     */
    public void updateDouble(int columnIndex, double x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBigDecimal(int, java.math.BigDecimal)
     */
    public void updateBigDecimal(int columnIndex, BigDecimal x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateString(int, java.lang.String)
     */
    public void updateString(int columnIndex, String x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBytes(int, byte[])
     */
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateDate(int, java.sql.Date)
     */
    public void updateDate(int columnIndex, java.sql.Date x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateTime(int, java.sql.Time)
     */
    public void updateTime(int columnIndex, Time x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateTimestamp(int, java.sql.Timestamp)
     */
    
    public void updateTimestamp(int columnIndex, Timestamp x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateAsciiStream(int, java.io.InputStream, int)
     */
    
    public void updateAsciiStream(int columnIndex, InputStream x, int length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBinaryStream(int, java.io.InputStream, int)
     */
    
    public void updateBinaryStream(int columnIndex, InputStream x, int length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateCharacterStream(int, java.io.Reader, int)
     */
    
    public void updateCharacterStream(int columnIndex, Reader x, int length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateObject(int, java.lang.Object, int)
     */
    
    public void updateObject(int columnIndex, Object x, int scaleOrLength)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateObject(int, java.lang.Object)
     */
    
    public void updateObject(int columnIndex, Object x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNull(java.lang.String)
     */
    
    public void updateNull(String columnLabel) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBoolean(java.lang.String, boolean)
     */
    
    public void updateBoolean(String columnLabel, boolean x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateByte(java.lang.String, byte)
     */
    
    public void updateByte(String columnLabel, byte x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateShort(java.lang.String, short)
     */
    
    public void updateShort(String columnLabel, short x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateInt(java.lang.String, int)
     */
    
    public void updateInt(String columnLabel, int x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateLong(java.lang.String, long)
     */
    
    public void updateLong(String columnLabel, long x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateFloat(java.lang.String, float)
     */
    
    public void updateFloat(String columnLabel, float x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateDouble(java.lang.String, double)
     */
    
    public void updateDouble(String columnLabel, double x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBigDecimal(java.lang.String, java.math.BigDecimal)
     */
    
    public void updateBigDecimal(String columnLabel, BigDecimal x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateString(java.lang.String, java.lang.String)
     */
    
    public void updateString(String columnLabel, String x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBytes(java.lang.String, byte[])
     */
    
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateDate(java.lang.String, java.sql.Date)
     */
    
    public void updateDate(String columnLabel, java.sql.Date x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateTime(java.lang.String, java.sql.Time)
     */
    
    public void updateTime(String columnLabel, Time x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateTimestamp(java.lang.String, java.sql.Timestamp)
     */
    
    public void updateTimestamp(String columnLabel, Timestamp x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateAsciiStream(java.lang.String, java.io.InputStream, int)
     */
    
    public void updateAsciiStream(String columnLabel, InputStream x, int length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBinaryStream(java.lang.String, java.io.InputStream, int)
     */
    
    public void updateBinaryStream(String columnLabel, InputStream x, int length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateCharacterStream(java.lang.String, java.io.Reader, int)
     */
    
    public void updateCharacterStream(String columnLabel, Reader reader,
            int length) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateObject(java.lang.String, java.lang.Object, int)
     */
    
    public void updateObject(String columnLabel, Object x, int scaleOrLength)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateObject(java.lang.String, java.lang.Object)
     */
    
    public void updateObject(String columnLabel, Object x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#insertRow()
     */
    
    public void insertRow() throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateRow()
     */
    
    public void updateRow() throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#deleteRow()
     */
    
    public void deleteRow() throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#refreshRow()
     */
    
    public void refreshRow() throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#cancelRowUpdates()
     */
    
    public void cancelRowUpdates() throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#moveToInsertRow()
     */
    
    public void moveToInsertRow() throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#moveToCurrentRow()
     */
    
    public void moveToCurrentRow() throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getStatement()
     */
    
    public java.sql.Statement getStatement() throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getObject(int, java.util.Map)
     */
    
    public Object getObject(int columnIndex, Map<String, Class<?>> map)
            throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getRef(int)
     */
    
    public Ref getRef(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBlob(int)
     */
    
    public Blob getBlob(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getClob(int)
     */
    
    public Clob getClob(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getArray(int)
     */
    
    public Array getArray(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getObject(java.lang.String, java.util.Map)
     */
    
    public Object getObject(String columnLabel, Map<String, Class<?>> map)
            throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getRef(java.lang.String)
     */
    
    public Ref getRef(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getBlob(java.lang.String)
     */
    
    public Blob getBlob(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getClob(java.lang.String)
     */
    
    public Clob getClob(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getArray(java.lang.String)
     */
    
    public Array getArray(String columnLabel) throws SQLException {
        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getDate(int, java.util.Calendar)
     */
    
    public java.sql.Date getDate(int columnIndex, Calendar cal)
            throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getDate(java.lang.String, java.util.Calendar)
     */
    
    public java.sql.Date getDate(String columnLabel, Calendar cal)
            throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getTime(int, java.util.Calendar)
     */
    
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getTime(java.lang.String, java.util.Calendar)
     */
    
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getTimestamp(int, java.util.Calendar)
     */
    
    public Timestamp getTimestamp(int columnIndex, Calendar cal)
            throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getTimestamp(java.lang.String, java.util.Calendar)
     */
    
    public Timestamp getTimestamp(String columnLabel, Calendar cal)
            throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getURL(int)
     */
    
    public URL getURL(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getURL(java.lang.String)
     */
    
    public URL getURL(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateRef(int, java.sql.Ref)
     */
    
    public void updateRef(int columnIndex, Ref x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateRef(java.lang.String, java.sql.Ref)
     */
    
    public void updateRef(String columnLabel, Ref x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBlob(int, java.sql.Blob)
     */
    
    public void updateBlob(int columnIndex, Blob x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBlob(java.lang.String, java.sql.Blob)
     */
    
    public void updateBlob(String columnLabel, Blob x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateClob(int, java.sql.Clob)
     */
    
    public void updateClob(int columnIndex, Clob x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateClob(java.lang.String, java.sql.Clob)
     */
    
    public void updateClob(String columnLabel, Clob x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateArray(int, java.sql.Array)
     */
    
    public void updateArray(int columnIndex, Array x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateArray(java.lang.String, java.sql.Array)
     */
    
    public void updateArray(String columnLabel, Array x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getRowId(int)
     */
    
    public RowId getRowId(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getRowId(java.lang.String)
     */
    
    public RowId getRowId(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateRowId(int, java.sql.RowId)
     */
    
    public void updateRowId(int columnIndex, RowId x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateRowId(java.lang.String, java.sql.RowId)
     */
    
    public void updateRowId(String columnLabel, RowId x) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getHoldability()
     */
    
    public int getHoldability() throws SQLException {

        return 0;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#isClosed()
     */
    
    public boolean isClosed() throws SQLException {

        return false;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNString(int, java.lang.String)
     */
    
    public void updateNString(int columnIndex, String nString)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNString(java.lang.String, java.lang.String)
     */
    
    public void updateNString(String columnLabel, String nString)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNClob(int, java.sql.NClob)
     */
    
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNClob(java.lang.String, java.sql.NClob)
     */
    
    public void updateNClob(String columnLabel, NClob nClob)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getNClob(int)
     */
    
    public NClob getNClob(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getNClob(java.lang.String)
     */
    
    public NClob getNClob(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getSQLXML(int)
     */
    
    public SQLXML getSQLXML(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getSQLXML(java.lang.String)
     */
    
    public SQLXML getSQLXML(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateSQLXML(int, java.sql.SQLXML)
     */
    
    public void updateSQLXML(int columnIndex, SQLXML xmlObject)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateSQLXML(java.lang.String, java.sql.SQLXML)
     */
    
    public void updateSQLXML(String columnLabel, SQLXML xmlObject)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getNString(int)
     */
    
    public String getNString(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getNString(java.lang.String)
     */
    
    public String getNString(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getNCharacterStream(int)
     */
    
    public Reader getNCharacterStream(int columnIndex) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getNCharacterStream(java.lang.String)
     */
    
    public Reader getNCharacterStream(String columnLabel) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNCharacterStream(int, java.io.Reader, long)
     */
    
    public void updateNCharacterStream(int columnIndex, Reader x, long length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNCharacterStream(java.lang.String, java.io.Reader, long)
     */
    
    public void updateNCharacterStream(String columnLabel, Reader reader,
            long length) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateAsciiStream(int, java.io.InputStream, long)
     */
    
    public void updateAsciiStream(int columnIndex, InputStream x, long length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBinaryStream(int, java.io.InputStream, long)
     */
    
    public void updateBinaryStream(int columnIndex, InputStream x, long length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateCharacterStream(int, java.io.Reader, long)
     */
    
    public void updateCharacterStream(int columnIndex, Reader x, long length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateAsciiStream(java.lang.String, java.io.InputStream, long)
     */
    
    public void updateAsciiStream(String columnLabel, InputStream x, long length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBinaryStream(java.lang.String, java.io.InputStream, long)
     */
    
    public void updateBinaryStream(String columnLabel, InputStream x,
            long length) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateCharacterStream(java.lang.String, java.io.Reader, long)
     */
    
    public void updateCharacterStream(String columnLabel, Reader reader,
            long length) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBlob(int, java.io.InputStream, long)
     */
    
    public void updateBlob(int columnIndex, InputStream inputStream, long length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBlob(java.lang.String, java.io.InputStream, long)
     */
    
    public void updateBlob(String columnLabel, InputStream inputStream,
            long length) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateClob(int, java.io.Reader, long)
     */
    
    public void updateClob(int columnIndex, Reader reader, long length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateClob(java.lang.String, java.io.Reader, long)
     */
    
    public void updateClob(String columnLabel, Reader reader, long length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNClob(int, java.io.Reader, long)
     */
    
    public void updateNClob(int columnIndex, Reader reader, long length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNClob(java.lang.String, java.io.Reader, long)
     */
    
    public void updateNClob(String columnLabel, Reader reader, long length)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNCharacterStream(int, java.io.Reader)
     */
    
    public void updateNCharacterStream(int columnIndex, Reader x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNCharacterStream(java.lang.String, java.io.Reader)
     */
    
    public void updateNCharacterStream(String columnLabel, Reader reader)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateAsciiStream(int, java.io.InputStream)
     */
    
    public void updateAsciiStream(int columnIndex, InputStream x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBinaryStream(int, java.io.InputStream)
     */
    
    public void updateBinaryStream(int columnIndex, InputStream x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateCharacterStream(int, java.io.Reader)
     */
    
    public void updateCharacterStream(int columnIndex, Reader x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateAsciiStream(java.lang.String, java.io.InputStream)
     */
    
    public void updateAsciiStream(String columnLabel, InputStream x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBinaryStream(java.lang.String, java.io.InputStream)
     */
    
    public void updateBinaryStream(String columnLabel, InputStream x)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateCharacterStream(java.lang.String, java.io.Reader)
     */
    
    public void updateCharacterStream(String columnLabel, Reader reader)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBlob(int, java.io.InputStream)
     */
    
    public void updateBlob(int columnIndex, InputStream inputStream)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateBlob(java.lang.String, java.io.InputStream)
     */
    
    public void updateBlob(String columnLabel, InputStream inputStream)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateClob(int, java.io.Reader)
     */
    
    public void updateClob(int columnIndex, Reader reader) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateClob(java.lang.String, java.io.Reader)
     */
    
    public void updateClob(String columnLabel, Reader reader)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNClob(int, java.io.Reader)
     */
    
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#updateNClob(java.lang.String, java.io.Reader)
     */
    
    public void updateNClob(String columnLabel, Reader reader)
            throws SQLException {

        
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getObject(int, java.lang.Class)
     */
    
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {

        return null;
    }

    /* (non-Javadoc)
     * @see java.sql.ResultSet#getObject(java.lang.String, java.lang.Class)
     */
    
    public <T> T getObject(String columnLabel, Class<T> type)
            throws SQLException {

        return null;
    }
}

/*
 * Copyright (C) 2017 The elasticsearch-sql-converter Authors
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
package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

import es.jdbc.PreparedStatement;
import es.jdbc.sql.beans.AnalyzerPolicy;
import es.jdbc.sql.beans.JoinRelation;
import es.jdbc.utils.CommonParams;
import es.jdbc.utils.CommonUtils;
import es.jdbc.utils.VariablesFactory;
import com.carrotsearch.hppc.ObjectLookupContainer;
import com.google.gson.Gson;

public class RestClientTester {
    /**
     * Elasticsearch日付変換用フォーマット.
     * Java日付⇒Elastic日付変換用.
     */
    public static final DateTimeFormatter ELASTICSEARCH_DATE_FORMATTER_NORMAL;
    static {
        DateTimeFormatterBuilder dateFormatBuilder = new DateTimeFormatterBuilder();
        DateTimeParser notmalParser = DateTimeFormat.forPattern("yyyyMMdd HHmmss")
                .withZone(DateTimeZone.forID("Asia/Tokyo")).getParser();
        dateFormatBuilder.append(notmalParser);
        ELASTICSEARCH_DATE_FORMATTER_NORMAL = dateFormatBuilder.toFormatter();
    }
    /**
     * strict_date_optional_time.
     */
    private static final String ELASTIC_DATE_FORMAT_STRICT_DATE_OPTIONAL_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    /**
     * Elasticsearch日付変換用フォーマット.
     * Elastic日付⇒Java日付変換用.
     */
    public static final DateTimeFormatter ELASTICSEARCH_DATE_FORMATTER_ELASTIC;
    static {
        DateTimeFormatterBuilder dateFormatBuilder = new DateTimeFormatterBuilder();
        DateTimeParser elasticParser = 
                DateTimeFormat.forPattern(ELASTIC_DATE_FORMAT_STRICT_DATE_OPTIONAL_TIME).withZone(DateTimeZone.forID("Asia/Tokyo")).getParser();
        dateFormatBuilder.append(elasticParser);
        ELASTICSEARCH_DATE_FORMATTER_ELASTIC = dateFormatBuilder.toFormatter();
    }
    /**
     * シングルトン・インスタンス保持.
     */
    private static final class ClientSingletonHolder {
        /**
         * コンストラクター.
         */
        private ClientSingletonHolder() { }
        /**
         * シングルトン・インスタンス.
         */
        static RestHighLevelClient instance = initializeClient();
        /**
         * Clientインスタンス初期化.
         * @return
         */
        static RestHighLevelClient initializeClient() {
            String[] hostAddrs = org.apache.commons.lang3.StringUtils.split("172.20.12.130", ",");
            HttpHost[] hosts = new HttpHost[hostAddrs.length];
            Integer hostPort = 9200;
            for (int i=0; i<hostAddrs.length; i++) {
                hosts[i] = new HttpHost(hostAddrs[i], hostPort);
            }
            RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(hosts));
            
            //Settings settings = Settings.settingsBuilder()
            //        .put("cluster.name", ApplicationSettingUtils.getElasticsearchClusterName())
            //        .put("client.transport.sniff", ApplicationSettingUtils.getElasticsearchClientSniffingMode())
            //        .build();
            //TransportClient client = TransportClient.builder().settings(settings).build();
            //String[] hostAddrs = org.apache.commons.lang3.StringUtils.split(ApplicationSettingUtils.getElasticsearchHostAddress(), ",");
            //for (String hostAddr : hostAddrs) {
            //    client.addTransportAddress(new InetSocketTransportAddress(
            //            new InetSocketAddress(hostAddr, ApplicationSettingUtils.getElasticsearchClusterTransmitPort())));
            //}
            return client;
        }
    }
    
    public static Map<String, Object> getResponseContent(HttpEntity entity) throws UnsupportedOperationException, IOException {
        Map<String, Object> retValue = null;
        
        if (entity == null) {
            return retValue;
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        StringBuilder content = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        
        Gson gson = new Gson();
        //return retValue = JsonUtils.parseJson(Map.class, content.toString());
        return retValue = gson.fromJson(content.toString(), Map.class);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        // Cluster Health
//        try {
//            Response response = ClientSingletonHolder.instance.getLowLevelClient().performRequest("GET", "_cluster/health");
//            StatusLine statusLine = response.getStatusLine();
//            System.out.println(statusLine.getStatusCode());
//            System.out.println(statusLine.getReasonPhrase());
//            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//            String line = reader.readLine();
//            System.out.println(line);
//            System.out.println(JsonUtils.parseJson(Map.class, line));
//        } catch(Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                ClientSingletonHolder.instance.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        
        // Index Exists
        //Response response;
        //try {
        //    response = ClientSingletonHolder.instance.getLowLevelClient().performRequest("GET", "sysdate_tester/_settings");
        //    StatusLine statusLine = response.getStatusLine();
        //    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
        //        Map<String, Object> responseMap = getResponseContent(response.getEntity());
        //        String responseJson = JsonUtils.stringify(responseMap);
        //        Pattern pattern = Pattern.compile(".*\"uuid\" *: *\"(\\w*)\".*");
        //        Matcher matcher = pattern.matcher(responseJson);
        //        System.out.println(matcher.find());
        //        System.out.println(matcher.groupCount());
        //        System.out.println(matcher.group(1));
        //    }
        //} catch (IOException e) {
        //    // TODO Auto-generated catch block
        //    e.printStackTrace();
        //} finally {
        //    try {
        //        ClientSingletonHolder.instance.close();
        //    } catch (IOException e) {
        //        e.printStackTrace();
        //    }
        //}
        
        //// Index Data Count
        //try {
        //    SearchRequest searchRequest = new SearchRequest();
        //    searchRequest.indices("sysdate_tester");
        //    SearchResponse searchResponse = ClientSingletonHolder.instance.search(searchRequest);
        //    System.out.println(searchResponse.status().getStatus());
        //    System.out.println(searchResponse.getHits().getTotalHits());
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}
        
        // Search Data
//        try {
//            SearchRequest searchRequest = new SearchRequest();
//            searchRequest.indices("sysdate_tester");
//            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); 
//            searchSourceBuilder.query(QueryBuilders.termQuery("ID", 1));
//            searchSourceBuilder.size(1);
//            searchRequest.source(searchSourceBuilder);
//            SearchResponse searchResponse = ClientSingletonHolder.instance.search(searchRequest);
//            
//            SearchHits searchHits = searchResponse.getHits();
//            if (searchHits.getTotalHits() > 0) {
//                //Loop all results
//                SearchHit[] hitArray = searchHits.getHits();
//                for (SearchHit hit : hitArray) {
//                    //Get data fron response Source
//                    Map<String, Object> sourceMap = hit.getSourceAsMap();
//                    System.out.println(sourceMap);
//                    Object sysDate = sourceMap.get("SYSTEM_DATETIME");
//                    System.out.println(sysDate.getClass());
//                    System.out.println(sysDate);
//                    DateTime dateTimeValue = ELASTICSEARCH_DATE_FORMATTER_ELASTIC.parseDateTime((String) sysDate);
//                    System.out.println(dateTimeValue);
//                    //Map<String, Object> sourceMap = hit.getSource();
//                    //String settingID = ElasticsearchUtils.getString(
//                    //        sourceMap, SysCodeSetting.SYS_CODE_SETTING.SETTING_ID.getFieldName());
//                    //Integer settingCode = ElasticsearchUtils.getInteger(
//                    //        sourceMap, SysCodeSetting.SYS_CODE_SETTING.SETTING_CODE.getFieldName());
//                    //
//                    //retValue.setSettingID(settingID);
//                    //retValue.setSettingCode(settingCode);
//                    //break;
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        
        // Update Data
        //try {
        //    UpdateRequest request = new UpdateRequest();
        //    request.index("sysdate_tester");
        //    request.id("1");
        //    request.type("doc");
        //    
        //    XContentBuilder contentBuilder = XContentFactory.jsonBuilder().startObject();
        //    contentBuilder.field("SYSTEM_DATETIME", ELASTICSEARCH_DATE_FORMATTER_NORMAL.parseDateTime(StringUtils.formatDateTime(new Date())));
        //    contentBuilder.endObject();
        //    
        //    request.doc(contentBuilder);
        //    UpdateResponse response = ClientSingletonHolder.instance.update(request);
        //    System.out.println(response.getResult() == DocWriteResponse.Result.CREATED);
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}
        
        //try {
        //    Response response = ClientSingletonHolder.instance.getLowLevelClient().performRequest("GET", "_aliases");
        //    StatusLine statusLine = response.getStatusLine();
        //    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
        //        Map<String, Object> responseMap = getResponseContent(response.getEntity());
        //        if (responseMap != null && responseMap.size() > 0) {
        //            Iterator<String> keys = responseMap.keySet().iterator();
        //            while (keys.hasNext()) {
        //                String key = keys.next();
        //                Map<String, Object> obj = (Map<String, Object>) responseMap.get(key);
        //                Map<String, Object> alias = (Map<String, Object> ) obj.get("aliases");
        //                System.out.println(key + ":=" + alias.size());
        //            }
        //        }
        //        
        //        System.out.println(responseMap);
        //    }
        //} catch (IOException e) {
        //    // TODO Auto-generated catch block
        //    e.printStackTrace();
        //}
        
        //// Create Index
        //try {
        //    //Mapping file path
        //    Path mappingPath = Paths.get(System.getenv("JBAT_BASE"), MessageFormat.format("/config/mapping/{0}.json", "sys_code_setting"));
        //    //Create Mapping
        //    String mappingJson = org.apache.commons.lang3.StringUtils.join(Files.readAllLines(mappingPath), "");
        //    XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().map(JsonUtils.parseJson(Map.class, mappingJson));
        //
        //    CreateIndexRequest createReq = new CreateIndexRequest("sys_code_setting")
        //            .timeout(Const.INDEXING_TIMEOUT_MILLIS)
        //            //.mapping(Const.COMMON_TYPE_NAME, mappingBuilder);
        //            .source(mappingBuilder);
        //    CreateIndexResponse indexResponse = ClientSingletonHolder.instance.indices().create(createReq);
        //    System.out.println(indexResponse.isAcknowledged());
        //} catch (IOException e) {
        //    // TODO Auto-generated catch block
        //    e.printStackTrace();
        //}
        
        // Delete Index
        //DeleteIndexRequest deleteReq = new DeleteIndexRequest("sysdate_tester");
        //try {
        //    DeleteIndexResponse deleteIndexResponse = ClientSingletonHolder.instance.indices().delete(deleteReq);
        //    System.out.println(deleteIndexResponse.isAcknowledged());
        //} catch (IOException e) {
        //    // TODO Auto-generated catch block
        //    System.out.println(2);
        //    e.printStackTrace();
        //} catch (ElasticsearchStatusException e) {
        //    System.out.println(1);
        //    e.printStackTrace();
        //}
        
        //// Alias Index
        //try {
        //    XContentBuilder contentBuilder = XContentFactory.jsonBuilder().startObject();
        //    contentBuilder.startArray("actions");
        //    
        //    contentBuilder.startObject();
        //    contentBuilder.startObject("add");
        //    contentBuilder.field("index", "sys_code_setting");
        //    contentBuilder.field("alias", "sys_code_setting_alias");
        //    contentBuilder.endObject();
        //    contentBuilder.endObject();
        //    
        //    contentBuilder.startObject();
        //    contentBuilder.startObject("add");
        //    contentBuilder.field("index", "sysdate_tester");
        //    contentBuilder.field("alias", "sysdate_tester_alias");
        //    contentBuilder.endObject();
        //    contentBuilder.endObject();
        //    
        //    contentBuilder.endArray();
        //    contentBuilder.endObject();
        //    HttpEntity entity = new NStringEntity(contentBuilder.string(), ContentType.APPLICATION_JSON);
        //    
        //    Response response = ClientSingletonHolder.instance.getLowLevelClient().performRequest("POST", "_aliases", Collections.emptyMap(), entity);
        //    System.out.println(response.getStatusLine());
        //} catch (IOException e) {
        //    // TODO Auto-generated catch block
        //    e.printStackTrace();
        //}
        
//        try {
//            Response response = ClientSingletonHolder.instance.getLowLevelClient().performRequest("HEAD", "sys_code_setting_s");
//            System.out.println(response.getStatusLine());
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        
//        try {
//            Response response = ClientSingletonHolder.instance.getLowLevelClient().performRequest("GET", "item_omniscient_info/_mapping");
//            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
//                Map<String, Object> responseMap = getResponseContent(response.getEntity());
//                getJoin(responseMap);
//            }
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } finally {
//            try {
//                ClientSingletonHolder.instance.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        //VariablesFactory.VARIABLES_STORE.ES_CLIENT_STORE.set(ClientSingletonHolder.instance);
        //try (VariablesFactory.VARIABLES_STORE clientInstance = VariablesFactory.VARIABLES_STORE.ES_CLIENT_STORE) {
        //    System.out.println(VariablesFactory.VARIABLES_STORE.ES_CLIENT_STORE.get(RestHighLevelClient.class)== null);
        //    System.out.println(CommonUtils.isParent("item_omniscient_info", "STOCK_PERMISSION_INFO", "COLOR_INFO"));
        //} catch (IOException e) {
        //    e.printStackTrace();
        //} catch (Exception e1) {
        //    e1.printStackTrace();
        //}
        //System.out.println(VariablesFactory.VARIABLES_STORE.ES_CLIENT_STORE.get(RestHighLevelClient.class)== null);
        
//        try {
////            String sql = "SELECT CHANNEL_CODE, MEDIA_KBN, MAX(FIRST_START_DATETIME) AS FIRST_START_DATETIME, MIN(SALE_PRICE_TAX) AS SALE_PRICE_TAX FROM TMGOD006 GROUP BY CHANNEL_CODE, MEDIA_KBN ORDER BY CHANNEL_CODE, MEDIA_KBN DESC";
//            String sql = "SELECT * FROM SKU_INFO WHERE EXISTS (SELECT 1 FROM STOCK_PERMISSION_INFO)";
//            JoinRelation itemInfo = new JoinRelation("ITEM_INFO");
//            JoinRelation colorInfo = new JoinRelation("COLOR_INFO");
//            JoinRelation stockInfo = new JoinRelation("STOCK_PERMISSION_INFO");
//            JoinRelation skuInfo = new JoinRelation("SKU_INFO");
//            itemInfo.addChild(colorInfo);
//            colorInfo.addChild(stockInfo);
//            colorInfo.addChild(skuInfo);
////            String sql = "SELECT ITEM_CODE FROM item_omniscient_info WHERE ITEM_CODE = ?";
//            AnalyzerPolicy policy = AnalyzerPolicy.getInstance()
//                    .setMainIndex("item_omniscient_info")
//                    .setIgnoreValueCondition(false)
//                    .setJoinRelation(itemInfo);
//                    //.setFetchSize(0);
//            PreparedStatement statement = new PreparedStatement(ClientSingletonHolder.instance, policy);
//            statement.setString(1, "1004776");
//            ResultSet rs = statement.executeQuery(sql);
////            while (rs.next()) {
////                System.out.println(rs.getString("CHANNEL_CODE"));
////                System.out.println(rs.getString("MEDIA_KBN"));
////                System.out.println(rs.getDate("FIRST_START_DATETIME"));
////                System.out.println(rs.getLong("SALE_PRICE_TAX"));
//////                System.out.println(rs.getString("ITEM_CODE"));
////                System.out.println();
////            }
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            System.out.println(e);
//            e.printStackTrace();
//        } finally {
//            try {
//                ClientSingletonHolder.instance.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

    	//String sql = "WITH     MATCHED_SKU_INFO AS (         SELECT             TM06.ITEM_CODE,             TM06.COLOR_CODE,             TM06.SKU_CODE,             TM35.CHANNEL_CODE,             TM35.MEDIA_KBN,             TM06.SIZE_KBN_CODE,             TM06.SIZE_KBN_BRANCH_NO,             TM06.SOLDOUT_FLG,             TM06.DOUBLE_PRICE_END_DATE,             NVL(TM06.EX_SALE_PRICE_TAX, 0) AS EX_SALE_PRICE_TAX,             NVL(TM06.EX_SALE_PRICE, 0) AS EX_SALE_PRICE,             TM08.SPEC_DETAIL_SIZE,             TO_DATE(TM06.SHIPMENT_START_DATE, 'YYYYMMDD') AS SHIPMENT_START_DATE,             TO_DATE(TM35.NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') AS NONDISP_START_DATETIME,             TO_DATE(TM35.NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS') AS NONDISP_END_DATETIME,             TO_DATE(TM06.OUTLET_START_DATE || TM06.OUTLET_START_TIME, 'YYYYMMDDHH24MISS') AS OUTLET_START_DATETIME         FROM             TMGOD006 TM06,             TMGOD008 TM08,             TMGOD035 TM35         WHERE             TM06.SKU_CODE = TM08.SKU_CODE AND             TM06.SKU_CODE = TM35.SKU_CODE AND             TM06.ITEM_CODE = ? AND                          TM35.CHANNEL_CODE = ? AND             TM35.MEDIA_KBN = ? AND             ? >= TO_DATE(TM35.SALE_START_DATETIME, 'YYYYMMDD HH24MISS') AND             ? <= TO_DATE(TM35.SALE_END_DATETIME, 'YYYYMMDD HH24MISS') AND                          (                 (                     (TM35.SECRET_START_DATETIME IS NULL AND TM35.SECRET_END_DATETIME IS NULL) OR                     ? < TO_DATE(TM35.SECRET_START_DATETIME, 'YYYYMMDD HH24MISS') OR                     ? > TO_DATE(TM35.SECRET_END_DATETIME, 'YYYYMMDD HH24MISS')                  )                              ) AND             (                                  TM08.ORDER_LIMIT_FLG = ? OR                                  (TM35.NONDISP_START_DATETIME IS NULL AND TM35.NONDISP_END_DATETIME IS NULL) OR                 ? < TO_DATE(TM35.NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') OR                 ? > TO_DATE(TM35.NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS')              ) AND                          TM06.DEL_FLG = 0 AND             TM08.DEL_FLG = 0 AND             TM35.DEL_FLG = 0      )      SELECT * FROM (     SELECT         T900.ITEM_CODE,         T900.ITEM_NAME,         T100.MEDIA_KBN,         T900.F_BRAND_KBN,         T900.STAFF_VOICE,         T900.ALL_ELEMENT,         T900.ITEM_DISP_BRAND_NAME,         T900.ITEM_INFO,         T900.ITEM_INFO_DETAIL,         T900.SPEC_MATERIAL,         T900.SPEC_MEMO,         T900.SPEC_BRA,         T900.PROMO_LEAD_COMMENT,         T900.ORIGIN_COUNTRY_NAME,         T900.BANNER_IMAGE,         T900.BANNER_URL,         T100.COLOR_CODE,         T109.COLOR_NAME,         T100.SKU_CODE,         T100.SIZE_KBN_CODE,         T100.SIZE_KBN_BRANCH_NO,         T100.SOLDOUT_FLG,         T100.DOUBLE_PRICE_END_DATE,         T100.EX_SALE_PRICE_TAX,         T100.SPEC_DETAIL_SIZE,         T109.SIZE_NAME,         T109.DISP_SORT,         T901.NEW_ITEM_FLG,         T901.NEW_COLOR_FLG,         T901.NEW_SIZE_FLG,         T901.MANUAL_ICON_FLG_1,         T901.MANUAL_ICON_FLG_2,         T901.MANUAL_ICON_FLG_3,         T901.MANUAL_ICON_FLG_4,         T901.MANUAL_ICON_FLG_5,         T901.NEW_OUTLET_FLG,         T901.OUTLET_FLG,         T901.PRICE_DOWN_FLG,         T901.ADVANCE_ORDER_FLG,         T901.WEB_ONLY_FLG,         T901.WEB_ADVANCE_FLG,         T901.SET_PROMOTION_FLG,         T901.MIX_MATCH_PROMOTION_FLG,         T901.SPECIFIC_PROMOTION_FLG,         T901.SECRET_FLG,                      DEP_SKU_CODE,                          \"EX_SALE_PRICE.EX_PRICE\",             \"SALE_PRICE.PRICE\"                  FROM         MATCHED_SKU_INFO T100,         SKU_PRICE_INFO T101,         SKU_PROMOTION_INFO T102,         ALLOCATION_AMOUNT_INFO T103,         SKU_UNIT_INFO T109,         STOCK_STATUS_MST T200,         ITEM_UNIT_INFO T900,         ITEM_ICON_INFO T901     WHERE         T100.SKU_CODE = T101.SKU_CODE AND         T100.SKU_CODE = T102.SKU_CODE(+) AND         T100.SKU_CODE = T103.SKU_CODE AND         T100.SKU_CODE = T109.SKU_CODE AND         T100.SKU_CODE = T901.SKU_CODE(+) AND         T100.ITEM_CODE = T900.ITEM_CODE                      AND ? BETWEEN DISP_START_DATETIME AND DISP_END_DATETIME                  ) WHERE DISTINCT_FLG = 1";
        //String sql = "WITH     MATCHED_SKU_INFO AS (         SELECT             TM06.ITEM_CODE,             TM06.COLOR_CODE,             TM06.SKU_CODE,             TM35.CHANNEL_CODE,             TM35.MEDIA_KBN,             TM06.SIZE_KBN_CODE,             TM06.SIZE_KBN_BRANCH_NO,             TM06.SOLDOUT_FLG,             TM06.DOUBLE_PRICE_END_DATE,             NVL(TM06.EX_SALE_PRICE_TAX, 0) AS EX_SALE_PRICE_TAX,             NVL(TM06.EX_SALE_PRICE, 0) AS EX_SALE_PRICE,             TM08.SPEC_DETAIL_SIZE,             TO_DATE(TM06.SHIPMENT_START_DATE, 'YYYYMMDD') AS SHIPMENT_START_DATE,             TO_DATE(TM35.NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') AS NONDISP_START_DATETIME,             TO_DATE(TM35.NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS') AS NONDISP_END_DATETIME,             TO_DATE(TM06.OUTLET_START_DATE || TM06.OUTLET_START_TIME, 'YYYYMMDDHH24MISS') AS OUTLET_START_DATETIME         FROM             TMGOD006 TM06,             TMGOD008 TM08,             TMGOD035 TM35         WHERE             TM06.SKU_CODE = TM08.SKU_CODE AND             TM06.SKU_CODE = TM35.SKU_CODE AND             TM06.ITEM_CODE = ? AND                          TM35.CHANNEL_CODE = ? AND             TM35.MEDIA_KBN = ? AND             ? >= TO_DATE(TM35.SALE_START_DATETIME, 'YYYYMMDD HH24MISS') AND             ? <= TO_DATE(TM35.SALE_END_DATETIME, 'YYYYMMDD HH24MISS') AND                          (                 (                     (TM35.SECRET_START_DATETIME IS NULL AND TM35.SECRET_END_DATETIME IS NULL) OR                     ? < TO_DATE(TM35.SECRET_START_DATETIME, 'YYYYMMDD HH24MISS') OR                     ? > TO_DATE(TM35.SECRET_END_DATETIME, 'YYYYMMDD HH24MISS')                  )                              ) AND             (                                  TM08.ORDER_LIMIT_FLG = ? OR                                  (TM35.NONDISP_START_DATETIME IS NULL AND TM35.NONDISP_END_DATETIME IS NULL) OR                 ? < TO_DATE(TM35.NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') OR                 ? > TO_DATE(TM35.NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS')              ) AND                          TM06.DEL_FLG = 0 AND             TM08.DEL_FLG = 0 AND             TM35.DEL_FLG = 0      )      SELECT * FROM (     SELECT         T900.ITEM_CODE,         T900.ITEM_NAME,         T100.MEDIA_KBN,         T900.F_BRAND_KBN,         T900.STAFF_VOICE,         T900.ALL_ELEMENT,         T900.ITEM_DISP_BRAND_NAME,         T900.ITEM_INFO,         T900.ITEM_INFO_DETAIL,         T900.SPEC_MATERIAL,         T900.SPEC_MEMO,         T900.SPEC_BRA,         T900.PROMO_LEAD_COMMENT,         T900.ORIGIN_COUNTRY_NAME,         T900.BANNER_IMAGE,         T900.BANNER_URL,         T100.COLOR_CODE,         T109.COLOR_NAME,         T100.SKU_CODE,         T100.SIZE_KBN_CODE,         T100.SIZE_KBN_BRANCH_NO,         T100.SOLDOUT_FLG,         T100.DOUBLE_PRICE_END_DATE,         T100.EX_SALE_PRICE_TAX,         T100.SPEC_DETAIL_SIZE,         T109.SIZE_NAME,         T109.DISP_SORT,         T901.NEW_ITEM_FLG,         T901.NEW_COLOR_FLG,         T901.NEW_SIZE_FLG,         T901.MANUAL_ICON_FLG_1,         T901.MANUAL_ICON_FLG_2,         T901.MANUAL_ICON_FLG_3,         T901.MANUAL_ICON_FLG_4,         T901.MANUAL_ICON_FLG_5,         T901.NEW_OUTLET_FLG,         T901.OUTLET_FLG,         T901.PRICE_DOWN_FLG,         T901.ADVANCE_ORDER_FLG,         T901.WEB_ONLY_FLG,         T901.WEB_ADVANCE_FLG,         T901.SET_PROMOTION_FLG,         T901.MIX_MATCH_PROMOTION_FLG,         T901.SPECIFIC_PROMOTION_FLG,         T901.SECRET_FLG,                      DEP_SKU_CODE,                          \"EX_SALE_PRICE.EX_PRICE_TAX\",             \"SALE_PRICE.PRICE_TAX\"                  FROM         MATCHED_SKU_INFO T100,         SKU_PRICE_INFO T101,         SKU_PROMOTION_INFO T102,         ALLOCATION_AMOUNT_INFO T103,         SKU_UNIT_INFO T109,         STOCK_STATUS_MST T200,         ITEM_UNIT_INFO T900,         ITEM_ICON_INFO T901     WHERE         T100.SKU_CODE = T101.SKU_CODE AND         T100.SKU_CODE = T102.SKU_CODE(+) AND         T100.SKU_CODE = T103.SKU_CODE AND         T100.SKU_CODE = T109.SKU_CODE AND         T100.SKU_CODE = T901.SKU_CODE(+) AND         T100.ITEM_CODE = T900.ITEM_CODE                      AND ? BETWEEN DISP_START_DATETIME AND DISP_END_DATETIME                  ) WHERE DISTINCT_FLG = 1  UNION ALL SELECT ITEM_CODE, COLOR_CODE, SKU_CODE, STOCK_AMOUNT, STOCK_STATUS, STOCK_STATUS_SEND_DATE  FROM sku_stock_permission_info  WHERE ITEM_CODE = ?          AND CHANNEL_CODE = ?      AND MEDIA_KBN = ?";
    	//String sql = "WITH     MATCHED_SKU_INFO AS (         SELECT             TM06.ITEM_CODE,             TM06.COLOR_CODE,             TM06.SKU_CODE,             TM35.CHANNEL_CODE,             TM35.MEDIA_KBN,             TM06.SIZE_KBN_CODE,             TM06.SIZE_KBN_BRANCH_NO,             TM06.SOLDOUT_FLG,             TM06.DOUBLE_PRICE_END_DATE,             NVL(TM06.EX_SALE_PRICE_TAX, 0) AS EX_SALE_PRICE_TAX,             NVL(TM06.EX_SALE_PRICE, 0) AS EX_SALE_PRICE,             TM08.SPEC_DETAIL_SIZE,             TO_DATE(TM06.SHIPMENT_START_DATE, 'YYYYMMDD') AS SHIPMENT_START_DATE,             TO_DATE(TM35.NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') AS NONDISP_START_DATETIME,             TO_DATE(TM35.NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS') AS NONDISP_END_DATETIME,             TO_DATE(TM06.OUTLET_START_DATE || TM06.OUTLET_START_TIME, 'YYYYMMDDHH24MISS') AS OUTLET_START_DATETIME         FROM                          SKU_INFO                      WHERE             TM06.SKU_CODE = TM08.SKU_CODE AND             TM06.SKU_CODE = TM35.SKU_CODE AND             TM06.ITEM_CODE = ? AND                          TM35.CHANNEL_CODE = ? AND             TM35.MEDIA_KBN = ? AND             ? >= TO_DATE(TM35.SALE_START_DATETIME, 'YYYYMMDD HH24MISS') AND             ? <= TO_DATE(TM35.SALE_END_DATETIME, 'YYYYMMDD HH24MISS') AND                          (                 (                     (TM35.SECRET_START_DATETIME IS NULL AND TM35.SECRET_END_DATETIME IS NULL) OR                     ? < TO_DATE(TM35.SECRET_START_DATETIME, 'YYYYMMDD HH24MISS') OR                     ? > TO_DATE(TM35.SECRET_END_DATETIME, 'YYYYMMDD HH24MISS')                  )                                  OR                 (                 EXISTS (                                          SELECT 1 FROM STOCK_PERMISSION_INFO                      WHERE ITEM_CODE = ? AND                         CHANNEL_CODE = ? AND                         MEDIA_KBN = ? AND                         (                             \"RIGHTS.CUSTCODE\" = ? OR                             \"RIGHTS.CUST_RANKING_CODE\" = ?                         )                                          )                 )                              ) AND             (                                  TM08.ORDER_LIMIT_FLG = ? OR                                  (TM35.NONDISP_START_DATETIME IS NULL AND TM35.NONDISP_END_DATETIME IS NULL) OR                 ? < TO_DATE(TM35.NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') OR                 ? > TO_DATE(TM35.NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS')              ) AND                          TM06.DEL_FLG = 0 AND             TM08.DEL_FLG = 0 AND             TM35.DEL_FLG = 0      )      SELECT * FROM (     SELECT         T900.ITEM_CODE,         T900.ITEM_NAME,         T100.MEDIA_KBN,         T900.F_BRAND_KBN,         T900.STAFF_VOICE,         T900.ALL_ELEMENT,         T900.ITEM_DISP_BRAND_NAME,         T900.ITEM_INFO,         T900.ITEM_INFO_DETAIL,         T900.SPEC_MATERIAL,         T900.SPEC_MEMO,         T900.SPEC_BRA,         T900.PROMO_LEAD_COMMENT,         T900.ORIGIN_COUNTRY_NAME,         T900.BANNER_IMAGE,         T900.BANNER_URL,         T100.COLOR_CODE,         T109.COLOR_NAME,         T100.SKU_CODE,         T100.SIZE_KBN_CODE,         T100.SIZE_KBN_BRANCH_NO,         T100.SOLDOUT_FLG,         T100.DOUBLE_PRICE_END_DATE,         T100.EX_SALE_PRICE_TAX,         T100.SPEC_DETAIL_SIZE,         T109.SIZE_NAME,         T109.DISP_SORT,         T901.NEW_ITEM_FLG,         T901.NEW_COLOR_FLG,         T901.NEW_SIZE_FLG,         T901.MANUAL_ICON_FLG_1,         T901.MANUAL_ICON_FLG_2,         T901.MANUAL_ICON_FLG_3,         T901.MANUAL_ICON_FLG_4,         T901.MANUAL_ICON_FLG_5,         T901.NEW_OUTLET_FLG,         T901.OUTLET_FLG,         T901.PRICE_DOWN_FLG,         T901.ADVANCE_ORDER_FLG,         T901.WEB_ONLY_FLG,         T901.WEB_ADVANCE_FLG,         T901.SET_PROMOTION_FLG,         T901.MIX_MATCH_PROMOTION_FLG,         T901.SPECIFIC_PROMOTION_FLG,         T901.SECRET_FLG,                      DEP_SKU_CODE,                          \"EX_SALE_PRICE.EX_PRICE_TAX\",             \"SALE_PRICE.PRICE_TAX\"                  FROM         MATCHED_SKU_INFO T100,         SKU_PRICE_INFO T101,         SKU_PROMOTION_INFO T102,         ALLOCATION_AMOUNT_INFO T103,         SKU_UNIT_INFO T109,         STOCK_STATUS_MST T200,         ITEM_UNIT_INFO T900,         ITEM_ICON_INFO T901     WHERE         T100.SKU_CODE = T101.SKU_CODE AND         T100.SKU_CODE = T102.SKU_CODE(+) AND         T100.SKU_CODE = T103.SKU_CODE AND         T100.SKU_CODE = T109.SKU_CODE AND         T100.SKU_CODE = T901.SKU_CODE(+) AND         T100.ITEM_CODE = T900.ITEM_CODE                      AND ? BETWEEN DISP_START_DATETIME AND DISP_END_DATETIME                  ) WHERE DISTINCT_FLG = 1";
    	//String sql = "SELECT ORIGINAL_CODE, ORIGINAL_KBN, \"MENUS.MENU_CODE\", \"MENUS.MENU_KBN\", \"MENUS.MENU_LEVEL\", COUNT(DISTINCT NESTED('MENUS'), \"MENUS.MENU_CODE\") AS COUNT_BY_LEVEL, COUNT_ALL(DISTINCT NESTED('MENUS'), FILTER_TERMS(\"MENUS.MENU_KBN\", ?,?,?,?), \"MENUS.MENU_CODE\") AS COUNT_BY_ALL FROM search_menu_defines WHERE NESTED('MENUS') = (SELECT 1 WHERE \"MENUS.MENU_KBN\" IN (?,?,?,?)) AND ORIGINAL_CODE IN (?,?) GROUP BY NESTED('MENUS'), \"MENUS.MENU_LEVEL\"";
    	//String sql = "SELECT ORIGINAL_CODE, ORIGINAL_KBN, \"MENUS.MENU_CODE\", \"MENUS.MENU_KBN\", \"MENUS.MENU_LEVEL\", COUNT(DISTINCT NESTED('MENUS'), \"MENUS.MENU_CODE\") AS COUNT_BY_LEVEL, COUNT_ALL(DISTINCT NESTED('MENUS'), FILTER_TERMS(\"MENUS.MENU_KBN\", 11,12,21,22), \"MENUS.MENU_CODE\") AS COUNT_BY_ALL FROM search_menu_defines WHERE NESTED('MENUS') = (SELECT 1 WHERE \"MENUS.MENU_KBN\" IN (11,12,21,22)) AND ORIGINAL_CODE IN (101) GROUP BY NESTED('MENUS'), \"MENUS.MENU_LEVEL\"";
    	//String sql = "SELECT     ITEM_CODE,          COUNT(OVER_PARTITION(ITEM_CODE), ITEM_CODE) AS CNT_PER_ITEM,     COUNT(OVER_PARTITION(ITEM_CODE, COLOR_CODE), COLOR_CODE) AS CNT_PER_COLOR,     COUNT(DISTINCT \"SALE_PRICE.PRICE\") AS PRICE_TYPE_CNT,              FIRST_VALUE(OVER_PARTITION(ITEM_CODE, ALIAS(APPEND_PARTITION(COLOR_CODE), \"DISP_COLOR\"), INNER_SIZE(1), INNER_ORDER(ORDER_ITEM(SEARCH_ATTRIBUTE_DISP, 'DESC'), ORDER_ITEM(\"ALLOCATION_FILTER>ALLOCATION_AMOUNT_PER_COLOR\", 'DESC'))), SKU_CODE) AS SKU_CODE,         MAX(OVER_PARTITION(ITEM_CODE, ALIAS(APPEND_PARTITION(COLOR_CODE), \"DISP_COLOR\")), DEP_SKU_FLG) AS SEARCH_ATTRIBUTE_DISP,                  SUM(OVER_PARTITION(ITEM_CODE, ALIAS(APPEND_PARTITION(COLOR_CODE), \"DISP_COLOR\")), ALIAS(FILTER_TERMS(SOLDOUT_FILTER_FLG, 0), \"ALLOCATION_FILTER\"), ALLOCATION_AMOUNT) AS ALLOCATION_AMOUNT_PER_COLOR,                  FIRST_VALUE(\"EX_SALE_PRICE.EX_PRICE_TAX\", \"SALE_PRICE.PRICE_TAX\", INNER_ORDER(ORDER_ITEM(ITEM_PRICE_DISP_SORT))) AS DEP_DISP_INFO,              SUM(SOLD_OUT_FLG) AS SOLD_COUNT,     SUM(STOCK_FLG) AS STOCK_COUNT,     MAX(STOCK_FEW_FLG) AS STOCK_FEW_FLG,     SUM(NEW_ITEM_FLG) AS NEW_ITEM_COUNT,     MAX(NEW_SIZE_FLG) AS NEW_SIZE_FLG,     SUM(OVER_PARTITION(ITEM_CODE, COLOR_CODE), NEW_COLOR_FLG) AS NEW_COLOR_COUNT,     MAX(MANUAL_ICON_FLG_1) AS MANUAL_ICON_FLG_1,     MAX(MANUAL_ICON_FLG_2) AS MANUAL_ICON_FLG_2,     MAX(MANUAL_ICON_FLG_3) AS MANUAL_ICON_FLG_3,     MAX(MANUAL_ICON_FLG_4) AS MANUAL_ICON_FLG_4,     MAX(MANUAL_ICON_FLG_5) AS MANUAL_ICON_FLG_5,     SUM(OLD_OUTLET_FLG) AS OLD_OUTLET_COUNT,     MAX(NEW_OUTLET_FLG) AS NEW_OUTLET_FLG,     MAX(OUTLET_FLG) AS OUTLET_FLG,     MAX(PRICE_DOWN_FLG) AS PRICE_DOWN_FLG,     MAX(ADVANCE_ORDER_FLG) AS ADVANCE_ORDER_FLG,     MAX(WEB_ONLY_FLG) AS WEB_ONLY_FLG,     SUM(WEB_NO_ADVANCE_FLG) AS WEB_NO_ADVANCE_COUNT,     MAX(WEB_ADVANCE_FLG) AS WEB_ADVANCE_FLG,     SUM(BACK_ORDER_FLG) AS BACK_ORDER_COUNT,     MAX(SET_PROMOTION_FLG) AS SET_PROMOTION_FLG,     MAX(MIX_MATCH_PROMOTION_FLG) AS MIX_MATCH_PROMOTION_FLG,     MAX(SPECIFIC_PROMOTION_FLG) AS SPECIFIC_PROMOTION_FLG,     MAX(SECRET_FLG) AS SECRET_FLG FROM     SKU_INFO WHERE     ITEM_CODE IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) AND          MEDIA_KBN = ? AND     CHANNEL_CODE  = ? AND     (         (         (SECRET_START_DATETIME IS NULL AND SECRET_END_DATETIME IS NULL) OR         ? < TO_DATE(SECRET_START_DATETIME, 'YYYYMMDD HH24MISS') OR         ? > TO_DATE(SECRET_END_DATETIME, 'YYYYMMDD HH24MISS')          )              ) AND     ? >= TO_DATE(SALE_START_DATETIME,'YYYYMMDD HH24MISS') AND     ? <= TO_DATE(SALE_END_DATETIME,'YYYYMMDD HH24MISS') AND     ? BETWEEN DISP_START_DATETIME AND DISP_END_DATETIME AND     (                  ORDER_LIMIT_FLG = ? OR                  (NONDISP_START_DATETIME IS NULL AND NONDISP_END_DATETIME IS NULL) OR         ? < TO_DATE(NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') OR         ? > TO_DATE(NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS')      )      GROUP BY ITEM_CODE ORDER BY ITEM_CODE";
//    	//Condition Checker
//    	String sql = "SELECT ORIGINAL_CODE, ORIGINAL_KBN, \"MENUS.MENU_CODE\", \"MENUS.MENU_KBN\", \"MENUS.MENU_LEVEL\", COUNT(DISTINCT NESTED('MENUS'), \"MENUS.MENU_CODE\") AS COUNT_BY_LEVEL, COUNT_ALL(DISTINCT NESTED('MENUS'), FILTER_TERMS(\"MENUS.MENU_KBN\", ?,?,?,?), \"MENUS.MENU_CODE\") AS COUNT_BY_ALL FROM search_menu_defines WHERE NESTED('MENUS') = (SELECT 1 WHERE \"MENUS.MENU_KBN\" IN (?,?,?,?)) AND ORIGINAL_CODE IN (?) GROUP BY NESTED('MENUS'), \"MENUS.MENU_LEVEL\"";
//    	//TargetInfo
//    	String sql = "SELECT     ITEM_CODE      FROM          ITEM_INFO      WHERE     EXISTS (         SELECT 1 FROM SKU_INFO         WHERE             (                 (                 (SECRET_START_DATETIME IS NULL AND SECRET_END_DATETIME IS NULL) OR                 ? < SECRET_START_DATETIME OR                 ? > SECRET_END_DATETIME                 )                              ) AND             ? >= SALE_START_DATETIME AND             ? <= SALE_END_DATETIME AND             ? BETWEEN DISP_START_DATETIME AND DISP_END_DATETIME AND             (                                  ORDER_LIMIT_FLG = ? OR                                  (NONDISP_START_DATETIME IS NULL AND NONDISP_END_DATETIME IS NULL) OR                 ? < NONDISP_START_DATETIME OR                 ? > NONDISP_END_DATETIME             ) AND                          MEDIA_KBN = ? AND             CHANNEL_CODE  = ?                          AND CONTAINS(PARENT_CATEGORY_CODE, ?) > 0                  )          ORDER BY FIRST_START_DATETIME DESC, ALLOCATION_AMOUNT DESC, SALE_PRICE_TAX, ITEM_CODE DESC";
//    	//ItemInfo
//    	String sql = "SELECT     ITEM_CODE,     ITEM_NAME,     ITEM_DISP_BRAND_NAME,     ITEM_INFO,     DISP_COLOR_CODE FROM     COLOR_INFO WHERE     EXISTS (         SELECT 1 FROM SKU_INFO         WHERE             ITEM_CODE IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) AND             MEDIA_KBN = ? AND             CHANNEL_CODE  = ? AND             (                 (                 (SECRET_START_DATETIME IS NULL AND SECRET_END_DATETIME IS NULL) OR                 ? < TO_DATE(SECRET_START_DATETIME, 'YYYYMMDD HH24MISS') OR                 ? > TO_DATE(SECRET_END_DATETIME, 'YYYYMMDD HH24MISS')                  )                              ) AND             ? >= TO_DATE(SALE_START_DATETIME,'YYYYMMDD HH24MISS') AND             ? <= TO_DATE(SALE_END_DATETIME,'YYYYMMDD HH24MISS') AND             ? BETWEEN DISP_START_DATETIME AND DISP_END_DATETIME AND             (                                  ORDER_LIMIT_FLG = ? OR                                  (NONDISP_START_DATETIME IS NULL AND NONDISP_END_DATETIME IS NULL) OR                 ? < TO_DATE(NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') OR                 ? > TO_DATE(NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS')              )                  ) ORDER BY ITEM_CODE, DISP_COLOR_CODE DESC";
    	//DispInfo
    	//String sql = "SELECT     ITEM_CODE,          COUNT(OVER_PARTITION(ITEM_CODE), ITEM_CODE) AS CNT_PER_ITEM,     COUNT(OVER_PARTITION(ITEM_CODE, COLOR_CODE), COLOR_CODE) AS CNT_PER_COLOR,     COUNT(DISTINCT \"SALE_PRICE.PRICE\") AS PRICE_TYPE_CNT,              FIRST_VALUE(OVER_PARTITION(ITEM_CODE, ALIAS(APPEND_PARTITION(COLOR_CODE), \"DISP_COLOR\"), INNER_SIZE(1), INNER_ORDER(ORDER_ITEM(SEARCH_ATTRIBUTE_DISP, 'DESC'), ORDER_ITEM(\"ALLOCATION_FILTER>ALLOCATION_AMOUNT_PER_COLOR\", 'DESC'))), SKU_CODE) AS SKU_CODE,         MAX(OVER_PARTITION(ITEM_CODE, ALIAS(APPEND_PARTITION(COLOR_CODE), \"DISP_COLOR\")), DEP_SKU_FLG) AS SEARCH_ATTRIBUTE_DISP,                  SUM(OVER_PARTITION(ITEM_CODE, ALIAS(APPEND_PARTITION(COLOR_CODE), \"DISP_COLOR\")), ALIAS(FILTER_TERMS(SOLDOUT_FILTER_FLG, 0), \"ALLOCATION_FILTER\"), ALLOCATION_AMOUNT) AS ALLOCATION_AMOUNT_PER_COLOR,                  FIRST_VALUE(\"EX_SALE_PRICE.EX_PRICE_TAX\", \"SALE_PRICE.PRICE_TAX\", INNER_ORDER(ORDER_ITEM(ITEM_PRICE_DISP_SORT))) AS DEP_DISP_INFO,              SUM(SOLD_OUT_FLG) AS SOLD_COUNT,     SUM(STOCK_FLG) AS STOCK_COUNT,     MAX(STOCK_FEW_FLG) AS STOCK_FEW_FLG,     SUM(NEW_ITEM_FLG) AS NEW_ITEM_COUNT,     MAX(NEW_SIZE_FLG) AS NEW_SIZE_FLG,     SUM(OVER_PARTITION(ITEM_CODE, COLOR_CODE), NEW_COLOR_FLG) AS NEW_COLOR_COUNT,     MAX(MANUAL_ICON_FLG_1) AS MANUAL_ICON_FLG_1,     MAX(MANUAL_ICON_FLG_2) AS MANUAL_ICON_FLG_2,     MAX(MANUAL_ICON_FLG_3) AS MANUAL_ICON_FLG_3,     MAX(MANUAL_ICON_FLG_4) AS MANUAL_ICON_FLG_4,     MAX(MANUAL_ICON_FLG_5) AS MANUAL_ICON_FLG_5,     SUM(OLD_OUTLET_FLG) AS OLD_OUTLET_COUNT,     MAX(NEW_OUTLET_FLG) AS NEW_OUTLET_FLG,     MAX(OUTLET_FLG) AS OUTLET_FLG,     MAX(PRICE_DOWN_FLG) AS PRICE_DOWN_FLG,     MAX(ADVANCE_ORDER_FLG) AS ADVANCE_ORDER_FLG,     MAX(WEB_ONLY_FLG) AS WEB_ONLY_FLG,     SUM(WEB_NO_ADVANCE_FLG) AS WEB_NO_ADVANCE_COUNT,     MAX(WEB_ADVANCE_FLG) AS WEB_ADVANCE_FLG,     SUM(BACK_ORDER_FLG) AS BACK_ORDER_COUNT,     MAX(SET_PROMOTION_FLG) AS SET_PROMOTION_FLG,     MAX(MIX_MATCH_PROMOTION_FLG) AS MIX_MATCH_PROMOTION_FLG,     MAX(SPECIFIC_PROMOTION_FLG) AS SPECIFIC_PROMOTION_FLG,     MAX(SECRET_FLG) AS SECRET_FLG FROM     SKU_INFO WHERE     ITEM_CODE IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) AND          MEDIA_KBN = ? AND     CHANNEL_CODE  = ? AND     (         (         (SECRET_START_DATETIME IS NULL AND SECRET_END_DATETIME IS NULL) OR         ? < TO_DATE(SECRET_START_DATETIME, 'YYYYMMDD HH24MISS') OR         ? > TO_DATE(SECRET_END_DATETIME, 'YYYYMMDD HH24MISS')          )                  OR         (         EXISTS (             SELECT 1 FROM STOCK_PERMISSION_INFO              WHERE                 CHANNEL_CODE = ? AND                 MEDIA_KBN = ? AND                 (                     \"RIGHTS.CUSTCODE\" = ? OR                     \"RIGHTS.CUST_RANKING_CODE\" = ?                 )             )         )              ) AND     ? >= TO_DATE(SALE_START_DATETIME,'YYYYMMDD HH24MISS') AND     ? <= TO_DATE(SALE_END_DATETIME,'YYYYMMDD HH24MISS') AND     ? BETWEEN DISP_START_DATETIME AND DISP_END_DATETIME AND     (                  ORDER_LIMIT_FLG = ? OR                  (NONDISP_START_DATETIME IS NULL AND NONDISP_END_DATETIME IS NULL) OR         ? < TO_DATE(NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') OR         ? > TO_DATE(NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS')      )      GROUP BY ITEM_CODE ORDER BY ITEM_CODE";
    	//String sql = "SELECT ORIGINAL_CODE, \"FILTER.SEARCH_CODE\", \"FILTER.HEADER_DISP_NAME\", \"FILTER.HEADER_DISP_SORT\", \"FILTER.SEARCH_CONDITION\", \"FILTER.DETAIL_DISP_NAME\", \"FILTER.DETAIL_DISP_SORT\", \"FILTER.SEARCH_CONDITION_CODE\" FROM search_menu_filters WHERE ORIGINAL_CODE IN (?)";
    	//String sql = "WITH MATCHED_SKU_INFO AS ( SELECT TM06.ITEM_CODE, TM06.COLOR_CODE, TM06.SKU_CODE, TM35.CHANNEL_CODE, TM35.MEDIA_KBN, TM06.SIZE_KBN_CODE, TM06.SIZE_KBN_BRANCH_NO, TM06.SOLDOUT_FLG, TM06.DOUBLE_PRICE_END_DATE, NVL(TM06.EX_SALE_PRICE_TAX, 0) AS EX_SALE_PRICE_TAX, NVL(TM06.EX_SALE_PRICE, 0) AS EX_SALE_PRICE, TM08.SPEC_DETAIL_SIZE, TO_DATE(TM06.SHIPMENT_START_DATE, 'YYYYMMDD') AS SHIPMENT_START_DATE, TO_DATE(TM35.NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') AS NONDISP_START_DATETIME, TO_DATE(TM35.NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS') AS NONDISP_END_DATETIME, TO_DATE(TM06.OUTLET_START_DATE || TM06.OUTLET_START_TIME, 'YYYYMMDDHH24MISS') AS OUTLET_START_DATETIME FROM SKU_INFO WHERE TM06.SKU_CODE = TM08.SKU_CODE AND TM06.SKU_CODE = TM35.SKU_CODE AND TM06.ITEM_CODE = ? AND TM35.CHANNEL_CODE = ? AND TM35.MEDIA_KBN = ? AND ? >= TO_DATE(TM35.SALE_START_DATETIME, 'YYYYMMDD HH24MISS') AND ? <= TO_DATE(TM35.SALE_END_DATETIME, 'YYYYMMDD HH24MISS') AND ( ( (TM35.SECRET_START_DATETIME IS NULL AND TM35.SECRET_END_DATETIME IS NULL) OR ? < TO_DATE(TM35.SECRET_START_DATETIME, 'YYYYMMDD HH24MISS') OR ? > TO_DATE(TM35.SECRET_END_DATETIME, 'YYYYMMDD HH24MISS')  ) ) AND ( TM08.ORDER_LIMIT_FLG = ? OR (TM35.NONDISP_START_DATETIME IS NULL AND TM35.NONDISP_END_DATETIME IS NULL) OR ? < TO_DATE(TM35.NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') OR ? > TO_DATE(TM35.NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS')  ) AND TM06.DEL_FLG = 0 AND TM08.DEL_FLG = 0 AND TM35.DEL_FLG = 0  ) SELECT * FROM ( SELECT T900.ITEM_CODE, T900.ITEM_NAME, T100.MEDIA_KBN, T900.F_BRAND_KBN, T900.STAFF_VOICE, T900.ALL_ELEMENT, T900.ITEM_DISP_BRAND_NAME, T900.ITEM_INFO, T900.ITEM_INFO_DETAIL, T900.SPEC_MATERIAL, T900.SPEC_MEMO, T900.SPEC_BRA, T900.PROMO_LEAD_COMMENT, T900.ORIGIN_COUNTRY_NAME, T900.BANNER_IMAGE, T900.BANNER_URL, T100.COLOR_CODE, T109.COLOR_NAME, T100.SKU_CODE, T100.SIZE_KBN_CODE, T100.SIZE_KBN_BRANCH_NO, T100.SOLDOUT_FLG, T100.DOUBLE_PRICE_END_DATE, T100.EX_SALE_PRICE_TAX, T100.SPEC_DETAIL_SIZE, T109.SIZE_NAME, T109.DISP_SORT, T901.NEW_ITEM_FLG, T901.NEW_COLOR_FLG, T901.NEW_SIZE_FLG, T901.MANUAL_ICON_FLG_1, T901.MANUAL_ICON_FLG_2, T901.MANUAL_ICON_FLG_3, T901.MANUAL_ICON_FLG_4, T901.MANUAL_ICON_FLG_5, T901.NEW_OUTLET_FLG, T901.OUTLET_FLG, T901.PRICE_DOWN_FLG, T901.ADVANCE_ORDER_FLG, T901.WEB_ONLY_FLG, T901.WEB_ADVANCE_FLG, T901.SET_PROMOTION_FLG, T901.MIX_MATCH_PROMOTION_FLG, T901.SPECIFIC_PROMOTION_FLG, T901.SECRET_FLG, DEP_SKU_CODE, \"EX_SALE_PRICE.EX_PRICE\", \"SALE_PRICE.PRICE\" FROM MATCHED_SKU_INFO T100, SKU_PRICE_INFO T101, SKU_PROMOTION_INFO T102, ALLOCATION_AMOUNT_INFO T103, SKU_UNIT_INFO T109, STOCK_STATUS_MST T200, ITEM_UNIT_INFO T900, ITEM_ICON_INFO T901 WHERE T100.SKU_CODE = T101.SKU_CODE AND T100.SKU_CODE = T102.SKU_CODE(+) AND T100.SKU_CODE = T103.SKU_CODE AND T100.SKU_CODE = T109.SKU_CODE AND T100.SKU_CODE = T901.SKU_CODE(+) AND T100.ITEM_CODE = T900.ITEM_CODE AND ? BETWEEN DISP_START_DATETIME AND DISP_END_DATETIME ) WHERE DISTINCT_FLG = 1";
    	//String sql = "SELECT ITEM_CODE, COUNT(ITEM_CODE) FROM TMGOD006 WHERE EXISTS (SELECT 1 FROM TMGOD035 WHERE TMGOD006.SKU_CODE = TMGOD035.SKU_CODE AND ? NOT BETWEEN TO_DATE(TMGOD035.SECRET_START_DATETIME, 'YYYYMMDD HH24MISS') AND TO_DATE(TMGOD035.SECRET_END_DATETIME, 'YYYYMMDD HH24MISS')) GROUP BY ITEM_CODE";
    	//String sql = "SELECT     TM06.ITEM_CODE,     COUNT(TM06.SKU_CODE) AS CNT FROM     TMGOD006 TM06,     TMGOD008 TM08,     TMGOD035 TM35 WHERE     TM06.SKU_CODE = TM35.SKU_CODE AND          TM35.CHANNEL_CODE = ? AND     TM35.MEDIA_KBN = ? AND     ? >= TO_DATE(TM35.SALE_START_DATETIME, 'YYYYMMDD HH24MISS') AND     ? <= TO_DATE(TM35.SALE_END_DATETIME, 'YYYYMMDD HH24MISS') AND     ? BETWEEN TO_DATE(TM35.SECRET_START_DATETIME, 'YYYYMMDD HH24MISS') AND TO_DATE(TM35.SECRET_END_DATETIME, 'YYYYMMDD HH24MISS') AND     (                  TM08.ORDER_LIMIT_FLG = ? OR                  (TM35.NONDISP_START_DATETIME IS NULL AND TM35.NONDISP_END_DATETIME IS NULL) OR         ? < TO_DATE(TM35.NONDISP_START_DATETIME, 'YYYYMMDD HH24MISS') OR         ? > TO_DATE(TM35.NONDISP_END_DATETIME, 'YYYYMMDD HH24MISS')      ) AND          ? BETWEEN DISP_START_DATETIME AND DISP_END_DATETIME AND          TM06.DEL_FLG = 0 AND     TM08.DEL_FLG = 0 AND     TM35.DEL_FLG = 0 GROUP BY TM06.ITEM_CODE";
    	//String sql = "SELECT COUNT(ITEM_CODE) FROM DUAL";
    	String sql = "SELECT ITEM_CODE FROM SKU_INFO WHERE ? NOT BETWEEN TO_DATE(T2.SECRET_START_DATETIME, 'YYYYMMDD HH24MISS') AND TO_DATE(T2.SECRET_END_DATETIME, 'YYYYMMDD HH24MISS')";
    	JoinRelation itemR = new JoinRelation("ITEM_INFO");
    	JoinRelation colorR = new JoinRelation("COLOR_INFO");
    	JoinRelation stockR = new JoinRelation("STOCK_PERMISSION_INFO");
    	JoinRelation skuR = new JoinRelation("SKU_INFO");
    	itemR.addChild(colorR);
    	colorR.addChild(stockR);
    	stockR.addChild(skuR);
        try (java.sql.PreparedStatement prepared = new PreparedStatement(sql, ClientSingletonHolder.instance, 
                AnalyzerPolicy.getInstance().setIgnoreValueCondition(true).setIgnoreWithClause(false).setMainIndex("item_omniscient_info").setJoinRelation(itemR));) {
        		//AnalyzerPolicy.getInstance().setIgnoreValueCondition(true).setIgnoreWithClause(false).setMainIndex("search_menu_defines"));) {
        		//AnalyzerPolicy.getInstance().setIgnoreValueCondition(true).setIgnoreWithClause(false).setMainIndex("search_menu_filters"));) {
        	int index = 0;
        	
        	Calendar calendar = Calendar.getInstance();
        	//calendar.set(2018, 7, 7, 14, 59, 39);
        	java.sql.Date sysDate = new java.sql.Date(calendar.getTimeInMillis());
//        	//Condition Checker
//        	prepared.setString(++index, "11");
//        	prepared.setString(++index, "12");
//        	prepared.setString(++index, "21");
//        	prepared.setString(++index, "22");
//        	prepared.setString(++index, "11");
//        	prepared.setString(++index, "12");
//        	prepared.setString(++index, "21");
//        	prepared.setString(++index, "22");
//        	prepared.setString(++index, "10101");
//        	prepared.setString(++index, "10102");
////        	java.sql.Date sysDate = new java.sql.Date(new Date().getTime());
//        	Calendar calendar = Calendar.getInstance();
//        	calendar.set(2018, 7, 7, 14, 59, 39);
//        	java.sql.Date sysDate = new java.sql.Date(calendar.getTimeInMillis());
//        	//TargetItem
//			prepared.setDate(++index, date);
//			prepared.setDate(++index, date);
//			prepared.setDate(++index, date);
//			prepared.setDate(++index, date);
//			prepared.setDate(++index, date);
//			prepared.setInt(++index, 1);
//			prepared.setDate(++index, date);
//			prepared.setDate(++index, date);
//			prepared.setInt(++index, 1);
//			prepared.setString(++index, "1");
//			prepared.setString(++index, "★101★");
//        	//ItemInfo
//        	prepared.setString(++index, "1015244");
//        	prepared.setString(++index, "1019610");
//        	prepared.setString(++index, "1022360");
//        	prepared.setString(++index, "1021926");
//        	prepared.setString(++index, "1022043");
//        	prepared.setString(++index, "1021999");
//        	prepared.setString(++index, "1019043");
//        	prepared.setString(++index, "1021490");
//        	prepared.setString(++index, "1010226");
//        	prepared.setString(++index, "1022060");
//        	prepared.setInt(++index, 1);
//        	prepared.setString(++index, "1");
//        	prepared.setDate(++index, date);
//        	prepared.setDate(++index, date);
//        	prepared.setDate(++index, date);
//        	prepared.setDate(++index, date);
//        	prepared.setDate(++index, date);
//        	prepared.setInt(++index, 1);
//        	prepared.setDate(++index, date);
//        	prepared.setDate(++index, date);
//        	//DispInfo
//        	prepared.setString(++index, "1019610");
//        	prepared.setString(++index, "1019619");
//        	prepared.setString(++index, "1021490");
//        	prepared.setString(++index, "1021491");
//        	prepared.setString(++index, "1010226");
//        	prepared.setString(++index, "1010115");
//        	prepared.setString(++index, "1011208");
//        	prepared.setString(++index, "1011209");
//        	prepared.setString(++index, "1015086");
//        	prepared.setString(++index, "1019611");
//        	prepared.setString(++index, "1019621");
//        	prepared.setString(++index, "1019622");
//        	prepared.setString(++index, "1019620");
//        	prepared.setString(++index, "1011323");
//        	prepared.setString(++index, "1019786");
//        	prepared.setInt(++index, 1);
//        	prepared.setString(++index, "1");
//        	prepared.setDate(++index, date);
//        	prepared.setDate(++index, date);
//        	prepared.setString(++index, "1");
//        	prepared.setInt(++index, 1);
//        	prepared.setLong(++index, 2395579531L);
//        	prepared.setInt(++index, 4);
//        	prepared.setDate(++index, date);
//        	prepared.setDate(++index, date);
//        	prepared.setDate(++index, date);
//        	prepared.setInt(++index, 1);
//        	prepared.setDate(++index, date);
//        	prepared.setDate(++index, date);
//        	//search_menu_filters
//        	prepared.setString(++index, "101");
        	
//        	prepared.setString(++index, "1015244");
//        	prepared.setString(++index, "1");
//            prepared.setInt(++index, 1);
//            prepared.setDate(++index, sysDate);
//            prepared.setDate(++index, sysDate);
//            prepared.setDate(++index, sysDate);
//            prepared.setDate(++index, sysDate);
//        	prepared.setString(++index, "1015244");
//        	prepared.setString(++index, "1");
//            prepared.setInt(++index, 1);
//            prepared.setLong(++index, 2395579531L);
//            prepared.setInt(++index, 4);
//            prepared.setInt(++index, 1);
//            prepared.setDate(++index, sysDate);
//            prepared.setDate(++index, sysDate);
//            prepared.setDate(++index, sysDate);
//        	//prepared.setString(++index, "1022060");
//        	//prepared.setString(++index, "1");
//            //prepared.setInt(++index, 1);
			////ItemDetail
			//prepared.setString(++index, "1019043");
//			prepared.setString(++index, "1");
//			prepared.setInt(++index, 1);
//			prepared.setDate(++index, sysDate);
//			prepared.setDate(++index, sysDate);
//			//prepared.setDate(++index, sysDate);
//			prepared.setDate(++index, sysDate);
//			prepared.setInt(++index, 1);
//			prepared.setDate(++index, sysDate);
//			prepared.setDate(++index, sysDate);
//			prepared.setDate(++index, sysDate);
        	
        	prepared.setDate(++index, sysDate);
            
        	long startTime = Calendar.getInstance().getTimeInMillis();
            es.jdbc.ResultSet rs = (es.jdbc.ResultSet) prepared.executeQuery();
            long endTime = Calendar.getInstance().getTimeInMillis();
            System.out.println("Dur:" + (endTime - startTime));
            while (rs.next()) {
//            	System.out.println("-----------------------------------");
//            	System.out.println(rs.getInt("COUNT_BY_ALL"));
//            	System.out.println(rs.getString("ORIGINAL_CODE"));
//            	System.out.println(rs.getString("ORIGINAL_KBN"));
//                //Nested検索結果「MENUS」
//                List<Object> menuList = rs.getList("MENUS", Object.class);
//                for (Object menu : menuList) {
//                    Map<String, Object> menuMap = (Map<String, Object>) menu;
//                    String menuCode = (String) menuMap.get("MENU_CODE");
//                    int menuLevel = (Integer) menuMap.get("MENU_LEVEL");
//                    String menuKbn = (String) menuMap.get("MENU_KBN");
//                    Long countByLevel = (Long) menuMap.get("COUNT_BY_LEVEL");
//                    System.out.println("--:" + menuKbn + ":" + menuCode + ":" + menuLevel + ":" + countByLevel);
//                }
//                System.out.println("-----------------------------------");
            	//System.out.println(rs.getString("SKU_CODE"));
            	System.out.println(rs.getString("ITEM_CODE"));
            	//System.out.println(rs.getString("COUNT_BY_ALL"));
//                Date sysdate = rs.getDate("SETTING_DATE");
//                if (sysdate != null) {
//                    int hour = sysCalendar.get(Calendar.HOUR_OF_DAY);
//                    int minute = sysCalendar.get(Calendar.MINUTE);
//                    int second = sysCalendar.get(Calendar.SECOND);
//                    sysCalendar.setTime(sysdate);
//                    sysCalendar.set(Calendar.HOUR_OF_DAY, hour);
//                    sysCalendar.set(Calendar.MINUTE, minute);
//                    sysCalendar.set(Calendar.SECOND, second);
//                    java.util.Date sysDate = sysCalendar.getTime();
//                    System.out.println(sysDate);
//                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ClientSingletonHolder.instance.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        //BoolQueryBuilder
//        QueryBuilder parentQuery = JoinQueryBuilders.hasParentQuery("TEST_TABLE", QueryBuilders.boolQuery(), false);
//        System.out.println(parentQuery);
        
//        JoinRelation itemInfo = new JoinRelation("ITEM_INFO");
//        JoinRelation colorInfo = new JoinRelation("COLOR_INFO");
//        JoinRelation stockInfo = new JoinRelation("STOCK_PERMISSION_INFO");
//        JoinRelation skuInfo = new JoinRelation("SKU_INFO");
//        itemInfo.addChild(colorInfo);
//        colorInfo.addChild(stockInfo);
//        colorInfo.addChild(skuInfo);
//        JoinRelation result = CommonUtils.seekRelation(itemInfo, itemInfo, "SKU_INFO");
//        System.out.println(result);
    }
//    static int[] oddNumbers(int l, int r) {
//        /*
//         * Write your code here.
//         */
//        //List<Integer> data = new ArrayList<Integer>();
//        int[] ret = new int[0];
//        for (int i = l; i <= r; i+=2) {
//            if (i%2 == 0){
//                i--;
//            } else {
//                ret = Arrays.copyOf(ret, ret.length + 1);
//                ret[ret.length-1] = i;
//            }
//        }
//        return ret;
//    }
    
//    public static void getJoin(Map<String, Object> dataMap) {
//        if (dataMap == null || dataMap.size() == 0) {
//            return;
//        }
//        Iterator<String> keys = dataMap.keySet().iterator();
//        while (keys.hasNext()) {
//            String key = keys.next();
//            Object value = dataMap.get(key);
//            if (value instanceof Map && value != null) {
//                Map<String, Object> valueMap = (Map<String, Object>) value;
//                Object typeValue = valueMap.get("type");
//                if ("join".equals(typeValue)) {
//                    System.out.println(valueMap.get("relations"));
//                } else {
//                    getJoin(valueMap);
//                }
//            }
//        }
//    }
}

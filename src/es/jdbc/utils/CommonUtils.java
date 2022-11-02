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
package es.jdbc.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;

import es.jdbc.sql.beans.JoinRelation;
import com.google.gson.Gson;

public class CommonUtils {

    private CommonUtils() { }
    
    /**
     * ハッシュキーからオリジナル名称を取得.
     * @param hashKey ハッシュキー
     * @param hashKeyPattern ハッシュキーネーミングパターン
     * @return オリジナル名称
     */
    public static String findOriginalHashKey(final String hashKey, final Pattern hashKeyPattern) {
        String retValue = hashKey;
        if (StringUtils.isBlank(retValue)) {
            return retValue;
        }
        
        Matcher matcher = hashKeyPattern.matcher(hashKey);
        if (matcher.find()) {
            retValue = matcher.group(1);
        }
        return retValue;
    }
    
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getResponseContent(HttpEntity entity) throws UnsupportedOperationException, IOException {
        Map<String, Object> retValue = null;
        
        if (entity == null) {
            return retValue;
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        StringBuilder content = new StringBuilder();
        String line = null;
        while (StringUtils.isNotBlank(line = reader.readLine())) {
            content.append(line);
        }
        
        Gson gson = new Gson();
        return retValue = gson.fromJson(content.toString(), Map.class);
    }
    
    
    private static JoinRelation seekRelationPath(final JoinRelation current, JoinRelation previous, final String relationName) {
        JoinRelation retValue = null;
        if (current == null || !StringUtils.isNotBlank(relationName)) {
            return retValue;
        }
        
        if (relationName.equals(current.getName())) {
            retValue = new JoinRelation(current.getName());
        } else {
            // Parent
            JoinRelation parent = current.getParent();
            if (parent != null && !parent.equals(previous)) {
                JoinRelation result = seekRelationPath(parent, current, relationName);
                if (result != null) {
                    retValue = new JoinRelation(current.getName());
                    retValue.setParent(result);
                }
            }
            // Child
            if (retValue == null && current.getChildren() != null) {
                List<JoinRelation> children = current.getChildren();
                for (JoinRelation child : children) {
                    if (child != null && !child.equals(previous)) {
                        JoinRelation result = seekRelationPath(child, current, relationName);
                        if (result != null) {
                            retValue = new JoinRelation(current.getName());
                            retValue.addChild(result);
                            break;
                        }
                    }
                }
            }
        }
        return retValue;
    }
    public static JoinRelation seekRelation(final JoinRelation current, JoinRelation previous, final String relationName) {
        JoinRelation retValue = null;
        if (current == null || !StringUtils.isNotBlank(relationName)) {
            return retValue;
        }
        
        if (relationName.equals(current.getName())) {
            retValue = current;
        } else {
            // Parent
            JoinRelation parent = current.getParent();
            if (parent != null && !parent.equals(previous)) {
                retValue = seekRelation(parent, current, relationName);
            }
            // Child
            List<JoinRelation> children = current.getChildren();
            if (retValue == null && children != null && children.size() > 0) {
                for (JoinRelation child : children) {
                    if (child != null && !child.equals(previous)) {
                        retValue = seekRelation(child, current, relationName);
                        if (retValue != null) {
                            break;
                        }
                    }
                }
            }
        }
        return retValue;
    }
    public static JoinRelation getRelationPath(final JoinRelation relation, final String startRelationName, final String endRelationName) {
        JoinRelation retValue = null;
        if (relation == null) {
            return retValue;
        }
        JoinRelation startRelation = seekRelation(relation, relation, startRelationName);
        retValue = seekRelationPath(startRelation, startRelation, endRelationName);
        
        return retValue;
    }
    
    //@SuppressWarnings("unchecked")
    //private static Map<String, Object> readRalation(Map<String, Object> dataMap) {
    //    Map<String, Object> retValue = null;
    //    if (dataMap == null || dataMap.size() == 0) {
    //        return retValue;
    //    }
    //    Iterator<String> keys = dataMap.keySet().iterator();
    //    while (keys.hasNext()) {
    //        String key = keys.next();
    //        Object value = dataMap.get(key);
    //        if (value instanceof Map && value != null) {
    //            Map<String, Object> valueMap = (Map<String, Object>) value;
    //            Object typeValue = valueMap.get("type");
    //            if ("join".equals(typeValue)) {
    //                retValue = (Map<String, Object>) valueMap.get("relations");
    //                break;
    //            } else {
    //                retValue = readRalation(valueMap);
    //            }
    //        }
    //    }
    //    return retValue;
    //}
    //
    //public static Map<String, Object> getIndexRelations(final String indexName) throws IOException {
    //    Map<String, Object> retValue = null;
    //    
    //    RestHighLevelClient client = VariablesFactory.VARIABLES_STORE.ES_CLIENT_STORE.get(RestHighLevelClient.class);
    //    if (StringUtils.isBlank(indexName) || client == null) {
    //        return retValue;
    //    }
    //    
    //    Response response = client.getLowLevelClient().performRequest("GET", indexName + "/_mapping");
    //    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
    //        Map<String, Object> responseMap = getResponseContent(response.getEntity());
    //        retValue = readRalation(responseMap);
    //    }
    //    
    //    return retValue;
    //}
    //
    //public static boolean isParent(final String indexName, final String relationName1, final String relationName2) throws IOException {
    //    boolean retValue = false;
    //    if (StringUtils.isBlank(indexName) || StringUtils.isBlank(relationName1) || StringUtils.isBlank(relationName2)) {
    //        return retValue;
    //    }
    //    
    //    Map<String, Object> relations = CommonUtils.getIndexRelations(indexName);
    //    Object relation2 = null;
    //    if (relations != null && (relation2 = relations.get(relationName2)) != null) {
    //        if (relation2 instanceof String) {
    //            retValue = StringUtils.equals(relationName1, (String) relation2);
    //        } else if (relation2.getClass().isArray()) {
    //            for (String relation2Item : (String[]) relation2) {
    //                if (retValue = StringUtils.equals(relationName1, relation2Item)) {
    //                    break;
    //                }
    //            }
    //        }
    //    }
    //    
    //    return retValue;
    //}
}
